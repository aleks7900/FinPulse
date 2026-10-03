package md.alexlab.finpulse.presentation.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import md.alexlab.finpulse.core.datastore.UserPreferences
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.DateRangePreset
import md.alexlab.finpulse.domain.model.backup.BackupExportPayload
import md.alexlab.finpulse.domain.model.backup.BackupValidationResult
import md.alexlab.finpulse.domain.model.backup.ExportFilterParams
import md.alexlab.finpulse.domain.model.backup.RestoreSummary
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import md.alexlab.finpulse.domain.usecase.backup.CreateBackupUseCase
import md.alexlab.finpulse.domain.usecase.backup.ExportTransactionsUseCase
import md.alexlab.finpulse.domain.usecase.backup.RestoreBackupUseCase
import md.alexlab.finpulse.domain.usecase.backup.ValidateBackupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExportUiState(
    val lastBackupTimestamp: Long? = null,
    val backupReminderInterval: String = "OFF",
    val isReminderDue: Boolean = false,
    val filterParams: ExportFilterParams = ExportFilterParams(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val validationResult: BackupValidationResult? = null,
    val isRestoreConfirmDialogVisible: Boolean = false,
    val isPasswordPromptVisible: Boolean = false,
    val restoreSummary: RestoreSummary? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ExportViewModel(
    private val createBackupUseCase: CreateBackupUseCase,
    private val validateBackupUseCase: ValidateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val exportTransactionsUseCase: ExportTransactionsUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val errorReporter: md.alexlab.finpulse.core.reporting.ErrorReporter = md.alexlab.finpulse.core.reporting.NoOpErrorReporter()
) : ViewModel() {

    private val _filterParams = MutableStateFlow(ExportFilterParams())
    private val _isLoading = MutableStateFlow(false)
    private val _validationResult = MutableStateFlow<BackupValidationResult?>(null)
    private val _isRestoreConfirmDialogVisible = MutableStateFlow(false)
    private val _isPasswordPromptVisible = MutableStateFlow(false)
    private val _pendingContentForRestore = MutableStateFlow<String?>(null)
    private val _restoreSummary = MutableStateFlow<RestoreSummary?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ExportUiState> = combine(
        userPreferencesDataStore.userPreferencesFlow,
        accountRepository.getActiveAccountsFlow(),
        categoryRepository.getAllCategoriesFlow(),
        _filterParams,
        combine(
            _isLoading,
            _validationResult,
            _isRestoreConfirmDialogVisible,
            _isPasswordPromptVisible,
            _restoreSummary
        ) { loading, valRes, confirmVis, passVis, summary ->
            DialogState(loading, valRes, confirmVis, passVis, summary)
        },
        combine(_errorMessage, _successMessage) { err, succ -> Pair(err, succ) }
    ) { args: Array<Any?> ->
        val prefs = args[0] as UserPreferences
        @Suppress("UNCHECKED_CAST")
        val accounts = args[1] as List<Account>
        @Suppress("UNCHECKED_CAST")
        val categories = args[2] as List<Category>
        val filter = args[3] as ExportFilterParams
        val dialogs = args[4] as DialogState
        @Suppress("UNCHECKED_CAST")
        val messages = args[5] as Pair<String?, String?>

        val now = System.currentTimeMillis()
        val lastBackup = prefs.lastBackupTimestamp
        val interval = prefs.backupReminderInterval
        val lastDismissed = prefs.lastBackupReminderDismissedMillis

        val isReminderDue = when (interval) {
            "WEEKLY" -> {
                val lastActivity = maxOf(lastBackup ?: 0L, lastDismissed)
                (now - lastActivity) > 7 * 86_400_000L
            }
            "MONTHLY" -> {
                val lastActivity = maxOf(lastBackup ?: 0L, lastDismissed)
                (now - lastActivity) > 30 * 86_400_000L
            }
            else -> false
        }

        ExportUiState(
            lastBackupTimestamp = lastBackup,
            backupReminderInterval = interval,
            isReminderDue = isReminderDue,
            filterParams = filter,
            accounts = accounts,
            categories = categories,
            isLoading = dialogs.isLoading,
            validationResult = dialogs.validationResult,
            isRestoreConfirmDialogVisible = dialogs.isRestoreConfirmDialogVisible,
            isPasswordPromptVisible = dialogs.isPasswordPromptVisible,
            restoreSummary = dialogs.restoreSummary,
            errorMessage = messages.first,
            successMessage = messages.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExportUiState()
    )

    private data class DialogState(
        val isLoading: Boolean,
        val validationResult: BackupValidationResult?,
        val isRestoreConfirmDialogVisible: Boolean,
        val isPasswordPromptVisible: Boolean,
        val restoreSummary: RestoreSummary?
    )

    fun createBackup(password: String? = null, onReady: (BackupExportPayload) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val payload = createBackupUseCase(password)
                _successMessage.value = "Backup created successfully"
                errorReporter.log("Backup archive created successfully")
                onReady(payload)
            } catch (e: Exception) {
                errorReporter.recordException(
                    throwable = e,
                    context = md.alexlab.finpulse.core.reporting.DiagnosticContext.build(
                        feature = md.alexlab.finpulse.core.reporting.DiagnosticContext.FEATURE_BACKUP_EXPORT,
                        operation = md.alexlab.finpulse.core.reporting.DiagnosticContext.OP_CREATE_BACKUP
                    )
                )
                _errorMessage.value = "Failed to create backup: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportCsv(onReady: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val csv = exportTransactionsUseCase.exportCsv(_filterParams.value)
                _successMessage.value = "CSV exported successfully"
                errorReporter.log("CSV transactions exported successfully")
                onReady(csv)
            } catch (e: Exception) {
                errorReporter.recordException(
                    throwable = e,
                    context = md.alexlab.finpulse.core.reporting.DiagnosticContext.build(
                        feature = md.alexlab.finpulse.core.reporting.DiagnosticContext.FEATURE_BACKUP_EXPORT,
                        operation = md.alexlab.finpulse.core.reporting.DiagnosticContext.OP_EXPORT_CSV
                    )
                )
                _errorMessage.value = "Failed to export CSV: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportFilteredJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val jsonStr = exportTransactionsUseCase.exportJson(_filterParams.value)
                _successMessage.value = "JSON exported successfully"
                errorReporter.log("JSON transactions exported successfully")
                onReady(jsonStr)
            } catch (e: Exception) {
                errorReporter.recordException(
                    throwable = e,
                    context = md.alexlab.finpulse.core.reporting.DiagnosticContext.build(
                        feature = md.alexlab.finpulse.core.reporting.DiagnosticContext.FEATURE_BACKUP_EXPORT,
                        operation = md.alexlab.finpulse.core.reporting.DiagnosticContext.OP_EXPORT_JSON
                    )
                )
                _errorMessage.value = "Failed to export JSON: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onFileContentSelectedForRestore(content: String) {
        _pendingContentForRestore.value = content
        val validation = validateBackupUseCase(content)
        if (!validation.isValid && validation.errors.contains("PASSWORD_REQUIRED")) {
            _isPasswordPromptVisible.value = true
        } else {
            _validationResult.value = validation
            _isRestoreConfirmDialogVisible.value = true
        }
    }

    fun submitPasswordForRestore(password: String) {
        val content = _pendingContentForRestore.value ?: return
        val validation = validateBackupUseCase(content, password)
        _isPasswordPromptVisible.value = false
        _validationResult.value = validation
        _isRestoreConfirmDialogVisible.value = true
    }

    fun confirmRestore() {
        val backupData = _validationResult.value?.backupData ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val summary = restoreBackupUseCase(backupData)
                _restoreSummary.value = summary
                _successMessage.value = "Restore completed successfully (${summary.totalRestoredRecords} records restored)"
                errorReporter.log("Backup restored successfully (${summary.totalRestoredRecords} records)")
                _isRestoreConfirmDialogVisible.value = false
                _validationResult.value = null
                _pendingContentForRestore.value = null
            } catch (e: Exception) {
                errorReporter.recordException(
                    throwable = e,
                    context = md.alexlab.finpulse.core.reporting.DiagnosticContext.build(
                        feature = md.alexlab.finpulse.core.reporting.DiagnosticContext.FEATURE_BACKUP_EXPORT,
                        operation = md.alexlab.finpulse.core.reporting.DiagnosticContext.OP_RESTORE_BACKUP
                    )
                )
                _errorMessage.value = "Restore failed: ${e.message}. Database was not modified."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissRestoreConfirmDialog() {
        _isRestoreConfirmDialogVisible.value = false
        _validationResult.value = null
        _pendingContentForRestore.value = null
    }

    fun dismissPasswordPrompt() {
        _isPasswordPromptVisible.value = false
        _pendingContentForRestore.value = null
    }

    fun dismissRestoreSummary() {
        _restoreSummary.value = null
    }

    fun setFilterDatePreset(preset: DateRangePreset, customStart: Long? = null, customEnd: Long? = null) {
        _filterParams.value = _filterParams.value.copy(
            dateRangePreset = preset,
            customStartDate = customStart,
            customEndDate = customEnd
        )
    }

    fun setSelectedAccount(accountId: String?) {
        _filterParams.value = _filterParams.value.copy(selectedAccountId = accountId)
    }

    fun setReminderInterval(interval: String) {
        viewModelScope.launch {
            userPreferencesDataStore.setBackupReminderInterval(interval)
        }
    }

    fun dismissReminder() {
        viewModelScope.launch {
            userPreferencesDataStore.setLastBackupReminderDismissedMillis(System.currentTimeMillis())
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
