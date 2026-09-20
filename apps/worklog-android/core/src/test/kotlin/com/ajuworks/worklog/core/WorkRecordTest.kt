package com.ajuworks.worklog.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Spec section 31: the calculation cases that must never regress. */
class WorkRecordTest {

    private val now = stamp(2026, 9, 20, 23, 0)

    @Test
    fun `plain day shift minus one hour break is eight hours`() {
        val record = session(
            start = stamp(2026, 9, 20, 9, 0),
            end = stamp(2026, 9, 20, 18, 0),
            breaks = listOf(breakEntry(stamp(2026, 9, 20, 12, 0), stamp(2026, 9, 20, 13, 0))),
        )

        assertThat(record.grossMillis(now)).isEqualTo(hm(9))
        assertThat(record.breakMillis(now)).isEqualTo(hm(1))
        assertThat(record.netMillis(now)).isEqualTo(hm(8))
    }

    @Test
    fun `multiple breaks are summed`() {
        val record = session(
            start = stamp(2026, 9, 20, 9, 0),
            end = stamp(2026, 9, 20, 18, 0),
            breaks = listOf(
                breakEntry(stamp(2026, 9, 20, 12, 0), stamp(2026, 9, 20, 12, 45)),
                breakEntry(stamp(2026, 9, 20, 15, 0), stamp(2026, 9, 20, 15, 15)),
            ),
        )

        assertThat(record.breakMillis(now)).isEqualTo(hm(1))
        assertThat(record.netMillis(now)).isEqualTo(hm(8))
    }

    @Test
    fun `overnight shift crossing midnight`() {
        val record = session(
            start = stamp(2026, 9, 20, 22, 0),
            end = stamp(2026, 9, 21, 6, 0),
        )

        assertThat(record.netMillis(now)).isEqualTo(hm(8))
        assertThat(record.workDate).isEqualTo(date(2026, 9, 20))
    }

    @Test
    fun `overnight shift crossing a month boundary counts on the starting day`() {
        val record = session(
            start = stamp(2026, 9, 30, 22, 0),
            end = stamp(2026, 10, 1, 6, 0),
        )

        assertThat(record.netMillis(now)).isEqualTo(hm(8))
        assertThat(record.workDate).isEqualTo(date(2026, 9, 30))
    }

    @Test
    fun `overnight shift crossing a year boundary counts on the starting day`() {
        val record = session(
            start = stamp(2026, 12, 31, 22, 0),
            end = stamp(2027, 1, 1, 6, 0),
        )

        assertThat(record.netMillis(now)).isEqualTo(hm(8))
        assertThat(record.workDate).isEqualTo(date(2026, 12, 31))
    }

    @Test
    fun `shift spanning a leap day`() {
        val record = session(
            start = stamp(2028, 2, 28, 22, 0),
            end = stamp(2028, 2, 29, 6, 0),
            breaks = listOf(breakEntry(stamp(2028, 2, 29, 1, 0), stamp(2028, 2, 29, 1, 30))),
        )

        assertThat(record.netMillis(stamp(2028, 3, 1, 0, 0))).isEqualTo(hm(7, 30))
        assertThat(record.workDate).isEqualTo(date(2028, 2, 28))
    }

    @Test
    fun `leap day itself is a normal work day`() {
        val record = session(
            start = stamp(2028, 2, 29, 9, 0),
            end = stamp(2028, 2, 29, 18, 0),
        )

        assertThat(record.netMillis(stamp(2028, 3, 1, 0, 0))).isEqualTo(hm(9))
        assertThat(record.workDate).isEqualTo(date(2028, 2, 29))
    }

    @Test
    fun `running session measures elapsed time up to now`() {
        val record = session(start = stamp(2026, 9, 20, 9, 3))

        assertThat(record.isRunning).isTrue()
        assertThat(record.netMillis(stamp(2026, 9, 20, 11, 20))).isEqualTo(hm(2, 17))
    }

    @Test
    fun `running break keeps growing while the break is open`() {
        val record = session(
            start = stamp(2026, 9, 20, 9, 0),
            breaks = listOf(breakEntry(stamp(2026, 9, 20, 12, 0))),
        )

        val at1230 = stamp(2026, 9, 20, 12, 30)
        assertThat(record.isOnBreak).isTrue()
        assertThat(record.breakMillis(at1230)).isEqualTo(hm(0, 30))
        assertThat(record.netMillis(at1230)).isEqualTo(hm(3))
    }

    @Test
    fun `overlapping breaks are counted once`() {
        val record = session(
            start = stamp(2026, 9, 20, 9, 0),
            end = stamp(2026, 9, 20, 18, 0),
            breaks = listOf(
                breakEntry(stamp(2026, 9, 20, 12, 0), stamp(2026, 9, 20, 13, 0)),
                breakEntry(stamp(2026, 9, 20, 12, 30), stamp(2026, 9, 20, 13, 30)),
            ),
        )

        assertThat(record.breakMillis(now)).isEqualTo(hm(1, 30))
        assertThat(record.netMillis(now)).isEqualTo(hm(7, 30))
    }

    @Test
    fun `a break reaching past the clock-out is clamped`() {
        val record = session(
            start = stamp(2026, 9, 20, 9, 0),
            end = stamp(2026, 9, 20, 12, 0),
            breaks = listOf(breakEntry(stamp(2026, 9, 20, 11, 0), stamp(2026, 9, 20, 20, 0))),
        )

        assertThat(record.breakMillis(now)).isEqualTo(hm(1))
        assertThat(record.netMillis(now)).isEqualTo(hm(2))
    }

    @Test
    fun `net time never goes negative`() {
        val record = session(
            start = stamp(2026, 9, 20, 9, 0),
            end = stamp(2026, 9, 20, 10, 0),
            breaks = listOf(breakEntry(stamp(2026, 9, 20, 8, 0), stamp(2026, 9, 20, 20, 0))),
        )

        assertThat(record.netMillis(now)).isEqualTo(0L)
    }

    @Test
    fun `a zero length break contributes nothing`() {
        val at = stamp(2026, 9, 20, 12, 0)
        val record = session(
            start = stamp(2026, 9, 20, 9, 0),
            end = stamp(2026, 9, 20, 18, 0),
            breaks = listOf(breakEntry(at, at)),
        )

        assertThat(record.breakMillis(now)).isEqualTo(0L)
        assertThat(record.netMillis(now)).isEqualTo(hm(9))
    }

    @Test
    fun `wall clock stays put when the device timezone changes later`() {
        // Clocked in at 09:00 in Tokyo, then the device moves to London.
        val record = session(
            start = stamp(2026, 9, 20, 9, 0, TOKYO),
            end = stamp(2026, 9, 20, 18, 0, TOKYO),
        )

        assertThat(record.start.localTime).isEqualTo(time(9, 0))
        assertThat(record.end!!.localTime).isEqualTo(time(18, 0))
        assertThat(record.workDate).isEqualTo(date(2026, 9, 20))
        assertThat(record.netMillis(now)).isEqualTo(hm(9))
    }

    @Test
    fun `elapsed time is unaffected by a DST transition in the device zone`() {
        val newYork = java.time.ZoneId.of("America/New_York")
        // 2026-03-08: US clocks jump 02:00 -> 03:00.
        val record = session(
            start = stamp(2026, 3, 7, 22, 0, newYork),
            end = stamp(2026, 3, 8, 6, 0, newYork),
        )

        // Seven wall-clock-looking hours, seven real hours.
        assertThat(record.netMillis(now)).isEqualTo(hm(7))
        assertThat(record.workDate).isEqualTo(date(2026, 3, 7))
    }
}
