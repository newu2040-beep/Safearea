package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuditEventEntity
import com.example.data.CryptoStorageManager
import com.example.data.DEFAULT_CATEGORIES
import com.example.data.DocumentCategoryGroup
import com.example.data.DocumentEntity
import com.example.data.DocumentFieldEntity
import com.example.data.DocumentWithFields
import com.example.data.FieldSource
import com.example.data.LockMethod
import com.example.data.SupportedFileFormatGroup
import com.example.data.VaultDatabase
import com.example.data.VaultRepository
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.VaultThemeId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class SafeAreaScreen {
    WELCOME,
    SECURITY_SETUP,
    UNLOCK,
    HOME,
    DOCUMENTS,
    DOCUMENT_DETAIL,
    ADD_DOCUMENT,
    CAMERA_SCANNER,
    DOCUMENT_EDITOR,
    TOOLS,
    PROFILE
}

enum class SortOption(val label: String) {
    MODIFIED_DATE("Modified Date"),
    ADDED_DATE("Date Added"),
    TITLE("Name (A–Z)"),
    FILE_SIZE("File Size"),
    EXPIRY_DATE("Expiry Date")
}

enum class ExpiryFilter(val label: String) {
    ALL("All Expiry"),
    ACTIVE("Valid / Active"),
    EXPIRING_SOON("Expiring Soon"),
    EXPIRED("Expired")
}

