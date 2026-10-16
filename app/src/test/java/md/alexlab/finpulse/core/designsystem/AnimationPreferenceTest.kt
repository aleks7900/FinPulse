package md.alexlab.finpulse.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationPreferenceTest {

    @Test
    fun `fromKey parses valid keys case-insensitively`() {
        assertEquals(AnimationPreference.FULL, AnimationPreference.fromKey("FULL"))
        assertEquals(AnimationPreference.FULL, AnimationPreference.fromKey("full"))
        assertEquals(AnimationPreference.BALANCED, AnimationPreference.fromKey("BALANCED"))
        assertEquals(AnimationPreference.BALANCED, AnimationPreference.fromKey("balanced"))
        assertEquals(AnimationPreference.MINIMAL, AnimationPreference.fromKey("MINIMAL"))
        assertEquals(AnimationPreference.MINIMAL, AnimationPreference.fromKey("minimal"))
        assertEquals(AnimationPreference.SYSTEM_DEFAULT, AnimationPreference.fromKey("SYSTEM"))
        assertEquals(AnimationPreference.SYSTEM_DEFAULT, AnimationPreference.fromKey("system"))
    }

    @Test
    fun `fromKey returns SYSTEM_DEFAULT for null or unknown keys`() {
        assertEquals(AnimationPreference.SYSTEM_DEFAULT, AnimationPreference.fromKey(null))
        assertEquals(AnimationPreference.SYSTEM_DEFAULT, AnimationPreference.fromKey(""))
        assertEquals(AnimationPreference.SYSTEM_DEFAULT, AnimationPreference.fromKey("UNKNOWN_VALUE"))
    }

    @Test
    fun `scaleDuration scales durations accurately for full motion`() {
        val config = MotionConfig(
            preference = AnimationPreference.FULL,
            durationMultiplier = 1.0f,
            isReducedMotion = false
        )

        assertEquals(300, config.scaleDuration(300))
        assertEquals(150, config.scaleDuration(150))
        assertEquals(1, config.scaleDuration(1))
        assertTrue(config.useSprings)
    }

    @Test
    fun `scaleDuration scales durations accurately for balanced motion`() {
        val config = MotionConfig(
            preference = AnimationPreference.BALANCED,
            durationMultiplier = 0.8f,
            isReducedMotion = false
        )

        assertEquals(240, config.scaleDuration(300))
        assertEquals(80, config.scaleDuration(100))
        assertTrue(config.useSprings)
    }

    @Test
    fun `scaleDuration returns zero and disables springs for reduced motion`() {
        val config = MotionConfig(
            preference = AnimationPreference.MINIMAL,
            durationMultiplier = 0.0f,
            isReducedMotion = true
        )

        assertEquals(0, config.scaleDuration(500))
        assertEquals(0, config.scaleDuration(200))
        assertFalse(config.useSprings)
    }

    @Test
    fun `motion config attributes match expected design system levels`() {
        val fullConfig = MotionConfig(
            preference = AnimationPreference.FULL,
            durationMultiplier = 1.0f,
            springDampingMultiplier = 1.0f,
            enableBouncing = true,
            enableParallax = true,
            enableContinuousEffects = true,
            enableStaggered = true,
            isReducedMotion = false
        )
        assertTrue(fullConfig.enableBouncing)
        assertTrue(fullConfig.enableParallax)
        assertTrue(fullConfig.enableContinuousEffects)
        assertTrue(fullConfig.enableStaggered)

        val balancedConfig = MotionConfig(
            preference = AnimationPreference.BALANCED,
            durationMultiplier = 0.8f,
            springDampingMultiplier = 1.15f,
            enableBouncing = false,
            enableParallax = false,
            enableContinuousEffects = true,
            enableStaggered = true,
            isReducedMotion = false
        )
        assertFalse(balancedConfig.enableBouncing)
        assertFalse(balancedConfig.enableParallax)
        assertTrue(balancedConfig.enableContinuousEffects)
        assertTrue(balancedConfig.enableStaggered)
    }
}
