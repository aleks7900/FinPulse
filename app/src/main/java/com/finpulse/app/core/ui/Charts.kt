package com.finpulse.app.core.ui

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finpulse.app.core.designsystem.AmberWarning
import com.finpulse.app.core.designsystem.CrimsonExpense
import com.finpulse.app.core.designsystem.EmeraldPrimary

data class DonutSegment(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    centerTitle: String,
    centerValue: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 28.dp
) {
    val total = segments.sumOf { it.value }.coerceAtLeast(1.0)
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(segments) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            var currentAngle = -90f

            for (seg in segments) {
                val sweep = ((seg.value / total) * 360f * animationProgress.value).toFloat()
                if (sweep > 0f) {
                    drawArc(
                        color = seg.color,
                        startAngle = currentAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    currentAngle += sweep
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = centerTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = centerValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SparklineChart(
    dataPoints: List<Double>,
    lineColor: Color = EmeraldPrimary,
    modifier: Modifier = Modifier
) {
    if (dataPoints.size < 2) return

    val minVal = dataPoints.minOrNull() ?: 0.0
    val maxVal = (dataPoints.maxOrNull() ?: 1.0).let { if (it == minVal) it + 1.0 else it }
    val range = maxVal - minVal

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(dataPoints) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (dataPoints.size - 1)

        val path = Path()
        val fillPath = Path()

        dataPoints.forEachIndexed { i, point ->
            val normY = ((point - minVal) / range).toFloat()
            val x = i * stepX
            val y = height - (normY * height * animProgress.value)

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(width, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun CashFlowBarChart(
    incomeValues: List<Double>,
    expenseValues: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val maxIncome = incomeValues.maxOrNull() ?: 1.0
    val maxExpense = expenseValues.maxOrNull() ?: 1.0
    val maxVal = maxOf(maxIncome, maxExpense, 1.0)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        labels.forEachIndexed { i, label ->
            val inc = incomeValues.getOrElse(i) { 0.0 }
            val exp = expenseValues.getOrElse(i) { 0.0 }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height(110.dp)
                ) {
                    val incHeightRatio = (inc / maxVal).toFloat().coerceIn(0.04f, 1f)
                    val expHeightRatio = (exp / maxVal).toFloat().coerceIn(0.04f, 1f)

                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .fillMaxHeight(incHeightRatio)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(EmeraldPrimary)
                    )
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .fillMaxHeight(expHeightRatio)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(CrimsonExpense)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BudgetProgressBar(
    percentage: Double,
    modifier: Modifier = Modifier
) {
    val clamped = percentage.coerceIn(0.0, 1.0).toFloat()
    val barColor = when {
        percentage >= 1.0 -> CrimsonExpense
        percentage >= 0.85 -> AmberWarning
        else -> EmeraldPrimary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(barColor)
        )
    }
}
