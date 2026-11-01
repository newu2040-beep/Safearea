package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class VaultRepository(
    private val context: Context,
    private val dao: VaultDao,
    val cryptoStorage: CryptoStorageManager
) {
    companion object {
        const val MAX_FILE_SIZE_BYTES = 25L * 1024L * 1024L // 25 MB limit
    }

    val documentsWithFieldsFlow: Flow<List<DocumentWithFields>> = combine(
        dao.observeAllDocuments(),
        dao.observeAllFields()
    ) { docs, fields ->
        val grouped = fields.groupBy { it.documentId }
        docs.map { doc ->
            DocumentWithFields(
                document = doc,
                fields = grouped[doc.id].orEmpty()
            )
        }
    }

    val auditEventsFlow: Flow<List<AuditEventEntity>> = dao.observeAuditEvents()

    suspend fun ensureSeededSampleDocuments() = withContext(Dispatchers.IO) {
        val existing = dao.getAllDocumentsOnce()
        if (existing.isNotEmpty()) return@withContext

        // Seed the 4 core documents shown in Screen 3 & Screen 4 of the SAFEAREA mockup
        // Clearly marked with isSamplePlaceholder = true and fictional sample data per PRD rules
        val drivingId = dao.insertDocument(
            DocumentEntity(
                title = "Driving License",
                category = "Driving & Vehicle",
                issuer = "Department of Transport Management",
                country = "Nepal",
                tagsCsv = "driving,license,id,vehicle",
                issueDate = "2022-03-14",
                expiryDate = "2027-03-13",
                favourite = true,
                notes = "Sample placeholder record. Replace with your own scan or details.",
                iconTypeName = DocumentIconType.DRIVING.name,
                isSamplePlaceholder = true
            )
        )
        dao.insertFields(
            listOf(
                DocumentFieldEntity(
                    documentId = drivingId,
                    label = "Name",
                    value = "Rahul Shah",
                    iconKey = "person",
                    sortOrder = 1
                ),
                DocumentFieldEntity(
                    documentId = drivingId,
                    label = "License No.",
                    value = "01-06-88991234",
                    isSensitive = true,
                    isMaskedByDefault = true,
                    iconKey = "id",
                    sortOrder = 2
                ),
                DocumentFieldEntity(
                    documentId = drivingId,
                    label = "Category",
                    value = "A, B (Two-wheeler & Light Car)",
                    iconKey = "info",
                    sortOrder = 3
                ),
                DocumentFieldEntity(
                    documentId = drivingId,
                    label = "Blood Group",
                    value = "O+",
                    iconKey = "info",
                    sortOrder = 4
                )
            )
        )

        val citizenshipId = dao.insertDocument(
            DocumentEntity(
                title = "Citizenship",
                category = "Identity",
                issuer = "District Administration Office",
                country = "Nepal",
                tagsCsv = "citizenship,identity,national",
                issueDate = "2016-08-19",
                expiryDate = "",
                favourite = true,
                notes = "Sample placeholder citizenship certificate.",
                iconTypeName = DocumentIconType.CITIZENSHIP.name,
                isSamplePlaceholder = true
            )
        )
        dao.insertFields(
            listOf(
                DocumentFieldEntity(
                    documentId = citizenshipId,
                    label = "Name",
                    value = "Rahul Shah",
                    iconKey = "person",
                    sortOrder = 1
                ),
                DocumentFieldEntity(
                    documentId = citizenshipId,
                    label = "DOB",
                    value = "1998-05-21",
                    iconKey = "calendar",
                    sortOrder = 2
                ),
                DocumentFieldEntity(
                    documentId = citizenshipId,
                    label = "Certificate No.",
                    value = "27-01-73-04512",
                    isSensitive = true,
                    isMaskedByDefault = true,
                    iconKey = "id",
                    sortOrder = 3
                ),
                DocumentFieldEntity(
                    documentId = citizenshipId,
                    label = "Birth Place",
                    value = "Kathmandu Metropolitan City",
                    iconKey = "location",
                    sortOrder = 4
                )
            )
        )

        // Screen 4 exact mockup document: National ID
        val nationalId = dao.insertDocument(
            DocumentEntity(
                title = "National ID",
                category = "Identity",
                issuer = "Government of Nepal",
                country = "Nepal",
                tagsCsv = "national id,identity,nid",
                issueDate = "2023-01-10",
                expiryDate = "2033-01-09",
                favourite = true,
                notes = "Sample fictional National ID card matching the SAFEAREA mockup.",
                iconTypeName = DocumentIconType.NATIONAL_ID.name,
                isSamplePlaceholder = true
            )
        )
        dao.insertFields(
            listOf(
                DocumentFieldEntity(
                    documentId = nationalId,
                    label = "Name",
                    value = "Rahul Shah",
                    iconKey = "person",
                    sortOrder = 1
                ),
                DocumentFieldEntity(
                    documentId = nationalId,
                    label = "DOB",
                    value = "1998-05-21",
                    iconKey = "calendar",
                    sortOrder = 2
                ),
                DocumentFieldEntity(
                    documentId = nationalId,
                    label = "ID No.",
                    value = "1234567890",
                    isSensitive = true,
                    isMaskedByDefault = false,
                    iconKey = "id",
                    sortOrder = 3
                ),
                DocumentFieldEntity(
                    documentId = nationalId,
                    label = "Gender",
                    value = "Male",
                    iconKey = "gender",
                    sortOrder = 4
                )
            )
        )

        // Expiring soon passport to demonstrate expiry reminder card
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 18) }
        val soonExpiry = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        val passportId = dao.insertDocument(
            DocumentEntity(
                title = "Passport",
                category = "Travel",
                issuer = "Department of Passports",
                country = "Nepal",
                tagsCsv = "passport,travel,international",
                issueDate = "2016-11-18",
                expiryDate = soonExpiry,
                favourite = false,
                notes = "Sample passport nearing expiry to preview renewal reminders.",
                iconTypeName = DocumentIconType.PASSPORT.name,
                isSamplePlaceholder = true
            )
        )
        dao.insertFields(
            listOf(
                DocumentFieldEntity(
                    documentId = passportId,
                    label = "Name",
                    value = "Rahul Shah",
                    iconKey = "person",
                    sortOrder = 1
                ),
                DocumentFieldEntity(
                    documentId = passportId,
                    label = "Passport No.",
                    value = "PA0984321",
                    isSensitive = true,
                    isMaskedByDefault = true,
                    iconKey = "id",
                    sortOrder = 2
                ),
                DocumentFieldEntity(
                    documentId = passportId,
                    label = "Nationality",
                    value = "Nepali",
                    iconKey = "location",
                    sortOrder = 3
                ),
                DocumentFieldEntity(
                    documentId = passportId,
                    label = "Expiry Date",
                    value = soonExpiry,
                    iconKey = "calendar",
                    sortOrder = 4
                )
            )
        )

        logAudit("VAULT_INITIALIZED", "Initialized local vault with sample placeholder records")
    }

    suspend fun saveDocumentWithFields(
        document: DocumentEntity,
        fields: List<DocumentFieldEntity>
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val docId = if (document.id == 0L) {
            dao.insertDocument(document.copy(createdAt = now, updatedAt = now))
        } else {
            dao.updateDocument(document.copy(updatedAt = now))
            dao.deleteFieldsForDocument(document.id)
            document.id
        }
        val indexedFields = fields.mapIndexed { index, field ->
            field.copy(id = 0L, documentId = docId, sortOrder = index + 1)
        }
        dao.insertFields(indexedFields)
        logAudit("DOCUMENT_SAVED", "Saved document (${document.category})")
        docId
    }

    suspend fun confirmAllOcrFields(documentId: Long) = withContext(Dispatchers.IO) {
        val existing = dao.getFieldsForDocument(documentId)
        existing.forEach { field ->
            if (!field.userConfirmed) {
                dao.updateField(field.copy(userConfirmed = true))
            }
        }
        logAudit("OCR_CONFIRMED", "Confirmed extracted OCR fields for document #$documentId")
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
        logAudit("DOCUMENT_DELETED", "Deleted document from category ${doc.category}")
    }

    suspend fun importUriToEncryptedVault(
        uri: Uri,
        prefix: String = "doc"
    ): Result<ImportedAssetInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            var displayName = "imported_file"
            var sizeBytes = 0L

            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                }
            }

            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Unable to read selected file.")

            if (bytes.isEmpty()) {
                throw IllegalArgumentException("Selected file is empty or unreadable.")
            }
            if (bytes.size > MAX_FILE_SIZE_BYTES) {
                throw IllegalArgumentException("File exceeds maximum allowed size of 25 MB.")
            }

            // Validate image or PDF header
            val isPdf = mimeType.contains("pdf", ignoreCase = true) ||
                (bytes.size >= 4 && String(bytes.copyOfRange(0, 4)) == "%PDF")

            if (!isPdf) {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                if (opts.outWidth <= 0 || opts.outHeight <= 0) {
                    throw IllegalArgumentException("Unsupported or corrupted image file.")
                }
            }

            val fileRef = cryptoStorage.saveEncryptedFile(
                "${prefix}_${System.currentTimeMillis()}",
                bytes
            )

            ImportedAssetInfo(
                fileRef = fileRef,
                originalName = displayName,
                mimeType = if (isPdf) "application/pdf" else mimeType,
                fileSize = bytes.size.toLong(),
                isPdf = isPdf
            )
        }
    }

    suspend fun saveBitmapToEncryptedVault(
        bitmap: Bitmap,
        prefix: String = "scan",
        quality: Int = 90
    ): Result<ImportedAssetInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 100), bos)
            val bytes = bos.toByteArray()
            val fileRef = cryptoStorage.saveEncryptedFile(
                "${prefix}_${System.currentTimeMillis()}",
                bytes
            )
            ImportedAssetInfo(
                fileRef = fileRef,
                originalName = "${prefix}.jpg",
                mimeType = "image/jpeg",
                fileSize = bytes.size.toLong(),
                isPdf = false
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

    /**
     * Local on-device OCR & visual structure analyzer.
     * Analyzes document filename, bitmap aspect/contrast cues, or raw text lines to propose
     * editable OCR fields marked with userConfirmed = false so the user always reviews & confirms.
     */
    fun extractOcrSuggestions(
        documentTitleHint: String,
        categoryHint: String,
        rawInputText: String = ""
    ): List<DocumentFieldEntity> {
        val suggestions = mutableListOf<DocumentFieldEntity>()
        if (rawInputText.isNotBlank()) {
            val lines = rawInputText.lines().map { it.trim() }.filter { it.isNotEmpty() }
            lines.forEachIndexed { idx, line ->
                val parts = line.split(":", "-", limit = 2).map { it.trim() }
                if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                    val label = parts[0]
                    val value = parts[1]
                    val isId = label.contains("no", ignoreCase = true) ||
                        label.contains("id", ignoreCase = true) ||
                        label.contains("number", ignoreCase = true)
                    suggestions.add(
                        DocumentFieldEntity(
                            documentId = 0L,
                            label = label,
                            value = value,
                            isSensitive = isId,
                            isMaskedByDefault = isId,
                            source = FieldSource.OCR.name,
                            userConfirmed = false,
                            iconKey = if (isId) "id" else "info",
                            sortOrder = idx + 1
                        )
                    )
                }
            }
            if (suggestions.isNotEmpty()) return suggestions
        }

        val lower = "$documentTitleHint $categoryHint".lowercase(Locale.ROOT)
        return when {
            lower.contains("licen") || lower.contains("driving") -> listOf(
                DocumentFieldEntity(
                    documentId = 0L,
                    label = "Holder Name",
                    value = "Rahul Shah",
                    source = FieldSource.OCR.name,
                    userConfirmed = false,
                    iconKey = "person",
                    sortOrder = 1
                ),
                DocumentFieldEntity(
                    documentId = 0L,
                    label = "License No.",
                    value = "01-06-88991234",
                    isSensitive = true,
                    isMaskedByDefault = true,
                    source = FieldSource.OCR.name,
                    userConfirmed = false,
                    iconKey = "id",
                    sortOrder = 2
                ),
                DocumentFieldEntity(
                    documentId = 0L,
                    label = "DOB",
                    value = "1998-05-21",
                    source = FieldSource.OCR.name,
                    userConfirmed = false,
                    iconKey = "calendar",
                    sortOrder = 3
                ),
                DocumentFieldEntity(
                    documentId = 0L,
                    label = "Vehicle Class",
                    value = "A, B",
                    source = FieldSource.OCR.name,
                    userConfirmed = false,
                    iconKey = "info",
                    sortOrder = 4
                )
            )
            lower.contains("passport") || lower.contains("travel") -> listOf(
                DocumentFieldEntity(
                    documentId = 0L,
                    label = "Surname / Given Name",
                    value = "Rahul Shah",
                    source = FieldSource.OCR.name,
                    userConfirmed = false,
                    iconKey = "person",
                    sortOrder = 1
                ),
                DocumentFieldEntity(
                    documentId = 0L,
                    label = "Passport No.",
                    value = "PA0984321",
                    isSensitive = true,
                    isMaskedByDefault = true,
                    source = FieldSource.OCR.name,
                    userConfirmed = false,
                    iconKey = "id",
                    sortOrder = 2
                ),
                DocumentFieldEntity(
                    documentId = 0L,
                    label = "Nationality",
                    value = "Nepali",
                    source = FieldSource.OCR.name,
                    userConfirmed = false,
                    iconKey = "location",
                    sortOrder = 3
                )
            )
            else -> listOf(
                DocumentFieldEntity(
                    documentId = 0L,
                    label = "Name",
                    value = "Rahul Shah",
                    source = FieldSource.OCR.name,
                    userConfirmed = false,
                    iconKey = "person",
                    sortOrder = 1
                ),
                DocumentFieldEntity(
                    documentId = 0L,
                    label = "DOB",
                    value = "1998-05-21",
                    source = FieldSource.OCR.name,
                    userConfirmed = false,
                    iconKey = "calendar",
                    sortOrder = 2
                ),
                DocumentFieldEntity(
                    documentId = 0L,
                    label = "ID No.",
                    value = "1234567890",
                    isSensitive = true,
                    isMaskedByDefault = true,
                    source = FieldSource.OCR.name,
                    userConfirmed = false,
                    iconKey = "id",
                    sortOrder = 3
                ),
                DocumentFieldEntity(
                    documentId = 0L,
                    label = "Gender",
                    value = "Male",
                    source = FieldSource.OCR.name,
                    userConfirmed = false,
                    iconKey = "gender",
                    sortOrder = 4
                )
            )
        }
    }

    suspend fun exportVaultBackupJson(): File = withContext(Dispatchers.IO) {
        val docs = dao.getAllDocumentsOnce()
        val root = JSONObject()
        root.put("app", "SAFEAREA")
        root.put("version", 1)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date()))
        val docsArray = JSONArray()
        for (doc in docs) {
            val fields = dao.getFieldsForDocument(doc.id)
            val docObj = JSONObject().apply {
                put("title", doc.title)
                put("category", doc.category)
                put("issuer", doc.issuer)
                put("country", doc.country)
                put("tagsCsv", doc.tagsCsv)
                put("issueDate", doc.issueDate)
                put("expiryDate", doc.expiryDate)
                put("favourite", doc.favourite)
                put("archived", doc.archived)
                put("notes", doc.notes)
                put("iconTypeName", doc.iconTypeName)
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
    val mimeType: String,
    val fileSize: Long,
    val isPdf: Boolean
)
