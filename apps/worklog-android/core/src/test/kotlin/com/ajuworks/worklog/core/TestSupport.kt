package com.ajuworks.worklog.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

val TOKYO: ZoneId = ZoneId.of("Asia/Tokyo")

fun stamp(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    zone: ZoneId = TOKYO,
): Stamp = Stamp.ofLocal(LocalDateTime.of(year, month, day, hour, minute), zone)

fun session(
    start: Stamp,
    end: Stamp? = null,
    breaks: List<BreakEntry> = emptyList(),
): WorkRecord = WorkRecord(id = 1L, start = start, end = end, breaks = breaks)

fun breakEntry(start: Stamp, end: Stamp? = null): BreakEntry =
    BreakEntry(workSessionId = 1L, start = start, end = end)

fun date(year: Int, month: Int, day: Int): LocalDate = LocalDate.of(year, month, day)

fun time(hour: Int, minute: Int): LocalTime = LocalTime.of(hour, minute)

/** Milliseconds for `h` hours and `m` minutes, for readable assertions. */
fun hm(hours: Long, minutes: Long = 0L): Long = WorkTimeFormat.fromHoursMinutes(hours, minutes)
