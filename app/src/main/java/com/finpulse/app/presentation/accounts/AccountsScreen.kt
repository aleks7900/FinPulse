package com.finpulse.app.presentation.accounts

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Wallet
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finpulse.app.R
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.TransferBlue
import com.finpulse.app.core.ui.getLocalizedName
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    uiState: AccountsUiState,
    onNavigateBack: () -> Unit,
    onShowAddEditDialog: (Boolean, Account?) -> Unit,
    onShowTransferDialog: (Boolean) -> Unit,
    onSaveAccount: (id: String?, name: String, type: AccountType, balanceMinor: Long, currencyCode: String?, institution: String?, colorHex: Long) -> Unit,
    onTransferFunds: (sourceId: String, destId: String, amountMinor: Long, destinationAmountMinor: Long?, exchangeRate: Double?, note: String) -> Unit,
    onArchiveAccount: (String, Boolean) -> Unit,
    onDeleteAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.accounts_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { onShowTransferDialog(true) }) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.accounts_transfer_title), tint = EmeraldPrimary)
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
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.account_add_dialog_title))
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
            // Aggregate Net Worth Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.dashboard_total_balance).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.hideBalances) "••••••••" else uiState.totalBalance.formatted(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.accounts_active_tracked, uiState.accounts.count { !it.isArchived }),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Accounts List
            items(uiState.accounts, key = { it.id }) { account ->
                AccountCard(
                    account = account,
                    hideBalances = uiState.hideBalances,
                    onClick = { onShowAddEditDialog(true, account) },
                    onArchive = { onArchiveAccount(account.id, !account.isArchived) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Add / Edit Account Dialog
        if (uiState.isAddEditDialogVisible) {
            AddEditAccountDialog(
                editingAccount = uiState.editingAccount,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { onShowAddEditDialog(false, null) },
                onSave = onSaveAccount,
                onDelete = { uiState.editingAccount?.let { onDeleteAccount(it.id) } }
            )
        }

        // Transfer Dialog
        if (uiState.isTransferDialogVisible) {
            TransferFundsDialog(
                accounts = uiState.accounts.filter { !it.isArchived },
                baseCurrency = uiState.baseCurrency,
                onDismiss = { onShowTransferDialog(false) },
                onTransfer = onTransferFunds
            )
        }
    }
}

@Composable
fun AccountCard(
    account: Account,
    hideBalances: Boolean,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (account.type) {
        AccountType.CASH -> Icons.Default.Payments
        AccountType.BANK -> Icons.Default.AccountBalance
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
        AccountType.SAVINGS -> Icons.Default.Savings
        AccountType.WALLET -> Icons.Default.Wallet
        else -> Icons.Default.AccountBalance
    }

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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(account.colorHex).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(account.colorHex),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                val typeName = account.type.getLocalizedName()
                val archivedSuffix = if (account.isArchived) " • " + stringResource(R.string.account_archived_label) else ""
                Text(
                    text = "${account.institution ?: typeName}$archivedSuffix",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (hideBalances) "••••••" else account.balance.formatted(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (account.balance.isNegative) CrimsonExpense else MaterialTheme.colorScheme.onSurface
                )
                account.creditLimit?.let { limit ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.account_limit_label, limit.formatted()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountDialog(
    editingAccount: Account?,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, type: AccountType, balanceMinor: Long, currencyCode: String?, institution: String?, colorHex: Long) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(editingAccount?.name ?: "") }
    var selectedType by remember { mutableStateOf(editingAccount?.type ?: AccountType.BANK) }
    var selectedCurrency by remember { mutableStateOf(editingAccount?.balance?.currencyCode ?: baseCurrency) }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    var balanceText by remember { mutableStateOf(editingAccount?.balance?.amountBigDecimal?.toPlainString() ?: "0.00") }
    var institution by remember { mutableStateOf(editingAccount?.institution ?: "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingAccount != null) stringResource(R.string.account_edit_dialog_title) else stringResource(R.string.account_add_dialog_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; isError = false },
                    label = { Text(stringResource(R.string.account_name_hint)) },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Currency selector dropdown
                ExposedDropdownMenuBox(
                    expanded = currencyDropdownExpanded,
                    onExpandedChange = { currencyDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "$selectedCurrency (${com.finpulse.app.core.model.CurrencyConfig.getSymbol(selectedCurrency)})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyDropdownExpanded,
                        onDismissRequest = { currencyDropdownExpanded = false }
                    ) {
                        com.finpulse.app.core.model.CurrencyConfig.supportedCurrencyCodes.forEach { code ->
                            DropdownMenuItem(
                                text = { Text("$code — ${com.finpulse.app.core.model.CurrencyConfig.getName(code)}") },
                                onClick = {
                                    selectedCurrency = code
                                    currencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text(stringResource(R.string.account_institution_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("${stringResource(R.string.account_starting_balance_hint)} ($selectedCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                        return@Button
                    }
                    val balanceVal = balanceText.toDoubleOrNull() ?: 0.0
                    val balanceMinor = com.finpulse.app.core.model.CurrencyConfig.fromMajor(balanceVal, selectedCurrency).amountMinor
                    val colorHex = when (selectedType) {
                        AccountType.SAVINGS -> 0xFF009688
                        AccountType.CREDIT_CARD -> 0xFFFF5722
                        AccountType.INVESTMENT -> 0xFF673AB7
                        else -> 0xFF2196F3
                    }
                    onSave(editingAccount?.id, name, selectedType, balanceMinor, selectedCurrency, institution, colorHex)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.action_save), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (editingAccount != null) {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = CrimsonExpense)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferFundsDialog(
    accounts: List<Account>,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onTransfer: (sourceId: String, destId: String, amountMinor: Long, destinationAmountMinor: Long?, exchangeRate: Double?, note: String) -> Unit
) {
    if (accounts.size < 2) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.accounts_transfer_cannot)) },
            text = { Text(stringResource(R.string.accounts_transfer_cannot_desc)) },
            confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } }
        )
        return
    }

    var sourceId by remember { mutableStateOf(accounts[0].id) }
    var destId by remember { mutableStateOf(accounts[1].id) }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }
    var destDropdownExpanded by remember { mutableStateOf(false) }

    val sourceAccount = remember(sourceId, accounts) { accounts.find { it.id == sourceId } ?: accounts[0] }
    val destAccount = remember(destId, accounts) { accounts.find { it.id == destId } ?: accounts[1] }

    val isCrossCurrency = sourceAccount.balance.currencyCode != destAccount.balance.currencyCode

    var amountText by remember { mutableStateOf("") }
    var customDestAmountText by remember { mutableStateOf("") }
    var isCustomRateEnabled by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.accounts_transfer_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Source Account Dropdown
                ExposedDropdownMenuBox(
                    expanded = sourceDropdownExpanded,
                    onExpandedChange = { sourceDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "${sourceAccount.name} (${sourceAccount.balance.formatted()})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = sourceDropdownExpanded,
                        onDismissRequest = { sourceDropdownExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} — ${acc.balance.formatted()}") },
                                onClick = {
                                    sourceId = acc.id
                                    if (destId == acc.id) {
                                        val other = accounts.find { it.id != acc.id }
                                        if (other != null) destId = other.id
                                    }
                                    sourceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Destination Account Dropdown
                ExposedDropdownMenuBox(
                    expanded = destDropdownExpanded,
                    onExpandedChange = { destDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "${destAccount.name} (${destAccount.balance.formatted()})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = destDropdownExpanded,
                        onDismissRequest = { destDropdownExpanded = false }
                    ) {
                        accounts.filter { it.id != sourceId }.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} — ${acc.balance.formatted()}") },
                                onClick = {
                                    destId = acc.id
                                    destDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Amount in Source Currency
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; isError = false },
                    label = { Text("${stringResource(R.string.tx_amount)} (${sourceAccount.balance.currencyCode})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Cross-currency transfer options: allows bank conversion differences
                if (isCrossCurrency) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Cross-Currency Transfer (${sourceAccount.balance.currencyCode} → ${destAccount.balance.currencyCode})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Optionally specify the exact destination amount received after bank fee or conversion spread.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customDestAmountText,
                                onValueChange = { customDestAmountText = it },
                                label = { Text("Received Amount (${destAccount.balance.currencyCode}) [Optional]") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.quick_add_note_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountText.toDoubleOrNull()
                    if (amountVal == null || amountVal <= 0.0 || sourceId == destId) {
                        isError = true
                        return@Button
                    }
                    val amountMinor = com.finpulse.app.core.model.CurrencyConfig.fromMajor(
                        amountVal,
                        sourceAccount.balance.currencyCode
                    ).amountMinor

                    var destAmountMinor: Long? = null
                    var exchangeRate: Double? = null

                    if (isCrossCurrency) {
                        val customDestVal = customDestAmountText.toDoubleOrNull()
                        if (customDestVal != null && customDestVal > 0.0) {
                            destAmountMinor = com.finpulse.app.core.model.CurrencyConfig.fromMajor(
                                customDestVal,
                                destAccount.balance.currencyCode
                            ).amountMinor
                            exchangeRate = customDestVal / amountVal
                        }
                    }

                    onTransfer(sourceId, destId, amountMinor, destAmountMinor, exchangeRate, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.accounts_transfer_action), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
