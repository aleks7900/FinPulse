package com.finpulse.app.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.finpulse.app.di.AppContainer
import com.finpulse.app.presentation.accounts.AccountsScreen
import com.finpulse.app.presentation.accounts.AccountsViewModel
import com.finpulse.app.presentation.analytics.AnalyticsScreen
import com.finpulse.app.presentation.analytics.AnalyticsViewModel
import com.finpulse.app.presentation.budgets.BudgetsScreen
import com.finpulse.app.presentation.budgets.BudgetsViewModel
import com.finpulse.app.presentation.categories.CategoriesScreen
import com.finpulse.app.presentation.dashboard.DashboardScreen
import com.finpulse.app.presentation.dashboard.DashboardViewModel
import com.finpulse.app.presentation.debt.DebtScreen
import com.finpulse.app.presentation.debt.DebtViewModel
import com.finpulse.app.presentation.export.ExportScreen
import com.finpulse.app.presentation.goals.GoalsScreen
import com.finpulse.app.presentation.goals.GoalsViewModel
import com.finpulse.app.presentation.insights.InsightsScreen
import com.finpulse.app.presentation.investments.InvestmentsScreen
import com.finpulse.app.presentation.investments.InvestmentsViewModel
import com.finpulse.app.presentation.more.MoreHubScreen
import com.finpulse.app.presentation.onboarding.OnboardingScreen
import com.finpulse.app.presentation.recurring.RecurringScreen
import com.finpulse.app.presentation.recurring.RecurringViewModel
import com.finpulse.app.presentation.search.SearchScreen
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.presentation.quickadd.QuickAddBottomSheet
import com.finpulse.app.presentation.quickadd.QuickAddViewModel
import com.finpulse.app.presentation.security.SecurityScreen
import com.finpulse.app.presentation.transactions.TransactionsScreen
import com.finpulse.app.presentation.transactions.TransactionsViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun FinPulseApp(
    container: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openQuickAddTrigger: StateFlow<Boolean>? = null,
    onQuickAddHandled: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val userPrefs by container.userPreferencesDataStore.userPreferencesFlow.collectAsState(
        initial = com.finpulse.app.core.datastore.UserPreferences()
    )

    val isTopLevelRoute = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Transactions.route,
        Screen.Budgets.route,
        Screen.Analytics.route,
        Screen.More.route
    )

    val startDestination = if (userPrefs.isOnboardingCompleted) Screen.Dashboard.route else Screen.Onboarding.route

    val quickAddViewModel: QuickAddViewModel = viewModel {
        QuickAddViewModel(
            createTransactionUseCase = container.createTransactionUseCase,
            suggestionsUseCase = container.getQuickAddSuggestionsUseCase,
            accountRepository = container.accountRepository,
            categoryRepository = container.categoryRepository,
            userPreferencesDataStore = container.userPreferencesDataStore
        )
    }
    val quickAddState by quickAddViewModel.uiState.collectAsState()
    var showQuickAddSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    openQuickAddTrigger?.let { triggerFlow ->
        val shouldOpen by triggerFlow.collectAsState()
        LaunchedEffect(shouldOpen) {
            if (shouldOpen) {
                quickAddViewModel.reset()
                showQuickAddSheet = true
                onQuickAddHandled()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (isTopLevelRoute) {
                FloatingActionButton(
                    onClick = {
                        quickAddViewModel.reset()
                        showQuickAddSheet = true
                    },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick Add Transaction"
                    )
                }
            }
        },
        bottomBar = {
            if (isTopLevelRoute) {
                FinPulseBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Onboarding
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    userPreferencesDataStore = container.userPreferencesDataStore,
                    accountRepository = container.accountRepository,
                    budgetRepository = container.budgetRepository,
                    onComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // 1. Dashboard
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = viewModel {
                    DashboardViewModel(
                        accountRepository = container.accountRepository,
                        transactionRepository = container.transactionRepository,
                        budgetRepository = container.budgetRepository,
                        categoryRepository = container.categoryRepository,
                        recurringRepository = container.recurringRepository,
                        goalRepository = container.goalRepository,
                        investmentRepository = container.investmentRepository,
                        debtRepository = container.debtRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore
                    )
                }
                val uiState by viewModel.uiState.collectAsState()

                DashboardScreen(
                    uiState = uiState,
                    onPeriodSelected = viewModel::onPeriodSelected,
                    onToggleHideBalances = viewModel::toggleHideBalances,
                    onNavigateToAddTransaction = {
                        quickAddViewModel.reset()
                        showQuickAddSheet = true
                    },
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                    onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToInsights = { navController.navigate(Screen.Insights.route) }
                )
            }

            // 2. Transactions
            composable(Screen.Transactions.route) {
                val viewModel: TransactionsViewModel = viewModel {
                    TransactionsViewModel(
                        transactionRepository = container.transactionRepository,
                        accountRepository = container.accountRepository,
                        categoryRepository = container.categoryRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore
                    )
                }
                val uiState by viewModel.uiState.collectAsState()

                TransactionsScreen(
                    uiState = uiState,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onTypeFilterChange = viewModel::onTypeFilterChange,
                    onAccountFilterChange = viewModel::onAccountFilterChange,
                    onCategoryFilterChange = viewModel::onCategoryFilterChange,
                    onSortOrderChange = viewModel::onSortOrderChange,
                    onShowFilterSheet = viewModel::showFilterSheet,
                    onShowAddEditDialog = viewModel::showAddEditDialog,
                    onSaveTransaction = viewModel::saveTransaction,
                    onDuplicateTransaction = viewModel::duplicateTransaction,
                    onDeleteTransaction = viewModel::deleteTransaction
                )
            }

            // 3. Budgets
            composable(Screen.Budgets.route) {
                val viewModel: BudgetsViewModel = viewModel {
                    BudgetsViewModel(
                        budgetRepository = container.budgetRepository,
                        categoryRepository = container.categoryRepository,
                        transactionRepository = container.transactionRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore
                    )
                }
                val uiState by viewModel.uiState.collectAsState()

                BudgetsScreen(
                    uiState = uiState,
                    onShowAddEditDialog = viewModel::showAddEditDialog,
                    onSaveBudget = viewModel::saveBudget,
                    onDeleteBudget = viewModel::deleteBudget
                )
            }

            // 4. Analytics
            composable(Screen.Analytics.route) {
                val viewModel: AnalyticsViewModel = viewModel {
                    AnalyticsViewModel(
                        transactionRepository = container.transactionRepository,
                        categoryRepository = container.categoryRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore
                    )
                }
                val uiState by viewModel.uiState.collectAsState()

                AnalyticsScreen(
                    uiState = uiState,
                    onPeriodSelected = viewModel::onPeriodSelected
                )
            }

            // 5. More (Hub)
            composable(Screen.More.route) {
                MoreHubScreen(
                    baseCurrency = userPrefs.baseCurrencyCode,
                    onNavigate = { screen -> navController.navigate(screen.route) }
                )
            }

            // Sub-screen: Accounts
            composable(Screen.Accounts.route) {
                val viewModel: AccountsViewModel = viewModel {
                    AccountsViewModel(
                        accountRepository = container.accountRepository,
                        transactionRepository = container.transactionRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore
                    )
                }
                val uiState by viewModel.uiState.collectAsState()

                AccountsScreen(
                    uiState = uiState,
                    onNavigateBack = { navController.popBackStack() },
                    onShowAddEditDialog = viewModel::showAddEditDialog,
                    onShowTransferDialog = viewModel::showTransferDialog,
                    onSaveAccount = viewModel::saveAccount,
                    onTransferFunds = viewModel::transferFunds,
                    onArchiveAccount = viewModel::archiveAccount,
                    onDeleteAccount = viewModel::deleteAccount
                )
            }

            // Sub-screen: Recurring & Subscriptions
            composable(Screen.Recurring.route) {
                val viewModel: RecurringViewModel = viewModel {
                    RecurringViewModel(
                        recurringRepository = container.recurringRepository,
                        accountRepository = container.accountRepository,
                        categoryRepository = container.categoryRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore
                    )
                }
                val uiState by viewModel.uiState.collectAsState()

                RecurringScreen(
                    uiState = uiState,
                    onNavigateBack = { navController.popBackStack() },
                    onShowAddEditDialog = viewModel::showAddEditDialog,
                    onSaveRecurring = viewModel::saveRecurring,
                    onToggleActive = viewModel::toggleActive,
                    onDeleteRecurring = viewModel::deleteRecurring
                )
            }

            // Sub-screen: Goals
            composable(Screen.Goals.route) {
                val viewModel: GoalsViewModel = viewModel {
                    GoalsViewModel(
                        goalRepository = container.goalRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore
                    )
                }
                val uiState by viewModel.uiState.collectAsState()

                GoalsScreen(
                    uiState = uiState,
                    onNavigateBack = { navController.popBackStack() },
                    onShowAddEditDialog = viewModel::showAddEditDialog,
                    onShowContributeDialog = viewModel::showContributeDialog,
                    onSaveGoal = viewModel::saveGoal,
                    onContribute = viewModel::contributeToGoal,
                    onDeleteGoal = viewModel::deleteGoal
                )
            }

            // Sub-screen: Investments
            composable(Screen.Investments.route) {
                val viewModel: InvestmentsViewModel = viewModel {
                    InvestmentsViewModel(
                        investmentRepository = container.investmentRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore
                    )
                }
                val uiState by viewModel.uiState.collectAsState()

                InvestmentsScreen(
                    uiState = uiState,
                    onNavigateBack = { navController.popBackStack() },
                    onShowAddEditDialog = viewModel::showAddEditDialog,
                    onSaveAsset = viewModel::saveAsset,
                    onDeleteAsset = viewModel::deleteAsset
                )
            }

            // Sub-screen: Debt Payoff Planner
            composable(Screen.Debt.route) {
                val viewModel: DebtViewModel = viewModel {
                    DebtViewModel(
                        debtRepository = container.debtRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore
                    )
                }
                val uiState by viewModel.uiState.collectAsState()

                DebtScreen(
                    uiState = uiState,
                    onNavigateBack = { navController.popBackStack() },
                    onStrategyChange = viewModel::onStrategyChange,
                    onShowAddEditDialog = viewModel::showAddEditDialog,
                    onShowPaymentDialog = viewModel::showPaymentDialog,
                    onSaveDebt = viewModel::saveDebt,
                    onMakePayment = viewModel::makePayment,
                    onDeleteDebt = viewModel::deleteDebt
                )
            }

            // Sub-screen: Categories
            composable(Screen.Categories.route) {
                CategoriesScreen(
                    categoryRepository = container.categoryRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Insights
            composable(Screen.Insights.route) {
                val dashboardViewModel: DashboardViewModel = viewModel {
                    DashboardViewModel(
                        accountRepository = container.accountRepository,
                        transactionRepository = container.transactionRepository,
                        budgetRepository = container.budgetRepository,
                        categoryRepository = container.categoryRepository,
                        recurringRepository = container.recurringRepository,
                        goalRepository = container.goalRepository,
                        investmentRepository = container.investmentRepository,
                        debtRepository = container.debtRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore
                    )
                }
                val uiState by dashboardViewModel.uiState.collectAsState()

                InsightsScreen(
                    insights = uiState.insights,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Global Search
            composable(Screen.Search.route) {
                SearchScreen(
                    transactionRepository = container.transactionRepository,
                    accountRepository = container.accountRepository,
                    categoryRepository = container.categoryRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Security & Privacy
            composable(Screen.Security.route) {
                SecurityScreen(
                    userPreferencesDataStore = container.userPreferencesDataStore,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Data Export
            composable(Screen.Export.route) {
                ExportScreen(
                    transactionRepository = container.transactionRepository,
                    accountRepository = container.accountRepository,
                    categoryRepository = container.categoryRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (showQuickAddSheet) {
        QuickAddBottomSheet(
            uiState = quickAddState,
            onDigitClick = quickAddViewModel::onNumpadDigit,
            onQuickAmountAdd = quickAddViewModel::onQuickAmountAdd,
            onTypeSelected = quickAddViewModel::onTypeSelected,
            onAccountSelected = quickAddViewModel::onAccountSelected,
            onDestinationAccountSelected = quickAddViewModel::onDestinationAccountSelected,
            onCategorySelected = quickAddViewModel::onCategorySelected,
            onMerchantSelected = quickAddViewModel::onMerchantSelected,
            onMerchantTextChange = quickAddViewModel::onMerchantTextChange,
            onDescriptionTextChange = quickAddViewModel::onDescriptionTextChange,
            onDateChoiceSelected = quickAddViewModel::onDateChoiceSelected,
            onSave = quickAddViewModel::save,
            onDismiss = {
                showQuickAddSheet = false
                quickAddViewModel.reset()
            },
            onTransactionSaved = { tx, andAddAnother ->
                if (!andAddAnother) {
                    showQuickAddSheet = false
                }
                scope.launch {
                    val action = snackbarHostState.showSnackbar(
                        message = "${tx.type.displayName} of ${tx.amount.formatted()} saved",
                        actionLabel = "Edit",
                        duration = SnackbarDuration.Short
                    )
                    if (action == SnackbarResult.ActionPerformed) {
                        quickAddViewModel.loadForEdit(tx)
                        showQuickAddSheet = true
                    }
                }
            }
        )
    }
}
