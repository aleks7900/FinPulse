package com.finpulse.app.presentation.navigation

import androidx.compose.foundation.layout.Box
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
    onQuickAddHandled: () -> Unit = {},
    openQuickAddTypeTrigger: StateFlow<String?>? = null,
    onQuickAddTypeHandled: () -> Unit = {},
    navigationTrigger: StateFlow<String?>? = null,
    onNavigationHandled: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = androidx.compose.ui.platform.LocalContext.current

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
            userPreferencesDataStore = container.userPreferencesDataStore,
            categorizeTransactionUseCase = container.categorizeTransactionUseCase,
            recordCategoryCorrectionUseCase = container.recordCategoryCorrectionUseCase
        )
    }
    val quickAddState by quickAddViewModel.uiState.collectAsState()
    var showQuickAddSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isAppLocked by container.appLockManager.isAppLocked.collectAsState()

    openQuickAddTrigger?.let { triggerFlow ->
        val shouldOpen by triggerFlow.collectAsState()
        LaunchedEffect(shouldOpen, isAppLocked) {
            if (shouldOpen && !isAppLocked) {
                quickAddViewModel.reset()
                showQuickAddSheet = true
                onQuickAddHandled()
            }
        }
    }

    openQuickAddTypeTrigger?.let { triggerFlow ->
        val triggerType by triggerFlow.collectAsState()
        LaunchedEffect(triggerType, isAppLocked) {
            if (!isAppLocked) {
                triggerType?.let { typeStr ->
                    quickAddViewModel.reset()
                    val txType = when (typeStr.uppercase()) {
                        "INCOME" -> com.finpulse.app.domain.model.TransactionType.INCOME
                        "TRANSFER" -> com.finpulse.app.domain.model.TransactionType.TRANSFER
                        else -> com.finpulse.app.domain.model.TransactionType.EXPENSE
                    }
                    quickAddViewModel.onTypeSelected(txType)
                    showQuickAddSheet = true
                    onQuickAddTypeHandled()
                }
            }
        }
    }

    navigationTrigger?.let { navFlow ->
        val targetRoute by navFlow.collectAsState()
        LaunchedEffect(targetRoute, isAppLocked) {
            if (!isAppLocked) {
                targetRoute?.let { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                    onNavigationHandled()
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
                        userPreferencesDataStore = container.userPreferencesDataStore,
                        getReviewInboxUseCase = container.getReviewInboxUseCase
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
                    onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                    onNavigateToReviewQueue = { navController.navigate(Screen.ReviewInbox.route) },
                    onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) }
                )
            }

            // 2. Transactions
            composable(Screen.Transactions.route) {
                val viewModel: TransactionsViewModel = viewModel {
                    TransactionsViewModel(
                        transactionRepository = container.transactionRepository,
                        accountRepository = container.accountRepository,
                        categoryRepository = container.categoryRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore,
                        savedFilterRepository = container.savedFilterRepository
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
                    onDeleteTransaction = viewModel::deleteTransaction,
                    onToggleFilterOnlyUnreviewed = viewModel::toggleFilterOnlyUnreviewed,
                    onDateRangePresetChange = viewModel::onDateRangePresetChange,
                    onAmountRangeChange = viewModel::onAmountRangeChange,
                    onCurrencyFilterChange = viewModel::onCurrencyFilterChange,
                    onStatusFilterChange = viewModel::onStatusFilterChange,
                    onSelectPreset = viewModel::onSelectPreset,
                    onSelectSavedFilter = viewModel::onSelectSavedFilter,
                    onResetFilters = viewModel::onResetFilters,
                    onShowSaveViewDialog = viewModel::showSaveViewDialog,
                    onSaveCurrentView = viewModel::saveCurrentFilterAsView,
                    onDeleteSavedView = viewModel::deleteSavedFilter,
                    onNavigateToReviewQueue = { navController.navigate(Screen.ReviewInbox.route) },
                    onNavigateToCsvImport = { navController.navigate(Screen.CsvImport.route) }
                )
            }

            // 3. Budgets
            composable(Screen.Budgets.route) {
                val viewModel: BudgetsViewModel = viewModel {
                    BudgetsViewModel(
                        budgetRepository = container.budgetRepository,
                        categoryRepository = container.categoryRepository,
                        transactionRepository = container.transactionRepository,
                        accountRepository = container.accountRepository,
                        recurringRepository = container.recurringRepository,
                        goalRepository = container.goalRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore,
                        calculateSafeToSpendUseCase = container.calculateSafeToSpendUseCase
                    )
                }
                val uiState by viewModel.uiState.collectAsState()

                BudgetsScreen(
                    uiState = uiState,
                    onShowAddEditDialog = viewModel::showAddEditDialog,
                    onSaveBudget = viewModel::saveBudget,
                    onDeleteBudget = viewModel::deleteBudget,
                    onToggleAssumptionsDialog = viewModel::toggleAssumptionsDialog,
                    onSelectFilter = viewModel::setFilter
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
                val context = androidx.compose.ui.platform.LocalContext.current
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                MoreHubScreen(
                    baseCurrency = userPrefs.baseCurrencyCode,
                    selectedLanguageCode = userPrefs.selectedLanguage,
                    onLanguageSelected = { lang ->
                        com.finpulse.app.core.locale.AppLocaleManager.setLocale(context, lang)
                        coroutineScope.launch {
                            container.userPreferencesDataStore.setSelectedLanguage(lang.code)
                        }
                    },
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
                        userPreferencesDataStore = container.userPreferencesDataStore,
                        getUpcomingOccurrencesUseCase = container.getUpcomingOccurrencesUseCase,
                        markOccurrencePaidUseCase = container.markOccurrencePaidUseCase,
                        skipOccurrenceUseCase = container.skipOccurrenceUseCase,
                        editOccurrenceUseCase = container.editOccurrenceUseCase,
                        manageRecurringRuleUseCase = container.manageRecurringRuleUseCase
                    )
                }
                val uiState by viewModel.uiState.collectAsState()

                RecurringScreen(
                    uiState = uiState,
                    onNavigateBack = { navController.popBackStack() },
                    onTabSelected = viewModel::onTabSelected,
                    onWindowDaysChanged = viewModel::onWindowDaysChanged,
                    onShowAddEditDialog = viewModel::showAddEditDialog,
                    onShowEditOccurrenceDialog = viewModel::showEditOccurrenceDialog,
                    onShowConfirmPayDialog = viewModel::showConfirmPayDialog,
                    onSaveRecurring = viewModel::saveRecurring,
                    onMarkPaid = viewModel::markOccurrencePaid,
                    onSkipOccurrence = viewModel::skipOccurrence,
                    onEditOccurrence = viewModel::editOccurrence,
                    onToggleActive = viewModel::toggleActive,
                    onCancelRule = viewModel::cancelRule,
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
                    onNavigateBack = { navController.popBackStack() },
                    hideBalances = userPrefs.hideBalances
                )
            }

            // Sub-screen: Security & Privacy
            composable(Screen.Security.route) {
                SecurityScreen(
                    userPreferencesDataStore = container.userPreferencesDataStore,
                    appLockManager = container.appLockManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Google Account & Cloud Sync
            composable(Screen.Account.route) {
                val accountViewModel: com.finpulse.app.presentation.account.AccountViewModel = viewModel {
                    com.finpulse.app.presentation.account.AccountViewModel(
                        authRepository = container.authRepository,
                        cloudSyncRepository = container.cloudSyncRepository,
                        googleAuthManager = container.googleAuthManager,
                        userPreferencesDataStore = container.userPreferencesDataStore
                    )
                }
                com.finpulse.app.presentation.account.AccountScreen(
                    viewModel = accountViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Data Export
            composable(Screen.Export.route) {
                val viewModel: com.finpulse.app.presentation.export.ExportViewModel = viewModel {
                    com.finpulse.app.presentation.export.ExportViewModel(
                        createBackupUseCase = container.createBackupUseCase,
                        validateBackupUseCase = container.validateBackupUseCase,
                        restoreBackupUseCase = container.restoreBackupUseCase,
                        exportTransactionsUseCase = container.exportTransactionsUseCase,
                        accountRepository = container.accountRepository,
                        categoryRepository = container.categoryRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore
                    )
                }
                ExportScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Smart Categorization & Rules
            composable(Screen.CategorizationRules.route) {
                val viewModel: com.finpulse.app.presentation.rules.CategorizationRulesViewModel = viewModel {
                    com.finpulse.app.presentation.rules.CategorizationRulesViewModel(
                        ruleRepository = container.categorizationRuleRepository,
                        categoryRepository = container.categoryRepository,
                        accountRepository = container.accountRepository,
                        getReviewQueueUseCase = container.getReviewQueueUseCase,
                        recordCategoryCorrectionUseCase = container.recordCategoryCorrectionUseCase,
                        manageRuleUseCase = container.manageCategorizationRuleUseCase,
                        findMatchingUseCase = container.findMatchingTransactionsForRuleUseCase,
                        applyRuleUseCase = container.applyRuleToExistingTransactionsUseCase
                    )
                }
                val uiState by viewModel.uiState.collectAsState()

                com.finpulse.app.presentation.rules.CategorizationRulesScreen(
                    uiState = uiState,
                    onNavigateBack = { navController.popBackStack() },
                    onTabSelected = viewModel::onTabSelected,
                    onConfirmSuggestion = viewModel::confirmSuggestion,
                    onOpenChangeCategory = viewModel::openChangeCategoryDialog,
                    onChangeCategory = viewModel::changeCategoryAndLearnSignal,
                    onOpenCreateRuleFromTransaction = viewModel::openCreateRuleFromTransaction,
                    onShowAddEditRuleDialog = viewModel::showAddEditRuleDialog,
                    onSaveRule = viewModel::saveRule,
                    onToggleRuleActive = viewModel::toggleRuleActive,
                    onUpdateRulePriority = viewModel::updateRulePriority,
                    onDeleteRule = viewModel::deleteRule,
                    onOpenApplyRuleDialog = viewModel::openApplyRuleDialog,
                    onSetOverrideManualOnApply = viewModel::setOverrideManualOnApply,
                    onConfirmApplyRule = viewModel::confirmApplyRule,
                    onDismissDialogs = viewModel::dismissDialogs
                )
            }

            // Sub-screen: Bank Statement CSV Import
            composable(Screen.CsvImport.route) {
                val viewModel: com.finpulse.app.presentation.csvimport.CsvImportViewModel = viewModel {
                    com.finpulse.app.presentation.csvimport.CsvImportViewModel(
                        accountRepository = container.accountRepository,
                        autoDetectCsvConfigUseCase = container.autoDetectCsvConfigUseCase,
                        parseCsvStatementUseCase = container.parseCsvStatementUseCase,
                        executeCsvImportUseCase = container.executeCsvImportUseCase,
                        manageImportProfilesUseCase = container.manageImportProfilesUseCase
                    )
                }
                com.finpulse.app.presentation.csvimport.CsvImportScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
            }

            // Sub-screen: Daily Transaction Review Inbox
            composable(Screen.ReviewInbox.route) {
                val viewModel: com.finpulse.app.presentation.review.ReviewInboxViewModel = viewModel {
                    com.finpulse.app.presentation.review.ReviewInboxViewModel(
                        getReviewInboxUseCase = container.getReviewInboxUseCase,
                        recordCategoryCorrectionUseCase = container.recordCategoryCorrectionUseCase,
                        resolveDuplicateTransactionUseCase = container.resolveDuplicateTransactionUseCase,
                        bulkCategorizeTransactionsUseCase = container.bulkCategorizeTransactionsUseCase,
                        updateTransactionDetailsUseCase = container.updateTransactionDetailsUseCase,
                        markOccurrencePaidUseCase = container.markOccurrencePaidUseCase,
                        categoryRepository = container.categoryRepository,
                        accountRepository = container.accountRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore
                    )
                }
                val uiState by viewModel.uiState.collectAsState()

                com.finpulse.app.presentation.review.ReviewInboxScreen(
                    uiState = uiState,
                    onNavigateBack = { navController.popBackStack() },
                    onTabSelected = viewModel::onTabSelected,
                    onToggleSelectMode = viewModel::onToggleSelectMode,
                    onToggleItemSelection = viewModel::onToggleItemSelection,
                    onSelectAll = viewModel::onSelectAll,
                    onConfirmItem = viewModel::onConfirmItem,
                    onOpenCategoryPicker = viewModel::onOpenCategoryPicker,
                    onCategorizeItem = viewModel::onCategorizeItem,
                    onOpenAccountPicker = viewModel::onOpenAccountPicker,
                    onChangeAccount = viewModel::onChangeAccount,
                    onOpenEditMerchant = viewModel::onOpenEditMerchant,
                    onSaveMerchant = viewModel::onSaveMerchant,
                    onOpenResolveDuplicate = viewModel::onOpenResolveDuplicate,
                    onResolveDuplicateKeepPrimary = viewModel::onResolveDuplicateKeepPrimary,
                    onResolveDuplicateKeepCandidate = viewModel::onResolveDuplicateKeepCandidate,
                    onResolveDuplicateDismiss = viewModel::onResolveDuplicateDismiss,
                    onOpenPayBill = viewModel::onOpenPayBill,
                    onConfirmPayBill = viewModel::onConfirmPayBill,
                    onDismissItem = viewModel::onDismissItem,
                    onDismissAllWarnings = viewModel::onDismissAllWarnings,
                    onApplySafeBulk = viewModel::onApplySafeBulk,
                    onOpenBulkCategoryPicker = viewModel::onOpenBulkCategoryPicker,
                    onConfirmBulkCategorize = viewModel::onConfirmBulkCategorize,
                    onDismissDialogs = viewModel::onDismissDialogs,
                    onClearMessage = viewModel::onClearMessage
                )
            }

            // Sub-screen: Financial Calendar & Cash Flow
            composable(Screen.Calendar.route) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val viewModel: com.finpulse.app.presentation.calendar.FinancialCalendarViewModel = viewModel {
                    com.finpulse.app.presentation.calendar.FinancialCalendarViewModel(
                        getFinancialCalendarUseCase = container.getFinancialCalendarUseCase,
                        recurringRepository = container.recurringRepository,
                        accountRepository = container.accountRepository,
                        categoryRepository = container.categoryRepository,
                        userPreferencesDataStore = container.userPreferencesDataStore,
                        markOccurrencePaidUseCase = container.markOccurrencePaidUseCase,
                        skipOccurrenceUseCase = container.skipOccurrenceUseCase,
                        editOccurrenceUseCase = container.editOccurrenceUseCase,
                        manageRecurringRuleUseCase = container.manageRecurringRuleUseCase
                    )
                }
                val uiState by viewModel.uiState.collectAsState()

                com.finpulse.app.presentation.calendar.FinancialCalendarScreen(
                    uiState = uiState,
                    onViewModeChange = viewModel::setViewMode,
                    onPeriodPresetChange = viewModel::setPeriodPreset,
                    onPreviousMonth = viewModel::selectPreviousMonth,
                    onNextMonth = viewModel::selectNextMonth,
                    onTodayClick = viewModel::selectToday,
                    onDateSelect = viewModel::selectDate,
                    onFilterChange = viewModel::setTypeFilter,
                    onEventClick = viewModel::onEventClicked,
                    onDismissEventDetails = viewModel::dismissEventDetails,
                    onShowMarkPaid = viewModel::showMarkPaidDialog,
                    onDismissMarkPaid = viewModel::dismissMarkPaidDialog,
                    onConfirmMarkPaid = viewModel::confirmMarkPaid,
                    onSkipEvent = viewModel::skipEvent,
                    onShowEditOccurrence = viewModel::showEditOccurrenceDialog,
                    onDismissEditOccurrence = viewModel::dismissEditOccurrenceDialog,
                    onSaveEditedOccurrence = viewModel::saveEditedOccurrence,
                    onSetReminder = { event, days, instant ->
                        viewModel.setEventReminder(context = context, event = event, reminderDaysBefore = days, sendInstantNotification = instant)
                    },
                    onClearReminderMessage = viewModel::clearReminderMessage,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenRecurringScreen = { navController.navigate(Screen.Recurring.route) }
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
                com.finpulse.app.presentation.widget.FinPulseWidgetUpdater.updateAllWidgets(context)
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

    // Lock Screen Full-Screen Blocking Overlay
    if (isAppLocked) {
        com.finpulse.app.presentation.security.LockScreen(
            userPrefs = userPrefs,
            appLockManager = container.appLockManager,
            onUnlockSuccess = {
                container.appLockManager.unlock()
            }
        )
    }
}
}
