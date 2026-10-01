package md.alexlab.finpulse.presentation.goals

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.CrimsonExpense
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.ui.BudgetProgressBar
import md.alexlab.finpulse.core.ui.DateFormatterUtils
import md.alexlab.finpulse.domain.model.FinancialGoal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    uiState: GoalsUiState,
    onNavigateBack: () -> Unit,
    onShowAddEditDialog: (Boolean, FinancialGoal?) -> Unit,
    onShowContributeDialog: (Boolean, FinancialGoal?) -> Unit,
    onSaveGoal: (id: String?, title: String, targetMinor: Long, currentMinor: Long, targetDate: Long) -> Unit,
    onContribute: (goalId: String, amountMinor: Long) -> Unit,
    onDeleteGoal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.goals_title),
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
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.goals_add_title))
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
            // Aggregate Goals Progress Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.goals_total_saved).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = if (uiState.hideBalances) "••••••" else uiState.totalSaved.formatted(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.hideBalances) "/ ••••••" else stringResource(R.string.goal_target_format, uiState.totalTarget.formatted()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val overallProgress = if (uiState.totalTarget.amountMinor > 0) {
                            uiState.totalSaved.amountMinor.toDouble() / uiState.totalTarget.amountMinor.toDouble()
                        } else 0.0

                        Spacer(modifier = Modifier.height(10.dp))
                        BudgetProgressBar(percentage = overallProgress)
                    }
                }
            }

            // Goals list
            if (uiState.goals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.goal_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.goal_empty_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        hideBalances = uiState.hideBalances,
                        onClick = { onShowAddEditDialog(true, goal) },
                        onContribute = { onShowContributeDialog(true, goal) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Add / Edit Dialog
        if (uiState.isAddEditDialogVisible) {
            AddEditGoalDialog(
                editingGoal = uiState.selectedGoal,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { onShowAddEditDialog(false, null) },
                onSave = onSaveGoal,
                onDelete = { uiState.selectedGoal?.let { onDeleteGoal(it.id) } }
            )
        }

        // Contribute Dialog
        if (uiState.isContributeDialogVisible && uiState.selectedGoal != null) {
            ContributeGoalDialog(
                goal = uiState.selectedGoal,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { onShowContributeDialog(false, null) },
                onContribute = { amountMinor -> onContribute(uiState.selectedGoal.id, amountMinor) }
            )
        }
    }
}

@Composable
fun GoalCard(
    goal: FinancialGoal,
    hideBalances: Boolean,
    onClick: () -> Unit,
    onContribute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deadlineFormatted = DateFormatterUtils.formatMonthYear(goal.targetDate)
    val suggestedMonthly = goal.calculateSuggestedMonthlyContribution()

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
                    imageVector = if (goal.isCompleted) Icons.Default.CheckCircle else Icons.Default.Savings,
                    contentDescription = null,
                    tint = if (goal.isCompleted) EmeraldPrimary else Color(goal.colorHex),
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${stringResource(R.string.goal_target_date)}: $deadlineFormatted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (hideBalances) "•••• / ••••" else "${goal.currentAmount.formatted()} / ${goal.targetAmount.formatted()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.goal_achieved_percent, (goal.progressPercentage * 100).toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            BudgetProgressBar(percentage = goal.progressPercentage)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (goal.isCompleted) stringResource(R.string.goal_completed) else stringResource(R.string.goal_suggested_monthly, suggestedMonthly.formatted()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!goal.isCompleted) {
                    OutlinedButton(
                        onClick = onContribute,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.goal_add_funds), style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditGoalDialog(
    editingGoal: FinancialGoal?,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (id: String?, title: String, targetMinor: Long, currentMinor: Long, targetDate: Long) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(editingGoal?.title ?: "") }
    var targetText by remember { mutableStateOf(editingGoal?.targetAmount?.amountBigDecimal?.toPlainString() ?: "") }
    var currentText by remember { mutableStateOf(editingGoal?.currentAmount?.amountBigDecimal?.toPlainString() ?: "0.00") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingGoal != null) stringResource(R.string.goal_edit_title) else stringResource(R.string.goals_add_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; isError = false },
                    label = { Text(stringResource(R.string.goal_name_hint)) },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("${stringResource(R.string.goal_target_amount_hint)} ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = it },
                    label = { Text("${stringResource(R.string.goal_current_amount_hint)} ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetVal = targetText.toDoubleOrNull()
                    val currentVal = currentText.toDoubleOrNull() ?: 0.0
                    if (title.isBlank() || targetVal == null || targetVal <= 0.0) {
                        isError = true
                        return@Button
                    }
                    val targetMinor = (targetVal * 100).toLong()
                    val currentMinor = (currentVal * 100).toLong()
                    val targetDate = editingGoal?.targetDate ?: (System.currentTimeMillis() + (180L * 86_400_000L)) // 6 mo default
                    onSave(editingGoal?.id, title, targetMinor, currentMinor, targetDate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.action_save), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (editingGoal != null) {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = CrimsonExpense)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    )
}

@Composable
fun ContributeGoalDialog(
    goal: FinancialGoal,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onContribute: (amountMinor: Long) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_deposit_title, goal.title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.goal_deposit_remaining, goal.remainingAmount.formatted()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; isError = false },
                    label = { Text("${stringResource(R.string.tx_amount)} ($baseCurrency)") },
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
                    onContribute((amountVal * 100).toLong())
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.goals_contribute_btn), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
