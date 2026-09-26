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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.finpulse.app.domain.model.Transaction
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
    onNavigateToReviewQueue: () -> Unit = {},
    onNavigateToCsvImport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryMap = uiState.categories.associateBy { it.id }
    val accountMap = uiState.accounts.associateBy { it.id }

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
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.action_filter),
                            tint = if (uiState.selectedTypeFilter != null || uiState.selectedAccountFilter != null || uiState.selectedCategoryFilter != null)
                                EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                        )
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
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // Quick Filter Pills Row
            TypeFilterRow(
                selectedType = uiState.selectedTypeFilter,
                onTypeSelected = onTypeFilterChange,
                filterOnlyUnreviewed = uiState.filterOnlyUnreviewed,
                unreviewedCount = uiState.unreviewedCount,
                onToggleFilterOnlyUnreviewed = onToggleFilterOnlyUnreviewed
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

            Spacer(modifier = Modifier.height(12.dp))

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

        // Filter Sheet
        if (uiState.isFilterSheetVisible) {
            FilterBottomSheet(
                accounts = uiState.accounts,
                categories = uiState.categories,
                selectedAccount = uiState.selectedAccountFilter,
                selectedCategory = uiState.selectedCategoryFilter,
                sortOrder = uiState.sortOrder,
                onAccountSelected = onAccountFilterChange,
                onCategorySelected = onCategoryFilterChange,
                onSortOrderChange = onSortOrderChange,
                onDismiss = { onShowFilterSheet(false) },
                onReset = {
                    onTypeFilterChange(null)
                    onAccountFilterChange(null)
                    onCategoryFilterChange(null)
                    onSortOrderChange(TransactionSort.DATE_DESC)
                    onShowFilterSheet(false)
                }
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
fun TypeFilterRow(
    selectedType: TransactionType?,
    onTypeSelected: (TransactionType?) -> Unit,
    filterOnlyUnreviewed: Boolean = false,
    unreviewedCount: Int = 0,
    onToggleFilterOnlyUnreviewed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (unreviewedCount > 0) {
            Surface(
                modifier = Modifier.clickable { onToggleFilterOnlyUnreviewed() },
                shape = RoundedCornerShape(14.dp),
                color = if (filterOnlyUnreviewed) AmberWarning else AmberWarning.copy(alpha = 0.15f),
                contentColor = if (filterOnlyUnreviewed) Color.Black else AmberWarning,
                border = if (!filterOnlyUnreviewed) androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f)) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stringResource(R.string.tx_needs_review)} ($unreviewedCount)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        val types = listOf(null to stringResource(R.string.tx_filter_all)) + TransactionType.values().map { it to it.getLocalizedName() }
        types.forEach { (type, label) ->
            val isSelected = !filterOnlyUnreviewed && selectedType == type
            Surface(
                modifier = Modifier.clickable { onTypeSelected(type) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    accounts: List<Account>,
    categories: List<Category>,
    selectedAccount: String?,
    selectedCategory: String?,
    sortOrder: TransactionSort,
    onAccountSelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSortOrderChange: (TransactionSort) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
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
                    Text(stringResource(R.string.action_reset), color = EmeraldPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.sort_by), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortChip(stringResource(R.string.sort_newest), sortOrder == TransactionSort.DATE_DESC) { onSortOrderChange(TransactionSort.DATE_DESC) }
                SortChip(stringResource(R.string.sort_oldest), sortOrder == TransactionSort.DATE_ASC) { onSortOrderChange(TransactionSort.DATE_ASC) }
                SortChip(stringResource(R.string.sort_highest), sortOrder == TransactionSort.AMOUNT_DESC) { onSortOrderChange(TransactionSort.AMOUNT_DESC) }
                SortChip(stringResource(R.string.sort_lowest), sortOrder == TransactionSort.AMOUNT_ASC) { onSortOrderChange(TransactionSort.AMOUNT_ASC) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.quick_add_select_category), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            var showFilterCategoryPicker by remember { mutableStateOf(false) }
            val filterCategory = categories.find { it.id == selectedCategory }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showFilterCategoryPicker = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (filterCategory != null) {
                        CategoryIconBadge(filterCategory.icon, filterCategory.colorHex, size = 24.dp, iconSize = 14.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = filterCategory.getDisplayName(),
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
            if (showFilterCategoryPicker) {
                CategoryPickerDialog(
                    categories = categories,
                    selectedCategoryId = selectedCategory,
                    onCategorySelected = { cat ->
                        onCategorySelected(cat.id)
                        showFilterCategoryPicker = false
                    },
                    onDismissRequest = { showFilterCategoryPicker = false }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_apply), color = Color.Black, fontWeight = FontWeight.Bold)
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
            initialType = catType,
            allowedType = catType,
            onCategorySelected = { cat ->
                categoryId = cat.id
                showCategoryPicker = false
            },
            onDismissRequest = { showCategoryPicker = false }
        )
    }
}
