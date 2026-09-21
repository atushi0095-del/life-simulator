package com.ajuworks.worklog.core

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.YearMonth
import org.junit.Test

class AggregationTest {

    private val now = stamp(2026, 10, 5, 23, 0)

    private val records = listOf(
        session(stamp(2026, 9, 18, 9, 12), stamp(2026, 9, 18, 20, 31)),
        session(stamp(2026, 9, 19, 8, 54), stamp(2026, 9, 19, 17, 58)),
        session(
            stamp(2026, 9, 20, 9, 3),
            stamp(2026, 9, 20, 18, 17),
            listOf(breakEntry(stamp(2026, 9, 20, 12, 0), stamp(2026, 9, 20, 13, 0))),
        ),
        // Overnight, September into October.
        session(stamp(2026, 9, 30, 22, 0), stamp(2026, 10, 1, 6, 0)),
    )

    @Test
    fun `daily totals use the clock-in day`() {
        val today = WorkAggregator.onDate(records, date(2026, 9, 20))

        assertThat(today).hasSize(1)
        assertThat(WorkAggregator.summarize(today, now).netMillis).isEqualTo(hm(8, 14))
    }

    @Test
    fun `an overnight shift belongs to the month it started in`() {
        val september = WorkAggregator.inMonth(records, YearMonth.of(2026, 9))
        val october = WorkAggregator.inMonth(records, YearMonth.of(2026, 10))

        assertThat(september).hasSize(4)
        assertThat(october).isEmpty()
    }

    @Test
    fun `monthly summary reports days worked and totals`() {
        val summary = WorkAggregator.summarize(
            WorkAggregator.inMonth(records, YearMonth.of(2026, 9)),
            now,
        )

        assertThat(summary.dayCount).isEqualTo(4)
        assertThat(summary.sessionCount).isEqualTo(4)
        assertThat(summary.breakMillis).isEqualTo(hm(1))
        // 11:19 + 09:04 + 09:14 + 08:00 gross, minus one hour of break.
        assertThat(summary.grossMillis).isEqualTo(hm(37, 37))
        assertThat(summary.netMillis).isEqualTo(hm(36, 37))
    }

    @Test
    fun `two sessions on one day count as one worked day`() {
        val split = listOf(
            session(stamp(2026, 9, 21, 9, 0), stamp(2026, 9, 21, 12, 0)),
            session(stamp(2026, 9, 21, 13, 0), stamp(2026, 9, 21, 18, 0)),
        )

        val summary = WorkAggregator.summarize(split, now)
        assertThat(summary.dayCount).isEqualTo(1)
        assertThat(summary.sessionCount).isEqualTo(2)
        assertThat(summary.netMillis).isEqualTo(hm(8))
    }

    @Test
    fun `week starting monday covers monday through sunday`() {
        // 2026-09-20 is a Sunday; the Monday-based week began on 2026-09-14.
        val start = WorkAggregator.startOfWeek(date(2026, 9, 20), DayOfWeek.MONDAY)
        assertThat(start).isEqualTo(date(2026, 9, 14))

        val week = WorkAggregator.inWeekOf(records, date(2026, 9, 20), DayOfWeek.MONDAY)
        assertThat(week).hasSize(3)
    }

    @Test
    fun `week starting sunday shifts the boundary`() {
        val start = WorkAggregator.startOfWeek(date(2026, 9, 20), DayOfWeek.SUNDAY)
        assertThat(start).isEqualTo(date(2026, 9, 20))

        val week = WorkAggregator.inWeekOf(records, date(2026, 9, 20), DayOfWeek.SUNDAY)
        assertThat(week.map { it.workDate }).containsExactly(date(2026, 9, 20))
    }

    @Test
    fun `summary of nothing is all zeroes`() {
        val summary = WorkAggregator.summarize(emptyList(), now)

        assertThat(summary.netMillis).isEqualTo(0L)
        assertThat(summary.dayCount).isEqualTo(0)
    }

