package com.finpulse.app.presentation.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finpulse.app.R
import com.finpulse.app.core.designsystem.AmberWarning
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.SapphireAccent
import com.finpulse.app.core.model.Money
import com.finpulse.app.core.ui.BudgetProgressBar
import com.finpulse.app.core.ui.CategoryIconBadge
import com.finpulse.app.core.ui.CategoryPickerDialog
import com.finpulse.app.core.ui.getCategoryDisplayName
import com.finpulse.app.core.ui.getDisplayName
import com.finpulse.app.core.ui.getLocalizedName
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.BudgetAlertLevel
import com.finpulse.app.domain.model.BudgetPeriod
import com.finpulse.app.domain.model.BudgetStatus
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.PacingStatus
import com.finpulse.app.domain.model.SafeToSpendBreakdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    uiState: BudgetsUiState,
    onShowAddEditDialog: (Boolean, Budget?) -> Unit,
    onSaveBudget: (
        id: String?,
        categoryId: String,
        name: String,
        limitMinor: Long,
        period: BudgetPeriod,
        isOverall: Boolean,
        isRolloverEnabled: Boolean,
        rolloverAmountMinor: Long,
        alertThresholdPercent: Int
    ) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onToggleAssumptionsDialog: (Boolean) -> Unit,
    onSelectFilter: (BudgetFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.budgets_title),
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
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.budget_add_dialog_title))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Safe-to-Spend Executive Card
            item {
                SafeToSpendCard(
                    breakdown = uiState.safeToSpendBreakdown,
                    hideBalances = uiState.hideBalances,
                    onInfoClick = { onToggleAssumptionsDialog(true) }
                )
            }

            // 2. Monthly Overall Budget Card
            item {
                OverallBudgetSection(
                    overallStatus = uiState.overallBudgetStatus,
                    totalSpent = uiState.totalSpent,
                    totalBudgeted = uiState.totalBudgeted,
                    hideBalances = uiState.hideBalances,
                    onAddOverallBudget = {
                        val placeholderOverall = Budget(
                            id = "",
                            categoryId = "overall",
                            name = "Overall Monthly Budget",
                            limitAmount = Money(200000L, uiState.baseCurrency),
                            periodType = BudgetPeriod.MONTHLY,
                            startDate = 0L,
                            endDate = 0L,
                            isOverall = true
                        )
                        onShowAddEditDialog(true, placeholderOverall)
                    },
                    onEditOverallBudget = { budget ->
                        onShowAddEditDialog(true, budget)
                    }
                )
            }

            // 3. Category Budgets Header & Filter Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Budgets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uiState.categoryBudgetStatuses.size} active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BudgetFilter.entries.forEach { filter ->
                            val isSelected = uiState.selectedFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectFilter(filter) },
                                label = { Text(filter.getLocalizedName(), style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }
            }

            // 4. Category Budget Cards
            if (uiState.categoryBudgetStatuses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.budget_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.budget_empty_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.categoryBudgetStatuses, key = { it.budget.id }) { status ->
                    CategoryBudgetCard(
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

        // Safe to Spend Assumptions & Formula Dialog
        if (uiState.isAssumptionsDialogVisible && uiState.safeToSpendBreakdown != null) {
            SafeToSpendAssumptionsDialog(
                breakdown = uiState.safeToSpendBreakdown,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { onToggleAssumptionsDialog(false) }
            )
        }
    }
}

/**
 * Executive Safe to Spend Card with Daily & Weekly pacing and transparency button.
 */
