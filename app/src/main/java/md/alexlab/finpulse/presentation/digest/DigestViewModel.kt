package md.alexlab.finpulse.presentation.digest

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.util.DigestNotificationHelper
import md.alexlab.finpulse.domain.model.DigestFrequency
import md.alexlab.finpulse.domain.model.DigestPeriod
import md.alexlab.finpulse.domain.model.FinancialDigest
import md.alexlab.finpulse.domain.usecase.digest.GetFinancialDigestUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DigestFeedback(
    val message: String? = null,
    @param:StringRes val messageRes: Int? = null
)

data class DigestUiState(
    val digest: FinancialDigest? = null,
    val selectedPeriod: DigestPeriod = DigestPeriod.DAILY,
    val frequency: DigestFrequency = DigestFrequency.DAILY,
    val privacyEnabled: Boolean = false,
    val deliveryHour: Int = 20,
    val isSettingsOpen: Boolean = false,
    val testNotificationMessage: String? = null,
    @param:StringRes val testNotificationMessageRes: Int? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class DigestViewModel(
    private val getFinancialDigestUseCase: GetFinancialDigestUseCase,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(DigestPeriod.DAILY)
    private val _isSettingsOpen = MutableStateFlow(false)
    private val _feedback = MutableStateFlow<DigestFeedback?>(null)

    val uiState: StateFlow<DigestUiState> = combine(
        _selectedPeriod.flatMapLatest { period ->
            getFinancialDigestUseCase(period)
        },
        _selectedPeriod,
        userPreferencesDataStore.userPreferencesFlow,
        _isSettingsOpen,
        _feedback
    ) { digest, period, prefs, isSettingsOpen, feedback ->
        DigestUiState(
            digest = digest,
            selectedPeriod = period,
            frequency = DigestFrequency.fromString(prefs.digestFrequency),
            privacyEnabled = prefs.digestPrivacyEnabled,
            deliveryHour = prefs.digestDeliveryHour,
            isSettingsOpen = isSettingsOpen,
            testNotificationMessage = feedback?.message,
            testNotificationMessageRes = feedback?.messageRes,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DigestUiState()
    )

    fun selectPeriod(period: DigestPeriod) {
        _selectedPeriod.value = period
    }

    fun toggleSettings(open: Boolean) {
        _isSettingsOpen.value = open
    }

    fun setFrequency(frequency: DigestFrequency) {
        viewModelScope.launch {
            userPreferencesDataStore.setDigestFrequency(frequency.name)
        }
    }

    fun setPrivacyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.setDigestPrivacyEnabled(enabled)
        }
    }

    fun setDeliveryHour(hour: Int) {
        viewModelScope.launch {
            userPreferencesDataStore.setDigestDeliveryHour(hour)
        }
    }

    fun sendTestNotification(context: Context) {
        viewModelScope.launch {
            try {
                val currentDigest = uiState.value.digest
                    ?: getFinancialDigestUseCase.getSnapshot(uiState.value.selectedPeriod)
                val privacy = uiState.value.privacyEnabled
                val success = DigestNotificationHelper.sendDigestNotification(context, currentDigest, privacy)
                _feedback.value = if (success) {
                    DigestFeedback(messageRes = R.string.digest_notif_sent_success)
                } else {
                    DigestFeedback(messageRes = R.string.digest_notif_send_failed)
                }
            } catch (_: Exception) {
                _feedback.value = DigestFeedback(messageRes = R.string.digest_notif_error_generating)
            }
        }
    }

    fun notifyPermissionDenied() {
        _feedback.value = DigestFeedback(messageRes = R.string.digest_notif_permission_denied)
    }

    fun clearTestNotificationMessage() {
        _feedback.value = null
    }
}
