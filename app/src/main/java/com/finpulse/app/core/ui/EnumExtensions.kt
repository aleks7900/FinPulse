package com.finpulse.app.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.finpulse.app.R
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.AmountMode
import com.finpulse.app.domain.model.AssetClass
import com.finpulse.app.domain.model.BudgetPeriod
import com.finpulse.app.domain.model.CustomIntervalUnit
import com.finpulse.app.domain.model.DebtType
import com.finpulse.app.domain.model.DuplicateStatus
import com.finpulse.app.domain.model.OccurrenceStatus
import com.finpulse.app.domain.model.PaymentFrequency
import com.finpulse.app.domain.model.TransactionType

fun TransactionType.getStringRes(): Int = when (this) {
    TransactionType.INCOME -> R.string.tx_type_income
    TransactionType.EXPENSE -> R.string.tx_type_expense
    TransactionType.TRANSFER -> R.string.tx_type_transfer
    TransactionType.REFUND -> R.string.tx_type_refund
}

@Composable
fun TransactionType.getLocalizedName(): String = stringResource(getStringRes())
fun TransactionType.getLocalizedName(context: Context): String = context.getString(getStringRes())

fun AccountType.getStringRes(): Int = when (this) {
    AccountType.CASH -> R.string.acc_type_cash
    AccountType.BANK -> R.string.acc_type_bank
    AccountType.CREDIT_CARD -> R.string.acc_type_credit_card
    AccountType.SAVINGS -> R.string.acc_type_savings
    AccountType.INVESTMENT -> R.string.acc_type_investment
    AccountType.WALLET -> R.string.acc_type_wallet
    AccountType.LOAN -> R.string.acc_type_loan
    AccountType.OTHER -> R.string.acc_type_other
}

@Composable
fun AccountType.getLocalizedName(): String = stringResource(getStringRes())
fun AccountType.getLocalizedName(context: Context): String = context.getString(getStringRes())

fun BudgetPeriod.getStringRes(): Int = when (this) {
    BudgetPeriod.WEEKLY -> R.string.period_weekly
    BudgetPeriod.MONTHLY -> R.string.period_monthly
    BudgetPeriod.CUSTOM -> R.string.period_custom
}

@Composable
fun BudgetPeriod.getLocalizedName(): String = stringResource(getStringRes())
fun BudgetPeriod.getLocalizedName(context: Context): String = context.getString(getStringRes())

fun PaymentFrequency.getStringRes(): Int = when (this) {
    PaymentFrequency.DAILY -> R.string.freq_daily
    PaymentFrequency.WEEKLY -> R.string.freq_weekly
    PaymentFrequency.BI_WEEKLY -> R.string.freq_biweekly
    PaymentFrequency.MONTHLY -> R.string.freq_monthly
    PaymentFrequency.QUARTERLY -> R.string.freq_quarterly
    PaymentFrequency.YEARLY -> R.string.freq_yearly
    PaymentFrequency.CUSTOM -> R.string.freq_custom
}

@Composable
fun PaymentFrequency.getLocalizedName(): String = stringResource(getStringRes())
fun PaymentFrequency.getLocalizedName(context: Context): String = context.getString(getStringRes())

fun CustomIntervalUnit.getStringRes(): Int = when (this) {
    CustomIntervalUnit.DAYS -> R.string.unit_days
    CustomIntervalUnit.WEEKS -> R.string.unit_weeks
    CustomIntervalUnit.MONTHS -> R.string.unit_months
    CustomIntervalUnit.YEARS -> R.string.unit_years
}

@Composable
fun CustomIntervalUnit.getLocalizedName(): String = stringResource(getStringRes())
fun CustomIntervalUnit.getLocalizedName(context: Context): String = context.getString(getStringRes())

fun OccurrenceStatus.getStringRes(): Int = when (this) {
    OccurrenceStatus.EXPECTED -> R.string.occ_upcoming
    OccurrenceStatus.OVERDUE -> R.string.occ_overdue
    OccurrenceStatus.GENERATED -> R.string.occ_generated
    OccurrenceStatus.PAID -> R.string.occ_paid
    OccurrenceStatus.SKIPPED -> R.string.occ_skipped
}

@Composable
fun OccurrenceStatus.getLocalizedName(): String = stringResource(getStringRes())
fun OccurrenceStatus.getLocalizedName(context: Context): String = context.getString(getStringRes())

fun AssetClass.getStringRes(): Int = when (this) {
    AssetClass.STOCK -> R.string.asset_stock
    AssetClass.ETF -> R.string.asset_etf
    AssetClass.CRYPTO -> R.string.asset_crypto
    AssetClass.BOND -> R.string.asset_bond
    AssetClass.REAL_ESTATE -> R.string.asset_real_estate
    AssetClass.COMMODITY -> R.string.asset_commodity
    AssetClass.CASH -> R.string.asset_cash
    AssetClass.OTHER -> R.string.asset_other
}

