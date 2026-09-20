package com.ajuworks.worklog.core

import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * CSV export, generated entirely on device and handed to the Android share
 * sheet. Nothing is uploaded by the app itself.
 */
object CsvExport {

    /**
     * Excel on Windows still guesses Shift_JIS for a bare UTF-8 file, which
     * turns Japanese headers into mojibake. The BOM is what makes it read the
     * file as UTF-8, so it is written unconditionally.
     */
    const val BOM: String = "﻿"

    private val DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.US)
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    data class Headers(
        val date: String,
        val start: String,
        val end: String,
        val breakTime: String,
        val net: String,
        val note: String,
    ) {
        companion object {
            val JAPANESE = Headers("日付", "出勤", "退勤", "休憩", "実働", "メモ")
            val ENGLISH = Headers("Date", "Clock in", "Clock out", "Break", "Worked", "Note")
        }
    }

    /**
     * Renders [records] oldest-first. Sessions still running are written with
     * empty clock-out and break/worked columns computed up to [now], so an
     * export taken mid-shift is still readable rather than blank.
     */
    fun render(
        records: List<WorkRecord>,
        now: Stamp,
        headers: Headers = Headers.JAPANESE,
        includeBom: Boolean = true,
    ): String = buildString {
        if (includeBom) append(BOM)
        appendLine(
            row(
                headers.date, headers.start, headers.end,
                headers.breakTime, headers.net, headers.note,
            )
        )
        records.sortedBy { it.start.instant }.forEach { record ->
            appendLine(
                row(
                    record.workDate.format(DATE),
                    record.start.localTime.format(TIME),
                    record.end?.localTime?.format(TIME).orEmpty(),
                    WorkTimeFormat.toClock(record.breakMillis(now)),
                    WorkTimeFormat.toClock(record.netMillis(now)),
                    record.note,
                )
            )
        }
    }

    fun fileName(prefix: String, stamp: Stamp): String =
        "$prefix-${stamp.localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US))}.csv"

    private fun row(vararg cells: String): String =
        cells.joinToString(",") { escape(it) }

    /** RFC 4180 quoting; only applied where a cell actually needs it. */
    private fun escape(cell: String): String =
        if (cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + cell.replace("\"", "\"\"") + "\""
        } else {
            cell
        }
}
