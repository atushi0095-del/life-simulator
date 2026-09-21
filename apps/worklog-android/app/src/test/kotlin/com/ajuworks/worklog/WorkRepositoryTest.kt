package com.ajuworks.worklog

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajuworks.worklog.core.Stamp
import com.ajuworks.worklog.data.WorkRepository
import com.ajuworks.worklog.data.local.WorkDatabase
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Persistence-level behaviour. These use a real (in-memory) Room database
 * rather than a fake, because the properties under test - that state survives
 * a process death, that there is never more than one open session - are
 * properties of the schema and the queries, not of the Kotlin above them.
 */
@RunWith(RobolectricTestRunner::class)
class WorkRepositoryTest {

    private val zone: ZoneId = ZoneId.of("Asia/Tokyo")
    private lateinit var database: WorkDatabase
    private var clockInstant: Instant = Instant.parse("2026-09-20T00:03:00Z") // 09:03 JST

    private val clock = object : Clock() {
        // `this@WorkRepositoryTest.zone` is required, not stylistic: a bare
        // `zone` here binds to Clock's own synthetic `zone` property (from
        // getZone()), not the outer field, and the override recurses into
        // itself until the stack runs out.
        override fun getZone(): ZoneId = this@WorkRepositoryTest.zone
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = clockInstant
    }

    private lateinit var repository: WorkRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = WorkRepository(database.workDao(), clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun advance(duration: Duration) {
        clockInstant = clockInstant.plus(duration)
    }

    @Test
    fun `clocking in stores an open session`() = runTest {
        repository.clockIn()

        val active = repository.activeRecord.first()
        assertThat(active).isNotNull()
        assertThat(active!!.isRunning).isTrue()
        assertThat(active.start.localTime.toString()).isEqualTo("09:03")
    }

    @Test
    fun `clocking in twice does not open a second session`() = runTest {
        val first = repository.clockIn()
        advance(Duration.ofMinutes(5))
        val second = repository.clockIn()

        assertThat(second).isEqualTo(first)
        assertThat(repository.allRecords.first()).hasSize(1)
    }

    @Test
    fun `a session survives losing the process`() = runTest {
        repository.clockIn()
        advance(Duration.ofHours(2).plusMinutes(17))

        // A force-stop keeps nothing in memory. Rebuilding the repository over
        // the same database is exactly what a cold start does.
        val restarted = WorkRepository(database.workDao(), clock)
        val active = restarted.activeRecord.first()

        assertThat(active).isNotNull()
        assertThat(active!!.netMillis(Stamp.now(clock))).isEqualTo(Duration.ofMinutes(137).toMillis())
    }

    @Test
    fun `clocking out closes the session and a break left running`() = runTest {
        repository.clockIn()
        advance(Duration.ofHours(3))
        repository.startBreak()
        advance(Duration.ofMinutes(45))
        repository.clockOut()

        val records = repository.allRecords.first()
        assertThat(records).hasSize(1)
        val record = records.single()
        assertThat(record.isRunning).isFalse()
        assertThat(record.breaks.single().isRunning).isFalse()
        assertThat(record.netMillis(Stamp.now(clock))).isEqualTo(Duration.ofHours(3).toMillis())
    }

    @Test
    fun `breaks can be taken more than once in a shift`() = runTest {
        // The spec's worked example, punched rather than typed:
        //   09:00 -> 18:00, breaks 12:00-12:45 and 15:00-15:15, = 8 hours.
        repository.clockIn()
        advance(Duration.ofHours(3)) // 12:00
        repository.startBreak()
        advance(Duration.ofMinutes(45)) // 12:45
        repository.endBreak()
        advance(Duration.ofHours(2).plusMinutes(15)) // 15:00
        repository.startBreak()
        advance(Duration.ofMinutes(15)) // 15:15
        repository.endBreak()
        advance(Duration.ofHours(2).plusMinutes(45)) // 18:00
        repository.clockOut()

        val record = repository.allRecords.first().single()
        assertThat(record.breaks).hasSize(2)
        assertThat(record.grossMillis(Stamp.now(clock))).isEqualTo(Duration.ofHours(9).toMillis())
        assertThat(record.breakMillis(Stamp.now(clock))).isEqualTo(Duration.ofHours(1).toMillis())
        assertThat(record.netMillis(Stamp.now(clock))).isEqualTo(Duration.ofHours(8).toMillis())
    }

    @Test
    fun `starting a break twice does not open a second break`() = runTest {
        repository.clockIn()
        repository.startBreak()
        val second = repository.startBreak()

        assertThat(second).isFalse()
        assertThat(repository.activeRecord.first()!!.breaks).hasSize(1)
    }

    @Test
    fun `clocking out with nothing running is a no-op`() = runTest {
        assertThat(repository.clockOut()).isFalse()
        assertThat(repository.allRecords.first()).isEmpty()
    }

    @Test
    fun `deleting a record removes its breaks too`() = runTest {
        val id = repository.clockIn()
        repository.startBreak()
        repository.endBreak()
        repository.clockOut()

        repository.delete(id)

        assertThat(repository.allRecords.first()).isEmpty()
        assertThat(database.workDao().runningBreakFor(id)).isNull()
    }

    @Test
    fun `an overnight shift is stored on the day it started`() = runTest {
        clockInstant = Instant.parse("2026-09-20T13:00:00Z") // 22:00 JST
        repository.clockIn()
        advance(Duration.ofHours(8))
        repository.clockOut()

        val record = repository.allRecords.first().single()
        assertThat(record.workDate.toString()).isEqualTo("2026-09-20")
        assertThat(record.netMillis(Stamp.now(clock))).isEqualTo(Duration.ofHours(8).toMillis())
    }

    @Test
    fun `monthly query attributes an overnight shift to the starting month`() = runTest {
        clockInstant = Instant.parse("2026-09-30T13:00:00Z") // 22:00 JST on the 30th
        repository.clockIn()
        advance(Duration.ofHours(8))
        repository.clockOut()

        val september = repository.recordsInMonth(java.time.YearMonth.of(2026, 9)).first()
        val october = repository.recordsInMonth(java.time.YearMonth.of(2026, 10)).first()

        assertThat(september).hasSize(1)
        assertThat(october).isEmpty()
    }

    @Test
    fun `editing a saved record replaces its breaks rather than appending`() = runTest {
        repository.clockIn()
        repository.startBreak()
        repository.endBreak()
        repository.clockOut()

        val record = repository.allRecords.first().single()
        repository.save(record.copy(breaks = emptyList()))

        assertThat(repository.allRecords.first().single().breaks).isEmpty()
    }
}
