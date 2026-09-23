package com.ajuworks.atonannichi

import com.ajuworks.atonannichi.core.TickSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TickScheduleTest {
    private val tokyo = ZoneId.of("Asia/Tokyo")
    private fun at(s: String, zone: ZoneId = tokyo) = LocalDateTime.parse(s).atZone(zone)

    @Test
    fun nextIsMorningOrMidnight() {
        assertEquals(at("2026-09-18T09:00"), TickSchedule.next(at("2026-09-18T08:59")))
        assertEquals(at("2026-09-19T00:00:30"), TickSchedule.next(at("2026-09-18T09:00")))
        assertEquals(at("2026-09-19T00:00:30"), TickSchedule.next(at("2026-09-18T23:59:59")))
        assertEquals(at("2026-09-19T09:00"), TickSchedule.next(at("2026-09-19T00:00:30")))
    }

    @Test
    fun yearEndAndLeapDay() {
        assertEquals(at("2027-01-01T00:00:30"), TickSchedule.next(at("2026-12-31T22:00")))
        assertEquals(at("2028-02-29T00:00:30"), TickSchedule.next(at("2028-02-28T12:00")))
    }

    @Test
    fun daylightSavingZone() {
        // 夏時間のある地域でも、次は「その地域の0時台」になる
        val ny = ZoneId.of("America/New_York")
        val next: ZonedDateTime = TickSchedule.next(at("2026-03-07T23:00", ny))
        assertEquals(at("2026-03-08T00:00:30", ny), next)
    }

    @Test
    fun notifyOnlyFromMorning() {
        assertFalse(TickSchedule.isNotifyTime(at("2026-09-18T00:00:30")))
        assertTrue(TickSchedule.isNotifyTime(at("2026-09-18T09:00")))
        assertTrue(TickSchedule.isNotifyTime(at("2026-09-18T21:00")))
    }
}
