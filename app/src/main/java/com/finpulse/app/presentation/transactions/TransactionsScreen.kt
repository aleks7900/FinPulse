package com.finpulse.app.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finpulse.app.R
import com.finpulse.app.core.designsystem.AmberWarning
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.TransferBlue
import com.finpulse.app.core.ui.CategoryIconBadge
import com.finpulse.app.core.ui.CategoryPickerDialog
import com.finpulse.app.core.ui.TransactionItem
import com.finpulse.app.core.ui.getDisplayName
import com.finpulse.app.core.ui.getLocalizedName
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.DateRangePreset
import com.finpulse.app.domain.model.SavedFilter
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionFilterParams
import com.finpulse.app.domain.model.TransactionPreset
import com.finpulse.app.domain.model.TransactionPresets
import com.finpulse.app.domain.model.TransactionSort
import com.finpulse.app.domain.model.TransactionStatusFilter
import com.finpulse.app.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: TransactionsUiState,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onAccountFilterChange: (String?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onSortOrderChange: (TransactionSort) -> Unit,
    onShowFilterSheet: (Boolean) -> Unit,
    onShowAddEditDialog: (Boolean, Transaction?) -> Unit,
    onSaveTransaction: (
        id: String?,
        amountMinor: Long,
        type: TransactionType,
        sourceAccountId: String,
        destinationAccountId: String?,
        categoryId: String,
        merchant: String?,
        description: String,
        tags: List<String>,
        notes: String?,
        timestamp: Long
    ) -> Unit,
    onDuplicateTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onToggleFilterOnlyUnreviewed: () -> Unit = {},
    onDateRangePresetChange: (DateRangePreset, Long?, Long?) -> Unit = { _, _, _ -> },
    onAmountRangeChange: (Long?, Long?) -> Unit = { _, _ -> },
    onCurrencyFilterChange: (String?) -> Unit = {},
    onStatusFilterChange: (TransactionStatusFilter) -> Unit = {},
    onSelectPreset: (TransactionPreset) -> Unit = {},
    onSelectSavedFilter: (SavedFilter) -> Unit = {},
    onResetFilters: () -> Unit = {},
    onShowSaveViewDialog: (Boolean) -> Unit = {},
    onSaveCurrentView: (String) -> Unit = {},
    onDeleteSavedView: (String) -> Unit = {},
    onNavigateToReviewQueue: () -> Unit = {},
    onNavigateToCsvImport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryMap = uiState.categories.associateBy { it.id }
    val accountMap = uiState.accounts.associateBy { it.id }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tx_filter_sort_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToCsvImport) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = stringResource(R.string.more_csv_title),
                            tint = EmeraldPrimary
                        )
                    }
                    IconButton(onClick = { onShowFilterSheet(true) }) {
                        if (uiState.activeFilterCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = EmeraldPrimary,
                                        contentColor = Color.Black
                                    ) {
                                        Text(uiState.activeFilterCount.toString(), fontWeight = FontWeight.Bold)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.action_filter),
                                    tint = EmeraldPrimary
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.action_filter),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.cd_add))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.tx_search_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            // Presets and Saved Views Row
            PresetsAndSavedViewsRow(
                activePresetId = uiState.activePresetId,
                activeSavedFilterId = uiState.activeSavedFilterId,
                savedFilters = uiState.savedFilters,
                unreviewedCount = uiState.unreviewedCount,
                hasActiveFilters = uiState.filterParams.isActive,
                onSelectPreset = onSelectPreset,
                onSelectSavedFilter = onSelectSavedFilter,
                onResetFilters = onResetFilters,
                onShowSaveViewDialog = { onShowSaveViewDialog(true) }
            )

            // Review Queue Alert Banner
            if (uiState.unreviewedCount > 0 && !uiState.filterOnlyUnreviewed) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToReviewQueue),
                    shape = RoundedCornerShape(10.dp),
                    color = AmberWarning.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.tx_review_banner, uiState.unreviewedCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.tx_review_queue_action),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning
                        )
                    }
                }
            }

            // Results count and active filter summary bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_results_count, uiState.transactions.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                if (uiState.filterParams.isActive) {
                    TextButton(onClick = onResetFilters) {
                        Text(
                            text = stringResource(R.string.action_reset),
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Transaction List
            if (uiState.transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.tx_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.tx_empty_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.transactions, key = { it.id }) { tx ->
                        val cat = categoryMap[tx.categoryId]
                        val acc = accountMap[tx.sourceAccountId]?.name ?: stringResource(R.string.tx_account)
                        TransactionItem(
                            transaction = tx,
                            category = cat,
                            accountName = acc,
                            hideBalances = uiState.hideBalances,
                            onClick = { onShowAddEditDialog(true, tx) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Comprehensive Filter Bottom Sheet
        if (uiState.isFilterSheetVisible) {
            FilterBottomSheet(
                accounts = uiState.accounts,
                categories = uiState.categories,
                filterParams = uiState.filterParams,
                savedFilters = uiState.savedFilters,
                activeSavedFilterId = uiState.activeSavedFilterId,
                onAccountSelected = onAccountFilterChange,
                onCategorySelected = onCategoryFilterChange,
                onTypeSelected = onTypeFilterChange,
                onSortOrderChange = onSortOrderChange,
                onDateRangePresetChange = onDateRangePresetChange,
                onAmountRangeChange = onAmountRangeChange,
                onCurrencyFilterChange = onCurrencyFilterChange,
                onStatusFilterChange = onStatusFilterChange,
                onSelectPreset = onSelectPreset,
                onSaveViewClick = {
                    onShowFilterSheet(false)
                    onShowSaveViewDialog(true)
                },
                onDeleteSavedView = onDeleteSavedView,
                onReset = onResetFilters,
                onDismiss = { onShowFilterSheet(false) }
            )
        }

        // Save View Dialog
        if (uiState.isSaveViewDialogVisible) {
            SaveFilterViewDialog(
                onDismiss = { onShowSaveViewDialog(false) },
                onSave = onSaveCurrentView
            )
        }

        // Add / Edit Transaction Dialog
        if (uiState.isAddEditDialogVisible) {
            AddEditTransactionDialog(
                editingTransaction = uiState.editingTransaction,
                accounts = uiState.accounts,
                categories = uiState.categories,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { onShowAddEditDialog(false, null) },
                onSave = onSaveTransaction,
                onDuplicate = { uiState.editingTransaction?.let { onDuplicateTransaction(it) } },
                onDelete = { uiState.editingTransaction?.let { onDeleteTransaction(it.id) } }
            )
        }
    }
}

@Composable
fun PresetsAndSavedViewsRow(
    activePresetId: String?,
    activeSavedFilterId: String?,
    savedFilters: List<SavedFilter>,
    unreviewedCount: Int,
    hasActiveFilters: Boolean,
    onSelectPreset: (TransactionPreset) -> Unit,
    onSelectSavedFilter: (SavedFilter) -> Unit,
    onResetFilters: () -> Unit,
    onShowSaveViewDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "All" chip
        val isAllSelected = activePresetId == null && activeSavedFilterId == null && !hasActiveFilters
        Surface(
            modifier = Modifier.clickable { onResetFilters() },
            shape = RoundedCornerShape(14.dp),
            color = if (isAllSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = if (isAllSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Text(
                text = stringResource(R.string.tx_filter_all),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Standard Presets
        TransactionPresets.ALL_PRESETS.forEach { preset ->
            val isSelected = activePresetId == preset.id
            val presetName = when (preset.id) {
                TransactionPresets.THIS_MONTH.id -> stringResource(R.string.preset_this_month)
                TransactionPresets.LAST_MONTH.id -> stringResource(R.string.preset_last_month)
                TransactionPresets.UNCATEGORIZED.id -> {
                    if (unreviewedCount > 0) "${stringResource(R.string.preset_uncategorized)} ($unreviewedCount)"
                    else stringResource(R.string.preset_uncategorized)
                }
                TransactionPresets.SUBSCRIPTIONS.id -> stringResource(R.string.preset_subscriptions)
                TransactionPresets.LARGE_EXPENSES.id -> stringResource(R.string.preset_large_expenses)
                TransactionPresets.TRANSFERS.id -> stringResource(R.string.preset_transfers)
                else -> preset.name
            }

            Surface(
                modifier = Modifier.clickable { onSelectPreset(preset) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                border = if (preset.id == TransactionPresets.UNCATEGORIZED.id && unreviewedCount > 0 && !isSelected)
                    androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (preset.id == TransactionPresets.UNCATEGORIZED.id && unreviewedCount > 0) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isSelected) Color.Black else AmberWarning,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = presetName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Saved Views
        savedFilters.forEach { savedFilter ->
            val isSelected = activeSavedFilterId == savedFilter.id
            Surface(
                modifier = Modifier.clickable { onSelectSavedFilter(savedFilter) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) TransferBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, TransferBlue.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (isSelected) Color.White else TransferBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = savedFilter.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Save Current View Chip
        if (hasActiveFilters && activeSavedFilterId == null) {
            Surface(
                modifier = Modifier.clickable { onShowSaveViewDialog() },
                shape = RoundedCornerShape(14.dp),
                color = EmeraldPrimary.copy(alpha = 0.15f),
                contentColor = EmeraldPrimary,
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.save_view_action),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SaveFilterViewDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var viewName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.save_view_dialog_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = viewName,
                    onValueChange = {
                        viewName = it
                        isError = false
                    },
                    placeholder = { Text(stringResource(R.string.save_view_name_hint)) },
                    singleLine = true,
                    isError = isError,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (viewName.trim().isNotBlank()) {
                        onSave(viewName.trim())
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.action_save), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    accounts: List<Account>,
    categories: List<Category>,
    filterParams: TransactionFilterParams,
    savedFilters: List<SavedFilter>,
    activeSavedFilterId: String?,
    onAccountSelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onTypeSelected: (TransactionType?) -> Unit,
    onSortOrderChange: (TransactionSort) -> Unit,
    onDateRangePresetChange: (DateRangePreset, Long?, Long?) -> Unit,
    onAmountRangeChange: (Long?, Long?) -> Unit,
    onCurrencyFilterChange: (String?) -> Unit,
    onStatusFilterChange: (TransactionStatusFilter) -> Unit,
    onSelectPreset: (TransactionPreset) -> Unit,
    onSaveViewClick: () -> Unit,
    onDeleteSavedView: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tx_filter_sort_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.action_reset), color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Sort Order
            Text(stringResource(R.string.sort_by), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip(stringResource(R.string.sort_newest), filterParams.sortOrder == TransactionSort.DATE_DESC) {
                    onSortOrderChange(TransactionSort.DATE_DESC)
                }
                SortChip(stringResource(R.string.sort_oldest), filterParams.sortOrder == TransactionSort.DATE_ASC) {
                    onSortOrderChange(TransactionSort.DATE_ASC)
                }
                SortChip(stringResource(R.string.sort_highest), filterParams.sortOrder == TransactionSort.AMOUNT_DESC) {
                    onSortOrderChange(TransactionSort.AMOUNT_DESC)
                }
                SortChip(stringResource(R.string.sort_lowest), filterParams.sortOrder == TransactionSort.AMOUNT_ASC) {
                    onSortOrderChange(TransactionSort.AMOUNT_ASC)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Transaction Type
            Text(stringResource(R.string.filter_type), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf(null to stringResource(R.string.tx_filter_all)) + TransactionType.values().map { it to it.getLocalizedName() }
                types.forEach { (type, label) ->
                    val isSelected = filterParams.type == type
                    SortChip(label, isSelected) {
                        onTypeSelected(type)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Status Filter
            Text(stringResource(R.string.filter_status), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statuses = listOf(
                    TransactionStatusFilter.ALL to stringResource(R.string.filter_status_all),
                    TransactionStatusFilter.CONFIRMED to stringResource(R.string.filter_status_confirmed),
                    TransactionStatusFilter.NEEDS_REVIEW to stringResource(R.string.filter_status_needs_review),
                    TransactionStatusFilter.EXCLUDED_FROM_BUDGET to stringResource(R.string.filter_status_budget_excluded),
                    TransactionStatusFilter.RECURRING to stringResource(R.string.filter_status_recurring)
                )
                statuses.forEach { (status, label) ->
                    val isSelected = filterParams.status == status
                    SortChip(label, isSelected) {
                        onStatusFilterChange(status)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Date Range
            Text(stringResource(R.string.filter_date_range), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val datePresets = listOf(
                    DateRangePreset.ALL to stringResource(R.string.date_preset_all),
                    DateRangePreset.THIS_MONTH to stringResource(R.string.date_preset_this_month),
                    DateRangePreset.LAST_MONTH to stringResource(R.string.date_preset_last_month),
                    DateRangePreset.THIS_YEAR to stringResource(R.string.date_preset_this_year)
                )
                datePresets.forEach { (preset, label) ->
                    val isSelected = filterParams.dateRangePreset == preset
                    SortChip(label, isSelected) {
                        onDateRangePresetChange(preset, null, null)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Account Filter
            Text(stringResource(R.string.filter_account), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isAllAccounts = filterParams.accountId == null
                SortChip(stringResource(R.string.filter_all_accounts), isAllAccounts) {
                    onAccountSelected(null)
                }
                accounts.forEach { acc ->
                    val isSelected = filterParams.accountId == acc.id
                    SortChip(acc.name, isSelected) {
                        onAccountSelected(acc.id)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Category Filter
            Text(stringResource(R.string.filter_category), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            var showCategoryPicker by remember { mutableStateOf(false) }
            val selectedCat = categories.find { it.id == filterParams.categoryId }
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
                    if (selectedCat != null) {
                        CategoryIconBadge(selectedCat.icon, selectedCat.colorHex, size = 24.dp, iconSize = 14.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedCat.getDisplayName(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onCategorySelected(null) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.category_all_categories),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (showCategoryPicker) {
                CategoryPickerDialog(
                    categories = categories,
                    selectedCategoryId = filterParams.categoryId,
                    onCategorySelected = { cat ->
                        onCategorySelected(cat.id)
                        showCategoryPicker = false
                    },
                    onDismissRequest = { showCategoryPicker = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. Amount Range Filter
            Text(stringResource(R.string.filter_amount_range), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            var minText by remember { mutableStateOf(filterParams.minAmountMinor?.let { (it / 100.0).toString() } ?: "") }
            var maxText by remember { mutableStateOf(filterParams.maxAmountMinor?.let { (it / 100.0).toString() } ?: "") }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = {
                        minText = it
                        val minMinor = it.toDoubleOrNull()?.let { v -> (v * 100).toLong() }
                        onAmountRangeChange(minMinor, filterParams.maxAmountMinor)
                    },
                    label = { Text(stringResource(R.string.filter_min_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxText,
                    onValueChange = {
                        maxText = it
                        val maxMinor = it.toDoubleOrNull()?.let { v -> (v * 100).toLong() }
                        onAmountRangeChange(filterParams.minAmountMinor, maxMinor)
                    },
                    label = { Text(stringResource(R.string.filter_max_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 8. Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filterParams.isActive) {
                    OutlinedButton(
                        onClick = onSaveViewClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.save_view_action))
                    }
                }

                if (activeSavedFilterId != null) {
                    IconButton(onClick = { onDeleteSavedView(activeSavedFilterId) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.delete_view_action), tint = CrimsonExpense)
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_apply), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    editingTransaction: Transaction?,
    accounts: List<Account>,
    categories: List<Category>,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        amountMinor: Long,
        type: TransactionType,
        sourceAccountId: String,
        destinationAccountId: String?,
        categoryId: String,
        merchant: String?,
        description: String,
        tags: List<String>,
        notes: String?,
        timestamp: Long
    ) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var amountText by remember {
        mutableStateOf(
            editingTransaction?.amount?.amountBigDecimal?.toPlainString() ?: ""
        )
    }
    var selectedType by remember {
        mutableStateOf(editingTransaction?.type ?: TransactionType.EXPENSE)
    }
    var sourceAccountId by remember {
        mutableStateOf(editingTransaction?.sourceAccountId ?: accounts.firstOrNull()?.id ?: "")
    }
    var destinationAccountId by remember {
        mutableStateOf(editingTransaction?.destinationAccountId ?: accounts.getOrNull(1)?.id)
    }
    var categoryId by remember {
        mutableStateOf(editingTransaction?.categoryId ?: categories.firstOrNull()?.id ?: "")
    }

    val relevantCategories = remember(categories, selectedType) {
        val catType = when (selectedType) {
            TransactionType.INCOME, TransactionType.REFUND -> CategoryType.INCOME
            else -> CategoryType.EXPENSE
        }
        categories.filter { it.type == catType }
    }
    var showCategoryPicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedType) {
        if (selectedType == TransactionType.TRANSFER) {
            categoryId = "cat_transfer"
        } else {
            val catType = when (selectedType) {
                TransactionType.INCOME, TransactionType.REFUND -> CategoryType.INCOME
                else -> CategoryType.EXPENSE
            }
            val currentCat = categories.find { it.id == categoryId }
            if (currentCat == null || currentCat.type != catType) {
                categoryId = categories.firstOrNull { it.type == catType }?.id ?: ""
            }
        }
    }

    var merchant by remember {
        mutableStateOf(editingTransaction?.merchant ?: "")
    }
    var description by remember {
        mutableStateOf(editingTransaction?.description ?: "")
    }
    var tagsText by remember {
        mutableStateOf(editingTransaction?.tags?.joinToString(", ") ?: "")
    }
    var notes by remember {
        mutableStateOf(editingTransaction?.notes ?: "")
    }

    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingTransaction != null) stringResource(R.string.action_edit) else stringResource(R.string.tx_record_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransactionType.values().take(3).forEach { type ->
                        val isSel = selectedType == type
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedType = type },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) when (type) {
                                TransactionType.EXPENSE -> CrimsonExpense
                                TransactionType.INCOME -> EmeraldPrimary
                                TransactionType.TRANSFER -> TransferBlue
                                else -> EmeraldPrimary
                            } else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                        ) {
                            Text(
                                text = type.getLocalizedName(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Category Selector Surface for non-transfers
                if (selectedType != TransactionType.TRANSFER) {
                    val currentCategory = categories.find { it.id == categoryId }
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
                            if (currentCategory != null) {
                                CategoryIconBadge(
                                    iconName = currentCategory.icon,
                                    colorHex = currentCategory.colorHex,
                                    size = 28.dp,
                                    iconSize = 16.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = currentCategory.getDisplayName(),
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

                // Amount TextField
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        isError = false
                    },
                    label = { Text("${stringResource(R.string.tx_amount)} ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Merchant / Payee
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text(stringResource(R.string.tx_merchant)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.tx_notes)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Tags
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text(stringResource(R.string.tx_tags)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountText.toDoubleOrNull()
                    if (amountVal == null || amountVal <= 0.0 || sourceAccountId.isBlank() || categoryId.isBlank()) {
                        isError = true
                        return@Button
                    }
                    val amountMinor = (amountVal * 100).toLong()
                    val tagList = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    onSave(
                        editingTransaction?.id,
                        amountMinor,
                        selectedType,
                        sourceAccountId,
                        if (selectedType == TransactionType.TRANSFER) destinationAccountId else null,
                        categoryId,
                        merchant,
                        description,
                        tagList,
                        notes,
                        editingTransaction?.timestamp ?: System.currentTimeMillis()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.action_save), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (editingTransaction != null) {
                    IconButton(onClick = onDuplicate) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = stringResource(R.string.action_duplicate))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = CrimsonExpense)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )

    if (showCategoryPicker) {
        val catType = when (selectedType) {
            TransactionType.INCOME, TransactionType.REFUND -> CategoryType.INCOME
            else -> CategoryType.EXPENSE
        }
        CategoryPickerDialog(
            categories = relevantCategories,
            selectedCategoryId = categoryId,
            onCategorySelected = { cat ->
                categoryId = cat.id
                showCategoryPicker = false
            },
            onDismissRequest = { showCategoryPicker = false }
        )
    }
}
