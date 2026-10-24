package com.example.data.repository

import com.example.data.ai.AskAiResult
import com.example.data.ai.FlashcardsResult
import com.example.data.ai.GeminiService
import com.example.data.ai.QuizResult
import com.example.data.ai.SummaryResult
import com.example.data.local.AuthUserState
import com.example.data.local.FlashcardDao
import com.example.data.local.FlashcardItemEntity
import com.example.data.local.FlashcardSetEntity
import com.example.data.local.FolderDao
import com.example.data.local.FolderEntity
import com.example.data.local.QuizHistoryDao
import com.example.data.local.QuizHistoryEntity
import com.example.data.local.SessionManager
import com.example.data.local.StudyNoteDao
import com.example.data.local.StudyNoteEntity
import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

class StudyMateRepository(
    private val userDao: UserDao,
    private val folderDao: FolderDao,
    private val noteDao: StudyNoteDao,
    private val flashcardDao: FlashcardDao,
    private val quizDao: QuizHistoryDao,
    private val sessionManager: SessionManager,
    private val geminiService: GeminiService
) {
    val authUserState: StateFlow<AuthUserState> = sessionManager.userState

    // ---- Auth ----
    suspend fun signUp(username: String, email: String, password: String):Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        val existing = userDao.getUserByEmail(cleanEmail)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("An account with $cleanEmail already exists."))
        }
        val salt = SessionManager.generateSalt()
        val hash = SessionManager.hashPassword(password, salt)
        val user = UserEntity(
            email = cleanEmail,
            username = username.trim(),
            passwordHash = hash,
            salt = salt
        )
        val id = userDao.insertUser(user)
        val created = user.copy(id = id)
        sessionManager.saveUserSession(id, created.username, created.email)

        // Seed default course folders
        folderDao.insertFolder(FolderEntity(userId = id, name = "Biology 101", colorHex = "#10B981"))
        folderDao.insertFolder(FolderEntity(userId = id, name = "Computer Science", colorHex = "#3B82F6"))
        folderDao.insertFolder(FolderEntity(userId = id, name = "Economics 201", colorHex = "#F59E0B"))

        return Result.success(created)
    }

    suspend fun logIn(email: String, password: String): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(cleanEmail)
            ?: return Result.failure(IllegalArgumentException("No account found with this email."))
        val inputHash = SessionManager.hashPassword(password, user.salt)
        if (inputHash != user.passwordHash) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }
        sessionManager.saveUserSession(user.id, user.username, user.email)
        return Result.success(user)
    }

    fun logOut() {
        sessionManager.clearSession()
    }

    fun getCustomApiKey(): String = sessionManager.getCustomApiKey()
    fun saveCustomApiKey(key: String) = sessionManager.saveCustomApiKey(key)

    // Current effective user id (fallback to guest ID 1 if not logged in so app is immediately usable)
    val currentUserId: Long
        get() = if (sessionManager.userState.value.isLoggedIn) sessionManager.userState.value.userId else 1L

    // ---- Folders ----
    fun getFolders(): Flow<List<FolderEntity>> = folderDao.getFoldersByUser(currentUserId)

    suspend fun createFolder(name: String, colorHex: String): Long {
        return folderDao.insertFolder(FolderEntity(userId = currentUserId, name = name.trim(), colorHex = colorHex))
    }

    suspend fun deleteFolder(folderId: Long) {
        folderDao.deleteFolder(folderId)
    }

    // ---- Notes ----
    fun getNotes(): Flow<List<StudyNoteEntity>> = noteDao.getNotesByUser(currentUserId)

    fun getNotesInFolder(folderId: Long): Flow<List<StudyNoteEntity>> = noteDao.getNotesByFolder(currentUserId, folderId)

    fun searchNotes(query: String): Flow<List<StudyNoteEntity>> = noteDao.searchNotes(currentUserId, query)

    suspend fun getNote(noteId: Long): StudyNoteEntity? = noteDao.getNoteById(noteId)

    suspend fun saveNote(
        title: String,
        content: String,
        folderId: Long?,
        tags: String,
        summary: String = "",
        existingNoteId: Long? = null
    ): Long {
        return if (existingNoteId != null && existingNoteId > 0) {
            val note = StudyNoteEntity(
                id = existingNoteId,
                userId = currentUserId,
                folderId = folderId,
                title = title.trim(),
                content = content.trim(),
                tags = tags.trim(),
                summary = summary.trim(),
                updatedAt = System.currentTimeMillis()
            )
            noteDao.updateNote(note)
            existingNoteId
        } else {
            val note = StudyNoteEntity(
                userId = currentUserId,
                folderId = folderId,
                title = title.trim(),
                content = content.trim(),
                tags = tags.trim(),
                summary = summary.trim()
            )
            noteDao.insertNote(note)
        }
    }

    suspend fun deleteNote(noteId: Long) {
        noteDao.deleteNote(noteId)
    }

    // ---- Flashcards ----
    fun getFlashcardSets(): Flow<List<FlashcardSetEntity>> = flashcardDao.getFlashcardSetsByUser(currentUserId)

    fun getFlashcardItems(setId: Long): Flow<List<FlashcardItemEntity>> = flashcardDao.getItemsForSet(setId)

    suspend fun saveFlashcards(result: FlashcardsResult, noteId: Long? = null): Long {
        val set = FlashcardSetEntity(
            userId = currentUserId,
            noteId = noteId,
            title = result.title
        )
        val setId = flashcardDao.insertSet(set)
        val items = result.cards.map {
            FlashcardItemEntity(
                setId = setId,
                term = it.term,
                definition = it.definition
            )
        }
        flashcardDao.insertItems(items)
        return setId
    }

    suspend fun updateCardMastery(cardId: Long, isMastered: Boolean) {
        flashcardDao.updateItemMastery(cardId, isMastered)
    }

    suspend fun deleteFlashcardSet(setId: Long) {
        flashcardDao.deleteItemsForSet(setId)
        flashcardDao.deleteSet(setId)
    }

    // ---- Quizzes ----
    fun getQuizHistories(): Flow<List<QuizHistoryEntity>> = quizDao.getQuizHistoryByUser(currentUserId)

    suspend fun saveQuizAttempt(
        noteId: Long?,
        noteTitle: String,
        score: Int,
        total: Int,
        quizResult: QuizResult
    ): Long {
        val details = JSONObject().apply {
            put("title", quizResult.title)
            val qArr = JSONArray()
            for (q in quizResult.questions) {
                val qObj = JSONObject().apply {
                    put("number", q.questionNumber)
                    put("question", q.question)
                    put("options", JSONArray(q.options))
                    put("correctIndex", q.correctIndex)
                    put("explanation", q.explanation)
                }
                qArr.put(qObj)
            }
            put("questions", qArr)
        }.toString()

        return quizDao.insertQuizHistory(
            QuizHistoryEntity(
                userId = currentUserId,
                noteId = noteId,
                noteTitle = noteTitle,
                score = score,
                totalQuestions = total,
                detailsJson = details
            )
        )
    }

    suspend fun deleteQuizHistory(id: Long) {
        quizDao.deleteQuizHistory(id)
    }

    // ---- AI Operations ----
    suspend fun summarize(notes: String): SummaryResult {
        return geminiService.summarizeNotes(notes, getCustomApiKey())
    }

    suspend fun askAi(notes: String, question: String): AskAiResult {
        return geminiService.askAiQuestion(notes, question, getCustomApiKey())
    }

    suspend fun generateQuiz(notes: String): QuizResult {
        return geminiService.generateQuiz(notes, getCustomApiKey())
    }

    suspend fun generateFlashcards(notes: String): FlashcardsResult {
        return geminiService.generateFlashcards(notes, getCustomApiKey())
    }
}
