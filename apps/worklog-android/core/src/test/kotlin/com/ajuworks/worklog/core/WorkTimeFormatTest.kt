package com.ajuworks.worklog.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkTimeFormatTest {

    @Test
    fun `zero renders as the empty-day placeholder`() {
        assertThat(WorkTimeFormat.toJapanese(0)).isEqualTo("0時間00分")
        assertThat(WorkTimeFormat.toEnglish(0)).isEqualTo("0h 00m")
        assertThat(WorkTimeFormat.toClock(0)).isEqualTo("00:00")
    }

    @Test
    fun `minutes are zero padded`() {
        assertThat(WorkTimeFormat.toJapanese(hm(8, 4))).isEqualTo("8時間04分")
        assertThat(WorkTimeFormat.toClock(hm(8, 4))).isEqualTo("08:04")
    }

    @Test
    fun `seconds are truncated rather than rounded up`() {
        val almost = hm(8, 14) + 59_000L
        assertThat(WorkTimeFormat.toJapanese(almost)).isEqualTo("8時間14分")
    }

    @Test
    fun `monthly totals are not wrapped at 24 hours`() {
        assertThat(WorkTimeFormat.toJapanese(hm(162, 24))).isEqualTo("162時間24分")
        assertThat(WorkTimeFormat.toClock(hm(162, 24))).isEqualTo("162:24")
    }
}
