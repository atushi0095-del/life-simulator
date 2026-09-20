package com.ajuworks.worklog.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ajuworks.worklog.appContainer
import com.ajuworks.worklog.core.CsvExport
import com.ajuworks.worklog.core.Stamp
import com.ajuworks.worklog.core.WorkAggregator
import com.ajuworks.worklog.core.WorkRecord
import com.ajuworks.worklog.core.WorkSummary
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val month: YearMonth,
    val records: List<WorkRecord> = emptyList(),
    val summary: WorkSummary = WorkSummary(),
    val now: Stamp,
    val earliestMonth: YearMonth? = null,
    val loaded: Boolean = false,
) {
    val canGoOlder: Boolean
        get() = earliestMonth?.let { month.isAfter(it) } ?: false

    val canGoNewer: Boolean
        get() = month.isBefore(YearMonth.from(now.localDate))
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val container = application.appContainer
    private val repository = container.workRepository

    private val selectedMonth = MutableStateFlow(YearMonth.from(repository.now().localDate))

    val uiState: StateFlow<HistoryUiState> = combine(
        selectedMonth,
        selectedMonth.flatMapLatest { repository.recordsInMonth(it) },
        repository.earliestWorkDate,
    ) { month, records, earliest ->
        val now = repository.now()
        HistoryUiState(
            month = month,
            // The query bounds a calendar month by clock-in time, but an
            // overnight shift started on the last day of the previous month
            // must not leak in - the domain rule is the authority.
            records = WorkAggregator.inMonth(records, month),
            summary = WorkAggregator.summarize(WorkAggregator.inMonth(records, month), now),
            now = now,
            earliestMonth = earliest?.let { YearMonth.from(it) },
            loaded = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(
            month = YearMonth.from(repository.now().localDate),
            now = repository.now(),
        ),
    )

    fun showPreviousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun showNextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }

    /** Renders the visible month as CSV. Writing the file is the caller's job. */
    fun csvForCurrentMonth(japanese: Boolean): Pair<String, String> {
        val state = uiState.value
        val content = CsvExport.render(
            records = state.records,
            now = state.now,
            headers = if (japanese) CsvExport.Headers.JAPANESE else CsvExport.Headers.ENGLISH,
        )
        val name = "worklog-${state.month}.csv"
        return name to content
    }

    /** Counts history visits; the caller shows an interstitial on a multiple. */
    suspend fun recordHistoryView(): Int = container.settingsRepository.recordHistoryView()

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return HistoryViewModel(app) as T
            }
        }
    }
}
