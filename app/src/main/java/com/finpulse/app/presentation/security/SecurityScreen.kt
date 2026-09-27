package com.finpulse.app.presentation.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.finpulse.app.R
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.designsystem.EmeraldPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    userPreferencesDataStore: UserPreferencesDataStore,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userPrefs by userPreferencesDataStore.userPreferencesFlow.collectAsState(
        initial = com.finpulse.app.core.datastore.UserPreferences()
    )
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var isPinDialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Biometrics Switch
                    SecurityRow(
                        title = stringResource(R.string.security_biometric),
                        subtitle = stringResource(R.string.security_biometric_desc),
                        checked = userPrefs.isBiometricEnabled,
                        onCheckedChange = { scope.launch { userPreferencesDataStore.setBiometricEnabled(it) } }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // PIN Switch
                    SecurityRow(
                        title = stringResource(R.string.security_pin),
                        subtitle = if (userPrefs.isPinEnabled) stringResource(R.string.security_pin_active) else stringResource(R.string.security_pin_desc),
                        checked = userPrefs.isPinEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                isPinDialogVisible = true
                            } else {
                                scope.launch { userPreferencesDataStore.setPin("") }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Screenshot Protection (FLAG_SECURE)
                    SecurityRow(
                        title = stringResource(R.string.security_screenshot_protection),
                        subtitle = stringResource(R.string.security_screenshot_protection_desc),
                        checked = userPrefs.enableScreenshotProtection,
                        onCheckedChange = { scope.launch { userPreferencesDataStore.setScreenshotProtection(it) } }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hide Balances
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

        if (isPinDialogVisible) {
            SetPinDialog(
                onDismiss = { isPinDialogVisible = false },
                onPinSet = { pin ->
                    scope.launch {
                        userPreferencesDataStore.setPin(pin)
                        isPinDialogVisible = false
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
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text(stringResource(R.string.security_enter_pin)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6) confirmPin = it },
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
