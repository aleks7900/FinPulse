package md.alexlab.finpulse.presentation.search

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.AmberWarning
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.ui.TransactionItem
import md.alexlab.finpulse.domain.model.TransactionFilterParams
import md.alexlab.finpulse.domain.model.TransactionPreset
import md.alexlab.finpulse.domain.model.TransactionPresets
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    hideBalances: Boolean = false
) {
    var query by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<TransactionPreset?>(null) }

    val filterParams = remember(query, selectedPreset) {
        val base = selectedPreset?.params ?: TransactionFilterParams()
        base.copy(query = query)
    }

    val transactions by transactionRepository.filterTransactionsFlow(filterParams).collectAsState(initial = emptyList())
    val accounts by accountRepository.getAllAccountsFlow().collectAsState(initial = emptyList())
    val categories by categoryRepository.getAllCategoriesFlow().collectAsState(initial = emptyList())

    val accountMap = accounts.associateBy { it.id }
    val categoryMap = categories.associateBy { it.id }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.search_title),
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
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

            // Preset Quick Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All Chip
                val isAll = selectedPreset == null
                Surface(
                    modifier = Modifier.clickable { selectedPreset = null },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isAll) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (isAll) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = stringResource(R.string.tx_filter_all),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                TransactionPresets.ALL_PRESETS.forEach { preset ->
                    val isSelected = selectedPreset?.id == preset.id
                    val presetName = when (preset.id) {
                        TransactionPresets.THIS_MONTH.id -> stringResource(R.string.preset_this_month)
                        TransactionPresets.LAST_MONTH.id -> stringResource(R.string.preset_last_month)
                        TransactionPresets.UNCATEGORIZED.id -> stringResource(R.string.preset_uncategorized)
                        TransactionPresets.SUBSCRIPTIONS.id -> stringResource(R.string.preset_subscriptions)
                        TransactionPresets.LARGE_EXPENSES.id -> stringResource(R.string.preset_large_expenses)
                        TransactionPresets.TRANSFERS.id -> stringResource(R.string.preset_transfers)
                        else -> preset.name
                    }

                    Surface(
                        modifier = Modifier.clickable {
                            selectedPreset = if (isSelected) null else preset
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = presetName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (query.isBlank() && selectedPreset == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.search_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.search_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.search_results_count, transactions.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(transactions, key = { it.id }) { tx ->
                        val cat = categoryMap[tx.categoryId]
                        val acc = accountMap[tx.sourceAccountId]?.name ?: "Account"
                        TransactionItem(
                            transaction = tx,
                            category = cat,
                            accountName = acc,
                            hideBalances = hideBalances,
                            onClick = {}
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}
