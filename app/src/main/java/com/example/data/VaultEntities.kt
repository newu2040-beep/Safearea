package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

enum class LockMethod(val title: String, val subtitle: String) {
    PIN("PIN", "4–6 digit security code"),
    PATTERN("Pattern", "Connect at least 4 nodes"),
    FACE_ID("Face Authentication", "System biometric verification"),
    FINGERPRINT("Fingerprint", "System fingerprint sensor")
}

enum class FieldSource {
    MANUAL,
    OCR
}

enum class ExportFormat(
    val label: String,
    val badge: String,
    val extension: String,
    val mimeType: String,
    val description: String
) {
    PDF(
        label = "PDF Report (.pdf)",
        badge = "PDF",
        extension = "pdf",
        mimeType = "application/pdf",
        description = "Printable multi-page PDF report of your documents & fields"
    ),
    CSV(
        label = "CSV Spreadsheet (.csv)",
        badge = "CSV",
        extension = "csv",
        mimeType = "text/csv",
        description = "Spreadsheet compatible with Excel, Google Sheets & Numbers"
    ),
    TXT(
        label = "Plain Text (.txt)",
        badge = "TXT",
        extension = "txt",
        mimeType = "text/plain",
        description = "Clean, human-readable text report of all vault records"
    ),
    MARKDOWN(
        label = "Markdown (.md)",
        badge = "MD",
        extension = "md",
        mimeType = "text/markdown",
        description = "Formatted Markdown tables & sections for Notion / Obsidian"
    ),
    HTML(
        label = "HTML Web Report (.html)",
        badge = "HTML",
        extension = "html",
        mimeType = "text/html",
        description = "Styled standalone HTML document viewable in any browser"
    ),
    JSON(
        label = "JSON Backup (.json)",
        badge = "JSON",
        extension = "json",
        mimeType = "application/json",
        description = "Structured machine-readable JSON vault archive"
    ),
    XML(
        label = "XML Archive (.xml)",
        badge = "XML",
        extension = "xml",
        mimeType = "application/xml",
        description = "Standard XML data interchange export of all documents"
    )
}

data class EmergencyIceProfile(
    val fullName: String = "",
    val bloodGroup: String = "",
    val allergies: String = "",
    val emergencyContact: String = "",
    val medicalConditions: String = "",
    val insurancePolicyNo: String = ""
) {
    val isConfigured: Boolean
        get() = fullName.isNotBlank() || bloodGroup.isNotBlank() || emergencyContact.isNotBlank() || allergies.isNotBlank()
}

