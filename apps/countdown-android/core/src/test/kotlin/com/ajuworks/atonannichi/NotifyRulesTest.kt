package com.ajuworks.atonannichi

import com.ajuworks.atonannichi.core.NotifyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NotifyRulesTest {
    private val event = LocalDate.of(2026, 10, 30)
    private val all = NotifyRules.OFFSETS.fold(0) { m, o -> m or NotifyRules.bitOf(o) }

    @Test
    fun dueOnConfiguredOffsets() {
        assertEquals(30, NotifyRules.due(event, event.minusDays(30), all, 0))
        assertEquals(7, NotifyRules.due(event, event.minusDays(7), all, 0))
        assertEquals(3, NotifyRules.due(event, event.minusDays(3), all, 0))
        assertEquals(1, NotifyRules.due(event, event.minusDays(1), all, 0))
        assertEquals(0, NotifyRules.due(event, event, all, 0))
        assertNull(NotifyRules.due(event, event.minusDays(5), all, 0))
        assertNull(NotifyRules.due(event, event.plusDays(1), all, 0))
    }

    @Test
    fun respectsDisabledAndAlreadySent() {
        val onlyToday = NotifyRules.bitOf(0)
        assertNull(NotifyRules.due(event, event.minusDays(7), onlyToday, 0))
        assertNull(NotifyRules.due(event, event, all, NotifyRules.bitOf(0)))
        assertEquals(0, NotifyRules.due(event, event, all, NotifyRules.bitOf(7)))
    }

    @Test
    fun defaultsAndToggle() {
        assertTrue(NotifyRules.isOn(NotifyRules.DEFAULT, 7))
        assertTrue(NotifyRules.isOn(NotifyRules.DEFAULT, 1))
        assertTrue(NotifyRules.isOn(NotifyRules.DEFAULT, 0))
        assertFalse(NotifyRules.isOn(NotifyRules.DEFAULT, 30))
        assertTrue(NotifyRules.isOn(NotifyRules.toggle(NotifyRules.DEFAULT, 30), 30))
        assertFalse(NotifyRules.isOn(NotifyRules.toggle(NotifyRules.DEFAULT, 7), 7))
    }
}
