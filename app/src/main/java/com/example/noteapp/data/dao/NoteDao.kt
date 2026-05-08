package com.example.noteapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.noteapp.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND (title LIKE '%' || :keyword || '%' OR content LIKE '%' || :keyword || '%') ORDER BY updatedAt DESC LIMIT 20")
    suspend fun searchAll(keyword: String): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("UPDATE notes SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 0")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 0 AND date(createdAt/1000, 'unixepoch', 'localtime') = date(:dateMillis/1000, 'unixepoch', 'localtime')")
    suspend fun countByDate(dateMillis: Long): Int

    @Query("SELECT DISTINCT date(createdAt/1000, 'unixepoch', 'localtime') as day FROM notes ORDER BY day ASC")
    suspend fun getAllActiveDates(): List<String>

    @Query("SELECT * FROM notes WHERE date(createdAt/1000, 'unixepoch', 'localtime') = :dateStr ORDER BY updatedAt DESC")
    suspend fun getByDate(dateStr: String): List<Note>

    @Query("SELECT date(createdAt/1000, 'unixepoch', 'localtime') as day, COUNT(*) as cnt FROM notes WHERE isDeleted = 0 GROUP BY day")
    suspend fun getDailyCounts(): List<DayCount>

    // Recycle bin
    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedNotes(): Flow<List<Note>>

    @Query("UPDATE notes SET isDeleted = 0 WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun permanentDelete(id: Long)

    @Query("UPDATE notes SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE notes SET reminderTime = :time WHERE id = :id")
    suspend fun setReminderTime(id: Long, time: Long)

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavorites(): Flow<List<Note>>

    @Query("SELECT content FROM notes")
    suspend fun getAllContents(): List<String>
}

data class DayCount(val day: String, val cnt: Int)
