package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AskAiResult
import com.example.data.ai.FlashcardItem
import com.example.data.ai.FlashcardsResult
import com.example.data.ai.GeminiService
import com.example.data.ai.QuizQuestion
import com.example.data.ai.QuizResult
import com.example.data.ai.SummaryResult
import com.example.data.local.AuthUserState
import com.example.data.local.FlashcardItemEntity
import com.example.data.local.FlashcardSetEntity
import com.example.data.local.FolderEntity
import com.example.data.local.QuizHistoryEntity
import com.example.data.local.SessionManager
import com.example.data.local.StudyMateDatabase
import com.example.data.local.StudyNoteEntity
import com.example.data.repository.StudyMateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkspaceUiState(
    val title: String = "",
    val notesText: String = "",
    val selectedFolderId: Long? = null,
    val tagsText: String = "",
    val activeNoteId: Long? = null,
    val summaryResult: SummaryResult? = null,
    val isSummarizing: Boolean = false,
    val summaryError: String? = null,
    val askQuestionText: String = "",
    val isAskingAi: Boolean = false,
    val askAiError: String? = null,
    val qaList: List<AskAiResult> = emptyList(),
    val saveNoteMessage: String? = null
)

data class QuizUiState(
    val currentQuiz: QuizResult? = null,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val selectedOptions: Map<Int, Int> = emptyMap(), // questionNumber -> selectedOptionIndex (0..3)
    val isSubmitted: Boolean = false,
    val score: Int = 0,
    val total: Int = 0,
    val activeQuestionIndex: Int = 0
)

data class FlashcardsUiState(
    val currentSet: FlashcardsResult? = null,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val currentCardIndex: Int = 0,
    val isFlipped: Boolean = false,
    val masteredCardIndices: Set<Int> = emptySet(),
    val savedSets: List<FlashcardSetEntity> = emptyList()
)

data class AuthUiState(
    val usernameInput: String = "",
    val emailInput: String = "",
    val passwordInput: String = "",
    val isSignUpMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val customApiKeyInput: String = ""
)

class StudyMateViewModel(application: Application) : AndroidViewModel(application) {
    private val database = StudyMateDatabase.getDatabase(application)
    private val sessionManager = SessionManager(application)
    private val geminiService = GeminiService()

    private val repository = StudyMateRepository(
        userDao = database.userDao(),
        folderDao = database.folderDao(),
        noteDao = database.studyNoteDao(),
        flashcardDao = database.flashcardDao(),
        quizDao = database.quizHistoryDao(),
        sessionManager = sessionManager,
        geminiService = geminiService
    )

    val authUserState: StateFlow<AuthUserState> = repository.authUserState

    private val _workspaceState = MutableStateFlow(WorkspaceUiState())
    val workspaceState: StateFlow<WorkspaceUiState> = _workspaceState.asStateFlow()

    private val _quizState = MutableStateFlow(QuizUiState())
    val quizState: StateFlow<QuizUiState> = _quizState.asStateFlow()

    private val _flashcardsState = MutableStateFlow(FlashcardsUiState())
    val flashcardsState: StateFlow<FlashcardsUiState> = _flashcardsState.asStateFlow()

    private val _authUiState = MutableStateFlow(
        AuthUiState(customApiKeyInput = repository.getCustomApiKey())
    )
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    // Notes and Folders flow
    val foldersFlow: StateFlow<List<FolderEntity>> = repository.getFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notesFlow: StateFlow<List<StudyNoteEntity>> = repository.getNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizHistoriesFlow: StateFlow<List<QuizHistoryEntity>> = repository.getQuizHistories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcardSetsFlow: StateFlow<List<FlashcardSetEntity>> = repository.getFlashcardSets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and filter state for notes library
    val notesSearchQuery = MutableStateFlow("")
    val selectedFilterFolderId = MutableStateFlow<Long?>(null)
    val selectedFilterTag = MutableStateFlow<String?>(null)

    val filteredNotesFlow: StateFlow<List<StudyNoteEntity>> = combine(
        notesFlow,
        notesSearchQuery,
        selectedFilterFolderId,
        selectedFilterTag
    ) { notes, query, folderId, tag ->
        notes.filter { note ->
            val matchesFolder = folderId == null || note.folderId == folderId
            val matchesTag = tag == null || note.tags.split(",").map { it.trim() }.contains(tag)
            val matchesQuery = query.isBlank() ||
                note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true) ||
                note.tags.contains(query, ignoreCase = true)
            matchesFolder && matchesTag && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // If guest or no user yet, seed default folders
        viewModelScope.launch {
            if (repository.getCustomApiKey().isNotEmpty()) {
                _authUiState.update { it.copy(customApiKeyInput = repository.getCustomApiKey()) }
            }
        }
    }

