package com.ajuworks.worklog.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ajuworks.worklog.appContainer
import com.ajuworks.worklog.core.Stamp
import com.ajuworks.worklog.core.WorkAggregator
import com.ajuworks.worklog.core.WorkRecord
import com.ajuworks.worklog.core.WorkSummary
import com.ajuworks.worklog.domain.WorkActions
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val active: WorkRecord? = null,
    val now: Stamp,
    val today: WorkSummary = WorkSummary(),
    val week: WorkSummary = WorkSummary(),
    val month: WorkSummary = WorkSummary(),
    val loaded: Boolean = false,
) {
    val isWorking: Boolean get() = active != null
    val isOnBreak: Boolean get() = active?.isOnBreak == true

    /** Elapsed time of the running session, ready to render. */
    val elapsedMillis: Long get() = active?.netMillis(now) ?: 0L
    val currentBreakMillis: Long get() = active?.breakMillis(now) ?: 0L
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = application.appContainer
    private val repository = container.workRepository
    private val actions = WorkActions(application)

    /**
     * A redraw tick, not a timer. Nothing is accumulated here - the tick only
     * causes the elapsed figure to be recomputed from the stored clock-in time,
     * and it only runs while the home screen is collecting. The number stays
     * correct whether the app was closed for a second or for a day.
     */
    private val tick = MutableStateFlow(repository.now())
    private var tickJob: Job? = null

    /**
     * The query window, recomputed whenever the calendar day or the week-start
     * setting changes.
     *
     * The window is derived from both (WorkAggregator.summaryWindow) rather
     * than fixed when the ViewModel is built: a screen left open across
     * midnight - or across a month boundary - would otherwise keep querying
     * yesterday's range, and a week-start change would not take effect until
     * the screen was recreated.
     */
    private val windowRecords: Flow<List<WorkRecord>> =
        combine(
            container.settingsRepository.settings.map { it.weekStart }.distinctUntilChanged(),
            tick.map { it.localDate }.distinctUntilChanged(),
        ) { weekStart, today -> weekStart to today }
            .distinctUntilChanged()
            .flatMapLatest { (weekStart, today) ->
                val window = WorkAggregator.summaryWindow(today, weekStart)
                repository.recordsBetween(window.start, window.endInclusive.plusDays(1))
            }

    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.activeRecord,
            windowRecords,
            container.settingsRepository.settings,
            tick,
        ) { active, records, settings, now ->
            val today = now.localDate
            HomeUiState(
                active = active,
                now = now,
                today = WorkAggregator.summarize(WorkAggregator.onDate(records, today), now),
                week = WorkAggregator.summarize(
                    WorkAggregator.inWeekOf(records, today, settings.weekStart),
                    now,
                ),
                month = WorkAggregator.summarize(
                    WorkAggregator.inMonth(records, YearMonth.from(today)),
                    now,
                ),
                loaded = true,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(now = repository.now()),
        )

    /** Called from the composable's lifecycle so it stops when the screen does. */
    fun startTicking() {
        if (tickJob?.isActive == true) return
        tickJob = viewModelScope.launch {
            while (true) {
                tick.value = repository.now()
                // Only minutes are displayed, so a per-second redraw would burn
                // battery to show the same string.
                delay(1_000L)
            }
        }
    }

    fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    fun clockIn() = viewModelScope.launch { actions.clockIn() }

    fun clockOut() = viewModelScope.launch { actions.clockOut() }

    fun startBreak() = viewModelScope.launch { actions.startBreak() }

    fun endBreak() = viewModelScope.launch { actions.endBreak() }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras,
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return HomeViewModel(app) as T
            }
        }
    }
}
