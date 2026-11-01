package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

class VaultRepository(
    private val context: Context,
    private val dao: VaultDao,
    val cryptoStorage: CryptoStorageManager
) {
    companion object {
        const val MAX_FILE_SIZE_BYTES = 50L * 1024L * 1024L // 50 MB limit
    }

    val documentsWithFieldsFlow: Flow<List<DocumentWithFields>> = combine(
        dao.observeAllDocuments(),
        dao.observeAllFields()
    ) { docs, fields ->
        val grouped = fields.groupBy { it.documentId }
        docs.filter { !it.isSamplePlaceholder }.map { doc ->
            DocumentWithFields(
                document = doc,
                fields = grouped[doc.id].orEmpty()
            )
        }
    }

    val auditEventsFlow: Flow<List<AuditEventEntity>> = dao.observeAuditEvents()

    /**
     * Purges any legacy demo/sample records so the vault contains exclusively real user documents.
     */
    suspend fun purgeAnyDemoData() = withContext(Dispatchers.IO) {
        val all = dao.getAllDocumentsOnce()
        all.filter { it.isSamplePlaceholder }.forEach { sample ->
            dao.deleteFieldsForDocument(sample.id)
            dao.deleteDocumentById(sample.id)
        }
        dao.deleteSamplePlaceholderDocuments()
    }

    suspend fun saveDocumentWithFields(
        document: DocumentEntity,
        fields: List<DocumentFieldEntity>
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cleanDoc = document.copy(
            isSamplePlaceholder = false,
            updatedAt = now,
            createdAt = if (document.id == 0L) now else document.createdAt,
            iconTypeName = DocumentCategoryGroup.fromCategoryName(document.category).name
        )
        val docId = if (cleanDoc.id == 0L) {
            dao.insertDocument(cleanDoc)
        } else {
            dao.updateDocument(cleanDoc)
            dao.deleteFieldsForDocument(cleanDoc.id)
            cleanDoc.id
        }
        val validFields = fields
            .filter { it.label.isNotBlank() || it.value.isNotBlank() }
            .mapIndexed { index, field ->
                field.copy(id = 0L, documentId = docId, sortOrder = index + 1)
            }
        if (validFields.isNotEmpty()) {
            dao.insertFields(validFields)
        }
        logAudit("DOCUMENT_SAVED", "Saved \"${cleanDoc.title}\" in ${cleanDoc.category}")
        docId
    }

    suspend fun markDocumentOpened(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        dao.updateDocument(doc.copy(lastOpenedAt = System.currentTimeMillis()))
    }

    suspend fun renameDocument(doc: DocumentEntity, newTitle: String) = withContext(Dispatchers.IO) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return@withContext
        dao.updateDocument(doc.copy(title = clean, updatedAt = System.currentTimeMillis()))
        logAudit("DOCUMENT_RENAMED", "Renamed document to \"$clean\"")
    }

    suspend fun moveDocumentCategory(doc: DocumentEntity, newCategory: String, newSubtype: String = "") =
        withContext(Dispatchers.IO) {
            dao.updateDocument(
                doc.copy(
                    category = newCategory,
                    documentSubtype = newSubtype.ifBlank { doc.documentSubtype },
                    iconTypeName = DocumentCategoryGroup.fromCategoryName(newCategory).name,
                    updatedAt = System.currentTimeMillis()
                )
            )
            logAudit("DOCUMENT_CATEGORIZED", "Moved \"${doc.title}\" to $newCategory")
        }

    suspend fun duplicateDocument(docWithFields: DocumentWithFields): Long = withContext(Dispatchers.IO) {
        val src = docWithFields.document
        val now = System.currentTimeMillis()
        var newFrontRef = ""
        var newBackRef = ""
        if (src.frontFileRef.isNotBlank()) {
            cryptoStorage.readDecryptedFile(src.frontFileRef)?.let { bytes ->
                newFrontRef = cryptoStorage.saveEncryptedFile("copy_front_$now", bytes)
            }
        }
        if (src.backFileRef.isNotBlank()) {
            cryptoStorage.readDecryptedFile(src.backFileRef)?.let { bytes ->
                newBackRef = cryptoStorage.saveEncryptedFile("copy_back_$now", bytes)
            }
        }
        val copyDoc = src.copy(
            id = 0L,
            title = "${src.title} (Copy)",
            frontFileRef = newFrontRef,
            backFileRef = newBackRef,
            createdAt = now,
            updatedAt = now
        )
        val newId = dao.insertDocument(copyDoc)
        val copyFields = docWithFields.fields.map { it.copy(id = 0L, documentId = newId) }
        dao.insertFields(copyFields)
        logAudit("DOCUMENT_DUPLICATED", "Duplicated \"${src.title}\"")
        newId
    }

    suspend fun confirmAllOcrFields(documentId: Long) = withContext(Dispatchers.IO) {
        val existing = dao.getFieldsForDocument(documentId)
        existing.forEach { field ->
            if (!field.userConfirmed) {
                dao.updateField(field.copy(userConfirmed = true))
            }
        }
        logAudit("OCR_CONFIRMED", "Confirmed extracted fields for document #$documentId")
    }

    suspend fun toggleFavourite(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        dao.updateDocument(doc.copy(favourite = !doc.favourite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleArchive(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        dao.updateDocument(doc.copy(archived = !doc.archived, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteDocument(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        cryptoStorage.deleteEncryptedFile(doc.frontFileRef)
        cryptoStorage.deleteEncryptedFile(doc.backFileRef)
        dao.deleteFieldsForDocument(doc.id)
        dao.deleteDocumentById(doc.id)
        logAudit("DOCUMENT_DELETED", "Permanently deleted \"${doc.title}\"")
    }

    suspend fun bulkDeleteDocuments(docs: List<DocumentEntity>) = withContext(Dispatchers.IO) {
        docs.forEach { doc ->
            cryptoStorage.deleteEncryptedFile(doc.frontFileRef)
            cryptoStorage.deleteEncryptedFile(doc.backFileRef)
            dao.deleteFieldsForDocument(doc.id)
            dao.deleteDocumentById(doc.id)
        }
        logAudit("BULK_DELETE", "Deleted ${docs.size} documents")
    }

    suspend fun bulkArchiveDocuments(docs: List<DocumentEntity>, archive: Boolean) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            docs.forEach { doc ->
                dao.updateDocument(doc.copy(archived = archive, updatedAt = now))
            }
            logAudit("BULK_ARCHIVE", "${if (archive) "Archived" else "Restored"} ${docs.size} documents")
        }

    suspend fun bulkMoveCategory(docs: List<DocumentEntity>, targetCategory: String) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val group = DocumentCategoryGroup.fromCategoryName(targetCategory)
            docs.forEach { doc ->
                dao.updateDocument(
                    doc.copy(
                        category = targetCategory,
                        iconTypeName = group.name,
                        updatedAt = now
                    )
                )
            }
            logAudit("BULK_CATEGORY", "Moved ${docs.size} documents to $targetCategory")
        }

    /**
     * Universal file importer via Android Storage Access Framework / Photo Picker.
     * Supports PDF, Word (.doc/.docx), Excel (.xls/.xlsx), PowerPoint (.ppt/.pptx),
     * Images (.jpg/.jpeg/.png/.webp/.heic/.heif/.bmp/.tiff), Text (.txt/.rtf/.md),
     * OpenDocument (.odt/.ods/.odp), CSV (.csv), JSON (.json), and XML (.xml).
     * Preserves original file byte-for-byte encrypted with Android Keystore AES-256-GCM.
     */
    suspend fun importUriToEncryptedVault(
        uri: Uri,
        prefix: String = "doc"
    ): Result<ImportedAssetInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            var displayName = "imported_document"
            var queriedSize = 0L

            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) {
                        val rawName = cursor.getString(nameIdx)
                        if (!rawName.isNullOrBlank()) displayName = rawName
                    }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        queriedSize = cursor.getLong(sizeIdx)
                    }
                }
            }

            if (queriedSize > MAX_FILE_SIZE_BYTES) {
                throw IllegalArgumentException("File exceeds maximum allowed size of 50 MB.")
            }

            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Unable to read selected file from storage.")

            if (bytes.isEmpty()) {
                throw IllegalArgumentException("Selected file is empty (0 bytes).")
            }
            if (bytes.size > MAX_FILE_SIZE_BYTES) {
                throw IllegalArgumentException("File exceeds maximum allowed size of 50 MB.")
            }

            val rawExt = displayName.substringAfterLast(".", "").lowercase(Locale.ROOT)
            val resolvedMime = resolver.getType(uri)
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(rawExt)
                ?: inferMimeTypeFromHeaderOrExt(bytes, rawExt)

            val ext = if (rawExt.isNotBlank()) {
                rawExt
            } else {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(resolvedMime)
                    ?: inferExtensionFromMime(resolvedMime, bytes)
            }

            val isPdf = ext == "pdf" ||
                resolvedMime.contains("pdf", ignoreCase = true) ||
                (bytes.size >= 4 && String(bytes.copyOfRange(0, 4)) == "%PDF")

            val fileRef = cryptoStorage.saveEncryptedFile(
                "${prefix}_${System.currentTimeMillis()}",
                bytes
            )

            val pageCount = if (isPdf) countPdfPages(fileRef, ext) else 1
            val (suggestedCategory, suggestedSubtype) = suggestCategoryAndSubtype(displayName, ext, resolvedMime)
            val extractedMetadataFields = extractRealMetadataFields(displayName, ext, resolvedMime, bytes, pageCount)

            ImportedAssetInfo(
                fileRef = fileRef,
                originalName = displayName,
                fileExtension = if (isPdf) "pdf" else ext,
                mimeType = if (isPdf) "application/pdf" else resolvedMime,
                fileSize = bytes.size.toLong(),
                pageCount = pageCount,
                isPdf = isPdf,
                suggestedCategory = suggestedCategory,
                suggestedSubtype = suggestedSubtype,
                extractedFields = extractedMetadataFields
            )
        }
    }

    private fun countPdfPages(fileRef: String, ext: String): Int {
        return runCatching {
            val tempFile = cryptoStorage.writeDecryptedTempFileForPreviewOrShare(fileRef, ext.ifBlank { "pdf" })
                ?: return 1
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount.coerceAtLeast(1)
                }
            }
        }.getOrDefault(1)
    }

    /**
     * Automatically detects file types & filename keywords to suggest the best Category and Subtype.
     */
    fun suggestCategoryAndSubtype(
        fileName: String,
        ext: String,
        mimeType: String
    ): Pair<String, String> {
        val lower = fileName.lowercase(Locale.ROOT)
        return when {
            lower.contains("passport") -> DocumentCategoryGroup.PERSONAL.title to "Passport"
            lower.contains("citizenship") -> DocumentCategoryGroup.PERSONAL.title to "Citizenship / National ID"
            lower.contains("voter") -> DocumentCategoryGroup.PERSONAL.title to "Voter ID"
            lower.contains("birth") -> DocumentCategoryGroup.PERSONAL.title to "Birth Certificate"
            lower.contains("marriage") -> DocumentCategoryGroup.PERSONAL.title to "Marriage Certificate"
            lower.contains("national") || lower.contains("nid") -> DocumentCategoryGroup.PERSONAL.title to "National Identity Card"
            lower.contains("licen") && lower.contains("driv") -> DocumentCategoryGroup.PERSONAL.title to "Driving Licence"
            lower.contains("bluebook") -> DocumentCategoryGroup.VEHICLE.title to "Bluebook"
            lower.contains("vehicle") || lower.contains("registration") || lower.contains("pollution") -> DocumentCategoryGroup.VEHICLE.title to "Vehicle Registration"
            lower.contains("degree") || lower.contains("university") -> DocumentCategoryGroup.EDUCATION.title to "University Degrees"
            lower.contains("transcript") || lower.contains("mark") -> DocumentCategoryGroup.EDUCATION.title to "Transcripts"
            lower.contains("diploma") || lower.contains("certificate") || lower.contains("school") || lower.contains("college") -> DocumentCategoryGroup.EDUCATION.title to "Academic Certificates"
            lower.contains("bank") || lower.contains("statement") -> DocumentCategoryGroup.FINANCIAL.title to "Bank Statements"
            lower.contains("tax") || lower.contains("pan") -> DocumentCategoryGroup.FINANCIAL.title to "Tax Documents"
            lower.contains("invoice") -> DocumentCategoryGroup.FINANCIAL.title to "Invoices"
            lower.contains("receipt") -> DocumentCategoryGroup.FINANCIAL.title to "Receipts"
            lower.contains("medical") || lower.contains("lab") || lower.contains("prescription") || lower.contains("vaccin") -> DocumentCategoryGroup.MEDICAL.title to "Medical Reports"
            lower.contains("resume") || lower.contains("cv") -> DocumentCategoryGroup.EMPLOYMENT.title to "CV / Resume"
            lower.contains("salary") || lower.contains("payslip") -> DocumentCategoryGroup.EMPLOYMENT.title to "Salary Slips"
            lower.contains("appointment") || lower.contains("experience") -> DocumentCategoryGroup.EMPLOYMENT.title to "Experience Letters"
            lower.contains("property") || lower.contains("land") || lower.contains("rental") || lower.contains("affidavit") || lower.contains("agreement") || lower.contains("contract") -> DocumentCategoryGroup.LEGAL.title to "Agreements"
            lower.contains("flight") || lower.contains("boarding") || lower.contains("ticket") -> DocumentCategoryGroup.TRAVEL.title to "Flight Tickets"
            lower.contains("visa") || lower.contains("itinerary") || lower.contains("hotel") -> DocumentCategoryGroup.TRAVEL.title to "Visa Documents"
            ext in listOf("xls", "xlsx", "ods", "csv") -> DocumentCategoryGroup.FINANCIAL.title to "Bank Statements"
            ext in listOf("ppt", "pptx", "odp") -> DocumentCategoryGroup.EMPLOYMENT.title to "Business Documents"
            ext in listOf("doc", "docx", "odt", "rtf") -> DocumentCategoryGroup.LEGAL.title to "Agreements"
            else -> DocumentCategoryGroup.PERSONAL.title to "Personal Identification Documents"
        }
    }

    /**
     * Extracts real metadata and text key-value pairs from the imported file without any fake placeholders.
     */
    private fun extractRealMetadataFields(
        fileName: String,
        ext: String,
        mimeType: String,
        bytes: ByteArray,
        pageCount: Int
    ): List<DocumentFieldEntity> {
        val fields = mutableListOf<DocumentFieldEntity>()
        fields.add(
            DocumentFieldEntity(
                documentId = 0L,
                label = "Original File",
                value = fileName,
                source = FieldSource.MANUAL.name,
                userConfirmed = true,
                iconKey = "info",
                sortOrder = 1
            )
        )
        fields.add(
            DocumentFieldEntity(
                documentId = 0L,
                label = "Format",
                value = "${ext.uppercase(Locale.ROOT).ifBlank { "FILE" }} ($mimeType)",
                source = FieldSource.MANUAL.name,
                userConfirmed = true,
                iconKey = "info",
                sortOrder = 2
            )
        )

        if (mimeType.startsWith("image/")) {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                fields.add(
                    DocumentFieldEntity(
                        documentId = 0L,
                        label = "Resolution",
                        value = "${opts.outWidth} × ${opts.outHeight} px",
                        source = FieldSource.MANUAL.name,
                        userConfirmed = true,
                        iconKey = "info",
                        sortOrder = 3
                    )
                )
            }
        } else if (ext == "pdf" || mimeType.contains("pdf")) {
            fields.add(
                DocumentFieldEntity(
                    documentId = 0L,
                    label = "Page Count",
                    value = "$pageCount page(s)",
                    source = FieldSource.MANUAL.name,
                    userConfirmed = true,
                    iconKey = "info",
                    sortOrder = 3
                )
            )
        } else if (ext in listOf("txt", "md", "csv", "json", "xml", "rtf")) {
            val text = runCatching { String(bytes, Charsets.UTF_8) }.getOrDefault("")
            val parsedPairs = parseKeyValueLinesFromText(text)
            parsedPairs.take(8).forEachIndexed { index, (k, v) ->
                fields.add(
                    DocumentFieldEntity(
                        documentId = 0L,
                        label = k,
                        value = v,
                        source = FieldSource.OCR.name,
                        userConfirmed = false,
                        iconKey = "info",
                        sortOrder = fields.size + index + 1
                    )
                )
            }
        } else if (ext in listOf("docx", "xlsx", "pptx", "odt", "ods", "odp")) {
            val text = extractTextFromOfficeOrOdfZip(bytes, ext)
            val parsedPairs = parseKeyValueLinesFromText(text)
            parsedPairs.take(6).forEachIndexed { index, (k, v) ->
                fields.add(
                    DocumentFieldEntity(
                        documentId = 0L,
                        label = k,
                        value = v,
                        source = FieldSource.OCR.name,
                        userConfirmed = false,
                        iconKey = "info",
                        sortOrder = fields.size + index + 1
                    )
                )
            }
        }

        return fields
    }

    fun parseKeyValueLinesFromText(rawText: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            val idx = line.indexOfFirst { it == ':' || it == '=' }
            if (idx in 1..40 && idx < line.length - 1) {
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                if (key.isNotEmpty() && value.isNotEmpty() && value.length <= 140) {
                    result.add(key to value)
                }
            }
        }
        return result
    }

    /**
     * Reads and renders human-readable text preview for Universal File Formats:
     * - Plain text (.txt, .md, .rtf)
     * - CSV (.csv) formatted as readable rows
     * - JSON (.json) pretty-printed
     * - XML (.xml) formatted
     * - Office OpenXML (.docx, .xlsx, .pptx) & OpenDocument (.odt, .ods, .odp) by parsing their internal XML entries
     * - Legacy binary Office (.doc, .xls, .ppt) by extracting printable ASCII/UTF-8 runs
     */
    suspend fun readDocumentTextPreview(doc: DocumentEntity): String? = withContext(Dispatchers.IO) {
        if (doc.frontFileRef.isBlank()) return@withContext null
        val bytes = cryptoStorage.readDecryptedFile(doc.frontFileRef) ?: return@withContext null
        val ext = doc.fileExtension.lowercase(Locale.ROOT)

        runCatching {
            when (ext) {
                "txt", "md" -> String(bytes, Charsets.UTF_8).take(12_000)
                "rtf" -> stripRtfControlWords(String(bytes, Charsets.UTF_8)).take(12_000)
                "json" -> {
                    val raw = String(bytes, Charsets.UTF_8).trim()
                    if (raw.startsWith("{")) {
                        JSONObject(raw).toString(2).take(12_000)
                    } else if (raw.startsWith("[")) {
                        JSONArray(raw).toString(2).take(12_000)
                    } else {
                        raw.take(12_000)
                    }
                }
                "csv" -> {
                    val lines = String(bytes, Charsets.UTF_8).lines().filter { it.isNotBlank() }.take(100)
                    lines.mapIndexed { idx, line ->
                        "Row ${idx + 1}: ${line.split(",").joinToString("  |  ") { it.trim() }}"
                    }.joinToString("\n")
                }
                "xml" -> String(bytes, Charsets.UTF_8).take(12_000)
                "docx", "xlsx", "pptx", "odt", "ods", "odp" -> {
                    extractTextFromOfficeOrOdfZip(bytes, ext).ifBlank {
                        "Office/OpenDocument archive (${bytes.size} bytes). Use 'Open in External App' below for full layout formatting."
                    }
                }
                "doc", "xls", "ppt" -> {
                    extractPrintableRunsFromBinaryOffice(bytes).ifBlank {
                        "Binary Microsoft Office document (${bytes.size} bytes). Use 'Open in External App' below to view full document."
                    }
                }
                else -> null
            }
        }.getOrNull()
    }

    private fun extractTextFromOfficeOrOdfZip(bytes: ByteArray, ext: String): String {
        val sb = StringBuilder()
        runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase(Locale.ROOT)
                    val isContentXml = name == "word/document.xml" ||
                        name == "xl/sharedstrings.xml" ||
                        (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) ||
                        name == "content.xml"
                    if (!entry.isDirectory && isContentXml) {
                        val xmlBytes = zis.readBytes()
                        val extracted = extractTextNodesFromXml(String(xmlBytes, Charsets.UTF_8))
                        if (extracted.isNotBlank()) {
                            if (sb.isNotEmpty()) sb.append("\n\n")
                            sb.append(extracted)
                        }
                    }
                    zis.closeEntry()
                    if (sb.length > 10_000) break
                    entry = zis.nextEntry
                }
            }
        }
        return sb.toString().trim().take(12_000)
    }

    private fun extractTextNodesFromXml(xmlContent: String): String {
        val out = StringBuilder()
        runCatching {
            val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.TEXT -> {
                        val t = parser.text?.trim()
                        if (!t.isNullOrEmpty()) {
                            out.append(t).append(" ")
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tag = parser.name?.lowercase(Locale.ROOT) ?: ""
                        if (tag.endsWith("p") || tag.endsWith("row") || tag.endsWith("tr")) {
                            out.append("\n")
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        return out.toString().replace(Regex("\\n{3,}"), "\n\n").trim()
    }

    private fun stripRtfControlWords(rtf: String): String {
        return rtf
            .replace(Regex("\\\\[a-zA-Z]+-?\\d* ?"), "")
            .replace(Regex("[{}]"), "")
            .trim()
    }

    private fun extractPrintableRunsFromBinaryOffice(bytes: ByteArray): String {
        val raw = String(bytes, Charsets.ISO_8859_1)
        val matches = Regex("[\\x20-\\x7E]{6,}").findAll(raw)
            .map { it.value.trim() }
            .filter {
                it.isNotEmpty() &&
                    !it.startsWith("Root Entry") &&
                    !it.startsWith("Microsoft") &&
                    !it.startsWith("Normal.dot")
            }
            .take(80)
            .toList()
        return matches.joinToString("\n")
    }

    private fun inferMimeTypeFromHeaderOrExt(bytes: ByteArray, ext: String): String {
        if (bytes.size >= 4 && String(bytes.copyOfRange(0, 4)) == "%PDF") return "application/pdf"
        return when (ext.lowercase(Locale.ROOT)) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "odt" -> "application/vnd.oasis.opendocument.text"
            "ods" -> "application/vnd.oasis.opendocument.spreadsheet"
            "odp" -> "application/vnd.oasis.opendocument.presentation"
            "csv" -> "text/csv"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "txt", "md" -> "text/plain"
            "rtf" -> "application/rtf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            "bmp" -> "image/bmp"
            "tiff", "tif" -> "image/tiff"
            else -> "application/octet-stream"
        }
    }

    private fun inferExtensionFromMime(mimeType: String, bytes: ByteArray): String {
        if (bytes.size >= 4 && String(bytes.copyOfRange(0, 4)) == "%PDF") return "pdf"
        val m = mimeType.lowercase(Locale.ROOT)
        return when {
            m.contains("pdf") -> "pdf"
            m.contains("wordprocessingml") -> "docx"
            m.contains("msword") -> "doc"
            m.contains("spreadsheetml") -> "xlsx"
            m.contains("ms-excel") -> "xls"
            m.contains("presentationml") -> "pptx"
            m.contains("ms-powerpoint") -> "ppt"
            m.contains("opendocument.text") -> "odt"
            m.contains("opendocument.spreadsheet") -> "ods"
            m.contains("opendocument.presentation") -> "odp"
            m.contains("csv") -> "csv"
            m.contains("json") -> "json"
            m.contains("xml") -> "xml"
            m.contains("rtf") -> "rtf"
            m.startsWith("text/") -> "txt"
            m.contains("png") -> "png"
            m.contains("webp") -> "webp"
            m.contains("heic") -> "heic"
            m.contains("heif") -> "heif"
            m.contains("bmp") -> "bmp"
            m.contains("tiff") -> "tiff"
            m.startsWith("image/") -> "jpg"
            else -> "bin"
        }
    }

    suspend fun saveBitmapToEncryptedVault(
        bitmap: Bitmap,
        prefix: String = "scan",
        quality: Int = 92
    ): Result<ImportedAssetInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(50, 100), bos)
            val bytes = bos.toByteArray()
            val ts = System.currentTimeMillis()
            val fileRef = cryptoStorage.saveEncryptedFile("${prefix}_$ts", bytes)
            ImportedAssetInfo(
                fileRef = fileRef,
                originalName = "${prefix}_$ts.jpg",
                fileExtension = "jpg",
                mimeType = "image/jpeg",
                fileSize = bytes.size.toLong(),
                pageCount = 1,
                isPdf = false,
                suggestedCategory = DocumentCategoryGroup.PERSONAL.title,
                suggestedSubtype = "Personal Identification Documents",
                extractedFields = listOf(
                    DocumentFieldEntity(
                        documentId = 0L,
                        label = "Scan Resolution",
                        value = "${bitmap.width} × ${bitmap.height} px",
                        source = FieldSource.MANUAL.name,
                        userConfirmed = true,
                        iconKey = "info",
                        sortOrder = 1
                    ),
                    DocumentFieldEntity(
                        documentId = 0L,
                        label = "Captured At",
                        value = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts)),
                        source = FieldSource.MANUAL.name,
                        userConfirmed = true,
                        iconKey = "calendar",
                        sortOrder = 2
                    )
                )
            )
        }
    }

    suspend fun rotateEncryptedImage(fileRef: String, degrees: Float): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = cryptoStorage.readDecryptedFile(fileRef)
                    ?: throw IllegalArgumentException("Image file not found in vault.")
                val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw IllegalArgumentException("Cannot decode image.")
                val matrix = Matrix().apply { postRotate(degrees) }
                val rotated = Bitmap.createBitmap(
                    original, 0, 0, original.width, original.height, matrix, true
                )
                val out = ByteArrayOutputStream()
                rotated.compress(Bitmap.CompressFormat.JPEG, 92, out)
                cryptoStorage.saveEncryptedFile("rot_${System.currentTimeMillis()}", out.toByteArray())
            }
        }

    suspend fun exportVaultBackupJson(): File = withContext(Dispatchers.IO) {
        val docs = dao.getAllDocumentsOnce().filter { !it.isSamplePlaceholder }
        val root = JSONObject()
        root.put("app", "SAFEAREA")
        root.put("version", 2)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date()))
        val docsArray = JSONArray()
        for (doc in docs) {
            val fields = dao.getFieldsForDocument(doc.id)
            val docObj = JSONObject().apply {
                put("title", doc.title)
                put("category", doc.category)
                put("documentSubtype", doc.documentSubtype)
                put("description", doc.description)
                put("issuer", doc.issuer)
                put("country", doc.country)
                put("tagsCsv", doc.tagsCsv)
                put("issueDate", doc.issueDate)
                put("expiryDate", doc.expiryDate)
                put("favourite", doc.favourite)
                put("archived", doc.archived)
                put("notes", doc.notes)
                put("originalFileName", doc.originalFileName)
                put("fileExtension", doc.fileExtension)
                put("mimeType", doc.mimeType)
                put("fileSize", doc.fileSize)
                val fieldsArr = JSONArray()
                for (f in fields) {
                    fieldsArr.put(
                        JSONObject().apply {
                            put("label", f.label)
                            put("value", f.value)
                            put("isSensitive", f.isSensitive)
                            put("isMaskedByDefault", f.isMaskedByDefault)
                            put("iconKey", f.iconKey)
                        }
                    )
                }
                put("fields", fieldsArr)
            }
            docsArray.put(docObj)
        }
        root.put("documents", docsArray)

        val outDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val backupFile = File(outDir, "safearea_vault_backup.json")
        backupFile.writeText(root.toString(2))
        logAudit("BACKUP_EXPORTED", "Exported ${docs.size} documents metadata backup")
        backupFile
    }

    suspend fun wipeAllData() = withContext(Dispatchers.IO) {
        dao.deleteAllFields()
        dao.deleteAllDocuments()
        dao.clearAuditEvents()
        cryptoStorage.wipeAllVaultFilesAndSettings()
    }

    suspend fun logAudit(type: String, summary: String) = withContext(Dispatchers.IO) {
        dao.insertAuditEvent(AuditEventEntity(eventType = type, summary = summary))
    }
}

data class ImportedAssetInfo(
    val fileRef: String,
    val originalName: String,
    val fileExtension: String,
    val mimeType: String,
    val fileSize: Long,
    val pageCount: Int,
    val isPdf: Boolean,
    val suggestedCategory: String,
    val suggestedSubtype: String,
    val extractedFields: List<DocumentFieldEntity>
)
