package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CryptoStorageManager
import com.example.data.DocumentCategoryGroup
import com.example.data.DocumentEntity
import com.example.data.DocumentFieldEntity
import com.example.data.DocumentWithFields
import com.example.data.EmergencyIceProfile
import com.example.data.ExportFormat
import com.example.data.SupportedFileFormatGroup
import com.example.data.VaultDatabase
import com.example.data.VaultRepository
import com.example.ui.daysUntilExpiry
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.BorderContrastStyle
import com.example.ui.theme.CardDensityStyle
import com.example.ui.theme.CornerRadiusStyle
import com.example.ui.theme.VaultThemeId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name and 18 themes plus full UI customization persistence work`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SAFEAREA", appName)

        assertEquals(18, VaultThemeId.entries.size)

        val storage = CryptoStorageManager(context)
        storage.selectedTheme = VaultThemeId.OLED_PITCH_BLACK
        storage.appearanceMode = AppearanceMode.DARK
        storage.cornerRadiusStyle = CornerRadiusStyle.PILL
        storage.cardDensityStyle = CardDensityStyle.SPACIOUS
        storage.borderContrastStyle = BorderContrastStyle.BOLD
        storage.customVaultTitle = "My Sovereign Vault"
        storage.customVaultTagline = "Offline & Encrypted"
        storage.emergencyIceProfile = EmergencyIceProfile(
            fullName = "Aarav Sharma",
            bloodGroup = "O+",
            allergies = "Penicillin",
            emergencyContact = "+977-9800000000"
        )

        assertEquals(VaultThemeId.OLED_PITCH_BLACK, storage.selectedTheme)
        assertEquals(AppearanceMode.DARK, storage.appearanceMode)
        assertEquals(CornerRadiusStyle.PILL, storage.cornerRadiusStyle)
        assertEquals(CardDensityStyle.SPACIOUS, storage.cardDensityStyle)
        assertEquals(BorderContrastStyle.BOLD, storage.borderContrastStyle)
        assertEquals("My Sovereign Vault", storage.customVaultTitle)
        assertEquals("Offline & Encrypted", storage.customVaultTagline)
        assertTrue(storage.emergencyIceProfile.isConfigured)
        assertEquals("O+", storage.emergencyIceProfile.bloodGroup)
    }

    @Test
    fun `pbkdf2 pin and pattern hashing and rate limiting work`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = CryptoStorageManager(context)

        storage.savePin("2580")
        assertTrue(storage.hasPinConfigured())
        assertTrue(storage.verifyPin("2580"))
        assertFalse(storage.verifyPin("9999"))

        storage.savePattern(listOf(0, 1, 2, 5, 8))
        assertTrue(storage.hasPatternConfigured())
        assertTrue(storage.verifyPattern(listOf(0, 1, 2, 5, 8)))
        assertFalse(storage.verifyPattern(listOf(0, 4, 8)))

        storage.resetFailedAttempts()
        assertEquals(0L, storage.recordFailedAttempt())
        assertEquals(0L, storage.recordFailedAttempt())
        val thirdDelay = storage.recordFailedAttempt()
        assertTrue(thirdDelay >= 10_000L)
        assertTrue(storage.getRemainingLockoutSeconds() > 0)
    }

    @Test
    fun `aes gcm file encryption and decryption roundtrip works`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = CryptoStorageManager(context)
        val samplePayload = "Sensitive Document Binary Payload 12345".toByteArray()

        val ref = storage.saveEncryptedFile("test_id_scan", samplePayload)
        val decrypted = storage.readDecryptedFile(ref)
        assertNotNull(decrypted)
        assertArrayEquals(samplePayload, decrypted)

        val days = daysUntilExpiry("2099-12-31")
        assertNotNull(days)
        assertTrue((days ?: 0L) > 365L)
    }

    @Test
    fun `all 15 document categories and 7 multi format exports work`() = runBlocking {
        assertEquals(15, DocumentCategoryGroup.entries.size)
        val totalSubtypes = DocumentCategoryGroup.entries.sumOf { it.subtypes.size }
        assertTrue(totalSubtypes >= 100)

        assertEquals(7, ExportFormat.entries.size)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = VaultDatabase.getDatabase(context)
        val storage = CryptoStorageManager(context)
        val repository = VaultRepository(context, db.vaultDao(), storage)

        val sampleDocs = listOf(
            DocumentWithFields(
                document = DocumentEntity(
                    id = 1L,
                    title = "National Identity Card",
                    category = DocumentCategoryGroup.PERSONAL.title,
                    documentSubtype = "National Identity Card",
                    issuer = "Department of National ID",
                    country = "Nepal",
                    expiryDate = "2032-05-20"
                ),
                fields = listOf(
                    DocumentFieldEntity(
                        documentId = 1L,
                        label = "ID Number",
                        value = "987-654-3210",
                        isSensitive = true
                    )
                )
            )
        )

        ExportFormat.entries.forEach { format ->
            val exportedFile = repository.exportVaultData(format, sampleDocs)
            assertTrue("Exported file for ${format.name} should exist", exportedFile.exists())
            assertTrue("Exported file for ${format.name} should be non-empty", exportedFile.length() > 0L)
            assertTrue(exportedFile.name.endsWith(".${format.extension}"))
        }

        val packetFile = repository.compileDocumentsPacketPdf("Official Packet", sampleDocs)
        assertTrue(packetFile.exists())
        assertTrue(packetFile.length() > 0L)
        assertTrue(packetFile.name.endsWith(".pdf"))

        assertEquals(
            SupportedFileFormatGroup.PDF,
            SupportedFileFormatGroup.detectGroup("pdf", "application/pdf")
        )
        assertEquals(
            SupportedFileFormatGroup.TEXT_DATA,
            SupportedFileFormatGroup.detectGroup("json", "application/json")
        )
    }
}
