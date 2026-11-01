package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DocumentWithFields
import com.example.data.LockMethod
import com.example.ui.SafeAreaScreen
import com.example.ui.SortOption
import com.example.ui.VaultUiState
import com.example.ui.components.DocumentCategoryIconBox
import com.example.ui.components.SafeAreaDocumentRowCard
import com.example.ui.components.SafeAreaOutlinedPillButton
import com.example.ui.components.SafeAreaShieldLogo
import com.example.ui.daysUntilExpiry
import com.example.ui.theme.LocalSafeAreaPalette

/**
 * Screen 3 of the SAFEAREA 5-screen mockup: Home Vault.
 */
@Composable
fun HomeVaultScreen(
    uiState: VaultUiState,
    documents: List<DocumentWithFields>,
    onOpenDocument: (Long) -> Unit,
    onSeeAllDocuments: () -> Unit,
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
            d != null && d <= 45
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            // Top bar with Settings gear icon at top-right and SAFEAREA logo + title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SafeAreaShieldLogo(size = 54.dp)
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
                            text = "Secure. Always.",
                            style = MaterialTheme.typography.bodyMedium,
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
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Live device-local date & time bar (Required by Section 5.3 & Section 9 of PRD)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.liveDateTimeText,
                style = MaterialTheme.typography.labelMedium,
                color = palette.inkMuted,
                modifier = Modifier.testTag("live_date_time_text")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Vault Status Card matching Screen 3 of mockup ("Vault is Secure / Protected with pattern lock")
            val statusProtected = uiState.appLockEnabled || uiState.hasPatternSet || uiState.hasPinSet
            val protectionLabel = when {
                !statusProtected -> "Tap to configure PIN, pattern, or biometric lock"
                uiState.primaryLockMethod == LockMethod.PATTERN -> "Protected with pattern lock · AES-256 Keystore"
                uiState.biometricEnabled -> "Protected with biometrics & PIN · AES-256 Keystore"
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
                border = BorderStroke(1.1.dp, palette.success.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.success),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (statusProtected) "Vault is Secure" else "Vault is Unlocked",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.onSuccessContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = protectionLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.onSuccessContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Vault security details",
                        tint = palette.onSuccessContainer
                    )
                }
            }

            // Expiry Reminder Card (if any document is nearing expiry)
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
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                                text = if (days < 0) "${firstExpiring.title} has expired" else "${firstExpiring.title} expires in $days days",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF5C3B00)
                            )
                            Text(
                                text = "Expiry date: ${firstExpiring.expiryDate} · Tap to view",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7A4F00)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // My Documents Section Header matching Screen 3 of mockup
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
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Your vault is empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.ink
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add your ID card, driving license, passport, or certificates below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkMuted
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activeDocs.take(6).forEach { item ->
                        val doc = item.document
                        val daysLeft = daysUntilExpiry(doc.expiryDate)
                        val expiryBadge = when {
                            daysLeft == null -> null
                            daysLeft < 0 -> "Expired"
                            daysLeft <= 45 -> "${daysLeft}d left"
                            else -> null
                        }
                        SafeAreaDocumentRowCard(
                            title = doc.title,
                            subtitle = doc.country.ifBlank { doc.category },
                            iconType = doc.iconType,
                            expiryBadgeText = expiryBadge,
                            isExpiringSoon = daysLeft != null && daysLeft <= 45,
                            onClick = { onOpenDocument(doc.id) },
                            modifier = Modifier.testTag("doc_card_${doc.title.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Outlined `+ Add Document` pill button matching Screen 3 of mockup
            SafeAreaOutlinedPillButton(
                text = "Add Document",
                leadingIcon = Icons.Default.Add,
                onClick = onAddDocument,
                modifier = Modifier.testTag("home_add_document_button")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick actions row (Scan, Search) required by Section 5.3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onQuickScan)
                        .testTag("home_quick_scan_button"),
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan Document",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.ink
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onSeeAllDocuments)
                        .testTag("home_quick_search_button"),
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = palette.ink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Search Vault",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.ink
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Full Documents Library Screen with Search, Category Filters, Sort, Tags, and Grid/List Toggle.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentsLibraryScreen(
    uiState: VaultUiState,
    documents: List<DocumentWithFields>,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onToggleFavourites: () -> Unit,
    onToggleExpiring: () -> Unit,
    onToggleArchived: () -> Unit,
    onSortChange: (SortOption) -> Unit,
    onToggleGridView: () -> Unit,
    onAddCustomCategory: (String) -> Unit,
    onOpenDocument: (Long) -> Unit,
    onAddDocument: () -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val filteredDocs = remember(
        documents,
        uiState.searchQuery,
        uiState.selectedCategory,
        uiState.filterFavouritesOnly,
        uiState.filterExpiringOnly,
        uiState.filterArchivedOnly,
        uiState.sortOption
    ) {
        val q = uiState.searchQuery.trim().lowercase()
        documents.filter { item ->
            val doc = item.document
            val matchesArchive = if (uiState.filterArchivedOnly) doc.archived else !doc.archived
            val matchesCategory = uiState.selectedCategory == "All" ||
                doc.category.equals(uiState.selectedCategory, ignoreCase = true)
            val matchesFav = !uiState.filterFavouritesOnly || doc.favourite
            val matchesExpiring = !uiState.filterExpiringOnly ||
                (daysUntilExpiry(doc.expiryDate)?.let { it <= 45 } == true)
            val matchesQuery = q.isEmpty() ||
                doc.title.lowercase().contains(q) ||
                doc.category.lowercase().contains(q) ||
                doc.country.lowercase().contains(q) ||
                doc.issuer.lowercase().contains(q) ||
                doc.tagsCsv.lowercase().contains(q) ||
                item.fields.any { f -> f.userConfirmed && (f.label.lowercase().contains(q) || f.value.lowercase().contains(q)) }

            matchesArchive && matchesCategory && matchesFav && matchesExpiring && matchesQuery
        }.let { list ->
            when (uiState.sortOption) {
                SortOption.RECENT -> list.sortedByDescending { it.document.updatedAt }
                SortOption.TITLE -> list.sortedBy { it.document.title.lowercase() }
                SortOption.EXPIRY -> list.sortedWith(
                    compareBy<DocumentWithFields> { it.document.expiryDate.isBlank() }
                        .thenBy { it.document.expiryDate }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "All Documents",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.ink
                    )
                    Text(
                        text = "${filteredDocs.size} matching records · Offline search",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )
                }

                Row {
                    IconButton(onClick = onToggleGridView) {
                        Icon(
                            imageVector = if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle grid or list view",
                            tint = palette.ink
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search title, category, tags, or fields...") },
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

            // Quick status filter chips
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
                    selected = uiState.filterExpiringOnly,
                    onClick = onToggleExpiring,
                    label = { Text("Expiring / Expired") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.primary,
                        selectedLabelColor = Color.White
                    )
                )
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
                        label = { Text(sort.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category filter row
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

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredDocs.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No matching documents found",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.ink
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try clearing your search or category filter, or add a new document.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkMuted
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
                    maxItemsInEachRow = 2
                ) {
                    filteredDocs.forEach { item ->
                        val doc = item.document
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenDocument(doc.id) },
                            shape = RoundedCornerShape(18.dp),
                            color = palette.surface,
                            border = BorderStroke(1.1.dp, palette.border.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                DocumentCategoryIconBox(iconType = doc.iconType, size = 40.dp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = doc.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = palette.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${doc.category} · ${doc.country}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.inkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
                            daysLeft < 0 -> "Expired"
                            daysLeft <= 45 -> "${daysLeft}d left"
                            else -> null
                        }
                        SafeAreaDocumentRowCard(
                            title = doc.title,
                            subtitle = "${doc.country.ifBlank { doc.category }} · ${doc.category}",
                            iconType = doc.iconType,
                            expiryBadgeText = badge,
                            isExpiringSoon = daysLeft != null && daysLeft <= 45,
                            onClick = { onOpenDocument(doc.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
                    label = { Text("Category Name (e.g. Insurance, Tax)") },
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
}
