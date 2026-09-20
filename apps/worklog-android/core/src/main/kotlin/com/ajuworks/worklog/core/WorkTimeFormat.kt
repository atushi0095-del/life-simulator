package com.ajuworks.worklog.core

import java.util.Locale
import kotlin.math.abs

/**
 * Duration rendering. Kept in :core so the app, the widget and the CSV writer
 * can never drift apart on rounding.
 *
 * Everything truncates towards zero rather than rounding: showing "8時間14分"
 * for 8h14m59s matches what a clock-watching user expects, and it means the
 * minute display never jumps ahead of the elapsed time.
 */
object WorkTimeFormat {

    fun hoursPart(millis: Long): Long = abs(millis) / 3_600_000L

    fun minutesPart(millis: Long): Long = (abs(millis) % 3_600_000L) / 60_000L

    /** "8時間14分" - the primary in-app display. */
    fun toJapanese(millis: Long): String =
        "${hoursPart(millis)}時間${String.format(Locale.US, "%02d", minutesPart(millis))}分"

    /** "8h 14m" - the English display. */
    fun toEnglish(millis: Long): String =
        "${hoursPart(millis)}h ${String.format(Locale.US, "%02d", minutesPart(millis))}m"

    /**
     * "08:14" - the CSV/spreadsheet form. Hours are not capped at 24 so a
     * monthly total renders as "162:24" rather than wrapping.
     */
    fun toClock(millis: Long): String =
        String.format(Locale.US, "%02d:%02d", hoursPart(millis), minutesPart(millis))

    fun fromHoursMinutes(hours: Long, minutes: Long): Long =
        hours * 3_600_000L + minutes * 60_000L
}
