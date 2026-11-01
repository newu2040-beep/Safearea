package com.example.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TagFaces
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.LockMethod
import com.example.ui.components.SafeAreaBackButton
import com.example.ui.components.SafeAreaOutlinedPillButton
import com.example.ui.components.SafeAreaPrimaryButton
import com.example.ui.components.SafeAreaShieldLogo
import com.example.ui.components.WelcomeDocumentsHeroIllustration
import com.example.ui.theme.LocalSafeAreaPalette
import kotlin.math.hypot

/**
 * Screen 1 of the SAFEAREA 5-screen mockup: Welcome / Onboarding.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    var showPrivacySheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            SafeAreaShieldLogo(size = 96.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SAFEAREA",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = palette.ink
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your Personal\nDocument Vault",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 26.sp
                ),
                color = palette.ink,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))
            WelcomeDocumentsHeroIllustration()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Save · Secure · Access",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.ink
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(bottom = 12.dp)
        ) {
            SafeAreaPrimaryButton(
                text = "Get Started",
                onClick = onGetStarted,
                trailingArrow = true,
                modifier = Modifier.testTag("get_started_button")
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showPrivacySheet = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("learn_privacy_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = palette.ink,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Your data stays private · Learn how privacy works",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.ink
                )
            }
        }
    }

    if (showPrivacySheet) {
        AlertDialog(
            onDismissRequest = { showPrivacySheet = false },
            containerColor = palette.surface,
            titleContentColor = palette.ink,
            textContentColor = palette.ink,
            title = {
                Text(
                    text = "Local-First Privacy & Security",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "• On-Device Only: Your scanned documents, PDFs, and fields stay in app-private storage on your device. Nothing is silently uploaded to any cloud server or external OCR service.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Hardware-Backed Encryption: Document files are encrypted with AES-256-GCM keys protected by Android Keystore. PINs and patterns are derived with PBKDF2-HMAC-SHA256 and never stored in plain text.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Just-In-Time Permissions: SAFEAREA never asks for permissions up front. Camera or notification access is requested only when you use that specific feature.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Important Disclaimer: SAFEAREA is a personal storage vault. It does not government-verify documents or replace legal originals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacySheet = false }) {
                    Text("Got it", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * Screen 2 of the SAFEAREA 5-screen mockup: Secure Your Vault.
 */
