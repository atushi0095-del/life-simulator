package com.ajuworks.worklog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ajuworks.worklog.core.Stamp
import com.ajuworks.worklog.core.WorkTimeFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val DATE_JA = DateTimeFormatter.ofPattern("M月d日(E)", Locale.JAPANESE)
private val DATE_EN = DateTimeFormatter.ofPattern("MMM d (E)", Locale.ENGLISH)
private val SHORT_DATE = DateTimeFormatter.ofPattern("M/d", Locale.US)

/** "09:03" - the wall clock the user saw when the stamp was taken. */
fun timeText(stamp: Stamp): String = stamp.localTime.format(TIME)

fun shortDateText(stamp: Stamp): String = stamp.localDate.format(SHORT_DATE)

fun shortDateText(date: java.time.LocalDate): String = date.format(SHORT_DATE)

fun longDateText(date: java.time.LocalDate, japanese: Boolean): String =
    date.format(if (japanese) DATE_JA else DATE_EN)

/**
 * How durations are worded. Japanese gets "8時間14分", everything else "8h 14m",
 * chosen once at the top of the tree so no screen can word it differently.
 */
val LocalDurationFormatter = staticCompositionLocalOf<(Long) -> String> {
    WorkTimeFormat::toJapanese
}

@Composable
fun rememberDurationFormatter(): (Long) -> String {
    val language = LocalConfiguration.current.locales[0].language
    return if (language == Locale.JAPANESE.language) {
        WorkTimeFormat::toJapanese
    } else {
        WorkTimeFormat::toEnglish
    }
}

@Composable
fun isJapanese(): Boolean =
    LocalConfiguration.current.locales[0].language == Locale.JAPANESE.language

/**
 * Runs [onStart] while the screen is resumed and [onStop] when it is not, so
 * the elapsed-time redraw stops dead the moment the app is backgrounded.
 */
@Composable
fun TickEffect(onStart: () -> Unit, onStop: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> onStart()
                Lifecycle.Event.ON_PAUSE -> onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onStop()
        }
    }
}
