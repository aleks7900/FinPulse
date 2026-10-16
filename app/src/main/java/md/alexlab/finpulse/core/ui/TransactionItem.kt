package md.alexlab.finpulse.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.AmberWarning
import md.alexlab.finpulse.core.designsystem.AnimatedAmountText
import md.alexlab.finpulse.core.designsystem.CrimsonExpense
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.designsystem.FinPulseMotion
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
    modifier: Modifier = Modifier,
    enableExpansion: Boolean = false
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
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(itemShape)
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy))
            .bounceClick(scaleDown = 0.98f) {
                if (enableExpansion) {
                    isExpanded = !isExpanded
                } else {
                    onClick()
                }
            },
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    AnimatedAmountText(
                        amountText = if (hideBalances) "••••••" else "$amountPrefix${transaction.amount.formatted()}",
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

            // Expanded details row with smooth spring accordion
            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(FinPulseRadius.md),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (!transaction.notes.isNullOrBlank()) {
                            Text(
                                text = transaction.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(FinPulseRadius.full),
                                color = EmeraldPrimary.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .bounceClick(scaleDown = 0.92f) { onClick() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.action_edit),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }
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
                .size(44.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .width(65.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerEffect()
        )
    }
}
