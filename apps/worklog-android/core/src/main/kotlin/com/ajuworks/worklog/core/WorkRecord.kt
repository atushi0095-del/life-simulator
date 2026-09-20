package com.ajuworks.worklog.core

import java.time.Instant
import java.time.LocalDate

/** One break inside a work session. [end] is null while the break is running. */
data class BreakEntry(
    val id: Long = 0L,
    val workSessionId: Long = 0L,
    val start: Stamp,
    val end: Stamp? = null,
) {
    val isRunning: Boolean get() = end == null
}

/**
 * One work session: a clock-in, an optional clock-out, and its breaks.
 *
 * A session with a null [end] is the *active* session. There is at most one at
 * a time, and it is what the home screen and the widget render.
 */
data class WorkRecord(
    val id: Long = 0L,
    val start: Stamp,
    val end: Stamp? = null,
    val breaks: List<BreakEntry> = emptyList(),
    val note: String = "",
    val createdAt: Instant = start.instant,
    val updatedAt: Instant = start.instant,
) {
    val isRunning: Boolean get() = end == null

    val runningBreak: BreakEntry? get() = breaks.firstOrNull { it.isRunning }

    val isOnBreak: Boolean get() = isRunning && runningBreak != null

    /**
     * The calendar day this session counts towards: **the day the user clocked
     * in**, in the wall clock that was in effect then. A 22:00 -> 06:00 shift is
     * therefore booked entirely on the evening it started. Documented in
     * README.md because it is the one rule that surprises people.
     */
    val workDate: LocalDate get() = start.localDate

    /** Wall-clock end of the session, or of the elapsed span while it runs. */
    fun endOrNow(now: Stamp): Stamp = end ?: now

    /** Clock-in to clock-out, breaks included. */
    fun grossMillis(now: Stamp): Long =
        (endOrNow(now).epochMillis - start.epochMillis).coerceAtLeast(0L)

    /**
     * Total break time, with overlapping breaks merged so a double-tap or an
     * overlapping manual edit can never subtract the same minute twice, and
     * every break clamped to the session so a break that outlives the clock-out
     * cannot push work time negative.
     */
    fun breakMillis(now: Stamp): Long {
        val sessionStart = start.epochMillis
        val sessionEnd = endOrNow(now).epochMillis
        if (sessionEnd <= sessionStart) return 0L

        val spans = breaks.mapNotNull { entry ->
            val from = entry.start.epochMillis.coerceIn(sessionStart, sessionEnd)
            val rawTo = (entry.end ?: now).epochMillis
            val to = rawTo.coerceIn(sessionStart, sessionEnd)
            if (to > from) from to to else null
        }
        return mergeSpans(spans).sumOf { (from, to) -> to - from }
    }

    /** Actual time worked: gross minus merged breaks, never negative. */
    fun netMillis(now: Stamp): Long =
        (grossMillis(now) - breakMillis(now)).coerceAtLeast(0L)
}

/**
 * Merges overlapping and touching half-open spans. Input need not be sorted.
 */
internal fun mergeSpans(spans: List<Pair<Long, Long>>): List<Pair<Long, Long>> {
    if (spans.size <= 1) return spans
    val sorted = spans.sortedBy { it.first }
    val merged = ArrayList<Pair<Long, Long>>(sorted.size)
    var (currentFrom, currentTo) = sorted.first()
    for (index in 1 until sorted.size) {
        val (from, to) = sorted[index]
        if (from <= currentTo) {
            if (to > currentTo) currentTo = to
        } else {
            merged += currentFrom to currentTo
            currentFrom = from
            currentTo = to
        }
    }
    merged += currentFrom to currentTo
    return merged
}
