package md.alexlab.finpulse.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import md.alexlab.finpulse.core.designsystem.FinPulseRadius
import md.alexlab.finpulse.core.designsystem.bounceClick

/**
 * Premium container surface with subtle hairline border and soft depth.
 */
@Composable
fun FinPulseCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(FinPulseRadius.xl),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    borderWidth: Dp = 1.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enableBounce: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        if (enableBounce) {
            Modifier.bounceClick(scaleDown = 0.98f, onClick = onClick)
        } else {
            Modifier.clickable(onClick = onClick)
        }
    } else {
        Modifier
    }

    val finalBorderModifier = if (border != null) {
        Modifier.border(border, shape)
    } else {
        Modifier.border(borderWidth, borderColor, shape)
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .then(finalBorderModifier)
            .then(clickModifier),
        shape = shape,
        color = backgroundColor,
        tonalElevation = 2.dp
    ) {
        Box(content = content)
    }
}

/**
 * Premium gradient card for highlight sections, dashboards, and featured stats.
 */
@Composable
fun FinPulseGradientCard(
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(FinPulseRadius.xl),
    borderColor: Color = Color.White.copy(alpha = 0.15f),
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.bounceClick(scaleDown = 0.98f, onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(gradientColors))
            .border(borderWidth, borderColor, shape)
            .then(clickModifier),
        content = content
    )
}
