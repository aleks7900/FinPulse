package md.alexlab.finpulse.presentation.investments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.CrimsonExpense
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.ui.getLocalizedName
import md.alexlab.finpulse.domain.model.AssetClass
import md.alexlab.finpulse.domain.model.InvestmentAsset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    uiState: InvestmentsUiState,
    onNavigateBack: () -> Unit,
    onShowAddEditDialog: (Boolean, InvestmentAsset?) -> Unit,
    onSaveAsset: (id: String?, name: String, symbol: String, assetClass: AssetClass, quantity: Double, purchaseMinor: Long, currentMinor: Long) -> Unit,
    onDeleteAsset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.investments_title),
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onShowAddEditDialog(true, null) },
                containerColor = EmeraldPrimary,
                contentColor = Color.Black
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.investments_add_title))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Portfolio Summary Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.investments_total_portfolio).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.hideBalances) "••••••••" else uiState.totalCurrentValue.formatted(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(stringResource(R.string.investments_total_invested), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (uiState.hideBalances) "••••" else uiState.totalInvested.formatted(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.investments_total_gain_loss), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val isPos = uiState.totalProfitLoss.isPositive
                                val sign = if (isPos) "+" else ""
                                Text(
                                    text = if (uiState.hideBalances) "••••" else "$sign${uiState.totalProfitLoss.formatted()} (${"%.1f".format(uiState.totalReturnPercentage)}%)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPos) EmeraldPrimary else CrimsonExpense
                                )
                            }
                        }
                    }
                }
            }

            // Asset list
            if (uiState.assets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.asset_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.asset_empty_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.assets, key = { it.id }) { asset ->
                    AssetCard(
                        asset = asset,
                        hideBalances = uiState.hideBalances,
                        onClick = { onShowAddEditDialog(true, asset) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Add / Edit Dialog
        if (uiState.isAddEditDialogVisible) {
            AddEditAssetDialog(
                editingAsset = uiState.editingAsset,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { onShowAddEditDialog(false, null) },
                onSave = onSaveAsset,
                onDelete = { uiState.editingAsset?.let { onDeleteAsset(it.id) } }
            )
        }
    }
}

@Composable
fun AssetCard(
    asset: InvestmentAsset,
    hideBalances: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPos = asset.profitLoss.isPositive
    val sign = if (isPos) "+" else ""

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${asset.name} • ${stringResource(R.string.investments_units, asset.quantity.toString())} • ${asset.assetClass.getLocalizedName()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (hideBalances) "••••••" else asset.currentValue.formatted(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (hideBalances) "••••" else "$sign${asset.profitLoss.formatted()} (${"%.1f".format(asset.percentageReturn)}%)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPos) EmeraldPrimary else CrimsonExpense
                )
            }
        }
    }
}

@Composable
fun AddEditAssetDialog(
    editingAsset: InvestmentAsset?,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, symbol: String, assetClass: AssetClass, quantity: Double, purchaseMinor: Long, currentMinor: Long) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(editingAsset?.name ?: "") }
    var symbol by remember { mutableStateOf(editingAsset?.symbol ?: "") }
    var assetClass by remember { mutableStateOf(editingAsset?.assetClass ?: AssetClass.STOCK) }
    var quantityText by remember { mutableStateOf(editingAsset?.quantity?.toString() ?: "1.0") }
    var purchasePriceText by remember { mutableStateOf(editingAsset?.purchasePrice?.amountBigDecimal?.toPlainString() ?: "") }
    var currentPriceText by remember { mutableStateOf(editingAsset?.currentPrice?.amountBigDecimal?.toPlainString() ?: "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (editingAsset != null) R.string.investments_edit_title else R.string.investments_add_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it; isError = false },
                    label = { Text(stringResource(R.string.asset_symbol_hint)) },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.asset_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text(stringResource(R.string.asset_quantity_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = purchasePriceText,
                    onValueChange = { purchasePriceText = it },
                    label = { Text("${stringResource(R.string.asset_purchase_price_hint)} ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentPriceText,
                    onValueChange = { currentPriceText = it },
                    label = { Text("${stringResource(R.string.asset_current_price_hint)} ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = quantityText.toDoubleOrNull()
                    val pBuy = purchasePriceText.toDoubleOrNull()
                    val pCur = currentPriceText.toDoubleOrNull()
                    if (symbol.isBlank() || q == null || q <= 0.0 || pBuy == null || pCur == null) {
                        isError = true
                        return@Button
                    }
                    onSave(
                        editingAsset?.id,
                        name.ifBlank { symbol },
                        symbol,
                        assetClass,
                        q,
                        (pBuy * 100).toLong(),
                        (pCur * 100).toLong()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.action_save), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (editingAsset != null) {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = CrimsonExpense)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    )
}
