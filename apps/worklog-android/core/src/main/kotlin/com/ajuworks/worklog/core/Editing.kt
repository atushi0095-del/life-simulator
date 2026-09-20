package com.ajuworks.worklog.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** A break as the editor presents it: two wall-clock times. */
data class BreakDraft(
    val id: Long = 0L,
    val startTime: LocalTime,
    val endTime: LocalTime?,
)

/**
 * A session as the editor presents it: one date plus wall-clock times.
 *
 * There is deliberately no "ends next day" checkbox. A clock-out that is at or
 * before the clock-in is read as the following day, which is the only reading
 * that can be true for a real shift and keeps the overnight case a zero-effort
 * one for the user. The editor reports it back as [Issue.OvernightAssumed] so
 * nothing happens silently.
 */
data class WorkDraft(
    val id: Long = 0L,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime?,
    val breaks: List<BreakDraft> = emptyList(),
    val note: String = "",
)

enum class Severity {
    /** Saving is refused. */
    ERROR,

    /** Saving is allowed; the user is shown a confirmation. */
    WARNING,

    /** Saving is allowed; the user is just told what was inferred. */
    INFO,
}

sealed class Issue(val severity: Severity) {
    /** The clock-out was read as the next calendar day. */
    data object OvernightAssumed : Issue(Severity.INFO)

    /** Two breaks overlap; the overlap is counted once. */
    data object OverlappingBreaks : Issue(Severity.INFO)

    /** A break falls outside the session and was ignored. */
    data class BreakOutsideSession(val index: Int) : Issue(Severity.ERROR)

    /** Breaks add up to the whole session or more. */
    data object BreaksExceedWork : Issue(Severity.ERROR)

    /** Longer than [WorkEditor.LONG_SESSION_HOURS] hours. */
    data class ImplausiblyLong(val hours: Long) : Issue(Severity.WARNING)

    /** The session ends in the future. */
    data object EndsInFuture : Issue(Severity.WARNING)
}

data class EditorResult(
    val record: WorkRecord,
    val issues: List<Issue>,
) {
    val blocked: Boolean get() = issues.any { it.severity == Severity.ERROR }
    val hasWarnings: Boolean get() = issues.any { it.severity == Severity.WARNING }
}

/**
 * Turns what the user typed into a [WorkRecord], resolving overnight rollovers
 * and reporting anything that does not add up.
 */
object WorkEditor {

    const val LONG_SESSION_HOURS = 16L

    fun resolve(
        draft: WorkDraft,
        zone: ZoneId,
        now: Instant,
        createdAt: Instant = now,
    ): EditorResult {
        val issues = mutableListOf<Issue>()

        val start = Stamp.ofLocal(draft.date, draft.startTime, zone)

        val end: Stamp? = draft.endTime?.let { endTime ->
            var candidate = Stamp.ofLocal(draft.date, endTime, zone)
            if (candidate <= start) {
                candidate = Stamp.ofLocal(draft.date.plusDays(1), endTime, zone)
                issues += Issue.OvernightAssumed
            }
            candidate
        }

        val breaks = draft.breaks.mapIndexed { index, breakDraft ->
            index to resolveBreak(breakDraft, draft.date, start, zone)
        }

        val boundary = end ?: Stamp(now, start.offset)
        breaks.forEach { (index, entry) ->
            val outside = entry.start < start ||
                entry.start > boundary ||
                (entry.end != null && entry.end > boundary)
            if (outside) issues += Issue.BreakOutsideSession(index)
        }

        val record = WorkRecord(
            id = draft.id,
            start = start,
            end = end,
            breaks = breaks.map { it.second },
            note = draft.note,
            createdAt = createdAt,
            updatedAt = now,
        )

        val nowStamp = Stamp(now, start.offset)
        val spans = record.breaks.mapNotNull { entry ->
            val to = (entry.end ?: nowStamp).epochMillis
            if (to > entry.start.epochMillis) entry.start.epochMillis to to else null
        }
        val rawBreakMillis = spans.sumOf { (from, to) -> to - from }
        if (rawBreakMillis > mergeSpans(spans).sumOf { (from, to) -> to - from }) {
            issues += Issue.OverlappingBreaks
        }

        if (end != null) {
            val gross = record.grossMillis(nowStamp)
            if (record.breakMillis(nowStamp) >= gross) issues += Issue.BreaksExceedWork
            val hours = gross / 3_600_000L
            if (hours >= LONG_SESSION_HOURS) issues += Issue.ImplausiblyLong(hours)
            if (end.instant.isAfter(now)) issues += Issue.EndsInFuture
        }

        return EditorResult(record, issues)
    }

    /**
     * Quick manual entry (spec: "+ 勤務を追加"), where the user gives a single
     * total break length instead of break clock times. The synthesized break is
     * centred in the session so the total is exact and the detail screen shows
     * something plausible rather than a break glued to the clock-in.
     */
    fun fromTotals(
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        breakMinutes: Long,
        zone: ZoneId,
        now: Instant,
        id: Long = 0L,
        note: String = "",
    ): EditorResult {
        val base = resolve(
            WorkDraft(id = id, date = date, startTime = startTime, endTime = endTime, note = note),
            zone = zone,
            now = now,
        )
        if (breakMinutes <= 0L) return base

        val record = base.record
        val end = record.end ?: return base
        val breakMillis = breakMinutes * 60_000L
        val gross = end.epochMillis - record.start.epochMillis
        if (breakMillis >= gross) {
            return base.copy(issues = base.issues + Issue.BreaksExceedWork)
        }
        val from = record.start.epochMillis + (gross - breakMillis) / 2
        val synthesized = BreakEntry(
            workSessionId = record.id,
            start = Stamp.of(from, record.start.offset.totalSeconds),
            end = Stamp.of(from + breakMillis, record.start.offset.totalSeconds),
        )
        return base.copy(record = record.copy(breaks = listOf(synthesized)))
    }

    private fun resolveBreak(
        draft: BreakDraft,
        date: LocalDate,
        sessionStart: Stamp,
        zone: ZoneId,
    ): BreakEntry {
        var start = Stamp.ofLocal(date, draft.startTime, zone)
        // A break is placed at its first occurrence at or after the clock-in, so
        // a 01:00 break inside a 22:00 -> 06:00 shift lands on the second day.
        if (start < sessionStart) start = Stamp.ofLocal(date.plusDays(1), draft.startTime, zone)

        val end = draft.endTime?.let { endTime ->
            var candidate = Stamp.ofLocal(start.localDate, endTime, zone)
            if (candidate <= start) candidate = Stamp.ofLocal(start.localDate.plusDays(1), endTime, zone)
            candidate
        }
        return BreakEntry(id = draft.id, start = start, end = end)
    }
}
