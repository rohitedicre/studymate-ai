package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE userId = :userId ORDER BY name ASC")
    fun getFoldersByUser(userId: Long): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)
}

@Dao
interface StudyNoteDao {
    @Query("SELECT * FROM study_notes WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getNotesByUser(userId: Long): Flow<List<StudyNoteEntity>>

    @Query("SELECT * FROM study_notes WHERE userId = :userId AND folderId = :folderId ORDER BY updatedAt DESC")
    fun getNotesByFolder(userId: Long, folderId: Long): Flow<List<StudyNoteEntity>>

    @Query("SELECT * FROM study_notes WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    fun searchNotes(userId: Long, query: String): Flow<List<StudyNoteEntity>>

    @Query("SELECT * FROM study_notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Long): StudyNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: StudyNoteEntity): Long

    @Update
    suspend fun updateNote(note: StudyNoteEntity)

    @Query("DELETE FROM study_notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: Long)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcard_sets WHERE userId = :userId ORDER BY createdAt DESC")
    fun getFlashcardSetsByUser(userId: Long): Flow<List<FlashcardSetEntity>>

    @Query("SELECT * FROM flashcard_items WHERE setId = :setId")
    fun getItemsForSet(setId: Long): Flow<List<FlashcardItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: FlashcardSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<FlashcardItemEntity>)

    @Query("UPDATE flashcard_items SET isMastered = :isMastered WHERE id = :itemId")
    suspend fun updateItemMastery(itemId: Long, isMastered: Boolean)

    @Query("DELETE FROM flashcard_sets WHERE id = :setId")
    suspend fun deleteSet(setId: Long)

    @Query("DELETE FROM flashcard_items WHERE setId = :setId")
    suspend fun deleteItemsForSet(setId: Long)
}

@Dao
interface QuizHistoryDao {
    @Query("SELECT * FROM quiz_histories WHERE userId = :userId ORDER BY timestamp DESC")
    fun getQuizHistoryByUser(userId: Long): Flow<List<QuizHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizHistory(history: QuizHistoryEntity): Long

    @Query("DELETE FROM quiz_histories WHERE id = :historyId")
    suspend fun deleteQuizHistory(historyId: Long)
}
