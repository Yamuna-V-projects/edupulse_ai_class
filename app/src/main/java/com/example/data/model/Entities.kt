package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: String, // "EDUCATOR" or "STUDENT"
    val password: String = "password123",
    val avatarColor: Long = 0xFF2563EB
)

@Entity(
    tableName = "classes",
    indices = [Index(value = ["code"], unique = true)]
)
data class ClassEntity(
    @PrimaryKey val id: String,
    val code: String, // e.g. "XJ79K2"
    val title: String,
    val subject: String,
    val section: String = "",
    val educatorId: String,
    val educatorName: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "enrollments",
    indices = [Index(value = ["classCode", "studentId"], unique = true)]
)
data class EnrollmentEntity(
    @PrimaryKey val id: String,
    val classCode: String,
    val studentId: String,
    val studentName: String,
    val joinedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "materials",
    indices = [Index(value = ["classCode"])]
)
data class MaterialEntity(
    @PrimaryKey val id: String,
    val classCode: String,
    val title: String,
    val topic: String,
    val content: String,
    val fileType: String = "MARKDOWN", // "PDF", "SYLLABUS", "MARKDOWN"
    val uploadedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "flashcard_decks",
    indices = [Index(value = ["classCode"])]
)
data class FlashcardDeckEntity(
    @PrimaryKey val id: String,
    val classCode: String,
    val materialId: String,
    val title: String,
    val cardsJson: String,
    val isAssigned: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "quizzes",
    indices = [Index(value = ["classCode"])]
)
data class QuizEntity(
    @PrimaryKey val id: String,
    val classCode: String,
    val materialId: String,
    val title: String,
    val difficulty: String, // "Easy", "Medium", "Hard"
    val questionsJson: String,
    val isAssigned: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "quiz_attempts",
    indices = [Index(value = ["quizId"]), Index(value = ["studentId"]), Index(value = ["classCode"])]
)
data class QuizAttemptEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val classCode: String,
    val studentId: String,
    val studentName: String,
    val score: Int,
    val totalQuestions: Int,
    val timeTakenSeconds: Int,
    val answersJson: String,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "question_metrics",
    indices = [Index(value = ["classCode"]), Index(value = ["topicTag"])]
)
data class QuestionMetricEntity(
    @PrimaryKey val id: String,
    val attemptId: String,
    val quizId: String,
    val classCode: String,
    val studentId: String,
    val questionId: Int,
    val topicTag: String,
    val isCorrect: Boolean
)
