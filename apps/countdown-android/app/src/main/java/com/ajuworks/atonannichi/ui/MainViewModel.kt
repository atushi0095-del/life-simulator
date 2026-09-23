package com.ajuworks.atonannichi.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ajuworks.atonannichi.core.DayCount
import com.ajuworks.atonannichi.core.TickSchedule
import com.ajuworks.atonannichi.data.EventDatabase
import com.ajuworks.atonannichi.data.EventEntity
import com.ajuworks.atonannichi.data.Photos
import com.ajuworks.atonannichi.widget.Tick
import com.ajuworks.atonannichi.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZonedDateTime

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = EventDatabase.get(app).dao()

    /** 画面に戻るたびに更新する「今日」（日付をまたいで開きっぱなしでもずれない） */
    private val _today = MutableStateFlow(DayCount.today())
    val today: StateFlow<LocalDate> = _today.asStateFlow()
    fun refreshToday() { _today.value = DayCount.today() }

    val events: StateFlow<List<EventEntity>> = combine(dao.observeAll(), _today) { list, t ->
        list.sortedBy { DayCount.sortKey(it.date, t) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _draft = MutableStateFlow<EventEntity?>(null)
    val draft: StateFlow<EventEntity?> = _draft.asStateFlow()
    private var draftOriginalPhoto: String? = null
    private val importedThisEdit = mutableListOf<String>()

    var savesForAd = 0
        private set

    fun newDraft() {
        begin(EventEntity(title = "", dateEpochDay = LocalDate.now().plusDays(30).toEpochDay()))
    }

    fun editDraft(id: Long) = viewModelScope.launch {
        dao.byId(id)?.let(::begin)
    }

    private fun begin(e: EventEntity) {
        draftOriginalPhoto = e.photoFile
        importedThisEdit.clear()
        _draft.value = e
    }

    fun update(e: EventEntity) { _draft.value = e }

    fun pickPhoto(uri: Uri) = viewModelScope.launch {
        val name = withContext(Dispatchers.IO) { Photos.import(getApplication(), uri) } ?: return@launch
        importedThisEdit += name
        _draft.value = _draft.value?.copy(photoFile = name, design = com.ajuworks.atonannichi.core.Design.PHOTO.name)
    }

    fun removePhoto() { _draft.value = _draft.value?.copy(photoFile = null) }

    /** 編集をやめた。取り込んだだけの写真は消す。 */
    fun cancelDraft() {
        val app = getApplication<Application>()
        importedThisEdit.forEach { Photos.delete(app, it) }
        importedThisEdit.clear()
        _draft.value = null
    }

    suspend fun saveDraft(): Boolean {
        val d = _draft.value ?: return false
        if (d.title.isBlank()) return false
        val app = getApplication<Application>()
        withContext(Dispatchers.IO) {
            val old = if (d.id != 0L) dao.byId(d.id) else null
            // 日付や通知設定を変えたら、通知済みの印を消して新しい日付で通知し直す
            val sent = if (old == null || old.dateEpochDay != d.dateEpochDay) 0 else d.sentMask
            dao.upsert(d.copy(title = d.title.trim(), sentMask = sent))
            // 使われなくなった写真を消す
            (importedThisEdit + listOfNotNull(draftOriginalPhoto)).filter { it != d.photoFile }.forEach { Photos.delete(app, it) }
            importedThisEdit.clear()
            WidgetUpdater.updateAll(app)
            if (TickSchedule.isNotifyTime(ZonedDateTime.now())) Tick.notifyDue(app)
        }
        savesForAd++
        _draft.value = null
        return true
    }

    fun delete(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        val app = getApplication<Application>()
        val e = dao.byId(id) ?: return@launch
        dao.delete(id)
        Photos.delete(app, e.photoFile)
        importedThisEdit.forEach { Photos.delete(app, it) }
        importedThisEdit.clear()
        _draft.value = null
        WidgetUpdater.updateAll(app)
    }
}
