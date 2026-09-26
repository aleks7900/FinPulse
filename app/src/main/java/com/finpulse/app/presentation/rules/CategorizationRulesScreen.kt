package com.finpulse.app.presentation.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finpulse.app.R
import com.finpulse.app.core.designsystem.AmberWarning
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.PurpleAccent
import com.finpulse.app.core.designsystem.TransferBlue
import com.finpulse.app.core.ui.CategoryIconBadge
import com.finpulse.app.core.ui.DateFormatterUtils
import com.finpulse.app.core.ui.getDisplayName
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.CategorizationConfidence
import com.finpulse.app.domain.model.CategorizationRule
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.MatchType
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.usecase.categorization.ReviewQueueItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorizationRulesScreen(
    uiState: CategorizationRulesUiState,
    onNavigateBack: () -> Unit,
    onTabSelected: (CategorizationTab) -> Unit,
    onConfirmSuggestion: (ReviewQueueItem) -> Unit,
    onOpenChangeCategory: (Transaction) -> Unit,
    onChangeCategory: (transactionId: String, newCategoryId: String) -> Unit,
    onOpenCreateRuleFromTransaction: (Transaction, suggestedCategory: String?) -> Unit,
    onShowAddEditRuleDialog: (Boolean, CategorizationRule?) -> Unit,
    onSaveRule: (
        id: String?,
        name: String,
        targetCategoryId: String,
        priority: Int,
        merchantPattern: String?,
        merchantMatchType: MatchType,
        descriptionPattern: String?,
        descriptionMatchType: MatchType,
        accountId: String?,
        minAmountMinor: Long?,
        maxAmountMinor: Long?,
        transactionType: TransactionType?,
        isActive: Boolean
    ) -> Unit,
    onToggleRuleActive: (CategorizationRule) -> Unit,
    onUpdateRulePriority: (ruleId: String, newPriority: Int) -> Unit,
    onDeleteRule: (ruleId: String) -> Unit,
    onOpenApplyRuleDialog: (ruleId: String) -> Unit,
    onSetOverrideManualOnApply: (Boolean) -> Unit,
    onConfirmApplyRule: () -> Unit,
    onDismissDialogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryMap = uiState.categories.associateBy { it.id }
    val accountMap = uiState.accounts.associateBy { it.id }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.rules_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.selectedTab == CategorizationTab.RULES) {
                FloatingActionButton(
                    onClick = { onShowAddEditRuleDialog(true, null) },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.rules_create_title))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Secondary Tab Row with Badge on Review Queue
            SecondaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldPrimary
            ) {
                CategorizationTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(if (tab == CategorizationTab.REVIEW_QUEUE) stringResource(R.string.rules_tab_review, uiState.reviewQueue.size).replace(Regex(""" \(\d+\)"""), "") else stringResource(R.string.rules_tab_rules, uiState.rules.size).replace(Regex(""" \(\d+\)"""), ""))
                                if (tab == CategorizationTab.REVIEW_QUEUE && uiState.reviewQueue.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(
                                        containerColor = AmberWarning,
                                        contentColor = Color.Black
                                    ) {
                                        Text(text = "${uiState.reviewQueue.size}")
                                    }
                                } else if (tab == CategorizationTab.RULES && uiState.rules.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Text(text = "${uiState.rules.size}")
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Tab Content
            when (uiState.selectedTab) {
                CategorizationTab.REVIEW_QUEUE -> {
                    ReviewQueueTabContent(
                        queue = uiState.reviewQueue,
                        categoryMap = categoryMap,
                        accountMap = accountMap,
                        onConfirmSuggestion = onConfirmSuggestion,
                        onOpenChangeCategory = onOpenChangeCategory,
                        onOpenCreateRule = onOpenCreateRuleFromTransaction
                    )
                }
                CategorizationTab.RULES -> {
                    RulesTabContent(
                        rules = uiState.rules,
                        categoryMap = categoryMap,
                        accountMap = accountMap,
                        onToggleActive = onToggleRuleActive,
                        onUpdatePriority = onUpdateRulePriority,
                        onEditRule = { onShowAddEditRuleDialog(true, it) },
                        onDeleteRule = onDeleteRule,
                        onApplyRule = onOpenApplyRuleDialog
                    )
                }
            }
        }
    }

    // Add / Edit Rule Dialog
    if (uiState.isAddEditDialogVisible) {
        AddEditRuleDialog(
            rule = uiState.editingRule,
            prefilledMerchant = uiState.prefilledMerchant,
            prefilledCategoryId = uiState.prefilledCategoryId,
            categories = uiState.categories,
            accounts = uiState.accounts,
            onDismiss = onDismissDialogs,
            onSave = onSaveRule
        )
    }

    // Apply Rule to Existing Confirmation Dialog
    if (uiState.isApplyDialogVisible && uiState.applyRulePreview != null) {
        ApplyRuleConfirmationDialog(
            preview = uiState.applyRulePreview,
            overrideManual = uiState.overrideManualOnApply,
            categoryMap = categoryMap,
            onToggleOverrideManual = onSetOverrideManualOnApply,
            onConfirmApply = onConfirmApplyRule,
            onDismiss = onDismissDialogs
        )
    }

    // Change Category Dialog
    if (uiState.isChangeCategoryDialogVisible && uiState.changingTransaction != null) {
        ChangeCategoryPickerModal(
            transaction = uiState.changingTransaction,
            categories = uiState.categories,
            onCategorySelected = { catId ->
                onChangeCategory(uiState.changingTransaction.id, catId)
            },
            onDismiss = onDismissDialogs
        )
    }
}

