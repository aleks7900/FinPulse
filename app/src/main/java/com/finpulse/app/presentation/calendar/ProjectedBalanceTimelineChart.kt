package com.finpulse.app.presentation.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finpulse.app.core.designsystem.AmberWarning
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.DayCashFlow
import java.time.format.DateTimeFormatter

@Composable
fun ProjectedBalanceTimelineChart(
    dailyProjections: List<DayCashFlow>,
    startingBalance: Money,
    projectedEndingBalance: Money,
    lowestBalance: Money,
    modifier: Modifier = Modifier
) {
    if (dailyProjections.isEmpty()) return

    val balanceValues = remember(dailyProjections) {
        dailyProjections.map { it.projectedEndBalance.amountMinor.toDouble() }
    }

    val minVal = balanceValues.minOrNull() ?: 0.0
    val maxVal = balanceValues.maxOrNull() ?: 1.0
    val effectiveMax = if (maxVal == minVal) maxVal + 1000.0 else maxVal
    val range = (effectiveMax - minVal).coerceAtLeast(1.0)

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(dailyProjections) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
    }

    val isDeficitProjected = lowestBalance.amountMinor < 0L
    val primaryChartColor = if (isDeficitProjected) CrimsonExpense else EmeraldPrimary

    Column(modifier = modifier) {
        // Timeline Header: Key Milestones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Starting Liquid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = startingBalance.formatted(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDeficitProjected) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CrimsonExpense,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "Lowest Projected",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDeficitProjected) CrimsonExpense else AmberWarning
                    )
                }
                Text(
                    text = lowestBalance.formatted(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDeficitProjected) CrimsonExpense else MaterialTheme.colorScheme.onSurface
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Projected End",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = projectedEndingBalance.formatted(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (projectedEndingBalance >= startingBalance) EmeraldPrimary else CrimsonExpense
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Canvas Timeline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingBottom = 16f
                val paddingTop = 12f
                val usableHeight = height - paddingBottom - paddingTop

                val n = balanceValues.size
                val stepX = if (n > 1) width / (n - 1) else width

                // Zero line if balance crosses or approaches zero
                if (minVal < 0 && effectiveMax > 0) {
                    val zeroNormY = ((0.0 - minVal) / range).toFloat()
                    val zeroY = height - paddingBottom - (zeroNormY * usableHeight)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.35f),
                        start = Offset(0f, zeroY),
                        end = Offset(width, zeroY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                val linePath = Path()
                val areaPath = Path()

                var lowestPointOffset: Offset? = null
                var lowestValueRecorded = Double.MAX_VALUE

                balanceValues.forEachIndexed { i, balance ->
                    val normY = ((balance - minVal) / range).toFloat()
                    val x = i * stepX
                    val y = height - paddingBottom - (normY * usableHeight * animProgress.value)

                    if (i == 0) {
                        linePath.moveTo(x, y)
                        areaPath.moveTo(x, height)
                        areaPath.lineTo(x, y)
                    } else {
                        // Smooth cubic bezier
                        val prevBalance = balanceValues[i - 1]
                        val prevNormY = ((prevBalance - minVal) / range).toFloat()
                        val prevX = (i - 1) * stepX
                        val prevY = height - paddingBottom - (prevNormY * usableHeight * animProgress.value)

                        val controlX1 = prevX + (x - prevX) / 2f
                        val controlY1 = prevY
                        val controlX2 = prevX + (x - prevX) / 2f
                        val controlY2 = y

                        linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                        areaPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                    }

                    if (balance < lowestValueRecorded) {
                        lowestValueRecorded = balance
                        lowestPointOffset = Offset(x, y)
                    }
                }

                areaPath.lineTo(width, height)
                areaPath.close()

                // Draw Gradient Fill
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryChartColor.copy(alpha = 0.28f),
                            primaryChartColor.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        startY = paddingTop,
                        endY = height
                    )
                )

                // Draw Main Line
                drawPath(
                    path = linePath,
                    color = primaryChartColor,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw End Point Dot
                val lastBalance = balanceValues.last()
                val lastNormY = ((lastBalance - minVal) / range).toFloat()
                val lastY = height - paddingBottom - (lastNormY * usableHeight * animProgress.value)
                drawCircle(
                    color = primaryChartColor,
                    radius = 5.dp.toPx(),
                    center = Offset(width, lastY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(width, lastY)
                )

                // Draw Lowest Point Highlight Dot
                lowestPointOffset?.let { pt ->
                    if (animProgress.value > 0.8f) {
                        val highlightColor = if (isDeficitProjected) CrimsonExpense else AmberWarning
                        drawCircle(
                            color = highlightColor.copy(alpha = 0.35f),
                            radius = 8.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = highlightColor,
                            radius = 4.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }

        // Timeline Date Range Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val formatter = remember { DateTimeFormatter.ofPattern("MMM d") }
            val firstDate = dailyProjections.first().date.format(formatter)
            val lastDate = dailyProjections.last().date.format(formatter)

            Text(
                text = firstDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${dailyProjections.size} days projected",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = lastDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
