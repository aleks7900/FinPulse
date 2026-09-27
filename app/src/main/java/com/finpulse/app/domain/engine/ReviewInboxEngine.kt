package com.finpulse.app.domain.engine

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.CategorizationCandidate
import com.finpulse.app.domain.model.CategorizationRule
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.MerchantSignal
import com.finpulse.app.domain.model.OccurrenceStatus
import com.finpulse.app.domain.model.RecurringOccurrence
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.ReviewInboxItem
import com.finpulse.app.domain.model.ReviewInboxSummary
import com.finpulse.app.domain.model.ReviewItemPriority
import com.finpulse.app.domain.model.ReviewItemType
import com.finpulse.app.domain.model.SafeBulkSuggestion
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object ReviewInboxEngine {

    private val UNKNOWN_CATEGORY_IDS = setOf(
        "cat_uncategorized",
        "cat_income_uncategorized",
        "cat_other",
        "cat_other_expenses",
        "cat_other_income"
    )

    private const val LARGE_AMOUNT_THRESHOLD_MINOR = 100_000L // $1,000.00
    private const val DAY_MILLIS = 86_400_000L
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    fun evaluate(
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>,
        activeRules: List<CategorizationRule>,
        signals: List<MerchantSignal>,
        recurringRules: List<RecurringTransaction>,
        occurrences: List<RecurringOccurrence>,
        dismissedIds: Set<String>,
        nowMillis: Long = System.currentTimeMillis()
    ): ReviewInboxSummary {
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        val activeAccountIds = accounts.filter { !it.isArchived }.map { it.id }.toSet()

        val items = mutableListOf<ReviewInboxItem>()

        // -------------------------------------------------------------
        // 1. Uncategorized & 2. Imported Transactions Needing Confirmation
        // -------------------------------------------------------------
        for (tx in transactions) {
            val isExplicitlyUnconfirmed = !tx.isCategoryConfirmed
            val isDefaultUncategorized = tx.categoryId.isBlank() || tx.categoryId in UNKNOWN_CATEGORY_IDS ||
                    categoryMap[tx.categoryId]?.name?.contains("uncategorized", ignoreCase = true) == true

            val isImported = tx.notes?.contains("CSV Import", ignoreCase = true) == true ||
                    tx.notes?.contains("Imported", ignoreCase = true) == true ||
                    tx.tags.contains("imported")

            if (isExplicitlyUnconfirmed || isDefaultUncategorized) {
                val candidate = CategorizationCandidate(
                    merchant = tx.merchant,
                    description = tx.description,
                    amountMinor = tx.amount.amountMinor,
                    sourceAccountId = tx.sourceAccountId,
                    type = tx.type
                )
                val suggestion = CategorizationEngine.categorize(
                    candidate = candidate,
                    rules = activeRules,
                    signals = signals
                )

                val suggestedCategory = suggestion.categoryId?.let { categoryMap[it] }

                if (isImported && !isDefaultUncategorized) {
                    // Imported with assigned category needing confirmation
                    val itemId = "inbox_import_${tx.id}"
                    val catName = categoryMap[tx.categoryId]?.name ?: "Uncategorized"
                    items.add(
                        ReviewInboxItem(
                            id = itemId,
                            type = ReviewItemType.IMPORTED_CONFIRMATION,
                            priority = ReviewItemPriority.ACTIONABLE,
                            title = tx.merchant?.takeIf { it.isNotBlank() } ?: tx.description.ifEmpty { "Imported Transaction" },
                            subtitle = "${accountMap[tx.sourceAccountId]?.name ?: "Account"} • ${formatDate(tx.timestamp)}",
                            timestamp = tx.timestamp,
                            amount = tx.amount,
                            transaction = tx,
                            suggestedCategoryId = suggestion.categoryId ?: tx.categoryId,
                            suggestedCategoryName = suggestedCategory?.name ?: catName,
                            suggestedCategoryConfidence = suggestion.confidenceScore,
                            reason = "Imported statement transaction assigned to $catName. Confirm or adjust details.",
                            accountName = accountMap[tx.sourceAccountId]?.name,
                            categoryName = catName
                        )
                    )
                } else {
                    // Uncategorized
                    val itemId = "inbox_uncat_${tx.id}"
                    val reason = if (suggestedCategory != null) {
                        "Suggested: ${suggestedCategory.name} (${(suggestion.confidenceScore * 100).toInt()}% match)"
                    } else {
                        "No category assigned. Please categorize to ensure accurate budgeting."
                    }

                    items.add(
                        ReviewInboxItem(
                            id = itemId,
                            type = ReviewItemType.UNCATEGORIZED,
                            priority = ReviewItemPriority.ACTIONABLE,
                            title = tx.merchant?.takeIf { it.isNotBlank() } ?: tx.description.ifEmpty { "Uncategorized Transaction" },
                            subtitle = "${accountMap[tx.sourceAccountId]?.name ?: "Account"} • ${formatDate(tx.timestamp)}",
                            timestamp = tx.timestamp,
                            amount = tx.amount,
                            transaction = tx,
                            suggestedCategoryId = suggestion.categoryId,
                            suggestedCategoryName = suggestedCategory?.name,
                            suggestedCategoryConfidence = suggestion.confidenceScore,
                            reason = reason,
                            accountName = accountMap[tx.sourceAccountId]?.name,
                            categoryName = categoryMap[tx.categoryId]?.name ?: "Uncategorized"
                        )
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 3. Suspected Duplicates (Deterministic duplicate pairing)
        // -------------------------------------------------------------
        val sortedForDuplicates = transactions.sortedBy { it.timestamp }
        val checkedPairs = mutableSetOf<String>()

        for (i in 0 until sortedForDuplicates.size) {
            val txA = sortedForDuplicates[i]
            for (j in i + 1 until sortedForDuplicates.size) {
                val txB = sortedForDuplicates[j]
                val timeDiff = abs(txA.timestamp - txB.timestamp)
                if (timeDiff > 3 * DAY_MILLIS) break // Window of 3 days

                if (txA.id == txB.id) continue
                if (txA.amount.amountMinor != txB.amount.amountMinor) continue
                if (txA.type != txB.type) continue

                val pairKey = if (txA.id < txB.id) "${txA.id}_${txB.id}" else "${txB.id}_${txA.id}"
                if (checkedPairs.contains(pairKey)) continue

                // Check similarity: same account OR similar description/merchant
                val sameAccount = txA.sourceAccountId == txB.sourceAccountId
                val textA = (txA.merchant ?: txA.description).lowercase().trim()
                val textB = (txB.merchant ?: txB.description).lowercase().trim()
                val textMatch = textA.isNotBlank() && textB.isNotBlank() &&
                        (textA == textB || textA.contains(textB) || textB.contains(textA))

                val isExactDuplicate = sameAccount && (timeDiff < DAY_MILLIS || textMatch)
                val isSuspectedDuplicate = isExactDuplicate || (timeDiff <= 2 * DAY_MILLIS && textMatch)

                if (isSuspectedDuplicate) {
                    checkedPairs.add(pairKey)
                    val days = (timeDiff / DAY_MILLIS).toInt()
                    val reason = if (days == 0) {
                        "Identical amount (${txA.amount.formatted()}) on the same day"
                    } else {
                        "Identical amount (${txA.amount.formatted()}) within $days day(s)"
                    }

                    items.add(
                        ReviewInboxItem(
                            id = "inbox_dup_$pairKey",
                            type = ReviewItemType.SUSPECTED_DUPLICATE,
                            priority = ReviewItemPriority.WARNING,
                            title = "Suspected Duplicate: ${txA.amount.formatted()}",
                            subtitle = "${accountMap[txA.sourceAccountId]?.name ?: "Account"} • ${txA.merchant ?: txA.description}",
                            timestamp = maxOf(txA.timestamp, txB.timestamp),
                            amount = txA.amount,
                            transaction = txA,
                            duplicateCandidate = txB,
                            reason = reason,
                            accountName = accountMap[txA.sourceAccountId]?.name,
                            categoryName = categoryMap[txA.categoryId]?.name
                        )
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 4. Missing Merchant Information
        // -------------------------------------------------------------
        for (tx in transactions) {
            if (tx.isCategoryConfirmed && tx.merchant.isNullOrBlank() &&
                (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.INCOME)
            ) {
                items.add(
                    ReviewInboxItem(
                        id = "inbox_merchant_${tx.id}",
                        type = ReviewItemType.MISSING_MERCHANT,
                        priority = ReviewItemPriority.ACTIONABLE,
                        title = tx.description.takeIf { it.isNotBlank() } ?: "Missing Merchant Name",
                        subtitle = "${accountMap[tx.sourceAccountId]?.name ?: "Account"} • ${formatDate(tx.timestamp)}",
                        timestamp = tx.timestamp,
                        amount = tx.amount,
                        transaction = tx,
                        reason = "Merchant name missing. Adding a merchant improves search and auto-categorization.",
                        accountName = accountMap[tx.sourceAccountId]?.name,
                        categoryName = categoryMap[tx.categoryId]?.name
                    )
                )
            }
        }

        // -------------------------------------------------------------
        // 5. Unusual Amounts Flagged by Deterministic Rules
        // -------------------------------------------------------------
        val categoryExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE && it.amount.amountMinor > 0L }
            .groupBy { it.categoryId }

        for (tx in transactions) {
            val amountMinor = tx.amount.amountMinor

            // Rule 1: Zero amount
            if (amountMinor == 0L) {
                items.add(
                    ReviewInboxItem(
                        id = "inbox_unusual_zero_${tx.id}",
                        type = ReviewItemType.UNUSUAL_AMOUNT,
                        priority = ReviewItemPriority.WARNING,
                        title = "Zero Amount: ${tx.merchant ?: tx.description.ifEmpty { "Transaction" }}",
                        subtitle = "${accountMap[tx.sourceAccountId]?.name ?: "Account"} • ${formatDate(tx.timestamp)}",
                        timestamp = tx.timestamp,
                        amount = tx.amount,
                        transaction = tx,
                        reason = "Transaction has an amount of $0.00. Check if this is an incomplete entry or hold.",
                        accountName = accountMap[tx.sourceAccountId]?.name,
                        categoryName = categoryMap[tx.categoryId]?.name
                    )
                )
            } else if (tx.type == TransactionType.EXPENSE) {
                val peerExpenses = categoryExpenses[tx.categoryId]?.filter { it.id != tx.id }.orEmpty()
                val peerAvgMinor = if (peerExpenses.size >= 2) {
                    peerExpenses.map { it.amount.amountMinor }.average().toLong()
                } else null

                // Rule 2: Outlier vs category baseline (> 3x)
                if (peerAvgMinor != null && peerAvgMinor > 0 && amountMinor > 3 * peerAvgMinor) {
                    val catName = categoryMap[tx.categoryId]?.name ?: "this category"
                    val ratio = "%.1f".format(amountMinor.toDouble() / peerAvgMinor)
                    items.add(
                        ReviewInboxItem(
                            id = "inbox_unusual_outlier_${tx.id}",
                            type = ReviewItemType.UNUSUAL_AMOUNT,
                            priority = ReviewItemPriority.WARNING,
                            title = "High Spending Outlier: ${tx.amount.formatted()}",
                            subtitle = "${tx.merchant ?: tx.description} • $catName",
                            timestamp = tx.timestamp,
                            amount = tx.amount,
                            transaction = tx,
                            reason = "This ${tx.amount.formatted()} expense is ${ratio}x higher than your average $catName spend (${Money(peerAvgMinor, tx.amount.currencyCode).formatted()}).",
                            accountName = accountMap[tx.sourceAccountId]?.name,
                            categoryName = catName
                        )
                    )
                } else if (amountMinor >= LARGE_AMOUNT_THRESHOLD_MINOR) {
                    // Rule 3: High value single transaction (>= $1,000.00)
                    items.add(
                        ReviewInboxItem(
                            id = "inbox_unusual_high_${tx.id}",
                            type = ReviewItemType.UNUSUAL_AMOUNT,
                            priority = ReviewItemPriority.WARNING,
                            title = "Large Expense: ${tx.amount.formatted()}",
                            subtitle = "${accountMap[tx.sourceAccountId]?.name ?: "Account"} • ${tx.merchant ?: tx.description}",
                            timestamp = tx.timestamp,
                            amount = tx.amount,
                            transaction = tx,
                            reason = "Single transaction of ${tx.amount.formatted()} exceeds the large expense threshold.",
                            accountName = accountMap[tx.sourceAccountId]?.name,
                            categoryName = categoryMap[tx.categoryId]?.name
                        )
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 6. Overdue Expected Bills
        // -------------------------------------------------------------
        val startOfTodayMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        for (occ in occurrences) {
            val isUnpaid = occ.status == OccurrenceStatus.EXPECTED || occ.status == OccurrenceStatus.OVERDUE
            if (isUnpaid && occ.dueDate < startOfTodayMillis) {
                val daysOverdue = maxOf(1, ((startOfTodayMillis - occ.dueDate) / DAY_MILLIS).toInt())
                items.add(
                    ReviewInboxItem(
                        id = "inbox_overdue_${occ.id}",
                        type = ReviewItemType.OVERDUE_BILL,
                        priority = ReviewItemPriority.CRITICAL,
                        title = "Overdue Bill: ${occ.ruleTitle}",
                        subtitle = "Due ${formatDate(occ.dueDate)} ($daysOverdue day${if (daysOverdue > 1) "s" else ""} ago)",
                        timestamp = occ.dueDate,
                        amount = occ.amount,
                        recurringOccurrence = occ,
                        reason = "Payment of ${occ.amount.formatted()} was due on ${formatDate(occ.dueDate)} and is currently unpaid.",
                        accountName = accountMap[occ.accountId]?.name,
                        categoryName = categoryMap[occ.categoryId]?.name
                    )
                )
            }
        }

        // -------------------------------------------------------------
        // 7. Failed Recurring Transaction Generation
        // -------------------------------------------------------------
        for (rule in recurringRules) {
            if (!rule.isActive || rule.isCancelled) continue

            val isAccountMissingOrArchived = !activeAccountIds.contains(rule.accountId)
            val isNextDuePassedWithoutTx = rule.nextDueDate < (nowMillis - 24 * 3600 * 1000L)

            if (isAccountMissingOrArchived) {
                items.add(
                    ReviewInboxItem(
                        id = "inbox_failed_acc_${rule.id}",
                        type = ReviewItemType.FAILED_RECURRING,
                        priority = ReviewItemPriority.CRITICAL,
                        title = "Recurring Issue: ${rule.title}",
                        subtitle = "Account unavailable",
                        timestamp = rule.nextDueDate,
                        amount = rule.amount,
                        recurringRule = rule,
                        reason = "Source account is missing or archived. Scheduled generation cannot proceed until account is updated.",
                        accountName = accountMap[rule.accountId]?.name ?: "Unknown Account",
                        categoryName = categoryMap[rule.categoryId]?.name
                    )
                )
            } else if (isNextDuePassedWithoutTx) {
                // Rule due date passed > 1 day ago without advancing
                val daysAgo = ((nowMillis - rule.nextDueDate) / DAY_MILLIS).toInt()
                items.add(
                    ReviewInboxItem(
                        id = "inbox_failed_due_${rule.id}_${rule.nextDueDate}",
                        type = ReviewItemType.FAILED_RECURRING,
                        priority = ReviewItemPriority.CRITICAL,
                        title = "Missed Generation: ${rule.title}",
                        subtitle = "Scheduled for ${formatDate(rule.nextDueDate)} ($daysAgo days ago)",
                        timestamp = rule.nextDueDate,
                        amount = rule.amount,
                        recurringRule = rule,
                        reason = "Recurring rule schedule was missed or stalled. Trigger generation now or mark paid.",
                        accountName = accountMap[rule.accountId]?.name,
                        categoryName = categoryMap[rule.categoryId]?.name
                    )
                )
            }
        }

        // -------------------------------------------------------------
        // Filter Dismissed Items
        // -------------------------------------------------------------
        val activeItems = items.filter { !dismissedIds.contains(it.id) }

        // Sort: CRITICAL (1) -> WARNING (2) -> ACTIONABLE (3), then timestamp descending
        val sortedItems = activeItems.sortedWith(
            compareBy<ReviewInboxItem> { it.priority.level }
                .thenByDescending { it.timestamp }
        )

        // -------------------------------------------------------------
        // 8. Safe Bulk Categorization Suggestions
        // -------------------------------------------------------------
        val safeBulkSuggestions = mutableListOf<SafeBulkSuggestion>()
        val unconfirmedWithSuggestions = activeItems
            .filter { (it.type == ReviewItemType.UNCATEGORIZED || it.type == ReviewItemType.IMPORTED_CONFIRMATION) && it.suggestedCategoryId != null && it.suggestedCategoryConfidence >= 0.75f }
            .mapNotNull { it.transaction }

        val groupedByMerchant = unconfirmedWithSuggestions
            .filter { !it.merchant.isNullOrBlank() }
            .groupBy { it.merchant!!.trim().lowercase() }

        for ((_, group) in groupedByMerchant) {
            if (group.size >= 2) {
                val candidateTx = group.first()
                val candidate = CategorizationCandidate(
                    merchant = candidateTx.merchant,
                    description = candidateTx.description,
                    amountMinor = candidateTx.amount.amountMinor,
                    sourceAccountId = candidateTx.sourceAccountId,
                    type = candidateTx.type
                )
                val suggestion = CategorizationEngine.categorize(candidate, activeRules, signals)
                val targetCatId = suggestion.categoryId
                val targetCatName = targetCatId?.let { categoryMap[it]?.name }

                if (targetCatId != null && targetCatName != null && suggestion.confidenceScore >= 0.75f) {
                    val merchantDisplayName = candidateTx.merchant!!
                    safeBulkSuggestions.add(
                        SafeBulkSuggestion(
                            id = "bulk_${merchantDisplayName.hashCode()}_$targetCatId",
                            merchant = merchantDisplayName,
                            suggestedCategoryId = targetCatId,
                            suggestedCategoryName = targetCatName,
                            confidence = suggestion.confidenceScore,
                            transactionIds = group.map { it.id },
                            count = group.size
                        )
                    )
                }
            }
        }

        return ReviewInboxSummary(
            items = sortedItems,
            safeBulkSuggestions = safeBulkSuggestions,
            totalCount = sortedItems.size,
            uncategorizedCount = sortedItems.count { it.type == ReviewItemType.UNCATEGORIZED },
            importedCount = sortedItems.count { it.type == ReviewItemType.IMPORTED_CONFIRMATION },
            duplicatesCount = sortedItems.count { it.type == ReviewItemType.SUSPECTED_DUPLICATE },
            missingMerchantCount = sortedItems.count { it.type == ReviewItemType.MISSING_MERCHANT },
            unusualAmountsCount = sortedItems.count { it.type == ReviewItemType.UNUSUAL_AMOUNT },
            overdueBillsCount = sortedItems.count { it.type == ReviewItemType.OVERDUE_BILL },
            failedRecurringCount = sortedItems.count { it.type == ReviewItemType.FAILED_RECURRING }
        )
    }

    private fun formatDate(timestampMillis: Long): String {
        return try {
            val date = Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            date.format(dateFormatter)
        } catch (_: Exception) {
            "Recent"
        }
    }
}
