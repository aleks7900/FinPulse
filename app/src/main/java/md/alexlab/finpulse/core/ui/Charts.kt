package md.alexlab.finpulse.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import md.alexlab.finpulse.core.designsystem.AmberWarning
import md.alexlab.finpulse.core.designsystem.AnimatedAmountText
import md.alexlab.finpulse.core.designsystem.CrimsonExpense
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.designsystem.FinPulseMotion
import md.alexlab.finpulse.core.designsystem.FinPulseRadius
import md.alexlab.finpulse.core.designsystem.LocalMotionConfig
import md.alexlab.finpulse.core.designsystem.bounceClick
import kotlin.math.atan2

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
    strokeWidth: Dp = 26.dp,
    showLegend: Boolean = true
) {
    val total = segments.sumOf { it.value }.coerceAtLeast(1.0)
    val motion = LocalMotionConfig.current
    val animationProgress = remember { Animatable(if (motion.isReducedMotion) 1f else 0f) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(segments) {
        selectedIndex = null
        if (!motion.isReducedMotion) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = motion.scaleDuration(850),
                    easing = FinPulseMotion.EmphasizedDecelerateEasing
                )
            )
        } else {
            animationProgress.snapTo(1f)
        }
    }

    val displayTitle = selectedIndex?.let { segments.getOrNull(it)?.label } ?: centerTitle
    val displayValue = selectedIndex?.let {
        val seg = segments.getOrNull(it)
        if (seg != null) {
            val pct = (seg.value / total * 100).toInt()
            "$pct%"
        } else centerValue
    } ?: centerValue

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(segments) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            // Normalize angle so -90 deg (top) is 0
                            angleDeg = (angleDeg + 90f + 360f) % 360f

                            var runningSweep = 0f
                            var hitIndex: Int? = null
                            for (i in segments.indices) {
                                val sweep = ((segments[i].value / total) * 360f).toFloat()
                                if (angleDeg >= runningSweep && angleDeg <= runningSweep + sweep) {
                                    hitIndex = i
                                    break
                                }
                                runningSweep += sweep
                            }
                            selectedIndex = if (selectedIndex == hitIndex) null else hitIndex
                        }
                    }
            ) {
                val strokePx = strokeWidth.toPx()
                val diameter = size.minDimension - strokePx - 20f
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                var currentAngle = -90f

                for ((index, seg) in segments.withIndex()) {
                    val isSelected = selectedIndex == index
                    val sweep = ((seg.value / total) * 360f * animationProgress.value).toFloat()
                    if (sweep > 0.5f) {
                        val currentStroke = if (isSelected) strokePx + 10f else strokePx

                        // Draw soft glow behind selected arc
                        if (isSelected && motion.enableContinuousEffects) {
                            drawArc(
                                color = seg.color.copy(alpha = 0.35f),
                                startAngle = currentAngle - 1f,
                                sweepAngle = sweep + 0.5f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = currentStroke + 8f, cap = StrokeCap.Round)
                            )
                        }

                        drawArc(
                            color = seg.color,
                            startAngle = currentAngle,
                            sweepAngle = (sweep - 1.5f).coerceAtLeast(0.1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = currentStroke, cap = StrokeCap.Round)
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
                    text = displayTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                AnimatedAmountText(
                    amountText = displayValue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (showLegend && segments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                segments.forEachIndexed { idx, seg ->
                    val isSelected = selectedIndex == idx
                    val pillBg by animateColorAsState(
                        targetValue = if (isSelected) seg.color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        animationSpec = tween(200),
                        label = "legendPillBg"
                    )

                    Surface(
                        shape = RoundedCornerShape(FinPulseRadius.full),
                        color = pillBg,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, seg.color) else null,
                        modifier = Modifier
                            .bounceClick(scaleDown = 0.94f) {
                                selectedIndex = if (selectedIndex == idx) null else idx
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(seg.color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = seg.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern smooth sparkline chart with progressive drawing and interactive touch scrubber.
 */
@Composable
fun SparklineChart(
    dataPoints: List<Double>,
    lineColor: Color = EmeraldPrimary,
    modifier: Modifier = Modifier,
    enableScrubber: Boolean = true
) {
    if (dataPoints.size < 2) return

    val motion = LocalMotionConfig.current
    val minVal = dataPoints.minOrNull() ?: 0.0
    val maxVal = (dataPoints.maxOrNull() ?: 1.0).let { if (it == minVal) it + 1.0 else it }
    val range = maxVal - minVal

    val animProgress = remember { Animatable(if (motion.isReducedMotion) 1f else 0f) }
    LaunchedEffect(dataPoints) {
        if (!motion.isReducedMotion) {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = motion.scaleDuration(750),
                    easing = FinPulseMotion.EmphasizedDecelerateEasing
                )
            )
        } else {
            animProgress.snapTo(1f)
        }
    }

    var scrubIndex by remember { mutableStateOf<Int?>(null) }
    var scrubNormalizedX by remember { mutableFloatStateOf(0f) }
    val scrubberLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dataPoints, enableScrubber) {
                    if (enableScrubber) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val stepX = size.width / (dataPoints.size - 1)
                                val idx = (offset.x / stepX).toInt().coerceIn(0, dataPoints.lastIndex)
                                scrubIndex = idx
                                scrubNormalizedX = offset.x.coerceIn(0f, size.width.toFloat())
                            },
                            onDrag = { change, _ ->
                                val stepX = size.width / (dataPoints.size - 1)
                                val idx = (change.position.x / stepX).toInt().coerceIn(0, dataPoints.lastIndex)
                                scrubIndex = idx
                                scrubNormalizedX = change.position.x.coerceIn(0f, size.width.toFloat())
                            },
                            onDragEnd = { scrubIndex = null },
                            onDragCancel = { scrubIndex = null }
                        )
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val stepX = width / (dataPoints.size - 1)

            val points = dataPoints.mapIndexed { i, point ->
                val normY = ((point - minVal) / range).toFloat()
                val x = i * stepX
                val y = height - (normY * (height - 14.dp.toPx())) - 7.dp.toPx()
                Offset(x, y)
            }

            val path = Path()
            val fillPath = Path()

            if (points.isNotEmpty()) {
                path.moveTo(points[0].x, points[0].y)
                fillPath.moveTo(points[0].x, height)
                fillPath.lineTo(points[0].x, points[0].y)

                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val controlX = (p0.x + p1.x) / 2f
                    path.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                    fillPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                }

                fillPath.lineTo(points.last().x, height)
                fillPath.close()

                // Progressive reveal clip along the width
                clipRect(right = width * animProgress.value) {
                    // Soft atmospheric fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Smooth line
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Draw end point indicator
                if (animProgress.value >= 0.95f) {
                    val lastPoint = points.last()
                    drawCircle(
                        color = lineColor.copy(alpha = 0.35f),
                        radius = 7.dp.toPx(),
                        center = lastPoint
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 3.5.dp.toPx(),
                        center = lastPoint
                    )
                }

                // Interactive Scrubber Indicator Line and Dot
                scrubIndex?.let { idx ->
                    val scrubPoint = points[idx]
                    drawLine(
                        color = scrubberLineColor,
                        start = Offset(scrubPoint.x, 0f),
                        end = Offset(scrubPoint.x, height),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = lineColor.copy(alpha = 0.45f),
                        radius = 9.dp.toPx(),
                        center = scrubPoint
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.5.dp.toPx(),
                        center = scrubPoint
                    )
                }
            }
        }

        // Floating scrubber tooltip
        scrubIndex?.let { idx ->
            val value = dataPoints[idx]
            val pctX = idx.toFloat() / dataPoints.lastIndex.coerceAtLeast(1)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .graphicsLayer {
                        translationX = (pctX * 120f) - 60f
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(FinPulseRadius.sm),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, lineColor.copy(alpha = 0.4f)),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = "%.2f".format(value),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Modern comparative income/expense bar chart with staggered growth and interactive tooltips.
 */
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
    val motion = LocalMotionConfig.current

    val animProgress = remember { Animatable(if (motion.isReducedMotion) 1f else 0f) }
    LaunchedEffect(incomeValues, expenseValues) {
        if (!motion.isReducedMotion) {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = motion.scaleDuration(700),
                    easing = FinPulseMotion.EmphasizedDecelerateEasing
                )
            )
        } else {
            animProgress.snapTo(1f)
        }
    }

    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
        // Floating comparison readout if a bar is selected
        selectedBarIndex?.let { idx ->
            val inc = incomeValues.getOrElse(idx) { 0.0 }
            val exp = expenseValues.getOrElse(idx) { 0.0 }
            val net = inc - exp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = labels.getOrElse(idx) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Net: ${if (net >= 0) "+%.0f".format(net) else "%.0f".format(net)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (net >= 0) EmeraldPrimary else CrimsonExpense
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            labels.forEachIndexed { i, label ->
                val inc = incomeValues.getOrElse(i) { 0.0 }
                val exp = expenseValues.getOrElse(i) { 0.0 }
                val isSelected = selectedBarIndex == i

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick(scaleDown = 0.94f) {
                            selectedBarIndex = if (selectedBarIndex == i) null else i
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.height(115.dp)
                    ) {
                        val incHeightRatio = ((inc / maxVal).toFloat() * animProgress.value).coerceIn(0.04f, 1f)
                        val expHeightRatio = ((exp / maxVal).toFloat() * animProgress.value).coerceIn(0.04f, 1f)

                        val barWidth = if (isSelected) 13.dp else 11.dp

                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .fillMaxHeight(incHeightRatio)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(EmeraldPrimary, EmeraldPrimary.copy(alpha = 0.7f))
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .fillMaxHeight(expHeightRatio)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(CrimsonExpense, CrimsonExpense.copy(alpha = 0.7f))
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Modern animated budget progress bar with multi-threshold coloring and spring physics.
 */
@Composable
fun BudgetProgressBar(
    percentage: Double,
    modifier: Modifier = Modifier
) {
    val clamped = percentage.coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = FinPulseMotion.springGentle,
        label = "budgetProgressAnim"
    )

    val targetBarColor = when {
        percentage >= 1.0 -> CrimsonExpense
        percentage >= 0.85 -> AmberWarning
        else -> EmeraldPrimary
    }

    val animatedBarColor by animateColorAsState(
        targetValue = targetBarColor,
        animationSpec = tween(FinPulseMotion.DurationNormal),
        label = "budgetColorAnim"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp),
        shape = RoundedCornerShape(FinPulseRadius.full),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(FinPulseRadius.full))
                .background(animatedBarColor)
        )
    }
}
