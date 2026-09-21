package com.ajuworks.worklog.data

import com.ajuworks.worklog.core.BreakEntry
import com.ajuworks.worklog.core.Stamp
import com.ajuworks.worklog.core.WorkRecord
import com.ajuworks.worklog.data.local.WorkDao
import com.ajuworks.worklog.data.local.toDomain
import com.ajuworks.worklog.data.local.toEntity
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Every state transition the app can make. The UI calls these and never touches
 * Room or SQL.
 *
 * There is no ticking timer anywhere in WorkLog. Clocking in writes a start
 * time; elapsed time is always `now - start - breaks`, computed when something
 * is drawn. That is why a force-stop, a doze, or a reboot costs nothing.
 */
class WorkRepository(
    private val dao: WorkDao,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    val zone: ZoneId get() = clock.zone

    fun now(): Stamp = Stamp.now(clock)

    val activeRecord: Flow<WorkRecord?> =
        dao.observeActiveSession().map { it?.toDomain() }

    val allRecords: Flow<List<WorkRecord>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun recordsInMonth(month: YearMonth): Flow<List<WorkRecord>> {
        val from = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val until = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.observeBetween(from, until).map { rows -> rows.map { it.toDomain() } }
    }

    fun recordsFrom(date: LocalDate): Flow<List<WorkRecord>> {
        val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.observeBetween(from, Long.MAX_VALUE).map { rows -> rows.map { it.toDomain() } }
    }

    /**
     * Sessions clocked in on or after [from] and before [until], both local
     * dates. Used for the home screen's one-query window; see
     * [com.ajuworks.worklog.core.WorkAggregator.summaryWindow].
     */
    fun recordsBetween(from: LocalDate, until: LocalDate): Flow<List<WorkRecord>> {
        val fromMillis = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val untilMillis = until.atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.observeBetween(fromMillis, untilMillis).map { rows -> rows.map { it.toDomain() } }
    }

    fun record(id: Long): Flow<WorkRecord?> = dao.observeById(id).map { it?.toDomain() }

    val earliestWorkDate: Flow<LocalDate?> = dao.observeEarliestStart().map { millis ->
        millis?.let { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    }

    /**
     * Clocks in. If a session is somehow already running (a double tap from the
     * widget and the app at once), the existing one is returned untouched
     * rather than starting a second - two open sessions would make every total
     * ambiguous.
     */
    suspend fun clockIn(): Long {
        dao.activeSession()?.let { return it.session.id }
        val now = now()
        return dao.insertSession(
            WorkRecord(start = now, createdAt = now.instant, updatedAt = now.instant).toEntity()
        )
    }

    /** Clocks out, closing any break still open so it cannot run past the end. */
    suspend fun clockOut(): Boolean {
        val active = dao.activeSession() ?: return false
        val now = now()
        dao.runningBreakFor(active.session.id)?.let { open ->
            dao.updateBreak(
                open.copy(
                    endEpochMillis = now.epochMillis,
                    endOffsetSeconds = now.offset.totalSeconds,
                )
            )
        }
        dao.updateSession(
            active.session.copy(
                endEpochMillis = now.epochMillis,
                endOffsetSeconds = now.offset.totalSeconds,
                updatedAt = now.epochMillis,
            )
        )
        return true
    }

    /** Starts a break. No-op if one is already running. */
    suspend fun startBreak(): Boolean {
        val active = dao.activeSession() ?: return false
        if (dao.runningBreakFor(active.session.id) != null) return false
        val now = now()
        dao.insertBreak(BreakEntry(start = now).toEntity(active.session.id))
        dao.updateSession(active.session.copy(updatedAt = now.epochMillis))
        return true
    }

    suspend fun endBreak(): Boolean {
        val active = dao.activeSession() ?: return false
        val open = dao.runningBreakFor(active.session.id) ?: return false
        val now = now()
        dao.updateBreak(
            open.copy(
                endEpochMillis = now.epochMillis,
                endOffsetSeconds = now.offset.totalSeconds,
            )
        )
        dao.updateSession(active.session.copy(updatedAt = now.epochMillis))
        return true
    }

    suspend fun save(record: WorkRecord): Long =
        if (record.id == 0L) {
            dao.insertSessionWithBreaks(
                record.toEntity(),
                record.breaks.map { it.toEntity(0L) },
            )
        } else {
            dao.replaceSession(
                record.toEntity(),
                record.breaks.map { it.toEntity(record.id) },
            )
            record.id
        }

    suspend fun delete(id: Long) = dao.deleteSessionById(id)

    suspend fun activeRecordNow(): WorkRecord? = dao.activeSession()?.toDomain()
}
