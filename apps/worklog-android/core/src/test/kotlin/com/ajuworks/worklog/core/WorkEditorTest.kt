package com.ajuworks.worklog.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Spec sections 10 and 11: manual correction and manual entry. */
class WorkEditorTest {

    private val now = stamp(2026, 9, 25, 12, 0).instant

    @Test
    fun `a clock-out earlier than the clock-in is read as the next day`() {
        val result = WorkEditor.resolve(
            WorkDraft(date = date(2026, 9, 20), startTime = time(22, 0), endTime = time(6, 0)),
            zone = TOKYO,
            now = now,
        )

        assertThat(result.blocked).isFalse()
        assertThat(result.issues).contains(Issue.OvernightAssumed)
        assertThat(result.record.netMillis(stamp(2026, 9, 25, 12, 0))).isEqualTo(hm(8))
        assertThat(result.record.end!!.localDate).isEqualTo(date(2026, 9, 21))
    }

    @Test
    fun `an ordinary day shift reports nothing`() {
        val result = WorkEditor.resolve(
            WorkDraft(
                date = date(2026, 9, 20),
                startTime = time(9, 0),
                endTime = time(18, 0),
                breaks = listOf(BreakDraft(startTime = time(12, 0), endTime = time(13, 0))),
            ),
            zone = TOKYO,
            now = now,
        )

        assertThat(result.issues).isEmpty()
        assertThat(result.record.netMillis(stamp(2026, 9, 25, 12, 0))).isEqualTo(hm(8))
    }

    @Test
    fun `a break inside an overnight shift lands on the following day`() {
        val result = WorkEditor.resolve(
            WorkDraft(
                date = date(2026, 9, 20),
                startTime = time(22, 0),
                endTime = time(6, 0),
                breaks = listOf(BreakDraft(startTime = time(1, 0), endTime = time(1, 30))),
            ),
            zone = TOKYO,
            now = now,
        )

        assertThat(result.blocked).isFalse()
        assertThat(result.record.breaks.single().start.localDate).isEqualTo(date(2026, 9, 21))
        assertThat(result.record.netMillis(stamp(2026, 9, 25, 12, 0))).isEqualTo(hm(7, 30))
    }

    @Test
    fun `a break after the clock-out is refused`() {
        val result = WorkEditor.resolve(
            WorkDraft(
                date = date(2026, 9, 20),
                startTime = time(9, 0),
                endTime = time(12, 0),
                breaks = listOf(BreakDraft(startTime = time(14, 0), endTime = time(15, 0))),
            ),
            zone = TOKYO,
            now = now,
        )

        assertThat(result.blocked).isTrue()
        assertThat(result.issues).contains(Issue.BreakOutsideSession(0))
    }

    @Test
    fun `breaks that swallow the whole session are refused`() {
        val result = WorkEditor.resolve(
            WorkDraft(
                date = date(2026, 9, 20),
                startTime = time(9, 0),
                endTime = time(10, 0),
                breaks = listOf(BreakDraft(startTime = time(9, 0), endTime = time(10, 0))),
            ),
            zone = TOKYO,
            now = now,
        )

        assertThat(result.blocked).isTrue()
        assertThat(result.issues).contains(Issue.BreaksExceedWork)
    }

    @Test
    fun `overlapping breaks are reported but still saveable`() {
        val result = WorkEditor.resolve(
            WorkDraft(
                date = date(2026, 9, 20),
                startTime = time(9, 0),
                endTime = time(18, 0),
                breaks = listOf(
                    BreakDraft(startTime = time(12, 0), endTime = time(13, 0)),
                    BreakDraft(startTime = time(12, 30), endTime = time(13, 30)),
                ),
            ),
            zone = TOKYO,
            now = now,
        )

        assertThat(result.blocked).isFalse()
        assertThat(result.issues).contains(Issue.OverlappingBreaks)
        assertThat(result.record.breakMillis(stamp(2026, 9, 25, 12, 0))).isEqualTo(hm(1, 30))
    }

    @Test
    fun `an implausibly long session warns but is allowed`() {
        val result = WorkEditor.resolve(
            WorkDraft(date = date(2026, 9, 20), startTime = time(6, 0), endTime = time(23, 0)),
            zone = TOKYO,
            now = now,
        )

        assertThat(result.blocked).isFalse()
        assertThat(result.hasWarnings).isTrue()
        assertThat(result.issues).contains(Issue.ImplausiblyLong(17))
    }

    @Test
    fun `a session ending in the future warns`() {
        val result = WorkEditor.resolve(
            WorkDraft(date = date(2026, 9, 26), startTime = time(9, 0), endTime = time(18, 0)),
            zone = TOKYO,
            now = now,
        )

        assertThat(result.issues).contains(Issue.EndsInFuture)
        assertThat(result.blocked).isFalse()
    }

    @Test
    fun `quick entry with total break minutes produces exact totals`() {
        val result = WorkEditor.fromTotals(
            date = date(2026, 9, 19),
            startTime = time(9, 0),
            endTime = time(18, 0),
            breakMinutes = 60,
            zone = TOKYO,
            now = now,
        )

        assertThat(result.blocked).isFalse()
        val at = stamp(2026, 9, 25, 12, 0)
        assertThat(result.record.breakMillis(at)).isEqualTo(hm(1))
        assertThat(result.record.netMillis(at)).isEqualTo(hm(8))
        assertThat(result.record.breaks.single().start.localTime).isEqualTo(time(13, 0))
    }

    @Test
    fun `quick entry with no break makes no break row`() {
        val result = WorkEditor.fromTotals(
            date = date(2026, 9, 19),
            startTime = time(9, 0),
            endTime = time(17, 0),
            breakMinutes = 0,
            zone = TOKYO,
            now = now,
        )

        assertThat(result.record.breaks).isEmpty()
        assertThat(result.record.netMillis(stamp(2026, 9, 25, 12, 0))).isEqualTo(hm(8))
    }

    @Test
    fun `quick entry refuses a break longer than the shift`() {
        val result = WorkEditor.fromTotals(
            date = date(2026, 9, 19),
            startTime = time(9, 0),
            endTime = time(10, 0),
            breakMinutes = 120,
            zone = TOKYO,
            now = now,
        )

        assertThat(result.blocked).isTrue()
    }

    @Test
    fun `editing a clock-out updates the totals`() {
        val draft = WorkDraft(date = date(2026, 9, 20), startTime = time(9, 0), endTime = time(18, 0))
        val at = stamp(2026, 9, 25, 12, 0)

        val before = WorkEditor.resolve(draft, TOKYO, now).record.netMillis(at)
        val after = WorkEditor.resolve(draft.copy(endTime = time(19, 30)), TOKYO, now).record.netMillis(at)

        assertThat(before).isEqualTo(hm(9))
        assertThat(after).isEqualTo(hm(10, 30))
    }

    @Test
    fun `a still-running session can be edited without a clock-out`() {
        val result = WorkEditor.resolve(
            WorkDraft(date = date(2026, 9, 25), startTime = time(9, 0), endTime = null),
            zone = TOKYO,
            now = now,
        )

        assertThat(result.blocked).isFalse()
        assertThat(result.record.isRunning).isTrue()
        assertThat(result.record.netMillis(stamp(2026, 9, 25, 12, 0))).isEqualTo(hm(3))
    }
}
