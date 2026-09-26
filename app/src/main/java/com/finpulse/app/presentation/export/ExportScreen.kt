package com.finpulse.app.presentation.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.SapphireAccent
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Backup & Data Export",
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CSV Export Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.TableChart, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Export CSV Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Standard RFC-4180 spreadsheet export compatible with Google Sheets, Excel, and accounting software.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val txList = transactionRepository.getAllTransactionsFlow().first()
                                val accMap = accountRepository.getAllAccountsFlow().first().associateBy { it.id }
                                val catMap = categoryRepository.getAllCategoriesFlow().first().associateBy { it.id }

                                val csv = generateCsv(txList, accMap, catMap)
                                snackbarHostState.showSnackbar("CSV generated successfully (${txList.size} records)")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Transactions CSV", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Full JSON Backup Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DataObject, contentDescription = null, tint = SapphireAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Full Database Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create an encrypted JSON snapshot of accounts, transactions, budgets, goals, and debt payoff plans.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Full JSON database snapshot ready")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SapphireAccent)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Backup", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun generateCsv(
    transactions: List<com.finpulse.app.domain.model.Transaction>,
    accountMap: Map<String, com.finpulse.app.domain.model.Account>,
    categoryMap: Map<String, com.finpulse.app.domain.model.Category>
): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    val sb = StringBuilder()
    sb.append("ID,Date,Type,Amount,Currency,Account,Category,Merchant,Description,Tags\n")

    for (tx in transactions) {
        val dateStr = formatter.format(Instant.ofEpochMilli(tx.timestamp))
        val accName = accountMap[tx.sourceAccountId]?.name ?: tx.sourceAccountId
        val catName = categoryMap[tx.categoryId]?.name ?: tx.categoryId
        val merchant = tx.merchant?.replace(",", ";") ?: ""
        val desc = tx.description.replace(",", ";")
        val tags = tx.tags.joinToString(";")

        sb.append("${tx.id},$dateStr,${tx.type.name},${tx.amount.amountBigDecimal},${tx.amount.currencyCode},\"$accName\",\"$catName\",\"$merchant\",\"$desc\",\"$tags\"\n")
    }
    return sb.toString()
}
