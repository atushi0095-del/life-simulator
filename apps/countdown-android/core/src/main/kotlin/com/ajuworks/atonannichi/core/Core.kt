package com.ajuworks.atonannichi.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** 当日を過ぎたイベントの扱い。 */
enum class AfterMode { COUNT_UP, END }

/** 表示状態。 */
sealed interface Countdown {
    data class Until(val days: Long) : Countdown
    data object Today : Countdown
    data class Since(val days: Long) : Countdown
    data object Ended : Countdown
}

object DayCount {
    /** 暦日の差。時刻は見ない（「あと何日」は日付で数える）。うるう年・年跨ぎも暦どおり。 */
    fun daysUntil(target: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(today, target)

    fun of(target: LocalDate, today: LocalDate, after: AfterMode): Countdown {
        val d = daysUntil(target, today)
        return when {
            d > 0 -> Countdown.Until(d)
            d == 0L -> Countdown.Today
            after == AfterMode.COUNT_UP -> Countdown.Since(-d)
            else -> Countdown.Ended
        }
    }

    fun today(zone: ZoneId = ZoneId.systemDefault(), nowMillis: Long = System.currentTimeMillis()): LocalDate =
        java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()

    /** 並び順: これからのもの（近い順）→ 当日 → 過ぎたもの（新しい順）。 */
    fun sortKey(target: LocalDate, today: LocalDate): Long {
        val d = daysUntil(target, today)
        return if (d >= 0) d else 1_000_000L - d
    }
}

/** 通知の判定。何日前に通知するかをビットで持つ。 */
object NotifyRules {
    val OFFSETS = listOf(30, 7, 3, 1, 0)
    fun bitOf(offset: Int): Int = 1 shl OFFSETS.indexOf(offset).also { require(it >= 0) }

    val DEFAULT: Int = bitOf(7) or bitOf(1) or bitOf(0)

    fun isOn(mask: Int, offset: Int) = mask and bitOf(offset) != 0

    fun toggle(mask: Int, offset: Int) = mask xor bitOf(offset)

    /**
     * 今日通知すべき「何日前」を返す（なければ null）。
     * [sentMask] は同じイベント・同じ日付に対して通知済みの印。二重通知を防ぐ。
     */
    fun due(target: LocalDate, today: LocalDate, enabledMask: Int, sentMask: Int): Int? {
        val d = DayCount.daysUntil(target, today)
        if (d < 0 || d > 30) return null
        val offset = d.toInt()
        if (offset !in OFFSETS) return null
        if (!isOn(enabledMask, offset) || isOn(sentMask, offset)) return null
        return offset
    }
}

/**
 * ウィジェットの描き直しと通知確認のタイミング。
 * 毎分の更新はしない。「日付が変わった直後」と「朝9時（通知）」の1日2回だけ。
 */
object TickSchedule {
    val MIDNIGHT: LocalTime = LocalTime.of(0, 0, 30)
    val MORNING: LocalTime = LocalTime.of(9, 0)

    fun next(now: ZonedDateTime): ZonedDateTime {
        val today = now.toLocalDate()
        val candidates = listOf(
            today.atTime(MIDNIGHT), today.atTime(MORNING),
            today.plusDays(1).atTime(MIDNIGHT), today.plusDays(1).atTime(MORNING),
        ).map { it.atZone(now.zone) }
        return candidates.filter { it.isAfter(now) }.minBy { it.toInstant() }
    }

    fun isNotifyTime(now: ZonedDateTime): Boolean = !now.toLocalTime().isBefore(MORNING)
}

/** 写真の上の文字を読みやすくするための暗幕の濃さ（0〜1）。平均輝度 0〜255 から決める。 */
object Readability {
    fun scrimAlpha(meanLuminance: Double): Float {
        // 明るい写真ほど濃く。暗い写真でも最低限は掛けて、白文字の縁を締める
        val t = (meanLuminance / 255.0).coerceIn(0.0, 1.0)
        return (0.18 + 0.42 * t).toFloat()
    }

    fun luminance(r: Int, g: Int, b: Int): Double = 0.2126 * r + 0.7152 * g + 0.0722 * b
}
