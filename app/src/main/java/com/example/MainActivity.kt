package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ClassEntity
import com.example.ui.components.EduPulseTopBar
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EducatorClassDetailScreen
import com.example.ui.screens.EducatorDashboardScreen
import com.example.ui.screens.FlashcardScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.StudentDashboardScreen
import com.example.ui.theme.EduPulseTheme
import com.example.ui.viewmodel.AppRole
import com.example.ui.viewmodel.EduPulseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EduPulseTheme {
                MainContent()
            }
        }
    }
}

@Composable
fun MainContent(viewModel: EduPulseViewModel = viewModel()) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val registeredUsers by viewModel.registeredUsers.collectAsStateWithLifecycle()

    Crossfade(targetState = isLoggedIn, label = "authTransition") { loggedIn ->
        if (!loggedIn) {
            AuthScreen(
                isAuthenticating = isAuthenticating,
                authError = authError,
                demoUsers = registeredUsers,
                onLogin = { email, password ->
                    viewModel.login(email, password)
                },
                onRegister = { name, email, password, role ->
                    viewModel.register(name, email, password, role)
                },
                onSocialLogin = { name, email, role, provider ->
                    viewModel.loginSocial(name, email, role, provider)
                },
                onResetPassword = { email, onResult ->
                    viewModel.requestPasswordReset(email, onResult)
                },
                onQuickLogin = { user ->
                    viewModel.quickLogin(user)
                },
                onClearError = {
                    viewModel.clearAuthError()
                }
            )
        } else {
            EduPulseApp(viewModel = viewModel)
        }
    }
}

