package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LockMethod(val title: String, val subtitle: String) {
    PIN("PIN", "Enter a 4–6 digit PIN"),
    PATTERN("Pattern", "Draw a pattern"),
    FACE_ID("Face ID", "Use your face"),
    FINGERPRINT("Fingerprint", "Use your fingerprint")
}

enum class FieldSource {
    MANUAL,
    OCR
}

enum class DocumentIconType(val label: String, val badgeHex: Long, val iconTintHex: Long) {
    DRIVING("Driving & Vehicle", 0xFFD9E6F7, 0xFF28559A),
    CITIZENSHIP("Citizenship", 0xFFDCECE2, 0xFF3A7D5C),
    NATIONAL_ID("Identity", 0xFFE6DDF5, 0xFF563D82),
    PASSPORT("Travel", 0xFFFADBD8, 0xFFB8332A),
    EDUCATION("Education", 0xFFFEF0D1, 0xFF9C6B19),
    FINANCE("Finance", 0xFFD8EFEF, 0xFF236E70),
    HEALTH("Health", 0xFFFCE0E6, 0xFFA83253),
    ADDRESS("Home & Address", 0xFFE8E4D9, 0xFF5C5240),
    WORK("Work", 0xFFE1E6F0, 0xFF364968),
    OTHER("Other", 0xFFEAE6DF, 0xFF4A453E)
}

val DEFAULT_CATEGORIES = listOf(
    "Identity",
    "Driving & Vehicle",
    "Travel",
    "Education",
    "Finance",
    "Health",
    "Home & Address",
    "Work",
    "Other"
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val issuer: String,
    val country: String,
    val tagsCsv: String = "",
    val issueDate: String = "",
    val expiryDate: String = "", // YYYY-MM-DD or empty
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val favourite: Boolean = false,
    val archived: Boolean = false,
    val notes: String = "",
    val frontFileRef: String = "", // relative path inside encrypted vault dir
    val backFileRef: String = "",  // second side or second page
    val mimeType: String = "image/jpeg",
    val fileSize: Long = 0L,
    val pageCount: Int = 1,
    val iconTypeName: String = DocumentIconType.NATIONAL_ID.name,
    val isSamplePlaceholder: Boolean = false,
    val reminderLeadDays: Int = 30,
    val reminderEnabled: Boolean = true
) {
    val tags: List<String>
        get() = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val iconType: DocumentIconType
        get() = runCatching { DocumentIconType.valueOf(iconTypeName) }
            .getOrDefault(DocumentIconType.OTHER)
}

@Entity(tableName = "document_fields")
data class DocumentFieldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val label: String,
    val value: String,
    val isSensitive: Boolean = false,
    val isMaskedByDefault: Boolean = false,
    val source: String = FieldSource.MANUAL.name, // MANUAL or OCR
    val userConfirmed: Boolean = true,
    val iconKey: String = "person", // person, calendar, id, gender, location, info
    val sortOrder: Int = 0
)

@Entity(tableName = "audit_events")
data class AuditEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DocumentWithFields(
    val document: DocumentEntity,
    val fields: List<DocumentFieldEntity>
)