    @Test
    fun `deleting a session removes its time from the month total`() {
        val month = WorkAggregator.inMonth(records, YearMonth.of(2026, 9))
        val before = WorkAggregator.summarize(month, now)

        val after = WorkAggregator.summarize(month.filterNot { it.workDate == date(2026, 9, 20) }, now)

        assertThat(before.netMillis - after.netMillis).isEqualTo(hm(8, 14))
        assertThat(after.dayCount).isEqualTo(3)
    }

    @Test
    fun `a running session contributes its elapsed time to todays total`() {
        val running = listOf(session(stamp(2026, 10, 5, 9, 0)))

        val summary = WorkAggregator.summarize(running, stamp(2026, 10, 5, 12, 30))
        assertThat(summary.netMillis).isEqualTo(hm(3, 30))
    }

    // --- summaryWindow -------------------------------------------------
    // The home screen fetches one range and derives today/week/month from it.
    // If the range misses a day, a total silently under-reports, so the window
    // is pinned down here rather than trusted.

    @Test
    fun `summary window covers both the week and the month start`() {
        // 2026-10-01 is a Thursday; the Monday-based week began 2026-09-28,
        // which is BEFORE the month start - the case a month-anchored window
        // gets wrong.
        val window = WorkAggregator.summaryWindow(date(2026, 10, 1), DayOfWeek.MONDAY)

        assertThat(window.start).isEqualTo(date(2026, 9, 28))
        assertThat(window.endInclusive).isEqualTo(date(2026, 10, 1))
    }

    @Test
    fun `summary window starts at the month when the week starts later`() {
        // 2026-10-15 is a Thursday; that week began 2026-10-12, inside the
        // month, so the month start is the binding constraint.
        val window = WorkAggregator.summaryWindow(date(2026, 10, 15), DayOfWeek.MONDAY)

        assertThat(window.start).isEqualTo(date(2026, 10, 1))
        assertThat(window.endInclusive).isEqualTo(date(2026, 10, 15))
    }

    @Test
    fun `summary window honours a sunday week start`() {
        // 2026-10-01 Thursday, Sunday-based week began 2026-09-27.
        val window = WorkAggregator.summaryWindow(date(2026, 10, 1), DayOfWeek.SUNDAY)

        assertThat(window.start).isEqualTo(date(2026, 9, 27))
    }

    @Test
    fun `summary window reaches back across a year boundary`() {
        // 2027-01-01 is a Friday; the Monday-based week began 2026-12-28.
        val window = WorkAggregator.summaryWindow(date(2027, 1, 1), DayOfWeek.MONDAY)

        assertThat(window.start).isEqualTo(date(2026, 12, 28))
        assertThat(window.endInclusive).isEqualTo(date(2027, 1, 1))
    }

    @Test
    fun `summary window is a single day when the month and week both start today`() {
        // 2026-06-01 is a Monday and the first of the month.
        val window = WorkAggregator.summaryWindow(date(2026, 6, 1), DayOfWeek.MONDAY)

        assertThat(window.start).isEqualTo(date(2026, 6, 1))
        assertThat(window.endInclusive).isEqualTo(date(2026, 6, 1))
    }

    @Test
    fun `every week day of a month-start week falls inside the window`() {
        // Walk a whole year and assert the window never misses a day that
        // either the week total or the month total would need.
        var day = date(2026, 1, 1)
        while (day.isBefore(date(2027, 1, 1))) {
            for (weekStart in listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY)) {
                val window = WorkAggregator.summaryWindow(day, weekStart)
                val weekFrom = WorkAggregator.startOfWeek(day, weekStart)
                val monthFrom = day.withDayOfMonth(1)

                assertThat(window.start).isAtMost(weekFrom)
                assertThat(window.start).isAtMost(monthFrom)
                assertThat(window.endInclusive).isEqualTo(day)
            }
            day = day.plusDays(1)
        }
    }
}
