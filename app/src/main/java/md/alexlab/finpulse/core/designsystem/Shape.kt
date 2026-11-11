package md.alexlab.finpulse.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive shape scale. Rounder, softer containers; the "increased" steps
 * are used for hero cards and sheets.
 */
val FinPulseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    largeIncreased = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
    extraLargeIncreased = RoundedCornerShape(36.dp),
    extraExtraLarge = RoundedCornerShape(48.dp)
)

object FinPulseRadius {
    val xs = 8.dp
    val sm = 12.dp
    val md = 18.dp
    val lg = 22.dp
    val xl = 28.dp
    val xxl = 32.dp
    val hero = 36.dp
    val full = 999.dp
}

/** 4-pt spacing scale used by refreshed components. */
object FinPulseSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    /** Horizontal screen gutter. */
    val gutter = 16.dp
}
