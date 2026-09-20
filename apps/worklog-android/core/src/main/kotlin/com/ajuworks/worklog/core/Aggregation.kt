package com.ajuworks.worklog.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Totals for a set of sessions over some period. */
data class WorkSummary(
    val netMillis: Long = 0L,
    val breakMillis: Long = 0L,
    val grossMillis: Long = 0L,
    /** Distinct calendar days with at least one session. */
    val dayCount: Int = 0,
    val sessionCount: Int = 0,
)

/**
 * Period totals.
 *
 * Every session is attributed to [WorkRecord.workDate] - the day it *started*.
 * An overnight shift therefore lands wholly in the starting day's week, month
 * and year, and never splits across a boundary. This is deliberate: splitting
 * would make a "today" figure change retroactively at midnight while the user
 * is still working.
 */
object WorkAggregator {

    fun summarize(records: List<WorkRecord>, now: Stamp): WorkSummary {
        if (records.isEmpty()) return WorkSummary()
        var net = 0L
        var breaks = 0L
        var gross = 0L
        for (record in records) {
            net += record.netMillis(now)
            breaks += record.breakMillis(now)
            gross += record.grossMillis(now)
        }
        return WorkSummary(
            netMillis = net,
            breakMillis = breaks,
            grossMillis = gross,
            dayCount = records.mapTo(HashSet()) { it.workDate }.size,
            sessionCount = records.size,
        )
    }

    fun onDate(records: List<WorkRecord>, date: LocalDate): List<WorkRecord> =
        records.filter { it.workDate == date }

    fun inMonth(records: List<WorkRecord>, month: YearMonth): List<WorkRecord> =
        records.filter { YearMonth.from(it.workDate) == month }

    fun inWeekOf(
        records: List<WorkRecord>,
        date: LocalDate,
        weekStart: DayOfWeek,
    ): List<WorkRecord> {
        val from = startOfWeek(date, weekStart)
        val until = from.plusWeeks(1)
        return records.filter { it.workDate >= from && it.workDate < until }
    }

    /** First day of the week containing [date], for a configurable week start. */
    fun startOfWeek(date: LocalDate, weekStart: DayOfWeek): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(weekStart))

    fun monthRange(month: YearMonth): ClosedRange<LocalDate> =
        month.atDay(1)..month.atEndOfMonth()
}
