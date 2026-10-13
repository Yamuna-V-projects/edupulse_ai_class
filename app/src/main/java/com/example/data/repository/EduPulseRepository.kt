package com.example.data.repository

import com.example.data.local.EduPulseDao
import com.example.data.model.ClassEntity
import com.example.data.model.ClassReportBundle
import com.example.data.model.EnrollmentEntity
import com.example.data.model.FlashcardDeckEntity
import com.example.data.model.MaterialEntity
import com.example.data.model.QuestionMetricEntity
import com.example.data.model.QuizAttemptEntity
import com.example.data.model.QuizEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class EduPulseRepository(private val dao: EduPulseDao) {

    // Users
    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()

    fun getUser(id: String): Flow<UserEntity?> = dao.getUserById(id)

    suspend fun insertUser(user: UserEntity) = dao.insertUser(user)

    suspend fun login(email: String, password: String): Result<UserEntity> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            return Result.failure(Exception("Please enter your email address."))
        }
        val user = dao.findUserByEmail(trimmedEmail)
            ?: return Result.failure(Exception("No account found with this email. Please register."))
        if (user.password.isNotEmpty() && user.password != password) {
            return Result.failure(Exception("Incorrect password. Please try again."))
        }
        return Result.success(user)
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: String
    ): Result<UserEntity> {
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(Exception("Please enter your full name."))
        }
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            return Result.failure(Exception("Please enter a valid email address."))
        }
        if (password.length < 4) {
            return Result.failure(Exception("Password must be at least 4 characters."))
        }

        val existing = dao.findUserByEmail(trimmedEmail)
        if (existing != null) {
            return Result.failure(Exception("An account with this email already exists. Please sign in."))
        }

        val avatarColor = if (role == "EDUCATOR") 0xFF2563EB else 0xFF0D9488
        val newUser = UserEntity(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            name = trimmedName,
            email = trimmedEmail,
            role = role,
            password = password,
            avatarColor = avatarColor
        )
        dao.insertUser(newUser)
        return Result.success(newUser)
    }

    suspend fun loginOrRegisterSocialUser(
        name: String,
        email: String,
        role: String,
        provider: String = "Google"
    ): Result<UserEntity> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            return Result.failure(Exception("Invalid email address for $provider sign in."))
        }
        val existing = dao.findUserByEmail(trimmedEmail)
        if (existing != null) {
            return Result.success(existing)
        }

        val trimmedName = name.trim().ifEmpty {
            trimmedEmail.substringBefore("@")
                .split(".", "_")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        }

        val avatarColor = when (provider.lowercase()) {
            "google" -> 0xFF4285F4
            "microsoft" -> 0xFF00A4EF
            "apple" -> 0xFF1E293B
            else -> if (role == "EDUCATOR") 0xFF2563EB else 0xFF0D9488
        }

        val newUser = UserEntity(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            name = trimmedName,
            email = trimmedEmail,
            role = role,
            password = "sso_${provider.lowercase()}",
            avatarColor = avatarColor
        )
        dao.insertUser(newUser)
        return Result.success(newUser)
    }

    suspend fun requestPasswordReset(email: String): Result<String> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            return Result.failure(Exception("Please enter a valid email address to reset password."))
        }
        val existing = dao.findUserByEmail(trimmedEmail)
        return if (existing != null) {
            Result.success("Password reset instructions and a 6-digit verification code have been dispatched to $trimmedEmail.")
        } else {
            Result.success("If an account exists with $trimmedEmail, reset instructions have been sent.")
        }
    }

    // Classes
    val allClasses: Flow<List<ClassEntity>> = dao.getAllClasses()

    fun getClassesForEducator(educatorId: String): Flow<List<ClassEntity>> =
        dao.getClassesByEducator(educatorId)

    fun getClassByCode(code: String): Flow<ClassEntity?> = dao.getClassByCode(code)

    suspend fun createClass(
        title: String,
        subject: String,
        section: String,
        educatorId: String,
        educatorName: String,
        description: String
    ): ClassEntity {
        val randomCode = generateClassCode()
        val newClass = ClassEntity(
            id = "class_${UUID.randomUUID().toString().take(8)}",
            code = randomCode,
            title = title,
            subject = subject,
            section = section,
            educatorId = educatorId,
            educatorName = educatorName,
            description = description
        )
        dao.insertClass(newClass)
        return newClass
    }

    suspend fun deleteClass(id: String) = dao.deleteClass(id)

    // Enrollments
    fun getEnrollmentsForStudent(studentId: String): Flow<List<EnrollmentEntity>> =
        dao.getEnrollmentsForStudent(studentId)

    fun getEnrollmentsForClass(classCode: String): Flow<List<EnrollmentEntity>> =
        dao.getEnrollmentsForClass(classCode)

    suspend fun joinClassByCode(
        code: String,
        studentId: String,
        studentName: String
    ): Result<ClassEntity> {
        val uppercaseCode = code.trim().uppercase()
        val foundClass = dao.findClassByCode(uppercaseCode)
            ?: return Result.failure(Exception("Class code '$uppercaseCode' not found. Please verify with your educator."))

        val isEnrolled = dao.isStudentEnrolled(uppercaseCode, studentId)
        if (!isEnrolled) {
            val enrollment = EnrollmentEntity(
                id = "enr_${UUID.randomUUID().toString().take(8)}",
                classCode = uppercaseCode,
                studentId = studentId,
                studentName = studentName
            )
            dao.insertEnrollment(enrollment)
        }
        return Result.success(foundClass)
    }

    // Materials
    fun getMaterialsForClass(classCode: String): Flow<List<MaterialEntity>> =
        dao.getMaterialsForClass(classCode)

    fun getMaterialById(id: String): Flow<MaterialEntity?> = dao.getMaterialById(id)

    suspend fun findMaterialById(id: String): MaterialEntity? = dao.findMaterialById(id)

    suspend fun addMaterial(
        classCode: String,
        title: String,
        topic: String,
        content: String,
        fileType: String = "MARKDOWN"
    ): MaterialEntity {
        val material = MaterialEntity(
            id = "mat_${UUID.randomUUID().toString().take(8)}",
            classCode = classCode,
            title = title,
            topic = topic,
            content = content,
            fileType = fileType
        )
        dao.insertMaterial(material)
        return material
    }

    suspend fun deleteMaterial(id: String) = dao.deleteMaterial(id)

    // Flashcards
    fun getFlashcardsForClass(classCode: String): Flow<List<FlashcardDeckEntity>> =
        dao.getFlashcardDecksForClass(classCode)

    fun getFlashcardDeckById(id: String): Flow<FlashcardDeckEntity?> =
        dao.getFlashcardDeckById(id)

    suspend fun insertFlashcardDeck(deck: FlashcardDeckEntity) =
        dao.insertFlashcardDeck(deck)

    suspend fun updateFlashcardDeckAssignment(id: String, isAssigned: Boolean) =
        dao.updateFlashcardDeckAssignment(id, isAssigned)

    // Quizzes
    fun getQuizzesForClass(classCode: String): Flow<List<QuizEntity>> =
        dao.getQuizzesForClass(classCode)

    fun getQuizById(id: String): Flow<QuizEntity?> = dao.getQuizById(id)

    suspend fun insertQuiz(quiz: QuizEntity) = dao.insertQuiz(quiz)

    suspend fun updateQuizAssignment(id: String, isAssigned: Boolean) =
        dao.updateQuizAssignment(id, isAssigned)

    // Quiz Attempts & Metrics
    fun getAttemptsForQuiz(quizId: String): Flow<List<QuizAttemptEntity>> =
        dao.getAttemptsForQuiz(quizId)

    fun getAttemptsForClass(classCode: String): Flow<List<QuizAttemptEntity>> =
        dao.getAttemptsForClass(classCode)

    fun getAttemptsForStudent(studentId: String): Flow<List<QuizAttemptEntity>> =
        dao.getAttemptsForStudent(studentId)

    suspend fun recordQuizAttempt(
        attempt: QuizAttemptEntity,
        metrics: List<QuestionMetricEntity>
    ) {
        dao.insertAttempt(attempt)
        dao.insertMetrics(metrics)
    }

    fun getMetricsForClass(classCode: String): Flow<List<QuestionMetricEntity>> =
        dao.getMetricsForClass(classCode)

    fun getMetricsForStudent(studentId: String): Flow<List<QuestionMetricEntity>> =
        dao.getMetricsForStudent(studentId)

    suspend fun fetchClassReportData(classCode: String): ClassReportBundle {
        val enrollments = dao.findEnrollmentsForClass(classCode)
        val materials = dao.findMaterialsForClass(classCode)
        val quizzes = dao.findQuizzesForClass(classCode)
        val attempts = dao.findAttemptsForClass(classCode)
        val metrics = dao.findMetricsForClass(classCode)
        return ClassReportBundle(
            enrollments = enrollments,
            materials = materials,
            quizzes = quizzes,
            attempts = attempts,
            metrics = metrics
        )
    }

    private fun generateClassCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
}