enum class DocumentCategoryGroup(
    val title: String,
    val shortTitle: String,
    val badgeHex: Long,
    val iconTintHex: Long,
    val subtypes: List<String>
) {
    PERSONAL(
        title = "Personal Documents",
        shortTitle = "Personal",
        badgeHex = 0xFFE6DDF5,
        iconTintHex = 0xFF563D82,
        subtypes = listOf(
            "Citizenship / National ID",
            "Passport",
            "Driving Licence",
            "Birth Certificate",
            "Marriage Certificate",
            "National Identity Card",
            "Personal Identification Documents",
            "Voter ID",
            "Social Security / PAN Card",
            "Family Relationship Certificate"
        )
    ),
    EDUCATION(
        title = "Education Documents",
        shortTitle = "Education",
        badgeHex = 0xFFFEF0D1,
        iconTintHex = 0xFF9C6B19,
        subtypes = listOf(
            "Academic Certificates",
            "School Documents",
            "College Documents",
            "University Degrees",
            "Transcripts",
            "Mark Sheets",
            "Training Certificates",
            "Diplomas",
            "Student ID Card",
            "Scholarship Letters"
        )
    ),
    FINANCIAL(
        title = "Financial Documents",
        shortTitle = "Financial",
        badgeHex = 0xFFD8EFEF,
        iconTintHex = 0xFF236E70,
        subtypes = listOf(
            "Bank Statements",
            "Bank Passbooks",
            "Tax Documents",
            "Invoices",
            "Receipts",
            "Payment Records",
            "Insurance Documents",
            "Financial Agreements",
            "Loan & Mortgage Records",
            "Investment & Portfolio Statements"
        )
    ),
    TAX_ACCOUNTING(
        title = "Tax & Accounting",
        shortTitle = "Tax & Audit",
        badgeHex = 0xFFE3F2DE,
        iconTintHex = 0xFF2E6B34,
        subtypes = listOf(
            "Income Tax Returns",
            "PAN / Tax Registration Card",
            "VAT / GST Filings",
            "Tax Clearance Certificate",
            "TDS / Withholding Certificates",
            "Audit Reports",
            "Charitable Donation Receipts"
        )
    ),
    MEDICAL(
        title = "Medical Documents",
        shortTitle = "Medical",
        badgeHex = 0xFFFCE0E6,
        iconTintHex = 0xFFA83253,
        subtypes = listOf(
            "Medical Reports",
            "Prescriptions",
            "Laboratory Reports",
            "Vaccination Records",
            "Health Insurance Cards",
            "Medical Certificates",
            "Discharge Summary",
            "Dental & Optical Records",
            "Blood Group & Allergy Card"
        )
    ),
    INSURANCE(
        title = "Insurance Policies",
        shortTitle = "Insurance",
        badgeHex = 0xFFDEF0FA,
        iconTintHex = 0xFF1F638F,
        subtypes = listOf(
            "Life Insurance Policy",
            "Health / Medical Insurance",
            "Term Insurance Certificate",
            "Home & Property Insurance",
            "Motor / Auto Insurance",
            "Travel Insurance Policy",
            "Insurance Claim Records"
        )
    ),
    EMPLOYMENT(
        title = "Employment & Professional",
        shortTitle = "Employment",
        badgeHex = 0xFFE1E6F0,
        iconTintHex = 0xFF364968,
        subtypes = listOf(
            "CV / Resume",
            "Employment Contracts",
            "Experience Letters",
            "Salary Slips",
            "Professional Certificates",
            "Appointment Letters",
            "Business Documents",
            "Employee ID Badge",
            "Relieving & Reference Letters"
        )
    ),
    BUSINESS(
        title = "Business & Corporate",
        shortTitle = "Business",
        badgeHex = 0xFFEDE2F8,
        iconTintHex = 0xFF5D348C,
        subtypes = listOf(
            "Company Registration Certificate",
            "Trade / Business Licence",
            "Articles of Incorporation",
            "Partnership Deed",
            "Board Resolutions",
            "Vendor & Client Contracts",
            "Trademark & IP Certificates"
        )
    ),
    LEGAL(
        title = "Legal Documents",
        shortTitle = "Legal",
        badgeHex = 0xFFE8E4D9,
        iconTintHex = 0xFF5C5240,
        subtypes = listOf(
            "Agreements",
            "Contracts",
            "Property Documents",
            "Land Ownership Certificates",
            "Rental Agreements",
            "Affidavits",
            "Legal Notices",
            "Power of Attorney",
            "Will & Estate Documents",
            "Court Orders & Judgments"
        )
    ),
    PROPERTY(
        title = "Property & Real Estate",
        shortTitle = "Property",
        badgeHex = 0xFFF5E6D6,
        iconTintHex = 0xFF8C531E,
        subtypes = listOf(
            "Land Ownership Deed (Lalpurja)",
            "House Sale / Purchase Deed",
            "Lease / Tenancy Agreement",
            "Property Tax Receipts",
            "Building Blueprint & Permit",
            "Home Valuation Report",
            "Society / HOA Certificates"
        )
    ),
    TRAVEL(
        title = "Travel Documents",
        shortTitle = "Travel",
        badgeHex = 0xFFFADBD8,
        iconTintHex = 0xFFB8332A,
        subtypes = listOf(
            "Flight Tickets",
            "Boarding Passes",
            "Hotel Reservations",
            "Visa Documents",
            "Travel Insurance",
            "Travel Itineraries",
            "International Driving Permit",
            "Rail & Bus Passes",
            "Foreign Exchange Receipts"
        )
    ),
    VEHICLE(
        title = "Vehicle Documents",
        shortTitle = "Vehicle",
        badgeHex = 0xFFD9E6F7,
        iconTintHex = 0xFF28559A,
        subtypes = listOf(
            "Vehicle Registration",
            "Vehicle Insurance",
            "Bluebook",
            "Driving Permits",
            "Pollution Certificates",
            "Vehicle Tax Records",
            "Service & Maintenance History",
            "Roadside Assistance Card"
        )
    ),
    HOME_UTILITIES(
        title = "Home, Utilities & Warranties",
        shortTitle = "Home & Bills",
        badgeHex = 0xFFFFE9D6,
        iconTintHex = 0xFFA64D14,
        subtypes = listOf(
            "Electricity & Water Bills",
            "Internet & Telecom Bills",
            "Appliance Warranty Cards",
            "Electronics Purchase Invoices",
            "Gas Connection Record",
            "Home Repair & AMC Contracts"
        )
    ),
    DIGITAL_LICENSES(
        title = "Digital & Memberships",
        shortTitle = "Digital & IDs",
        badgeHex = 0xFFDDF3F5,
        iconTintHex = 0xFF176A73,
        subtypes = listOf(
            "Software Licence Keys",
            "Domain & Hosting Records",
            "Professional Memberships",
            "Library & Club Cards",
            "Loyalty & Frequent Flyer Cards",
            "Event & Conference Passes",
            "Recovery Backup Codes"
        )
    ),
    OTHER(
        title = "Other Documents",
        shortTitle = "Other",
        badgeHex = 0xFFDCECE2,
        iconTintHex = 0xFF3A7D5C,
        subtypes = listOf(
            "Utility Bills",
            "Warranty Cards",
            "Membership Cards",
            "ID Cards",
            "Notes",
            "Letters",
            "Custom Documents"
        )
    );

    companion object {
        fun fromCategoryName(category: String): DocumentCategoryGroup {
            val lower = category.lowercase(Locale.ROOT)
            return entries.find {
                it.title.equals(category, ignoreCase = true) ||
                    it.shortTitle.equals(category, ignoreCase = true) ||
                    it.subtypes.any { sub -> sub.equals(category, ignoreCase = true) }
            } ?: when {
                lower.contains("personal") || lower.contains("identity") || lower.contains("citizen") || lower.contains("voter") -> PERSONAL
                lower.contains("educat") || lower.contains("school") || lower.contains("college") || lower.contains("degree") || lower.contains("diploma") -> EDUCATION
                lower.contains("tax") || lower.contains("audit") || lower.contains("vat") || lower.contains("gst") -> TAX_ACCOUNTING
                lower.contains("financ") || lower.contains("bank") || lower.contains("invoice") || lower.contains("receipt") -> FINANCIAL
                lower.contains("insur") || lower.contains("policy") -> INSURANCE
                lower.contains("medic") || lower.contains("health") || lower.contains("prescript") || lower.contains("vaccin") -> MEDICAL
                lower.contains("business") || lower.contains("corporat") || lower.contains("company") || lower.contains("trade") -> BUSINESS
                lower.contains("employ") || lower.contains("work") || lower.contains("resume") || lower.contains("salary") -> EMPLOYMENT
                lower.contains("property") || lower.contains("real estate") || lower.contains("lalpurja") || lower.contains("lease") -> PROPERTY
                lower.contains("legal") || lower.contains("land") || lower.contains("contract") || lower.contains("affidavit") -> LEGAL
                lower.contains("travel") || lower.contains("flight") || lower.contains("visa") || lower.contains("hotel") || lower.contains("boarding") -> TRAVEL
                lower.contains("vehicle") || lower.contains("driv") || lower.contains("bluebook") || lower.contains("car") -> VEHICLE
                lower.contains("home") || lower.contains("utilit") || lower.contains("warrant") || lower.contains("bill") -> HOME_UTILITIES
                lower.contains("digital") || lower.contains("software") || lower.contains("member") -> DIGITAL_LICENSES
                else -> OTHER
            }
        }
    }
}

