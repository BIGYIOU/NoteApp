package com.example.noteapp.ui.note

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteapp.data.db.AppDatabase
import com.example.noteapp.data.model.Note
import com.example.noteapp.util.ImageUtils
import com.example.noteapp.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val noteDao = db.noteDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Note>>(emptyList())
    val searchResults: StateFlow<List<Note>> = _searchResults.asStateFlow()

    val notes: StateFlow<List<Note>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            noteDao.getAll()
        } else {
            // searchResults are set via searchNotes() for the main page
            noteDao.getAll()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearch(query: String) {
        _searchQuery.value = query
    }

    fun searchNotes(keyword: String) {
        if (keyword.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = noteDao.searchAll(keyword.trim())
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun createNote(callback: (Long) -> Unit) {
        viewModelScope.launch {
            val id = noteDao.insert(Note(title = "", content = ""))
            Logger.i("NoteVM", "创建笔记 id=$id")
            callback(id)
        }
    }

    suspend fun insertNote(title: String, content: String): Long {
        val id = noteDao.insert(Note(title = title, content = content))
        Logger.i("NoteVM", "保存新笔记 id=$id")
        return id
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.update(note.copy(modifyCount = note.modifyCount + 1, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            Logger.i("NoteVM", "移入回收站 id=${note.id} title=${note.title}")
            noteDao.update(note.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
        }
    }

    val deletedNotes: StateFlow<List<Note>> = noteDao.getDeletedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreNote(id: Long) {
        viewModelScope.launch {
            Logger.i("NoteVM", "恢复笔记 id=$id")
            noteDao.restore(id)
        }
    }

    fun permanentDeleteNote(id: Long, content: String) {
        viewModelScope.launch {
            Logger.i("NoteVM", "彻底删除 id=$id")
            noteDao.permanentDelete(id)
            ImageUtils.deleteReferencedImages(content)
        }
    }

    fun updateNoteTime(note: Note, newTimeMillis: Long) {
        viewModelScope.launch {
            Logger.i("NoteVM", "修改时间 id=${note.id} time=$newTimeMillis")
            noteDao.update(note.copy(createdAt = newTimeMillis, updatedAt = newTimeMillis))
        }
    }

    fun togglePinned(note: Note) {
        viewModelScope.launch {
            noteDao.setPinned(note.id, !note.isPinned)
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            noteDao.setFavorite(note.id, !note.isFavorite)
        }
    }

    fun setReminderTime(noteId: Long, time: Long) {
        viewModelScope.launch {
            noteDao.setReminderTime(noteId, time)
        }
    }

    val favoriteNotes: StateFlow<List<Note>> = noteDao.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showFavorites = MutableStateFlow(false)
    val showFavorites = _showFavorites.asStateFlow()

    fun setShowFavorites(show: Boolean) {
        _showFavorites.value = show
    }

    suspend fun getNoteById(id: Long): Note? = noteDao.getById(id)

    // Stats for sidebar
    private val _noteCount = MutableStateFlow(0)
    val noteCount: StateFlow<Int> = _noteCount.asStateFlow()

    private val _consecutiveDays = MutableStateFlow(0)
    val consecutiveDays: StateFlow<Int> = _consecutiveDays.asStateFlow()

    private val _dailyCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val dailyCounts: StateFlow<Map<String, Int>> = _dailyCounts.asStateFlow()

    fun refreshStats() {
        viewModelScope.launch {
            _noteCount.value = noteDao.countAll()
            _consecutiveDays.value = calcConsecutiveDays()
            _dailyCounts.value = noteDao.getDailyCounts().associate { it.day to it.cnt }
        }
    }

    suspend fun getNotesByDate(dateStr: String): List<Note> = noteDao.getByDate(dateStr)

    private suspend fun calcConsecutiveDays(): Int {
        val dates = noteDao.getAllActiveDates()
        if (dates.isEmpty()) return 0
        var streak = 1
        var maxStreak = 1
        for (i in 1 until dates.size) {
            if (dates[i] == dates[i - 1]) continue
            val prev = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dates[i - 1])
            val curr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dates[i])
            if (prev != null && curr != null) {
                val diff = (curr.time - prev.time) / (1000 * 60 * 60 * 24)
                if (diff == 1L) {
                    streak++
                    if (streak > maxStreak) maxStreak = streak
                } else {
                    streak = 1
                }
            }
        }
        return maxStreak
    }
}
