package com.ajuworks.worklog.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CsvExportTest {

    private val now = stamp(2026, 9, 21, 12, 0)

    @Test
    fun `rows match the documented shape`() {
        val csv = CsvExport.render(
            listOf(
                session(
                    stamp(2026, 9, 20, 9, 3),
                    stamp(2026, 9, 20, 18, 17),
                    listOf(breakEntry(stamp(2026, 9, 20, 12, 0), stamp(2026, 9, 20, 13, 0))),
                )
            ),
            now = now,
            includeBom = false,
        )

        assertThat(csv.lines()[1]).isEqualTo("2026/09/20,09:03,18:17,01:00,08:14,")
    }

    @Test
    fun `a BOM is written so Excel reads it as UTF-8`() {
        val csv = CsvExport.render(emptyList(), now)

        assertThat(csv.startsWith(CsvExport.BOM)).isTrue()
        assertThat(csv).contains("日付,出勤,退勤,休憩,実働,メモ")
    }

    @Test
    fun `rows are ordered oldest first regardless of input order`() {
        val csv = CsvExport.render(
            listOf(
                session(stamp(2026, 9, 20, 9, 0), stamp(2026, 9, 20, 18, 0)),
                session(stamp(2026, 9, 18, 9, 0), stamp(2026, 9, 18, 18, 0)),
            ),
            now = now,
            includeBom = false,
        )

        val dates = csv.lines().drop(1).filter { it.isNotBlank() }.map { it.substringBefore(',') }
        assertThat(dates).containsExactly("2026/09/18", "2026/09/20").inOrder()
    }

    @Test
    fun `an overnight row is dated by its clock-in day`() {
        val csv = CsvExport.render(
            listOf(session(stamp(2026, 9, 30, 22, 0), stamp(2026, 10, 1, 6, 0))),
            now = stamp(2026, 10, 2, 9, 0),
            includeBom = false,
        )

        assertThat(csv.lines()[1]).isEqualTo("2026/09/30,22:00,06:00,00:00,08:00,")
    }

    @Test
    fun `a running session exports with an empty clock-out`() {
        val csv = CsvExport.render(
            listOf(session(stamp(2026, 9, 21, 9, 0))),
            now = stamp(2026, 9, 21, 12, 0),
            includeBom = false,
        )

        assertThat(csv.lines()[1]).isEqualTo("2026/09/21,09:00,,00:00,03:00,")
    }

    @Test
    fun `notes containing commas and quotes are escaped`() {
        val record = session(stamp(2026, 9, 20, 9, 0), stamp(2026, 9, 20, 18, 0))
            .copy(note = "A, \"B\"")

        val csv = CsvExport.render(listOf(record), now, includeBom = false)

        assertThat(csv.lines()[1]).endsWith(",\"A, \"\"B\"\"\"")
    }

    @Test
    fun `english headers are available`() {
        val csv = CsvExport.render(emptyList(), now, CsvExport.Headers.ENGLISH, includeBom = false)

        assertThat(csv.lines().first()).isEqualTo("Date,Clock in,Clock out,Break,Worked,Note")
    }
}
