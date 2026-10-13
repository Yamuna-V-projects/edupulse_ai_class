package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.CsvReportGenerator
import java.io.File
import com.example.data.local.EduPulseDatabase
import com.example.data.model.ClassEntity
import com.example.data.model.ClassReportBundle
import com.example.data.model.EnrollmentEntity
import com.example.data.model.FlashcardDeckEntity
import com.example.data.model.FlashcardItem
import com.example.data.model.JsonParserHelper
import com.example.data.model.MaterialEntity
import com.example.data.model.QuestionMetricEntity
import com.example.data.model.QuizAttemptEntity
import com.example.data.model.QuizEntity
import com.example.data.model.QuizQuestion
import com.example.data.model.StudentAnswerRecord
import com.example.data.model.StudentBadge
import com.example.data.model.TopicWeaknessStat
import com.example.data.model.UserEntity
import com.example.data.network.GeminiApiClient
import com.example.data.rag.TextChunk
import com.example.data.repository.EduPulseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppRole {
    EDUCATOR,
    STUDENT
}

enum class GenerationMode {
    NONE,
    FLASHCARDS,
    QUIZ
}

class EduPulseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EduPulseRepository
    init {
        val db = EduPulseDatabase.getDatabase(application, viewModelScope)
        repository = EduPulseRepository(db.eduPulseDao())
        viewModelScope.launch(Dispatchers.IO) {
            val user = db.eduPulseDao().findUserByEmail("aris.vance@university.edu")
            if (user == null) {
                EduPulseDatabase.seedInitialData(db.eduPulseDao())
            }
        }
    }

    // Authentication & Role
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    val registeredUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentRole = MutableStateFlow(AppRole.EDUCATOR)
    val currentRole: StateFlow<AppRole> = _currentRole.asStateFlow()

    private val _currentUser = MutableStateFlow(
        UserEntity("edu_vance", "Dr. Aris Vance", "aris.vance@university.edu", "EDUCATOR")
    )
    val currentUser: StateFlow<UserEntity> = _currentUser.asStateFlow()

    // Navigation and Selection
    private val _selectedClass = MutableStateFlow<ClassEntity?>(null)
    val selectedClass: StateFlow<ClassEntity?> = _selectedClass.asStateFlow()

    // Educator Data Flows
    val allClasses: StateFlow<List<ClassEntity>> = repository.allClasses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedClassMaterials = MutableStateFlow<List<MaterialEntity>>(emptyList())
    val selectedClassMaterials: StateFlow<List<MaterialEntity>> = _selectedClassMaterials.asStateFlow()

    private val _selectedClassFlashcards = MutableStateFlow<List<FlashcardDeckEntity>>(emptyList())
    val selectedClassFlashcards: StateFlow<List<FlashcardDeckEntity>> = _selectedClassFlashcards.asStateFlow()

    private val _selectedClassQuizzes = MutableStateFlow<List<QuizEntity>>(emptyList())
    val selectedClassQuizzes: StateFlow<List<QuizEntity>> = _selectedClassQuizzes.asStateFlow()

    private val _selectedClassEnrollments = MutableStateFlow<List<EnrollmentEntity>>(emptyList())
    val selectedClassEnrollments: StateFlow<List<EnrollmentEntity>> = _selectedClassEnrollments.asStateFlow()

    private val _selectedClassAttempts = MutableStateFlow<List<QuizAttemptEntity>>(emptyList())
    val selectedClassAttempts: StateFlow<List<QuizAttemptEntity>> = _selectedClassAttempts.asStateFlow()

    private val _selectedClassMetrics = MutableStateFlow<List<QuestionMetricEntity>>(emptyList())
    val selectedClassMetrics: StateFlow<List<QuestionMetricEntity>> = _selectedClassMetrics.asStateFlow()

    // Student Data Flows
    private val _studentEnrollments = MutableStateFlow<List<EnrollmentEntity>>(emptyList())
    val studentEnrollments: StateFlow<List<EnrollmentEntity>> = _studentEnrollments.asStateFlow()

    private val _studentAttempts = MutableStateFlow<List<QuizAttemptEntity>>(emptyList())
    val studentAttempts: StateFlow<List<QuizAttemptEntity>> = _studentAttempts.asStateFlow()

    // Generation / RAG State
    private val _generationMode = MutableStateFlow(GenerationMode.NONE)
    val generationMode: StateFlow<GenerationMode> = _generationMode.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generatedFlashcards = MutableStateFlow<List<FlashcardItem>?>(null)
    val generatedFlashcards: StateFlow<List<FlashcardItem>?> = _generatedFlashcards.asStateFlow()

    private val _generatedQuiz = MutableStateFlow<List<QuizQuestion>?>(null)
    val generatedQuiz: StateFlow<List<QuizQuestion>?> = _generatedQuiz.asStateFlow()

    private val _groundedChunks = MutableStateFlow<List<TextChunk>>(emptyList())
    val groundedChunks: StateFlow<List<TextChunk>> = _groundedChunks.asStateFlow()

    private val _activeMaterialForGeneration = MutableStateFlow<MaterialEntity?>(null)
    val activeMaterialForGeneration: StateFlow<MaterialEntity?> = _activeMaterialForGeneration.asStateFlow()

    private val _generationDifficulty = MutableStateFlow("Medium")
    val generationDifficulty: StateFlow<String> = _generationDifficulty.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    // Active Study / Interactive Quiz Session State
    private val _activeDeck = MutableStateFlow<FlashcardDeckEntity?>(null)
    val activeDeck: StateFlow<FlashcardDeckEntity?> = _activeDeck.asStateFlow()
    private val _cardIndex = MutableStateFlow(0)
    val cardIndex: StateFlow<Int> = _cardIndex.asStateFlow()
    private val _isFlipped = MutableStateFlow(false)
    val isFlipped: StateFlow<Boolean> = _isFlipped.asStateFlow()

    private val _activeQuiz = MutableStateFlow<QuizEntity?>(null)
    val activeQuiz: StateFlow<QuizEntity?> = _activeQuiz.asStateFlow()
    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions.asStateFlow()
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()
    private val _selectedQuizOption = MutableStateFlow<String?>(null)
    val selectedQuizOption: StateFlow<String?> = _selectedQuizOption.asStateFlow()
    private val _isAnswerSubmitted = MutableStateFlow(false)
    val isAnswerSubmitted: StateFlow<Boolean> = _isAnswerSubmitted.asStateFlow()
    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()
    private val _recordedAnswers = MutableStateFlow<List<StudentAnswerRecord>>(emptyList())
    val recordedAnswers: StateFlow<List<StudentAnswerRecord>> = _recordedAnswers.asStateFlow()
    private val _isQuizFinished = MutableStateFlow(false)
    val isQuizFinished: StateFlow<Boolean> = _isQuizFinished.asStateFlow()
    private val _quizElapsedSeconds = MutableStateFlow(0)
    val quizElapsedSeconds: StateFlow<Int> = _quizElapsedSeconds.asStateFlow()

    init {
        // Observe student data for Maya
        observeStudentData()
    }

    fun login(email: String, password: String) {
        _isAuthenticating.value = true
        _authError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.login(email, password)
            _isAuthenticating.value = false
            result.onSuccess { user ->
                _currentUser.value = user
                _currentRole.value = if (user.role == "EDUCATOR") AppRole.EDUCATOR else AppRole.STUDENT
                _isLoggedIn.value = true
                _selectedClass.value = null
                if (user.role == "STUDENT") {
                    observeStudentData()
                }
            }.onFailure { ex ->
                _authError.value = ex.message ?: "Authentication failed."
            }
        }
    }

    fun register(name: String, email: String, password: String, role: String) {
        _isAuthenticating.value = true
        _authError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.register(name, email, password, role)
            _isAuthenticating.value = false
            result.onSuccess { user ->
                _currentUser.value = user
                _currentRole.value = if (user.role == "EDUCATOR") AppRole.EDUCATOR else AppRole.STUDENT
                _isLoggedIn.value = true
                _selectedClass.value = null
                if (user.role == "STUDENT") {
                    observeStudentData()
                }
            }.onFailure { ex ->
                _authError.value = ex.message ?: "Registration failed."
            }
        }
    }

    fun quickLogin(user: UserEntity) {
        _currentUser.value = user
        _currentRole.value = if (user.role == "EDUCATOR") AppRole.EDUCATOR else AppRole.STUDENT
        _isLoggedIn.value = true
        _authError.value = null
        _selectedClass.value = null
        if (user.role == "STUDENT") {
            observeStudentData()
        }
    }

    fun loginSocial(name: String, email: String, role: String, provider: String = "Google") {
        _isAuthenticating.value = true
        _authError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.loginOrRegisterSocialUser(name, email, role, provider)
            _isAuthenticating.value = false
            result.onSuccess { user ->
                _currentUser.value = user
                _currentRole.value = if (user.role == "EDUCATOR") AppRole.EDUCATOR else AppRole.STUDENT
                _isLoggedIn.value = true
                _selectedClass.value = null
                _uiMessage.value = "Signed in with $provider as ${user.name}"
                if (user.role == "STUDENT") {
                    observeStudentData()
                }
            }.onFailure { ex ->
                _authError.value = ex.message ?: "$provider authentication failed."
            }
        }
    }

    fun requestPasswordReset(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.requestPasswordReset(email)
            result.onSuccess { msg ->
                onResult(true, msg)
            }.onFailure { ex ->
                onResult(false, ex.message ?: "Failed to process password reset.")
            }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _selectedClass.value = null
        clearActiveSessions()
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun switchRole(role: AppRole) {
        _currentRole.value = role
        if (role == AppRole.EDUCATOR) {
            _currentUser.value = UserEntity("edu_vance", "Dr. Aris Vance", "aris.vance@university.edu", "EDUCATOR")
        } else {
            _currentUser.value = UserEntity("stu_maya", "Maya Chen", "maya.chen@student.edu", "STUDENT")
            observeStudentData()
        }
        _selectedClass.value = null
        clearActiveSessions()
    }

    private fun observeStudentData() {
        val studentId = _currentUser.value.id
        viewModelScope.launch(Dispatchers.IO) {
            repository.getEnrollmentsForStudent(studentId).collect {
                _studentEnrollments.value = it
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAttemptsForStudent(studentId).collect {
                _studentAttempts.value = it
            }
        }
    }

    fun selectClass(classEntity: ClassEntity?) {
        _selectedClass.value = classEntity
        if (classEntity != null) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.getMaterialsForClass(classEntity.code).collect {
                    _selectedClassMaterials.value = it
                }
            }
            viewModelScope.launch(Dispatchers.IO) {
                repository.getFlashcardsForClass(classEntity.code).collect {
                    _selectedClassFlashcards.value = it
                }
            }
            viewModelScope.launch(Dispatchers.IO) {
                repository.getQuizzesForClass(classEntity.code).collect {
                    _selectedClassQuizzes.value = it
                }
            }
            viewModelScope.launch(Dispatchers.IO) {
                repository.getEnrollmentsForClass(classEntity.code).collect {
                    _selectedClassEnrollments.value = it
                }
            }
            viewModelScope.launch(Dispatchers.IO) {
                repository.getAttemptsForClass(classEntity.code).collect {
                    _selectedClassAttempts.value = it
                }
            }
            viewModelScope.launch(Dispatchers.IO) {
                repository.getMetricsForClass(classEntity.code).collect {
                    _selectedClassMetrics.value = it
                }
            }
        } else {
            _selectedClassMaterials.value = emptyList()
            _selectedClassFlashcards.value = emptyList()
            _selectedClassQuizzes.value = emptyList()
            _selectedClassEnrollments.value = emptyList()
            _selectedClassAttempts.value = emptyList()
            _selectedClassMetrics.value = emptyList()
        }
    }

    // Educator Actions
    fun createNewClass(title: String, subject: String, section: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = _currentUser.value
            val newClass = repository.createClass(
                title = title,
                subject = subject,
                section = section,
                educatorId = user.id,
                educatorName = user.name,
                description = description
            )
            _uiMessage.value = "Class '${newClass.title}' created with Code: ${newClass.code}"
            selectClass(newClass)
        }
    }

    fun uploadMaterial(title: String, topic: String, content: String, fileType: String = "MARKDOWN") {
        val currentClass = _selectedClass.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val mat = repository.addMaterial(
                classCode = currentClass.code,
                title = title,
                topic = topic,
                content = content,
                fileType = fileType
            )
            _uiMessage.value = "Material '${mat.title}' added to class!"
        }
    }

    fun prepareGeneration(material: MaterialEntity, mode: GenerationMode, difficulty: String = "Medium") {
        _activeMaterialForGeneration.value = material
        _generationMode.value = mode
        _generationDifficulty.value = difficulty
        _generatedFlashcards.value = null
        _generatedQuiz.value = null
        _groundedChunks.value = emptyList()

        triggerGeneration()
    }

    fun triggerGeneration() {
        val material = _activeMaterialForGeneration.value ?: return
        val mode = _generationMode.value
        val difficulty = _generationDifficulty.value

        _isGenerating.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (mode) {
                    GenerationMode.FLASHCARDS -> {
                        val (cards, chunks) = GeminiApiClient.generateFlashcardsWithRag(material.content)
                        _generatedFlashcards.value = cards
                        _groundedChunks.value = chunks
                        _uiMessage.value = "Generated ${cards.size} Flashcards using RAG!"
                    }
                    GenerationMode.QUIZ -> {
                        val (questions, chunks) = GeminiApiClient.generateQuizWithRag(material.content, difficulty)
                        _generatedQuiz.value = questions
                        _groundedChunks.value = chunks
                        _uiMessage.value = "Generated $difficulty Quiz with ${questions.size} questions!"
                    }
                    GenerationMode.NONE -> {}
                }
            } catch (e: Exception) {
                _uiMessage.value = "Generation error: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun publishAndAssign() {
        val currentClass = _selectedClass.value ?: return
        val material = _activeMaterialForGeneration.value ?: return
        val mode = _generationMode.value

        viewModelScope.launch(Dispatchers.IO) {
            when (mode) {
                GenerationMode.FLASHCARDS -> {
                    val cards = _generatedFlashcards.value ?: return@launch
                    val deck = FlashcardDeckEntity(
                        id = "deck_${UUID.randomUUID().toString().take(8)}",
                        classCode = currentClass.code,
                        materialId = material.id,
                        title = "${material.title} - Study Deck",
                        cardsJson = JsonParserHelper.flashcardsToJson(cards),
                        isAssigned = true
                    )
                    repository.insertFlashcardDeck(deck)
                    _uiMessage.value = "Published Flashcard Deck to Class ${currentClass.code}!"
                }
                GenerationMode.QUIZ -> {
                    val questions = _generatedQuiz.value ?: return@launch
                    val diff = _generationDifficulty.value
                    val quiz = QuizEntity(
                        id = "quiz_${UUID.randomUUID().toString().take(8)}",
                        classCode = currentClass.code,
                        materialId = material.id,
                        title = "${material.title} ($diff Quiz)",
                        difficulty = diff,
                        questionsJson = JsonParserHelper.quizQuestionsToJson(questions),
                        isAssigned = true
                    )
                    repository.insertQuiz(quiz)
                    _uiMessage.value = "Published $diff Quiz to Class ${currentClass.code}!"
                }
                GenerationMode.NONE -> {}
            }
            dismissGeneration()
        }
    }

    fun dismissGeneration() {
        _generationMode.value = GenerationMode.NONE
        _generatedFlashcards.value = null
        _generatedQuiz.value = null
        _groundedChunks.value = emptyList()
        _activeMaterialForGeneration.value = null
    }

    // Student Actions
    fun joinClass(code: String) {
        val user = _currentUser.value
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.joinClassByCode(code, user.id, user.name)
            result.onSuccess {
                _uiMessage.value = "Successfully joined ${it.title}!"
                selectClass(it)
            }.onFailure {
                _uiMessage.value = it.message ?: "Failed to join class."
            }
        }
    }

    // Flashcard Session
    fun startFlashcardDeck(deck: FlashcardDeckEntity) {
        _activeDeck.value = deck
        _cardIndex.value = 0
        _isFlipped.value = false
    }

    fun flipCard() {
        _isFlipped.value = !_isFlipped.value
    }

    fun nextCard(total: Int) {
        if (_cardIndex.value < total - 1) {
            _cardIndex.value += 1
            _isFlipped.value = false
        }
    }

    fun prevCard() {
        if (_cardIndex.value > 0) {
            _cardIndex.value -= 1
            _isFlipped.value = false
        }
    }

    fun closeFlashcardSession() {
        _activeDeck.value = null
        _cardIndex.value = 0
        _isFlipped.value = false
    }

    // Quiz Session
    fun startQuiz(quiz: QuizEntity) {
        val questions = JsonParserHelper.parseQuizQuestions(quiz.questionsJson)
        _activeQuiz.value = quiz
        _quizQuestions.value = questions
        _currentQuestionIndex.value = 0
        _selectedQuizOption.value = null
        _isAnswerSubmitted.value = false
        _quizScore.value = 0
        _recordedAnswers.value = emptyList()
        _isQuizFinished.value = false
        _quizElapsedSeconds.value = 0
    }

    fun selectQuizOption(option: String) {
        if (!_isAnswerSubmitted.value) {
            _selectedQuizOption.value = option
        }
    }

    fun submitAnswer() {
        val option = _selectedQuizOption.value ?: return
        val questions = _quizQuestions.value
        val currentIndex = _currentQuestionIndex.value
        val question = questions.getOrNull(currentIndex) ?: return

        _isAnswerSubmitted.value = true
        val isCorrect = option.trim() == question.correctAnswer.trim()
        if (isCorrect) {
            _quizScore.value += 1
        }

        val record = StudentAnswerRecord(
            questionId = question.id,
            selectedOption = option,
            isCorrect = isCorrect,
            topicTag = question.topicTag
        )
        _recordedAnswers.value = _recordedAnswers.value + record
    }

    fun nextQuizQuestion() {
        val questions = _quizQuestions.value
        if (_currentQuestionIndex.value < questions.size - 1) {
            _currentQuestionIndex.value += 1
            _selectedQuizOption.value = null
            _isAnswerSubmitted.value = false
        } else {
            finishQuiz()
        }
    }

    private fun finishQuiz() {
        val quiz = _activeQuiz.value ?: return
        val user = _currentUser.value
        val total = _quizQuestions.value.size
        val score = _quizScore.value
        val answers = _recordedAnswers.value
        val timeTaken = (_quizElapsedSeconds.value).coerceAtLeast(35)

        _isQuizFinished.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val attemptId = "att_${UUID.randomUUID().toString().take(8)}"
            val attempt = QuizAttemptEntity(
                id = attemptId,
                quizId = quiz.id,
                classCode = quiz.classCode,
                studentId = user.id,
                studentName = user.name,
                score = score,
                totalQuestions = total,
                timeTakenSeconds = timeTaken,
                answersJson = JsonParserHelper.studentAnswersToJson(answers)
            )

            val metrics = answers.map { ans ->
                QuestionMetricEntity(
                    id = "qm_${UUID.randomUUID().toString().take(8)}",
                    attemptId = attemptId,
                    quizId = quiz.id,
                    classCode = quiz.classCode,
                    studentId = user.id,
                    questionId = ans.questionId,
                    topicTag = ans.topicTag,
                    isCorrect = ans.isCorrect
                )
            }
            repository.recordQuizAttempt(attempt, metrics)
        }
    }

    fun closeQuizSession() {
        _activeQuiz.value = null
        _quizQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        _selectedQuizOption.value = null
        _isAnswerSubmitted.value = false
        _isQuizFinished.value = false
    }

    fun clearActiveSessions() {
        closeFlashcardSession()
        closeQuizSession()
        dismissGeneration()
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    // Concept Weakness Computation for Classroom Analytics
    fun computeTopicWeaknesses(metrics: List<QuestionMetricEntity>): List<TopicWeaknessStat> {
        val grouped = metrics.groupBy { it.topicTag }
        return grouped.map { (topic, list) ->
            val total = list.size
            val incorrect = list.count { !it.isCorrect }
            val rate = if (total > 0) ((incorrect * 100) / total) else 0
            TopicWeaknessStat(
                topicTag = topic,
                totalAttempts = total,
                incorrectAttempts = incorrect,
                failureRatePercent = rate
            )
        }.sortedByDescending { it.failureRatePercent }
    }

    fun computeStudentBadges(attempts: List<QuizAttemptEntity>): List<StudentBadge> {
        val totalAttempts = attempts.size
        val hasHighScore = attempts.any { it.totalQuestions > 0 && (it.score.toDouble() / it.totalQuestions) >= 0.8 }
        val hasPerfectScore = attempts.any { it.totalQuestions > 0 && it.score == it.totalQuestions }
        val isFast = attempts.any { it.timeTakenSeconds in 1..60 }

        return listOf(
            StudentBadge(
                id = "b1",
                title = "Quiz Initiate",
                description = "Completed your first adaptive quiz",
                iconName = "CheckCircle",
                earned = totalAttempts >= 1
            ),
            StudentBadge(
                id = "b2",
                title = "High Flyer (80%+)",
                description = "Scored 80% or higher on a quiz",
                iconName = "Star",
                earned = hasHighScore
            ),
            StudentBadge(
                id = "b3",
                title = "Perfectionist (100%)",
                description = "Scored 100% on any classroom quiz",
                iconName = "EmojiEvents",
                earned = hasPerfectScore
            ),
            StudentBadge(
                id = "b4",
                title = "Knowledge Seeker",
                description = "Completed 3 or more assessments",
                iconName = "School",
                earned = totalAttempts >= 3
            )
        )
    }

    suspend fun getFullClassCsv(classItem: ClassEntity): String {
        val bundle = repository.fetchClassReportData(classItem.code)
        val weaknesses = computeTopicWeaknesses(bundle.metrics)
        return CsvReportGenerator.generateClassPerformanceCsv(
            classItem = classItem,
            enrollments = bundle.enrollments,
            quizzes = bundle.quizzes,
            attempts = bundle.attempts,
            weaknessStats = weaknesses,
            materials = bundle.materials,
            metrics = bundle.metrics
        )
    }

    fun exportAndShareClassCsv(context: Context, classItem: ClassEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val csv = getFullClassCsv(classItem)
            launch(Dispatchers.Main) {
                CsvReportGenerator.shareCsvReport(context, classItem.title, classItem.code, csv)
            }
        }
    }

    fun saveClassCsvLocally(context: Context, classItem: ClassEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val csv = getFullClassCsv(classItem)
            launch(Dispatchers.Main) {
                CsvReportGenerator.saveCsvToDeviceWithToast(context, classItem.title, classItem.code, csv)
            }
        }
    }
}
