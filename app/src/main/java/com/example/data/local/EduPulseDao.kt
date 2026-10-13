package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ClassEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.FlashcardDeckEntity
import com.example.data.model.MaterialEntity
import com.example.data.model.QuestionMetricEntity
import com.example.data.model.QuizAttemptEntity
import com.example.data.model.QuizEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EduPulseDao {

    // Users
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun findUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) AND password = :password LIMIT 1")
    suspend fun authenticateUser(email: String, password: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Classes
    @Query("SELECT * FROM classes ORDER BY createdAt DESC")
    fun getAllClasses(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE educatorId = :educatorId ORDER BY createdAt DESC")
    fun getClassesByEducator(educatorId: String): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE code = :code LIMIT 1")
    fun getClassByCode(code: String): Flow<ClassEntity?>

    @Query("SELECT * FROM classes WHERE code = :code LIMIT 1")
    suspend fun findClassByCode(code: String): ClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classEntity: ClassEntity)

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteClass(id: String)

    // Enrollments
    @Query("SELECT * FROM enrollments WHERE studentId = :studentId")
    fun getEnrollmentsForStudent(studentId: String): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE classCode = :classCode")
    fun getEnrollmentsForClass(classCode: String): Flow<List<EnrollmentEntity>>

    @Query("SELECT COUNT(*) > 0 FROM enrollments WHERE classCode = :classCode AND studentId = :studentId")
    suspend fun isStudentEnrolled(classCode: String, studentId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollment(enrollment: EnrollmentEntity)

    // Materials
    @Query("SELECT * FROM materials WHERE classCode = :classCode ORDER BY uploadedAt DESC")
    fun getMaterialsForClass(classCode: String): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE id = :id LIMIT 1")
    fun getMaterialById(id: String): Flow<MaterialEntity?>

    @Query("SELECT * FROM materials WHERE id = :id LIMIT 1")
    suspend fun findMaterialById(id: String): MaterialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: MaterialEntity)

    @Query("DELETE FROM materials WHERE id = :id")
    suspend fun deleteMaterial(id: String)

    // Flashcard Decks
    @Query("SELECT * FROM flashcard_decks WHERE classCode = :classCode ORDER BY createdAt DESC")
    fun getFlashcardDecksForClass(classCode: String): Flow<List<FlashcardDeckEntity>>

    @Query("SELECT * FROM flashcard_decks WHERE id = :id LIMIT 1")
    fun getFlashcardDeckById(id: String): Flow<FlashcardDeckEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcardDeck(deck: FlashcardDeckEntity)

    @Query("UPDATE flashcard_decks SET isAssigned = :isAssigned WHERE id = :id")
    suspend fun updateFlashcardDeckAssignment(id: String, isAssigned: Boolean)

    // Quizzes
    @Query("SELECT * FROM quizzes WHERE classCode = :classCode ORDER BY createdAt DESC")
    fun getQuizzesForClass(classCode: String): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes WHERE id = :id LIMIT 1")
    fun getQuizById(id: String): Flow<QuizEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity)

    @Query("UPDATE quizzes SET isAssigned = :isAssigned WHERE id = :id")
    suspend fun updateQuizAssignment(id: String, isAssigned: Boolean)

    // Quiz Attempts
    @Query("SELECT * FROM quiz_attempts WHERE quizId = :quizId ORDER BY completedAt DESC")
    fun getAttemptsForQuiz(quizId: String): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts WHERE classCode = :classCode ORDER BY completedAt DESC")
    fun getAttemptsForClass(classCode: String): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts WHERE studentId = :studentId ORDER BY completedAt DESC")
    fun getAttemptsForStudent(studentId: String): Flow<List<QuizAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttemptEntity)

    // Question Metrics
    @Query("SELECT * FROM question_metrics WHERE classCode = :classCode")
    fun getMetricsForClass(classCode: String): Flow<List<QuestionMetricEntity>>

    @Query("SELECT * FROM question_metrics WHERE studentId = :studentId")
    fun getMetricsForStudent(studentId: String): Flow<List<QuestionMetricEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetrics(metrics: List<QuestionMetricEntity>)

    // Synchronous data queries for CSV performance report generation
    @Query("SELECT * FROM enrollments WHERE classCode = :classCode")
    suspend fun findEnrollmentsForClass(classCode: String): List<EnrollmentEntity>

    @Query("SELECT * FROM materials WHERE classCode = :classCode ORDER BY uploadedAt DESC")
    suspend fun findMaterialsForClass(classCode: String): List<MaterialEntity>

    @Query("SELECT * FROM quizzes WHERE classCode = :classCode ORDER BY createdAt DESC")
    suspend fun findQuizzesForClass(classCode: String): List<QuizEntity>

    @Query("SELECT * FROM quiz_attempts WHERE classCode = :classCode ORDER BY completedAt DESC")
    suspend fun findAttemptsForClass(classCode: String): List<QuizAttemptEntity>

    @Query("SELECT * FROM question_metrics WHERE classCode = :classCode")
    suspend fun findMetricsForClass(classCode: String): List<QuestionMetricEntity>
}

