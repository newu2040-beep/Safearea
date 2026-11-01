package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CryptoStorageManager
import com.example.ui.daysUntilExpiry
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.VaultThemeId
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
    fun `app name and theme persistence work`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SAFEAREA", appName)

        val storage = CryptoStorageManager(context)
        storage.selectedTheme = VaultThemeId.LAVENDER
        storage.appearanceMode = AppearanceMode.DARK
        assertEquals(VaultThemeId.LAVENDER, storage.selectedTheme)
        assertEquals(AppearanceMode.DARK, storage.appearanceMode)
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
}