    // ================= WORKSPACE ACTIONS =================

    fun onNoteTitleChanged(newTitle: String) {
        _workspaceState.update { it.copy(title = newTitle, saveNoteMessage = null) }
    }

    fun onNotesTextChanged(newText: String) {
        _workspaceState.update { it.copy(notesText = newText, summaryError = null, askAiError = null, saveNoteMessage = null) }
    }

    fun onFolderSelected(folderId: Long?) {
        _workspaceState.update { it.copy(selectedFolderId = folderId) }
    }

    fun onTagsChanged(newTags: String) {
        _workspaceState.update { it.copy(tagsText = newTags) }
    }

    fun onAskQuestionChanged(newQuestion: String) {
        _workspaceState.update { it.copy(askQuestionText = newQuestion, askAiError = null) }
    }

    fun clearWorkspace() {
        _workspaceState.value = WorkspaceUiState()
    }

    fun loadSampleNotes(sampleType: Int = 0) {
        val sample = when (sampleType % 3) {
            0 -> SampleNotesData.BIOLOGY_CELLULAR_RESPIRATION
            1 -> SampleNotesData.ECONOMICS_MACRO_PRINCIPLES
            else -> SampleNotesData.CS_OPERATING_SYSTEMS
        }
        _workspaceState.update {
            it.copy(
                title = sample.title,
                notesText = sample.notes,
                tagsText = sample.tags,
                summaryResult = null,
                summaryError = null,
                askAiError = null,
                saveNoteMessage = null
            )
        }
    }

    fun loadNoteIntoWorkspace(note: StudyNoteEntity) {
        _workspaceState.update {
            it.copy(
                title = note.title,
                notesText = note.content,
                selectedFolderId = note.folderId,
                tagsText = note.tags,
                activeNoteId = note.id,
                summaryResult = if (note.summary.isNotBlank()) {
                    SummaryResult(note.title, note.summary, emptyList(), emptyList())
                } else null,
                saveNoteMessage = "Loaded '${note.title}' into editor"
            )
        }
    }

