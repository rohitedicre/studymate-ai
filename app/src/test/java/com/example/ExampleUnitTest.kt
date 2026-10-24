package com.example

import com.example.data.ai.GeminiService
import com.example.data.local.SessionManager
import com.example.ui.SampleNotesData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testPasswordHashingAndSaltValidation() {
        val password = "CollegeStudent2026!"
        val salt1 = SessionManager.generateSalt()
        val salt2 = SessionManager.generateSalt()

        assertNotEquals(salt1, salt2)

        val hash1 = SessionManager.hashPassword(password, salt1)
        val hash2 = SessionManager.hashPassword(password, salt1)
        val hashDifferentSalt = SessionManager.hashPassword(password, salt2)

        // Deterministic with same salt
        assertEquals(hash1, hash2)
        // Different hash with different salt
        assertNotEquals(hash1, hashDifferentSalt)
        // Verify sha-256 output length
        assertEquals(64, hash1.length)
    }

    @Test
    fun testGeminiServiceSummarization() = runBlocking {
        val service = GeminiService()
        val notes = SampleNotesData.BIOLOGY_CELLULAR_RESPIRATION.notes

        val summary = service.summarizeNotes(notes)
        assertNotNull(summary)
        assertTrue(summary.title.isNotBlank())
        assertTrue(summary.summary.isNotBlank())
        assertTrue(summary.keyTakeaways.isNotEmpty())
        assertTrue(summary.bulletPoints.isNotEmpty())
    }

    @Test
    fun testGeminiServiceAskAiAdmitsMissingInformation() = runBlocking {
        val service = GeminiService()
        val notes = SampleNotesData.BIOLOGY_CELLULAR_RESPIRATION.notes

        // Question about something NOT in biology cellular respiration notes
        val missingTopicQuestion = "What was the inflation rate in France during the Napoleonic wars?"
        val result = service.askAiQuestion(notes, missingTopicQuestion)

        // Must admit when information is missing from the supplied notes!
        assertTrue(result.isMissingInformation)
        assertTrue(
            result.answer.contains("not mentioned", ignoreCase = true) ||
            result.answer.contains("do not contain", ignoreCase = true)
        )
    }

    @Test
    fun testGeminiServiceAskAiAnswersFromNotes() = runBlocking {
        val service = GeminiService()
        val notes = SampleNotesData.BIOLOGY_CELLULAR_RESPIRATION.notes

        val relevantQuestion = "What is the final electron acceptor in the electron transport chain?"
        val result = service.askAiQuestion(notes, relevantQuestion)

        assertFalse(result.isMissingInformation)
        assertTrue(result.answer.contains("Oxygen", ignoreCase = true))
    }

    @Test
    fun testGeminiServiceQuizGenerationHasFiveQuestionsWithFourOptions() = runBlocking {
        val service = GeminiService()
        val notes = SampleNotesData.ECONOMICS_MACRO_PRINCIPLES.notes

        val quiz = service.generateQuiz(notes)
        assertNotNull(quiz)
        assertEquals(5, quiz.questions.size)

        for (q in quiz.questions) {
            assertEquals(4, q.options.size)
            assertTrue(q.correctIndex in 0..3)
            assertTrue(q.question.isNotBlank())
            assertTrue(q.explanation.isNotBlank())
        }
    }

    @Test
    fun testGeminiServiceFlashcardsExtraction() = runBlocking {
        val service = GeminiService()
        val notes = SampleNotesData.CS_OPERATING_SYSTEMS.notes

        val flashcards = service.generateFlashcards(notes)
        assertNotNull(flashcards)
        assertTrue(flashcards.cards.size >= 4)

        for (card in flashcards.cards) {
            assertTrue(card.term.isNotBlank())
            assertTrue(card.definition.isNotBlank())
        }
    }
}
