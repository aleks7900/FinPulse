package com.finpulse.app.presentation.csvimport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finpulse.app.R
import com.finpulse.app.core.designsystem.AmberWarning
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.PurpleAccent
import com.finpulse.app.core.designsystem.SapphireAccent
import com.finpulse.app.core.ui.DateFormatterUtils
import com.finpulse.app.core.ui.getLocalizedName
import com.finpulse.app.domain.model.AmountMode
import com.finpulse.app.domain.model.CsvColumnMapping
import com.finpulse.app.domain.model.CsvFormatConfig
import com.finpulse.app.domain.model.DuplicateStatus
import com.finpulse.app.domain.model.ParsedCsvRow
import com.finpulse.app.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportScreen(
    viewModel: CsvImportViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                }.orEmpty()
                val fileName = queryFileName(context, uri) ?: "statement.csv"
                viewModel.onFileLoaded(fileName, content)
            } catch (e: Exception) {
                // Ignore or handle
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.csv_import_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (uiState.currentStep) {
                                ImportStep.SELECT_FILE -> stringResource(R.string.csv_step_select_file)
                                ImportStep.MAPPING_CONFIG -> stringResource(R.string.csv_step_mapping)
                                ImportStep.PREVIEW_REVIEW -> stringResource(R.string.csv_step_preview)
                                ImportStep.IMPORT_SUMMARY -> stringResource(R.string.csv_step_summary)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (uiState.currentStep) {
                                ImportStep.SELECT_FILE -> onNavigateBack()
                                ImportStep.MAPPING_CONFIG -> viewModel.onBackToFileSelect()
                                ImportStep.PREVIEW_REVIEW -> viewModel.onBackToMapping()
                                ImportStep.IMPORT_SUMMARY -> onNavigateToTransactions()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (uiState.currentStep != ImportStep.SELECT_FILE && uiState.currentStep != ImportStep.IMPORT_SUMMARY) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.action_reset)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Stepper breadcrumbs
                ImportStepperHeader(currentStep = uiState.currentStep)

                when (uiState.currentStep) {
                    ImportStep.SELECT_FILE -> {
                        SelectFileView(
                            onSelectFile = {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "text/*",
                                        "text/comma-separated-values",
                                        "application/csv",
                                        "application/vnd.ms-excel"
                                    )
                                )
                            },
                            onLoadDemoData = { demoName, demoContent ->
                                viewModel.onFileLoaded(demoName, demoContent)
                            }
                        )
                    }
                    ImportStep.MAPPING_CONFIG -> {
                        MappingConfigView(
                            uiState = uiState,
                            onProfileSelected = viewModel::onSelectProfile,
                            onFormatConfigChange = viewModel::onFormatConfigChange,
                            onColumnMappingChange = viewModel::onColumnMappingChange,
                            onSelectAccount = viewModel::onSelectAccount,
                            onShowSaveProfileDialog = { viewModel.onShowSaveProfileDialog(true) },
                            onProceed = viewModel::proceedToPreview
                        )
                    }
                    ImportStep.PREVIEW_REVIEW -> {
                        PreviewReviewView(
                            uiState = uiState,
                            onTabSelected = viewModel::onSetPreviewTab,
                            onToggleExclusion = viewModel::onToggleRowExclusion,
                            onExcludeAllDuplicates = viewModel::onExcludeAllDuplicates,
                            onIncludeAll = viewModel::onIncludeAll,
                            onExcludeAll = viewModel::onExcludeAll,
                            onBackToMapping = viewModel::onBackToMapping,
                            onExecuteImport = viewModel::executeImport
                        )
                    }
                    ImportStep.IMPORT_SUMMARY -> {
                        uiState.importSummary?.let { summary ->
                            ImportSummaryView(
                                summary = summary,
                                onDone = onNavigateToTransactions,
                                onImportAnother = viewModel::reset
                            )
                        }
                    }
                }
            }

            // Loading overlay
            if (uiState.isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = EmeraldPrimary)
                                Text(
                                    text = uiState.loadingMessage.ifEmpty { "Processing statement..." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Error banner
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::onClearError) {
                            Text(stringResource(R.string.action_dismiss), color = Color.White)
                        }
                    },
                    containerColor = CrimsonExpense
                ) {
                    Text(text = error, color = Color.White)
                }
            }

            // Save profile dialog
            if (uiState.isSaveProfileDialogOpen) {
                SaveProfileDialog(
                    onDismiss = { viewModel.onShowSaveProfileDialog(false) },
                    onConfirm = { name, institution ->
                        viewModel.onSaveNewProfile(name, institution)
                    }
                )
            }
        }
    }
}

