package com.ajuworks.atonannichi.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ajuworks.atonannichi.core.AfterMode
import com.ajuworks.atonannichi.core.Countdown
import com.ajuworks.atonannichi.core.DayCount
import com.ajuworks.atonannichi.core.Design
import com.ajuworks.atonannichi.core.NotifyRules
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import androidx.core.content.edit

@Entity(tableName = "event")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateEpochDay: Long,
    /** 0:00 からの分。null は時刻なし */
    val timeMinutes: Int? = null,
    /** アプリ専用領域にコピーした写真のファイル名。外部には出さない */
    val photoFile: String? = null,
    val icon: String = "🎉",
    val memo: String = "",
    val notifyMask: Int = NotifyRules.DEFAULT,
    /** 通知済みの印（日付を変えたらリセット） */
    val sentMask: Int = 0,
    val afterMode: String = AfterMode.COUNT_UP.name,
    val design: String = Design.SOFT.name,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(dateEpochDay)
    val time: LocalTime? get() = timeMinutes?.let { LocalTime.of(it / 60, it % 60) }
    val after: AfterMode get() = AfterMode.entries.firstOrNull { it.name == afterMode } ?: AfterMode.COUNT_UP
    val designEnum: Design get() = Design.of(design)
    fun countdown(today: LocalDate): Countdown = DayCount.of(date, today, after)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM event") fun observeAll(): Flow<List<EventEntity>>
    @Query("SELECT * FROM event") suspend fun all(): List<EventEntity>
    @Query("SELECT * FROM event WHERE id = :id") suspend fun byId(id: Long): EventEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(e: EventEntity): Long
    @Query("DELETE FROM event WHERE id = :id") suspend fun delete(id: Long)
    @Query("UPDATE event SET sentMask = :mask WHERE id = :id") suspend fun setSent(id: Long, mask: Int)
}

@Database(entities = [EventEntity::class], version = 1, exportSchema = true)
abstract class EventDatabase : RoomDatabase() {
    abstract fun dao(): EventDao

    companion object {
        @Volatile private var instance: EventDatabase? = null
        fun get(context: Context): EventDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, EventDatabase::class.java, "events.db").build().also { instance = it }
        }
    }
}

/**
 * ウィジェットごとの設定（どのイベントを、どのデザインで）。
 * ウィジェットのレシーバーから同期的に読むため SharedPreferences を使う。
 */
class WidgetConfigStore(context: Context) {
    private val sp = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    data class Config(val eventId: Long, val design: Design)

    fun get(widgetId: Int): Config? {
        val ev = sp.getLong("e$widgetId", -1)
        if (ev < 0) return null
        return Config(ev, Design.of(sp.getString("d$widgetId", null)))
    }

    fun put(widgetId: Int, c: Config) = sp.edit { putLong("e$widgetId", c.eventId).putString("d$widgetId", c.design.name) }

    fun remove(widgetId: Int) = sp.edit { remove("e$widgetId").remove("d$widgetId") }
}
