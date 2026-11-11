package md.alexlab.finpulse.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import md.alexlab.finpulse.core.designsystem.bounceClick

/**
 * Tonal container surface (Material 3 Expressive).
 *
 * Depth comes from the surfaceContainer tone ladder instead of hairline borders and shadows,
 * which reads cleaner in both themes and adapts to dynamic color. Pass [border] (or a
 * non-zero [borderWidth]) only when a card needs to call attention to itself.
 */
@Composable
fun FinPulseCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    borderWidth: Dp = 0.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enableBounce: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val resolvedBorder = border ?: if (borderWidth > 0.dp) BorderStroke(borderWidth, borderColor) else null

    if (onClick != null && !enableBounce) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = backgroundColor,
            border = resolvedBorder
        ) {
            Box(content = content)
        }
    } else {
        Surface(
            modifier = modifier
                .clip(shape)
                .then(
                    if (onClick != null) Modifier.bounceClick(scaleDown = 0.98f, onClick = onClick)
                    else Modifier
                ),
            shape = shape,
            color = backgroundColor,
            border = resolvedBorder
        ) {
            Box(content = content)
        }
    }
}

/**
 * Highlight card for featured stats and promos.
 */
@Composable
fun FinPulseGradientCard(
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    borderWidth: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(gradientColors))
            .then(if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape) else Modifier)
            .then(
                if (onClick != null) Modifier.bounceClick(scaleDown = 0.98f, onClick = onClick)
                else Modifier
            ),
        content = content
    )
}
