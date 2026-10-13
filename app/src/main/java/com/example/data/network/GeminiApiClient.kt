package com.example.data.network

import com.example.BuildConfig
import com.example.data.model.FlashcardItem
import com.example.data.model.JsonParserHelper
import com.example.data.model.QuizQuestion
import com.example.data.rag.RagEngine
import com.example.data.rag.TextChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiClient {

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateFlashcardsWithRag(
        materialContent: String,
        topicQuery: String = ""
    ): Pair<List<FlashcardItem>, List<TextChunk>> = withContext(Dispatchers.IO) {
        val retrievedChunks = RagEngine.retrieveRelevantChunks(
            documentContent = materialContent,
            query = topicQuery.ifEmpty { "definitions core concepts formulas rules" },
            topK = 4
        )

        val contextBlock = retrievedChunks.joinToString("\n\n---\n\n") {
            "[Section: ${it.sectionHeader}]\n${it.text}"
        }

        val prompt = """
            You are an expert curriculum designer and tutor. Based STRICTLY and EXCLUSIVELY on the provided classroom source text chunks below, generate 5 high-yield study flashcards.
            
            SOURCE TEXT:
            $contextBlock
            
            REQUIREMENTS:
            - Return ONLY a JSON array of objects with the schema:
            [
              {"front": "Clear question or term", "back": "Precise grounded answer or explanation"}
            ]
            - Do not include markdown code fence formatting like ```json.
            - Ensure all answers strictly reflect the facts in the provided source text chunks.
        """.trimIndent()

        val cards = callGeminiForFlashcards(prompt, retrievedChunks)
        cards to retrievedChunks
    }

    suspend fun generateQuizWithRag(
        materialContent: String,
        difficulty: String,
        topicQuery: String = ""
    ): Pair<List<QuizQuestion>, List<TextChunk>> = withContext(Dispatchers.IO) {
        val retrievedChunks = RagEngine.retrieveRelevantChunks(
            documentContent = materialContent,
            query = topicQuery.ifEmpty { "practice questions principles difficulty: $difficulty" },
            topK = 4
        )

        val contextBlock = retrievedChunks.joinToString("\n\n---\n\n") {
            "[Section: ${it.sectionHeader}]\n${it.text}"
        }

        val prompt = """
            You are an educational assessment specialist. Based STRICTLY and EXCLUSIVELY on the provided classroom source text chunks below, generate a 5-question multiple choice adaptive quiz at $difficulty difficulty.
            
            SOURCE TEXT:
            $contextBlock
            
            REQUIREMENTS:
            - Return ONLY a strict JSON array of objects matching this exact schema:
            [
              {
                "id": 1,
                "question": "Question text...",
                "options": ["A: ...", "B: ...", "C: ...", "D: ..."],
                "correct_answer": "Exact matching option string e.g. A: ...",
                "explanation": "Clear grounded explanation citing the source chunk facts",
                "topic_tag": "Specific topic or concept name from the text"
              }
            ]
            - Ensure exactly 4 options per question (A, B, C, D).
            - Do not wrap in markdown code blocks like ```json.
            - Ensure questions test deep comprehension matching the $difficulty difficulty level.
        """.trimIndent()

        val questions = callGeminiForQuiz(prompt, difficulty, retrievedChunks)
        questions to retrievedChunks
    }

    private fun callGeminiForFlashcards(prompt: String, fallbackChunks: List<TextChunk>): List<FlashcardItem> {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val jsonPayload = buildJsonRequestBody(prompt)
                val request = Request.Builder()
                    .url("$BASE_URL?key=$apiKey")
                    .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val parsedText = extractCandidateText(responseBody)
                    val cards = JsonParserHelper.parseFlashcards(parsedText)
                    if (cards.isNotEmpty()) return cards
                }
            } catch (_: Exception) {
                // Graceful fallback to localized generator
            }
        }
        return generateFallbackFlashcards(fallbackChunks)
    }

    private fun callGeminiForQuiz(prompt: String, difficulty: String, fallbackChunks: List<TextChunk>): List<QuizQuestion> {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val jsonPayload = buildJsonRequestBody(prompt)
                val request = Request.Builder()
                    .url("$BASE_URL?key=$apiKey")
                    .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val parsedText = extractCandidateText(responseBody)
                    val questions = JsonParserHelper.parseQuizQuestions(parsedText)
                    if (questions.isNotEmpty()) return questions
                }
            } catch (_: Exception) {
                // Graceful fallback to localized generator
            }
        }
        return generateFallbackQuiz(fallbackChunks, difficulty)
    }

    private fun buildJsonRequestBody(promptText: String): String {
        val root = JSONObject()
        val contents = JSONArray()
        val contentObj = JSONObject()
        val parts = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", promptText)
        parts.put(partObj)
        contentObj.put("parts", parts)
        contents.put(contentObj)
        root.put("contents", contents)

        val generationConfig = JSONObject()
        generationConfig.put("responseMimeType", "application/json")
        generationConfig.put("temperature", 0.3)
        root.put("generationConfig", generationConfig)

        return root.toString()
    }

    private fun extractCandidateText(responseJson: String): String {
        try {
            val root = JSONObject(responseJson)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content") ?: return ""
                val parts = content.optJSONArray("parts") ?: return ""
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
        } catch (_: Exception) {}
        return ""
    }

    private fun generateFallbackFlashcards(chunks: List<TextChunk>): List<FlashcardItem> {
        val cards = mutableListOf<FlashcardItem>()
        for ((idx, chunk) in chunks.withIndex()) {
            val sentences = chunk.text.split(Regex("(?<=[.!?])\\s+"))
                .filter { it.isNotBlank() && it.length > 20 }
            val front = "Key Concept (${chunk.sectionHeader}): What is the primary principle of this topic?"
            val back = sentences.firstOrNull() ?: chunk.text.take(150)
            cards.add(FlashcardItem(front = front, back = back))
            if (cards.size >= 5) break
        }

        if (cards.isEmpty()) {
            cards.add(FlashcardItem("Core Definition", "Fundamental principle derived from classroom study material."))
            cards.add(FlashcardItem("Practical Application", "Application of core laws in real-world environments."))
        }
        return cards
    }

    private fun generateFallbackQuiz(chunks: List<TextChunk>, difficulty: String): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()
        for ((idx, chunk) in chunks.take(5).withIndex()) {
            val qNum = idx + 1
            val topic = chunk.sectionHeader
            val sentences = chunk.text.split(Regex("(?<=[.!?])\\s+")).filter { it.length > 25 }
            val keyFact = sentences.firstOrNull() ?: "Core theoretical principle applies here."

            questions.add(
                QuizQuestion(
                    id = qNum,
                    question = "[$difficulty] According to the lecture on '$topic', which statement is correct?",
                    options = listOf(
                        "A: $keyFact",
                        "B: The opposite effect is observed under standard conditions",
                        "C: This principle only applies in non-linear isolated states",
                        "D: None of the above interpretations are valid"
                    ),
                    correctAnswer = "A: $keyFact",
                    explanation = "Directly verified from source chunk '$topic': $keyFact",
                    topicTag = topic
                )
            )
        }
        return questions
    }
}
