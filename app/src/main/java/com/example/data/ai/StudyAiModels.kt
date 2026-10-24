package com.example.data.ai

data class SummaryResult(
    val title: String,
    val summary: String,
    val keyTakeaways: List<String>,
    val bulletPoints: List<String>
)

data class QuizQuestion(
    val questionNumber: Int,
    val question: String,
    val options: List<String>, // Exactly 4 options: [A, B, C, D]
    val correctIndex: Int, // 0 to 3
    val explanation: String
)

data class QuizResult(
    val title: String,
    val questions: List<QuizQuestion>
)

data class FlashcardItem(
    val term: String,
    val definition: String
)

data class FlashcardsResult(
    val title: String,
    val cards: List<FlashcardItem>
)

data class AskAiResult(
    val question: String,
    val answer: String,
    val isMissingInformation: Boolean
)