    fun summarizeNotes() {
        val notes = _workspaceState.value.notesText.trim()
        if (notes.isBlank()) {
            _workspaceState.update { it.copy(summaryError = "Please enter or paste study notes first.") }
            return
        }

        viewModelScope.launch {
            _workspaceState.update { it.copy(isSummarizing = true, summaryError = null) }
            try {
                val result = repository.summarize(notes)
                _workspaceState.update { it.copy(summaryResult = result, isSummarizing = false) }
            } catch (e: Exception) {
                _workspaceState.update {
                    it.copy(
                        isSummarizing = false,
                        summaryError = "Failed to summarize: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun askAiQuestion() {
        val notes = _workspaceState.value.notesText.trim()
        val question = _workspaceState.value.askQuestionText.trim()

        if (notes.isBlank()) {
            _workspaceState.update { it.copy(askAiError = "Notes are empty. Please provide notes to ask questions about.") }
            return
        }
        if (question.isBlank()) {
            _workspaceState.update { it.copy(askAiError = "Please type a question to ask AI.") }
            return
        }

        viewModelScope.launch {
            _workspaceState.update { it.copy(isAskingAi = true, askAiError = null) }
            try {
                val result = repository.askAi(notes, question)
                _workspaceState.update { state ->
                    state.copy(
                        isAskingAi = false,
                        askQuestionText = "",
                        qaList = listOf(result) + state.qaList
                    )
                }
            } catch (e: Exception) {
                _workspaceState.update {
                    it.copy(
                        isAskingAi = false,
                        askAiError = "Failed to answer question: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun saveCurrentNote() {
        val state = _workspaceState.value
        val title = if (state.title.isNotBlank()) state.title else "Untitled Study Note"
        val content = state.notesText.trim()
        if (content.isBlank()) {
            _workspaceState.update { it.copy(saveNoteMessage = "Cannot save empty notes.") }
            return
        }

        viewModelScope.launch {
            val summaryText = state.summaryResult?.summary ?: ""
            val noteId = repository.saveNote(
                title = title,
                content = content,
                folderId = state.selectedFolderId,
                tags = state.tagsText,
                summary = summaryText,
                existingNoteId = state.activeNoteId
            )
            _workspaceState.update {
                it.copy(
                    activeNoteId = noteId,
                    saveNoteMessage = "Note '$title' saved successfully!"
                )
            }
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            if (_workspaceState.value.activeNoteId == noteId) {
                _workspaceState.update { it.copy(activeNoteId = null) }
            }
        }
    }

    // ================= QUIZ ACTIONS =================

    fun generateQuizFromNotes(notesInput: String? = null) {
        val notes = (notesInput ?: _workspaceState.value.notesText).trim()
        if (notes.isBlank()) {
            _quizState.update { it.copy(errorMessage = "Notes are empty. Please paste notes before generating a quiz.") }
            return
        }

        viewModelScope.launch {
            _quizState.update { it.copy(isGenerating = true, errorMessage = null, isSubmitted = false, selectedOptions = emptyMap(), activeQuestionIndex = 0) }
            try {
                val quiz = repository.generateQuiz(notes)
                _quizState.update {
                    it.copy(
                        currentQuiz = quiz,
                        isGenerating = false,
                        total = quiz.questions.size,
                        score = 0
                    )
                }
            } catch (e: Exception) {
                _quizState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = "Quiz generation error: ${e.localizedMessage ?: "Please try again."}"
                    )
                }
            }
        }
    }

    fun selectQuizOption(questionNumber: Int, optionIndex: Int) {
        if (_quizState.value.isSubmitted) return
        _quizState.update {
            val updated = it.selectedOptions.toMutableMap()
            updated[questionNumber] = optionIndex
            it.copy(selectedOptions = updated)
        }
    }

    fun submitQuiz() {
        val state = _quizState.value
        val quiz = state.currentQuiz ?: return
        var correctCount = 0

        for (q in quiz.questions) {
            val userPick = state.selectedOptions[q.questionNumber]
            if (userPick != null && userPick == q.correctIndex) {
                correctCount++
            }
        }

        _quizState.update {
            it.copy(
                isSubmitted = true,
                score = correctCount,
                total = quiz.questions.size
            )
        }

        // Save attempt to Room history
        viewModelScope.launch {
            val noteTitle = _workspaceState.value.title.ifBlank { quiz.title }
            repository.saveQuizAttempt(
                noteId = _workspaceState.value.activeNoteId,
                noteTitle = noteTitle,
                score = correctCount,
                total = quiz.questions.size,
                quizResult = quiz
            )
        }
    }

    fun setQuizQuestionIndex(index: Int) {
        _quizState.update { it.copy(activeQuestionIndex = index.coerceIn(0, (it.currentQuiz?.questions?.size ?: 1) - 1)) }
    }

    fun deleteQuizHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteQuizHistory(id)
        }
    }

    // ================= FLASHCARDS ACTIONS =================

    fun generateFlashcardsFromNotes(notesInput: String? = null) {
        val notes = (notesInput ?: _workspaceState.value.notesText).trim()
        if (notes.isBlank()) {
            _flashcardsState.update { it.copy(errorMessage = "Notes are empty. Please provide notes to generate flashcards.") }
            return
        }

        viewModelScope.launch {
            _flashcardsState.update {
                it.copy(isGenerating = true, errorMessage = null, currentCardIndex = 0, isFlipped = false, masteredCardIndices = emptySet())
            }
            try {
                val result = repository.generateFlashcards(notes)
                val setId = repository.saveFlashcards(result, _workspaceState.value.activeNoteId)
                _flashcardsState.update {
                    it.copy(
                        currentSet = result,
                        isGenerating = false
                    )
                }
            } catch (e: Exception) {
                _flashcardsState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = "Flashcards error: ${e.localizedMessage ?: "Failed to generate."}"
                    )
                }
            }
        }
    }

    fun flipCard() {
        _flashcardsState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun nextCard() {
        val count = _flashcardsState.value.currentSet?.cards?.size ?: 0
        if (count == 0) return
        _flashcardsState.update {
            it.copy(
                currentCardIndex = (it.currentCardIndex + 1) % count,
                isFlipped = false
            )
        }
    }

    fun previousCard() {
        val count = _flashcardsState.value.currentSet?.cards?.size ?: 0
        if (count == 0) return
        _flashcardsState.update {
            val newIdx = if (it.currentCardIndex - 1 < 0) count - 1 else it.currentCardIndex - 1
            it.copy(
                currentCardIndex = newIdx,
                isFlipped = false
            )
        }
    }

    fun toggleCardMastered(index: Int) {
        _flashcardsState.update {
            val mastered = it.masteredCardIndices.toMutableSet()
            if (mastered.contains(index)) {
                mastered.remove(index)
            } else {
                mastered.add(index)
            }
            it.copy(masteredCardIndices = mastered)
        }
    }

    fun deleteFlashcardSet(setId: Long) {
        viewModelScope.launch {
            repository.deleteFlashcardSet(setId)
            if (_flashcardsState.value.currentSet != null) {
                _flashcardsState.update { it.copy(currentSet = null) }
            }
        }
    }

    // ================= FOLDER MANAGEMENT =================

    fun createFolder(name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createFolder(name, colorHex)
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            if (selectedFilterFolderId.value == folderId) {
                selectedFilterFolderId.value = null
            }
            if (_workspaceState.value.selectedFolderId == folderId) {
                _workspaceState.update { it.copy(selectedFolderId = null) }
            }
        }
    }

    // ================= AUTH ACTIONS =================

    fun onUsernameInputChanged(name: String) {
        _authUiState.update { it.copy(usernameInput = name, error = null) }
    }

    fun onEmailInputChanged(email: String) {
        _authUiState.update { it.copy(emailInput = email, error = null) }
    }

    fun onPasswordInputChanged(password: String) {
        _authUiState.update { it.copy(passwordInput = password, error = null) }
    }

    fun toggleAuthMode() {
        _authUiState.update {
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                error = null,
                successMessage = null
            )
        }
    }

    fun performAuthAction() {
        val state = _authUiState.value
        val email = state.emailInput.trim()
        val password = state.passwordInput

        if (email.isBlank() || !email.contains("@")) {
            _authUiState.update { it.copy(error = "Please enter a valid email address.") }
            return
        }
        if (password.length < 4) {
            _authUiState.update { it.copy(error = "Password must be at least 4 characters.") }
            return
        }

        viewModelScope.launch {
            _authUiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            if (state.isSignUpMode) {
                val username = state.usernameInput.trim().ifBlank { email.substringBefore("@") }
                val result = repository.signUp(username, email, password)
                result.fold(
                    onSuccess = { user ->
                        _authUiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Welcome, ${user.username}! Account created.",
                                passwordInput = ""
                            )
                        }
                    },
                    onFailure = { err ->
                        _authUiState.update {
                            it.copy(isLoading = false, error = err.message ?: "Sign up failed")
                        }
                    }
                )
            } else {
                val result = repository.logIn(email, password)
                result.fold(
                    onSuccess = { user ->
                        _authUiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Welcome back, ${user.username}!",
                                passwordInput = ""
                            )
                        }
                    },
                    onFailure = { err ->
                        _authUiState.update {
                            it.copy(isLoading = false, error = err.message ?: "Login failed")
                        }
                    }
                )
            }
        }
    }

    fun logOut() {
        repository.logOut()
        _authUiState.update {
            it.copy(
                passwordInput = "",
                successMessage = "Logged out successfully"
            )
        }
    }

    fun onApiKeyInputChanged(newKey: String) {
        _authUiState.update { it.copy(customApiKeyInput = newKey) }
    }

    fun saveApiKey() {
        val key = _authUiState.value.customApiKeyInput.trim()
        repository.saveCustomApiKey(key)
        _authUiState.update { it.copy(successMessage = "API Key saved successfully!") }
    }
}

