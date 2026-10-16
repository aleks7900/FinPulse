package md.alexlab.finpulse.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Unified Motion Design System tokens and specifications for FinPulse.
 */
object FinPulseMotion {

    // ── DURATION TOKENS (ms) ──────────────────────────────────────────────────
    const val DurationInstant = 0
    const val DurationUltraFast = 120
    const val DurationFast = 200
    const val DurationNormal = 320
    const val DurationDeliberate = 460
    const val DurationSpacious = 650

    // ── EASING CURVES (Material 3 Motion Specs) ──────────────────────────────
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardEasing = FastOutSlowInEasing
    val StandardDecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val StandardAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)

    // ── SPRING PHYSICS TOKENS ────────────────────────────────────────────────
    val springStandard: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val springGentle: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    val springSnappy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val springBouncy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val springTactile: SpringSpec<Float> = spring(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMediumLow
    )

    val springSheet: SpringSpec<Float> = spring(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMediumLow
    )

    val springElastic: SpringSpec<Float> = spring(
        dampingRatio = 0.62f,
        stiffness = Spring.StiffnessLow
    )

    // ── SCREEN NAVIGATION TRANSITIONS ─────────────────────────────────────────

    /**
     * Shared-axis container enter transition with subtle scale & slide.
     */
    val screenEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> (fullWidth * 0.14f).toInt() },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
    ) + fadeIn(
        animationSpec = tween(DurationFast, easing = EmphasizedDecelerateEasing)
    ) + scaleIn(
        initialScale = 0.96f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
    )

    val screenExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -(fullWidth * 0.08f).toInt() },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
    ) + fadeOut(
        animationSpec = tween(160, easing = EmphasizedAccelerateEasing)
    ) + scaleOut(
        targetScale = 0.98f,
        animationSpec = tween(160, easing = EmphasizedAccelerateEasing)
    )

    val screenPopEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> -(fullWidth * 0.08f).toInt() },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
    ) + fadeIn(
        animationSpec = tween(DurationFast, easing = EmphasizedDecelerateEasing)
    ) + scaleIn(
        initialScale = 0.98f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
    )

    val screenPopExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> (fullWidth * 0.14f).toInt() },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
    ) + fadeOut(
        animationSpec = tween(160, easing = EmphasizedAccelerateEasing)
    ) + scaleOut(
        targetScale = 0.96f,
        animationSpec = tween(160, easing = EmphasizedAccelerateEasing)
    )

    /**
     * Direction-aware bottom navigation destination transitions.
     */
    fun tabEnter(slideLeft: Boolean): EnterTransition {
        val directionSign = if (slideLeft) 1 else -1
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> (fullWidth * 0.12f * directionSign).toInt() },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
        ) + fadeIn(animationSpec = tween(DurationFast, easing = EmphasizedDecelerateEasing))
    }

    fun tabExit(slideLeft: Boolean): ExitTransition {
        val directionSign = if (slideLeft) -1 else 1
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> (fullWidth * 0.12f * directionSign).toInt() },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
        ) + fadeOut(animationSpec = tween(160, easing = EmphasizedAccelerateEasing))
    }

    /**
     * Modal bottom sheet entrance & exit.
     */
    val modalEnter: EnterTransition = slideInVertically(
        initialOffsetY = { it },
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    ) + fadeIn(animationSpec = tween(DurationFast, easing = EmphasizedDecelerateEasing))

    val modalExit: ExitTransition = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)
    ) + fadeOut(animationSpec = tween(180, easing = EmphasizedAccelerateEasing))

    /**
     * Dialog morphing transitions.
     */
    val dialogEnter: EnterTransition = scaleIn(
        initialScale = 0.88f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
    ) + fadeIn(animationSpec = tween(DurationFast, easing = EmphasizedDecelerateEasing))

    val dialogExit: ExitTransition = scaleOut(
        targetScale = 0.88f,
        animationSpec = tween(160, easing = EmphasizedAccelerateEasing)
    ) + fadeOut(animationSpec = tween(160, easing = EmphasizedAccelerateEasing))
}

