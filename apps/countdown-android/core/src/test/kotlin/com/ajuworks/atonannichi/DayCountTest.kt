package com.ajuworks.atonannichi

import com.ajuworks.atonannichi.core.AfterMode
import com.ajuworks.atonannichi.core.Countdown
import com.ajuworks.atonannichi.core.DayCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DayCountTest {
    private fun d(y: Int, m: Int, day: Int): LocalDate = LocalDate.of(y, m, day)

    @Test
    fun todayTomorrowYesterday() {
        val today = d(2026, 9, 18)
        assertEquals(Countdown.Today, DayCount.of(today, today, AfterMode.COUNT_UP))
        assertEquals(Countdown.Until(1), DayCount.of(d(2026, 9, 19), today, AfterMode.COUNT_UP))
        assertEquals(Countdown.Since(1), DayCount.of(d(2026, 9, 17), today, AfterMode.COUNT_UP))
        assertEquals(Countdown.Ended, DayCount.of(d(2026, 9, 17), today, AfterMode.END))
        assertEquals(Countdown.Until(42), DayCount.of(d(2026, 10, 30), today, AfterMode.COUNT_UP))
    }

    @Test
    fun yearBoundary() {
        assertEquals(1, DayCount.daysUntil(d(2027, 1, 1), d(2026, 12, 31)))
        assertEquals(365, DayCount.daysUntil(d(2027, 12, 31), d(2026, 12, 31)))
    }

    @Test
    fun leapYear() {
        assertEquals(2, DayCount.daysUntil(d(2028, 3, 1), d(2028, 2, 28)))
        assertEquals(1, DayCount.daysUntil(d(2027, 3, 1), d(2027, 2, 28)))
        assertEquals(366, DayCount.daysUntil(d(2029, 1, 1), d(2028, 1, 1)))
        assertEquals(Countdown.Today, DayCount.of(d(2028, 2, 29), d(2028, 2, 29), AfterMode.END))
    }

    @Test
    fun timeZonesUseLocalDate() {
        // 同じ瞬間でも、東京ではもう翌日・ロサンゼルスではまだ前日
        val instant = LocalDateTime.of(2026, 10, 29, 23, 30).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val tokyo = DayCount.today(ZoneId.of("Asia/Tokyo"), instant)
        val la = DayCount.today(ZoneId.of("America/Los_Angeles"), instant)
        assertEquals(d(2026, 10, 30), tokyo)
        assertEquals(d(2026, 10, 29), la)
        assertEquals(Countdown.Today, DayCount.of(d(2026, 10, 30), tokyo, AfterMode.COUNT_UP))
        assertEquals(Countdown.Until(1), DayCount.of(d(2026, 10, 30), la, AfterMode.COUNT_UP))
    }

    @Test
    fun sortPutsUpcomingFirstAndPastLast() {
        val today = d(2026, 9, 18)
        val keys = listOf(d(2026, 9, 10), d(2026, 12, 1), d(2026, 9, 18), d(2026, 9, 20)).sortedBy { DayCount.sortKey(it, today) }
        assertEquals(listOf(d(2026, 9, 18), d(2026, 9, 20), d(2026, 12, 1), d(2026, 9, 10)), keys)
        assertTrue(DayCount.sortKey(d(2026, 9, 17), today) < DayCount.sortKey(d(2020, 1, 1), today))
    }
}
