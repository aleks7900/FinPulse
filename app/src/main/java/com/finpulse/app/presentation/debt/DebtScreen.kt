package com.finpulse.app.presentation.debt

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.DebtOrange
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.ui.BudgetProgressBar
import com.finpulse.app.domain.engine.DebtStrategy
import com.finpulse.app.domain.model.Debt
import com.finpulse.app.domain.model.DebtType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtScreen(
    uiState: DebtUiState,
    onNavigateBack: () -> Unit,
    onStrategyChange: (DebtStrategy) -> Unit,
    onShowAddEditDialog: (Boolean, Debt?) -> Unit,
    onShowPaymentDialog: (Boolean, Debt?) -> Unit,
    onSaveDebt: (id: String?, name: String, type: DebtType, totalMinor: Long, remainingMinor: Long, rate: Double, minPaymentMinor: Long) -> Unit,
    onMakePayment: (debtId: String, amountMinor: Long) -> Unit,
    onDeleteDebt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Debt Payoff Planner",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onShowAddEditDialog(true, null) },
                containerColor = EmeraldPrimary,
                contentColor = Color.Black
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Debt")
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
            // Strategy Selector Switcher
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StrategyChip(
                        title = "Snowball Strategy",
                        subtitle = "Lowest Balance First",
                        selected = uiState.selectedStrategy == DebtStrategy.SNOWBALL,
                        onClick = { onStrategyChange(DebtStrategy.SNOWBALL) },
                        modifier = Modifier.weight(1f)
                    )
                    StrategyChip(
                        title = "Avalanche Strategy",
                        subtitle = "Highest Interest First",
                        selected = uiState.selectedStrategy == DebtStrategy.AVALANCHE,
                        onClick = { onStrategyChange(DebtStrategy.AVALANCHE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Payoff Plan Summary Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "TOTAL OUTSTANDING LIABILITIES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.hideBalances) "••••••••" else uiState.payoffPlan.totalDebt.formatted(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = DebtOrange
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Min. Monthly Payment", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (uiState.hideBalances) "••••" else uiState.payoffPlan.totalMonthlyMinimum.formatted(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Estimated Freedom", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val months = uiState.payoffPlan.estimatedMonthsToFree
                                Text(
                                    text = if (months > 0) "$months months" else "Debt-Free!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Debts List
            if (uiState.payoffPlan.orderedDebts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Zero Debt Tracked",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Track credit cards, personal loans, mortgages, and auto financing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.payoffPlan.orderedDebts, key = { it.id }) { debt ->
                    DebtCard(
                        debt = debt,
                        hideBalances = uiState.hideBalances,
                        onClick = { onShowAddEditDialog(true, debt) },
                        onPay = { onShowPaymentDialog(true, debt) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Add / Edit Dialog
        if (uiState.isAddEditDialogVisible) {
            AddEditDebtDialog(
                editingDebt = uiState.selectedDebt,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { onShowAddEditDialog(false, null) },
                onSave = onSaveDebt,
                onDelete = { uiState.selectedDebt?.let { onDeleteDebt(it.id) } }
            )
        }

        // Make Payment Dialog
        if (uiState.isPaymentDialogVisible && uiState.selectedDebt != null) {
            MakePaymentDialog(
                debt = uiState.selectedDebt,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { onShowPaymentDialog(false, null) },
                onPay = { amountMinor -> onMakePayment(uiState.selectedDebt.id, amountMinor) }
            )
        }
    }
}

@Composable
fun StrategyChip(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = if (selected) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DebtCard(
    debt: Debt,
    hideBalances: Boolean,
    onClick: () -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (debt.remainingBalance.isZero) Icons.Default.CheckCircle else Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = if (debt.remainingBalance.isZero) EmeraldPrimary else DebtOrange,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debt.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${debt.type.displayName} • ${debt.interestRatePercent}% APR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (hideBalances) "••••••" else debt.remainingBalance.formatted(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (debt.remainingBalance.isZero) EmeraldPrimary else DebtOrange
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Min: ${debt.minimumPayment.formatted()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            BudgetProgressBar(percentage = debt.payoffProgressPercentage)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(debt.payoffProgressPercentage * 100).toInt()}% paid off",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!debt.remainingBalance.isZero) {
                    OutlinedButton(
                        onClick = onPay,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Record Payment", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditDebtDialog(
    editingDebt: Debt?,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, type: DebtType, totalMinor: Long, remainingMinor: Long, rate: Double, minPaymentMinor: Long) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(editingDebt?.name ?: "") }
    var selectedType by remember { mutableStateOf(editingDebt?.type ?: DebtType.CREDIT_CARD) }
    var totalText by remember { mutableStateOf(editingDebt?.totalPrincipal?.amountBigDecimal?.toPlainString() ?: "") }
    var remainingText by remember { mutableStateOf(editingDebt?.remainingBalance?.amountBigDecimal?.toPlainString() ?: "") }
    var rateText by remember { mutableStateOf(editingDebt?.interestRatePercent?.toString() ?: "18.0") }
    var minPaymentText by remember { mutableStateOf(editingDebt?.minimumPayment?.amountBigDecimal?.toPlainString() ?: "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingDebt != null) "Edit Debt" else "Add Debt / Loan", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; isError = false },
                    label = { Text("Debt Name (e.g. Visa Card)") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalText,
                    onValueChange = { totalText = it },
                    label = { Text("Original Principal ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remainingText,
                    onValueChange = { remainingText = it },
                    label = { Text("Current Balance Remaining ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("APR %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minPaymentText,
                        onValueChange = { minPaymentText = it },
                        label = { Text("Min. Payment") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val totVal = totalText.toDoubleOrNull()
                    val remVal = remainingText.toDoubleOrNull()
                    val r = rateText.toDoubleOrNull() ?: 0.0
                    val minVal = minPaymentText.toDoubleOrNull() ?: 0.0
                    if (name.isBlank() || totVal == null || remVal == null) {
                        isError = true
                        return@Button
                    }
                    onSave(
                        editingDebt?.id,
                        name,
                        selectedType,
                        (totVal * 100).toLong(),
                        (remVal * 100).toLong(),
                        r,
                        (minVal * 100).toLong()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (editingDebt != null) {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonExpense)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun MakePaymentDialog(
    debt: Debt,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onPay: (amountMinor: Long) -> Unit
) {
    var amountText by remember { mutableStateOf(debt.minimumPayment.amountBigDecimal.toPlainString()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment for ${debt.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Remaining balance: ${debt.remainingBalance.formatted()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; isError = false },
                    label = { Text("Payment Amount ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountText.toDoubleOrNull()
                    if (amountVal == null || amountVal <= 0.0) {
                        isError = true
                        return@Button
                    }
                    onPay((amountVal * 100).toLong())
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Confirm Payment", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
