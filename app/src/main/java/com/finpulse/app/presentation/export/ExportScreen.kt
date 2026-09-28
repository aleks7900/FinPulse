package com.finpulse.app.presentation.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.finpulse.app.R
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.ExpenseRed
import com.finpulse.app.core.designsystem.SapphireAccent
import com.finpulse.app.domain.model.DateRangePreset
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var isEncryptChecked by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var exportFormatIsCsv by remember { mutableStateOf(true) }

    var restoreDecryptPassword by remember { mutableStateOf("") }
    var isRestorePasswordVisible by remember { mutableStateOf(false) }

    // Launcher for selecting a backup file to restore
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                }.orEmpty()
                viewModel.onFileContentSelectedForRestore(content)
            } catch (e: Exception) {
                // Handled in viewModel
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.export_screen_title),
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Periodic Backup Reminder Banner (if due)
            if (uiState.isReminderDue) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.backup_reminder_due_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val days = if (uiState.backupReminderInterval == "WEEKLY") 7 else 30
                        Text(
                            text = stringResource(R.string.backup_reminder_due_desc, days),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.createBackup(
                                        password = if (isEncryptChecked && backupPassword.isNotBlank()) backupPassword else null
                                    ) { payload ->
                                        shareTextFile(context, payload.filename, payload.content, "application/json")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(R.string.backup_create_btn), color = Color.White)
                            }
                            OutlinedButton(onClick = { viewModel.dismissReminder() }) {
                                Text(stringResource(R.string.action_dismiss))
                            }
                        }
                    }
                }
            }

            // 2. Status & Reminder Settings Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = SapphireAccent, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.backup_reminder_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val dateFormatted = uiState.lastBackupTimestamp?.let {
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(it))
                    }
                    Text(
                        text = if (dateFormatted != null) {
                            stringResource(R.string.backup_last_date, dateFormatted)
                        } else {
                            stringResource(R.string.backup_never)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.backup_reminder_interval_label),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.backupReminderInterval == "OFF",
                            onClick = { viewModel.setReminderInterval("OFF") },
                            label = { Text(stringResource(R.string.backup_reminder_off)) }
                        )
                        FilterChip(
                            selected = uiState.backupReminderInterval == "WEEKLY",
                            onClick = { viewModel.setReminderInterval("WEEKLY") },
                            label = { Text(stringResource(R.string.backup_reminder_weekly)) }
                        )
                        FilterChip(
                            selected = uiState.backupReminderInterval == "MONTHLY",
                            onClick = { viewModel.setReminderInterval("MONTHLY") },
                            label = { Text(stringResource(R.string.backup_reminder_monthly)) }
                        )
                    }
                }
            }

            // 3. Full Database Backup Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.export_json_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.export_json_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isEncryptChecked,
                            onCheckedChange = { isEncryptChecked = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(stringResource(R.string.backup_encrypt_toggle), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.backup_encrypt_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    AnimatedVisibility(visible = isEncryptChecked) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedTextField(
                                value = backupPassword,
                                onValueChange = { backupPassword = it },
                                label = { Text(stringResource(R.string.backup_password_label)) },
                                placeholder = { Text(stringResource(R.string.backup_password_hint)) },
                                singleLine = true,
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val pass = if (isEncryptChecked && backupPassword.isNotBlank()) backupPassword else null
                            viewModel.createBackup(password = pass) { payload ->
                                shareTextFile(context, payload.filename, payload.content, "application/json")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        enabled = !uiState.isLoading && (!isEncryptChecked || backupPassword.isNotBlank())
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(stringResource(R.string.backup_create_btn), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 4. Restore from Backup Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null, tint = SapphireAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.backup_restore_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.backup_restore_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { filePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = SapphireAccent),
                        enabled = !uiState.isLoading
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.backup_restore_select_btn), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. Filtered Transactions Export Card (CSV & JSON)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.TableChart, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.export_filter_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.export_csv_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Date range selection
                    Text(stringResource(R.string.export_filter_date_label), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.filterParams.dateRangePreset == DateRangePreset.ALL,
                            onClick = { viewModel.setFilterDatePreset(DateRangePreset.ALL) },
                            label = { Text(stringResource(R.string.date_preset_all)) }
                        )
                        FilterChip(
                            selected = uiState.filterParams.dateRangePreset == DateRangePreset.THIS_MONTH,
                            onClick = { viewModel.setFilterDatePreset(DateRangePreset.THIS_MONTH) },
                            label = { Text(stringResource(R.string.date_preset_this_month)) }
                        )
                        FilterChip(
                            selected = uiState.filterParams.dateRangePreset == DateRangePreset.LAST_MONTH,
                            onClick = { viewModel.setFilterDatePreset(DateRangePreset.LAST_MONTH) },
                            label = { Text(stringResource(R.string.date_preset_last_month)) }
                        )
                        FilterChip(
                            selected = uiState.filterParams.dateRangePreset == DateRangePreset.THIS_YEAR,
                            onClick = { viewModel.setFilterDatePreset(DateRangePreset.THIS_YEAR) },
                            label = { Text(stringResource(R.string.date_preset_this_year)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Account filter
                    Text(stringResource(R.string.export_filter_account_label), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = uiState.filterParams.selectedAccountId == null,
                            onClick = { viewModel.setSelectedAccount(null) },
                            label = { Text(stringResource(R.string.export_filter_all_accounts)) }
                        )
                        uiState.accounts.forEach { acc ->
                            FilterChip(
                                selected = uiState.filterParams.selectedAccountId == acc.id,
                                onClick = { viewModel.setSelectedAccount(acc.id) },
                                label = { Text(acc.name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Export Format: CSV or JSON
                    Text(stringResource(R.string.export_filter_format_label), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = exportFormatIsCsv,
                                onClick = { exportFormatIsCsv = true }
                            )
                            Text(stringResource(R.string.export_format_csv_option), style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !exportFormatIsCsv,
                                onClick = { exportFormatIsCsv = false }
                            )
                            Text(stringResource(R.string.export_format_json_tx), style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val timestamp = System.currentTimeMillis()
                            if (exportFormatIsCsv) {
                                viewModel.exportCsv { csvData ->
                                    shareTextFile(context, "finpulse_transactions_${timestamp}.csv", csvData, "text/csv")
                                }
                            } else {
                                viewModel.exportFilteredJson { jsonData ->
                                    shareTextFile(context, "finpulse_transactions_${timestamp}.json", jsonData, "application/json")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        enabled = !uiState.isLoading
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.export_transactions_btn), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // Password Prompt for Decryption
    if (uiState.isPasswordPromptVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPasswordPrompt() },
            icon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SapphireAccent) },
            title = { Text(stringResource(R.string.backup_restore_decrypt_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.backup_restore_decrypt_prompt))
                    OutlinedTextField(
                        value = restoreDecryptPassword,
                        onValueChange = { restoreDecryptPassword = it },
                        label = { Text(stringResource(R.string.backup_password_label)) },
                        singleLine = true,
                        visualTransformation = if (isRestorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { isRestorePasswordVisible = !isRestorePasswordVisible }) {
                                Icon(
                                    imageVector = if (isRestorePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitPasswordForRestore(restoreDecryptPassword)
                        restoreDecryptPassword = ""
                    },
                    enabled = restoreDecryptPassword.isNotBlank()
                ) {
                    Text(stringResource(R.string.backup_restore_decrypt_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPasswordPrompt() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Restore Inspection & Confirmation Dialog
    if (uiState.isRestoreConfirmDialogVisible) {
        val validation = uiState.validationResult
        val metadata = validation?.metadata

        AlertDialog(
            onDismissRequest = { viewModel.dismissRestoreConfirmDialog() },
            icon = {
                Icon(
                    imageVector = if (validation?.isValid == true) Icons.Default.Warning else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (validation?.isValid == true) ExpenseRed else MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.backup_restore_confirm_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (metadata != null) {
                        val dateStr = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(metadata.createdAt))
                        Text(stringResource(R.string.backup_restore_inspect_date, dateStr), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.backup_restore_inspect_version, metadata.backupVersion, metadata.schemaVersion), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.export_records_to_restore), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("• " + stringResource(R.string.backup_restore_count_accounts, metadata.counts.accountsCount), style = MaterialTheme.typography.bodySmall)
                        Text("• " + stringResource(R.string.backup_restore_count_categories, metadata.counts.categoriesCount), style = MaterialTheme.typography.bodySmall)
                        Text("• " + stringResource(R.string.backup_restore_count_transactions, metadata.counts.transactionsCount), style = MaterialTheme.typography.bodySmall)
                        Text("• " + stringResource(R.string.backup_restore_count_budgets, metadata.counts.budgetsCount), style = MaterialTheme.typography.bodySmall)
                        Text("• " + stringResource(R.string.backup_restore_count_recurring, metadata.counts.recurringRulesCount), style = MaterialTheme.typography.bodySmall)
                        Text("• " + stringResource(R.string.backup_restore_count_goals, metadata.counts.financialGoalsCount), style = MaterialTheme.typography.bodySmall)
                    }

                    if (validation?.warnings?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.backup_restore_warnings_title), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        validation.warnings.forEach { warning ->
                            Text("⚠ $warning", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    if (validation?.errors?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.backup_restore_errors_title), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        validation.errors.forEach { err ->
                            Text("❌ $err", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.backup_restore_confirm_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRestore() },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    enabled = validation?.isValid == true && !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(stringResource(R.string.backup_restore_confirm_btn), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestoreConfirmDialog() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Restore Success Summary Dialog
    if (uiState.restoreSummary != null) {
        val summary = uiState.restoreSummary!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestoreSummary() },
            icon = { Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary) },
            title = { Text(stringResource(R.string.backup_restore_summary_title)) },
            text = {
                Text(stringResource(R.string.backup_restore_summary_desc, summary.totalRestoredRecords))
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissRestoreSummary() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(stringResource(R.string.action_done), color = Color.Black)
                }
            }
        )
    }
}

private fun shareTextFile(context: Context, filename: String, content: String, mimeType: String) {
    try {
        val file = File(context.cacheDir, filename)
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.export_share_dialog_title)))
    } catch (_: Exception) {
        // Fallback to text extra if file sharing fails
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_SUBJECT, filename)
        }
        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.export_share_dialog_title)))
    }
}
