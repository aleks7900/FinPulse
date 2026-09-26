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
import com.finpulse.app.core.designsystem.TransferBlue
import com.finpulse.app.core.ui.TransactionItem
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Category
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
                        text = "Transactions Ledger",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { onShowFilterSheet(true) }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
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
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
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
                placeholder = { Text("Search merchant, note, tags...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
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
                            text = "${uiState.unreviewedCount} transactions need category review",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Review Queue →",
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
                            text = "No Transactions Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try clearing filters or search query.",
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
                        val acc = accountMap[tx.sourceAccountId]?.name ?: "Account"
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
                        text = "Needs Review ($unreviewedCount)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        val types = listOf(null to "All") + TransactionType.values().map { it to it.displayName }
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
                    text = "Filter & Sort Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onReset) {
                    Text("Reset All", color = EmeraldPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Sort By", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortChip("Newest", sortOrder == TransactionSort.DATE_DESC) { onSortOrderChange(TransactionSort.DATE_DESC) }
                SortChip("Oldest", sortOrder == TransactionSort.DATE_ASC) { onSortOrderChange(TransactionSort.DATE_ASC) }
                SortChip("Highest", sortOrder == TransactionSort.AMOUNT_DESC) { onSortOrderChange(TransactionSort.AMOUNT_DESC) }
                SortChip("Lowest", sortOrder == TransactionSort.AMOUNT_ASC) { onSortOrderChange(TransactionSort.AMOUNT_ASC) }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Filters", color = Color.Black, fontWeight = FontWeight.Bold)
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
                text = if (editingTransaction != null) "Edit Transaction" else "Record Transaction",
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
                                text = type.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    label = { Text("Amount ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Merchant / Payee
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Payee") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Item") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Tags
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Tags (comma separated, e.g. food, trip)") },
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
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (editingTransaction != null) {
                    IconButton(onClick = onDuplicate) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonExpense)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