data class VaultUiState(
    val currentScreen: SafeAreaScreen = SafeAreaScreen.WELCOME,
    val navigationStack: List<SafeAreaScreen> = emptyList(),
    val isVaultLocked: Boolean = false,
    val appLockEnabled: Boolean = false,
    val primaryLockMethod: LockMethod = LockMethod.PATTERN,
    val biometricEnabled: Boolean = false,
    val hasPinSet: Boolean = false,
    val hasPatternSet: Boolean = false,
    val lockoutRemainingSeconds: Int = 0,
    val unlockErrorMessage: String? = null,
    val selectedTheme: VaultThemeId = VaultThemeId.CREAM,
    val appearanceMode: AppearanceMode = AppearanceMode.LIGHT,
    val autoLockTimeoutSeconds: Int = 60,
    val lockOnBackground: Boolean = true,
    val hideInAppSwitcher: Boolean = false,
    val expiryNotificationsEnabled: Boolean = true,
    val liveDateTimeText: String = "",
    val selectedDocumentId: Long? = null,
    val editingDocument: DocumentEntity? = null,
    val editingFields: List<DocumentFieldEntity> = emptyList(),
    val isOcrReviewMode: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val selectedFormatGroup: SupportedFileFormatGroup = SupportedFileFormatGroup.ALL,
    val expiryFilter: ExpiryFilter = ExpiryFilter.ALL,
    val filterFavouritesOnly: Boolean = false,
    val filterRecentlyOpenedOnly: Boolean = false,
    val filterArchivedOnly: Boolean = false,
    val sortOption: SortOption = SortOption.MODIFIED_DATE,
    val isGridView: Boolean = false,
    val selectedBulkDocIds: Set<Long> = emptySet(),
    val customCategories: List<String> = emptyList(),
    val statusBannerMessage: String? = null,
    val clipboardClearCountdownSec: Int = 0,
    val vaultStorageBytes: Long = 0L,
    val cacheStorageBytes: Long = 0L,
    val useSystemFont: Boolean = false,
    val fontScale: Float = 1.0f,
    val grantedFolderUri: String = "",
    val permissionsRefreshTick: Int = 0,
    val isLoadingImport: Boolean = false
) {
    val allCategories: List<String>
        get() = (DEFAULT_CATEGORIES + customCategories).distinct()

    val isBulkSelectionActive: Boolean
        get() = selectedBulkDocIds.isNotEmpty()
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val database = VaultDatabase.getDatabase(application)
    val cryptoStorage = CryptoStorageManager(application)
    val repository = VaultRepository(application, database.vaultDao(), cryptoStorage)

    val documentsWithFields: StateFlow<List<DocumentWithFields>> =
        repository.documentsWithFieldsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val auditEvents: StateFlow<List<AuditEventEntity>> =
        repository.auditEventsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private var clipboardClearJob: Job? = null
    private var statusDismissJob: Job? = null
    private var lockoutTimerJob: Job? = null
    private var inactivityJob: Job? = null

    init {
        val initialOnboardingDone = cryptoStorage.onboardingComplete
        val lockEnabled = cryptoStorage.appLockEnabled
        val startScreen = when {
            !initialOnboardingDone -> SafeAreaScreen.WELCOME
            lockEnabled -> SafeAreaScreen.UNLOCK
            else -> SafeAreaScreen.HOME
        }

        _uiState.update {
            it.copy(
                currentScreen = startScreen,
                isVaultLocked = lockEnabled && initialOnboardingDone,
                appLockEnabled = lockEnabled,
                primaryLockMethod = cryptoStorage.primaryLockMethod,
                biometricEnabled = cryptoStorage.biometricEnabled,
                hasPinSet = cryptoStorage.hasPinConfigured(),
                hasPatternSet = cryptoStorage.hasPatternConfigured(),
                selectedTheme = cryptoStorage.selectedTheme,
                appearanceMode = cryptoStorage.appearanceMode,
                autoLockTimeoutSeconds = cryptoStorage.autoLockTimeoutSeconds,
                lockOnBackground = cryptoStorage.lockOnBackground,
                hideInAppSwitcher = cryptoStorage.hideInAppSwitcher,
                expiryNotificationsEnabled = cryptoStorage.expiryNotificationsEnabled,
                useSystemFont = cryptoStorage.useSystemFont,
                fontScale = cryptoStorage.fontScale,
                grantedFolderUri = cryptoStorage.grantedFolderUri,
                customCategories = cryptoStorage.customCategoriesCsv.split(",")
                    .map { c -> c.trim() }
                    .filter { c -> c.isNotEmpty() },
                liveDateTimeText = computeLiveDateTime(application)
            )
        }

        viewModelScope.launch {
            repository.purgeAnyDemoData()
            refreshStorageStats()
        }

        viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(liveDateTimeText = computeLiveDateTime(getApplication())) }
                delay(15_000L)
            }
        }

        refreshLockoutCountdown()
        resetInactivityTimer()
    }

    fun computeLiveDateTime(context: Context): String {
        val now = Date()
        val is24Hour = DateFormat.is24HourFormat(context)
        val datePattern = "EEE, d MMM yyyy"
        val timePattern = if (is24Hour) "HH:mm" else "h:mm a"
        val dateStr = SimpleDateFormat(datePattern, Locale.getDefault()).format(now)
        val timeStr = SimpleDateFormat(timePattern, Locale.getDefault()).format(now)
        return "Today · $dateStr · $timeStr"
    }

    fun onAppResumed() {
        _uiState.update {
            it.copy(
                liveDateTimeText = computeLiveDateTime(getApplication()),
                permissionsRefreshTick = it.permissionsRefreshTick + 1
            )
        }
        refreshLockoutCountdown()
        resetInactivityTimer()
    }

    fun onAppBackgrounded() {
        if (cryptoStorage.appLockEnabled && cryptoStorage.lockOnBackground && cryptoStorage.onboardingComplete) {
            lockVaultNow()
        }
    }

    fun onUserInteraction() {
        resetInactivityTimer()
    }

    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        val timeoutSec = _uiState.value.autoLockTimeoutSeconds
        if (!_uiState.value.appLockEnabled || _uiState.value.isVaultLocked || timeoutSec <= 0) return
        inactivityJob = viewModelScope.launch {
            delay(timeoutSec * 1000L)
            if (_uiState.value.appLockEnabled && !_uiState.value.isVaultLocked && cryptoStorage.onboardingComplete) {
                lockVaultNow()
            }
        }
    }

    fun navigateTo(target: SafeAreaScreen, clearBackStack: Boolean = false) {
        onUserInteraction()
        _uiState.update { state ->
            val updatedStack = if (clearBackStack) {
                emptyList()
            } else if (state.currentScreen != target) {
                (state.navigationStack + state.currentScreen).takeLast(12)
            } else {
                state.navigationStack
            }
            state.copy(
                currentScreen = target,
                navigationStack = updatedStack,
                unlockErrorMessage = null,
                selectedBulkDocIds = if (target != SafeAreaScreen.DOCUMENTS) emptySet() else state.selectedBulkDocIds
            )
        }
        if (target == SafeAreaScreen.PROFILE || target == SafeAreaScreen.TOOLS) {
            refreshStorageStats()
        }
    }

    fun navigateBack(): Boolean {
        onUserInteraction()
        val current = _uiState.value
        if (current.isBulkSelectionActive) {
            clearBulkSelection()
            return true
        }
        if (current.isVaultLocked && current.currentScreen == SafeAreaScreen.UNLOCK) {
            return false
        }
        val stack = current.navigationStack
        return if (stack.isNotEmpty()) {
            val previous = stack.last()
            _uiState.update {
                it.copy(
                    currentScreen = previous,
                    navigationStack = stack.dropLast(1)
                )
            }
            true
        } else if (current.currentScreen != SafeAreaScreen.HOME && current.currentScreen != SafeAreaScreen.WELCOME) {
            _uiState.update { it.copy(currentScreen = SafeAreaScreen.HOME) }
            true
        } else {
            false
        }
    }

    fun openDocumentDetails(documentId: Long) {
        viewModelScope.launch {
            documentsWithFields.value.find { it.document.id == documentId }?.let {
                repository.markDocumentOpened(it.document)
            }
        }
        _uiState.update { it.copy(selectedDocumentId = documentId) }
        navigateTo(SafeAreaScreen.DOCUMENT_DETAIL)
    }

    // --- Onboarding & Security Setup ---

    fun selectLockMethodDraft(method: LockMethod) {
        _uiState.update { it.copy(primaryLockMethod = method, unlockErrorMessage = null) }
    }

    fun completeSecuritySetupWithPin(pin: String, enableBiometric: Boolean, recoveryHint: String) {
        cryptoStorage.savePin(pin)
        cryptoStorage.primaryLockMethod = if (enableBiometric) LockMethod.FINGERPRINT else LockMethod.PIN
        cryptoStorage.biometricEnabled = enableBiometric
        cryptoStorage.appLockEnabled = true
        cryptoStorage.onboardingComplete = true
        cryptoStorage.recoveryPhraseHint = recoveryHint
        cryptoStorage.resetFailedAttempts()

        _uiState.update {
            it.copy(
                appLockEnabled = true,
                isVaultLocked = false,
                primaryLockMethod = cryptoStorage.primaryLockMethod,
                biometricEnabled = enableBiometric,
                hasPinSet = true,
                currentScreen = SafeAreaScreen.HOME,
                navigationStack = emptyList()
            )
        }
        showStatusMessage("Vault PIN protection enabled")
        viewModelScope.launch {
            repository.logAudit("SECURITY_ENABLED", "Configured PIN protection for SAFEAREA")
        }
        resetInactivityTimer()
    }

    fun completeSecuritySetupWithPattern(
        patternNodes: List<Int>,
        fallbackPin: String?,
        enableBiometric: Boolean
    ) {
        cryptoStorage.savePattern(patternNodes)
        if (!fallbackPin.isNullOrBlank() && fallbackPin.length >= 4) {
            cryptoStorage.savePin(fallbackPin)
        }
        cryptoStorage.primaryLockMethod = LockMethod.PATTERN
        cryptoStorage.biometricEnabled = enableBiometric
        cryptoStorage.appLockEnabled = true
        cryptoStorage.onboardingComplete = true
        cryptoStorage.resetFailedAttempts()

        _uiState.update {
            it.copy(
                appLockEnabled = true,
                isVaultLocked = false,
                primaryLockMethod = LockMethod.PATTERN,
                biometricEnabled = enableBiometric,
                hasPatternSet = true,
                hasPinSet = cryptoStorage.hasPinConfigured(),
                currentScreen = SafeAreaScreen.HOME,
                navigationStack = emptyList()
            )
        }
        showStatusMessage("Pattern lock enabled")
        viewModelScope.launch {
            repository.logAudit("SECURITY_ENABLED", "Configured pattern lock for SAFEAREA")
        }
        resetInactivityTimer()
    }

    fun skipLockSetupForNow() {
        cryptoStorage.onboardingComplete = true
        cryptoStorage.appLockEnabled = false
        _uiState.update {
            it.copy(
                appLockEnabled = false,
                isVaultLocked = false,
                currentScreen = SafeAreaScreen.HOME,
                navigationStack = emptyList()
            )
        }
    }

    fun lockVaultNow() {
        if (!cryptoStorage.hasPinConfigured() && !cryptoStorage.hasPatternConfigured()) {
            showStatusMessage("Configure a PIN or Pattern first to lock your vault.")
            navigateTo(SafeAreaScreen.SECURITY_SETUP)
            return
        }
        cryptoStorage.appLockEnabled = true
        _uiState.update {
            it.copy(
                appLockEnabled = true,
                isVaultLocked = true,
                currentScreen = SafeAreaScreen.UNLOCK,
                unlockErrorMessage = null
            )
        }
    }

    fun unlockWithPin(candidatePin: String): Boolean {
        if (cryptoStorage.getRemainingLockoutSeconds() > 0) {
            refreshLockoutCountdown()
            return false
        }
        return if (cryptoStorage.verifyPin(candidatePin)) {
            onUnlockSuccess("PIN")
            true
        } else {
            onUnlockFailure("Incorrect PIN.")
            false
        }
    }

    fun unlockWithPattern(candidateNodes: List<Int>): Boolean {
        if (cryptoStorage.getRemainingLockoutSeconds() > 0) {
            refreshLockoutCountdown()
            return false
        }
        return if (cryptoStorage.verifyPattern(candidateNodes)) {
            onUnlockSuccess("Pattern")
            true
        } else {
            onUnlockFailure("Incorrect pattern.")
            false
        }
    }

    fun onBiometricUnlockSucceeded() {
        onUnlockSuccess("Biometric")
    }

    fun onBiometricUnlockFailed(reason: String) {
        _uiState.update { it.copy(unlockErrorMessage = reason) }
    }

    private fun onUnlockSuccess(methodUsed: String) {
        cryptoStorage.resetFailedAttempts()
        _uiState.update {
            it.copy(
                isVaultLocked = false,
                lockoutRemainingSeconds = 0,
                unlockErrorMessage = null,
                currentScreen = SafeAreaScreen.HOME,
                navigationStack = emptyList()
            )
        }
        showStatusMessage("Vault unlocked ($methodUsed)")
        viewModelScope.launch {
            repository.logAudit("VAULT_UNLOCKED", "Unlocked vault via $methodUsed")
        }
        resetInactivityTimer()
    }

    private fun onUnlockFailure(baseError: String) {
        val delayMs = cryptoStorage.recordFailedAttempt()
        val attempts = cryptoStorage.failedUnlockAttempts
        val msg = if (delayMs > 0L) {
            "$baseError Too many attempts ($attempts). Wait ${delayMs / 1000}s."
        } else {
            "$baseError Attempt $attempts of 3 before temporary cooldown."
        }
        _uiState.update { it.copy(unlockErrorMessage = msg) }
        refreshLockoutCountdown()
    }

    private fun refreshLockoutCountdown() {
        lockoutTimerJob?.cancel()
        val initialSec = cryptoStorage.getRemainingLockoutSeconds()
        if (initialSec <= 0) {
            _uiState.update { it.copy(lockoutRemainingSeconds = 0) }
            return
        }
        lockoutTimerJob = viewModelScope.launch {
            var remaining = initialSec
            while (remaining > 0) {
                _uiState.update { it.copy(lockoutRemainingSeconds = remaining) }
                delay(1000L)
                remaining = cryptoStorage.getRemainingLockoutSeconds()
            }
            _uiState.update { it.copy(lockoutRemainingSeconds = 0, unlockErrorMessage = null) }
        }
    }

    // --- Theme & Settings ---

    fun setTheme(theme: VaultThemeId) {
        cryptoStorage.selectedTheme = theme
        _uiState.update { it.copy(selectedTheme = theme) }
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        cryptoStorage.appearanceMode = mode
        _uiState.update { it.copy(appearanceMode = mode) }
    }

    fun setUseSystemFont(enabled: Boolean) {
        cryptoStorage.useSystemFont = enabled
        _uiState.update { it.copy(useSystemFont = enabled) }
        showStatusMessage(if (enabled) "Switched to System Font" else "Switched to Editorial Font")
    }

    fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(0.85f, 1.30f)
        cryptoStorage.fontScale = clamped
        _uiState.update { it.copy(fontScale = clamped) }
    }

    fun setGrantedFolderUri(uriStr: String) {
        cryptoStorage.grantedFolderUri = uriStr
        _uiState.update {
            it.copy(
                grantedFolderUri = uriStr,
                permissionsRefreshTick = it.permissionsRefreshTick + 1
            )
        }
        showStatusMessage("Directory access saved")
    }

    fun onPermissionsResult(results: Map<String, Boolean>) {
        _uiState.update {
            it.copy(permissionsRefreshTick = it.permissionsRefreshTick + 1)
        }
    }

    fun setAutoLockTimeout(seconds: Int) {
        cryptoStorage.autoLockTimeoutSeconds = seconds
        _uiState.update { it.copy(autoLockTimeoutSeconds = seconds) }
        resetInactivityTimer()
    }

    fun setLockOnBackground(enabled: Boolean) {
        cryptoStorage.lockOnBackground = enabled
        _uiState.update { it.copy(lockOnBackground = enabled) }
    }

    fun setHideInAppSwitcher(enabled: Boolean) {
        cryptoStorage.hideInAppSwitcher = enabled
        _uiState.update { it.copy(hideInAppSwitcher = enabled) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        if (enabled && !cryptoStorage.hasPinConfigured() && !cryptoStorage.hasPatternConfigured()) {
            navigateTo(SafeAreaScreen.SECURITY_SETUP)
            return
        }
        cryptoStorage.appLockEnabled = enabled
        _uiState.update { it.copy(appLockEnabled = enabled) }
    }

    fun setExpiryNotificationsEnabled(enabled: Boolean) {
        cryptoStorage.expiryNotificationsEnabled = enabled
        _uiState.update { it.copy(expiryNotificationsEnabled = enabled) }
        if (enabled) {
            triggerExpiryNotificationCheck()
        }
    }

    fun addCustomCategory(categoryName: String) {
        val trimmed = categoryName.trim()
        if (trimmed.isEmpty()) return
        val updated = (_uiState.value.customCategories + trimmed).distinct()
        cryptoStorage.customCategoriesCsv = updated.joinToString(",")
        _uiState.update { it.copy(customCategories = updated) }
        showStatusMessage("Created category \"$trimmed\"")
    }

    // --- Search, Filtering, Sorting & Bulk Selection ---

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectCategoryFilter(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun selectFormatGroupFilter(group: SupportedFileFormatGroup) {
        _uiState.update { it.copy(selectedFormatGroup = group) }
    }

    fun selectExpiryFilter(filter: ExpiryFilter) {
        _uiState.update { it.copy(expiryFilter = filter) }
    }

    fun toggleFavouritesFilter() {
        _uiState.update { it.copy(filterFavouritesOnly = !it.filterFavouritesOnly) }
    }

    fun toggleRecentlyOpenedFilter() {
        _uiState.update { it.copy(filterRecentlyOpenedOnly = !it.filterRecentlyOpenedOnly) }
    }

    fun toggleArchivedFilter() {
        _uiState.update { it.copy(filterArchivedOnly = !it.filterArchivedOnly) }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun toggleGridView() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun toggleBulkDocumentSelection(docId: Long) {
        _uiState.update { state ->
            val next = state.selectedBulkDocIds.toMutableSet()
            if (!next.add(docId)) next.remove(docId)
            state.copy(selectedBulkDocIds = next)
        }
    }

    fun selectAllVisibleDocuments(docIds: List<Long>) {
        _uiState.update { it.copy(selectedBulkDocIds = docIds.toSet()) }
    }

    fun clearBulkSelection() {
        _uiState.update { it.copy(selectedBulkDocIds = emptySet()) }
    }

    fun bulkDeleteSelectedDocuments() {
        val ids = _uiState.value.selectedBulkDocIds
        if (ids.isEmpty()) return
        val targets = documentsWithFields.value.map { it.document }.filter { it.id in ids }
        viewModelScope.launch {
            repository.bulkDeleteDocuments(targets)
            clearBulkSelection()
            showStatusMessage("Deleted ${targets.size} document(s)")
            refreshStorageStats()
        }
    }

    fun bulkArchiveSelectedDocuments(archive: Boolean) {
        val ids = _uiState.value.selectedBulkDocIds
        if (ids.isEmpty()) return
        val targets = documentsWithFields.value.map { it.document }.filter { it.id in ids }
        viewModelScope.launch {
            repository.bulkArchiveDocuments(targets, archive)
            clearBulkSelection()
            showStatusMessage("${if (archive) "Archived" else "Restored"} ${targets.size} document(s)")
        }
    }

    fun bulkMoveCategorySelectedDocuments(targetCategory: String) {
        val ids = _uiState.value.selectedBulkDocIds
        if (ids.isEmpty()) return
        val targets = documentsWithFields.value.map { it.document }.filter { it.id in ids }
        viewModelScope.launch {
            repository.bulkMoveCategory(targets, targetCategory)
            clearBulkSelection()
            showStatusMessage("Moved ${targets.size} document(s) to $targetCategory")
        }
    }

    // --- Document Add, Scan, Import, Edit, Duplicate, Rename ---

    fun startManualAddDocument(
        presetCategory: String = DocumentCategoryGroup.PERSONAL.title,
        presetSubtype: String = ""
    ) {
        val defaultGroup = DocumentCategoryGroup.fromCategoryName(presetCategory)
        val subtype = presetSubtype.ifBlank { defaultGroup.subtypes.firstOrNull().orEmpty() }
        val defaultFields = listOf(
            DocumentFieldEntity(
                documentId = 0L,
                label = "Document Number / ID",
                value = "",
                isSensitive = true,
                isMaskedByDefault = true,
                iconKey = "id",
                sortOrder = 1
            ),
            DocumentFieldEntity(
                documentId = 0L,
                label = "Holder / Owner Name",
                value = "",
                iconKey = "person",
                sortOrder = 2
            )
        )
        _uiState.update {
            it.copy(
                editingDocument = DocumentEntity(
                    title = subtype,
                    category = presetCategory,
                    documentSubtype = subtype,
                    issuer = "",
                    country = "",
                    iconTypeName = defaultGroup.name
                ),
                editingFields = defaultFields,
                isOcrReviewMode = false
            )
        }
        navigateTo(SafeAreaScreen.DOCUMENT_EDITOR)
    }

    fun startEditingExistingDocument(docWithFields: DocumentWithFields) {
        _uiState.update {
            it.copy(
                editingDocument = docWithFields.document,
                editingFields = docWithFields.fields,
                isOcrReviewMode = false
            )
        }
        navigateTo(SafeAreaScreen.DOCUMENT_EDITOR)
    }

    fun duplicateDocument(docWithFields: DocumentWithFields) {
        viewModelScope.launch {
            val newId = repository.duplicateDocument(docWithFields)
            showStatusMessage("Duplicated \"${docWithFields.document.title}\"")
            openDocumentDetails(newId)
            refreshStorageStats()
        }
    }

    fun renameDocument(doc: DocumentEntity, newTitle: String) {
        viewModelScope.launch {
            repository.renameDocument(doc, newTitle)
            showStatusMessage("Renamed to \"${newTitle.trim()}\"")
        }
    }

    fun moveDocumentCategory(doc: DocumentEntity, newCategory: String, newSubtype: String = "") {
        viewModelScope.launch {
            repository.moveDocumentCategory(doc, newCategory, newSubtype)
            showStatusMessage("Moved to $newCategory")
        }
    }

    fun importDocumentFromUri(uri: Uri, isBackSide: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingImport = true) }
            val result = repository.importUriToEncryptedVault(uri, if (isBackSide) "back" else "front")
            _uiState.update { it.copy(isLoadingImport = false) }

            result.onSuccess { asset ->
                val currentEditing = _uiState.value.editingDocument
                if (currentEditing != null && _uiState.value.currentScreen == SafeAreaScreen.DOCUMENT_EDITOR) {
                    val updatedDoc = if (isBackSide) {
                        currentEditing.copy(
                            backFileRef = asset.fileRef,
                            pageCount = (currentEditing.pageCount + asset.pageCount).coerceAtLeast(2)
                        )
                    } else {
                        currentEditing.copy(
                            frontFileRef = asset.fileRef,
                            originalFileName = asset.originalName,
                            fileExtension = asset.fileExtension,
                            mimeType = asset.mimeType,
                            fileSize = asset.fileSize,
                            pageCount = asset.pageCount,
                            title = currentEditing.title.ifBlank {
                                asset.originalName.substringBeforeLast(".").replace("_", " ")
                            }
                        )
                    }
                    _uiState.update {
                        it.copy(editingDocument = updatedDoc)
                    }
                    showStatusMessage("Attached ${asset.originalName}")
                } else {
                    val cleanTitle = asset.originalName.substringBeforeLast(".")
                        .replace("_", " ")
                        .replaceFirstChar { c -> c.uppercase() }
                    val draftDoc = DocumentEntity(
                        title = cleanTitle,
                        category = asset.suggestedCategory,
                        documentSubtype = asset.suggestedSubtype,
                        originalFileName = asset.originalName,
                        fileExtension = asset.fileExtension,
                        frontFileRef = asset.fileRef,
                        mimeType = asset.mimeType,
                        fileSize = asset.fileSize,
                        pageCount = asset.pageCount,
                        iconTypeName = DocumentCategoryGroup.fromCategoryName(asset.suggestedCategory).name,
                        isSamplePlaceholder = false
                    )
                    _uiState.update {
                        it.copy(
                            editingDocument = draftDoc,
                            editingFields = asset.extractedFields,
                            isOcrReviewMode = asset.extractedFields.any { f -> !f.userConfirmed }
                        )
                    }
                    showStatusMessage("Imported ${asset.originalName} (${asset.fileExtension.uppercase(Locale.ROOT)})")
                    navigateTo(SafeAreaScreen.DOCUMENT_EDITOR)
                }
                refreshStorageStats()
            }.onFailure { err ->
                showStatusMessage(err.message ?: "Could not import selected file.")
            }
        }
    }

    fun onCameraScanCaptured(
        frontBitmap: Bitmap,
        backBitmap: Bitmap?,
        titleHint: String,
        categoryHint: String,
        subtypeHint: String,
        rawOcrText: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingImport = true) }
            val frontRes = repository.saveBitmapToEncryptedVault(frontBitmap, "scan_front")
            val backRes = backBitmap?.let { repository.saveBitmapToEncryptedVault(it, "scan_back") }
            _uiState.update { it.copy(isLoadingImport = false) }

            val frontInfo = frontRes.getOrNull()
            if (frontInfo == null) {
                showStatusMessage("Failed to save captured camera scan.")
                return@launch
            }
            val backInfo = backRes?.getOrNull()

            val parsedKeyValues = repository.parseKeyValueLinesFromText(rawOcrText)
            val ocrFields = mutableListOf<DocumentFieldEntity>()
            ocrFields.addAll(frontInfo.extractedFields)
            parsedKeyValues.forEachIndexed { idx, (k, v) ->
                val isId = k.contains("no", ignoreCase = true) || k.contains("id", ignoreCase = true)
                ocrFields.add(
                    DocumentFieldEntity(
                        documentId = 0L,
                        label = k,
                        value = v,
                        isSensitive = isId,
                        isMaskedByDefault = isId,
                        source = FieldSource.OCR.name,
                        userConfirmed = false,
                        iconKey = if (isId) "id" else "info",
                        sortOrder = ocrFields.size + idx + 1
                    )
                )
            }

            val draftDoc = DocumentEntity(
                title = titleHint.ifBlank { subtypeHint.ifBlank { "Scanned Document" } },
                category = categoryHint,
                documentSubtype = subtypeHint,
                originalFileName = frontInfo.originalName,
                fileExtension = "jpg",
                frontFileRef = frontInfo.fileRef,
                backFileRef = backInfo?.fileRef ?: "",
                mimeType = "image/jpeg",
                fileSize = frontInfo.fileSize + (backInfo?.fileSize ?: 0L),
                pageCount = if (backInfo != null) 2 else 1,
                iconTypeName = DocumentCategoryGroup.fromCategoryName(categoryHint).name,
                isSamplePlaceholder = false
            )

            _uiState.update {
                it.copy(
                    editingDocument = draftDoc,
                    editingFields = ocrFields,
                    isOcrReviewMode = ocrFields.any { f -> !f.userConfirmed }
                )
            }
            showStatusMessage("Camera scan saved. Complete document details below.")
            navigateTo(SafeAreaScreen.DOCUMENT_EDITOR)
            refreshStorageStats()
        }
    }

    fun saveEditingDocument(
        updatedDoc: DocumentEntity,
        updatedFields: List<DocumentFieldEntity>,
        confirmOcrNow: Boolean
    ) {
        viewModelScope.launch {
            val finalFields = if (confirmOcrNow) {
                updatedFields.map { it.copy(userConfirmed = true) }
            } else {
                updatedFields
            }
            val finalDoc = updatedDoc.copy(
                title = updatedDoc.title.trim().ifEmpty {
                    updatedDoc.documentSubtype.ifEmpty { "Untitled Document" }
                },
                iconTypeName = DocumentCategoryGroup.fromCategoryName(updatedDoc.category).name,
                isSamplePlaceholder = false
            )
            val savedId = repository.saveDocumentWithFields(finalDoc, finalFields)
            _uiState.update {
                it.copy(
                    selectedDocumentId = savedId,
                    editingDocument = null,
                    editingFields = emptyList(),
                    isOcrReviewMode = false
                )
            }
            showStatusMessage("Saved \"${finalDoc.title}\"")
            navigateTo(SafeAreaScreen.DOCUMENT_DETAIL, clearBackStack = false)
            refreshStorageStats()
            if (_uiState.value.expiryNotificationsEnabled && finalDoc.expiryDate.isNotBlank()) {
                triggerExpiryNotificationCheck()
            }
        }
    }

    fun confirmOcrFieldsForCurrentDocument(documentId: Long) {
        viewModelScope.launch {
            repository.confirmAllOcrFields(documentId)
            showStatusMessage("Confirmed all extracted fields")
        }
    }

    fun rotateDocumentAttachment(doc: DocumentEntity, isBackSide: Boolean = false) {
        val targetRef = if (isBackSide) doc.backFileRef else doc.frontFileRef
        if (targetRef.isBlank()) return
        viewModelScope.launch {
            repository.rotateEncryptedImage(targetRef, 90f).onSuccess { newRef ->
                val updated = if (isBackSide) {
                    doc.copy(backFileRef = newRef, updatedAt = System.currentTimeMillis())
                } else {
                    doc.copy(frontFileRef = newRef, updatedAt = System.currentTimeMillis())
                }
                val fields = documentsWithFields.value.find { it.document.id == doc.id }?.fields.orEmpty()
                repository.saveDocumentWithFields(updated, fields)
                showStatusMessage("Rotated image by 90°")
            }.onFailure {
                showStatusMessage("Unable to rotate image attachment.")
            }
        }
    }

    fun toggleFavourite(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.toggleFavourite(doc)
            showStatusMessage(if (!doc.favourite) "Added to favourites" else "Removed from favourites")
        }
    }

    fun toggleArchive(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.toggleArchive(doc)
            showStatusMessage(if (!doc.archived) "Archived \"${doc.title}\"" else "Restored \"${doc.title}\"")
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            showStatusMessage("Deleted \"${doc.title}\"")
            if (_uiState.value.currentScreen == SafeAreaScreen.DOCUMENT_DETAIL) {
                navigateTo(SafeAreaScreen.HOME, clearBackStack = true)
            }
            refreshStorageStats()
        }
    }

    // --- Privacy-Conscious Clipboard Copier with 30s Auto-Clear Timer ---

    fun copyTextToClipboard(label: String, textToCopy: String) {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, textToCopy))

        statusDismissJob?.cancel()
        clipboardClearJob?.cancel()
        clipboardClearJob = viewModelScope.launch {
            var remaining = 30
            _uiState.update {
                it.copy(
                    statusBannerMessage = "Copied $label · Auto-clearing clipboard",
                    clipboardClearCountdownSec = remaining
                )
            }
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _uiState.update { it.copy(clipboardClearCountdownSec = remaining) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
            _uiState.update { it.copy(clipboardClearCountdownSec = 0) }
            showStatusMessage("Clipboard cleared for privacy")
        }
    }

    fun copyAllDocumentInfo(docWithFields: DocumentWithFields) {
        val doc = docWithFields.document
        val sb = StringBuilder()
        sb.appendLine(doc.title)
        if (doc.documentSubtype.isNotBlank()) sb.appendLine("Type: ${doc.documentSubtype}")
        sb.appendLine("Category: ${doc.category}")
        if (doc.issuer.isNotBlank()) sb.appendLine("Issuer: ${doc.issuer}")
        if (doc.country.isNotBlank()) sb.appendLine("Country/Region: ${doc.country}")
        docWithFields.fields.forEach { field ->
            sb.appendLine("${field.label}: ${field.value}")
        }
        if (doc.issueDate.isNotBlank()) sb.appendLine("Issue Date: ${doc.issueDate}")
        if (doc.expiryDate.isNotBlank()) sb.appendLine("Expiry Date: ${doc.expiryDate}")
        if (doc.notes.isNotBlank()) sb.appendLine("Notes: ${doc.notes}")
        copyTextToClipboard(doc.title, sb.toString().trim())
    }

    // --- Native Open in External Viewer & Share/Export ---

    fun openDocumentInExternalViewer(context: Context, doc: DocumentEntity) {
        if (doc.frontFileRef.isBlank()) {
            showStatusMessage("No file attached to open externally.")
            return
        }
        viewModelScope.launch {
            val ext = doc.fileExtension.ifBlank { "bin" }
            val tempFile = cryptoStorage.writeDecryptedTempFileForPreviewOrShare(doc.frontFileRef, ext)
            if (tempFile == null) {
                showStatusMessage("Unable to decrypt file for viewing.")
                return@launch
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, doc.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching {
                context.startActivity(Intent.createChooser(viewIntent, "Open ${doc.title}").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }.onFailure {
                showStatusMessage("No compatible external viewer app installed for .${ext} files.")
            }
        }
    }

    fun shareDocumentSummaryOrFile(context: Context, docWithFields: DocumentWithFields, includeAttachment: Boolean) {
        viewModelScope.launch {
            val doc = docWithFields.document
            val textSummary = buildString {
                appendLine(doc.title)
                appendLine("Category: ${doc.category}${if (doc.documentSubtype.isNotBlank()) " · ${doc.documentSubtype}" else ""}")
                docWithFields.fields.forEach { f ->
                    appendLine("${f.label}: ${f.value}")
                }
                if (doc.expiryDate.isNotBlank()) {
                    appendLine("Expiry Date: ${doc.expiryDate}")
                }
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_SUBJECT, doc.title)
                putExtra(Intent.EXTRA_TEXT, textSummary)
                type = "text/plain"
            }

            if (includeAttachment && doc.frontFileRef.isNotBlank()) {
                val ext = doc.fileExtension.ifBlank {
                    if (doc.mimeType.contains("pdf")) "pdf" else "jpg"
                }
                val tempFile = cryptoStorage.writeDecryptedTempFileForPreviewOrShare(doc.frontFileRef, ext)
                if (tempFile != null) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        tempFile
                    )
                    sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    sendIntent.type = doc.mimeType
                    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            val chooser = Intent.createChooser(sendIntent, "Share ${doc.title}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(chooser) }
            repository.logAudit("DOCUMENT_SHARED", "Shared \"${doc.title}\"")
        }
    }

    fun exportEncryptedVaultBackup(context: Context) {
        viewModelScope.launch {
            runCatching {
                val backupFile = repository.exportVaultBackupJson()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    backupFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "SAFEAREA Vault Backup")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Export SAFEAREA Backup").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                showStatusMessage("Backup exported")
                refreshStorageStats()
            }.onFailure {
                showStatusMessage("Could not export backup.")
            }
        }
    }

    // --- Storage & Expiry Helpers ---

    fun refreshStorageStats() {
        _uiState.update {
            it.copy(
                vaultStorageBytes = cryptoStorage.getVaultStorageBytes(),
                cacheStorageBytes = cryptoStorage.getCacheStorageBytes()
            )
        }
    }

    fun clearTemporaryCache() {
        cryptoStorage.clearExportCache()
        refreshStorageStats()
        showStatusMessage("Cleared temporary export cache")
    }

    fun wipeAllVaultData() {
        viewModelScope.launch {
            repository.wipeAllData()
            _uiState.update {
                VaultUiState(
                    currentScreen = SafeAreaScreen.WELCOME,
                    selectedTheme = cryptoStorage.selectedTheme,
                    appearanceMode = cryptoStorage.appearanceMode,
                    useSystemFont = cryptoStorage.useSystemFont,
                    fontScale = cryptoStorage.fontScale,
                    liveDateTimeText = computeLiveDateTime(getApplication())
                )
            }
            showStatusMessage("All local vault data has been permanently deleted.")
        }
    }

    fun triggerExpiryNotificationCheck() {
        if (!_uiState.value.expiryNotificationsEnabled) return
        val docs = documentsWithFields.value.map { it.document }
        val expiringCount = docs.count { doc ->
            val d = daysUntilExpiry(doc.expiryDate)
            d != null && d in 0L..45L
        }
        if (expiringCount <= 0) return

        val context = getApplication<Application>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "safearea_expiry_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Document Expiry Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Privacy-safe reminders when stored documents are nearing expiry"
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("SAFEAREA Expiry Reminder")
            .setContentText("$expiringCount stored document(s) in your vault are nearing expiry.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching { manager.notify(1001, notification) }
    }

    fun showStatusMessage(message: String) {
        statusDismissJob?.cancel()
        _uiState.update { it.copy(statusBannerMessage = message) }
        statusDismissJob = viewModelScope.launch {
            delay(3200L)
            if (_uiState.value.clipboardClearCountdownSec == 0) {
                _uiState.update { it.copy(statusBannerMessage = null) }
            }
        }
    }

    fun clearStatusMessage() {
        statusDismissJob?.cancel()
        _uiState.update { it.copy(statusBannerMessage = null) }
    }
}

fun daysUntilExpiry(expiryDateStr: String): Long? {
    if (expiryDateStr.isBlank()) return null
    return runCatching {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val target = sdf.parse(expiryDateStr) ?: return null
        val diffMs = target.time - System.currentTimeMillis()
        TimeUnit.MILLISECONDS.toDays(diffMs)
    }.getOrNull()
}
