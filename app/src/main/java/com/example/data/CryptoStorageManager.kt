package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.BorderContrastStyle
import com.example.ui.theme.CardDensityStyle
import com.example.ui.theme.CornerRadiusStyle
import com.example.ui.theme.VaultThemeId
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles:
 * 1. Non-sensitive settings & UI customization persistence (theme, appearance mode, corner radius, density, border contrast, custom vault title/tagline, Emergency ICE profile).
 * 2. PBKDF2WithHmacSHA256 credential derivation for PIN & Pattern (never stores plain-text PINs or patterns).
 * 3. Exponential rate-limiting on failed unlock attempts.
 * 4. AES-256-GCM file encryption & decryption backed by Android Keystore (with fallback key for JVM/Robolectric tests).
 */
class CryptoStorageManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("safearea_prefs", Context.MODE_PRIVATE)

    private val vaultDir: File
        get() = File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }

    val exportCacheDir: File
        get() = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }

    companion object {
        private const val KEYSTORE_ALIAS = "safearea_master_aes_key"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
        private const val PBKDF2_ITERATIONS = 64_000
        private const val PBKDF2_KEY_BITS = 256
    }

    // --- Settings & UI Customization Preferences ---

    var onboardingComplete: Boolean
        get() = prefs.getBoolean("onboarding_complete", false)
        set(value) = prefs.edit().putBoolean("onboarding_complete", value).apply()

    var appLockEnabled: Boolean
        get() = prefs.getBoolean("app_lock_enabled", false)
        set(value) = prefs.edit().putBoolean("app_lock_enabled", value).apply()

    var primaryLockMethod: LockMethod
        get() {
            val raw = prefs.getString("primary_lock_method", LockMethod.PATTERN.name) ?: LockMethod.PATTERN.name
            return runCatching { LockMethod.valueOf(raw) }.getOrDefault(LockMethod.PATTERN)
        }
        set(value) = prefs.edit().putString("primary_lock_method", value.name).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean("biometric_enabled", false)
        set(value) = prefs.edit().putBoolean("biometric_enabled", value).apply()

    var selectedTheme: VaultThemeId
        get() {
            val raw = prefs.getString("theme_id", VaultThemeId.CREAM.name) ?: VaultThemeId.CREAM.name
            return runCatching { VaultThemeId.valueOf(raw) }.getOrDefault(VaultThemeId.CREAM)
        }
        set(value) = prefs.edit().putString("theme_id", value.name).apply()

    var appearanceMode: AppearanceMode
        get() {
            val raw = prefs.getString("appearance_mode", AppearanceMode.LIGHT.name) ?: AppearanceMode.LIGHT.name
            return runCatching { AppearanceMode.valueOf(raw) }.getOrDefault(AppearanceMode.LIGHT)
        }
        set(value) = prefs.edit().putString("appearance_mode", value.name).apply()

    var cornerRadiusStyle: CornerRadiusStyle
        get() {
            val raw = prefs.getString("corner_radius_style", CornerRadiusStyle.APPLE_ROUNDED.name)
                ?: CornerRadiusStyle.APPLE_ROUNDED.name
            return runCatching { CornerRadiusStyle.valueOf(raw) }.getOrDefault(CornerRadiusStyle.APPLE_ROUNDED)
        }
        set(value) = prefs.edit().putString("corner_radius_style", value.name).apply()

    var cardDensityStyle: CardDensityStyle
        get() {
            val raw = prefs.getString("card_density_style", CardDensityStyle.COMFORTABLE.name)
                ?: CardDensityStyle.COMFORTABLE.name
            return runCatching { CardDensityStyle.valueOf(raw) }.getOrDefault(CardDensityStyle.COMFORTABLE)
        }
        set(value) = prefs.edit().putString("card_density_style", value.name).apply()

    var borderContrastStyle: BorderContrastStyle
        get() {
            val raw = prefs.getString("border_contrast_style", BorderContrastStyle.STANDARD.name)
                ?: BorderContrastStyle.STANDARD.name
            return runCatching { BorderContrastStyle.valueOf(raw) }.getOrDefault(BorderContrastStyle.STANDARD)
        }
        set(value) = prefs.edit().putString("border_contrast_style", value.name).apply()

    var customVaultTitle: String
        get() = prefs.getString("custom_vault_title", "SAFEAREA")?.ifBlank { "SAFEAREA" } ?: "SAFEAREA"
        set(value) = prefs.edit().putString("custom_vault_title", value.trim().ifBlank { "SAFEAREA" }).apply()

    var customVaultTagline: String
        get() = prefs.getString("custom_vault_tagline", "Personal Document Vault")
            ?.ifBlank { "Personal Document Vault" } ?: "Personal Document Vault"
        set(value) = prefs.edit().putString(
            "custom_vault_tagline",
            value.trim().ifBlank { "Personal Document Vault" }
        ).apply()

    var showSecurityScoreCard: Boolean
        get() = prefs.getBoolean("show_security_score_card", true)
        set(value) = prefs.edit().putBoolean("show_security_score_card", value).apply()

    var showCategoryGridOnHome: Boolean
        get() = prefs.getBoolean("show_category_grid_on_home", true)
        set(value) = prefs.edit().putBoolean("show_category_grid_on_home", value).apply()

    var emergencyIceProfile: EmergencyIceProfile
        get() = EmergencyIceProfile(
            fullName = prefs.getString("ice_full_name", "") ?: "",
            bloodGroup = prefs.getString("ice_blood_group", "") ?: "",
            allergies = prefs.getString("ice_allergies", "") ?: "",
            emergencyContact = prefs.getString("ice_emergency_contact", "") ?: "",
            medicalConditions = prefs.getString("ice_medical_conditions", "") ?: "",
            insurancePolicyNo = prefs.getString("ice_insurance_policy", "") ?: ""
        )
        set(value) {
            prefs.edit()
                .putString("ice_full_name", value.fullName.trim())
                .putString("ice_blood_group", value.bloodGroup.trim())
                .putString("ice_allergies", value.allergies.trim())
                .putString("ice_emergency_contact", value.emergencyContact.trim())
                .putString("ice_medical_conditions", value.medicalConditions.trim())
                .putString("ice_insurance_policy", value.insurancePolicyNo.trim())
                .apply()
        }

    var autoLockTimeoutSeconds: Int
        get() = prefs.getInt("auto_lock_timeout_sec", 60) // Default 1 minute
        set(value) = prefs.edit().putInt("auto_lock_timeout_sec", value).apply()

    var lockOnBackground: Boolean
        get() = prefs.getBoolean("lock_on_background", true)
        set(value) = prefs.edit().putBoolean("lock_on_background", value).apply()

    var hideInAppSwitcher: Boolean
        get() = prefs.getBoolean("hide_in_app_switcher", false)
        set(value) = prefs.edit().putBoolean("hide_in_app_switcher", value).apply()

    var expiryNotificationsEnabled: Boolean
        get() = prefs.getBoolean("expiry_notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("expiry_notifications_enabled", value).apply()

    var customCategoriesCsv: String
        get() = prefs.getString("custom_categories_csv", "") ?: ""
        set(value) = prefs.edit().putString("custom_categories_csv", value).apply()

    var recoveryPhraseHint: String
        get() = prefs.getString("recovery_hint", "") ?: ""
        set(value) = prefs.edit().putString("recovery_hint", value).apply()

    var useSystemFont: Boolean
        get() = prefs.getBoolean("use_system_font", false)
        set(value) = prefs.edit().putBoolean("use_system_font", value).apply()

    var fontScale: Float
        get() = prefs.getFloat("font_scale", 1.0f)
        set(value) = prefs.edit().putFloat("font_scale", value).apply()

    var grantedFolderUri: String
        get() = prefs.getString("granted_folder_uri", "") ?: ""
        set(value) = prefs.edit().putString("granted_folder_uri", value).apply()

    /**
     * Creates a writable file inside `cacheDir/exports` for `ActivityResultContracts.TakePicture()`
     * so the device's native Android camera app can write full-resolution photos via `FileProvider`.
     */
    fun createTempCameraCaptureFile(prefix: String = "camera_capture"): File {
        return File(exportCacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
    }

    // --- Credential Derivation (PBKDF2WithHmacSHA256) ---

    fun hasPinConfigured(): Boolean =
        !prefs.getString("pin_hash", null).isNullOrEmpty()

    fun hasPatternConfigured(): Boolean =
        !prefs.getString("pattern_hash", null).isNullOrEmpty()

    fun savePin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = deriveCredentialHash(pin, salt)
        prefs.edit()
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPin(candidatePin: String): Boolean {
        val saltB64 = prefs.getString("pin_salt", null) ?: return false
        val expectedHashB64 = prefs.getString("pin_hash", null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expectedHash = Base64.decode(expectedHashB64, Base64.NO_WRAP)
        val actualHash = deriveCredentialHash(candidatePin, salt)
        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    fun savePattern(patternNodes: List<Int>) {
        val normalized = patternNodes.joinToString("-")
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = deriveCredentialHash(normalized, salt)
        prefs.edit()
            .putString("pattern_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pattern_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPattern(candidateNodes: List<Int>): Boolean {
        val saltB64 = prefs.getString("pattern_salt", null) ?: return false
        val expectedHashB64 = prefs.getString("pattern_hash", null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expectedHash = Base64.decode(expectedHashB64, Base64.NO_WRAP)
        val actualHash = deriveCredentialHash(candidateNodes.joinToString("-"), salt)
        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    private fun deriveCredentialHash(secret: String, salt: ByteArray): ByteArray {
        return try {
            val spec = PBEKeySpec(secret.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded
        } catch (e: Exception) {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(salt)
            digest.digest(secret.toByteArray(Charsets.UTF_8))
        }
    }

    // --- Rate Limiting on Failed Attempts ---

    var failedUnlockAttempts: Int
        get() = prefs.getInt("failed_unlock_attempts", 0)
        set(value) = prefs.edit().putInt("failed_unlock_attempts", value).apply()

    var lockoutUntilEpochMs: Long
        get() = prefs.getLong("lockout_until_epoch_ms", 0L)
        set(value) = prefs.edit().putLong("lockout_until_epoch_ms", value).apply()

    fun recordFailedAttempt(): Long {
        val nextCount = failedUnlockAttempts + 1
        failedUnlockAttempts = nextCount
        val delayMs = when {
            nextCount < 3 -> 0L
            nextCount == 3 -> 10_000L
            nextCount == 4 -> 20_000L
            nextCount == 5 -> 30_000L
            else -> 60_000L
        }
        if (delayMs > 0L) {
            lockoutUntilEpochMs = System.currentTimeMillis() + delayMs
        }
        return delayMs
    }

    fun resetFailedAttempts() {
        failedUnlockAttempts = 0
        lockoutUntilEpochMs = 0L
    }

    fun getRemainingLockoutSeconds(): Int {
        val remaining = lockoutUntilEpochMs - System.currentTimeMillis()
        return if (remaining > 0L) ((remaining + 999L) / 1000L).toInt() else 0
    }

    // --- Android Keystore AES-256-GCM File Encryption ---

    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val existingKey = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
            if (existingKey != null) return existingKey

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            // Fallback key in local preferences if running in JVM unit test without AndroidKeyStore provider
            val fallbackB64 = prefs.getString("fallback_key_b64", null)
            val keyBytes = if (fallbackB64 != null) {
                Base64.decode(fallbackB64, Base64.NO_WRAP)
            } else {
                ByteArray(32).also {
                    SecureRandom().nextBytes(it)
                    prefs.edit().putString("fallback_key_b64", Base64.encodeToString(it, Base64.NO_WRAP)).apply()
                }
            }
            SecretKeySpec(keyBytes, "AES")
        }
    }

    fun saveEncryptedFile(fileName: String, plainBytes: ByteArray): String {
        val targetFile = File(vaultDir, "$fileName.enc")
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainBytes)

        targetFile.outputStream().use { out ->
            out.write(iv.size)
            out.write(iv)
            out.write(cipherBytes)
        }
        return targetFile.name
    }

    fun readDecryptedFile(fileRef: String): ByteArray? {
        if (fileRef.isBlank()) return null
        val targetFile = File(vaultDir, fileRef)
        if (!targetFile.exists()) return null
        return try {
            val allBytes = targetFile.readBytes()
            if (allBytes.size < 2 + GCM_IV_LENGTH) return null
            val ivSize = allBytes[0].toInt() and 0xFF
            val iv = allBytes.copyOfRange(1, 1 + ivSize)
            val cipherBytes = allBytes.copyOfRange(1 + ivSize, allBytes.size)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(cipherBytes)
        } catch (e: Exception) {
            null
        }
    }

    fun writeDecryptedTempFileForPreviewOrShare(fileRef: String, extension: String): File? {
        val bytes = readDecryptedFile(fileRef) ?: return null
        val cleanName = fileRef.removeSuffix(".enc").replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val outFile = File(exportCacheDir, "${cleanName}.$extension")
        outFile.writeBytes(bytes)
        return outFile
    }

    fun deleteEncryptedFile(fileRef: String) {
        if (fileRef.isBlank()) return
        val file = File(vaultDir, fileRef)
        if (file.exists()) file.delete()
    }

    fun getVaultStorageBytes(): Long {
        return vaultDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun getCacheStorageBytes(): Long {
        return exportCacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun clearExportCache() {
        exportCacheDir.listFiles()?.forEach { it.delete() }
    }

    fun wipeAllVaultFilesAndSettings() {
        vaultDir.listFiles()?.forEach { it.delete() }
        exportCacheDir.listFiles()?.forEach { it.delete() }
        val keepTheme = selectedTheme
        val keepAppearance = appearanceMode
        prefs.edit().clear().apply()
        selectedTheme = keepTheme
        appearanceMode = keepAppearance
    }
}