/**
 * Tactile spring bounce micro-interaction for buttons, cards, and interactive chips.
 * Gracefully adapts to active MotionConfig and reduced-motion preferences.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.96f,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val motion = LocalMotionConfig.current
    if (!enabled || motion.isReducedMotion || !motion.enableBouncing) {
        return@composed if (onClick != null) {
            this.pointerInput(enabled) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        val upOrCancel = waitForUpOrCancellation()
                        if (upOrCancel != null) {
                            onClick()
                        }
                    }
                }
            }
        } else {
            this
        }
    }

    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(scaleDown, enabled) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    scope.launch {
                        scale.animateTo(scaleDown, FinPulseMotion.springTactile)
                    }
                    val upOrCancel = waitForUpOrCancellation()
                    scope.launch {
                        scale.animateTo(1f, FinPulseMotion.springBouncy)
                    }
                    if (upOrCancel != null && onClick != null) {
                        onClick()
                    }
                }
            }
        }
}

/**
 * Staggered entrance animation for lists, dashboard cards, and item sequences.
 * Index-based progressive reveal with smooth translateY and alpha fade.
 */
fun Modifier.staggeredEntrance(
    index: Int = 0,
    baseDelayMs: Int = 35,
    durationMs: Int = FinPulseMotion.DurationNormal,
    offsetDp: Dp = 18.dp
): Modifier = composed {
    val motion = LocalMotionConfig.current
    if (motion.isReducedMotion || !motion.enableStaggered) {
        return@composed this
    }

    val density = LocalDensity.current
    val offsetPx = with(density) { offsetDp.toPx() }
    val alphaAnim = remember { Animatable(0f) }
    val translationAnim = remember { Animatable(offsetPx) }

    LaunchedEffect(Unit) {
        val delayTime = (index.coerceIn(0, 8) * baseDelayMs * motion.durationMultiplier).toLong()
        delay(delayTime)
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = motion.scaleDuration(durationMs),
                    easing = FinPulseMotion.EmphasizedDecelerateEasing
                )
            )
        }
        launch {
            translationAnim.animateTo(
                targetValue = 0f,
                animationSpec = FinPulseMotion.springStandard
            )
        }
    }

    this.graphicsLayer {
        alpha = alphaAnim.value
        translationY = translationAnim.value
    }
}

/**
 * Enhanced Shimmer skeleton loading effect for data placeholders.
 * Smooth diagonal sweep with theme-aware colors and high performance.
 */
fun Modifier.shimmerEffect(
    shape: Shape? = null
): Modifier = composed {
    val isDark = MaterialTheme.colorScheme.background.value == DarkBackground.value ||
            MaterialTheme.colorScheme.surface.value == DarkSurface.value

    val shimmerColors = if (isDark) {
        listOf(
            ShimmerDarkBase,
            ShimmerDarkHighlight,
            ShimmerDarkBase
        )
    } else {
        listOf(
            ShimmerLightBase,
            ShimmerLightHighlight,
            ShimmerLightBase
        )
    }

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -350f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 320f, translateAnim + 320f)
    )

    val mod = if (shape != null) this.clip(shape) else this
    mod.background(brush)
}

/**
 * Soft ambient glow / breathing lighting effect modifier for important cards and buttons.
 */
fun Modifier.glowPulse(
    color: Color = EmeraldPrimary,
    enabled: Boolean = true,
    minAlpha: Float = 0.12f,
    maxAlpha: Float = 0.38f,
    durationMs: Int = 1800
): Modifier = composed {
    val motion = LocalMotionConfig.current
    if (!enabled || motion.isReducedMotion || !motion.enableContinuousEffects) {
        return@composed this
    }

    val transition = rememberInfiniteTransition(label = "glowPulseTransition")
    val glowAlpha by transition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    this.drawBehind {
        drawCircle(
            color = color.copy(alpha = glowAlpha),
            radius = size.maxDimension / 2f + 8.dp.toPx()
        )
    }
}

