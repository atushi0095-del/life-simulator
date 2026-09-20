package com.ajuworks.worklog.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.ajuworks.worklog.core.BreakEntry
import com.ajuworks.worklog.core.Stamp
import com.ajuworks.worklog.core.WorkRecord
import java.time.Instant

/**
 * Timestamps are stored as two plain columns - epoch milliseconds plus the UTC
 * offset that was in effect when the stamp was taken - never as formatted text.
 * See [Stamp] for why the offset is kept.
 */
@Entity(tableName = "work_sessions")
data class WorkSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "start_epoch_millis") val startEpochMillis: Long,
    @ColumnInfo(name = "start_offset_seconds") val startOffsetSeconds: Int,
    /** Null while the session is running. There is at most one such row. */
    @ColumnInfo(name = "end_epoch_millis") val endEpochMillis: Long? = null,
    @ColumnInfo(name = "end_offset_seconds") val endOffsetSeconds: Int? = null,
    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "break_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["work_session_id"],
            // Deleting a work session must not leave orphaned breaks behind;
            // a stray break would corrupt nothing but would leak rows forever.
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("work_session_id")],
)
data class BreakSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "work_session_id") val workSessionId: Long,
    @ColumnInfo(name = "start_epoch_millis") val startEpochMillis: Long,
    @ColumnInfo(name = "start_offset_seconds") val startOffsetSeconds: Int,
    @ColumnInfo(name = "end_epoch_millis") val endEpochMillis: Long? = null,
    @ColumnInfo(name = "end_offset_seconds") val endOffsetSeconds: Int? = null,
)

/** A session with its breaks, as Room reads it in one query. */
data class WorkSessionWithBreaks(
    @Embedded val session: WorkSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "work_session_id")
    val breaks: List<BreakSessionEntity>,
)

fun WorkSessionWithBreaks.toDomain(): WorkRecord = WorkRecord(
    id = session.id,
    start = Stamp.of(session.startEpochMillis, session.startOffsetSeconds),
    end = session.endEpochMillis?.let { Stamp.of(it, session.endOffsetSeconds ?: session.startOffsetSeconds) },
    breaks = breaks
        .sortedBy { it.startEpochMillis }
        .map { it.toDomain() },
    note = session.note,
    createdAt = Instant.ofEpochMilli(session.createdAt),
    updatedAt = Instant.ofEpochMilli(session.updatedAt),
)

fun BreakSessionEntity.toDomain(): BreakEntry = BreakEntry(
    id = id,
    workSessionId = workSessionId,
    start = Stamp.of(startEpochMillis, startOffsetSeconds),
    end = endEpochMillis?.let { Stamp.of(it, endOffsetSeconds ?: startOffsetSeconds) },
)

fun WorkRecord.toEntity(): WorkSessionEntity = WorkSessionEntity(
    id = id,
    startEpochMillis = start.epochMillis,
    startOffsetSeconds = start.offset.totalSeconds,
    endEpochMillis = end?.epochMillis,
    endOffsetSeconds = end?.offset?.totalSeconds,
    note = note,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

fun BreakEntry.toEntity(sessionId: Long): BreakSessionEntity = BreakSessionEntity(
    id = id,
    workSessionId = sessionId,
    startEpochMillis = start.epochMillis,
    startOffsetSeconds = start.offset.totalSeconds,
    endEpochMillis = end?.epochMillis,
    endOffsetSeconds = end?.offset?.totalSeconds,
)
