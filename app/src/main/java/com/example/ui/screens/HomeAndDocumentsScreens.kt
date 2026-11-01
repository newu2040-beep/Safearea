package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DocumentCategoryGroup
import com.example.data.DocumentWithFields
import com.example.data.LockMethod
import com.example.data.SupportedFileFormatGroup
import com.example.ui.ExpiryFilter
import com.example.ui.SortOption
import com.example.ui.VaultUiState
import com.example.ui.components.DocumentCategoryIconBox
import com.example.ui.components.SafeAreaDocumentRowCard
import com.example.ui.components.SafeAreaOutlinedPillButton
import com.example.ui.components.SafeAreaPrimaryButton
import com.example.ui.components.SafeAreaShieldLogo
import com.example.ui.components.formatCompactFileSize
import com.example.ui.daysUntilExpiry
import com.example.ui.theme.LocalSafeAreaPalette

/**
 * Home Vault Dashboard:
 * - Responsive Apple-inspired layout adapting to phone & tablet widths
 * - Live device-local date & time
 * - Vault security status card
 * - Interactive 9 Document Category Groups grid with individual icons and live document counters
 * - Expiring / Expired alerts
 * - Recently Opened & My Documents list (zero demo/placeholder data)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeVaultScreen(
    uiState: VaultUiState,
    documents: List<DocumentWithFields>,
    onOpenDocument: (Long) -> Unit,
    onSeeAllDocuments: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onAddDocument: () -> Unit,
    onQuickScan: () -> Unit,
    onOpenSettings: () -> Unit,
    onLockVaultNow: () -> Unit,
    onConfigureSecurity: () -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    val activeDocs = remember(documents) { documents.filter { !it.document.archived } }
    val expiringDocs = remember(activeDocs) {
        activeDocs.filter {
            val d = daysUntilExpiry(it.document.expiryDate)
            d != null && d <= 45L
        }.sortedBy { it.document.expiryDate }
    }
    val recentlyOpenedDocs = remember(activeDocs) {
        activeDocs.filter { it.document.lastOpenedAt > 0L }
            .sortedByDescending { it.document.lastOpenedAt }
            .take(3)
    }

    val categoryCounts = remember(activeDocs) {
        DocumentCategoryGroup.entries.associateWith { group ->
            activeDocs.count { it.document.categoryGroup == group }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
    ) {
        val isWideScreen = maxWidth >= 600.dp
        val maxContentWidth = if (isWideScreen) 720.dp else 540.dp
        val categoryColumns = if (isWideScreen) 3 else 3

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxContentWidth)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        SafeAreaShieldLogo(size = 50.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SAFEAREA",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = palette.ink
                            )
                            Text(
                                text = "Personal Document Vault",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.inkMuted
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onLockVaultNow,
                            modifier = Modifier.testTag("lock_now_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.appLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock vault",
                                tint = palette.ink
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = palette.ink,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = uiState.liveDateTimeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.inkMuted,
                    modifier = Modifier.testTag("live_date_time_text")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Vault Security Status Banner
                val statusProtected = uiState.appLockEnabled || uiState.hasPatternSet || uiState.hasPinSet
                val protectionLabel = when {
                    !statusProtected -> "Tap to configure biometric, PIN, or pattern lock"
                    uiState.biometricEnabled -> "Protected with Biometrics & PIN · AES-256 Keystore"
                    uiState.primaryLockMethod == LockMethod.PATTERN -> "Protected with Pattern Lock · AES-256 Keystore"
                    else -> "Protected with ${uiState.primaryLockMethod.title} · AES-256 Keystore"
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            if (statusProtected) onLockVaultNow() else onConfigureSecurity()
                        }
                        .testTag("vault_status_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = palette.successContainer,
                    border = BorderStroke(1.1.dp, palette.success.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(palette.success),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (statusProtected) "Vault is Secure" else "Secure Your Vault",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.onSuccessContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = protectionLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.onSuccessContainer.copy(alpha = 0.85f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Vault security details",
                            tint = palette.onSuccessContainer
                        )
                    }
                }

                // Expiring / Expired Document Alert Banner
                if (expiringDocs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val firstExpiring = expiringDocs.first().document
                    val days = daysUntilExpiry(firstExpiring.expiryDate) ?: 0L
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenDocument(firstExpiring.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = palette.warningContainer,
                        border = BorderStroke(1.dp, palette.warning)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFF8A5A00),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (days < 0) {
                                        "${firstExpiring.title} has expired"
                                    } else {
                                        "${firstExpiring.title} expires in $days day(s)"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF5C3B00)
                                )
                                Text(
                                    text = "Expiry: ${firstExpiring.expiryDate} · ${expiringDocs.size} alert(s) active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF7A4F00)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF5C3B00)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Action Bar (Scan with Camera, Import Any File, Search Vault)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickDashboardTile(
                        title = "Scan Camera",
                        icon = Icons.Default.CameraAlt,
                        onClick = onQuickScan,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_quick_scan_button")
                    )
                    QuickDashboardTile(
                        title = "Import File",
                        icon = Icons.Default.UploadFile,
                        onClick = onAddDocument,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_quick_import_button")
                    )
                    QuickDashboardTile(
                        title = "Search Vault",
                        icon = Icons.Default.Search,
                        onClick = onSeeAllDocuments,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_quick_search_button")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Document Categories Section (All 9 Category Groups with Individual Icons & Live Counters)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Document Categories",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.ink
                    )
                    Text(
                        text = "${activeDocs.size} total stored",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.inkMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = categoryColumns
                ) {
                    DocumentCategoryGroup.entries.forEach { group ->
                        val count = categoryCounts[group] ?: 0
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenCategory(group.title) }
                                .testTag("category_card_${group.name.lowercase()}"),
                            shape = RoundedCornerShape(16.dp),
                            color = palette.surface,
                            border = BorderStroke(1.dp, palette.border.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                DocumentCategoryIconBox(
                                    categoryGroup = group,
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = group.shortTitle,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = palette.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (count == 1) "1 doc" else "$count docs",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (count > 0) palette.primary else palette.inkMuted
                                )
                            }
                        }
                    }
                }

                // Recently Opened Section (when documents have been opened)
                if (recentlyOpenedDocs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Recently Opened",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.ink
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentlyOpenedDocs.forEach { item ->
                            val doc = item.document
                            SafeAreaDocumentRowCard(
                                title = doc.title,
                                subtitle = doc.documentSubtype.ifBlank { doc.category },
                                categoryGroup = doc.categoryGroup,
                                formatBadge = doc.displayFormatBadge,
                                fileSizeText = formatCompactFileSize(doc.fileSize),
                                isFavourite = doc.favourite,
                                onClick = { onOpenDocument(doc.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // My Documents Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Documents (${activeDocs.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.ink
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onSeeAllDocuments)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("see_all_documents_button")
                    ) {
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.ink
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = palette.ink,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activeDocs.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = palette.surface,
                        border = BorderStroke(1.1.dp, palette.border.copy(alpha = 0.28f))
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Documents Stored Yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.ink
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Scan IDs, passports, certificates, or import PDF, Word, Excel, PowerPoint, and image files into your encrypted on-device vault.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.inkMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            SafeAreaPrimaryButton(
                                text = "Add or Scan Document",
                                leadingIcon = Icons.Default.Add,
                                onClick = onAddDocument,
                                modifier = Modifier.testTag("empty_state_add_button")
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        activeDocs.take(8).forEach { item ->
                            val doc = item.document
                            val daysLeft = daysUntilExpiry(doc.expiryDate)
                            val expiryBadge = when {
                                daysLeft == null -> null
                                daysLeft < 0L -> "Expired"
                                daysLeft <= 45L -> "${daysLeft}d left"
                                else -> null
                            }
                            val subtitleText = buildString {
                                append(doc.documentSubtype.ifBlank { doc.category })
                                if (doc.country.isNotBlank()) {
                                    append(" · ")
                                    append(doc.country)
                                }
                            }
                            SafeAreaDocumentRowCard(
                                title = doc.title,
                                subtitle = subtitleText,
                                categoryGroup = doc.categoryGroup,
                                formatBadge = doc.displayFormatBadge,
                                fileSizeText = formatCompactFileSize(doc.fileSize),
                                expiryBadgeText = expiryBadge,
                                isExpiringSoon = daysLeft != null && daysLeft <= 45L,
                                isFavourite = doc.favourite,
                                onClick = { onOpenDocument(doc.id) },
                                modifier = Modifier.testTag("doc_card_${doc.id}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SafeAreaOutlinedPillButton(
                    text = "Add New Document",
                    leadingIcon = Icons.Default.Add,
                    onClick = onAddDocument,
                    modifier = Modifier.testTag("home_add_document_button")
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun QuickDashboardTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalSafeAreaPalette.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = palette.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Full Documents Library Screen:
 * - Search by name, category, subtype, file format/extension, tags, or field values
 * - Filter by Category, File Format, Expiry Status, Favourites, Recently Opened, Archived
 * - Sort by Modified Date, Date Added, Name (A–Z), File Size, Expiry Date
 * - Multi-Document Selection & Bulk Actions (Bulk Move Category, Bulk Archive/Restore, Bulk Delete)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentsLibraryScreen(
    uiState: VaultUiState,
    documents: List<DocumentWithFields>,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectFormatGroup: (SupportedFileFormatGroup) -> Unit,
    onSelectExpiryFilter: (ExpiryFilter) -> Unit,
    onToggleFavourites: () -> Unit,
    onToggleRecentlyOpened: () -> Unit,
    onToggleArchived: () -> Unit,
    onSortChange: (SortOption) -> Unit,
    onToggleGridView: () -> Unit,
    onToggleBulkSelection: (Long) -> Unit,
    onSelectAllVisible: (List<Long>) -> Unit,
    onClearBulkSelection: () -> Unit,
    onBulkDeleteSelected: () -> Unit,
    onBulkArchiveSelected: (Boolean) -> Unit,
    onBulkMoveCategorySelected: (String) -> Unit,
    onAddCustomCategory: (String) -> Unit,
    onOpenDocument: (Long) -> Unit,
    onAddDocument: () -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var showBulkMoveDialog by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    val filteredDocs = remember(
        documents,
        uiState.searchQuery,
        uiState.selectedCategory,
        uiState.selectedFormatGroup,
        uiState.expiryFilter,
        uiState.filterFavouritesOnly,
        uiState.filterRecentlyOpenedOnly,
        uiState.filterArchivedOnly,
        uiState.sortOption
    ) {
        val q = uiState.searchQuery.trim().lowercase()
        documents.filter { item ->
            val doc = item.document
            val matchesArchive = if (uiState.filterArchivedOnly) doc.archived else !doc.archived
            val matchesCategory = uiState.selectedCategory == "All" ||
                doc.category.equals(uiState.selectedCategory, ignoreCase = true) ||
                doc.categoryGroup.title.equals(uiState.selectedCategory, ignoreCase = true) ||
                doc.categoryGroup.shortTitle.equals(uiState.selectedCategory, ignoreCase = true) ||
                doc.documentSubtype.equals(uiState.selectedCategory, ignoreCase = true)
            val matchesFormat = uiState.selectedFormatGroup == SupportedFileFormatGroup.ALL ||
                doc.formatGroup == uiState.selectedFormatGroup
            val daysLeft = daysUntilExpiry(doc.expiryDate)
            val matchesExpiry = when (uiState.expiryFilter) {
                ExpiryFilter.ALL -> true
                ExpiryFilter.ACTIVE -> daysLeft == null || daysLeft > 45L
                ExpiryFilter.EXPIRING_SOON -> daysLeft != null && daysLeft in 0L..45L
                ExpiryFilter.EXPIRED -> daysLeft != null && daysLeft < 0L
            }
            val matchesFav = !uiState.filterFavouritesOnly || doc.favourite
            val matchesRecent = !uiState.filterRecentlyOpenedOnly || doc.lastOpenedAt > 0L
            val matchesQuery = q.isEmpty() ||
                doc.title.lowercase().contains(q) ||
                doc.category.lowercase().contains(q) ||
                doc.documentSubtype.lowercase().contains(q) ||
                doc.fileExtension.lowercase().contains(q) ||
                doc.originalFileName.lowercase().contains(q) ||
                doc.country.lowercase().contains(q) ||
                doc.issuer.lowercase().contains(q) ||
                doc.tagsCsv.lowercase().contains(q) ||
                item.fields.any { f -> f.label.lowercase().contains(q) || f.value.lowercase().contains(q) }

            matchesArchive && matchesCategory && matchesFormat && matchesExpiry && matchesFav && matchesRecent && matchesQuery
        }.let { list ->
            when (uiState.sortOption) {
                SortOption.MODIFIED_DATE -> list.sortedByDescending { it.document.updatedAt }
                SortOption.ADDED_DATE -> list.sortedByDescending { it.document.createdAt }
                SortOption.TITLE -> list.sortedBy { it.document.title.lowercase() }
                SortOption.FILE_SIZE -> list.sortedByDescending { it.document.fileSize }
                SortOption.EXPIRY_DATE -> list.sortedWith(
                    compareBy<DocumentWithFields> { it.document.expiryDate.isBlank() }
                        .thenBy { it.document.expiryDate }
                )
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
    ) {
        val isWideScreen = maxWidth >= 600.dp
        val maxContentWidth = if (isWideScreen) 720.dp else 560.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxContentWidth)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isBulkSelectionActive) {
                                "${uiState.selectedBulkDocIds.size} Selected"
                            } else {
                                "All Documents"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = palette.ink
                        )
                        Text(
                            text = "${filteredDocs.size} matching document(s) · Long-press to multi-select",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkMuted
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (filteredDocs.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    if (uiState.isBulkSelectionActive) {
                                        onClearBulkSelection()
                                    } else {
                                        onSelectAllVisible(filteredDocs.map { it.document.id })
                                    }
                                },
                                modifier = Modifier.testTag("toggle_bulk_select_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "Select documents",
                                    tint = if (uiState.isBulkSelectionActive) palette.primary else palette.ink
                                )
                            }
                        }
                        IconButton(onClick = onToggleGridView) {
                            Icon(
                                imageVector = if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle grid or list view",
                                tint = palette.ink
                            )
                        }
                    }
                }

                // Multi-Document Bulk Actions Bar
                if (uiState.isBulkSelectionActive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surface,
                        border = BorderStroke(1.2.dp, palette.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onSelectAllVisible(filteredDocs.map { it.document.id }) }
                            ) {
                                Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("All (${filteredDocs.size})")
                            }

                            Row {
                                IconButton(onClick = { showBulkMoveDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.DriveFileMove,
                                        contentDescription = "Move selected to category",
                                        tint = palette.ink
                                    )
                                }
                                IconButton(
                                    onClick = { onBulkArchiveSelected(!uiState.filterArchivedOnly) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Archive,
                                        contentDescription = "Archive or restore selected",
                                        tint = palette.ink
                                    )
                                }
                                IconButton(onClick = { showBulkDeleteDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete selected",
                                        tint = palette.danger
                                    )
                                }
                                TextButton(onClick = onClearBulkSelection) {
                                    Text("Done", color = palette.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search name, category, format (.pdf, .docx), or tags...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = palette.inkMuted)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("documents_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 1. Category Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf("All") + uiState.allCategories
                    categories.forEach { cat ->
                        val selected = uiState.selectedCategory.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCategory(cat) },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.ink,
                                selectedLabelColor = palette.surface
                            )
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = { showNewCategoryDialog = true },
                        label = { Text("+ Category") }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. File Format Filter Row (PDF, Images, Word, Excel, PowerPoint, Text/Data)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SupportedFileFormatGroup.entries.forEach { formatGroup ->
                        val selected = uiState.selectedFormatGroup == formatGroup
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectFormatGroup(formatGroup) },
                            label = { Text(formatGroup.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3. Status, Expiry & Sort Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.filterFavouritesOnly,
                        onClick = onToggleFavourites,
                        label = { Text("Favourites") },
                        leadingIcon = {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = uiState.filterRecentlyOpenedOnly,
                        onClick = onToggleRecentlyOpened,
                        label = { Text("Recently Opened") },
                        leadingIcon = {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                    ExpiryFilter.entries.forEach { expFilter ->
                        if (expFilter != ExpiryFilter.ALL) {
                            FilterChip(
                                selected = uiState.expiryFilter == expFilter,
                                onClick = {
                                    onSelectExpiryFilter(
                                        if (uiState.expiryFilter == expFilter) ExpiryFilter.ALL else expFilter
                                    )
                                },
                                label = { Text(expFilter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    FilterChip(
                        selected = uiState.filterArchivedOnly,
                        onClick = onToggleArchived,
                        label = { Text("Archived") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                    SortOption.entries.forEach { sort ->
                        FilterChip(
                            selected = uiState.sortOption == sort,
                            onClick = { onSortChange(sort) },
                            label = { Text("Sort: ${sort.label}") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredDocs.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surface,
                        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.28f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Matching Documents",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.ink
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try clearing your active filters or import a new document.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.inkMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            SafeAreaOutlinedPillButton(
                                text = "Add New Document",
                                leadingIcon = Icons.Default.Add,
                                onClick = onAddDocument
                            )
                        }
                    }
                } else if (uiState.isGridView) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = if (isWideScreen) 3 else 2
                    ) {
                        filteredDocs.forEach { item ->
                            val doc = item.document
                            val isSelected = doc.id in uiState.selectedBulkDocIds
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        if (uiState.isBulkSelectionActive) {
                                            onToggleBulkSelection(doc.id)
                                        } else {
                                            onOpenDocument(doc.id)
                                        }
                                    },
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSelected) palette.primary.copy(alpha = 0.1f) else palette.surface,
                                border = BorderStroke(
                                    if (isSelected) 1.8.dp else 1.1.dp,
                                    if (isSelected) palette.primary else palette.border.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DocumentCategoryIconBox(categoryGroup = doc.categoryGroup, size = 40.dp)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = palette.canvas
                                        ) {
                                            Text(
                                                text = doc.displayFormatBadge,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = palette.ink,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = doc.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = palette.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = doc.documentSubtype.ifBlank { doc.category },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.inkMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sizeStr = formatCompactFileSize(doc.fileSize)
                                    if (sizeStr.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = sizeStr,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.inkMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredDocs.forEach { item ->
                            val doc = item.document
                            val daysLeft = daysUntilExpiry(doc.expiryDate)
                            val badge = when {
                                daysLeft == null -> null
                                daysLeft < 0L -> "Expired"
                                daysLeft <= 45L -> "${daysLeft}d left"
                                else -> null
                            }
                            val isSelected = doc.id in uiState.selectedBulkDocIds
                            SafeAreaDocumentRowCard(
                                title = doc.title,
                                subtitle = "${doc.documentSubtype.ifBlank { doc.category }} · ${doc.category}",
                                categoryGroup = doc.categoryGroup,
                                formatBadge = doc.displayFormatBadge,
                                fileSizeText = formatCompactFileSize(doc.fileSize),
                                expiryBadgeText = badge,
                                isExpiringSoon = daysLeft != null && daysLeft <= 45L,
                                isFavourite = doc.favourite,
                                isSelectionMode = uiState.isBulkSelectionActive,
                                isSelected = isSelected,
                                onClick = {
                                    if (uiState.isBulkSelectionActive) {
                                        onToggleBulkSelection(doc.id)
                                    } else {
                                        onOpenDocument(doc.id)
                                    }
                                },
                                onLongClick = { onToggleBulkSelection(doc.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            containerColor = palette.surface,
            title = { Text("Create Custom Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name (e.g. Property, Certificates)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCustomCategory(newCategoryName)
                            newCategoryName = ""
                            showNewCategoryDialog = false
                        }
                    }
                ) {
                    Text("Add", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) {
                    Text("Cancel", color = palette.inkMuted)
                }
            }
        )
    }

    if (showBulkMoveDialog) {
        AlertDialog(
            onDismissRequest = { showBulkMoveDialog = false },
            containerColor = palette.surface,
            title = { Text("Move ${uiState.selectedBulkDocIds.size} Document(s) To") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.allCategories.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.canvas,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showBulkMoveDialog = false
                                    onBulkMoveCategorySelected(cat)
                                }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.ink,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBulkMoveDialog = false }) {
                    Text("Cancel", color = palette.inkMuted)
                }
            }
        )
    }

    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            containerColor = palette.surface,
            title = { Text("Delete ${uiState.selectedBulkDocIds.size} Selected Document(s)?") },
            text = {
                Text(
                    "This will permanently delete the selected documents and their encrypted files from your device."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBulkDeleteDialog = false
                        onBulkDeleteSelected()
                    }
                ) {
                    Text("Delete Permanently", color = palette.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) {
                    Text("Cancel", color = palette.inkMuted)
                }
            }
        )
    }
}