@Composable
fun SecuritySetupScreen(
    selectedMethod: LockMethod,
    onSelectMethod: (LockMethod) -> Unit,
    onSetupPinCompleted: (pin: String, enableBiometric: Boolean, recoveryHint: String) -> Unit,
    onSetupPatternCompleted: (pattern: List<Int>, fallbackPin: String?, enableBiometric: Boolean) -> Unit,
    onSkipSetup: () -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    val context = LocalContext.current

    var showCredentialDialog by remember { mutableStateOf(false) }
    var pinStepFirst by remember { mutableStateOf("") }
    var pinStepConfirm by remember { mutableStateOf("") }
    var recoveryHint by remember { mutableStateOf("") }
    var acknowledgeRecoveryWarning by remember { mutableStateOf(true) }
    var setupError by remember { mutableStateOf<String?>(null) }

    val patternFirst = remember { mutableStateListOf<Int>() }
    val patternConfirm = remember { mutableStateListOf<Int>() }
    var isConfirmingPattern by remember { mutableStateOf(false) }
    var fallbackPinForPattern by remember { mutableStateOf("") }

    val biometricSupported = remember { isDeviceBiometricCapable(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SafeAreaBackButton(onClick = onBack)
            TextButton(onClick = onSkipSetup) {
                Text(
                    text = "Skip for now",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.inkMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Secure Your Vault",
            style = MaterialTheme.typography.headlineLarge,
            color = palette.ink,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Choose how you want to lock\nSAFEAREA.",
            style = MaterialTheme.typography.bodyLarge,
            color = palette.inkMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LockMethod.entries.forEach { method ->
                val isSelected = selectedMethod == method
                SecurityOptionCard(
                    method = method,
                    isSelected = isSelected,
                    onClick = { onSelectMethod(method) }
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            SafeAreaPrimaryButton(
                text = "Continue",
                onClick = {
                    setupError = null
                    pinStepFirst = ""
                    pinStepConfirm = ""
                    patternFirst.clear()
                    patternConfirm.clear()
                    isConfirmingPattern = false
                    showCredentialDialog = true
                },
                modifier = Modifier.testTag("continue_security_button")
            )
        }
    }

    if (showCredentialDialog) {
        val needsPattern = selectedMethod == LockMethod.PATTERN
        val wantsBiometric = selectedMethod == LockMethod.FACE_ID || selectedMethod == LockMethod.FINGERPRINT

        AlertDialog(
            onDismissRequest = { showCredentialDialog = false },
            containerColor = palette.surface,
            titleContentColor = palette.ink,
            textContentColor = palette.ink,
            title = {
                Text(
                    text = when {
                        needsPattern && !isConfirmingPattern -> "Draw Your Unlock Pattern"
                        needsPattern && isConfirmingPattern -> "Confirm Unlock Pattern"
                        wantsBiometric -> "Set Fallback PIN & Enable ${selectedMethod.title}"
                        else -> "Create & Confirm 4–6 Digit PIN"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (wantsBiometric && !biometricSupported) {
                        Surface(
                            color = palette.warningContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Note: Hardware biometric enrollment wasn't detected on this device/emulator. Your fallback PIN will protect the vault until biometrics are enrolled in Android Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7A4F00),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (needsPattern) {
                        Text(
                            text = if (!isConfirmingPattern) {
                                "Connect at least 4 dots on the grid (or tap nodes in sequence)."
                            } else {
                                "Draw the same pattern again to confirm."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.inkMuted
                        )
                        val activeList = if (!isConfirmingPattern) patternFirst else patternConfirm
                        InteractivePatternGrid(
                            selectedNodes = activeList,
                            onNodeAdded = { node ->
                                if (!activeList.contains(node)) {
                                    activeList.add(node)
                                }
                            },
                            onClear = { activeList.clear() }
                        )
                        OutlinedTextField(
                            value = fallbackPinForPattern,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) fallbackPinForPattern = it },
                            label = { Text("Optional Fallback PIN (4–6 digits)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = pinStepFirst,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pinStepFirst = it },
                            label = { Text("Enter 4–6 digit PIN") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pin_input_first")
                        )
                        OutlinedTextField(
                            value = pinStepConfirm,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pinStepConfirm = it },
                            label = { Text("Confirm PIN") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pin_input_confirm")
                        )
                        OutlinedTextField(
                            value = recoveryHint,
                            onValueChange = { recoveryHint = it },
                            label = { Text("Optional Recovery Hint (never enter your PIN)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Recovery warning required by Section 5.2 of PRD
                    Surface(
                        color = palette.canvas,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.25f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { acknowledgeRecoveryWarning = !acknowledgeRecoveryWarning }
                                .padding(8.dp)
                        ) {
                            Checkbox(
                                checked = acknowledgeRecoveryWarning,
                                onCheckedChange = { acknowledgeRecoveryWarning = it },
                                colors = CheckboxDefaults.colors(checkedColor = palette.primary)
                            )
                            Text(
                                text = "I understand my credentials stay on-device and are never sent to a server.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.ink
                            )
                        }
                    }

                    if (setupError != null) {
                        Text(
                            text = setupError!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.danger
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!acknowledgeRecoveryWarning) {
                            setupError = "Please acknowledge the local storage recovery notice."
                            return@TextButton
                        }
                        if (needsPattern) {
                            if (!isConfirmingPattern) {
                                if (patternFirst.size < 4) {
                                    setupError = "Connect at least 4 dots."
                                } else {
                                    setupError = null
                                    isConfirmingPattern = true
                                }
                            } else {
                                if (patternConfirm.toList() != patternFirst.toList()) {
                                    setupError = "Patterns do not match. Try confirming again."
                                    patternConfirm.clear()
                                } else {
                                    showCredentialDialog = false
                                    onSetupPatternCompleted(
                                        patternFirst.toList(),
                                        fallbackPinForPattern.takeIf { it.length >= 4 },
                                        false
                                    )
                                }
                            }
                        } else {
                            if (pinStepFirst.length < 4) {
                                setupError = "PIN must be 4 to 6 digits."
                            } else if (pinStepFirst != pinStepConfirm) {
                                setupError = "PIN entries do not match."
                            } else {
                                showCredentialDialog = false
                                if (wantsBiometric && biometricSupported) {
                                    triggerPlatformBiometricPrompt(
                                        context = context,
                                        title = "Confirm ${selectedMethod.title}",
                                        subtitle = "Verify device biometrics for SAFEAREA",
                                        onSuccess = {
                                            onSetupPinCompleted(pinStepFirst, true, recoveryHint)
                                        },
                                        onError = {
                                            // Still enable PIN + biometric preference so fallback works
                                            onSetupPinCompleted(pinStepFirst, true, recoveryHint)
                                        }
                                    )
                                } else {
                                    onSetupPinCompleted(pinStepFirst, wantsBiometric, recoveryHint)
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_security_setup_button")
                ) {
                    Text(
                        text = if (needsPattern && !isConfirmingPattern) "Next" else "Enable Lock",
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCredentialDialog = false }) {
                    Text("Cancel", color = palette.inkMuted)
                }
            }
        )
    }
}

@Composable
private fun SecurityOptionCard(
    method: LockMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    val cardBg = if (isSelected) palette.successContainer else palette.surface
    val borderStroke = BorderStroke(
        width = if (isSelected) 1.5.dp else 1.2.dp,
        color = palette.border
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("lock_option_${method.name.lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                when (method) {
                    LockMethod.PIN -> PinDotsIcon(color = palette.ink)
                    LockMethod.PATTERN -> PatternGridIcon(color = palette.ink)
                    LockMethod.FACE_ID -> FaceScanFrameIcon(color = palette.ink)
                    LockMethod.FINGERPRINT -> Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = palette.ink,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = method.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = method.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkMuted
                )
            }

            // Right radio circle or green filled checkmark matching Screen 2 of mockup
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(palette.success),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(1.5.dp, palette.ink.copy(alpha = 0.65f), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun PinDotsIcon(color: Color) {
    Canvas(modifier = Modifier.size(34.dp)) {
        val step = size.width / 3f
        for (r in 0..2) {
            for (c in 0..2) {
                drawCircle(
                    color = color,
                    radius = 3.8f,
                    center = Offset(step * c + step / 2f, step * r + step / 2f)
                )
            }
        }
    }
}

@Composable
private fun PatternGridIcon(color: Color) {
    Canvas(modifier = Modifier.size(34.dp)) {
        val step = size.width / 3f
        fun pt(r: Int, c: Int) = Offset(step * c + step / 2f, step * r + step / 2f)

        // Diagonal line connecting pattern nodes as shown in Screen 2 of mockup
        drawLine(
            color = color,
            start = pt(0, 2),
            end = pt(2, 0),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
        for (r in 0..2) {
            for (c in 0..2) {
                drawCircle(
                    color = color,
                    radius = 3.8f,
                    center = pt(r, c),
                    style = Stroke(width = 2.5f)
                )
            }
        }
    }
}

@Composable
private fun FaceScanFrameIcon(color: Color) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(38.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.width
            val len = s * 0.26f
            val stroke = 4.5f
            // Top-left corner
            drawLine(color, Offset(0f, len), Offset(0f, 0f), stroke, StrokeCap.Round)
            drawLine(color, Offset(0f, 0f), Offset(len, 0f), stroke, StrokeCap.Round)
            // Top-right corner
            drawLine(color, Offset(s - len, 0f), Offset(s, 0f), stroke, StrokeCap.Round)
            drawLine(color, Offset(s, 0f), Offset(s, len), stroke, StrokeCap.Round)
            // Bottom-left corner
            drawLine(color, Offset(0f, s - len), Offset(0f, s), stroke, StrokeCap.Round)
            drawLine(color, Offset(0f, s), Offset(len, s), stroke, StrokeCap.Round)
            // Bottom-right corner
            drawLine(color, Offset(s - len, s), Offset(s, s), stroke, StrokeCap.Round)
            drawLine(color, Offset(s, s - len), Offset(s, s), stroke, StrokeCap.Round)
        }
        Icon(
            imageVector = Icons.Default.TagFaces,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Interactive 3x3 Pattern Drawing Grid supporting both drag gestures and node taps.
 */
@Composable
fun InteractivePatternGrid(
    selectedNodes: List<Int>,
    onNodeAdded: (Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalSafeAreaPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(palette.canvas)
                .border(1.2.dp, palette.border.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val hit = hitTestPatternNode(offset, size.width.toFloat(), size.height.toFloat())
                            if (hit != null) onNodeAdded(hit)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val hit = hitTestPatternNode(change.position, size.width.toFloat(), size.height.toFloat())
                            if (hit != null) onNodeAdded(hit)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = size.width / 3f
                val cellH = size.height / 3f
                fun centerOf(node: Int): Offset {
                    val r = node / 3
                    val c = node % 3
                    return Offset(cellW * c + cellW / 2f, cellH * r + cellH / 2f)
                }

                // Draw connecting lines
                for (i in 0 until (selectedNodes.size - 1)) {
                    drawLine(
                        color = palette.primary,
                        start = centerOf(selectedNodes[i]),
                        end = centerOf(selectedNodes[i + 1]),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                }

                // Draw 9 nodes
                for (node in 0..8) {
                    val c = centerOf(node)
                    val active = selectedNodes.contains(node)
                    drawCircle(
                        color = if (active) palette.primary else palette.ink.copy(alpha = 0.35f),
                        radius = if (active) 18f else 11f,
                        center = c
                    )
                    if (active) {
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = c
                        )
                    }
                }
            }

            // Also support accessible taps on each cell
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0..2) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0..2) {
                            val idx = r * 3 + c
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clickable { onNodeAdded(idx) }
                                    .testTag("pattern_node_$idx")
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedNodes.isEmpty()) "No nodes selected" else "Pattern: ${selectedNodes.size} dots",
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkMuted
            )
            TextButton(onClick = onClear) {
                Text("Clear Pattern", color = palette.primary)
            }
        }
    }
}

private fun hitTestPatternNode(pos: Offset, width: Float, height: Float): Int? {
    val cellW = width / 3f
    val cellH = height / 3f
    val radius = cellW * 0.38f
    for (node in 0..8) {
        val r = node / 3
        val c = node % 3
        val cx = cellW * c + cellW / 2f
        val cy = cellH * r + cellH / 2f
        if (hypot(pos.x - cx, pos.y - cy) <= radius) {
            return node
        }
    }
    return null
}

/**
 * Vault Unlock Screen with Pattern / PIN / Native BiometricPrompt and rate-limiting protection.
 */
@Composable
fun VaultUnlockScreen(
    primaryMethod: LockMethod,
    biometricEnabled: Boolean,
    hasPinSet: Boolean,
    hasPatternSet: Boolean,
    recoveryHint: String,
    lockoutSeconds: Int,
    errorMessage: String?,
    onUnlockWithPin: (String) -> Boolean,
    onUnlockWithPattern: (List<Int>) -> Boolean,
    onBiometricSuccess: () -> Unit,
    onBiometricFailed: (String) -> Unit,
    onResetVaultAfterConfirmation: () -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    val context = LocalContext.current

    var usePinFallback by remember { mutableStateOf(primaryMethod == LockMethod.PIN || !hasPatternSet) }
    var pinInput by remember { mutableStateOf("") }
    val drawnPattern = remember { mutableStateListOf<Int>() }
    var showHintDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SafeAreaShieldLogo(size = 76.dp)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "SAFEAREA is Locked",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.ink
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Authenticate to view your personal documents.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.inkMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (lockoutSeconds > 0) {
            Surface(
                color = palette.warningContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.warning)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFF8A5A00)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Too many failed attempts. Please wait ${lockoutSeconds}s before trying again.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF7A4F00)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (!usePinFallback && hasPatternSet) {
            InteractivePatternGrid(
                selectedNodes = drawnPattern,
                onNodeAdded = { node ->
                    if (lockoutSeconds == 0 && !drawnPattern.contains(node)) {
                        drawnPattern.add(node)
                    }
                },
                onClear = { drawnPattern.clear() }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SafeAreaPrimaryButton(
                text = "Unlock with Pattern",
                enabled = lockoutSeconds == 0 && drawnPattern.size >= 4,
                onClick = {
                    val ok = onUnlockWithPattern(drawnPattern.toList())
                    if (!ok) drawnPattern.clear()
                },
                modifier = Modifier.widthIn(max = 360.dp)
            )
        } else {
            OutlinedTextField(
                value = pinInput,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pinInput = it },
                label = { Text("Enter 4–6 digit PIN") },
                singleLine = true,
                enabled = lockoutSeconds == 0,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .testTag("unlock_pin_input")
            )
            Spacer(modifier = Modifier.height(14.dp))
            SafeAreaPrimaryButton(
                text = "Unlock Vault",
                enabled = lockoutSeconds == 0 && pinInput.length >= 4,
                onClick = {
                    val ok = onUnlockWithPin(pinInput)
                    if (!ok) pinInput = ""
                },
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .testTag("unlock_submit_button")
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.danger,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (biometricEnabled || primaryMethod == LockMethod.FINGERPRINT || primaryMethod == LockMethod.FACE_ID) {
            SafeAreaOutlinedPillButton(
                text = "Use Device Biometrics",
                leadingIcon = Icons.Default.Fingerprint,
                onClick = {
                    triggerPlatformBiometricPrompt(
                        context = context,
                        title = "Unlock SAFEAREA",
                        subtitle = "Authenticate with fingerprint or face",
                        onSuccess = onBiometricSuccess,
                        onError = onBiometricFailed
                    )
                },
                modifier = Modifier.widthIn(max = 360.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (hasPinSet && hasPatternSet) {
                TextButton(onClick = { usePinFallback = !usePinFallback }) {
                    Text(
                        text = if (usePinFallback) "Switch to Pattern" else "Use Fallback PIN",
                        color = palette.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            TextButton(onClick = { showHintDialog = true }) {
                Text("Forgot PIN / Recovery?", color = palette.inkMuted)
            }
        }
    }

    if (showHintDialog) {
        AlertDialog(
            onDismissRequest = { showHintDialog = false },
            containerColor = palette.surface,
            title = { Text("Credential Recovery") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (recoveryHint.isNotBlank()) {
                            "Your saved recovery hint: \"$recoveryHint\""
                        } else {
                            "No recovery hint was set during vault configuration."
                        }
                    )
                    Text(
                        text = "Because SAFEAREA encrypts data locally without a cloud account, resetting your lock requires wiping stored vault data to protect your privacy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHintDialog = false
                        onResetVaultAfterConfirmation()
                    }
                ) {
                    Text("Wipe & Reset Vault", color = palette.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHintDialog = false }) {
                    Text("Cancel", color = palette.ink)
                }
            }
        )
    }
}

fun isDeviceBiometricCapable(context: Context): Boolean {
    val pm = context.packageManager
    val hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        pm.hasSystemFeature(PackageManager.FEATURE_FACE)
    } else false
    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    return (hasFingerprint || hasFace) && (keyguard?.isDeviceSecure == true)
}

fun triggerPlatformBiometricPrompt(
    context: Context,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        onError("Native BiometricPrompt requires Android 9+. Please use your PIN or Pattern.")
        return
    }
    if (!isDeviceBiometricCapable(context)) {
        onError("No enrolled biometric or screen lock found on this device. Please use your fallback PIN or Pattern.")
        return
    }
    try {
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt.Builder(context)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton(
                "Use PIN / Pattern",
                executor,
                DialogInterface.OnClickListener { _, _ ->
                    onError("Switched to PIN / Pattern fallback.")
                }
            )
            .build()

        prompt.authenticate(
            CancellationSignal(),
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString?.toString() ?: "Biometric authentication canceled.")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Biometric not recognized. Try again or use PIN.")
                }
            }
        )
    } catch (e: Exception) {
        onError("Biometric sensor unavailable (${e.localizedMessage ?: "fallback to PIN"}).")
    }
}
