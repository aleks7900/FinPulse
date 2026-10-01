package md.alexlab.finpulse.presentation.digest

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class DigestUiState(
    val digest: FinancialDigest? = null,
    val selectedPeriod: DigestPeriod = DigestPeriod.DAILY,
    val frequency: DigestFrequency = DigestFrequency.DAILY,
    val privacyEnabled: Boolean = false,
    val deliveryHour: Int = 20,
    val isSettingsOpen: Boolean = false,
    val testNotificationMessage: String? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class DigestViewModel(
    private val getFinancialDigestUseCase: GetFinancialDigestUseCase,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(DigestPeriod.DAILY)
    private val _isSettingsOpen = MutableStateFlow(false)
    private val _testNotificationMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DigestUiState> = combine(
        _selectedPeriod.flatMapLatest { period ->
            getFinancialDigestUseCase(period)
        },
        _selectedPeriod,
        userPreferencesDataStore.userPreferencesFlow,
        _isSettingsOpen,
        _testNotificationMessage
    ) { digest, period, prefs, isSettingsOpen, testMsg ->
        DigestUiState(
            digest = digest,
            selectedPeriod = period,
            frequency = DigestFrequency.fromString(prefs.digestFrequency),
            privacyEnabled = prefs.digestPrivacyEnabled,
            deliveryHour = prefs.digestDeliveryHour,
            isSettingsOpen = isSettingsOpen,
            testNotificationMessage = testMsg,
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
        val currentDigest = uiState.value.digest ?: return
        val privacy = uiState.value.privacyEnabled
        val success = DigestNotificationHelper.sendDigestNotification(context, currentDigest, privacy)
        _testNotificationMessage.value = if (success) "Notification dispatched!" else "Failed to post notification (check permissions)"
    }

    fun clearTestNotificationMessage() {
        _testNotificationMessage.value = null
    }
}
