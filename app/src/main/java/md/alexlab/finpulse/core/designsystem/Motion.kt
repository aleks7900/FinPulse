package md.alexlab.finpulse.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch

object FinPulseMotion {
    // Curated spring animation specs
    val springStandard = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val springGentle = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    val springSnappy = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val springBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // Screen navigation transitions
    val screenEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
    ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))

    val screenExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -(fullWidth * 0.10f).toInt() },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
    ) + fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))

    val screenPopEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> -(fullWidth * 0.10f).toInt() },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
    ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))

    val screenPopExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
    ) + fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
}

/**
 * Tactile spring bounce microinteraction for buttons, cards, and interactive chips.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(scaleDown) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    scope.launch {
                        scale.animateTo(scaleDown, FinPulseMotion.springSnappy)
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
 * Shimmer skeleton loading effect for data placeholders.
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
        initialValue = -300f,
        targetValue = 1300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 300f, translateAnim + 300f)
    )

    val mod = if (shape != null) this.clip(shape) else this
    mod.background(brush)
}

/**
 * Animated balance / number rolling transition component.
 */
@Composable
fun AnimatedAmountText(
    amountText: String,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Bold,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = amountText,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.95f, animationSpec = FinPulseMotion.springStandard))
                .togetherWith(
                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                            scaleOut(targetScale = 0.95f, animationSpec = FinPulseMotion.springStandard)
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