// Sample Study Notes for College Students
object SampleNotesData {
    data class SampleNote(
        val title: String,
        val tags: String,
        val notes: String
    )

    val BIOLOGY_CELLULAR_RESPIRATION = SampleNote(
        title = "Cellular Respiration & ATP Synthesis",
        tags = "Biology, ExamPrep, Metabolism",
        notes = """
# Cellular Respiration: Catabolic Metabolic Pathways
Cellular respiration is the biochemical process by which eukaryotic organisms break down organic nutrients (primarily glucose) to synthesize Adenosine Triphosphate (ATP), releasing carbon dioxide and water as metabolic byproducts.

## Key Stages of Respiration:
1. Glycolysis:
- Location: Takes place in the cytoplasm of the cell.
- Reactants: 1 molecule of Glucose (6 carbons).
- Products: 2 molecules of Pyruvate (3 carbons each), a net gain of 2 ATP (via substrate-level phosphorylation), and 2 NADH.
- Oxygen Requirement: Glycolysis is anaerobic and does not require oxygen.

2. Pyruvate Oxidation:
- Location: Mitochondrial matrix.
- Pyruvate is converted into Acetyl-CoA, releasing 1 CO2 and producing 1 NADH per pyruvate (total 2 CO2 and 2 NADH per glucose).

3. Krebs Cycle (Citric Acid Cycle):
- Location: Mitochondrial matrix.
- Acetyl-CoA joins with oxaloacetate to form citrate.
- Yields per glucose: 2 ATP, 6 NADH, 2 FADH2, and 4 CO2 molecules.

4. Oxidative Phosphorylation & Electron Transport Chain (ETC):
- Location: Inner mitochondrial membrane (cristae).
- High-energy electrons from NADH and FADH2 are transferred along protein complexes I through IV.
- Protons (H+) are pumped across the inner membrane into the intermembrane space, generating an electrochemical proton gradient (chemiosmosis).
- ATP Synthase utilizes this proton-motive force to phosphorylate ADP into ATP.
- Final Electron Acceptor: Molecular Oxygen (O2), which combines with electrons and protons to form water (H2O).
- Total theoretical yield: 30 to 32 ATP molecules per oxidized glucose molecule.
        """.trimIndent()
    )

