package com.finpulse.app.presentation.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finpulse.app.R
import com.finpulse.app.core.designsystem.AmberWarning
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.DebtOrange
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.InvestmentPurple
import com.finpulse.app.core.designsystem.SapphireAccent
import com.finpulse.app.core.designsystem.TealSavings
import com.finpulse.app.core.ui.getCategoryDisplayName
import com.finpulse.app.core.ui.getLocalizedName
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.ReviewInboxItem
import com.finpulse.app.domain.model.ReviewInboxSummary
import com.finpulse.app.domain.model.ReviewItemPriority
import com.finpulse.app.domain.model.ReviewItemType
import com.finpulse.app.domain.model.SafeBulkSuggestion
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewInboxScreen(
    uiState: ReviewInboxUiState,
    onNavigateBack: () -> Unit,
    onTabSelected: (ReviewInboxTab) -> Unit,
    onToggleSelectMode: () -> Unit,
    onToggleItemSelection: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onConfirmItem: (ReviewInboxItem) -> Unit,
    onOpenCategoryPicker: (ReviewInboxItem) -> Unit,
    onCategorizeItem: (ReviewInboxItem, String) -> Unit,
    onOpenAccountPicker: (ReviewInboxItem) -> Unit,
    onChangeAccount: (ReviewInboxItem, String) -> Unit,
    onOpenEditMerchant: (ReviewInboxItem) -> Unit,
    onSaveMerchant: (ReviewInboxItem, String) -> Unit,
    onOpenResolveDuplicate: (ReviewInboxItem) -> Unit,
    onResolveDuplicateKeepPrimary: (ReviewInboxItem) -> Unit,
    onResolveDuplicateKeepCandidate: (ReviewInboxItem) -> Unit,
    onResolveDuplicateDismiss: (ReviewInboxItem) -> Unit,
    onOpenPayBill: (ReviewInboxItem) -> Unit,
    onConfirmPayBill: (ReviewInboxItem, Long?, String?) -> Unit,
    onDismissItem: (ReviewInboxItem) -> Unit,
    onDismissAllWarnings: () -> Unit,
    onApplySafeBulk: (SafeBulkSuggestion) -> Unit,
    onOpenBulkCategoryPicker: () -> Unit,
    onConfirmBulkCategorize: (String) -> Unit,
    onDismissDialogs: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.review_inbox_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.summary.totalCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (uiState.summary.overdueBillsCount > 0 || uiState.summary.failedRecurringCount > 0) CrimsonExpense else AmberWarning
                            ) {
                                Text(
                                    text = uiState.summary.totalCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleSelectMode) {
                        Icon(
                            imageVector = if (uiState.isSelectionMode) Icons.Default.Close else Icons.Default.DoneAll,
                            contentDescription = stringResource(if (uiState.isSelectionMode) R.string.review_inbox_cancel_selection else R.string.review_inbox_multiselect),
                            tint = if (uiState.isSelectionMode) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.review_inbox_more_actions)
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.review_inbox_dismiss_all_warnings)) },
                            leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null) },
                            onClick = {
                                onDismissAllWarnings()
                                showOverflowMenu = false
                            }
                        )
                        if (uiState.isSelectionMode) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.review_inbox_select_all)) },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                onClick = {
                                    onSelectAll(true)
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.review_inbox_deselect_all)) },
                                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                                onClick = {
                                    onSelectAll(false)
                                    showOverflowMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = uiState.isSelectionMode && uiState.selectedItemIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.review_inbox_selected_count, uiState.selectedItemIds.size),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onSelectAll(false) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.review_inbox_clear_selection), style = MaterialTheme.typography.labelMedium)
                            }
                            Button(
                                onClick = onOpenBulkCategoryPicker,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.review_inbox_bulk_categorize), color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Tabs
            ReviewInboxTabRow(
                selectedTab = uiState.selectedTab,
                summary = uiState.summary,
                onTabSelected = onTabSelected
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Safe Bulk Categorization Prompt
            if (uiState.summary.safeBulkSuggestions.isNotEmpty() &&
                (uiState.selectedTab == ReviewInboxTab.ALL || uiState.selectedTab == ReviewInboxTab.UNCATEGORIZED)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.summary.safeBulkSuggestions.forEach { suggestion ->
                        SafeBulkBannerCard(
                            suggestion = suggestion,
                            onApply = { onApplySafeBulk(suggestion) }
                        )
                    }
                }
            }

            // Items List or Empty State
            if (uiState.filteredItems.isEmpty()) {
                EmptyInboxState(
                    selectedTab = uiState.selectedTab,
                    onBackToDashboard = onNavigateBack,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredItems, key = { it.id }) { item ->
                        val isSelected = uiState.selectedItemIds.contains(item.id)
                        ReviewItemCard(
                            item = item,
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = isSelected,
                            onToggleSelection = { onToggleItemSelection(item.id) },
                            onConfirm = { onConfirmItem(item) },
                            onCategorize = { onOpenCategoryPicker(item) },
                            onChangeAccount = { onOpenAccountPicker(item) },
                            onEditMerchant = { onOpenEditMerchant(item) },
                            onResolveDuplicate = { onOpenResolveDuplicate(item) },
                            onPayBill = { onOpenPayBill(item) },
                            onDismiss = { onDismissItem(item) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Dialogs
    if (uiState.isCategoryPickerDialogVisible && uiState.categorizingItem != null) {
        CategoryPickerDialog(
            categories = uiState.categories,
            currentItem = uiState.categorizingItem,
            onCategorySelected = { catId -> onCategorizeItem(uiState.categorizingItem, catId) },
            onDismiss = onDismissDialogs
        )
    }

    if (uiState.isAccountPickerDialogVisible && uiState.changingAccountItem != null) {
        AccountPickerDialog(
            accounts = uiState.accounts,
            currentItem = uiState.changingAccountItem,
            onAccountSelected = { accId -> onChangeAccount(uiState.changingAccountItem, accId) },
            onDismiss = onDismissDialogs
        )
    }

    if (uiState.isEditMerchantDialogVisible && uiState.editingMerchantItem != null) {
        EditMerchantDialog(
            item = uiState.editingMerchantItem,
            onSave = { newMerchant -> onSaveMerchant(uiState.editingMerchantItem, newMerchant) },
            onDismiss = onDismissDialogs
        )
    }

    if (uiState.isResolveDuplicateDialogVisible && uiState.resolvingDuplicateItem != null) {
        ResolveDuplicateDialog(
            item = uiState.resolvingDuplicateItem,
            onKeepPrimary = { onResolveDuplicateKeepPrimary(uiState.resolvingDuplicateItem) },
            onKeepCandidate = { onResolveDuplicateKeepCandidate(uiState.resolvingDuplicateItem) },
            onNotDuplicate = { onResolveDuplicateDismiss(uiState.resolvingDuplicateItem) },
            onDismiss = onDismissDialogs
        )
    }

    if (uiState.isPayBillDialogVisible && uiState.payingBillItem != null) {
        PayBillDialog(
            item = uiState.payingBillItem,
            accounts = uiState.accounts,
            onConfirmPay = { amountMinor, accId -> onConfirmPayBill(uiState.payingBillItem, amountMinor, accId) },
            onDismiss = onDismissDialogs
        )
    }

    if (uiState.isBulkCategoryPickerVisible) {
        BulkCategoryPickerDialog(
            categories = uiState.categories,
            selectedCount = uiState.selectedItemIds.size,
            onCategorySelected = onConfirmBulkCategorize,
            onDismiss = onDismissDialogs
        )
    }
}

@Composable
fun ReviewInboxTabRow(
    selectedTab: ReviewInboxTab,
    summary: ReviewInboxSummary,
    onTabSelected: (ReviewInboxTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReviewInboxTab.entries.forEach { tab ->
            val count = when (tab) {
                ReviewInboxTab.ALL -> summary.totalCount
                ReviewInboxTab.UNCATEGORIZED -> summary.uncategorizedCount + summary.importedCount
                ReviewInboxTab.DUPLICATES -> summary.duplicatesCount
                ReviewInboxTab.BILLS -> summary.overdueBillsCount + summary.failedRecurringCount
                ReviewInboxTab.WARNINGS -> summary.unusualAmountsCount + summary.missingMerchantCount
            }

            val isSelected = tab == selectedTab
            Surface(
                modifier = Modifier.clickable { onTabSelected(tab) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tab.getLocalizedName(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    if (count > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color.Black.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SafeBulkBannerCard(
    suggestion: SafeBulkSuggestion,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = EmeraldPrimary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.review_inbox_safe_bulk_title),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                val localizedCatName = getCategoryDisplayName(suggestion.suggestedCategoryId, suggestion.suggestedCategoryName)
                Text(
                    text = stringResource(R.string.review_inbox_safe_bulk_desc, suggestion.count, suggestion.merchant, localizedCatName),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(stringResource(R.string.review_inbox_categorize_all, suggestion.count), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ReviewItemCard(
    item: ReviewInboxItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onConfirm: () -> Unit,
    onCategorize: () -> Unit,
    onChangeAccount: () -> Unit,
    onEditMerchant: () -> Unit,
    onResolveDuplicate: () -> Unit,
    onPayBill: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val (badgeColor, badgeIcon) = when (item.type) {
        ReviewItemType.OVERDUE_BILL, ReviewItemType.FAILED_RECURRING -> CrimsonExpense to Icons.Default.Warning
        ReviewItemType.SUSPECTED_DUPLICATE -> AmberWarning to Icons.Default.ContentCopy
        ReviewItemType.UNUSUAL_AMOUNT -> DebtOrange to Icons.Default.ErrorOutline
        ReviewItemType.MISSING_MERCHANT -> SapphireAccent to Icons.Default.Edit
        ReviewItemType.UNCATEGORIZED -> EmeraldPrimary to Icons.Default.Category
        ReviewItemType.IMPORTED_CONFIRMATION -> TealSavings to Icons.Default.ReceiptLong
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelectionMode) Modifier.clickable(onClick = onToggleSelection) else Modifier
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Type Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.type.getLocalizedName(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Amount
                Text(
                    text = item.amount.formatted(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.amount.amountMinor == 0L) AmberWarning else MaterialTheme.colorScheme.onSurface
                )

                // Overflow Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.review_inbox_actions),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (item.transaction != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.review_inbox_change_account)) },
                                leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onChangeAccount()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.review_inbox_edit_merchant)) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEditMerchant()
                                }
                            )
                        }
                        if (item.isDismissible) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.review_inbox_dismiss_warning)) },
                                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Subtitle
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Reason / Highlight Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Duplicate Comparison Preview
            if (item.type == ReviewItemType.SUSPECTED_DUPLICATE && item.duplicateCandidate != null && item.transaction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                DuplicateComparisonBox(
                    txA = item.transaction,
                    txB = item.duplicateCandidate
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (item.type) {
                    ReviewItemType.UNCATEGORIZED, ReviewItemType.IMPORTED_CONFIRMATION -> {
                        if (item.suggestedCategoryId != null) {
                            Button(
                                onClick = onConfirm,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                val targetCategoryName = getCategoryDisplayName(item.suggestedCategoryId, item.suggestedCategoryName ?: stringResource(R.string.review_inbox_confirm_category_fallback))
                                Text(
                                    text = stringResource(R.string.review_inbox_confirm_category, targetCategoryName),
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onCategorize,
                            shape = RoundedCornerShape(12.dp),
                            modifier = if (item.suggestedCategoryId != null) Modifier else Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.review_inbox_categorize))
                        }
                    }
                    ReviewItemType.SUSPECTED_DUPLICATE -> {
                        Button(
                            onClick = onResolveDuplicate,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.review_inbox_resolve_duplicate), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.review_inbox_not_duplicate))
                        }
                    }
                    ReviewItemType.OVERDUE_BILL -> {
                        Button(
                            onClick = onPayBill,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonExpense),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.review_inbox_mark_bill_paid), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.review_inbox_dismiss))
                        }
                    }
                    ReviewItemType.FAILED_RECURRING -> {
                        Button(
                            onClick = onPayBill,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonExpense),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.review_inbox_pay_now), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.review_inbox_dismiss))
                        }
                    }
                    ReviewItemType.MISSING_MERCHANT -> {
                        Button(
                            onClick = onEditMerchant,
                            colors = ButtonDefaults.buttonColors(containerColor = SapphireAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.review_inbox_add_merchant), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onConfirm,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.review_inbox_keep_as_is))
                        }
                    }
                    ReviewItemType.UNUSUAL_AMOUNT -> {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.review_inbox_looks_correct))
                        }
                        OutlinedButton(
                            onClick = onCategorize,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.review_inbox_edit))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicateComparisonBox(
    txA: com.finpulse.app.domain.model.Transaction,
    txB: com.finpulse.app.domain.model.Transaction,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val dateA = Instant.ofEpochMilli(txA.timestamp).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
    val dateB = Instant.ofEpochMilli(txB.timestamp).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AmberWarning.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = stringResource(R.string.review_inbox_comparing_twin),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AmberWarning
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.review_inbox_twin_original, txA.id.takeLast(6)), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("$dateA • ${txA.merchant ?: txA.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.review_inbox_twin_candidate, txB.id.takeLast(6)), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("$dateB • ${txB.merchant ?: txB.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun EmptyInboxState(
    selectedTab: ReviewInboxTab,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (selectedTab == ReviewInboxTab.ALL) stringResource(R.string.review_inbox_all_caught_up) else stringResource(R.string.review_inbox_no_items_tab, selectedTab.getLocalizedName()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.review_inbox_empty_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onBackToDashboard,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.review_inbox_return_dashboard), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// Interactive Dialogs
// -------------------------------------------------------------

@Composable
fun CategoryPickerDialog(
    categories: List<Category>,
    currentItem: ReviewInboxItem,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = categories.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.review_inbox_assign_category), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.review_inbox_search_categories)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered) { cat ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategorySelected(cat.id) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (cat.id == currentItem.suggestedCategoryId) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(cat.colorHex))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = getCategoryDisplayName(cat.id, cat.name),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (cat.id == currentItem.suggestedCategoryId) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun AccountPickerDialog(
    accounts: List<Account>,
    currentItem: ReviewInboxItem,
    onAccountSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.review_inbox_reassign_account), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.filter { !it.isArchived }.forEach { acc ->
                    val isCurrent = acc.id == currentItem.transaction?.sourceAccountId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAccountSelected(acc.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isCurrent) BorderStroke(1.5.dp, EmeraldPrimary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(acc.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.review_inbox_account_balance, acc.balance.formatted()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isCurrent) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun EditMerchantDialog(
    item: ReviewInboxItem,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var merchantText by remember { mutableStateOf(item.transaction?.merchant.orEmpty().ifEmpty { item.transaction?.description.orEmpty() }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.review_inbox_edit_merchant_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.review_inbox_edit_merchant_prompt, item.amount.formatted()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text(stringResource(R.string.review_inbox_merchant_payee)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(merchantText) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.action_save), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun ResolveDuplicateDialog(
    item: ReviewInboxItem,
    onKeepPrimary: () -> Unit,
    onKeepCandidate: () -> Unit,
    onNotDuplicate: () -> Unit,
    onDismiss: () -> Unit
) {
    val txA = item.transaction ?: return
    val txB = item.duplicateCandidate ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.review_inbox_resolve_duplicate_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.review_inbox_duplicate_prompt, txA.amount.formatted()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onKeepPrimary),
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldPrimary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.review_inbox_keep_original), fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        Text(
                            text = "${stringResource(R.string.review_inbox_original)}: ${txA.merchant ?: txA.description} (ID: ...${txA.id.takeLast(6)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Option 2
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onKeepCandidate),
                    shape = RoundedCornerShape(12.dp),
                    color = SapphireAccent.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, SapphireAccent.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.review_inbox_keep_candidate), fontWeight = FontWeight.Bold, color = SapphireAccent)
                        Text(
                            text = "${stringResource(R.string.review_inbox_candidate)}: ${txB.merchant ?: txB.description} (ID: ...${txB.id.takeLast(6)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Option 3
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNotDuplicate),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.review_inbox_both_legitimate), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = stringResource(R.string.review_inbox_empty_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun PayBillDialog(
    item: ReviewInboxItem,
    accounts: List<Account>,
    onConfirmPay: (Long?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(item.recurringOccurrence?.accountId ?: accounts.firstOrNull()?.id.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.review_inbox_confirm_payment), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.review_inbox_pay_bill_prompt, item.amount.formatted(), item.title),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.review_inbox_pay_bill_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.review_inbox_paying_from), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                accounts.filter { !it.isArchived }.forEach { acc ->
                    val isSelected = acc.id == selectedAccountId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAccountId = acc.id },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, EmeraldPrimary) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(acc.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text(acc.balance.formatted(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPay(null, selectedAccountId) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.review_inbox_record_payment), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun BulkCategoryPickerDialog(
    categories: List<Category>,
    selectedCount: Int,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = categories.filter { it.name.contains(searchQuery, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.review_inbox_bulk_categorize_title, selectedCount), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.review_inbox_search_categories)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered) { cat ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategorySelected(cat.id) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(cat.colorHex))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(getCategoryDisplayName(cat.id, cat.name), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
