package md.alexlab.finpulse.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.AmberWarning
import md.alexlab.finpulse.core.designsystem.CrimsonExpense
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.designsystem.FinPulseRadius
import md.alexlab.finpulse.core.designsystem.TransferBlue
import md.alexlab.finpulse.core.designsystem.bounceClick
import md.alexlab.finpulse.core.designsystem.shimmerEffect
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType

@Composable
fun TransactionItem(
    transaction: Transaction,
    category: Category?,
    accountName: String,
    hideBalances: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == TransactionType.INCOME || transaction.type == TransactionType.REFUND
    val isTransfer = transaction.type == TransactionType.TRANSFER

    val amountColor: Color = when {
        isTransfer -> TransferBlue
        isIncome -> EmeraldPrimary
        else -> CrimsonExpense
    }

    val amountPrefix = when {
        isTransfer -> ""
        isIncome -> "+"
        else -> "-"
    }

    val formattedDate = DateFormatterUtils.formatDateTime(transaction.timestamp)
    val itemShape = RoundedCornerShape(FinPulseRadius.lg)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(itemShape)
            .bounceClick(scaleDown = 0.98f, onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                iconName = category?.icon ?: "paid",
                colorHex = category?.colorHex ?: 0xFF607D8B
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val fallbackTitle = category?.getDisplayName() ?: stringResource(R.string.tx_default_title)
                    Text(
                        text = transaction.merchant?.takeIf { it.isNotBlank() }
                            ?: transaction.description.takeIf { it.isNotBlank() }
                            ?: fallbackTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!transaction.isCategoryConfirmed || transaction.categoryId == "cat_uncategorized") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(FinPulseRadius.xs),
                            color = AmberWarning.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = stringResource(R.string.tx_needs_review),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "$accountName • $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (hideBalances) "••••••" else "$amountPrefix${transaction.amount.formatted()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                if (transaction.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.tags.joinToString(", ") { "#$it" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Shimmer placeholder skeleton for transactions during loading.
 */
@Composable
fun TransactionItemSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .shimmerEffect(RoundedCornerShape(14.dp))
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(16.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(12.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .width(60.dp)
                .height(16.dp)
                .shimmerEffect(RoundedCornerShape(4.dp))
        )
    }
}