enum class SupportedFileFormatGroup(
    val label: String,
    val extensions: List<String>
) {
    ALL("All Formats", emptyList()),
    PDF("PDF (.pdf)", listOf("pdf")),
    IMAGE("Images", listOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "tiff", "tif")),
    WORD("Word / Docs", listOf("doc", "docx", "odt", "rtf")),
    SPREADSHEET("Excel / Sheets", listOf("xls", "xlsx", "ods", "csv")),
    PRESENTATION("PowerPoint / Slides", listOf("ppt", "pptx", "odp")),
    TEXT_DATA("Text / JSON / XML", listOf("txt", "md", "json", "xml"));

    companion object {
        val ALL_SUPPORTED_EXTENSIONS = listOf(
            "pdf",
            "doc", "docx",
            "xls", "xlsx",
            "ppt", "pptx",
            "jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "tiff", "tif",
            "txt", "rtf", "md",
            "odt", "ods", "odp",
            "csv", "json", "xml"
        )

        fun detectGroup(extension: String, mimeType: String): SupportedFileFormatGroup {
            val ext = extension.lowercase(Locale.ROOT).removePrefix(".")
            for (group in entries) {
                if (group != ALL && ext in group.extensions) return group
            }
            val mime = mimeType.lowercase(Locale.ROOT)
            return when {
                mime.contains("pdf") -> PDF
                mime.startsWith("image/") -> IMAGE
                mime.contains("word") || mime.contains("opendocument.text") || mime.contains("rtf") -> WORD
                mime.contains("excel") || mime.contains("spreadsheet") || mime.contains("csv") -> SPREADSHEET
                mime.contains("powerpoint") || mime.contains("presentation") -> PRESENTATION
                mime.startsWith("text/") || mime.contains("json") || mime.contains("xml") -> TEXT_DATA
                else -> ALL
            }
        }
    }
}

