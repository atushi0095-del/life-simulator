package com.ajuworks.atonannichi.widget

import android.content.Context
import com.ajuworks.atonannichi.R
import com.ajuworks.atonannichi.core.Countdown
import com.ajuworks.atonannichi.data.EventEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** カード・ウィジェットに載せる文字。 */
data class Labels(val headline: String, val number: String, val unit: String, val dateLine: String, val bigWord: Boolean) {
    companion object {
        private val dateFmt = DateTimeFormatter.ofPattern("yyyy.MM.dd")

        fun of(context: Context, e: EventEntity, today: LocalDate): Labels {
            val date = e.date.format(dateFmt) + (e.time?.let { " %02d:%02d".format(it.hour, it.minute) } ?: "")
            return when (val c = e.countdown(today)) {
                is Countdown.Until -> Labels(
                    context.getString(R.string.headline_until, e.title),
                    c.days.toString(),
                    context.getString(if (c.days == 1L) R.string.unit_day else R.string.unit_days),
                    date,
                    bigWord = false,
                )
                Countdown.Today -> Labels(e.title, context.getString(R.string.today_word), "", date, bigWord = true)
                is Countdown.Since -> Labels(
                    context.getString(R.string.headline_since, e.title),
                    c.days.toString(),
                    context.getString(R.string.unit_ago),
                    date,
                    bigWord = false,
                )
                Countdown.Ended -> Labels(e.title, context.getString(R.string.ended_word), "", date, bigWord = true)
            }
        }
    }
}
