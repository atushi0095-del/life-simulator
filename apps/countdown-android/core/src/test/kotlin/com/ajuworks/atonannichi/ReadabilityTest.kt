package com.ajuworks.atonannichi

import com.ajuworks.atonannichi.core.Readability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadabilityTest {
    @Test
    fun brighterPhotosGetDarkerScrim() {
        val dark = Readability.scrimAlpha(20.0)
        val mid = Readability.scrimAlpha(128.0)
        val bright = Readability.scrimAlpha(250.0)
        assertTrue(dark < mid && mid < bright)
        assertTrue("always some scrim", dark >= 0.18f)
        assertTrue("never opaque", bright <= 0.6f)
    }

    @Test
    fun luminanceOfWhiteAndBlack() {
        assertEquals(255.0, Readability.luminance(255, 255, 255), 0.01)
        assertEquals(0.0, Readability.luminance(0, 0, 0), 0.01)
    }
}
