package com.ajuworks.worklog.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ajuworks.worklog.appContainer
import com.ajuworks.worklog.core.BreakDraft
import com.ajuworks.worklog.core.Issue
import com.ajuworks.worklog.core.WorkDraft
import com.ajuworks.worklog.core.WorkEditor
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class EditUiState(
    val draft: WorkDraft,
    val issues: List<Issue> = emptyList(),
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    val blocked: Boolean get() = issues.any { it.severity == com.ajuworks.worklog.core.Severity.ERROR }
}

/**
 * Backs both "edit this record" and "+ add a shift". A new record is just a
 * draft with id 0, so there is one screen and one set of validation rules
 * rather than two that can disagree.
 */
class EditRecordViewModel(
    application: Application,
    private val recordId: Long,
) : AndroidViewModel(application) {

    private val container = application.appContainer
    private val repository = container.workRepository

    private val _state = MutableStateFlow(
        EditUiState(
            draft = WorkDraft(
                date = LocalDate.now(repository.zone),
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(18, 0),
            ),
            isNew = true,
            loading = recordId != 0L,
        )
    )
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    init {
        if (recordId != 0L) {
            viewModelScope.launch {
                val record = repository.record(recordId).first()
                if (record != null) {
                    _state.value = EditUiState(
                        draft = WorkDraft(
                            id = record.id,
                            date = record.workDate,
                            startTime = record.start.localTime,
                            endTime = record.end?.localTime,
                            breaks = record.breaks.map {
                                BreakDraft(
                                    id = it.id,
                                    startTime = it.start.localTime,
                                    endTime = it.end?.localTime,
                                )
                            },
                            note = record.note,
                        ),
                        isNew = false,
                        loading = false,
                    )
                    revalidate()
                } else {
                    _state.value = _state.value.copy(loading = false)
                }
            }
        }
    }

    fun setDate(date: LocalDate) = update { it.copy(date = date) }

    fun setStartTime(time: LocalTime) = update { it.copy(startTime = time) }

    fun setEndTime(time: LocalTime?) = update { it.copy(endTime = time) }

    fun setNote(note: String) = update { it.copy(note = note) }

    fun addBreak() = update {
        it.copy(breaks = it.breaks + BreakDraft(startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0)))
    }

    fun removeBreak(index: Int) = update {
        it.copy(breaks = it.breaks.filterIndexed { i, _ -> i != index })
    }

    fun setBreakStart(index: Int, time: LocalTime) = update { draft ->
        draft.copy(
            breaks = draft.breaks.mapIndexed { i, b ->
                if (i == index) b.copy(startTime = time) else b
            },
        )
    }

    fun setBreakEnd(index: Int, time: LocalTime?) = update { draft ->
        draft.copy(
            breaks = draft.breaks.mapIndexed { i, b ->
                if (i == index) b.copy(endTime = time) else b
            },
        )
    }

    fun save() {
        val current = _state.value
        val result = WorkEditor.resolve(
            draft = current.draft,
            zone = repository.zone,
            now = repository.now().instant,
        )
        if (result.blocked) {
            _state.value = current.copy(issues = result.issues)
            return
        }
        viewModelScope.launch {
            repository.save(result.record)
            // An edit can change whether a session is still open, so the
            // reminder and the widget have to be re-derived, not assumed.
            com.ajuworks.worklog.widget.WorkLogWidget.refresh(getApplication())
            _state.value = _state.value.copy(saved = true, issues = result.issues)
        }
    }

    fun delete() {
        if (recordId == 0L) return
        viewModelScope.launch {
            repository.delete(recordId)
            com.ajuworks.worklog.widget.WorkLogWidget.refresh(getApplication())
            _state.value = _state.value.copy(deleted = true)
        }
    }

    private fun update(transform: (WorkDraft) -> WorkDraft) {
        _state.value = _state.value.copy(draft = transform(_state.value.draft))
        revalidate()
    }

    private fun revalidate() {
        val result = WorkEditor.resolve(
            draft = _state.value.draft,
            zone = repository.zone,
            now = repository.now().instant,
        )
        _state.value = _state.value.copy(issues = result.issues)
    }

    companion object {
        fun factory(recordId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                    return EditRecordViewModel(app, recordId) as T
                }
            }
    }
}