val DEFAULT_CATEGORIES: List<String> = DocumentCategoryGroup.entries.map { it.title }

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // e.g., "Personal Documents"
    val documentSubtype: String = "", // e.g., "Passport", "Bank Statements"
    val description: String = "",
    val issuer: String = "",
    val country: String = "",
    val tagsCsv: String = "",
    val issueDate: String = "",
    val expiryDate: String = "", // YYYY-MM-DD or empty
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = 0L,
    val favourite: Boolean = false,
    val archived: Boolean = false,
    val notes: String = "",
    val frontFileRef: String = "",
    val backFileRef: String = "",
    val originalFileName: String = "",
    val fileExtension: String = "",
    val mimeType: String = "application/octet-stream",
    val fileSize: Long = 0L,
    val pageCount: Int = 1,
    val iconTypeName: String = DocumentCategoryGroup.PERSONAL.name,
    val isSamplePlaceholder: Boolean = false,
    val reminderLeadDays: Int = 30,
    val reminderEnabled: Boolean = true
) {
    val tags: List<String>
        get() = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val categoryGroup: DocumentCategoryGroup
        get() = DocumentCategoryGroup.fromCategoryName(category)

    val formatGroup: SupportedFileFormatGroup
        get() = SupportedFileFormatGroup.detectGroup(fileExtension, mimeType)

    val displayFormatBadge: String
        get() = when {
            fileExtension.isNotBlank() -> fileExtension.uppercase(Locale.ROOT)
            mimeType.contains("pdf", ignoreCase = true) -> "PDF"
            mimeType.startsWith("image/", ignoreCase = true) -> mimeType.substringAfter("/").uppercase(Locale.ROOT).take(4)
            frontFileRef.isBlank() -> "RECORD"
            else -> "FILE"
        }
}

@Entity(tableName = "document_fields")
data class DocumentFieldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val label: String,
    val value: String,
    val isSensitive: Boolean = false,
    val isMaskedByDefault: Boolean = false,
    val source: String = FieldSource.MANUAL.name,
    val userConfirmed: Boolean = true,
    val iconKey: String = "info",
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