@Composable
private fun ImportStepperHeader(currentStep: ImportStep) {
    val steps = listOf(
        ImportStep.SELECT_FILE to stringResource(R.string.csv_step_select_file).replace(Regex("^\\d+\\.\\s*"), ""),
        ImportStep.MAPPING_CONFIG to stringResource(R.string.csv_step_mapping).replace(Regex("^\\d+\\.\\s*"), ""),
        ImportStep.PREVIEW_REVIEW to stringResource(R.string.csv_step_preview).replace(Regex("^\\d+\\.\\s*"), ""),
        ImportStep.IMPORT_SUMMARY to stringResource(R.string.csv_step_summary).replace(Regex("^\\d+\\.\\s*"), "")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, (step, label) ->
            val isCurrent = step == currentStep
            val isPast = step.ordinal < currentStep.ordinal

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        isCurrent -> EmeraldPrimary
                        isPast -> EmeraldPrimary.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isPast) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .height(2.dp)
                        .background(
                            if (isPast) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectFileView(
    onSelectFile: () -> Unit,
    onLoadDemoData: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Hero Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Text(
                    text = "Import Bank Statement CSV",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Upload bank or credit card statements exported in CSV format. FinPulse will automatically detect date formats, amounts, and column structures.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onSelectFile,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.csv_select_file_btn),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Privacy Note
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "100% On-Device & Private. Never sent to the cloud.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Demo Datasets Section for instant testing
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SapphireAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Quick Demo Statements",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Don't have a file ready? Load a sample bank statement to test mapping, duplicate detection, and categorization:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DemoButton(
                    title = "Standard Bank Export (US Style)",
                    description = "Date, Description, Amount (negative for expense)",
                    onClick = {
                        onLoadDemoData("demo_us_statement.csv", DemoDataSets.US_STANDARD_CSV)
                    }
                )

                DemoButton(
                    title = "European Statement (Semicolon & Comma Decimal)",
                    description = "dd.MM.yyyy, German format, 12,50 EUR",
                    onClick = {
                        onLoadDemoData("demo_european_statement.csv", DemoDataSets.EUROPEAN_CSV)
                    }
                )

                DemoButton(
                    title = "Split Debit / Credit Columns",
                    description = "Separate columns for Outflow vs Inflow",
                    onClick = {
                        onLoadDemoData("demo_split_columns.csv", DemoDataSets.SPLIT_DEBIT_CREDIT_CSV)
                    }
                )
            }
        }
    }
}

