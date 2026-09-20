package com.ajuworks.worklog.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {

    /**
     * The one session without a clock-out, if any. This is the whole of the
     * app's "am I working?" state - it lives in the database and nowhere else,
     * so a force-stop, a crash or a reboot cannot lose it.
     */
    @Transaction
    @Query("SELECT * FROM work_sessions WHERE end_epoch_millis IS NULL ORDER BY start_epoch_millis DESC LIMIT 1")
    fun observeActiveSession(): Flow<WorkSessionWithBreaks?>

    @Transaction
    @Query("SELECT * FROM work_sessions WHERE end_epoch_millis IS NULL ORDER BY start_epoch_millis DESC LIMIT 1")
    suspend fun activeSession(): WorkSessionWithBreaks?

    @Transaction
    @Query("SELECT * FROM work_sessions ORDER BY start_epoch_millis DESC")
    fun observeAll(): Flow<List<WorkSessionWithBreaks>>

    /**
     * Sessions whose clock-in falls in [fromEpochMillis, untilEpochMillis).
     * Bounds are computed from local dates by the repository, because the
     * calendar day a session belongs to is a local-time question.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM work_sessions
        WHERE start_epoch_millis >= :fromEpochMillis AND start_epoch_millis < :untilEpochMillis
        ORDER BY start_epoch_millis DESC
        """
    )
    fun observeBetween(fromEpochMillis: Long, untilEpochMillis: Long): Flow<List<WorkSessionWithBreaks>>

    @Transaction
    @Query("SELECT * FROM work_sessions WHERE id = :id")
    fun observeById(id: Long): Flow<WorkSessionWithBreaks?>

    @Transaction
    @Query("SELECT * FROM work_sessions WHERE id = :id")
    suspend fun sessionById(id: Long): WorkSessionWithBreaks?

    /** Earliest and latest clock-in, used to build the month picker. */
    @Query("SELECT MIN(start_epoch_millis) FROM work_sessions")
    fun observeEarliestStart(): Flow<Long?>

    @Insert
    suspend fun insertSession(session: WorkSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkSessionEntity)

    @Delete
    suspend fun deleteSession(session: WorkSessionEntity)

    @Query("DELETE FROM work_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Insert
    suspend fun insertBreak(entry: BreakSessionEntity): Long

    @Update
    suspend fun updateBreak(entry: BreakSessionEntity)

    @Query("DELETE FROM break_sessions WHERE work_session_id = :sessionId")
    suspend fun deleteBreaksFor(sessionId: Long)

    @Query("SELECT * FROM break_sessions WHERE work_session_id = :sessionId AND end_epoch_millis IS NULL LIMIT 1")
    suspend fun runningBreakFor(sessionId: Long): BreakSessionEntity?

    @Query("DELETE FROM work_sessions")
    suspend fun deleteAll()

    /**
     * Replaces a session and all of its breaks in one transaction, so an edit
     * can never land half-applied and leave totals wrong.
     */
    @Transaction
    suspend fun replaceSession(session: WorkSessionEntity, breaks: List<BreakSessionEntity>) {
        updateSession(session)
        deleteBreaksFor(session.id)
        breaks.forEach { insertBreak(it.copy(id = 0L, workSessionId = session.id)) }
    }

    @Transaction
    suspend fun insertSessionWithBreaks(
        session: WorkSessionEntity,
        breaks: List<BreakSessionEntity>,
    ): Long {
        val id = insertSession(session.copy(id = 0L))
        breaks.forEach { insertBreak(it.copy(id = 0L, workSessionId = id)) }
        return id
    }
}
