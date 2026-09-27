package com.finpulse.app.presentation.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finpulse.app.R
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.security.AppLockManager
import com.finpulse.app.core.security.BiometricAuthManager
import com.finpulse.app.core.security.BiometricAvailability
import com.finpulse.app.core.security.LockTimeoutPolicy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    userPreferencesDataStore: UserPreferencesDataStore,
    appLockManager: AppLockManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userPrefs by userPreferencesDataStore.userPreferencesFlow.collectAsState(
        initial = com.finpulse.app.core.datastore.UserPreferences()
    )
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var isSetPinDialogVisible by remember { mutableStateOf(false) }
    var isChangePinDialogVisible by remember { mutableStateOf(false) }
    var isDisablePinDialogVisible by remember { mutableStateOf(false) }
    var isTimeoutDialogVisible by remember { mutableStateOf(false) }

    val biometricAvailability = remember(context) {
        BiometricAuthManager.checkAvailability(context, userPrefs.deviceCredentialFallbackEnabled)
    }

    val biometricSubtitle = when (biometricAvailability) {
        BiometricAvailability.AVAILABLE -> stringResource(R.string.security_biometric_available)
        BiometricAvailability.NONE_ENROLLED -> stringResource(R.string.security_biometric_not_enrolled)
        BiometricAvailability.NO_HARDWARE -> stringResource(R.string.security_biometric_no_hardware)
        else -> stringResource(R.string.security_biometric_desc)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.security_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Authentication & App Lock
            Text(
                text = stringResource(R.string.security_section_auth),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary,
                modifier = Modifier.padding(start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Biometrics Toggle
                    SecurityRow(
                        title = stringResource(R.string.security_biometric),
                        subtitle = biometricSubtitle,
                        checked = userPrefs.isBiometricEnabled,
                        onCheckedChange = { enable ->
                            if (enable && biometricAvailability == BiometricAvailability.NONE_ENROLLED) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.security_biometric_not_enrolled)
                                    )
                                }
                            }
                            scope.launch { userPreferencesDataStore.setBiometricEnabled(enable) }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // PIN Code Toggle
                    SecurityRow(
                        title = stringResource(R.string.security_pin),
                        subtitle = if (userPrefs.isPinEnabled) {
                            stringResource(R.string.security_pin_active)
                        } else {
                            stringResource(R.string.security_pin_desc)
                        },
                        checked = userPrefs.isPinEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                isSetPinDialogVisible = true
                            } else {
                                isDisablePinDialogVisible = true
                            }
                        }
                    )

                    // Change PIN action row when PIN is enabled
                    AnimatedVisibility(visible = userPrefs.isPinEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isChangePinDialogVisible = true }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.security_change_pin),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = EmeraldPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Device Credential Fallback
                    SecurityRow(
                        title = stringResource(R.string.security_device_credential),
                        subtitle = stringResource(R.string.security_device_credential_desc),
                        checked = userPrefs.deviceCredentialFallbackEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { userPreferencesDataStore.setDeviceCredentialFallbackEnabled(enabled) }
                        }
                    )
                }
            }

            // Section 2: Lock Policy
            Text(
                text = stringResource(R.string.security_section_policy),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary,
                modifier = Modifier.padding(start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Auto-Lock Timeout Selector Row
                    val currentPolicy = LockTimeoutPolicy.fromName(userPrefs.lockTimeout)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTimeoutDialogVisible = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.security_timeout_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(currentPolicy.displayNameResId),
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Immediate Lock App Now button
                    Button(
                        onClick = { appLockManager.lockNow() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.security_lock_now_btn),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Section 3: Privacy & Screen Protection
            Text(
                text = stringResource(R.string.security_section_privacy),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary,
                modifier = Modifier.padding(start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Screenshot Shield (FLAG_SECURE)
                    SecurityRow(
                        title = stringResource(R.string.security_screenshot_protection),
                        subtitle = stringResource(R.string.security_screenshot_protection_desc),
                        checked = userPrefs.enableScreenshotProtection,
                        onCheckedChange = { scope.launch { userPreferencesDataStore.setScreenshotProtection(it) } }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hide Balances (Privacy Mode)
                    SecurityRow(
                        title = stringResource(R.string.security_hide_balances),
                        subtitle = stringResource(R.string.security_hide_balances_desc),
                        checked = userPrefs.hideBalances,
                        onCheckedChange = { scope.launch { userPreferencesDataStore.setHideBalances(it) } }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Widget Privacy Mode
                    SecurityRow(
                        title = stringResource(R.string.security_widget_privacy),
                        subtitle = stringResource(R.string.security_widget_privacy_desc),
                        checked = userPrefs.widgetPrivacyEnabled,
                        onCheckedChange = {
                            scope.launch {
                                userPreferencesDataStore.setWidgetPrivacyEnabled(it)
                                com.finpulse.app.presentation.widget.FinPulseWidgetUpdater.updateAllWidgets(context)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mask Widgets on App Lock
                    SecurityRow(
                        title = stringResource(R.string.security_widget_mask_lock),
                        subtitle = stringResource(R.string.security_widget_mask_lock_desc),
                        checked = userPrefs.widgetMaskOnAppLock,
                        onCheckedChange = {
                            scope.launch {
                                userPreferencesDataStore.setWidgetMaskOnAppLock(it)
                                com.finpulse.app.presentation.widget.FinPulseWidgetUpdater.updateAllWidgets(context)
                            }
                        }
                    )
                }
            }
        }

        // Set Initial PIN Dialog
        if (isSetPinDialogVisible) {
            SetPinDialog(
                onDismiss = { isSetPinDialogVisible = false },
                onPinSet = { pin ->
                    scope.launch {
                        userPreferencesDataStore.setPin(pin)
                        isSetPinDialogVisible = false
                        snackbarHostState.showSnackbar(context.getString(R.string.security_pin_set_success))
                    }
                }
            )
        }

        // Change Existing PIN Dialog
        if (isChangePinDialogVisible) {
            ChangePinDialog(
                userPrefs = userPrefs,
                appLockManager = appLockManager,
                onDismiss = { isChangePinDialogVisible = false },
                onPinChanged = { newPin ->
                    scope.launch {
                        userPreferencesDataStore.setPin(newPin)
                        isChangePinDialogVisible = false
                        snackbarHostState.showSnackbar(context.getString(R.string.security_pin_set_success))
                    }
                }
            )
        }

        // Confirm Disable PIN Dialog
        if (isDisablePinDialogVisible) {
            DisablePinDialog(
                userPrefs = userPrefs,
                appLockManager = appLockManager,
                onDismiss = { isDisablePinDialogVisible = false },
                onPinVerified = {
                    scope.launch {
                        userPreferencesDataStore.setPin("")
                        isDisablePinDialogVisible = false
                    }
                }
            )
        }

        // Auto-Lock Timeout Picker Dialog
        if (isTimeoutDialogVisible) {
            TimeoutSelectionDialog(
                currentTimeout = userPrefs.lockTimeout,
                onDismiss = { isTimeoutDialogVisible = false },
                onTimeoutSelected = { selectedPolicy ->
                    scope.launch {
                        userPreferencesDataStore.setLockTimeout(selectedPolicy.name)
                        isTimeoutDialogVisible = false
                    }
                }
            )
        }
    }
}

@Composable
fun SecurityRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
        )
    }
}

@Composable
fun SetPinDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pinMinDigitsMsg = stringResource(R.string.security_pin_min_digits)
    val pinMismatchMsg = stringResource(R.string.security_pin_mismatch)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.security_pin_configure_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text(stringResource(R.string.security_enter_pin)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) confirmPin = it },
                    label = { Text(stringResource(R.string.security_confirm_pin)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length < 4) {
                        errorMessage = pinMinDigitsMsg
                        return@Button
                    }
                    if (pin != confirmPin) {
                        errorMessage = pinMismatchMsg
                        return@Button
                    }
                    onPinSet(pin)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.security_pin_save_btn), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun ChangePinDialog(
    userPrefs: com.finpulse.app.core.datastore.UserPreferences,
    appLockManager: AppLockManager,
    onDismiss: () -> Unit,
    onPinChanged: (String) -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val incorrectCurrentMsg = stringResource(R.string.security_current_pin_incorrect)
    val pinMinDigitsMsg = stringResource(R.string.security_pin_min_digits)
    val pinMismatchMsg = stringResource(R.string.security_pin_mismatch)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.security_change_pin), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) currentPin = it },
                    label = { Text(stringResource(R.string.security_current_pin)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text(stringResource(R.string.security_new_pin)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmNewPin = it },
                    label = { Text(stringResource(R.string.security_confirm_new_pin)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isCurrentValid = appLockManager.verifyPin(
                        enteredPin = currentPin,
                        storedHash = userPrefs.pinHash,
                        saltBase64 = userPrefs.pinSalt
                    )
                    if (!isCurrentValid) {
                        errorMessage = incorrectCurrentMsg
                        return@Button
                    }
                    if (newPin.length < 4) {
                        errorMessage = pinMinDigitsMsg
                        return@Button
                    }
                    if (newPin != confirmNewPin) {
                        errorMessage = pinMismatchMsg
                        return@Button
                    }
                    onPinChanged(newPin)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.security_pin_save_btn), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun DisablePinDialog(
    userPrefs: com.finpulse.app.core.datastore.UserPreferences,
    appLockManager: AppLockManager,
    onDismiss: () -> Unit,
    onPinVerified: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val incorrectPinMsg = stringResource(R.string.security_pin_incorrect)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.security_current_pin), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.security_lock_enter_pin), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) enteredPin = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isValid = appLockManager.verifyPin(
                        enteredPin = enteredPin,
                        storedHash = userPrefs.pinHash,
                        saltBase64 = userPrefs.pinSalt
                    )
                    if (isValid) {
                        onPinVerified()
                    } else {
                        errorMessage = incorrectPinMsg
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.action_confirm), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun TimeoutSelectionDialog(
    currentTimeout: String,
    onDismiss: () -> Unit,
    onTimeoutSelected: (LockTimeoutPolicy) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.security_timeout_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LockTimeoutPolicy.entries.forEach { policy ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTimeoutSelected(policy) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = policy.name == currentTimeout,
                            onClick = { onTimeoutSelected(policy) },
                            colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(policy.displayNameResId),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
