package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.DocumentCategoryGroup
import com.example.data.DocumentEntity
import com.example.ui.SafeAreaScreen
import com.example.ui.VaultViewModel
import com.example.ui.components.SafeAreaBottomNavigationBar
import com.example.ui.screens.AddDocumentScreen
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.DocumentEditorScreen
import com.example.ui.screens.DocumentsLibraryScreen
import com.example.ui.screens.HomeVaultScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.SecuritySetupScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.VaultUnlockScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.screens.buildRequiredPermissionsArray
import com.example.ui.theme.LocalSafeAreaPalette
import com.example.ui.theme.SafeAreaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            SafeAreaApp()
        }
    }
}

@Composable
fun SafeAreaApp(
    viewModel: VaultViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val documents by viewModel.documentsWithFields.collectAsStateWithLifecycle()
    val auditEvents by viewModel.auditEvents.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val startupPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap ->
        viewModel.onPermissionsResult(resultMap)
    }

    LaunchedEffect(Unit) {
        runCatching {
            startupPermissionsLauncher.launch(buildRequiredPermissionsArray())
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onAppResumed()
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(
        enabled = uiState.isBulkSelectionActive ||
            (uiState.currentScreen != SafeAreaScreen.HOME &&
                uiState.currentScreen != SafeAreaScreen.WELCOME &&
                uiState.currentScreen != SafeAreaScreen.UNLOCK)
    ) {
        viewModel.navigateBack()
    }

    val fallbackPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.importDocumentFromUri(uri)
    }

    SafeAreaTheme(
        themeId = uiState.selectedTheme,
        appearanceMode = uiState.appearanceMode,
        useSystemFont = uiState.useSystemFont,
        fontScale = uiState.fontScale,
        cornerRadiusStyle = uiState.cornerRadiusStyle,
        cardDensityStyle = uiState.cardDensityStyle,
        borderContrastStyle = uiState.borderContrastStyle
    ) {
        val palette = LocalSafeAreaPalette.current
        val showBottomNav = uiState.currentScreen in listOf(
            SafeAreaScreen.HOME,
            SafeAreaScreen.DOCUMENTS,
            SafeAreaScreen.ADD_DOCUMENT,
            SafeAreaScreen.TOOLS,
            SafeAreaScreen.PROFILE
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = palette.canvas,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showBottomNav) {
                    SafeAreaBottomNavigationBar(
                        currentScreen = uiState.currentScreen,
                        onNavigate = { dest ->
                            viewModel.navigateTo(
                                target = dest,
                                clearBackStack = dest == SafeAreaScreen.HOME
                            )
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (uiState.currentScreen) {
                    SafeAreaScreen.WELCOME -> {
                        WelcomeScreen(
                            onGetStarted = {
                                viewModel.navigateTo(SafeAreaScreen.SECURITY_SETUP)
                            }
                        )
                    }

                    SafeAreaScreen.SECURITY_SETUP -> {
                        SecuritySetupScreen(
                            selectedMethod = uiState.primaryLockMethod,
                            onSelectMethod = { viewModel.selectLockMethodDraft(it) },
                            onSetupPinCompleted = { pin, bio, hint ->
                                viewModel.completeSecuritySetupWithPin(pin, bio, hint)
                            },
                            onSetupPatternCompleted = { pattern, fallbackPin, bio ->
                                viewModel.completeSecuritySetupWithPattern(pattern, fallbackPin, bio)
                            },
                            onSkipSetup = { viewModel.skipLockSetupForNow() },
                            onBack = { viewModel.navigateBack() }
                        )
                    }

                    SafeAreaScreen.UNLOCK -> {
                        VaultUnlockScreen(
                            primaryMethod = uiState.primaryLockMethod,
                            biometricEnabled = uiState.biometricEnabled,
                            hasPinSet = uiState.hasPinSet,
                            hasPatternSet = uiState.hasPatternSet,
                            recoveryHint = viewModel.cryptoStorage.recoveryPhraseHint,
                            lockoutSeconds = uiState.lockoutRemainingSeconds,
                            errorMessage = uiState.unlockErrorMessage,
                            onUnlockWithPin = { viewModel.unlockWithPin(it) },
                            onUnlockWithPattern = { viewModel.unlockWithPattern(it) },
                            onBiometricSuccess = { viewModel.onBiometricUnlockSucceeded() },
                            onBiometricFailed = { viewModel.onBiometricUnlockFailed(it) },
                            onResetVaultAfterConfirmation = { viewModel.wipeAllVaultData() }
                        )
                    }

                    SafeAreaScreen.HOME -> {
                        HomeVaultScreen(
                            uiState = uiState,
                            documents = documents,
                            onOpenDocument = { docId -> viewModel.openDocumentDetails(docId) },
                            onSeeAllDocuments = {
                                viewModel.selectCategoryFilter("All")
                                viewModel.navigateTo(SafeAreaScreen.DOCUMENTS)
                            },
                            onOpenCategory = { categoryTitle ->
                                viewModel.selectCategoryFilter(categoryTitle)
                                viewModel.navigateTo(SafeAreaScreen.DOCUMENTS)
                            },
                            onAddDocument = { viewModel.navigateTo(SafeAreaScreen.ADD_DOCUMENT) },
                            onQuickScan = { viewModel.navigateTo(SafeAreaScreen.CAMERA_SCANNER) },
                            onOpenSettings = { viewModel.navigateTo(SafeAreaScreen.PROFILE) },
                            onLockVaultNow = { viewModel.lockVaultNow() },
                            onConfigureSecurity = { viewModel.navigateTo(SafeAreaScreen.SECURITY_SETUP) },
                            onSaveIceProfile = { viewModel.saveEmergencyIceProfile(it) }
                        )
                    }

                    SafeAreaScreen.DOCUMENTS -> {
                        DocumentsLibraryScreen(
                            uiState = uiState,
                            documents = documents,
                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                            onSelectCategory = { viewModel.selectCategoryFilter(it) },
                            onSelectFormatGroup = { viewModel.selectFormatGroupFilter(it) },
                            onSelectExpiryFilter = { viewModel.selectExpiryFilter(it) },
                            onToggleFavourites = { viewModel.toggleFavouritesFilter() },
                            onToggleRecentlyOpened = { viewModel.toggleRecentlyOpenedFilter() },
                            onToggleArchived = { viewModel.toggleArchivedFilter() },
                            onSortChange = { viewModel.setSortOption(it) },
                            onToggleGridView = { viewModel.toggleGridView() },
                            onToggleBulkSelection = { viewModel.toggleBulkDocumentSelection(it) },
                            onSelectAllVisible = { viewModel.selectAllVisibleDocuments(it) },
                            onClearBulkSelection = { viewModel.clearBulkSelection() },
                            onBulkDeleteSelected = { viewModel.bulkDeleteSelectedDocuments() },
                            onBulkArchiveSelected = { viewModel.bulkArchiveSelectedDocuments(it) },
                            onBulkMoveCategorySelected = { viewModel.bulkMoveCategorySelectedDocuments(it) },
                            onCompilePdfPacketSelected = { title ->
                                viewModel.compileAndShareDocumentPacketPdf(context, title, uiState.selectedBulkDocIds)
                            },
                            onAddCustomCategory = { viewModel.addCustomCategory(it) },
                            onOpenDocument = { viewModel.openDocumentDetails(it) },
                            onAddDocument = { viewModel.navigateTo(SafeAreaScreen.ADD_DOCUMENT) }
                        )
                    }

                    SafeAreaScreen.DOCUMENT_DETAIL -> {
                        val selectedDoc = documents.find { it.document.id == uiState.selectedDocumentId }
                            ?: documents.firstOrNull()
                        DocumentDetailScreen(
                            docWithFields = selectedDoc,
                            allCategories = uiState.allCategories,
                            cryptoStorage = viewModel.cryptoStorage,
                            repository = viewModel.repository,
                            onBack = { viewModel.navigateBack() },
                            onEditDocument = { viewModel.startEditingExistingDocument(it) },
                            onRenameDocument = { doc, newTitle -> viewModel.renameDocument(doc, newTitle) },
                            onDuplicateDocument = { viewModel.duplicateDocument(it) },
                            onMoveCategory = { doc, newCat -> viewModel.moveDocumentCategory(doc, newCat) },
                            onToggleFavourite = { viewModel.toggleFavourite(it) },
                            onToggleArchive = { viewModel.toggleArchive(it) },
                            onRotateAttachment = { doc, isBack -> viewModel.rotateDocumentAttachment(doc, isBack) },
                            onDeleteDocument = { viewModel.deleteDocument(it) },
                            onConfirmOcrFields = { viewModel.confirmOcrFieldsForCurrentDocument(it) },
                            onCopySingleField = { label, value -> viewModel.copyTextToClipboard(label, value) },
                            onCopyAllFields = { viewModel.copyAllDocumentInfo(it) },
                            onOpenExternalViewer = { doc -> viewModel.openDocumentInExternalViewer(context, doc) },
                            onShareDocument = { docWithFields, includeFile ->
                                viewModel.shareDocumentSummaryOrFile(context, docWithFields, includeFile)
                            }
                        )
                    }

                    SafeAreaScreen.ADD_DOCUMENT -> {
                        AddDocumentScreen(
                            onBack = { viewModel.navigateBack() },
                            onOpenCameraScanner = { viewModel.navigateTo(SafeAreaScreen.CAMERA_SCANNER) },
                            onImportUri = { uri -> viewModel.importDocumentFromUri(uri) },
                            onEnterManually = { presetCat, presetSub ->
                                viewModel.startManualAddDocument(presetCat, presetSub)
                            }
                        )
                    }

                    SafeAreaScreen.CAMERA_SCANNER -> {
                        CameraScannerScreen(
                            onBack = { viewModel.navigateBack() },
                            onFallbackImportPhoto = {
                                fallbackPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onFallbackManualEntry = {
                                viewModel.startManualAddDocument()
                            },
                            onScanCompleted = { front, back, titleHint, catHint, subtypeHint, ocrText ->
                                viewModel.onCameraScanCaptured(front, back, titleHint, catHint, subtypeHint, ocrText)
                            }
                        )
                    }

                    SafeAreaScreen.DOCUMENT_EDITOR -> {
                        val draftDoc = uiState.editingDocument ?: DocumentEntity(
                            title = "",
                            category = DocumentCategoryGroup.PERSONAL.title,
                            documentSubtype = DocumentCategoryGroup.PERSONAL.subtypes.first(),
                            issuer = "",
                            country = ""
                        )
                        DocumentEditorScreen(
                            initialDocument = draftDoc,
                            initialFields = uiState.editingFields,
                            allCategories = uiState.allCategories,
                            isOcrReviewMode = uiState.isOcrReviewMode,
                            onAttachFrontOrBackUri = { uri, isBack ->
                                viewModel.importDocumentFromUri(uri, isBack)
                            },
                            onSave = { doc, fields, confirmOcr ->
                                viewModel.saveEditingDocument(doc, fields, confirmOcr)
                            },
                            onCancel = { viewModel.navigateBack() }
                        )
                    }

                    SafeAreaScreen.TOOLS -> {
                        ToolsScreen(
                            uiState = uiState,
                            documents = documents,
                            onOpenScanner = { viewModel.navigateTo(SafeAreaScreen.CAMERA_SCANNER) },
                            onOpenDocument = { viewModel.openDocumentDetails(it) },
                            onCopyCustomSelection = { label, text ->
                                viewModel.copyTextToClipboard(label, text)
                            },
                            onClearCache = { viewModel.clearTemporaryCache() },
                            onExportBackup = { viewModel.exportEncryptedVaultBackup(context) },
                            onExportInFormat = { fmt -> viewModel.shareVaultExportInFormat(context, fmt) },
                            onSaveExportToUri = { fmt, uri -> viewModel.saveVaultExportToUri(context, fmt, uri) },
                            onCompilePacketPdf = { title, ids ->
                                viewModel.compileAndShareDocumentPacketPdf(context, title, ids)
                            },
                            onSaveEmergencyIceProfile = { viewModel.saveEmergencyIceProfile(it) }
                        )
                    }

                    SafeAreaScreen.PROFILE -> {
                        ProfileSettingsScreen(
                            uiState = uiState,
                            auditEvents = auditEvents,
                            onSelectTheme = { viewModel.setTheme(it) },
                            onSelectAppearanceMode = { viewModel.setAppearanceMode(it) },
                            onToggleUseSystemFont = { viewModel.setUseSystemFont(it) },
                            onSelectFontScale = { viewModel.setFontScale(it) },
                            onSetCornerRadiusStyle = { viewModel.setCornerRadiusStyle(it) },
                            onSetCardDensityStyle = { viewModel.setCardDensityStyle(it) },
                            onSetBorderContrastStyle = { viewModel.setBorderContrastStyle(it) },
                            onSetCustomVaultBranding = { title, tagline ->
                                viewModel.updateCustomVaultBranding(title, tagline)
                            },
                            onSetShowSecurityScoreCard = { viewModel.setShowSecurityScoreCard(it) },
                            onSetShowCategoryGridOnHome = { viewModel.setShowCategoryGridOnHome(it) },
                            onAddCustomCategory = { viewModel.addCustomCategory(it) },
                            onPermissionsUpdated = { viewModel.onPermissionsResult(it) },
                            onFolderAccessGranted = { viewModel.setGrantedFolderUri(it) },
                            onToggleAppLock = { viewModel.setAppLockEnabled(it) },
                            onConfigureLockMethod = { viewModel.navigateTo(SafeAreaScreen.SECURITY_SETUP) },
                            onLockVaultNow = { viewModel.lockVaultNow() },
                            onChangeAutoLockTimeout = { viewModel.setAutoLockTimeout(it) },
                            onToggleLockOnBackground = { viewModel.setLockOnBackground(it) },
                            onToggleHideInAppSwitcher = { viewModel.setHideInAppSwitcher(it) },
                            onToggleExpiryNotifications = { viewModel.setExpiryNotificationsEnabled(it) },
                            onClearCache = { viewModel.clearTemporaryCache() },
                            onExportBackup = { viewModel.exportEncryptedVaultBackup(context) },
                            onExportInFormat = { fmt -> viewModel.shareVaultExportInFormat(context, fmt) },
                            onSaveExportToUri = { fmt, uri -> viewModel.saveVaultExportToUri(context, fmt, uri) },
                            onWipeAllData = { viewModel.wipeAllVaultData() }
                        )
                    }
                }

                // Auto-dismissing Status / Clipboard Privacy Auto-Clear Banner
                val bannerMsg = uiState.statusBannerMessage
                AnimatedVisibility(
                    visible = !bannerMsg.isNullOrBlank(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.ink,
                        border = BorderStroke(1.dp, palette.secondary),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.clearStatusMessage() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = palette.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = buildString {
                                    append(bannerMsg ?: "")
                                    if (uiState.clipboardClearCountdownSec > 0) {
                                        append(" (${uiState.clipboardClearCountdownSec}s)")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.surface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearStatusMessage() },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss notification",
                                    tint = palette.surface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