/**
 * Animated balance / number rolling transition component with digit/content slide.
 */
@Composable
fun AnimatedAmountText(
    amountText: String,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Bold,
    modifier: Modifier = Modifier
) {
    val motion = LocalMotionConfig.current
    if (motion.isReducedMotion) {
        Text(
            text = amountText,
            style = style,
            fontWeight = fontWeight,
            color = color,
            modifier = modifier
        )
        return
    }

    AnimatedContent(
        targetState = amountText,
        transitionSpec = {
            (slideInVertically(
                initialOffsetY = { fullHeight -> (fullHeight * 0.35f).toInt() },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(FinPulseMotion.DurationFast, easing = FinPulseMotion.EmphasizedDecelerateEasing)))
                .togetherWith(
                    slideOutVertically(
                        targetOffsetY = { fullHeight -> -(fullHeight * 0.35f).toInt() },
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeOut(animationSpec = tween(150, easing = FinPulseMotion.EmphasizedAccelerateEasing))
                )
        },
        label = "AnimatedAmountText",
        modifier = modifier
    ) { targetText ->
        Text(
            text = targetText,
            style = style,
            fontWeight = fontWeight,
            color = color
        )
    }
}

/**
 * High-precision animated numeric balance counter that smoothly counts between values.
 */
@Composable
fun AnimatedRollingCounter(
    targetValue: Double,
    prefix: String = "$",
    suffix: String = "",
    decimals: Int = 2,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Bold,
    modifier: Modifier = Modifier
) {
    val motion = LocalMotionConfig.current
    if (motion.isReducedMotion) {
        val formatted = String.format(Locale.US, "%.${decimals}f", targetValue)
        Text(
            text = "$prefix$formatted$suffix",
            style = style,
            fontWeight = fontWeight,
            color = color,
            modifier = modifier
        )
        return
    }

    val animatedValue = remember { Animatable(targetValue.toFloat()) }

    LaunchedEffect(targetValue) {
        animatedValue.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = FinPulseMotion.springGentle
        )
    }

    val formatted = String.format(Locale.US, "%,.${decimals}f", animatedValue.value)
    Text(
        text = "$prefix$formatted$suffix",
        style = style,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}

/**
 * Lightweight celebratory particle burst for goals achieved or savings milestones reached.
 * Rendered purely via Canvas with automatic termination to prevent battery waste.
 */
@Composable
fun MilestoneCelebration(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 36,
    onFinished: () -> Unit = {}
) {
    val motion = LocalMotionConfig.current
    if (!trigger || motion.isReducedMotion || !motion.enableContinuousEffects) {
        return
    }

    val progress = remember { Animatable(0f) }

    val particles = remember(trigger) {
        val colors = listOf(
            Color(0xFFF59E0B), // Gold
            Color(0xFF10B981), // Emerald
            Color(0xFF3B82F6), // Sapphire
            Color(0xFF8B5CF6), // Purple
            Color(0xFFEC4899)  // Pink
        )
        List(particleCount) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 220f + 140f
            val size = Random.nextFloat() * 6f + 4f
            val color = colors[Random.nextInt(colors.size)]
            CelebrationParticle(angle, speed, size, color)
        }
    }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200, easing = FinPulseMotion.EmphasizedDecelerateEasing)
        )
        onFinished()
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val currentProgress = progress.value
            val alpha = (1f - currentProgress).coerceIn(0f, 1f)

            particles.forEach { p ->
                val distance = p.speed * currentProgress
                val x = center.x + cos(p.angle) * distance
                // Add gravity arc downward
                val y = center.y + sin(p.angle) * distance + (currentProgress * currentProgress * 80f)
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.size * (1f - currentProgress * 0.4f),
                    center = Offset(x, y)
                )
            }
        }
    }
}

private data class CelebrationParticle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)
