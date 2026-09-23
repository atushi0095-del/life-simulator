package com.ajuworks.atonannichi

import android.app.AlarmManager
import androidx.test.core.app.ApplicationProvider
import com.ajuworks.atonannichi.core.TickSchedule
import com.ajuworks.atonannichi.widget.Tick
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.ZonedDateTime

/**
 * The countdown only has to change when the date does, so the app schedules two
 * inexact alarms a day instead of ticking. A regression here would either stop
 * the widget updating at midnight or start waking the device far too often, and
 * neither is visible in the UI, so it is asserted directly.
 */
@RunWith(RobolectricTestRunner::class)
class TickAlarmTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test
    fun `scheduling sets exactly one pending alarm in the future`() {
        Tick.scheduleNext(context)

        val scheduled = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms

        assertThat(scheduled).hasSize(1)
        assertThat(scheduled.single().triggerAtTime).isGreaterThan(System.currentTimeMillis())
    }

    @Test
    fun `rescheduling replaces the alarm rather than stacking them up`() {
        repeat(3) { Tick.scheduleNext(context) }

        val scheduled = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms

        assertThat(scheduled).hasSize(1)
    }

    @Test
    fun `the next alarm is the next midnight or morning slot`() {
        Tick.scheduleNext(context)

        val expected = TickSchedule.next(ZonedDateTime.now()).toInstant().toEpochMilli()
        val actual = shadowOf(context.getSystemService(AlarmManager::class.java))
            .scheduledAlarms.single().triggerAtTime

        // A second of slack: the test and the app each call now() separately.
        assertThat(actual).isIn(com.google.common.collect.Range.closed(expected - 1_000, expected + 1_000))
    }
}
