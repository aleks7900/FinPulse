package com.finpulse.app.presentation.csvimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.domain.engine.CsvParserEngine
import com.finpulse.app.domain.model.AmountMode
import com.finpulse.app.domain.model.CsvColumnMapping
import com.finpulse.app.domain.model.CsvFormatConfig
import com.finpulse.app.domain.model.DuplicateStatus
import com.finpulse.app.domain.model.ImportProfile
import com.finpulse.app.domain.model.ImportSummary
import com.finpulse.app.domain.model.ParsedCsvRow
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.usecase.csv.AutoDetectCsvConfigUseCase
import com.finpulse.app.domain.usecase.csv.ExecuteCsvImportUseCase
import com.finpulse.app.domain.usecase.csv.ManageImportProfilesUseCase
import com.finpulse.app.domain.usecase.csv.ParseCsvStatementUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class ImportStep {
    SELECT_FILE,
    MAPPING_CONFIG,
    PREVIEW_REVIEW,
    IMPORT_SUMMARY
}

enum class PreviewTab(val title: String) {
    ALL("All Rows"),
    NEW("New"),
    DUPLICATES("Duplicates"),
    INVALID("Invalid")
}

data class CsvImportUiState(
    val currentStep: ImportStep = ImportStep.SELECT_FILE,
    val fileName: String? = null,
    val rawCsvContent: String = "",
    val detectedHeaders: List<String> = emptyList(),
    val sampleRows: List<List<String>> = emptyList(),
    val formatConfig: CsvFormatConfig = CsvFormatConfig(),
    val columnMapping: CsvColumnMapping = CsvColumnMapping(),
    val selectedProfileId: String? = null,
    val availableProfiles: List<ImportProfile> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: String? = null,
    val parsedRows: List<ParsedCsvRow> = emptyList(),
    val currentPreviewTab: PreviewTab = PreviewTab.ALL,
    val isSaveProfileDialogOpen: Boolean = false,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val errorMessage: String? = null,
    val importSummary: ImportSummary? = null
) {
    val totalValidRows: Int get() = parsedRows.count { it.isValid }
    val newRowsCount: Int get() = parsedRows.count { it.isValid && it.duplicateStatus == DuplicateStatus.NEW }
    val duplicatesCount: Int get() = parsedRows.count { it.isValid && it.duplicateStatus != DuplicateStatus.NEW }
    val invalidRowsCount: Int get() = parsedRows.count { !it.isValid }
    val readyToImportCount: Int get() = parsedRows.count { it.isValid && !it.isExcluded }
}

