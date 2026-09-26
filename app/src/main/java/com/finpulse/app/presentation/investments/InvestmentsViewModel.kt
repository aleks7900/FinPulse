package com.finpulse.app.presentation.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.AssetClass
import com.finpulse.app.domain.model.InvestmentAsset
import com.finpulse.app.domain.repository.InvestmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class InvestmentsUiState(
    val assets: List<InvestmentAsset> = emptyList(),
    val totalInvested: Money = Money.zero(),
    val totalCurrentValue: Money = Money.zero(),
    val totalProfitLoss: Money = Money.zero(),
    val totalReturnPercentage: Double = 0.0,
    val hideBalances: Boolean = false,
    val baseCurrency: String = "USD",
    val isAddEditDialogVisible: Boolean = false,
    val editingAsset: InvestmentAsset? = null
)

class InvestmentsViewModel(
    private val investmentRepository: InvestmentRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _isAddEditDialogVisible = MutableStateFlow(false)
    private val _editingAsset = MutableStateFlow<InvestmentAsset?>(null)

    val uiState: StateFlow<InvestmentsUiState> = combine(
        investmentRepository.getAllAssetsFlow(),
        userPreferencesDataStore.userPreferencesFlow,
        _isAddEditDialogVisible,
        _editingAsset
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val assets = params[0] as List<InvestmentAsset>
        val userPrefs = params[1] as com.finpulse.app.core.datastore.UserPreferences
        val isAddVisible = params[2] as Boolean
        val editingA = params[3] as InvestmentAsset?

        val currency = userPrefs.baseCurrencyCode
        val totalInvestedMinor = assets.sumOf { it.totalInvested.amountMinor }
        val totalCurrentMinor = assets.sumOf { it.currentValue.amountMinor }
        val profitLossMinor = totalCurrentMinor - totalInvestedMinor

        val returnPct = if (totalInvestedMinor > 0) {
            (profitLossMinor.toDouble() / totalInvestedMinor.toDouble()) * 100.0
        } else 0.0

        InvestmentsUiState(
            assets = assets,
            totalInvested = Money(totalInvestedMinor, currency),
            totalCurrentValue = Money(totalCurrentMinor, currency),
            totalProfitLoss = Money(profitLossMinor, currency),
            totalReturnPercentage = returnPct,
            hideBalances = userPrefs.hideBalances,
            baseCurrency = currency,
            isAddEditDialogVisible = isAddVisible,
            editingAsset = editingA
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InvestmentsUiState()
    )

    fun showAddEditDialog(show: Boolean, asset: InvestmentAsset? = null) {
        _editingAsset.value = asset
        _isAddEditDialogVisible.value = show
    }

    fun saveAsset(
        id: String?,
        name: String,
        symbol: String,
        assetClass: AssetClass,
        quantity: Double,
        purchasePriceMinor: Long,
        currentPriceMinor: Long
    ) {
        viewModelScope.launch {
            val a = InvestmentAsset(
                id = id ?: UUID.randomUUID().toString(),
                name = name,
                symbol = symbol.uppercase(),
                assetClass = assetClass,
                quantity = quantity,
                purchasePrice = Money(purchasePriceMinor, uiState.value.baseCurrency),
                currentPrice = Money(currentPriceMinor, uiState.value.baseCurrency)
            )
            investmentRepository.saveAsset(a)
            _isAddEditDialogVisible.value = false
            _editingAsset.value = null
        }
    }

    fun deleteAsset(id: String) {
        viewModelScope.launch {
            investmentRepository.deleteAsset(id)
            _isAddEditDialogVisible.value = false
            _editingAsset.value = null
        }
    }
}
