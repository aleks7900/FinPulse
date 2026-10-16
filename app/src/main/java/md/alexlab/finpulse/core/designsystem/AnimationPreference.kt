package md.alexlab.finpulse.core.designsystem

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * User-configurable animation preference levels for FinPulse.
 */
enum class AnimationPreference(val key: String) {
    FULL("FULL"),
    BALANCED("BALANCED"),
    MINIMAL("MINIMAL"),
    SYSTEM_DEFAULT("SYSTEM");

    companion object {
        fun fromKey(key: String?): AnimationPreference {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: SYSTEM_DEFAULT
        }
    }
}

/**
 * Resolved motion configuration reflecting user preference, battery constraints,
 * and Android system-level reduced-motion/animation-scale accessibility settings.
 */
@Immutable
data class MotionConfig(
    val preference: AnimationPreference = AnimationPreference.FULL,
    val durationMultiplier: Float = 1.0f,
    val springDampingMultiplier: Float = 1.0f,
    val enableBouncing: Boolean = true,
    val enableParallax: Boolean = true,
    val enableContinuousEffects: Boolean = true,
    val enableStaggered: Boolean = true,
    val isReducedMotion: Boolean = false
) {
    /**
     * Scale a duration in milliseconds by the active motion configuration multiplier.
     */
    fun scaleDuration(durationMs: Int): Int {
        if (isReducedMotion) return 0
        return (durationMs * durationMultiplier).toInt().coerceAtLeast(1)
    }

    /**
     * Determine whether rich physics spring effects should be applied.
     */
    val useSprings: Boolean
        get() = !isReducedMotion && durationMultiplier > 0.05f
}

/**
 * CompositionLocal providing active motion parameters to all Compose nodes.
 */
val LocalMotionConfig = compositionLocalOf { MotionConfig() }

/**
 * Inspect Android system animation settings to determine whether animations are reduced or disabled.
 */
fun isSystemReducedMotion(context: Context): Boolean {
    return try {
        val resolver = context.contentResolver
        val transitionScale = Settings.Global.getFloat(
            resolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            1.0f
        )
        val animatorScale = Settings.Global.getFloat(
            resolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        transitionScale <= 0.05f || animatorScale <= 0.05f
    } catch (_: Exception) {
        false
    }
}

/**
 * Resolve effective MotionConfig given the user's preference key and context.
 */
fun resolveMotionConfig(preferenceKey: String?, context: Context): MotionConfig {
    val preference = AnimationPreference.fromKey(preferenceKey)
    val systemReduced = isSystemReducedMotion(context)

    return when (preference) {
        AnimationPreference.FULL -> {
            if (systemReduced) {
                // If system globally disables animations, respect accessibility
                MotionConfig(
                    preference = AnimationPreference.FULL,
                    durationMultiplier = 0.0f,
                    enableBouncing = false,
                    enableParallax = false,
                    enableContinuousEffects = false,
                    enableStaggered = false,
                    isReducedMotion = true
                )
            } else {
                MotionConfig(
                    preference = AnimationPreference.FULL,
                    durationMultiplier = 1.0f,
                    springDampingMultiplier = 1.0f,
                    enableBouncing = true,
                    enableParallax = true,
                    enableContinuousEffects = true,
                    enableStaggered = true,
                    isReducedMotion = false
                )
            }
        }
        AnimationPreference.BALANCED -> {
            if (systemReduced) {
                MotionConfig(
                    preference = AnimationPreference.BALANCED,
                    durationMultiplier = 0.0f,
                    enableBouncing = false,
                    enableParallax = false,
                    enableContinuousEffects = false,
                    enableStaggered = false,
                    isReducedMotion = true
                )
            } else {
                MotionConfig(
                    preference = AnimationPreference.BALANCED,
                    durationMultiplier = 0.8f,
                    springDampingMultiplier = 1.15f,
                    enableBouncing = false,
                    enableParallax = false,
                    enableContinuousEffects = true,
                    enableStaggered = true,
                    isReducedMotion = false
                )
            }
        }
        AnimationPreference.MINIMAL -> {
            MotionConfig(
                preference = AnimationPreference.MINIMAL,
                durationMultiplier = 0.0f,
                enableBouncing = false,
                enableParallax = false,
                enableContinuousEffects = false,
                enableStaggered = false,
                isReducedMotion = true
            )
        }
        AnimationPreference.SYSTEM_DEFAULT -> {
            if (systemReduced) {
                MotionConfig(
                    preference = AnimationPreference.SYSTEM_DEFAULT,
                    durationMultiplier = 0.0f,
                    enableBouncing = false,
                    enableParallax = false,
                    enableContinuousEffects = false,
                    enableStaggered = false,
                    isReducedMotion = true
                )
            } else {
                MotionConfig(
                    preference = AnimationPreference.SYSTEM_DEFAULT,
                    durationMultiplier = 1.0f,
                    springDampingMultiplier = 1.0f,
                    enableBouncing = true,
                    enableParallax = true,
                    enableContinuousEffects = true,
                    enableStaggered = true,
                    isReducedMotion = false
                )
            }
        }
    }
}

/**
 * Composable provider wrapping UI hierarchies with the resolved MotionConfig.
 */
@Composable
fun ProvideMotionConfig(
    preferenceKey: String?,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val config = remember(preferenceKey) {
        resolveMotionConfig(preferenceKey, context)
    }

    CompositionLocalProvider(LocalMotionConfig provides config) {
        content()
    }
}

/**
 * Accessor for the active MotionConfig.
 */
val motionConfig: MotionConfig
    @Composable
    @ReadOnlyComposable
    get() = LocalMotionConfig.current