class CsvImportViewModel(
    private val accountRepository: AccountRepository,
    private val autoDetectCsvConfigUseCase: AutoDetectCsvConfigUseCase,
    private val parseCsvStatementUseCase: ParseCsvStatementUseCase,
    private val executeCsvImportUseCase: ExecuteCsvImportUseCase,
    private val manageImportProfilesUseCase: ManageImportProfilesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CsvImportUiState())
    val uiState: StateFlow<CsvImportUiState> = _uiState.asStateFlow()

    init {
        // Load active accounts
        viewModelScope.launch {
            accountRepository.getActiveAccountsFlow().collect { accounts ->
                _uiState.update { state ->
                    val defaultAcc = state.selectedAccountId ?: accounts.firstOrNull()?.id
                    state.copy(
                        accounts = accounts,
                        selectedAccountId = defaultAcc
                    )
                }
            }
        }

        // Load available import profiles
        viewModelScope.launch {
            manageImportProfilesUseCase.getAllProfilesFlow().collect { profiles ->
                _uiState.update { it.copy(availableProfiles = profiles) }
            }
        }
    }

    fun onFileLoaded(fileName: String, content: String) {
        if (content.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Selected file is empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Analyzing CSV statement format...",
                    errorMessage = null
                )
            }

            try {
                val (detectedConfig, detectedMapping) = autoDetectCsvConfigUseCase(content)
                val rawRecords = CsvParserEngine.parseRawRecords(
                    csvText = content,
                    delimiter = detectedConfig.delimiter,
                    hasHeader = detectedConfig.hasHeader
                )

                val headers = if (detectedConfig.hasHeader && rawRecords.isNotEmpty()) {
                    rawRecords.first()
                } else if (rawRecords.isNotEmpty()) {
                    List(rawRecords.first().size) { "Col ${it + 1}" }
                } else {
                    emptyList()
                }

                val sample = if (detectedConfig.hasHeader) rawRecords.drop(1).take(5) else rawRecords.take(5)

                _uiState.update { state ->
                    state.copy(
                        currentStep = ImportStep.MAPPING_CONFIG,
                        fileName = fileName,
                        rawCsvContent = content,
                        formatConfig = detectedConfig,
                        columnMapping = detectedMapping,
                        detectedHeaders = headers,
                        sampleRows = sample,
                        isLoading = false,
                        loadingMessage = "",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to parse CSV: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun onSelectProfile(profileId: String) {
        val profile = _uiState.value.availableProfiles.find { it.id == profileId } ?: return
        _uiState.update { state ->
            state.copy(
                selectedProfileId = profile.id,
                formatConfig = profile.formatConfig,
                columnMapping = profile.columnMapping,
                selectedAccountId = profile.defaultAccountId ?: state.selectedAccountId
            )
        }
    }

    fun onFormatConfigChange(newConfig: CsvFormatConfig) {
        _uiState.update { state ->
            // Recompute sample records if delimiter or hasHeader changed
            val rawRecords = CsvParserEngine.parseRawRecords(
                csvText = state.rawCsvContent,
                delimiter = newConfig.delimiter,
                hasHeader = newConfig.hasHeader
            )
            val headers = if (newConfig.hasHeader && rawRecords.isNotEmpty()) {
                rawRecords.first()
            } else if (rawRecords.isNotEmpty()) {
                List(rawRecords.first().size) { "Col ${it + 1}" }
            } else {
                emptyList()
            }
            val sample = if (newConfig.hasHeader) rawRecords.drop(1).take(5) else rawRecords.take(5)

            state.copy(
                formatConfig = newConfig,
                detectedHeaders = headers,
                sampleRows = sample
            )
        }
    }

    fun onColumnMappingChange(newMapping: CsvColumnMapping) {
        _uiState.update { it.copy(columnMapping = newMapping) }
    }

    fun onSelectAccount(accountId: String) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun proceedToPreview() {
        val state = _uiState.value
        val accountId = state.selectedAccountId
        if (accountId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Please select a destination account") }
            return
        }

        if (!state.columnMapping.isValid(state.formatConfig.amountMode)) {
            _uiState.update { it.copy(errorMessage = "Please map required Date, Amount, and Description columns") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Parsing statement rows & checking duplicates...",
                    errorMessage = null
                )
            }

            try {
                val parsed = parseCsvStatementUseCase(
                    csvText = state.rawCsvContent,
                    config = state.formatConfig,
                    mapping = state.columnMapping,
                    destinationAccountId = accountId
                )

                _uiState.update {
                    it.copy(
                        currentStep = ImportStep.PREVIEW_REVIEW,
                        parsedRows = parsed,
                        isLoading = false,
                        loadingMessage = "",
                        currentPreviewTab = PreviewTab.ALL
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error parsing statement: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun onToggleRowExclusion(rowIndex: Int) {
        _uiState.update { state ->
            val updated = state.parsedRows.map { row ->
                if (row.rowIndex == rowIndex) {
                    row.copy(isExcluded = !row.isExcluded)
                } else {
                    row
                }
            }
            state.copy(parsedRows = updated)
        }
    }

    fun onExcludeAllDuplicates() {
        _uiState.update { state ->
            val updated = state.parsedRows.map { row ->
                if (row.duplicateStatus != DuplicateStatus.NEW) {
                    row.copy(isExcluded = true)
                } else {
                    row
                }
            }
            state.copy(parsedRows = updated)
        }
    }

    fun onIncludeAll() {
        _uiState.update { state ->
            val updated = state.parsedRows.map { row ->
                if (row.isValid) row.copy(isExcluded = false) else row
            }
            state.copy(parsedRows = updated)
        }
    }

    fun onExcludeAll() {
        _uiState.update { state ->
            val updated = state.parsedRows.map { it.copy(isExcluded = true) }
            state.copy(parsedRows = updated)
        }
    }

    fun onSetPreviewTab(tab: PreviewTab) {
        _uiState.update { it.copy(currentPreviewTab = tab) }
    }

    fun onShowSaveProfileDialog(show: Boolean) {
        _uiState.update { it.copy(isSaveProfileDialogOpen = show) }
    }

    fun onSaveNewProfile(name: String, institution: String?) {
        val state = _uiState.value
        val profile = ImportProfile(
            id = "profile_custom_${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            institution = institution?.trim()?.takeIf { it.isNotEmpty() },
            formatConfig = state.formatConfig,
            columnMapping = state.columnMapping,
            defaultAccountId = state.selectedAccountId,
            isSystemPreset = false
        )

        viewModelScope.launch {
            manageImportProfilesUseCase.saveProfile(profile)
            _uiState.update {
                it.copy(
                    isSaveProfileDialogOpen = false,
                    selectedProfileId = profile.id
                )
            }
        }
    }

    fun executeImport() {
        val state = _uiState.value
        val accountId = state.selectedAccountId
        if (accountId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Missing destination account") }
            return
        }

        val rowsToImport = state.parsedRows.filter { it.isValid && !it.isExcluded }
        if (rowsToImport.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "No valid, un-excluded rows selected for import") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Importing ${rowsToImport.size} transactions...",
                    errorMessage = null
                )
            }

            try {
                val summary = executeCsvImportUseCase(
                    destinationAccountId = accountId,
                    allRows = state.parsedRows
                )

                _uiState.update {
                    it.copy(
                        currentStep = ImportStep.IMPORT_SUMMARY,
                        importSummary = summary,
                        isLoading = false,
                        loadingMessage = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Import failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun onBackToMapping() {
        _uiState.update { it.copy(currentStep = ImportStep.MAPPING_CONFIG) }
    }

    fun onBackToFileSelect() {
        _uiState.update { it.copy(currentStep = ImportStep.SELECT_FILE) }
    }

    fun onClearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun reset() {
        _uiState.value = CsvImportUiState(
            accounts = _uiState.value.accounts,
            availableProfiles = _uiState.value.availableProfiles,
            selectedAccountId = _uiState.value.accounts.firstOrNull()?.id
        )
    }
}
