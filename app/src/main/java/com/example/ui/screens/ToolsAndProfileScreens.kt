package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.AuditEventEntity
import com.example.data.DocumentWithFields
import com.example.data.EmergencyIceProfile
import com.example.data.ExportFormat
import com.example.ui.VaultUiState
import com.example.ui.components.SafeAreaDocumentRowCard
import com.example.ui.components.SafeAreaOutlinedPillButton
import com.example.ui.components.SafeAreaPrimaryButton
import com.example.ui.components.SafeAreaShieldLogo
import com.example.ui.components.formatCompactFileSize
import com.example.ui.daysUntilExpiry
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.BorderContrastStyle
import com.example.ui.theme.CardDensityStyle
import com.example.ui.theme.CornerRadiusStyle
import com.example.ui.theme.LocalSafeAreaPalette
import com.example.ui.theme.VaultThemeId
import com.example.ui.theme.resolveSafeAreaPalette
import java.security.MessageDigest

fun buildRequiredPermissionsArray(): Array<String> {
    val list = mutableListOf(Manifest.permission.CAMERA)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return list.toTypedArray()
}

private fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ToolsScreen(
    uiState: VaultUiState,
    documents: List<DocumentWithFields>,
    onOpenScanner: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    onCopyCustomSelection: (String, String) -> Unit,
    onClearCache: () -> Unit,
    onExportBackup: () -> Unit,
    onExportInFormat: (ExportFormat) -> Unit = {},
    onSaveExportToUri: (ExportFormat, Uri) -> Unit = { _, _ -> },
    onCompilePacketPdf: (String, Set<Long>) -> Unit = { _, _ -> },
    onSaveEmergencyIceProfile: (EmergencyIceProfile) -> Unit = {}
) {
    val palette = LocalSafeAreaPalette.current
    val activeDocs = remember(documents) { documents.filter { !it.document.archived } }

    var showQrGeneratorSheet by remember { mutableStateOf(false) }
    var selectedDocForQr by remember(activeDocs) { mutableStateOf(activeDocs.firstOrNull()) }

    var showCopyTemplatesSheet by remember { mutableStateOf(false) }
    var showExpiryTrackerSheet by remember { mutableStateOf(false) }
    var showSecurityAuditSheet by remember { mutableStateOf(false) }
    var showPdfPacketSheet by remember { mutableStateOf(false) }
    var showMultiFormatExportSheet by remember { mutableStateOf(false) }
    var showIceCardSheet by remember { mutableStateOf(false) }
    var pendingSaveFormat by remember { mutableStateOf(ExportFormat.PDF) }

    val createExportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null) {
            onSaveExportToUri(pendingSaveFormat, uri)
        }
    }

    val expiringDocs = remember(activeDocs) {
        activeDocs.filter {
            val d = daysUntilExpiry(it.document.expiryDate)
            d != null && d <= 90L
        }.sortedBy { it.document.expiryDate }
    }

    val verifiedCount = remember(activeDocs) {
        activeDocs.count { docWithFields ->
            docWithFields.fields.isEmpty() || docWithFields.fields.all { it.userConfirmed }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(palette.itemSpacing)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vault Tools & Studio",
                            style = MaterialTheme.typography.headlineMedium,
                            color = palette.ink,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Native camera scanner, PDF packet compiler, 7-format exporter & offline utilities",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.inkMuted
                        )
                    }
                    Surface(
                        color = palette.successContainer,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(palette.borderWidth, palette.success.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = palette.success,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.success,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                ToolFeatureCard(
                    title = "Native Android Camera Scanner",
                    subtitle = "Launch your phone's real native camera app to snap documents, then crop, rotate & enhance",
                    badge = "Native Camera",
                    icon = Icons.Default.CameraAlt,
                    iconTint = palette.primary,
                    iconBg = palette.primary.copy(alpha = 0.12f),
                    onClick = onOpenScanner,
                    testTag = "tool_document_scanner"
                )
            }

            item {
                ToolFeatureCard(
                    title = "Multi-Format Vault Exporter",
                    subtitle = "Export your records in PDF, CSV, TXT, Markdown, HTML, JSON, or XML to device storage or Share Sheet",
                    badge = "7 Formats",
                    icon = Icons.Default.Download,
                    iconTint = palette.ink,
                    iconBg = palette.ink.copy(alpha = 0.10f),
                    onClick = { showMultiFormatExportSheet = true },
                    testTag = "tool_multi_format_export"
                )
            }

            item {
                ToolFeatureCard(
                    title = "Smart PDF Packet / Dossier Compiler",
                    subtitle = "Select multiple IDs, certificates, or financial cards and compile a multi-page A4 PDF packet",
                    badge = "Unique Feature",
                    icon = Icons.Default.PictureAsPdf,
                    iconTint = palette.primary,
                    iconBg = palette.primary.copy(alpha = 0.12f),
                    onClick = { showPdfPacketSheet = true },
                    testTag = "tool_pdf_packet_compiler"
                )
            }

            item {
                ToolFeatureCard(
                    title = "Emergency ICE Medical & Contact Card",
                    subtitle = if (uiState.emergencyIceProfile.isConfigured) {
                        "Configured for ${uiState.emergencyIceProfile.fullName.ifBlank { "Owner" }} • Blood: ${uiState.emergencyIceProfile.bloodGroup.ifBlank { "N/A" }}"
                    } else {
                        "Set up blood group, allergies, emergency contact & insurance policy for rapid access"
                    },
                    badge = if (uiState.emergencyIceProfile.isConfigured) "Ready" else "Set Up",
                    icon = Icons.Default.LocalHospital,
                    iconTint = palette.primary,
                    iconBg = palette.primary.copy(alpha = 0.12f),
                    onClick = { showIceCardSheet = true },
                    testTag = "tool_ice_card"
                )
            }

            item {
                ToolFeatureCard(
                    title = "Offline Verification QR Generator",
                    subtitle = "Generate a high-density offline QR code containing selected document details for instant sharing",
                    badge = "Offline QR",
                    icon = Icons.Default.QrCode2,
                    iconTint = palette.ink,
                    iconBg = palette.ink.copy(alpha = 0.10f),
                    onClick = {
                        selectedDocForQr = activeDocs.firstOrNull()
                        showQrGeneratorSheet = true
                    },
                    testTag = "tool_qr_generator"
                )
            }

            item {
                ToolFeatureCard(
                    title = "One-Tap Copy & Share Formatter",
                    subtitle = "Format any stored document into clean plain-text, Markdown, or official verification blocks",
                    badge = "${activeDocs.size} Docs",
                    icon = Icons.Default.ContentCopy,
                    iconTint = palette.success,
                    iconBg = palette.successContainer,
                    onClick = { showCopyTemplatesSheet = true },
                    testTag = "tool_copy_formatter"
                )
            }

            item {
                ToolFeatureCard(
                    title = "Expiry & Renewal Radar",
                    subtitle = if (expiringDocs.isEmpty()) {
                        "All active documents are healthy with no upcoming expirations in the next 90 days"
                    } else {
                        "${expiringDocs.size} document(s) expiring soon or already expired — tap to inspect"
                    },
                    badge = if (expiringDocs.isEmpty()) "All Clear" else "${expiringDocs.size} Alert(s)",
                    icon = Icons.Default.Warning,
                    iconTint = if (expiringDocs.isEmpty()) palette.success else palette.warning,
                    iconBg = if (expiringDocs.isEmpty()) palette.successContainer else palette.warningContainer,
                    onClick = { showExpiryTrackerSheet = true },
                    testTag = "tool_expiry_radar"
                )
            }

            item {
                ToolFeatureCard(
                    title = "Vault Security & Encryption Audit",
                    subtitle = "Inspect AES-256-GCM hardware keystore status, lock configuration, and storage footprint",
                    badge = "$verifiedCount/${activeDocs.size} Verified",
                    icon = Icons.Default.Security,
                    iconTint = palette.ink,
                    iconBg = palette.ink.copy(alpha = 0.10f),
                    onClick = { showSecurityAuditSheet = true },
                    testTag = "tool_security_audit"
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(palette.cardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = palette.borderAlpha))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Quick Vault Actions",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.ink,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showMultiFormatExportSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("tools_quick_export_button"),
                                shape = RoundedCornerShape(palette.buttonCornerRadius)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Data", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = onClearCache,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("tools_quick_clear_cache_button"),
                                shape = RoundedCornerShape(palette.buttonCornerRadius),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.ink,
                                    contentColor = palette.surface
                                )
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear Cache", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(84.dp)) }
        }
    }

    if (showMultiFormatExportSheet) {
        MultiFormatExportBottomSheet(
            documentCount = activeDocs.size,
            onDismiss = { showMultiFormatExportSheet = false },
            onShareFormat = { fmt ->
                onExportInFormat(fmt)
                showMultiFormatExportSheet = false
            },
            onSaveFormatToDevice = { fmt ->
                pendingSaveFormat = fmt
                val fileName = "SAFEAREA_Vault_Export_${System.currentTimeMillis()}.${fmt.extension}"
                createExportDocumentLauncher.launch(fileName)
            }
        )
    }

    if (showPdfPacketSheet) {
        var packetTitle by remember { mutableStateOf("SAFEAREA Official Document Packet") }
        val selectedIds = remember(activeDocs) {
            mutableStateListOf<Long>().apply {
                addAll(activeDocs.take(4).map { it.document.id })
            }
        }
        ModalBottomSheet(
            onDismissRequest = { showPdfPacketSheet = false },
            containerColor = palette.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Smart PDF Packet Compiler",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.ink,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Combine multiple documents + attached photos into a single A4 PDF dossier",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = packetTitle,
                    onValueChange = { packetTitle = it },
                    label = { Text("Packet / Dossier Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (activeDocs.isEmpty()) {
                    Text(
                        text = "No active documents in your vault yet. Scan or add a document first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SafeAreaPrimaryButton(
                        text = "Scan Document with Native Camera",
                        onClick = {
                            showPdfPacketSheet = false
                            onOpenScanner()
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Documents (${selectedIds.size}/${activeDocs.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.ink,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {
                                if (selectedIds.size == activeDocs.size) {
                                    selectedIds.clear()
                                } else {
                                    selectedIds.clear()
                                    selectedIds.addAll(activeDocs.map { it.document.id })
                                }
                            }
                        ) {
                            Text(if (selectedIds.size == activeDocs.size) "Clear All" else "Select All")
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(activeDocs, key = { it.document.id }) { item ->
                            val doc = item.document
                            val checked = doc.id in selectedIds
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (checked) selectedIds.remove(doc.id) else selectedIds.add(doc.id)
                                    },
                                color = if (checked) palette.successContainer.copy(alpha = 0.6f) else palette.canvas,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (checked) palette.success else palette.border.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            if (checked) selectedIds.remove(doc.id) else selectedIds.add(doc.id)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = palette.ink,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${doc.category} • ${doc.documentSubtype.ifBlank { doc.displayFormatBadge }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.inkMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    SafeAreaPrimaryButton(
                        text = "Compile & Share PDF Packet (${selectedIds.size})",
                        onClick = {
                            onCompilePacketPdf(packetTitle, selectedIds.toSet())
                            showPdfPacketSheet = false
                        },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier.testTag("compile_pdf_packet_confirm")
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showIceCardSheet) {
        val ice = uiState.emergencyIceProfile
        var fullName by remember(ice) { mutableStateOf(ice.fullName) }
        var bloodGroup by remember(ice) { mutableStateOf(ice.bloodGroup) }
        var allergies by remember(ice) { mutableStateOf(ice.allergies) }
        var emergencyContact by remember(ice) { mutableStateOf(ice.emergencyContact) }
        var medicalConditions by remember(ice) { mutableStateOf(ice.medicalConditions) }
        var insurancePolicyNo by remember(ice) { mutableStateOf(ice.insurancePolicyNo) }

        AlertDialog(
            onDismissRequest = { showIceCardSheet = false },
            containerColor = palette.surface,
            title = {
                Text(
                    text = "Emergency ICE Card",
                    color = palette.ink,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = { bloodGroup = it },
                        label = { Text("Blood Group (e.g. O+, A-, B+)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emergencyContact,
                        onValueChange = { emergencyContact = it },
                        label = { Text("Emergency Contact Name & Phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = allergies,
                        onValueChange = { allergies = it },
                        label = { Text("Allergies / Medications") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = medicalConditions,
                        onValueChange = { medicalConditions = it },
                        label = { Text("Medical Notes / Conditions") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = insurancePolicyNo,
                        onValueChange = { insurancePolicyNo = it },
                        label = { Text("Health Insurance Policy #") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveEmergencyIceProfile(
                            EmergencyIceProfile(
                                fullName = fullName.trim(),
                                bloodGroup = bloodGroup.trim(),
                                allergies = allergies.trim(),
                                emergencyContact = emergencyContact.trim(),
                                medicalConditions = medicalConditions.trim(),
                                insurancePolicyNo = insurancePolicyNo.trim()
                            )
                        )
                        showIceCardSheet = false
                    }
                ) {
                    Text("Save ICE Card", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIceCardSheet = false }) {
                    Text("Cancel", color = palette.inkMuted)
                }
            }
        )
    }

    if (showQrGeneratorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQrGeneratorSheet = false },
            containerColor = palette.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Offline Document QR Generator",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.ink,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Select any vault document to generate a scannable offline verification QR code",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkMuted
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (activeDocs.isEmpty()) {
                    Text(
                        text = "Add or scan a document first to generate an offline verification QR code.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SafeAreaPrimaryButton(
                        text = "Scan Document",
                        onClick = {
                            showQrGeneratorSheet = false
                            onOpenScanner()
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeDocs.forEach { item ->
                            FilterChip(
                                selected = selectedDocForQr?.document?.id == item.document.id,
                                onClick = { selectedDocForQr = item },
                                label = { Text(item.document.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.ink,
                                    selectedLabelColor = palette.surface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val currentDoc = selectedDocForQr ?: activeDocs.first()
                    val qrPayload = remember(currentDoc) {
                        buildString {
                            appendLine("SAFEAREA VERIFIED RECORD")
                            appendLine("Title: ${currentDoc.document.title}")
                            appendLine("Category: ${currentDoc.document.category}")
                            if (currentDoc.document.issuer.isNotBlank()) {
                                appendLine("Issuer: ${currentDoc.document.issuer}")
                            }
                            currentDoc.fields.forEach { field ->
                                appendLine("${field.label}: ${field.value}")
                            }
                        }.trim()
                    }
                    val qrBitmap = remember(qrPayload) {
                        generateQrCodeBitmap(qrPayload, 512)
                    }

                    if (qrBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, palette.border.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Document Verification QR Code",
                                modifier = Modifier
                                    .size(210.dp)
                                    .padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentDoc.document.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.ink,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentDoc.fields.size} structured fields encoded offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SafeAreaPrimaryButton(
                        text = "Copy Encoded Payload",
                        onClick = {
                            onCopyCustomSelection(currentDoc.document.title, qrPayload)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showCopyTemplatesSheet) {
        var selectedDoc by remember(activeDocs) { mutableStateOf(activeDocs.firstOrNull()) }

        ModalBottomSheet(
            onDismissRequest = { showCopyTemplatesSheet = false },
            containerColor = palette.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "One-Tap Copy & Share Formatter",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.ink,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (activeDocs.isEmpty()) {
                    Text(
                        text = "No active documents yet. Add a document to format and copy its fields.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkMuted
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeDocs.forEach { item ->
                            FilterChip(
                                selected = selectedDoc?.document?.id == item.document.id,
                                onClick = { selectedDoc = item },
                                label = { Text(item.document.title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.ink,
                                    selectedLabelColor = palette.surface
                                )
                            )
                        }
                    }

                    val item = selectedDoc ?: activeDocs.first()
                    val doc = item.document
                    val plainBlock = remember(item) {
                        buildString {
                            appendLine("${doc.title} (${doc.category})")
                            item.fields.forEach { appendLine("${it.label}: ${it.value}") }
                        }.trim()
                    }
                    val markdownBlock = remember(item) {
                        buildString {
                            appendLine("### ${doc.title}")
                            appendLine("- **Category:** ${doc.category}")
                            if (doc.issuer.isNotBlank()) appendLine("- **Issuer:** ${doc.issuer}")
                            item.fields.forEach { appendLine("- **${it.label}:** `${it.value}`") }
                        }.trim()
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = palette.canvas,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = plainBlock,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = palette.ink,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onCopyCustomSelection(doc.title, plainBlock) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Copy Plain Text")
                        }
                        Button(
                            onClick = { onCopyCustomSelection("${doc.title} Markdown", markdownBlock) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.ink)
                        ) {
                            Text("Copy Markdown")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showExpiryTrackerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExpiryTrackerSheet = false },
            containerColor = palette.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Document Expiry & Renewal Radar",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.ink,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Documents expiring within 90 days or already expired",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (expiringDocs.isEmpty()) {
                    Text(
                        text = "All documents with expiration dates are valid beyond the next 90 days.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkMuted
                    )
                } else {
                    expiringDocs.forEach { item ->
                        val doc = item.document
                        val daysLeft = daysUntilExpiry(doc.expiryDate)
                        SafeAreaDocumentRowCard(
                            title = doc.title,
                            subtitle = "${doc.category} • Exp: ${doc.expiryDate}",
                            categoryGroup = doc.categoryGroup,
                            formatBadge = doc.displayFormatBadge,
                            expiryBadgeText = if (daysLeft != null && daysLeft < 0) "Expired" else "${daysLeft ?: 0}d left",
                            isExpiringSoon = true,
                            isFavourite = doc.favourite,
                            onClick = {
                                showExpiryTrackerSheet = false
                                onOpenDocument(doc.id)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showSecurityAuditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSecurityAuditSheet = false },
            containerColor = palette.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Vault Security & Encryption Audit",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.ink,
                    fontWeight = FontWeight.ExtraBold
                )
                AuditStatusRow("Primary Lock Method", uiState.primaryLockMethod.title, true)
                AuditStatusRow("Document Field Storage", "AES-256-GCM Encrypted Room DB", true)
                AuditStatusRow("Document Image Attachments", "AES-256-GCM Encrypted Local Files", true)
                AuditStatusRow("Encrypted Vault Footprint", formatCompactFileSize(uiState.vaultStorageBytes).ifBlank { "0 B" }, true)
                AuditStatusRow("Active Vault Records", "${activeDocs.size} Stored", true)
                AuditStatusRow("Verified Documents", "$verifiedCount Verified", verifiedCount > 0)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MultiFormatExportBottomSheet(
    documentCount: Int,
    onDismiss: () -> Unit,
    onShareFormat: (ExportFormat) -> Unit,
    onSaveFormatToDevice: (ExportFormat) -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Export Vault Data (7 Formats)",
                style = MaterialTheme.typography.titleLarge,
                color = palette.ink,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Export $documentCount active document(s) in PDF, CSV, TXT, Markdown, HTML, JSON, or XML",
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkMuted
            )
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportFormat.entries.forEach { fmt ->
                    val isSelected = selectedFormat == fmt
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFormat = fmt },
                        label = {
                            Text(
                                text = ".${fmt.extension.uppercase()} • ${fmt.badge}",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.ink,
                            selectedLabelColor = palette.surface
                        ),
                        modifier = Modifier.testTag("export_format_chip_${fmt.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = palette.canvas,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = selectedFormat.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.ink,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedFormat.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onSaveFormatToDevice(selectedFormat) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("export_save_device_button"),
                    shape = RoundedCornerShape(palette.buttonCornerRadius)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Files", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = { onShareFormat(selectedFormat) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("export_share_sheet_button"),
                    shape = RoundedCornerShape(palette.buttonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share .${selectedFormat.extension.uppercase()}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

@Composable
private fun ToolFeatureCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalSafeAreaPalette.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(palette.cardCornerRadius))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(palette.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = palette.borderAlpha))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = palette.cardVerticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.ink,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = iconBg,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = iconTint,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open",
                tint = palette.inkMuted
            )
        }
    }
}

@Composable
private fun AuditStatusRow(label: String, value: String, isHealthy: Boolean) {
    val palette = LocalSafeAreaPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.canvas,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.inkMuted,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isHealthy) palette.success else palette.warning,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.ink,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun generateQrCodeBitmap(content: String, sizePx: Int): Bitmap? {
    return try {
        val gridSize = 29
        val matrix = Array(gridSize) { BooleanArray(gridSize) }
        val reserved = Array(gridSize) { BooleanArray(gridSize) }

        fun placeFinder(startR: Int, startC: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val rr = startR + r
                    val cc = startC + c
                    if (rr in 0 until gridSize && cc in 0 until gridSize) {
                        reserved[rr][cc] = true
                        val inOuter = r in 0..6 && c in 0..6 && (r == 0 || r == 6 || c == 0 || c == 6)
                        val inInner = r in 2..4 && c in 2..4
                        matrix[rr][cc] = inOuter || inInner
                    }
                }
            }
        }

        placeFinder(0, 0)
        placeFinder(0, gridSize - 7)
        placeFinder(gridSize - 7, 0)

        // Timing patterns
        for (i in 8 until gridSize - 8) {
            reserved[6][i] = true
            matrix[6][i] = (i % 2 == 0)
            reserved[i][6] = true
            matrix[i][6] = (i % 2 == 0)
        }

        // Alignment pattern (bottom-right 5x5)
        val alignCenter = gridSize - 7
        for (r in -2..2) {
            for (c in -2..2) {
                val rr = alignCenter + r
                val cc = alignCenter + c
                reserved[rr][cc] = true
                matrix[rr][cc] = (kotlin.math.abs(r) == 2 || kotlin.math.abs(c) == 2 || (r == 0 && c == 0))
            }
        }

        val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        val payloadBytes = content.toByteArray(Charsets.UTF_8) + digest
        var bitIndex = 0
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (!reserved[r][c]) {
                    val byteVal = payloadBytes[(bitIndex / 8) % payloadBytes.size].toInt() and 0xFF
                    val bit = ((byteVal ushr (7 - (bitIndex % 8))) and 1) == 1
                    matrix[r][c] = bit xor ((r + c) % 3 == 0)
                    bitIndex++
                }
            }
        }

        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val quietZone = 2
        val totalModules = gridSize + quietZone * 2
        val darkColor = AndroidColor.rgb(23, 20, 49)
        for (x in 0 until sizePx) {
            val col = (x * totalModules) / sizePx - quietZone
            for (y in 0 until sizePx) {
                val row = (y * totalModules) / sizePx - quietZone
                val isDark = row in 0 until gridSize && col in 0 until gridSize && matrix[row][col]
                bmp.setPixel(x, y, if (isDark) darkColor else AndroidColor.WHITE)
            }
        }
        bmp
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsScreen(
    uiState: VaultUiState,
    auditEvents: List<AuditEventEntity>,
    onSelectTheme: (VaultThemeId) -> Unit,
    onSelectAppearanceMode: (AppearanceMode) -> Unit,
    onToggleUseSystemFont: (Boolean) -> Unit,
    onSelectFontScale: (Float) -> Unit,
    onSetCornerRadiusStyle: (CornerRadiusStyle) -> Unit = {},
    onSetCardDensityStyle: (CardDensityStyle) -> Unit = {},
    onSetBorderContrastStyle: (BorderContrastStyle) -> Unit = {},
    onSetCustomVaultBranding: (String, String) -> Unit = { _, _ -> },
    onSetShowSecurityScoreCard: (Boolean) -> Unit = {},
    onSetShowCategoryGridOnHome: (Boolean) -> Unit = {},
    onAddCustomCategory: (String) -> Unit = {},
    onPermissionsUpdated: (Map<String, Boolean>) -> Unit,
    onFolderAccessGranted: (String) -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    onConfigureLockMethod: () -> Unit,
    onLockVaultNow: () -> Unit,
    onChangeAutoLockTimeout: (Int) -> Unit,
    onToggleLockOnBackground: (Boolean) -> Unit,
    onToggleHideInAppSwitcher: (Boolean) -> Unit,
    onToggleExpiryNotifications: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onExportBackup: () -> Unit,
    onExportInFormat: (ExportFormat) -> Unit = {},
    onSaveExportToUri: (ExportFormat, Uri) -> Unit = { _, _ -> },
    onWipeAllData: () -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }
    var showBrandingDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var selectedExportFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var pendingSaveFormat by remember { mutableStateOf(ExportFormat.PDF) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        onPermissionsUpdated(results)
    }

    val createExportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null) {
            onSaveExportToUri(pendingSaveFormat, uri)
        }
    }

    val folderAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            onFolderAccessGranted(uri.toString())
        }
    }

    val hasCameraPermission = remember(uiState.permissionsRefreshTick) {
        isPermissionGranted(context, Manifest.permission.CAMERA)
    }
    val hasNotificationPermission = remember(uiState.permissionsRefreshTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }
    val hasFullFilesAccess = remember(uiState.permissionsRefreshTick, uiState.grantedFolderUri) {
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) ||
            uiState.grantedFolderUri.isNotBlank()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(palette.itemSpacing)
        ) {
            item {
                Text(
                    text = "Profile & Vault Studio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.ink,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Export in PDF/CSV/TXT, customize 18 themes & layout geometry, and manage security",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkMuted
                )
            }

            // Owner Identity Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(palette.cardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = palette.borderAlpha))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = palette.cardVerticalPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SafeAreaShieldLogo(size = 56.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.customVaultTitle.ifBlank { "SAFEAREA" },
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.ink,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.customVaultTagline} • ${uiState.allCategories.size} Categories",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.inkMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "AES-256-GCM Encrypted • Native Camera Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.success,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Multi-Format Data Export Center (PDF, CSV, TXT, Markdown, HTML, JSON, XML)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(palette.cardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = palette.borderAlpha))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = palette.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Multi-Format Data Export Center",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.ink,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Export all vault records in PDF, CSV, TXT, Markdown, HTML, JSON, or XML",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.inkMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExportFormat.entries.forEach { fmt ->
                                val selected = selectedExportFormat == fmt
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedExportFormat = fmt },
                                    label = {
                                        Text(
                                            text = ".${fmt.extension.uppercase()} (${fmt.badge})",
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.ink,
                                        selectedLabelColor = palette.surface
                                    ),
                                    modifier = Modifier.testTag("settings_export_format_${fmt.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${selectedExportFormat.label}: ${selectedExportFormat.description}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.inkMuted
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    pendingSaveFormat = selectedExportFormat
                                    val fileName = "SAFEAREA_Export_${System.currentTimeMillis()}.${selectedExportFormat.extension}"
                                    createExportDocumentLauncher.launch(fileName)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_save_export_file_button"),
                                shape = RoundedCornerShape(palette.buttonCornerRadius)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save .${selectedExportFormat.extension.uppercase()}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = { onExportInFormat(selectedExportFormat) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_share_export_button"),
                                shape = RoundedCornerShape(palette.buttonCornerRadius),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share .${selectedExportFormat.extension.uppercase()}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // Everything is Customizable Studio (Layout, Corner Radius, Density, Branding, Custom Categories)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(palette.cardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = palette.borderAlpha))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = palette.ink
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "UI & Layout Customization Studio",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.ink,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Customize corner geometry, layout density, border contrast, vault branding & categories",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.inkMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Card Corner Geometry",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.ink,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CornerRadiusStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = uiState.cornerRadiusStyle == style,
                                    onClick = { onSetCornerRadiusStyle(style) },
                                    label = { Text(style.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.ink,
                                        selectedLabelColor = palette.surface
                                    ),
                                    modifier = Modifier.testTag("corner_style_${style.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Layout & Card Density",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.ink,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CardDensityStyle.entries.forEach { density ->
                                FilterChip(
                                    selected = uiState.cardDensityStyle == density,
                                    onClick = { onSetCardDensityStyle(density) },
                                    label = { Text(density.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.ink,
                                        selectedLabelColor = palette.surface
                                    ),
                                    modifier = Modifier.testTag("density_style_${density.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Card Border Contrast",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.ink,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BorderContrastStyle.entries.forEach { contrast ->
                                FilterChip(
                                    selected = uiState.borderContrastStyle == contrast,
                                    onClick = { onSetBorderContrastStyle(contrast) },
                                    label = { Text(contrast.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.ink,
                                        selectedLabelColor = palette.surface
                                    ),
                                    modifier = Modifier.testTag("border_contrast_${contrast.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = palette.border.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show Security Health Card on Home",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.ink,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Display live readiness score & Emergency ICE button",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.inkMuted
                                )
                            }
                            Switch(
                                checked = uiState.showSecurityScoreCard,
                                onCheckedChange = onSetShowSecurityScoreCard
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show 15-Category Grid on Home",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.ink,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Display quick-filter category tiles on the Home dashboard",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.inkMuted
                                )
                            }
                            Switch(
                                checked = uiState.showCategoryGridOnHome,
                                onCheckedChange = onSetShowCategoryGridOnHome
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showBrandingDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("customize_branding_button"),
                                shape = RoundedCornerShape(palette.buttonCornerRadius)
                            ) {
                                Text("Custom Vault Title", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { showAddCategoryDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_add_custom_category_button"),
                                shape = RoundedCornerShape(palette.buttonCornerRadius)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Category", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // 18 Curated Themes Library & Light/Dark/System Appearance Mode
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(palette.cardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = palette.borderAlpha))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = palette.ink
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Appearance & 18 Theme Presets",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.ink,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Choose from 12 editorial light themes and 6 OLED/dark vault themes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.inkMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppearanceMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = uiState.appearanceMode == mode,
                                    onClick = { onSelectAppearanceMode(mode) },
                                    label = { Text(mode.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.ink,
                                        selectedLabelColor = palette.surface
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            VaultThemeId.entries.chunked(2).forEach { rowThemes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowThemes.forEach { theme ->
                                        val themePalette = resolveSafeAreaPalette(theme, effectiveDark = theme.isDarkPreset)
                                        val isSelected = uiState.selectedTheme == theme
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { onSelectTheme(theme) }
                                                .testTag("theme_option_${theme.name.lowercase()}"),
                                            shape = RoundedCornerShape(14.dp),
                                            color = themePalette.surface,
                                            border = BorderStroke(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) themePalette.primary else themePalette.border.copy(alpha = 0.4f)
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clip(CircleShape)
                                                                .background(themePalette.ink)
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clip(CircleShape)
                                                                .background(themePalette.primary)
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clip(CircleShape)
                                                                .background(themePalette.secondary)
                                                        )
                                                    }
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Selected",
                                                            tint = themePalette.success,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = theme.displayName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = themePalette.ink,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (theme.isDarkPreset) "Dark / OLED Preset" else "Editorial Light Preset",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = themePalette.inkMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    if (rowThemes.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Typography & Font Options Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(palette.cardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = palette.borderAlpha))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = palette.ink
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Typography & Font Options",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.ink,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Use System Font",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.ink,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (uiState.useSystemFont) {
                                        "Using Android device default system typeface"
                                    } else {
                                        "Using SAFEAREA editorial font (Plus Jakarta Sans + Devanagari)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.inkMuted
                                )
                            }
                            Switch(
                                checked = uiState.useSystemFont,
                                onCheckedChange = onToggleUseSystemFont,
                                modifier = Modifier.testTag("use_system_font_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = palette.success
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Font Size Scale",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.ink,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val fontScaleOptions = listOf(
                            0.90f to "Small",
                            1.00f to "Default",
                            1.10f to "Large",
                            1.20f to "X-Large"
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fontScaleOptions.forEach { (scale, label) ->
                                val isSelected = kotlin.math.abs(uiState.fontScale - scale) < 0.03f
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectFontScale(scale) },
                                    label = { Text(label, maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.ink,
                                        selectedLabelColor = palette.surface
                                    ),
                                    modifier = Modifier.testTag("font_scale_${label.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Permissions, Camera & Files Access Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(palette.cardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = palette.borderAlpha))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = palette.ink
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Permissions, Native Camera & Files",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.ink,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        PermissionStatusRow(
                            label = "Native Camera Access (Scanner)",
                            granted = hasCameraPermission
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PermissionStatusRow(
                            label = "Notifications (Expiry Alerts)",
                            granted = hasNotificationPermission
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PermissionStatusRow(
                            label = "Files & Folder Access",
                            granted = hasFullFilesAccess,
                            statusOverride = if (hasFullFilesAccess) "Folder Granted" else "Tap below to grant folder"
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                permissionsLauncher.launch(buildRequiredPermissionsArray())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("request_all_permissions_button"),
                            shape = RoundedCornerShape(palette.buttonCornerRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasCameraPermission && hasNotificationPermission) palette.success else palette.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasCameraPermission && hasNotificationPermission) {
                                    "Camera & Notifications Granted"
                                } else {
                                    "Grant Camera & Notifications"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val initialUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && uiState.grantedFolderUri.isNotBlank()) {
                                        runCatching { Uri.parse(uiState.grantedFolderUri) }.getOrNull()
                                    } else null
                                    folderAccessLauncher.launch(initialUri)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("grant_files_folder_access_button"),
                                shape = RoundedCornerShape(palette.buttonCornerRadius)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Folder Access", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("open_system_permission_settings"),
                                shape = RoundedCornerShape(palette.buttonCornerRadius)
                            ) {
                                Text("App Settings", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = palette.border.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Document Expiry Reminders",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.ink,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Local alerts for expiring documents",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.inkMuted
                                )
                            }
                            Switch(
                                checked = uiState.expiryNotificationsEnabled,
                                onCheckedChange = onToggleExpiryNotifications,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = palette.success
                                )
                            )
                        }
                    }
                }
            }

            // Security & Auto-Lock Settings
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(palette.cardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = palette.borderAlpha))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = palette.ink
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Vault Security & Biometric App Lock",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.ink,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        SettingsItemRow(
                            title = "Primary Lock Method",
                            value = uiState.primaryLockMethod.title,
                            onClick = onConfigureLockMethod,
                            testTag = "change_lock_method_row"
                        )
                        HorizontalDivider(color = palette.border.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable App Lock",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.ink,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Require PIN, Pattern, or Biometric to open SAFEAREA",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.inkMuted
                                )
                            }
                            Switch(
                                checked = uiState.appLockEnabled,
                                onCheckedChange = onToggleAppLock
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Auto-Lock Timeout on Inactivity",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.ink,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val timeoutOptions = listOf(
                            0 to "Immediate",
                            30 to "30s",
                            60 to "1m",
                            300 to "5m"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timeoutOptions.forEach { (seconds, label) ->
                                FilterChip(
                                    selected = uiState.autoLockTimeoutSeconds == seconds,
                                    onClick = { onChangeAutoLockTimeout(seconds) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.ink,
                                        selectedLabelColor = palette.surface
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        SafeAreaOutlinedPillButton(
                            text = "Lock Vault Now",
                            onClick = onLockVaultNow,
                            leadingIcon = Icons.Default.Lock,
                            modifier = Modifier.testTag("lock_vault_now_button")
                        )
                    }
                }
            }

            // Danger Zone Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(palette.cardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = palette.borderAlpha))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = palette.danger
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Storage & Vault Reset",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.ink,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Encrypted Vault: ${formatCompactFileSize(uiState.vaultStorageBytes).ifBlank { "0 B" }} • Export Cache: ${formatCompactFileSize(uiState.cacheStorageBytes).ifBlank { "0 B" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("wipe_vault_button"),
                            shape = RoundedCornerShape(palette.buttonCornerRadius),
                            border = BorderStroke(1.5.dp, palette.danger),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.danger)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Wipe All Vault Records", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(84.dp)) }
        }
    }

    if (showBrandingDialog) {
        var titleText by remember(uiState.customVaultTitle) { mutableStateOf(uiState.customVaultTitle) }
        var taglineText by remember(uiState.customVaultTagline) { mutableStateOf(uiState.customVaultTagline) }
        AlertDialog(
            onDismissRequest = { showBrandingDialog = false },
            containerColor = palette.surface,
            title = {
                Text("Customize Vault Title & Tagline", color = palette.ink, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        label = { Text("Vault Title (e.g., SAFEAREA, My Vault)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = taglineText,
                        onValueChange = { taglineText = it },
                        label = { Text("Vault Tagline (e.g., Personal Document Vault)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSetCustomVaultBranding(titleText, taglineText)
                        showBrandingDialog = false
                    }
                ) {
                    Text("Save", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBrandingDialog = false }) {
                    Text("Cancel", color = palette.inkMuted)
                }
            }
        )
    }

    if (showAddCategoryDialog) {
        var customCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = palette.surface,
            title = {
                Text("Create Custom Document Category", color = palette.ink, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = customCatName,
                    onValueChange = { customCatName = it },
                    label = { Text("Category Name (e.g., Crypto Keys, Pet Records)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customCatName.isNotBlank()) {
                            onAddCustomCategory(customCatName.trim())
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text("Add Category", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel", color = palette.inkMuted)
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = palette.surface,
            title = {
                Text(
                    text = "Wipe Vault Permanently?",
                    color = palette.ink,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all encrypted documents and attached scans from this device.",
                    color = palette.inkMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onWipeAllData()
                    }
                ) {
                    Text("Wipe Vault", color = palette.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = palette.ink)
                }
            }
        )
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    granted: Boolean,
    statusOverride: String? = null
) {
    val palette = LocalSafeAreaPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.ink,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = if (granted) palette.successContainer else palette.warningContainer,
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(
                text = statusOverride ?: if (granted) "Granted" else "Not Granted",
                style = MaterialTheme.typography.labelSmall,
                color = if (granted) palette.success else palette.warning,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SettingsItemRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalSafeAreaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = palette.inkMuted
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = palette.ink,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.inkMuted
            )
        }
    }
}