@Composable
fun SafeToSpendCard(
    breakdown: SafeToSpendBreakdown?,
    hideBalances: Boolean,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F3B2C),
            Color(0xFF132B29),
            Color(0xFF0F172A)
        )
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.safe_to_spend_title).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.clip(CircleShape).clickable { onInfoClick() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = stringResource(R.string.safe_to_spend_how_calculated),
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Formula",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val safeAmountFormatted = if (breakdown == null) {
                    "$0.00"
                } else if (hideBalances) {
                    "••••••"
                } else {
                    breakdown.discretionarySafeToSpend.formatted()
                }

                Text(
                    text = safeAmountFormatted,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = stringResource(R.string.safe_to_spend_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pacing split pills: Daily & Weekly
                if (breakdown != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Daily Pill
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = stringResource(R.string.safe_to_spend_daily).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (hideBalances) "••••" else stringResource(R.string.safe_to_spend_daily_pace, breakdown.dailySafeToSpend.formatted()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }

                        // Weekly Pill
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = stringResource(R.string.safe_to_spend_weekly).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (hideBalances) "••••" else stringResource(R.string.safe_to_spend_weekly_pace, breakdown.weeklySafeToSpend.formatted()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Days Left Pill
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "CYCLE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.safe_to_spend_days_left, breakdown.daysRemaining),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SapphireAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Monthly Overall Budget Section: either presents active overall budget or prompt to create one.
 */
@Composable
fun OverallBudgetSection(
    overallStatus: BudgetStatus?,
    totalSpent: Money,
    totalBudgeted: Money,
    hideBalances: Boolean,
    onAddOverallBudget: () -> Unit,
    onEditOverallBudget: (Budget) -> Unit,
    modifier: Modifier = Modifier
) {
    if (overallStatus != null) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onEditOverallBudget(overallStatus.budget) },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.budget_overall_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.budget_overall_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (overallStatus.budget.isRolloverEnabled) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SapphireAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Rollover active",
                                style = MaterialTheme.typography.labelSmall,
                                color = SapphireAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (hideBalances) "••••••" else overallStatus.spentAmount.formatted(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hideBalances) "/ ••••••" else "limit ${overallStatus.budget.effectiveLimit.formatted()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                BudgetProgressBar(percentage = overallStatus.percentageConsumed)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = if (overallStatus.isExceeded) {
                        "Over limit by " + (overallStatus.spentAmount - overallStatus.budget.effectiveLimit).formatted()
                    } else {
                        "${overallStatus.remainingAmount.formatted()} remaining"
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (overallStatus.isExceeded) CrimsonExpense else EmeraldPrimary
                    )

                    overallStatus.pacingMetrics?.let { pacing ->
                        val (pacingLabel, pacingColor) = when (pacing.pacingStatus) {
                            PacingStatus.ON_TRACK -> "On Track" to EmeraldPrimary
                            PacingStatus.AHEAD_OF_PACE -> "Ahead of Pace" to AmberWarning
                            PacingStatus.UNDER_PACE -> "Under Pace" to SapphireAccent
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = pacingColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = pacingLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = pacingColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Prompt card to establish overall budget
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onAddOverallBudget() },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.budget_add_overall_prompt),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Cap discretionary money across all categories",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddOverallBudget,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(stringResource(R.string.budget_set_limit), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Detailed Category Budget Card with Pacing, Rollover, and Alerts.
 */
@Composable
fun CategoryBudgetCard(
    status: BudgetStatus,
    hideBalances: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category Badge, Name, Rollover chip & Spent/Limit
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
                        text = getCategoryDisplayName(status.budget.categoryId, status.categoryName),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${status.budget.periodType.getLocalizedName()} budget",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (status.budget.isRolloverEnabled && status.budget.rolloverAmountMinor != 0L) {
                            val sign = if (status.budget.rolloverAmountMinor > 0) "+" else ""
                            val rolloverMoney = Money(status.budget.rolloverAmountMinor, status.budget.limitAmount.currencyCode)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SapphireAccent.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Rollover: $sign${rolloverMoney.formatted()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SapphireAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (hideBalances) "•••• / ••••" else "${status.spentAmount.formatted()} / ${status.budget.effectiveLimit.formatted()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (hideBalances) "••••" else {
                            if (status.isExceeded) {
                                val overspent = (status.spentAmount - status.budget.effectiveLimit).formatted()
                                "Over by $overspent"
                            } else {
                                stringResource(R.string.debt_card_balance_left, status.remainingAmount.formatted())
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (status.isExceeded) CrimsonExpense else EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            BudgetProgressBar(percentage = status.percentageConsumed)

            Spacer(modifier = Modifier.height(10.dp))

            // Daily & Weekly Pacing Row
            status.pacingMetrics?.let { pacing ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pacingText = when (pacing.pacingStatus) {
                        PacingStatus.ON_TRACK -> stringResource(R.string.budget_pacing_on_track, pacing.actualDailySpend.formatted())
                        PacingStatus.AHEAD_OF_PACE -> stringResource(R.string.budget_pacing_ahead, pacing.dailyPacingDelta.formatted())
                        PacingStatus.UNDER_PACE -> stringResource(R.string.budget_pacing_under, pacing.dailyPacingDelta.absolute().formatted())
                    }
                    val pacingColor = when (pacing.pacingStatus) {
                        PacingStatus.ON_TRACK -> EmeraldPrimary
                        PacingStatus.AHEAD_OF_PACE -> AmberWarning
                        PacingStatus.UNDER_PACE -> SapphireAccent
                    }

                    Text(
                        text = pacingText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = pacingColor
                    )

                    Text(
                        text = stringResource(R.string.budget_projected, pacing.projectedSpend.formatted()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Refund indicator if applicable
            if (status.refundsAmount.isPositive) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.budget_refunds_offset, status.refundsAmount.formatted()),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                }
            }

            // Threshold Alert Badge (if warning or exceeded)
            if (status.alertLevel != BudgetAlertLevel.NORMAL) {
                Spacer(modifier = Modifier.height(8.dp))
                val (alertBg, alertFg, alertText) = when (status.alertLevel) {
                    BudgetAlertLevel.EXCEEDED -> Triple(
                        CrimsonExpense.copy(alpha = 0.15f),
                        CrimsonExpense,
                        stringResource(R.string.budget_exceeded)
                    )
                    BudgetAlertLevel.WARNING_THRESHOLD -> Triple(
                        AmberWarning.copy(alpha = 0.15f),
                        AmberWarning,
                        stringResource(R.string.budget_threshold_warning, status.budget.alertThresholdPercent)
                    )
                    BudgetAlertLevel.INFO_70 -> Triple(
                        SapphireAccent.copy(alpha = 0.15f),
                        SapphireAccent,
                        stringResource(R.string.budget_threshold_heads_up)
                    )
                    BudgetAlertLevel.NORMAL -> Triple(Color.Transparent, Color.Transparent, "")
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = alertBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = alertFg,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = alertText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = alertFg
                        )
                    }
                }
            }
        }
    }
}

/**
 * Add / Edit Budget Dialog supporting Category budgets and Overall budgets with Rollover and Alert Thresholds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetDialog(
    editingBudget: Budget?,
    categories: List<Category>,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        categoryId: String,
        name: String,
        limitMinor: Long,
        period: BudgetPeriod,
        isOverall: Boolean,
        isRolloverEnabled: Boolean,
        rolloverAmountMinor: Long,
        alertThresholdPercent: Int
    ) -> Unit,
    onDelete: () -> Unit
) {
    val expenseCategories = remember(categories) {
        categories.filter { it.type == CategoryType.EXPENSE }
    }

    var isOverall by remember {
        mutableStateOf(editingBudget?.isOverall == true || editingBudget?.categoryId == "overall")
    }

    var categoryId by remember {
        mutableStateOf(editingBudget?.categoryId?.takeIf { it != "overall" } ?: expenseCategories.firstOrNull()?.id ?: "")
    }

    val selectedCategory = remember(expenseCategories, categoryId) {
        expenseCategories.find { it.id == categoryId }
    }
    var showCategoryPicker by remember { mutableStateOf(false) }

    var limitText by remember {
        mutableStateOf(editingBudget?.limitAmount?.amountBigDecimal?.toPlainString() ?: "")
    }

    var nameText by remember {
        mutableStateOf(editingBudget?.name ?: "")
    }

    var selectedPeriod by remember {
        mutableStateOf(editingBudget?.periodType ?: BudgetPeriod.MONTHLY)
    }

    var isRolloverEnabled by remember {
        mutableStateOf(editingBudget?.isRolloverEnabled == true)
    }

    var rolloverText by remember {
        val minor = editingBudget?.rolloverAmountMinor ?: 0L
        mutableStateOf(if (minor != 0L) (minor.toDouble() / 100.0).toString() else "")
    }

    var alertThreshold by remember {
        mutableIntStateOf(editingBudget?.alertThresholdPercent ?: 85)
    }

    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (editingBudget != null && editingBudget.id.isNotBlank()) {
                    stringResource(R.string.action_edit)
                } else {
                    stringResource(R.string.budget_add_dialog_title)
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Budget Type Switcher: Overall vs Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isOverall = false },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isOverall) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (!isOverall) Color.Black else MaterialTheme.colorScheme.onSurface
                    ) {
                        Text(
                            text = stringResource(R.string.budget_type_category),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isOverall = true },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isOverall) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isOverall) Color.Black else MaterialTheme.colorScheme.onSurface
                    ) {
                        Text(
                            text = stringResource(R.string.budget_type_overall),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // If Category budget, show category selector
                if (!isOverall) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCategoryPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedCategory != null) {
                                CategoryIconBadge(
                                    iconName = selectedCategory.icon,
                                    colorHex = selectedCategory.colorHex,
                                    size = 28.dp,
                                    iconSize = 16.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = selectedCategory.getDisplayName(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.quick_add_select_category),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Spending Limit
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it; isError = false },
                    label = { Text("${stringResource(R.string.budget_limit_hint)} ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Rollover Setting
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.budget_rollover_enable),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.budget_rollover_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isRolloverEnabled,
                                onCheckedChange = { isRolloverEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                            )
                        }

                        if (isRolloverEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = rolloverText,
                                onValueChange = { rolloverText = it },
                                label = { Text(stringResource(R.string.budget_rollover_amount_hint)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Alert Threshold Selector
                Column {
                    Text(
                        text = stringResource(R.string.budget_alert_threshold, alertThreshold),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(70, 80, 85, 90, 95).forEach { thresh ->
                            val isSel = alertThreshold == thresh
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { alertThreshold = thresh },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface
                            ) {
                                Text(
                                    text = "$thresh%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limitVal = limitText.toDoubleOrNull()
                    if (limitVal == null || limitVal <= 0.0 || (!isOverall && categoryId.isBlank())) {
                        isError = true
                        return@Button
                    }
                    val limitMinor = (limitVal * 100).toLong()
                    val rolloverMinor = ((rolloverText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    val catName = if (isOverall) {
                        nameText.ifBlank { "Overall Monthly Budget" }
                    } else {
                        selectedCategory?.name ?: categories.find { it.id == categoryId }?.name ?: "Budget"
                    }

                    val budgetId = editingBudget?.id?.takeIf { it.isNotBlank() }

                    onSave(
                        budgetId,
                        if (isOverall) "overall" else categoryId,
                        catName,
                        limitMinor,
                        selectedPeriod,
                        isOverall,
                        isRolloverEnabled,
                        rolloverMinor,
                        alertThreshold
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.action_save), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (editingBudget != null && editingBudget.id.isNotBlank()) {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = CrimsonExpense)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    )

    if (showCategoryPicker) {
        CategoryPickerDialog(
            categories = expenseCategories,
            selectedCategoryId = categoryId,
            initialType = CategoryType.EXPENSE,
            allowedType = CategoryType.EXPENSE,
            onCategorySelected = { cat ->
                categoryId = cat.id
                showCategoryPicker = false
            },
            onDismissRequest = { showCategoryPicker = false }
        )
    }
}

/**
 * Transparent Safe-to-Spend Breakdown & Assumptions Dialog.
 */
@Composable
fun SafeToSpendAssumptionsDialog(
    breakdown: SafeToSpendBreakdown,
    baseCurrency: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.safe_to_spend_breakdown_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Formula breakdown steps
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("(+) ${stringResource(R.string.safe_to_spend_liquid_funds)}", style = MaterialTheme.typography.bodySmall)
                            Text(breakdown.totalLiquidFunds.formatted(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("(-) ${stringResource(R.string.safe_to_spend_bills)}", style = MaterialTheme.typography.bodySmall)
                            Text("- ${breakdown.upcomingObligations.formatted()}", style = MaterialTheme.typography.bodySmall, color = CrimsonExpense)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("(-) ${stringResource(R.string.safe_to_spend_savings)}", style = MaterialTheme.typography.bodySmall)
                            Text("- ${breakdown.savingsTargets.formatted()}", style = MaterialTheme.typography.bodySmall, color = SapphireAccent)
                        }

                        if (breakdown.overallBudgetConstraint != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("(Cap) ${stringResource(R.string.safe_to_spend_budget_cap)}", style = MaterialTheme.typography.bodySmall)
                                Text(breakdown.overallBudgetConstraint.formatted(), style = MaterialTheme.typography.bodySmall, color = AmberWarning)
                            }
                        }

                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "(=) Discretionary Pool",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = breakdown.discretionarySafeToSpend.formatted(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }

                // Visible Assumptions list
                Text(
                    text = stringResource(R.string.safe_to_spend_assumptions_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    breakdown.assumptions.forEach { assumption ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = assumption,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Safety Disclaimer Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = stringResource(R.string.safe_to_spend_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.action_got_it), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}
