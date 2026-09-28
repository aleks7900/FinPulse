package com.finpulse.app.presentation.currencies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.model.CurrencyConfig
import com.finpulse.app.domain.model.ExchangeRate
import com.finpulse.app.domain.repository.ExchangeRateProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CurrenciesUiState(
    val baseCurrency: String = "USD",
    val ratesAgainstBase: List<ExchangeRate> = emptyList(),
    val lastUpdatedTimestamp: Long? = null,
    val isRefreshing: Boolean = false,
    val editingRate: ExchangeRate? = null,
    val supportedCurrencies: List<String> = CurrencyConfig.supportedCurrencyCodes
)

class CurrenciesViewModel(
    private val exchangeRateProvider: ExchangeRateProvider,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _editingRate = MutableStateFlow<ExchangeRate?>(null)

    val uiState: StateFlow<CurrenciesUiState> = combine(
        userPreferencesDataStore.userPreferencesFlow,
        exchangeRateProvider.getAllRatesFlow(),
        exchangeRateProvider.getLastUpdatedTimestampFlow(),
        _isRefreshing,
        _editingRate
    ) { userPrefs, allRates, lastUpdated, isRefreshing, editingRate ->
        val base = userPrefs.baseCurrencyCode.uppercase()

        // Generate or collect rates from base currency to every other supported currency
        val ratesForBase = CurrencyConfig.supportedCurrencyCodes
            .filter { it != base }
            .map { target ->
                // Check if direct rate exists in allRates
                val direct = allRates.find {
                    it.fromCurrency.equals(base, ignoreCase = true) &&
                    it.toCurrency.equals(target, ignoreCase = true)
                }
                if (direct != null) {
                    direct
                } else {
                    // Check if inverse rate exists
                    val inverse = allRates.find {
                        it.fromCurrency.equals(target, ignoreCase = true) &&
                        it.toCurrency.equals(base, ignoreCase = true)
                    }
                    if (inverse != null && inverse.rate > 0.0) {
                        ExchangeRate(
                            fromCurrency = base,
                            toCurrency = target,
                            rate = 1.0 / inverse.rate,
                            isManual = inverse.isManual,
                            timestamp = inverse.timestamp
                        )
                    } else {
                        // Offline reference rate
                        exchangeRateProvider.getRate(base, target)
                    }
                }
            }

        CurrenciesUiState(
            baseCurrency = base,
            ratesAgainstBase = ratesForBase,
            lastUpdatedTimestamp = lastUpdated,
            isRefreshing = isRefreshing,
            editingRate = editingRate,
            supportedCurrencies = CurrencyConfig.supportedCurrencyCodes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CurrenciesUiState()
    )

    fun setBaseCurrency(currencyCode: String) {
        viewModelScope.launch {
            userPreferencesDataStore.setBaseCurrency(currencyCode.uppercase())
        }
    }

    fun refreshRates() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                exchangeRateProvider.refreshRates()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun openEditRate(rate: ExchangeRate) {
        _editingRate.value = rate
    }

    fun dismissEditRate() {
        _editingRate.value = null
    }

    fun saveManualRate(fromCurrency: String, toCurrency: String, newRate: Double) {
        viewModelScope.launch {
            exchangeRateProvider.setManualRate(fromCurrency, toCurrency, newRate)
            _editingRate.value = null
        }
    }

    fun resetRateToDefault(fromCurrency: String, toCurrency: String) {
        viewModelScope.launch {
            exchangeRateProvider.resetToDefault(fromCurrency, toCurrency)
            _editingRate.value = null
        }
    }
}
