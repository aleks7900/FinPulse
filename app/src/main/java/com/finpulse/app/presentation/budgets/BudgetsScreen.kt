package com.finpulse.app.presentation.budgets

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finpulse.app.core.designsystem.AmberWarning
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.ui.BudgetProgressBar
import com.finpulse.app.core.ui.CategoryIconBadge
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.BudgetPeriod
import com.finpulse.app.domain.model.BudgetStatus
import com.finpulse.app.domain.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    uiState: BudgetsUiState,
    onShowAddEditDialog: (Boolean, Budget?) -> Unit,
    onSaveBudget: (id: String?, categoryId: String, name: String, limitMinor: Long, period: BudgetPeriod) -> Unit,
    onDeleteBudget: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Budget Control",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Budget")
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
            // Aggregate Budget Status Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "MONTHLY BUDGET CONSUMPTION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = if (uiState.hideBalances) "••••••" else uiState.totalSpent.formatted(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.hideBalances) "/ ••••••" else "limit ${uiState.totalBudgeted.formatted()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val totalPercentage = if (uiState.totalBudgeted.amountMinor > 0) {
                            uiState.totalSpent.amountMinor.toDouble() / uiState.totalBudgeted.amountMinor.toDouble()
                        } else 0.0

                        BudgetProgressBar(percentage = totalPercentage)

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${(totalPercentage * 100).toInt()}% consumed overall",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Budget Status Items
            if (uiState.budgetStatuses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No Budgets Configured",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create category budgets to prevent spending overruns.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.budgetStatuses, key = { it.budget.id }) { status ->
                    BudgetDetailCard(
                        status = status,
                        hideBalances = uiState.hideBalances,
                        onClick = { onShowAddEditDialog(true, status.budget) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Add / Edit Dialog
        if (uiState.isAddEditDialogVisible) {
            AddEditBudgetDialog(
                editingBudget = uiState.editingBudget,
                categories = uiState.categories,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { onShowAddEditDialog(false, null) },
                onSave = onSaveBudget,
                onDelete = { uiState.editingBudget?.let { onDeleteBudget(it.id) } }
            )
        }
    }
}

@Composable
fun BudgetDetailCard(
    status: BudgetStatus,
    hideBalances: Boolean,
    onClick: () -> Unit,
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
                CategoryIconBadge(
                    iconName = status.categoryName.lowercase(),
                    colorHex = status.categoryColorHex
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${status.budget.periodType.displayName} Budget",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (hideBalances) "•••• / ••••" else "${status.spentAmount.formatted()} / ${status.budget.limitAmount.formatted()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (hideBalances) "•••• left" else "${status.remainingAmount.formatted()} left",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (status.isExceeded) CrimsonExpense else EmeraldPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            BudgetProgressBar(percentage = status.percentageConsumed)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(status.percentageConsumed * 100).toInt()}% consumed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (status.isExceeded) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = CrimsonExpense, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exceeded", style = MaterialTheme.typography.labelSmall, color = CrimsonExpense, fontWeight = FontWeight.Bold)
                    }
                } else if (status.projectedSpend > status.budget.limitAmount) {
                    Text(
                        text = "Projected: ${status.projectedSpend.formatted()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarning,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetDialog(
    editingBudget: Budget?,
    categories: List<Category>,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (id: String?, categoryId: String, name: String, limitMinor: Long, period: BudgetPeriod) -> Unit,
    onDelete: () -> Unit
) {
    var categoryId by remember { mutableStateOf(editingBudget?.categoryId ?: categories.firstOrNull()?.id ?: "") }
    var limitText by remember { mutableStateOf(editingBudget?.limitAmount?.amountBigDecimal?.toPlainString() ?: "") }
    var selectedPeriod by remember { mutableStateOf(editingBudget?.periodType ?: BudgetPeriod.MONTHLY) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editingBudget != null) "Edit Budget" else "Set Category Budget", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Limit
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it; isError = false },
                    label = { Text("Budget Limit ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Period Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(BudgetPeriod.MONTHLY, BudgetPeriod.WEEKLY).forEach { p ->
                        val isSel = selectedPeriod == p
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPeriod = p },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface
                        ) {
                            Text(
                                text = p.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limitVal = limitText.toDoubleOrNull()
                    if (limitVal == null || limitVal <= 0.0 || categoryId.isBlank()) {
                        isError = true
                        return@Button
                    }
                    val limitMinor = (limitVal * 100).toLong()
                    val catName = categories.find { it.id == categoryId }?.name ?: "Budget"
                    onSave(editingBudget?.id, categoryId, catName, limitMinor, selectedPeriod)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (editingBudget != null) {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonExpense)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