@Composable
private fun DemoButton(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = SapphireAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MappingConfigView(
    uiState: CsvImportUiState,
    onProfileSelected: (String) -> Unit,
    onFormatConfigChange: (CsvFormatConfig) -> Unit,
    onColumnMappingChange: (CsvColumnMapping) -> Unit,
    onSelectAccount: (String) -> Unit,
    onShowSaveProfileDialog: () -> Unit,
    onProceed: () -> Unit
) {
    val formatConfig = uiState.formatConfig
    val mapping = uiState.columnMapping
    val headers = uiState.detectedHeaders

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // File Info Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SapphireAccent.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = SapphireAccent
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.fileName ?: "statement.csv",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${headers.size} columns detected • ${uiState.sampleRows.size} sample rows",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Profile Selector & Save Profile
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Import Profile",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onShowSaveProfileDialog) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Profile")
                    }
                }

                var profileExpanded by remember { mutableStateOf(false) }
                val currentProfile = uiState.availableProfiles.find { it.id == uiState.selectedProfileId }

                ExposedDropdownMenuBox(
                    expanded = profileExpanded,
                    onExpandedChange = { profileExpanded = !profileExpanded }
                ) {
                    OutlinedTextField(
                        value = currentProfile?.name ?: "Auto-Detected Format",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = profileExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = profileExpanded,
                        onDismissRequest = { profileExpanded = false }
                    ) {
                        uiState.availableProfiles.forEach { profile ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(profile.name, fontWeight = FontWeight.SemiBold)
                                        profile.institution?.let {
                                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                onClick = {
                                    onProfileSelected(profile.id)
                                    profileExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Destination Account
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.csv_destination_account),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                var accountExpanded by remember { mutableStateOf(false) }
                val selectedAcc = uiState.accounts.find { it.id == uiState.selectedAccountId }

                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = !accountExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAcc?.name ?: "Select Destination Account",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = EmeraldPrimary)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        uiState.accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(acc.name, fontWeight = FontWeight.SemiBold)
                                        Text(acc.balance.formatted(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    onSelectAccount(acc.id)
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Format Configurations Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "CSV Format Settings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Header toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.csv_has_header), fontWeight = FontWeight.Medium)
                        Text("First line contains column names", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = formatConfig.hasHeader,
                        onCheckedChange = { onFormatConfigChange(formatConfig.copy(hasHeader = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                    )
                }

                // Delimiter selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Delimiter", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(',' to "Comma (,)", ';' to "Semicolon (;)", '\t' to "Tab (\\t)", '|' to "Pipe (|)").forEach { (delim, label) ->
                            FilterChip(
                                selected = formatConfig.delimiter == delim,
                                onClick = { onFormatConfigChange(formatConfig.copy(delimiter = delim)) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }

                // Decimal separator
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Decimal Separator", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf('.' to "Dot (12.50)", ',' to "Comma (12,50)").forEach { (sep, label) ->
                            FilterChip(
                                selected = formatConfig.decimalSeparator == sep,
                                onClick = { onFormatConfigChange(formatConfig.copy(decimalSeparator = sep)) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }

                // Date format
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.csv_date_format), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = formatConfig.dateFormat,
                        onValueChange = { onFormatConfigChange(formatConfig.copy(dateFormat = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy", "dd.MM.yyyy", "yyyy/MM/dd").forEach { fmt ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.clickable { onFormatConfigChange(formatConfig.copy(dateFormat = fmt)) }
                            ) {
                                Text(
                                    text = fmt,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Amount Mode
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Amount Column Mode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = formatConfig.amountMode == AmountMode.SINGLE_AMOUNT,
                            onClick = { onFormatConfigChange(formatConfig.copy(amountMode = AmountMode.SINGLE_AMOUNT)) },
                            label = { Text(AmountMode.SINGLE_AMOUNT.getLocalizedName()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = EmeraldPrimary
                            )
                        )
                        FilterChip(
                            selected = formatConfig.amountMode == AmountMode.SEPARATE_DEBIT_CREDIT,
                            onClick = { onFormatConfigChange(formatConfig.copy(amountMode = AmountMode.SEPARATE_DEBIT_CREDIT)) },
                            label = { Text(AmountMode.SEPARATE_DEBIT_CREDIT.getLocalizedName()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = EmeraldPrimary
                            )
                        )
                    }
                }

                // Reversed debit/credit toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reverse Outflow / Inflow Signs", fontWeight = FontWeight.Medium)
                        Text(
                            text = "Enable if debits/expenses are shown as positive in statement",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = formatConfig.reversedDebitCredit,
                        onCheckedChange = { onFormatConfigChange(formatConfig.copy(reversedDebitCredit = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                    )
                }
            }
        }

        // Column Mapping Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Column Mappings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Date
                ColumnDropdownField(
                    label = "Transaction Date *",
                    selectedIndex = mapping.dateColumnIndex,
                    headers = headers,
                    onSelectIndex = { onColumnMappingChange(mapping.copy(dateColumnIndex = it)) }
                )

                // Description
                ColumnDropdownField(
                    label = "Description / Narrative *",
                    selectedIndex = mapping.descriptionColumnIndex,
                    headers = headers,
                    onSelectIndex = { onColumnMappingChange(mapping.copy(descriptionColumnIndex = it)) }
                )

                // Amount columns
                if (formatConfig.amountMode == AmountMode.SINGLE_AMOUNT) {
                    ColumnDropdownField(
                        label = "Amount *",
                        selectedIndex = mapping.amountColumnIndex,
                        headers = headers,
                        onSelectIndex = { onColumnMappingChange(mapping.copy(amountColumnIndex = it)) }
                    )
                } else {
                    ColumnDropdownField(
                        label = "Debit (Money Out)",
                        selectedIndex = mapping.debitColumnIndex,
                        headers = headers,
                        onSelectIndex = { onColumnMappingChange(mapping.copy(debitColumnIndex = it)) }
                    )
                    ColumnDropdownField(
                        label = "Credit (Money In)",
                        selectedIndex = mapping.creditColumnIndex,
                        headers = headers,
                        onSelectIndex = { onColumnMappingChange(mapping.copy(creditColumnIndex = it)) }
                    )
                }

                // Merchant
                ColumnDropdownField(
                    label = "Merchant / Payee (Optional)",
                    selectedIndex = mapping.merchantColumnIndex,
                    headers = headers,
                    onSelectIndex = { onColumnMappingChange(mapping.copy(merchantColumnIndex = it)) }
                )

                // Category
                ColumnDropdownField(
                    label = "Category (Optional)",
                    selectedIndex = mapping.categoryColumnIndex,
                    headers = headers,
                    onSelectIndex = { onColumnMappingChange(mapping.copy(categoryColumnIndex = it)) }
                )

                // Currency
                ColumnDropdownField(
                    label = "Currency (Optional)",
                    selectedIndex = mapping.currencyColumnIndex,
                    headers = headers,
                    onSelectIndex = { onColumnMappingChange(mapping.copy(currencyColumnIndex = it)) }
                )

                // Balance
                ColumnDropdownField(
                    label = "Balance (Optional)",
                    selectedIndex = mapping.balanceColumnIndex,
                    headers = headers,
                    onSelectIndex = { onColumnMappingChange(mapping.copy(balanceColumnIndex = it)) }
                )
            }
        }

        // Action Button: Proceed to Preview
        Button(
            onClick = onProceed,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Preview Statement Rows",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnDropdownField(
    label: String,
    selectedIndex: Int,
    headers: List<String>,
    onSelectIndex: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = if (selectedIndex in headers.indices) {
        "Col ${selectedIndex + 1}: ${headers[selectedIndex]}"
    } else {
        "Not Mapped"
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = displayValue,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Not Mapped", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        onSelectIndex(-1)
                        expanded = false
                    }
                )
                headers.forEachIndexed { index, header ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Col ${index + 1}: $header",
                                fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSelectIndex(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewReviewView(
    uiState: CsvImportUiState,
    onTabSelected: (PreviewTab) -> Unit,
    onToggleExclusion: (Int) -> Unit,
    onExcludeAllDuplicates: () -> Unit,
    onIncludeAll: () -> Unit,
    onExcludeAll: () -> Unit,
    onBackToMapping: () -> Unit,
    onExecuteImport: () -> Unit
) {
    val displayedRows = when (uiState.currentPreviewTab) {
        PreviewTab.ALL -> uiState.parsedRows
        PreviewTab.NEW -> uiState.parsedRows.filter { it.isValid && it.duplicateStatus == DuplicateStatus.NEW }
        PreviewTab.DUPLICATES -> uiState.parsedRows.filter { it.isValid && it.duplicateStatus != DuplicateStatus.NEW }
        PreviewTab.INVALID -> uiState.parsedRows.filter { !it.isValid }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Summary & Quick actions Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.csv_preview_valid_count, uiState.readyToImportCount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Destination: ${uiState.accounts.find { it.id == uiState.selectedAccountId }?.name ?: "Account"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onExecuteImport,
                        enabled = uiState.readyToImportCount > 0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text(
                            text = stringResource(R.string.csv_confirm_import_btn, uiState.readyToImportCount),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Quick exclusion buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.duplicatesCount > 0) {
                        OutlinedButton(
                            onClick = onExcludeAllDuplicates,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Exclude All Duplicates (${uiState.duplicatesCount})")
                        }
                    }
                    OutlinedButton(
                        onClick = onIncludeAll,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Include All Valid")
                    }
                    OutlinedButton(
                        onClick = onExcludeAll,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Exclude All")
                    }
                }
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = uiState.currentPreviewTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.currentPreviewTab.ordinal]),
                    color = EmeraldPrimary
                )
            }
        ) {
            PreviewTab.entries.forEach { tab ->
                val count = when (tab) {
                    PreviewTab.ALL -> uiState.parsedRows.size
                    PreviewTab.NEW -> uiState.newRowsCount
                    PreviewTab.DUPLICATES -> uiState.duplicatesCount
                    PreviewTab.INVALID -> uiState.invalidRowsCount
                }
                Tab(
                    selected = uiState.currentPreviewTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = "${tab.title} ($count)",
                            fontWeight = if (uiState.currentPreviewTab == tab) FontWeight.Bold else FontWeight.Normal,
                            color = if (uiState.currentPreviewTab == tab) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // Rows List
        if (displayedRows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No rows in this tab",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedRows, key = { it.rowIndex }) { row ->
                    ParsedRowCard(
                        row = row,
                        onToggleExclusion = { onToggleExclusion(row.rowIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ParsedRowCard(
    row: ParsedCsvRow,
    onToggleExclusion: () -> Unit
) {
    val isIncluded = row.isValid && !row.isExcluded
    val cardColor = when {
        !row.isValid -> CrimsonExpense.copy(alpha = 0.08f)
        row.duplicateStatus == DuplicateStatus.EXACT_DUPLICATE -> AmberWarning.copy(alpha = 0.08f)
        row.duplicateStatus == DuplicateStatus.POTENTIAL_DUPLICATE -> AmberWarning.copy(alpha = 0.05f)
        row.isExcluded -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = cardColor,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                !row.isValid -> CrimsonExpense.copy(alpha = 0.3f)
                row.duplicateStatus != DuplicateStatus.NEW -> AmberWarning.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (row.isValid) {
                    Checkbox(
                        checked = isIncluded,
                        onCheckedChange = { onToggleExclusion() },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = CrimsonExpense,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.parsedDescription.ifBlank { row.parsedMerchant ?: "Row #${row.rowIndex}" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.parsedDate?.let { epoch ->
                            val dateStr = DateFormatterUtils.formatDate(epoch)
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        row.parsedMerchant?.let { merch ->
                            Text(
                                text = "• $merch",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Amount
                if (row.parsedAmountMinor != null && row.parsedType != null) {
                    val sign = if (row.parsedType == TransactionType.INCOME) "+" else "-"
                    val color = if (row.parsedType == TransactionType.INCOME) EmeraldPrimary else CrimsonExpense
                    val formatted = "$sign$${"%.2f".format(row.parsedAmountMinor / 100.0)}"

                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }

            // Badges / Alerts
            if (!row.isValid) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CrimsonExpense.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = CrimsonExpense, modifier = Modifier.size(12.dp))
                        Text(
                            text = row.errorReason ?: "Invalid row",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonExpense,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (row.duplicateStatus != DuplicateStatus.NEW) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AmberWarning.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(12.dp))
                        val statusLabel = row.duplicateStatus.getLocalizedName()
                        val candidateDesc = row.duplicateTransactionDescription?.let { " ($it)" }.orEmpty()
                        Text(
                            text = "$statusLabel$candidateDesc",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarning,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Category Suggestion Chip
            if (row.isValid && !row.predictedCategoryId.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "Category: ${row.predictedCategoryId?.replace("cat_", "")?.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportSummaryView(
    summary: com.finpulse.app.domain.model.ImportSummary,
    onDone: () -> Unit,
    onImportAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Check
        Surface(
            shape = CircleShape,
            color = EmeraldPrimary.copy(alpha = 0.15f),
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.csv_summary_success),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.csv_summary_imported, summary.importedCount) + " • ${summary.destinationAccountName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Stats Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryStatRow("Total CSV Rows", "${summary.totalRows}")
                SummaryStatRow(
                    label = stringResource(R.string.csv_summary_imported, summary.importedCount).substringBefore(":").ifEmpty { "Successfully Imported" },
                    value = "${summary.importedCount}",
                    valueColor = EmeraldPrimary
                )
                SummaryStatRow("Total Inflow Added", "+$${"%.2f".format(summary.totalIncomeMinor / 100.0)}", EmeraldPrimary)
                SummaryStatRow("Total Outflow Added", "-$${"%.2f".format(summary.totalExpenseMinor / 100.0)}", CrimsonExpense)
                SummaryStatRow(
                    label = stringResource(R.string.csv_summary_skipped, summary.skippedDuplicateCount).substringBefore(":").ifEmpty { "Duplicates Skipped" },
                    value = "${summary.skippedDuplicateCount}",
                    valueColor = AmberWarning
                )
                SummaryStatRow("Excluded by User", "${summary.excludedCount}")
                SummaryStatRow("Invalid Rows Ignored", "${summary.invalidCount}")
            }
        }

        // Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = stringResource(R.string.nav_transactions),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            OutlinedButton(
                onClick = onImportAnother,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Import Another Statement")
            }
        }
    }
}

@Composable
private fun SummaryStatRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun SaveProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, institution: String?) -> Unit
) {
    var profileName by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Import Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Save this format and column mapping so future statements from this bank can be imported in one tap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name (e.g. Chase Checking)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text("Bank / Institution (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(profileName, institution.ifBlank { null }) },
                enabled = profileName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun queryFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        val path = uri.path
        val cut = path?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = path.substring(cut + 1)
        }
    }
    return result
}

private object DemoDataSets {
    val US_STANDARD_CSV = """
Date,Description,Amount,Category,Balance
2024-03-15,"Whole Foods Market",-68.40,"Groceries",4831.60
2024-03-15,"Shell Oil Fuel",-42.50,"Auto & Transport",4789.10
2024-03-14,"Acme Corp Payroll",3200.00,"Salary",8031.60
2024-03-12,"Netflix Subscription",-15.99,"Subscriptions",4831.60
2024-03-10,"Starbucks Coffee",-6.75,"Dining",4847.59
2024-03-08,"Apartment Rent",-1450.00,"Housing",4854.34
2024-03-05,"Target Store",-34.20,"Shopping",6304.34
    """.trimIndent()

    val EUROPEAN_CSV = """
Buchungstag;Verwendungszweck;Betrag;Währung
15.03.2024;EDEKA Supermarkt;-45,80;EUR
14.03.2024;Gehaltszahlung Arbeitgeber;2850,00;EUR
12.03.2024;Deutsche Bahn Ticket;-64,20;EUR
10.03.2024;Spotify Musikdienst;-10,99;EUR
08.03.2024;Miete Dauerauftrag;-850,00;EUR
    """.trimIndent()

    val SPLIT_DEBIT_CREDIT_CSV = """
Date,Description,Debit,Credit,Balance
2024-03-16,Trader Joe's,54.20,,3145.80
2024-03-15,Direct Deposit Salary,,2600.00,3200.00
2024-03-14,Uber Trip,22.40,,600.00
2024-03-12,Amazon Marketplace,89.90,,622.40
    """.trimIndent()
}
