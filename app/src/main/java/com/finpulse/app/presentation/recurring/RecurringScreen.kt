package com.finpulse.app.presentation.recurring

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.PurpleAccent
import com.finpulse.app.core.ui.CategoryIconBadge
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.PaymentFrequency
import com.finpulse.app.domain.model.RecurringTransaction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    uiState: RecurringUiState,
    onNavigateBack: () -> Unit,
    onShowAddEditDialog: (Boolean, RecurringTransaction?) -> Unit,
    onSaveRecurring: (id: String?, title: String, amountMinor: Long, accountId: String, categoryId: String, frequency: PaymentFrequency, nextDueDate: Long, isSubscription: Boolean) -> Unit,
    onToggleActive: (RecurringTransaction) -> Unit,
    onDeleteRecurring: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryMap = uiState.categories.associateBy { it.id }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Subscriptions & Bills",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Recurring")
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
            // Cost Overview Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "ACTIVE RECURRING COMMITMENTS",
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
                            Column {
                                Text(
                                    text = if (uiState.hideBalances) "••••••" else uiState.totalMonthlyCost.formatted(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Monthly cost", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (uiState.hideBalances) "••••••" else uiState.totalAnnualCost.formatted(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleAccent
                                )
                                Text("Annual cost", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Recurring Items
            if (uiState.recurringList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No Subscriptions Tracked",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Track Netflix, Spotify, gym, rent, and recurring utilities.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.recurringList, key = { it.id }) { item ->
                    val cat = categoryMap[item.categoryId]
                    RecurringItemCard(
                        recurring = item,
                        category = cat,
                        hideBalances = uiState.hideBalances,
                        onClick = { onShowAddEditDialog(true, item) },
                        onToggle = { onToggleActive(item) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Add / Edit Dialog
        if (uiState.isAddEditDialogVisible) {
            AddEditRecurringDialog(
                editingRecurring = uiState.editingRecurring,
                accounts = uiState.accounts,
                categories = uiState.categories,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { onShowAddEditDialog(false, null) },
                onSave = onSaveRecurring,
                onDelete = { uiState.editingRecurring?.let { onDeleteRecurring(it.id) } }
            )
        }
    }
}

@Composable
fun RecurringItemCard(
    recurring: RecurringTransaction,
    category: Category?,
    hideBalances: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nextDateFormatted = Instant.ofEpochMilli(recurring.nextDueDate)
        .atZone(ZoneId.systemDefault())
        .format(DateFormatter)

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
            CategoryIconBadge(
                iconName = category?.icon ?: "subscriptions",
                colorHex = category?.colorHex ?: 0xFF9C27B0
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recurring.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${recurring.frequency.displayName} • Next: $nextDateFormatted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (hideBalances) "••••••" else recurring.amount.formatted(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Switch(
                    checked = recurring.isActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                )
            }
        }
    }
}

@Composable
fun AddEditRecurringDialog(
    editingRecurring: RecurringTransaction?,
    accounts: List<Account>,
    categories: List<Category>,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (id: String?, title: String, amountMinor: Long, accountId: String, categoryId: String, frequency: PaymentFrequency, nextDueDate: Long, isSubscription: Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(editingRecurring?.title ?: "") }
    var amountText by remember { mutableStateOf(editingRecurring?.amount?.amountBigDecimal?.toPlainString() ?: "") }
    var accountId by remember { mutableStateOf(editingRecurring?.accountId ?: accounts.firstOrNull()?.id ?: "") }
    var categoryId by remember { mutableStateOf(editingRecurring?.categoryId ?: categories.firstOrNull()?.id ?: "") }
    var frequency by remember { mutableStateOf(editingRecurring?.frequency ?: PaymentFrequency.MONTHLY) }
    var isSubscription by remember { mutableStateOf(editingRecurring?.isSubscription ?: true) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editingRecurring != null) "Edit Recurring Item" else "New Recurring Payment", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; isError = false },
                    label = { Text("Title (e.g. Netflix, Rent)") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Frequency Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(PaymentFrequency.MONTHLY, PaymentFrequency.YEARLY, PaymentFrequency.WEEKLY).forEach { f ->
                        val isSel = frequency == f
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { frequency = f },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface
                        ) {
                            Text(
                                text = f.displayName,
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
                    val amountVal = amountText.toDoubleOrNull()
                    if (title.isBlank() || amountVal == null || amountVal <= 0.0) {
                        isError = true
                        return@Button
                    }
                    val amountMinor = (amountVal * 100).toLong()
                    val nextDue = editingRecurring?.nextDueDate ?: (System.currentTimeMillis() + (frequency.approxDays * 86_400_000L))
                    onSave(editingRecurring?.id, title, amountMinor, accountId, categoryId, frequency, nextDue, isSubscription)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (editingRecurring != null) {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonExpense)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
