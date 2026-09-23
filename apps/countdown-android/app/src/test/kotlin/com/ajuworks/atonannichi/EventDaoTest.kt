package com.ajuworks.atonannichi

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajuworks.atonannichi.core.DayCount
import com.ajuworks.atonannichi.data.EventDatabase
import com.ajuworks.atonannichi.data.EventEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * Room round-trips. The interesting part is not that Room stores a row - it is
 * that an edit reaches the store intact and a delete leaves nothing behind,
 * because a widget pointing at a deleted event must find it gone rather than
 * stale.
 */
@RunWith(RobolectricTestRunner::class)
class EventDaoTest {
    private lateinit var db: EventDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EventDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun event(title: String, date: LocalDate) =
        EventEntity(title = title, dateEpochDay = date.toEpochDay())

    @Test
    fun `insert then read back`() = runTest {
        val id = db.dao().upsert(event("沖縄旅行", LocalDate.of(2026, 10, 30)))

        val stored = db.dao().byId(id)

        assertThat(stored).isNotNull()
        assertThat(stored!!.title).isEqualTo("沖縄旅行")
        assertThat(stored.date).isEqualTo(LocalDate.of(2026, 10, 30))
    }

    @Test
    fun `edit replaces the stored row rather than adding one`() = runTest {
        val id = db.dao().upsert(event("旅行", LocalDate.of(2026, 10, 30)))

        db.dao().upsert(db.dao().byId(id)!!.copy(title = "沖縄旅行", dateEpochDay = LocalDate.of(2026, 11, 1).toEpochDay()))

        assertThat(db.dao().all()).hasSize(1)
        assertThat(db.dao().byId(id)!!.title).isEqualTo("沖縄旅行")
        assertThat(db.dao().byId(id)!!.date).isEqualTo(LocalDate.of(2026, 11, 1))
    }

    @Test
    fun `delete removes the row`() = runTest {
        val id = db.dao().upsert(event("ライブ", LocalDate.of(2026, 12, 1)))

        db.dao().delete(id)

        assertThat(db.dao().byId(id)).isNull()
        assertThat(db.dao().all()).isEmpty()
    }

    @Test
    fun `sort order puts the nearest upcoming event first and past events last`() = runTest {
        val today = LocalDate.of(2026, 10, 1)
        db.dao().upsert(event("来月", today.plusDays(30)))
        db.dao().upsert(event("来週", today.plusDays(7)))
        db.dao().upsert(event("先週", today.minusDays(7)))
        db.dao().upsert(event("今日", today))

        val titles = db.dao().all()
            .sortedBy { DayCount.sortKey(it.date, today) }
            .map { it.title }

        assertThat(titles).containsExactly("今日", "来週", "来月", "先週").inOrder()
    }

    @Test
    fun `marking a notification as sent survives a read`() = runTest {
        val id = db.dao().upsert(event("誕生日", LocalDate.of(2026, 10, 8)))

        db.dao().setSent(id, 0b101)

        assertThat(db.dao().byId(id)!!.sentMask).isEqualTo(0b101)
    }
}
