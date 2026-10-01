package md.alexlab.finpulse.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.model.CurrencyInfo
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.Budget
import md.alexlab.finpulse.domain.model.BudgetPeriod
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.BudgetRepository
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun OnboardingScreen(
    userPreferencesDataStore: UserPreferencesDataStore,
    accountRepository: AccountRepository,
    budgetRepository: BudgetRepository,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }

    var selectedCurrency by remember { mutableStateOf("USD") }
    var accountName by remember { mutableStateOf("Primary Checking") }
    var startingBalanceText by remember { mutableStateOf("1500.00") }
    var monthlyBudgetText by remember { mutableStateOf("2000.00") }

    fun finishOnboarding() {
        scope.launch {
            // Save base currency
            userPreferencesDataStore.setBaseCurrency(selectedCurrency)

            // Create initial account
            val balVal = startingBalanceText.toDoubleOrNull() ?: 0.0
            val initialAcc = Account(
                id = UUID.randomUUID().toString(),
                name = accountName.ifBlank { "Primary Checking" },
                type = AccountType.BANK,
                balance = Money((balVal * 100).toLong(), selectedCurrency),
                availableBalance = Money((balVal * 100).toLong(), selectedCurrency)
            )
            accountRepository.saveAccount(initialAcc)

            // Create initial budget
            val budgetVal = monthlyBudgetText.toDoubleOrNull() ?: 0.0
            if (budgetVal > 0.0) {
                val (startMillis, endMillis) = md.alexlab.finpulse.core.model.TimePeriod.MONTH.toDateRange()
                val initialBudget = Budget(
                    id = UUID.randomUUID().toString(),
                    categoryId = "cat_food",
                    name = "Food & Dining",
                    limitAmount = Money((budgetVal * 100).toLong(), selectedCurrency),
                    periodType = BudgetPeriod.MONTHLY,
                    startDate = startMillis,
                    endDate = endMillis
                )
                budgetRepository.saveBudget(initialBudget)
            }

            // Mark onboarding completed
            userPreferencesDataStore.setOnboardingCompleted(true)
            onComplete()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            // Step Indicator Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.onboarding_step_format, step + 1),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                TextButton(onClick = { finishOnboarding() }) {
                    Text(stringResource(R.string.onboarding_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "OnboardingSteps"
            ) { currentStep ->
                when (currentStep) {
                    0 -> WelcomeStep()
                    1 -> CurrencyStep(
                        selected = selectedCurrency,
                        onSelect = { selectedCurrency = it }
                    )
                    2 -> AccountStep(
                        name = accountName,
                        onNameChange = { accountName = it },
                        balance = startingBalanceText,
                        onBalanceChange = { startingBalanceText = it },
                        currency = selectedCurrency
                    )
                    3 -> BudgetStep(
                        budget = monthlyBudgetText,
                        onBudgetChange = { monthlyBudgetText = it },
                        currency = selectedCurrency
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (step < 3) {
                        step += 1
                    } else {
                        finishOnboarding()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (step == 3) stringResource(R.string.onboarding_finish_btn) else stringResource(R.string.action_continue),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun WelcomeStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(EmeraldPrimary, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("FP", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.Black)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CurrencyStep(
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.onboarding_currency_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_currency_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CurrencyInfo.SUPPORTED_CURRENCIES) { curr ->
                val isSel = curr.code == selected
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(curr.code) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSel) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(curr.flag, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${curr.code} • ${curr.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        if (isSel) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountStep(
    name: String,
    onNameChange: (String) -> Unit,
    balance: String,
    onBalanceChange: (String) -> Unit,
    currency: String
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.onboarding_account_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_account_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.account_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = balance,
            onValueChange = onBalanceChange,
            label = { Text("${stringResource(R.string.account_starting_balance_hint)} ($currency)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BudgetStep(
    budget: String,
    onBudgetChange: (String) -> Unit,
    currency: String
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.onboarding_budget_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_budget_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = budget,
            onValueChange = onBudgetChange,
            label = { Text("${stringResource(R.string.budget_limit_hint)} ($currency)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
