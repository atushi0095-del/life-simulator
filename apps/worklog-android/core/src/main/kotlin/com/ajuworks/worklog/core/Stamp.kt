package com.ajuworks.worklog.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * A point in time as WorkLog records it.
 *
 * Two values are stored, never a formatted string:
 *  - [instant]: the absolute moment, which is what all arithmetic uses.
 *  - [offset]: the UTC offset that was in effect *at the device* when the stamp
 *    was taken.
 *
 * Keeping the offset is what makes records survive a timezone change. If a user
 * clocks in at 09:00 JST and later flies to London, the record must still read
 * "09:00" and still belong to that Japanese calendar day - recomputing the wall
 * clock from the *current* zone would silently rewrite history. Elapsed time is
 * unaffected either way because it is derived from [instant].
 */
data class Stamp(
    val instant: Instant,
    val offset: ZoneOffset,
) : Comparable<Stamp> {

    val epochMillis: Long get() = instant.toEpochMilli()

    /** Wall-clock date and time as the user saw it when the stamp was taken. */
    val localDateTime: LocalDateTime get() = LocalDateTime.ofInstant(instant, offset)

    val localDate: LocalDate get() = localDateTime.toLocalDate()

    val localTime: LocalTime get() = localDateTime.toLocalTime()

    override fun compareTo(other: Stamp): Int = instant.compareTo(other.instant)

    operator fun minus(other: Stamp): Long = epochMillis - other.epochMillis

    companion object {
        fun now(clock: java.time.Clock): Stamp {
            val instant = clock.instant()
            return Stamp(instant, clock.zone.rules.getOffset(instant))
        }

        /** Rebuilds a stamp from persisted columns. */
        fun of(epochMillis: Long, offsetSeconds: Int): Stamp =
            Stamp(Instant.ofEpochMilli(epochMillis), ZoneOffset.ofTotalSeconds(offsetSeconds))

        /**
         * Builds a stamp from a wall-clock value the user typed, resolving it in
         * [zone]. Used by manual entry and by the history editor.
         */
        fun ofLocal(dateTime: LocalDateTime, zone: ZoneId): Stamp {
            val offset = zone.rules.getOffset(dateTime)
            return Stamp(dateTime.toInstant(offset), offset)
        }

        fun ofLocal(date: LocalDate, time: LocalTime, zone: ZoneId): Stamp =
            ofLocal(LocalDateTime.of(date, time), zone)
    }
}
