package com.finpulse.app.presentation.quickadd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.TransferBlue
import com.finpulse.app.core.model.CurrencyInfo
import com.finpulse.app.core.ui.CategoryIconBadge
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    uiState: QuickAddUiState,
    onDigitClick: (Char) -> Unit,
    onQuickAmountAdd: (Int) -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    onAccountSelected: (String) -> Unit,
    onDestinationAccountSelected: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onMerchantSelected: (String) -> Unit,
    onMerchantTextChange: (String) -> Unit,
    onDescriptionTextChange: (String) -> Unit,
    onDateChoiceSelected: (QuickAddDateChoice) -> Unit,
    onSave: (andAddAnother: Boolean, onSaved: (Transaction) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onTransactionSaved: (Transaction, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDetails by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // 1. Header: Type Selector & Account Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Switcher
                TypeSegmentedControl(
                    selectedType = uiState.selectedType,
                    onTypeSelected = onTypeSelected
                )

                // Account Dropdown Pill
                AccountPickerPill(
                    accounts = uiState.accounts,
                    selectedAccountId = uiState.selectedAccountId,
                    onAccountSelected = onAccountSelected
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Transfer Destination Account (if Transfer selected)
            if (uiState.selectedType == TransactionType.TRANSFER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Transfer to:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AccountPickerPill(
                        accounts = uiState.accounts.filter { it.id != uiState.selectedAccountId },
                        selectedAccountId = uiState.selectedDestinationAccountId,
                        onAccountSelected = onDestinationAccountSelected
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Amount Display
            val symbol = CurrencyInfo.findByCode(uiState.currencyCode).symbol
            val typeAccentColor by animateColorAsState(
                targetValue = when (uiState.selectedType) {
                    TransactionType.EXPENSE -> CrimsonExpense
                    TransactionType.INCOME -> EmeraldPrimary
                    TransactionType.TRANSFER -> TransferBlue
                    else -> EmeraldPrimary
                },
                animationSpec = tween(300),
                label = "TypeColor"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = symbol,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.amountInput,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.amountInput == "0") MaterialTheme.colorScheme.onSurfaceVariant else typeAccentColor
                    )
                }
            }

            // Quick increment chips: +5, +10, +20, +50
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf(5, 10, 20, 50, 100).forEach { inc ->
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable { onQuickAmountAdd(inc) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "+$inc",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Frequent Category Selector (1-tap)
            if (uiState.selectedType != TransactionType.TRANSFER) {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.frequentCategories.forEach { category ->
                        val isSelected = category.id == uiState.selectedCategoryId
                        Surface(
                            modifier = Modifier.clickable { onCategorySelected(category.id) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(category.colorHex).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(category.colorHex)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIconBadge(
                                    iconName = category.icon,
                                    colorHex = category.colorHex,
                                    size = 24.dp,
                                    iconSize = 14.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 5. Frequent Merchants (1-tap prediction)
            if (uiState.frequentMerchants.isNotEmpty() && uiState.selectedType == TransactionType.EXPENSE) {
                val merchantScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(merchantScroll),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.frequentMerchants.forEach { m ->
                        val isSelected = uiState.merchant.equals(m, ignoreCase = true)
                        Surface(
                            modifier = Modifier.clickable { onMerchantSelected(m) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary) else null
                        ) {
                            Text(
                                text = m,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 6. Date Selector Chips: [Today] [Yesterday] [Pick Date]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAddDateChoice.values().forEach { choice ->
                    val isSelected = uiState.dateChoice == choice
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDateChoiceSelected(choice) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = choice.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Expandable optional notes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDetails = !showDetails }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (showDetails) "Hide notes & tags" else "+ Add note, payee or tags",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldPrimary
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = showDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    OutlinedTextField(
                        value = uiState.merchant,
                        onValueChange = onMerchantTextChange,
                        label = { Text("Merchant / Payee") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = onDescriptionTextChange,
                        label = { Text("Note / Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Error display
            uiState.errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = err, color = CrimsonExpense, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7. Ultra-Responsive Numeric Keypad
            NumericKeypad(onDigitClick = onDigitClick)

            Spacer(modifier = Modifier.height(14.dp))

            // 8. Dual Action Buttons: [Save & Add Another] and [Save]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!uiState.isEditing) {
                    OutlinedButton(
                        onClick = {
                            onSave(true) { tx ->
                                onTransactionSaved(tx, true)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Another", color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = {
                        onSave(false) { tx ->
                            onTransactionSaved(tx, false)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = typeAccentColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isEditing) "Update" else "Save",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TypeSegmentedControl(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER).forEach { type ->
                val isSelected = selectedType == type
                val selColor = when (type) {
                    TransactionType.EXPENSE -> CrimsonExpense
                    TransactionType.INCOME -> EmeraldPrimary
                    TransactionType.TRANSFER -> TransferBlue
                    else -> EmeraldPrimary
                }
                Surface(
                    modifier = Modifier.clickable { onTypeSelected(type) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) selColor else Color.Transparent,
                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountPickerPill(
    accounts: List<Account>,
    selectedAccountId: String?,
    onAccountSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currentAccount = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentAccount?.name ?: "Account",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = { Text("${acc.name} (${acc.balance.formatted()})") },
                    onClick = {
                        onAccountSelected(acc.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun NumericKeypad(
    onDigitClick: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('.', '0', '⌫')
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { onDigitClick(key) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (key == '⌫') {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = key.toString(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
