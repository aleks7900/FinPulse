package com.finpulse.app.presentation.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.finpulse.app.R
import com.finpulse.app.core.datastore.UserPreferences
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.ExpenseRed
import com.finpulse.app.core.designsystem.SapphireAccent
import com.finpulse.app.core.security.AppLockManager
import com.finpulse.app.core.security.BiometricAuthManager
import com.finpulse.app.core.security.BiometricAvailability
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LockScreen(
    userPrefs: UserPreferences,
    appLockManager: AppLockManager,
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    val biometricAvailability = remember(context) {
        BiometricAuthManager.checkAvailability(context, userPrefs.deviceCredentialFallbackEnabled)
    }

    val canUseBiometrics = userPrefs.isBiometricEnabled &&
            (biometricAvailability == BiometricAvailability.AVAILABLE)

    fun launchBiometricPrompt(allowDeviceCredential: Boolean = userPrefs.deviceCredentialFallbackEnabled) {
        val activity = context as? FragmentActivity ?: return
        BiometricAuthManager.authenticate(
            activity = activity,
            title = context.getString(R.string.security_biometric_title),
            subtitle = context.getString(R.string.security_biometric_subtitle),
            negativeButtonText = context.getString(R.string.action_cancel),
            allowDeviceCredential = allowDeviceCredential,
            onSuccess = {
                appLockManager.unlock()
                onUnlockSuccess()
            },
            onError = { errorCode, errString ->
                if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    errorMessage = errString.toString()
                }
            },
            onFailed = {
                errorMessage = context.getString(R.string.security_biometric_failed)
            }
        )
    }

    // Trigger biometric prompt on first display if enabled
    LaunchedEffect(Unit) {
        if (canUseBiometrics) {
            launchBiometricPrompt()
        }
    }

    fun triggerShake() {
        coroutineScope.launch {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 20f,
                animationSpec = tween(durationMillis = 50)
            )
            shakeOffset.animateTo(
                targetValue = -20f,
                animationSpec = tween(durationMillis = 50)
            )
            shakeOffset.animateTo(
                targetValue = 15f,
                animationSpec = tween(durationMillis = 50)
            )
            shakeOffset.animateTo(
                targetValue = -15f,
                animationSpec = tween(durationMillis = 50)
            )
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 50)
            )
        }
    }

    fun handleDigitEntered(digit: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        errorMessage = null
        if (enteredPin.length < 6) {
            val updated = enteredPin + digit
            enteredPin = updated

            // Auto-verify when 4 to 6 digits match
            if (updated.length >= 4) {
                val isValid = appLockManager.verifyPin(
                    enteredPin = updated,
                    storedHash = userPrefs.pinHash,
                    saltBase64 = userPrefs.pinSalt
                )
                if (isValid) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUnlockSuccess()
                } else if (updated.length == 6) {
                    // Maximum digits reached and incorrect
                    triggerShake()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    errorMessage = context.getString(R.string.security_pin_incorrect)
                    enteredPin = ""
                }
            }
        }
    }

    fun handleBackspace() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        errorMessage = null
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume touch events to prevent click-through */ },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: App Logo & Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(EmeraldPrimary, SapphireAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "FinPulse Locked",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (userPrefs.isPinEnabled) {
                        stringResource(R.string.security_lock_enter_pin)
                    } else {
                        stringResource(R.string.security_lock_authenticate)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // PIN Dots Indicator & Error Feedback
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            ) {
                if (userPrefs.isPinEnabled) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val maxDots = maxOf(4, enteredPin.length)
                        for (i in 0 until maxDots) {
                            val isFilled = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ExpenseRed,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // PIN Keypad / Biometric Controls
            if (userPrefs.isPinEnabled) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    val digits = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9")
                    )

                    for (row in digits) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            for (digit in row) {
                                KeypadButton(
                                    text = digit,
                                    onClick = { handleDigitEntered(digit) }
                                )
                            }
                        }
                    }

                    // Bottom Row: Biometric trigger / Clear, 0, Backspace
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (canUseBiometrics) {
                            KeypadIconButton(
                                icon = Icons.Default.Fingerprint,
                                contentDescription = stringResource(R.string.security_unlock_biometric),
                                onClick = { launchBiometricPrompt() }
                            )
                        } else {
                            KeypadActionButton(
                                text = "C",
                                onClick = {
                                    enteredPin = ""
                                    errorMessage = null
                                }
                            )
                        }

                        KeypadButton(
                            text = "0",
                            onClick = { handleDigitEntered("0") }
                        )

                        KeypadIconButton(
                            icon = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = stringResource(R.string.action_delete),
                            onClick = { handleBackspace() }
                        )
                    }
                }
            } else {
                // Biometrics only / Device Credential only
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    if (canUseBiometrics) {
                        Button(
                            onClick = { launchBiometricPrompt() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.security_unlock_biometric),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    if (userPrefs.deviceCredentialFallbackEnabled) {
                        OutlinedButton(
                            onClick = { launchBiometricPrompt(allowDeviceCredential = true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Password, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.security_unlock_device_credential),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun KeypadActionButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
