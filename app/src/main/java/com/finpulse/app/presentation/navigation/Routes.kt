package com.finpulse.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Transactions : Screen("transactions")
    data object Budgets : Screen("budgets")
    data object Analytics : Screen("analytics")
    data object More : Screen("more")

    // Sub-screens & Hub items
    data object Accounts : Screen("accounts")
    data object Recurring : Screen("recurring")
    data object Goals : Screen("goals")
    data object Investments : Screen("investments")
    data object Debt : Screen("debt")
    data object Categories : Screen("categories")
    data object Insights : Screen("insights")
    data object Search : Screen("search")
    data object Security : Screen("security")
    data object Export : Screen("export")
    data object CategorizationRules : Screen("categorization_rules")
    data object CsvImport : Screen("csv_import")
    data object Onboarding : Screen("onboarding")
    data object ReviewInbox : Screen("review_inbox")
    data object Calendar : Screen("calendar")
    data object Account : Screen("account")
    data object Digest : Screen("digest")
}