@Composable
fun AssetClass.getLocalizedName(): String = stringResource(getStringRes())
fun AssetClass.getLocalizedName(context: Context): String = context.getString(getStringRes())

fun DebtType.getStringRes(): Int = when (this) {
    DebtType.CREDIT_CARD -> R.string.debt_credit_card
    DebtType.PERSONAL_LOAN -> R.string.debt_personal_loan
    DebtType.MORTGAGE -> R.string.debt_mortgage
    DebtType.STUDENT_LOAN -> R.string.debt_student_loan
    DebtType.AUTO_LOAN -> R.string.debt_auto_loan
    DebtType.OTHER -> R.string.debt_other
}

@Composable
fun DebtType.getLocalizedName(): String = stringResource(getStringRes())
fun DebtType.getLocalizedName(context: Context): String = context.getString(getStringRes())

fun DuplicateStatus.getStringRes(): Int = when (this) {
    DuplicateStatus.NEW -> R.string.dup_new
    DuplicateStatus.EXACT_DUPLICATE -> R.string.dup_exact
    DuplicateStatus.POTENTIAL_DUPLICATE -> R.string.dup_potential
}

@Composable
fun DuplicateStatus.getLocalizedName(): String = stringResource(getStringRes())
fun DuplicateStatus.getLocalizedName(context: Context): String = context.getString(getStringRes())

fun AmountMode.getStringRes(): Int = when (this) {
    AmountMode.SINGLE_AMOUNT -> R.string.amount_mode_single
    AmountMode.SEPARATE_DEBIT_CREDIT -> R.string.amount_mode_separate
}

@Composable
fun AmountMode.getLocalizedName(): String = stringResource(getStringRes())
fun AmountMode.getLocalizedName(context: Context): String = context.getString(getStringRes())

fun com.finpulse.app.core.model.TimePeriod.getStringRes(): Int = when (this) {
    com.finpulse.app.core.model.TimePeriod.WEEK -> R.string.period_week
    com.finpulse.app.core.model.TimePeriod.MONTH -> R.string.period_month
    com.finpulse.app.core.model.TimePeriod.THREE_MONTHS -> R.string.period_three_months
    com.finpulse.app.core.model.TimePeriod.SIX_MONTHS -> R.string.period_six_months
    com.finpulse.app.core.model.TimePeriod.YEAR -> R.string.period_year
    com.finpulse.app.core.model.TimePeriod.ALL -> R.string.period_all_time
}

@Composable
fun com.finpulse.app.core.model.TimePeriod.getLocalizedName(): String = stringResource(getStringRes())
fun com.finpulse.app.core.model.TimePeriod.getLocalizedName(context: Context): String = context.getString(getStringRes())

fun com.finpulse.app.domain.engine.DebtStrategy.getTitleRes(): Int = when (this) {
    com.finpulse.app.domain.engine.DebtStrategy.SNOWBALL -> R.string.debt_strategy_snowball_title
    com.finpulse.app.domain.engine.DebtStrategy.AVALANCHE -> R.string.debt_strategy_avalanche_title
}

fun com.finpulse.app.domain.engine.DebtStrategy.getSubtitleRes(): Int = when (this) {
    com.finpulse.app.domain.engine.DebtStrategy.SNOWBALL -> R.string.debt_strategy_snowball_sub
    com.finpulse.app.domain.engine.DebtStrategy.AVALANCHE -> R.string.debt_strategy_avalanche_sub
}

@Composable
fun com.finpulse.app.domain.engine.DebtStrategy.getLocalizedTitle(): String = stringResource(getTitleRes())
@Composable
fun com.finpulse.app.domain.engine.DebtStrategy.getLocalizedSubtitle(): String = stringResource(getSubtitleRes())

fun com.finpulse.app.domain.model.CategoryType.getStringRes(): Int = when (this) {
    com.finpulse.app.domain.model.CategoryType.EXPENSE -> R.string.category_type_expense
    com.finpulse.app.domain.model.CategoryType.INCOME -> R.string.category_type_income
}

@Composable
fun com.finpulse.app.domain.model.CategoryType.getLocalizedName(): String = stringResource(getStringRes())
fun com.finpulse.app.domain.model.CategoryType.getLocalizedName(context: Context): String = context.getString(getStringRes())

fun com.finpulse.app.presentation.recurring.RecurringTab.getStringRes(): Int = when (this) {
    com.finpulse.app.presentation.recurring.RecurringTab.UPCOMING -> R.string.recurring_tab_upcoming
    com.finpulse.app.presentation.recurring.RecurringTab.RULES -> R.string.recurring_tab_rules
    com.finpulse.app.presentation.recurring.RecurringTab.SUBSCRIPTIONS -> R.string.recurring_tab_subscriptions
}

@Composable
fun com.finpulse.app.presentation.recurring.RecurringTab.getLocalizedName(): String = stringResource(getStringRes())
fun com.finpulse.app.presentation.recurring.RecurringTab.getLocalizedName(context: Context): String = context.getString(getStringRes())