    val ECONOMICS_MACRO_PRINCIPLES = SampleNote(
        title = "Macroeconomics: Inflation & Monetary Policy",
        tags = "Economics, Finance, Midterm",
        notes = """
# Principles of Macroeconomics: Inflation, Output & Central Banking
Macroeconomics studies economy-wide phenomena such as total production, aggregate employment, price levels, and business cycles.

## Foundational Concepts:
- Gross Domestic Product (GDP): The total market value of all final goods and services produced within a country's borders in a given time period. Formula: GDP = C + I + G + (X - M).
- Inflation: The sustained general increase in price levels over time, reducing consumer purchasing power. Measured via the Consumer Price Index (CPI) and PCE deflator.
- Demand-Pull Inflation: Occurs when aggregate demand for goods and services outpaces aggregate supply ("too many dollars chasing too few goods").
- Cost-Push Inflation: Occurs when production costs rise (e.g. oil shocks, supply disruptions), forcing firms to raise selling prices despite stagnant output.

## Monetary Policy Tools (Central Bank / Federal Reserve):
1. Federal Funds Rate: The interest rate commercial banks charge one another for overnight reserve loans.
2. Open Market Operations (OMO): The buying and selling of government securities to adjust reserve liquidity in the banking system.
3. Reserve Requirements: The minimum fraction of customer deposits banks must hold in reserve.
4. Quantitative Easing (QE): Large-scale asset purchases to lower long-term interest rates and stimulate economic activity during severe recessions.
- Contractionary Policy: Raising interest rates to cool economic overheated demand and curb inflation.
- Expansionary Policy: Lowering interest rates to stimulate investment and reduce unemployment.
        """.trimIndent()
    )

    val CS_OPERATING_SYSTEMS = SampleNote(
        title = "Operating Systems: Concurrency & Synchronization",
        tags = "CS, OperatingSystems, Finals",
        notes = """
# Operating Systems: Concurrency, Synchronization & Deadlocks
Modern multi-core operating systems use processes and threads to achieve parallel and concurrent execution.

## Core Definitions:
- Process: An execution instance of a program with its own dedicated memory address space (heap, stack, data, code).
- Thread: A lightweight unit of CPU utilization within a process, sharing the parent process's memory space, file descriptors, and global variables, but possessing its own registers and stack.

## Race Conditions & Critical Sections:
- Race Condition: A concurrency flaw where multiple threads access and mutate shared data concurrently, and the final outcome depends on the non-deterministic thread scheduling order.
- Critical Section: The block of code that accesses shared resources. Proper synchronization requires three criteria: Mutual Exclusion, Progress, and Bounded Waiting.
- Synchronization Primitives: Mutex locks, Semaphores (Counting and Binary), and Monitors.

## Deadlocks:
A deadlock is a permanent state where a set of processes are blocked because each process holds a resource and waits for another resource held by another process.
- The Coffman Four Conditions (all 4 must hold simultaneously for deadlock):
  1. Mutual Exclusion: At least one resource is held in a non-shareable mode.
  2. Hold and Wait: A process holds at least one resource and is waiting to acquire additional resources held by other processes.
  3. No Preemption: Resources cannot be forcibly seized from a process until released voluntarily.
  4. Circular Wait: A closed chain of processes exists such that each process holds a resource that is requested by the next process in the chain.
- Handling Strategies: Deadlock Prevention, Banker's Algorithm (Deadlock Avoidance), Detection & Recovery, and the Ostrich Algorithm.
        """.trimIndent()
    )
}