// -------------------------------------------------------------------------
// Tab 1: Review Queue Content
// -------------------------------------------------------------------------
@Composable
private fun ReviewQueueTabContent(
    queue: List<ReviewQueueItem>,
    categoryMap: Map<String, Category>,
    accountMap: Map<String, Account>,
    onConfirmSuggestion: (ReviewQueueItem) -> Unit,
    onOpenChangeCategory: (Transaction) -> Unit,
    onOpenCreateRule: (Transaction, String?) -> Unit
) {
    if (queue.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.rules_review_queue_clear),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.rules_review_queue_clear_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.rules_transactions_need_review, queue.size),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.rules_one_tap_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(queue, key = { it.transaction.id }) { item ->
                ReviewQueueItemCard(
                    item = item,
                    categoryMap = categoryMap,
                    accountMap = accountMap,
                    onConfirm = { onConfirmSuggestion(item) },
                    onChangeCategory = { onOpenChangeCategory(item.transaction) },
                    onCreateRule = { onOpenCreateRule(item.transaction, item.suggestion.categoryId) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ReviewQueueItemCard(
    item: ReviewQueueItem,
    categoryMap: Map<String, Category>,
    accountMap: Map<String, Account>,
    onConfirm: () -> Unit,
    onChangeCategory: () -> Unit,
    onCreateRule: () -> Unit
) {
    val tx = item.transaction
    val suggestion = item.suggestion
    val suggestedCat = suggestion.categoryId?.let { categoryMap[it] }
    val currentCat = categoryMap[tx.categoryId]
    val account = accountMap[tx.sourceAccountId]

    val dateStr = DateFormatterUtils.formatDate(tx.timestamp)

    val confidenceColor = when (suggestion.confidence) {
        CategorizationConfidence.EXACT_RULE -> EmeraldPrimary
        CategorizationConfidence.HIGH -> TransferBlue
        CategorizationConfidence.MEDIUM -> AmberWarning
        CategorizationConfidence.LOW, CategorizationConfidence.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val confidenceLabel = when (suggestion.confidence) {
        CategorizationConfidence.EXACT_RULE -> "100% Rule Match"
        CategorizationConfidence.HIGH -> "${(suggestion.confidenceScore * 100).toInt()}% Confident"
        CategorizationConfidence.MEDIUM -> "Keyword Match"
        CategorizationConfidence.LOW -> "Low Confidence"
        CategorizationConfidence.NONE -> stringResource(R.string.cat_uncategorized)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Merchant & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.merchant?.takeIf { it.isNotBlank() } ?: tx.description.ifBlank { stringResource(R.string.tx_default_title) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        account?.let {
                            Text(
                                text = "• ${it.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    text = tx.amount.formatted(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (tx.type == TransactionType.INCOME) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Suggestion Pill Row
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        CategoryIconBadge(
                            iconName = suggestedCat?.icon ?: currentCat?.icon ?: "help",
                            colorHex = suggestedCat?.colorHex ?: currentCat?.colorHex ?: 0xFF9E9E9E,
                            size = 28.dp,
                            iconSize = 16.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = suggestedCat?.getDisplayName() ?: currentCat?.getDisplayName() ?: stringResource(R.string.cat_uncategorized),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = suggestion.explanation,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Confidence Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = confidenceColor.copy(alpha = 0.15f),
                        contentColor = confidenceColor
                    ) {
                        Text(
                            text = confidenceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Confirm Suggestion (if suggestion exists)
                if (suggestion.categoryId != null) {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_confirm))
                    }
                }

                // Change Category
                OutlinedButton(
                    onClick = onChangeCategory,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_edit))
                }

                // Create Rule from this merchant
                IconButton(
                    onClick = onCreateRule,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = stringResource(R.string.rules_create_title),
                        tint = PurpleAccent
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Tab 2: Rules Content
// -------------------------------------------------------------------------
@Composable
private fun RulesTabContent(
    rules: List<CategorizationRule>,
    categoryMap: Map<String, Category>,
    accountMap: Map<String, Account>,
    onToggleActive: (CategorizationRule) -> Unit,
    onUpdatePriority: (ruleId: String, newPriority: Int) -> Unit,
    onEditRule: (CategorizationRule) -> Unit,
    onDeleteRule: (ruleId: String) -> Unit,
    onApplyRule: (ruleId: String) -> Unit
) {
    if (rules.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = PurpleAccent.copy(alpha = 0.12f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Rule,
                            contentDescription = null,
                            tint = PurpleAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.rules_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Create rules to automatically classify transactions by merchant, description keywords, and accounts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rules are evaluated in order of priority (highest first)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(rules, key = { it.id }) { rule ->
                RuleCard(
                    rule = rule,
                    category = categoryMap[rule.targetCategoryId],
                    account = rule.accountId?.let { accountMap[it] },
                    onToggleActive = { onToggleActive(rule) },
                    onIncreasePriority = { onUpdatePriority(rule.id, rule.priority + 10) },
                    onDecreasePriority = { onUpdatePriority(rule.id, maxOf(0, rule.priority - 10)) },
                    onEdit = { onEditRule(rule) },
                    onDelete = { onDeleteRule(rule.id) },
                    onApply = { onApplyRule(rule.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun RuleCard(
    rule: CategorizationRule,
    category: Category?,
    account: Account?,
    onToggleActive: () -> Unit,
    onIncreasePriority: () -> Unit,
    onDecreasePriority: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onApply: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (rule.isActive) 0.5f else 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Name, Priority Badge, Active Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PurpleAccent.copy(alpha = 0.15f),
                        contentColor = PurpleAccent
                    ) {
                        Text(
                            text = "Priority ${rule.priority}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Switch(
                    checked = rule.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldPrimary,
                        checkedTrackColor = EmeraldPrimary.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Target Category Pill
            category?.let { cat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Assign to: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    CategoryIconBadge(
                        iconName = cat.icon,
                        colorHex = cat.colorHex,
                        size = 20.dp,
                        iconSize = 12.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cat.getDisplayName(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Conditions summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!rule.merchantPattern.isNullOrBlank()) {
                    Text(
                        text = "• Merchant ${rule.merchantMatchType.name.lowercase()}: \"${rule.merchantPattern}\"",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!rule.descriptionPattern.isNullOrBlank()) {
                    Text(
                        text = "• Description ${rule.descriptionMatchType.name.lowercase()}: \"${rule.descriptionPattern}\"",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                account?.let {
                    Text(
                        text = "• Source Account: ${it.name}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (rule.transactionType != null) {
                    Text(
                        text = "• Type: ${rule.transactionType.displayName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row: Priority buttons, Apply, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onIncreasePriority, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Increase Priority", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDecreasePriority, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Decrease Priority", modifier = Modifier.size(18.dp))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onApply,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply to Existing", fontSize = 12.sp)
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Rule", modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Rule", tint = CrimsonExpense, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Modal 1: Add / Edit Rule Dialog
// -------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditRuleDialog(
    rule: CategorizationRule?,
    prefilledMerchant: String?,
    prefilledCategoryId: String?,
    categories: List<Category>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        name: String,
        targetCategoryId: String,
        priority: Int,
        merchantPattern: String?,
        merchantMatchType: MatchType,
        descriptionPattern: String?,
        descriptionMatchType: MatchType,
        accountId: String?,
        minAmountMinor: Long?,
        maxAmountMinor: Long?,
        transactionType: TransactionType?,
        isActive: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf(rule?.name ?: if (!prefilledMerchant.isNullOrBlank()) "Auto: $prefilledMerchant" else "") }
    var targetCategoryId by remember {
        mutableStateOf(rule?.targetCategoryId ?: prefilledCategoryId ?: categories.firstOrNull()?.id ?: "")
    }
    var priority by remember { mutableStateOf((rule?.priority ?: 10).toString()) }
    var merchantPattern by remember { mutableStateOf(rule?.merchantPattern ?: prefilledMerchant ?: "") }
    var merchantMatchType by remember { mutableStateOf(rule?.merchantMatchType ?: MatchType.CONTAINS) }
    var descriptionPattern by remember { mutableStateOf(rule?.descriptionPattern ?: "") }
    var descriptionMatchType by remember { mutableStateOf(rule?.descriptionMatchType ?: MatchType.CONTAINS) }
    var selectedAccountId by remember { mutableStateOf(rule?.accountId) }
    var isActive by remember { mutableStateOf(rule?.isActive ?: true) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var matchTypeDropdownExpanded by remember { mutableStateOf(false) }

    val selectedCategory = categories.find { it.id == targetCategoryId }
    val selectedAccount = accounts.find { it.id == selectedAccountId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (rule != null) "Edit Categorization Rule" else "New Categorization Rule",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rule Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    placeholder = { Text("e.g. Starbucks Coffee") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Target Category Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CategoryIconBadge(cat.icon, cat.colorHex, size = 24.dp, iconSize = 14.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(cat.name)
                                    }
                                },
                                onClick = {
                                    targetCategoryId = cat.id
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Priority
                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Priority (Higher evaluated first)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Merchant Condition
                OutlinedTextField(
                    value = merchantPattern,
                    onValueChange = { merchantPattern = it },
                    label = { Text("Merchant Pattern (Optional)") },
                    placeholder = { Text("e.g. Uber, Netflix") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Merchant Match Type
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = merchantMatchType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Merchant Match Type") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { matchTypeDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = matchTypeDropdownExpanded,
                        onDismissRequest = { matchTypeDropdownExpanded = false }
                    ) {
                        MatchType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    merchantMatchType = type
                                    matchTypeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Description Pattern
                OutlinedTextField(
                    value = descriptionPattern,
                    onValueChange = { descriptionPattern = it },
                    label = { Text("Description Pattern (Optional)") },
                    placeholder = { Text("e.g. Salary, Rent") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Account Constraint (Optional: Account + Merchant -> Category)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Any Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Specific Account (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { accountDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Any Account") },
                            onClick = {
                                selectedAccountId = null
                                accountDropdownExpanded = false
                            }
                        )
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = {
                                    selectedAccountId = acc.id
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        rule?.id,
                        name,
                        targetCategoryId,
                        priority.toIntOrNull() ?: 10,
                        merchantPattern.takeIf { it.isNotBlank() },
                        merchantMatchType,
                        descriptionPattern.takeIf { it.isNotBlank() },
                        descriptionMatchType,
                        selectedAccountId,
                        null,
                        null,
                        null,
                        isActive
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// -------------------------------------------------------------------------
// Modal 2: Apply Rule to Existing Confirmation Dialog
// -------------------------------------------------------------------------
@Composable
private fun ApplyRuleConfirmationDialog(
    preview: com.finpulse.app.domain.usecase.categorization.RuleMatchPreview,
    overrideManual: Boolean,
    categoryMap: Map<String, Category>,
    onToggleOverrideManual: (Boolean) -> Unit,
    onConfirmApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val targetCat = categoryMap[preview.rule.targetCategoryId]

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.FlashOn, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(32.dp))
        },
        title = {
            Text(
                text = "Apply \"${preview.rule.name}\" to Existing?",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Assigns target category \"${targetCat?.name ?: "Selected"}\" to matching historical transactions.",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Match breakdown
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Matching Breakdown:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Safe (Uncategorized / Unreviewed): ${preview.safeMatches.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "• Previously Manual Categorized: ${preview.manualMatches.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (preview.manualMatches.isNotEmpty()) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Explicit Confirmation Checkbox to override manual categories
                if (preview.manualMatches.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleOverrideManual(!overrideManual) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = overrideManual,
                            onCheckedChange = { onToggleOverrideManual(it) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Explicitly overwrite ${preview.manualMatches.size} manually categorized transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overrideManual) CrimsonExpense else MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Text(
                        text = "All ${preview.safeMatches.size} matching transactions are unreviewed and safe to update.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmApply,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Confirm & Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// -------------------------------------------------------------------------
// Modal 3: Change Category Modal Picker
// -------------------------------------------------------------------------
@Composable
private fun ChangeCategoryPickerModal(
    transaction: Transaction,
    categories: List<Category>,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Select Category",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "For: ${transaction.merchant ?: transaction.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(categories, key = { it.id }) { cat ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (cat.id == transaction.categoryId) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(cat.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconBadge(cat.icon, cat.colorHex, size = 28.dp, iconSize = 16.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = cat.getDisplayName(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (cat.id == transaction.categoryId) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