@Composable
fun EduPulseApp(viewModel: EduPulseViewModel) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val selectedClass by viewModel.selectedClass.collectAsStateWithLifecycle()

    // Educator data
    val allClasses by viewModel.allClasses.collectAsStateWithLifecycle()
    val selectedMaterials by viewModel.selectedClassMaterials.collectAsStateWithLifecycle()
    val selectedFlashcards by viewModel.selectedClassFlashcards.collectAsStateWithLifecycle()
    val selectedQuizzes by viewModel.selectedClassQuizzes.collectAsStateWithLifecycle()
    val selectedEnrollments by viewModel.selectedClassEnrollments.collectAsStateWithLifecycle()
    val selectedAttempts by viewModel.selectedClassAttempts.collectAsStateWithLifecycle()
    val selectedMetrics by viewModel.selectedClassMetrics.collectAsStateWithLifecycle()

    // Student data
    val studentEnrollments by viewModel.studentEnrollments.collectAsStateWithLifecycle()
    val studentAttempts by viewModel.studentAttempts.collectAsStateWithLifecycle()

    // Generation state
    val generationMode by viewModel.generationMode.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generatedFlashcards by viewModel.generatedFlashcards.collectAsStateWithLifecycle()
    val generatedQuiz by viewModel.generatedQuiz.collectAsStateWithLifecycle()
    val groundedChunks by viewModel.groundedChunks.collectAsStateWithLifecycle()

    // Active Sessions
    val activeDeck by viewModel.activeDeck.collectAsStateWithLifecycle()
    val cardIndex by viewModel.cardIndex.collectAsStateWithLifecycle()
    val isFlipped by viewModel.isFlipped.collectAsStateWithLifecycle()

    val activeQuiz by viewModel.activeQuiz.collectAsStateWithLifecycle()
    val quizQuestions by viewModel.quizQuestions.collectAsStateWithLifecycle()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val selectedQuizOption by viewModel.selectedQuizOption.collectAsStateWithLifecycle()
    val isAnswerSubmitted by viewModel.isAnswerSubmitted.collectAsStateWithLifecycle()
    val quizScore by viewModel.quizScore.collectAsStateWithLifecycle()
    val recordedAnswers by viewModel.recordedAnswers.collectAsStateWithLifecycle()
    val isQuizFinished by viewModel.isQuizFinished.collectAsStateWithLifecycle()

    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

    var currentNavIndex by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Toast UI messages
    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUiMessage()
        }
    }

    // Android Back Handler
    BackHandler(enabled = activeDeck != null || activeQuiz != null || selectedClass != null || currentNavIndex != 0) {
        when {
            activeDeck != null -> viewModel.closeFlashcardSession()
            activeQuiz != null -> viewModel.closeQuizSession()
            selectedClass != null -> viewModel.selectClass(null)
            currentNavIndex != 0 -> currentNavIndex = 0
        }
    }

    // Full screen sessions take precedence over scaffold
    if (activeDeck != null) {
        FlashcardScreen(
            deck = activeDeck!!,
            currentIndex = cardIndex,
            isFlipped = isFlipped,
            onFlip = { viewModel.flipCard() },
            onNext = { total -> viewModel.nextCard(total) },
            onPrev = { viewModel.prevCard() },
            onClose = { viewModel.closeFlashcardSession() }
        )
        return
    }

    if (activeQuiz != null) {
        QuizScreen(
            quiz = activeQuiz!!,
            questions = quizQuestions,
            currentIndex = currentQuestionIndex,
            selectedOption = selectedQuizOption,
            isAnswerSubmitted = isAnswerSubmitted,
            score = quizScore,
            recordedAnswers = recordedAnswers,
            isQuizFinished = isQuizFinished,
            onSelectOption = { viewModel.selectQuizOption(it) },
            onSubmitAnswer = { viewModel.submitAnswer() },
            onNextQuestion = { viewModel.nextQuizQuestion() },
            onClose = { viewModel.closeQuizSession() }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            EduPulseTopBar(
                currentRole = currentRole,
                userName = currentUser.name,
                onToggleRole = {
                    val nextRole = if (currentRole == AppRole.EDUCATOR) AppRole.STUDENT else AppRole.EDUCATOR
                    viewModel.switchRole(nextRole)
                    currentNavIndex = 0
                },
                onLogout = {
                    viewModel.logout()
                }
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                NavigationBarItem(
                    selected = currentNavIndex == 0,
                    onClick = { currentNavIndex = 0 },
                    icon = { Icon(Icons.Default.School, contentDescription = "Classes") },
                    label = { Text("Classes") },
                    modifier = Modifier.testTag("nav_classes")
                )
                NavigationBarItem(
                    selected = currentNavIndex == 1,
                    onClick = { currentNavIndex = 1 },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    modifier = Modifier.testTag("nav_analytics")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentNavIndex) {
                0 -> {
                    // Classes Tab
                    if (currentRole == AppRole.EDUCATOR) {
                        if (selectedClass != null) {
                            val weaknesses = viewModel.computeTopicWeaknesses(selectedMetrics)
                            EducatorClassDetailScreen(
                                classItem = selectedClass!!,
                                materials = selectedMaterials,
                                flashcardDecks = selectedFlashcards,
                                quizzes = selectedQuizzes,
                                enrollments = selectedEnrollments,
                                attempts = selectedAttempts,
                                metrics = selectedMetrics,
                                weaknessStats = weaknesses,
                                generationMode = generationMode,
                                isGenerating = isGenerating,
                                generatedFlashcards = generatedFlashcards,
                                generatedQuiz = generatedQuiz,
                                groundedChunks = groundedChunks,
                                onBack = { viewModel.selectClass(null) },
                                onUploadMaterial = { title, topic, content ->
                                    viewModel.uploadMaterial(title, topic, content)
                                },
                                onPrepareGeneration = { mat, mode, diff ->
                                    viewModel.prepareGeneration(mat, mode, diff)
                                },
                                onPublishAndAssign = { viewModel.publishAndAssign() },
                                onDismissGeneration = { viewModel.dismissGeneration() }
                            )
                        } else {
                            val educatorClasses = allClasses.filter { it.educatorId == currentUser.id }
                            EducatorDashboardScreen(
                                classes = if (educatorClasses.isNotEmpty()) educatorClasses else allClasses,
                                onSelectClass = { viewModel.selectClass(it) },
                                onCreateClass = { title, subject, section, desc ->
                                    viewModel.createNewClass(title, subject, section, desc)
                                },
                                onExportClassCsv = { classItem ->
                                    viewModel.exportAndShareClassCsv(context, classItem)
                                }
                            )
                        }
                    } else {
                        // Student Role
                        val badges = viewModel.computeStudentBadges(studentAttempts)
                        StudentDashboardScreen(
                            allClasses = allClasses,
                            enrollments = studentEnrollments,
                            selectedClass = selectedClass,
                            classMaterials = selectedMaterials,
                            classFlashcards = selectedFlashcards,
                            classQuizzes = selectedQuizzes,
                            studentAttempts = studentAttempts,
                            badges = badges,
                            onSelectClass = { viewModel.selectClass(it) },
                            onJoinClass = { code -> viewModel.joinClass(code) },
                            onStartFlashcards = { deck -> viewModel.startFlashcardDeck(deck) },
                            onStartQuiz = { quiz -> viewModel.startQuiz(quiz) }
                        )
                    }
                }
                1 -> {
                    // Analytics Tab
                    val weaknesses = viewModel.computeTopicWeaknesses(selectedMetrics)
                    val badges = viewModel.computeStudentBadges(studentAttempts)
                    AnalyticsScreen(
                        currentRole = currentRole,
                        classes = allClasses,
                        selectedClass = selectedClass,
                        classAttempts = if (selectedClass != null) selectedAttempts else studentAttempts,
                        weaknessStats = weaknesses,
                        studentAttempts = studentAttempts,
                        badges = badges,
                        onExportCsv = { classItem ->
                            viewModel.exportAndShareClassCsv(context, classItem)
                        },
                        onSaveCsvLocally = { classItem ->
                            viewModel.saveClassCsvLocally(context, classItem)
                        }
                    )
                }
            }
        }
    }
}
