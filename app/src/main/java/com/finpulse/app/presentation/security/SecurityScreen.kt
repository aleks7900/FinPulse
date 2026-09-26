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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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
    var isPinDialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Security & Privacy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        title = "Biometric Authentication",
                        subtitle = "Unlock FinPulse using fingerprint or face scan",
                        checked = userPrefs.isBiometricEnabled,
                        onCheckedChange = { scope.launch { userPreferencesDataStore.setBiometricEnabled(it) } }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // PIN Switch
                    SecurityRow(
                        title = "App PIN Code",
                        subtitle = if (userPrefs.isPinEnabled) "PIN protection is active" else "Protect app with 4-digit code",
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
                        title = "Screenshot Protection",
                        subtitle = "Prevents screenshots and hides app previews in Android recent apps",
                        checked = userPrefs.enableScreenshotProtection,
                        onCheckedChange = { scope.launch { userPreferencesDataStore.setScreenshotProtection(it) } }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hide Balances
                    SecurityRow(
                        title = "Privacy Mode (Hide Balances)",
                        subtitle = "Mask amounts with dots across dashboard and lists",
                        checked = userPrefs.hideBalances,
                        onCheckedChange = { scope.launch { userPreferencesDataStore.setHideBalances(it) } }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Security PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text("Enter PIN (4-6 digits)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6) confirmPin = it },
                    label = { Text("Confirm PIN") },
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
                        errorMessage = "PIN must be at least 4 digits"
                        return@Button
                    }
                    if (pin != confirmPin) {
                        errorMessage = "PINs do not match"
                        return@Button
                    }
                    onPinSet(pin)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save PIN", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
