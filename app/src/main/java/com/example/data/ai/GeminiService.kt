package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "GeminiService"

class GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun summarizeNotes(notes: String, apiKeyOverride: String = ""): SummaryResult = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(apiKeyOverride)
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    You are an academic study assistant for college students.
                    Please summarize the following study notes thoroughly.
                    Respond in valid JSON format only, matching this structure:
                    {
                      "title": "A short, descriptive title for these notes",
                      "summary": "A cohesive executive summary paragraph (3-5 sentences)",
                      "keyTakeaways": ["Core concept 1", "Core concept 2", "Core concept 3", "Core concept 4"],
                      "bulletPoints": ["Detailed point 1", "Detailed point 2", "Detailed point 3", "Detailed point 4", "Detailed point 5"]
                    }
                    Do not include markdown code block tags. Output pure JSON.
                    
                    Notes:
                    $notes
                """.trimIndent()

                val rawJson = callGeminiApi(prompt, apiKey)
                val cleanJson = cleanJsonString(rawJson)
                val obj = JSONObject(cleanJson)
                val title = obj.optString("title", "Notes Summary")
                val summary = obj.optString("summary", "Summary of provided study materials.")
                val takeaways = jsonArrayToList(obj.optJSONArray("keyTakeaways"))
                val bullets = jsonArrayToList(obj.optJSONArray("bulletPoints"))
                return@withContext SummaryResult(
                    title = title,
                    summary = summary,
                    keyTakeaways = if (takeaways.isNotEmpty()) takeaways else listOf("Key concept extracted from notes"),
                    bulletPoints = if (bullets.isNotEmpty()) bullets else listOf("Detailed breakdown from notes")
                )
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API summarization failed, utilizing fallback engine", e)
            }
        }
        // Fallback offline extraction
        fallbackSummarize(notes)
    }

    suspend fun askAiQuestion(notes: String, question: String, apiKeyOverride: String = ""): AskAiResult = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(apiKeyOverride)
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    You are an honest and rigorous academic study tutor for a college student.
                    Your task is to answer the student's question BASED STRICTLY ON THE PROVIDED NOTES.
                    
                    CRITICAL INSTRUCTION:
                    If the provided notes do NOT contain sufficient information to answer the question, or if the question asks about something entirely unmentioned in the notes, you MUST explicitly admit that the information is missing from the supplied notes. Do not hallucinate external facts.
                    
                    Format your response in pure JSON only:
                    {
                      "isMissingInformation": true/false,
                      "answer": "Your factual response or clear statement admitting the info is missing from the notes"
                    }
                    
                    Notes:
                    $notes
                    
                    Student Question:
                    $question
                """.trimIndent()

                val rawJson = callGeminiApi(prompt, apiKey)
                val cleanJson = cleanJsonString(rawJson)
                val obj = JSONObject(cleanJson)
                val isMissing = obj.optBoolean("isMissingInformation", false)
                val answer = obj.optString("answer", "No answer could be determined from notes.")
                return@withContext AskAiResult(
                    question = question,
                    answer = answer,
                    isMissingInformation = isMissing
                )
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API Q&A failed, utilizing fallback engine", e)
            }
        }
        fallbackAskAi(notes, question)
    }

    suspend fun generateQuiz(notes: String, apiKeyOverride: String = ""): QuizResult = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(apiKeyOverride)
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    You are a university professor creating an exam prep quiz for college students.
                    Based strictly on the provided study notes, generate exactly 5 multiple choice questions.
                    Each question must have exactly 4 choices (index 0 for A, 1 for B, 2 for C, 3 for D).
                    Provide the correct 0-based index and a detailed answer key explanation.
                    
                    Respond ONLY with valid JSON with this exact schema:
                    {
                      "title": "Topic Quiz Title",
                      "questions": [
                        {
                          "questionNumber": 1,
                          "question": "Question text...",
                          "options": ["Option A", "Option B", "Option C", "Option D"],
                          "correctIndex": 0,
                          "explanation": "Explanation why this is correct based on notes..."
                        }
                      ]
                    }
                    Ensure exactly 5 questions are provided.
                    
                    Notes:
                    $notes
                """.trimIndent()

                val rawJson = callGeminiApi(prompt, apiKey)
                val cleanJson = cleanJsonString(rawJson)
                val obj = JSONObject(cleanJson)
                val title = obj.optString("title", "Practice Quiz")
                val qArray = obj.getJSONArray("questions")
                val questionsList = mutableListOf<QuizQuestion>()
                for (i in 0 until qArray.length()) {
                    val qObj = qArray.getJSONObject(i)
                    val optsArray = qObj.getJSONArray("options")
                    val options = mutableListOf<String>()
                    for (j in 0 until optsArray.length()) {
                        options.add(optsArray.getString(j))
                    }
                    while (options.size < 4) {
                        options.add("Option ${options.size + 1}")
                    }
                    questionsList.add(
                        QuizQuestion(
                            questionNumber = i + 1,
                            question = qObj.optString("question", "Question ${i + 1}"),
                            options = options.take(4),
                            correctIndex = qObj.optInt("correctIndex", 0).coerceIn(0, 3),
                            explanation = qObj.optString("explanation", "Refer to provided study notes.")
                        )
                    )
                }
                if (questionsList.size >= 5) {
                    return@withContext QuizResult(title, questionsList.take(5))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API quiz generation failed, utilizing fallback engine", e)
            }
        }
        fallbackQuiz(notes)
    }

    suspend fun generateFlashcards(notes: String, apiKeyOverride: String = ""): FlashcardsResult = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(apiKeyOverride)
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    You are an academic learning coach. Extract the key terms, definitions, formulas, or concepts from the provided study notes into flashcards.
                    Generate between 6 and 10 flashcards.
                    Respond ONLY with valid JSON in this schema:
                    {
                      "title": "Flashcards Set Title",
                      "cards": [
                        {
                          "term": "Term or Concept",
                          "definition": "Clear concise explanation or definition from the notes"
                        }
                      ]
                    }
                    
                    Notes:
                    $notes
                """.trimIndent()

                val rawJson = callGeminiApi(prompt, apiKey)
                val cleanJson = cleanJsonString(rawJson)
                val obj = JSONObject(cleanJson)
                val title = obj.optString("title", "Study Flashcards")
                val cArray = obj.getJSONArray("cards")
                val cardsList = mutableListOf<FlashcardItem>()
                for (i in 0 until cArray.length()) {
                    val cObj = cArray.getJSONObject(i)
                    val term = cObj.optString("term", "").trim()
                    val definition = cObj.optString("definition", "").trim()
                    if (term.isNotBlank() && definition.isNotBlank()) {
                        cardsList.add(FlashcardItem(term, definition))
                    }
                }
                if (cardsList.isNotEmpty()) {
                    return@withContext FlashcardsResult(title, cardsList)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API flashcards failed, utilizing fallback engine", e)
            }
        }
        fallbackFlashcards(notes)
    }

    private fun resolveApiKey(overrideKey: String): String {
        if (overrideKey.isNotBlank()) return overrideKey.trim()
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        return buildKey.trim()
    }

    private fun callGeminiApi(promptText: String, apiKey: String): String {
        val models = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")
        var lastException: Exception? = null

        for (modelName in models) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val jsonPayload = JSONObject().apply {
                    val contents = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val parts = JSONArray().apply {
                                val partObj = JSONObject().apply {
                                    put("text", promptText)
                                }
                                put(partObj)
                            }
                            put("parts", parts)
                        }
                        put(contentObj)
                    }
                    put("contents", contents)
                    val genConfig = JSONObject().apply {
                        put("temperature", 0.2)
                    }
                    put("generationConfig", genConfig)
                }

                val request = Request.Builder()
                    .url(url)
                    .post(jsonPayload.toString().toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw IllegalStateException("API error HTTP ${response.code}: $bodyString")
                    }
                    val respObj = JSONObject(bodyString)
                    val candidates = respObj.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            return parts.getJSONObject(0).getString("text")
                        }
                    }
                    throw IllegalStateException("No text candidate returned from $modelName")
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Attempt with $modelName failed: ${e.message}")
            }
        }
        throw lastException ?: IllegalStateException("All Gemini models failed")
    }

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        clean = clean.trim()
        val firstBrace = clean.indexOf('{')
        val lastBrace = clean.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            clean = clean.substring(firstBrace, lastBrace + 1)
        }
        return clean
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        return list
    }

    // ================= FALLBACK ENGINE =================

    private fun fallbackSummarize(notes: String): SummaryResult {
        val lines = notes.lines().map { it.trim() }.filter { it.isNotBlank() }
        val titleCandidate = lines.firstOrNull { it.length in 5..60 && !it.endsWith(".") } ?: "Study Notes Summary"
        val sentences = notes.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.length > 20 }
        
        val summaryText = if (sentences.isNotEmpty()) {
            sentences.take(3).joinToString(" ")
        } else {
            notes.take(200)
        }

        val takeaways = sentences.drop(3).take(4).map { "• " + it.trim().removePrefix("-").removePrefix("•").trim() }
            .ifEmpty { listOf("• Core review of foundational subject matter", "• Key mechanisms and definitions highlighted") }

        val bulletPoints = lines.filter { it.startsWith("-") || it.startsWith("•") || it.contains(":") }
            .take(5)
            .map { it.removePrefix("-").removePrefix("•").trim() }
            .ifEmpty { sentences.takeLast(4) }

        return SummaryResult(
            title = titleCandidate,
            summary = summaryText,
            keyTakeaways = takeaways,
            bulletPoints = bulletPoints
        )
    }

    private fun fallbackAskAi(notes: String, question: String): AskAiResult {
        val qTokens = question.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(" ")
            .filter { it.length > 3 && it !in setOf("what", "when", "where", "which", "does", "have", "from", "with", "about", "explain") }

        val notesLower = notes.lowercase()
        val noteWords = notesLower.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()
        val matchCount = qTokens.count { noteWords.contains(it) }

        // Must admit when information is missing from supplied notes!
        if (qTokens.isNotEmpty() && matchCount == 0) {
            val previewTopic = notes.lines().firstOrNull { it.isNotBlank() }?.take(40) ?: "the provided content"
            return AskAiResult(
                question = question,
                answer = "I searched your supplied notes carefully, but this information is not mentioned. Your notes focus primarily on '$previewTopic'. Please update or expand your notes to ask about this topic.",
                isMissingInformation = true
            )
        }

        // Find best matching sentences
        val sentences = notes.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }
        val matchingSentences = sentences.filter { s ->
            val sLower = s.lowercase()
            qTokens.any { sLower.contains(it) }
        }

        return if (matchingSentences.isNotEmpty()) {
            val combined = matchingSentences.take(3).joinToString(" ")
            AskAiResult(
                question = question,
                answer = "According to your notes:\n\n$combined",
                isMissingInformation = false
            )
        } else {
            AskAiResult(
                question = question,
                answer = "The provided notes do not contain clear information regarding '${question.take(30)}...'. Please verify your notes contain this subject.",
                isMissingInformation = true
            )
        }
    }

    private fun fallbackQuiz(notes: String): QuizResult {
        val sentences = notes.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length in 35..180 }

        val questions = mutableListOf<QuizQuestion>()
        var qNum = 1

        val definitions = mutableListOf<Pair<String, String>>()
        for (s in sentences) {
            if (s.contains(" is ") || s.contains(" are ") || s.contains(" refers to ")) {
                val parts = s.split(Regex(" is | are | refers to "), 2)
                if (parts.size == 2 && parts[0].length in 4..30) {
                    definitions.add(Pair(parts[0].trim(), parts[1].trim()))
                }
            }
        }

        for (def in definitions.take(3)) {
            val correct = def.second.take(60)
            val q = QuizQuestion(
                questionNumber = qNum++,
                question = "According to the notes, what best defines '${def.first}'?",
                options = listOf(
                    correct,
                    "An unrelated structural inhibitor in secondary stages",
                    "A variable metric that decreases during standard reaction",
                    "The inverse thermodynamic constant of the mechanism"
                ).shuffled(),
                correctIndex = 0, // We'll compute after shuffle
                explanation = "'${def.first}' is described as: ${def.second}"
            )
            // Re-identify correct index
            val actualIndex = q.options.indexOf(correct).coerceAtLeast(0)
            questions.add(q.copy(correctIndex = actualIndex))
        }

        // Add standard comprehensive questions from sentences
        while (questions.size < 5) {
            val index = questions.size
            val source = sentences.getOrNull(index) ?: "The primary thesis established in the provided study material."
            val correctOption = "It directly states: " + source.take(60)
            val opts = listOf(
                correctOption,
                "It contradicts the foundational findings of the chapter",
                "It occurs only under external non-standard laboratory conditions",
                "It remains completely undetermined by contemporary research"
            ).shuffled()
            val actualIndex = opts.indexOf(correctOption).coerceAtLeast(0)
            questions.add(
                QuizQuestion(
                    questionNumber = qNum++,
                    question = "Which statement accurately reflects the principles outlined in section ${index + 1}?",
                    options = opts,
                    correctIndex = actualIndex,
                    explanation = "Direct reference from notes: \"$source\""
                )
            )
        }

        return QuizResult("Study Notes Assessment Quiz", questions.take(5))
    }

    private fun fallbackFlashcards(notes: String): FlashcardsResult {
        val cards = mutableListOf<FlashcardItem>()
        val lines = notes.lines().map { it.trim() }.filter { it.isNotBlank() }

        for (line in lines) {
            if (line.contains(":") && !line.startsWith("http")) {
                val parts = line.split(":", limit = 2)
                val term = parts[0].removePrefix("-").removePrefix("•").trim()
                val def = parts[1].trim()
                if (term.length in 3..40 && def.length > 10) {
                    cards.add(FlashcardItem(term, def))
                }
            } else if (line.contains(" - ") || line.contains(" – ")) {
                val parts = line.split(Regex(" - | – "), limit = 2)
                val term = parts[0].removePrefix("-").removePrefix("•").trim()
                val def = parts[1].trim()
                if (term.length in 3..40 && def.length > 10) {
                    cards.add(FlashcardItem(term, def))
                }
            }
        }

        if (cards.size < 4) {
            val sentences = notes.split(Regex("(?<=[.!?])\\s+")).filter { it.length > 30 }
            for ((idx, s) in sentences.take(6).withIndex()) {
                val words = s.split(" ").filter { it.isNotBlank() }
                val term = words.take(3).joinToString(" ").replace(Regex("[^a-zA-Z0-9 ]"), "")
                cards.add(FlashcardItem(term.ifBlank { "Concept ${idx + 1}" }, s))
            }
        }

        return FlashcardsResult("Key Concept Cards", cards.take(10))
    }
}
