package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class FlashcardItem(
    val front: String,
    val back: String
)

data class QuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String,
    val topicTag: String
)

data class StudentAnswerRecord(
    val questionId: Int,
    val selectedOption: String,
    val isCorrect: Boolean,
    val topicTag: String
)

data class TopicWeaknessStat(
    val topicTag: String,
    val totalAttempts: Int,
    val incorrectAttempts: Int,
    val failureRatePercent: Int
)

data class StudentBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val earned: Boolean
)

data class ClassReportBundle(
    val enrollments: List<EnrollmentEntity>,
    val materials: List<MaterialEntity>,
    val quizzes: List<QuizEntity>,
    val attempts: List<QuizAttemptEntity>,
    val metrics: List<QuestionMetricEntity>
)

object JsonParserHelper {
    fun parseFlashcards(jsonString: String): List<FlashcardItem> {
        val list = mutableListOf<FlashcardItem>()
        try {
            val trimmed = extractJsonArrayString(jsonString)
            val jsonArray = JSONArray(trimmed)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    FlashcardItem(
                        front = obj.optString("front", "Question"),
                        back = obj.optString("back", "Answer")
                    )
                )
            }
        } catch (_: Exception) {
            // Fallback parsing if malformed
        }
        return list
    }

    fun parseQuizQuestions(jsonString: String): List<QuizQuestion> {
        val list = mutableListOf<QuizQuestion>()
        try {
            val trimmed = extractJsonArrayString(jsonString)
            val jsonArray = JSONArray(trimmed)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val optionsArray = obj.optJSONArray("options")
                val optionsList = mutableListOf<String>()
                if (optionsArray != null) {
                    for (j in 0 until optionsArray.length()) {
                        optionsList.add(optionsArray.getString(j))
                    }
                }
                list.add(
                    QuizQuestion(
                        id = obj.optInt("id", i + 1),
                        question = obj.optString("question", "Question $i"),
                        options = if (optionsList.isNotEmpty()) optionsList else listOf("Option A", "Option B", "Option C", "Option D"),
                        correctAnswer = obj.optString("correct_answer", optionsList.firstOrNull() ?: "Option A"),
                        explanation = obj.optString("explanation", "Grounded context explanation"),
                        topicTag = obj.optString("topic_tag", "General Concept")
                    )
                )
            }
        } catch (_: Exception) {
            // Fallback
        }
        return list
    }

    fun flashcardsToJson(cards: List<FlashcardItem>): String {
        val array = JSONArray()
        for (c in cards) {
            val obj = JSONObject()
            obj.put("front", c.front)
            obj.put("back", c.back)
            array.put(obj)
        }
        return array.toString()
    }

    fun quizQuestionsToJson(questions: List<QuizQuestion>): String {
        val array = JSONArray()
        for (q in questions) {
            val obj = JSONObject()
            obj.put("id", q.id)
            obj.put("question", q.question)
            val opts = JSONArray()
            q.options.forEach { opts.put(it) }
            obj.put("options", opts)
            obj.put("correct_answer", q.correctAnswer)
            obj.put("explanation", q.explanation)
            obj.put("topic_tag", q.topicTag)
            array.put(obj)
        }
        return array.toString()
    }

    fun studentAnswersToJson(answers: List<StudentAnswerRecord>): String {
        val array = JSONArray()
        for (a in answers) {
            val obj = JSONObject()
            obj.put("questionId", a.questionId)
            obj.put("selectedOption", a.selectedOption)
            obj.put("isCorrect", a.isCorrect)
            obj.put("topicTag", a.topicTag)
            array.put(obj)
        }
        return array.toString()
    }

    fun parseStudentAnswers(jsonString: String): List<StudentAnswerRecord> {
        val list = mutableListOf<StudentAnswerRecord>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    StudentAnswerRecord(
                        questionId = obj.optInt("questionId", i + 1),
                        selectedOption = obj.optString("selectedOption", ""),
                        isCorrect = obj.optBoolean("isCorrect", false),
                        topicTag = obj.optString("topicTag", "General")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun extractJsonArrayString(raw: String): String {
        val trimmed = raw.trim()
        val startIndex = trimmed.indexOf('[')
        val endIndex = trimmed.lastIndexOf(']')
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            trimmed.substring(startIndex, endIndex + 1)
        } else {
            trimmed
        }
    }
}
