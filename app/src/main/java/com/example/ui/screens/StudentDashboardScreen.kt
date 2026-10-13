package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.FlashcardDeckEntity
import com.example.data.model.JsonParserHelper
import com.example.data.model.MaterialEntity
import com.example.data.model.QuizAttemptEntity
import com.example.data.model.QuizEntity
import com.example.data.model.StudentBadge
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun StudentDashboardScreen(
    allClasses: List<ClassEntity>,
    enrollments: List<EnrollmentEntity>,
    selectedClass: ClassEntity?,
    classMaterials: List<MaterialEntity>,
    classFlashcards: List<FlashcardDeckEntity>,
    classQuizzes: List<QuizEntity>,
    studentAttempts: List<QuizAttemptEntity>,
    badges: List<StudentBadge>,
    onSelectClass: (ClassEntity?) -> Unit,
    onJoinClass: (String) -> Unit,
    onStartFlashcards: (FlashcardDeckEntity) -> Unit,
    onStartQuiz: (QuizEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showJoinDialog by remember { mutableStateOf(false) }
    var viewingMaterial by remember { mutableStateOf<MaterialEntity?>(null) }

    val enrolledCodes = enrollments.map { it.classCode }.toSet()
    val joinedClasses = allClasses.filter { it.code in enrolledCodes }

    Box(modifier = modifier.fillMaxSize()) {
        if (selectedClass == null) {
            // Joined Classes Overview
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    // Student Welcome Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Student Learning Hub",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Access teacher-assigned syllabus notes, practice with AI flashcard study decks, and take adaptive tests.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Stats row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricCard(
                            title = "Enrolled Classes",
                            value = joinedClasses.size.toString(),
                            subtitle = "Active courses",
                            icon = Icons.Default.School,
                            iconTint = PrimaryBlue,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            title = "Tests Completed",
                            value = studentAttempts.size.toString(),
                            subtitle = "Quiz attempts",
                            icon = Icons.Default.Quiz,
                            iconTint = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Enrolled Classes Header & Join Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Classes (${joinedClasses.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Button(
                            onClick = { showJoinDialog = true },
                            modifier = Modifier.testTag("join_class_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Join Class", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (joinedClasses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Not enrolled in any classes yet",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enter a 6-character Class Code provided by your teacher (e.g., 'XJ79K2' or 'CS42B1').",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { showJoinDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text("Join with Class Code")
                                }
                            }
                        }
                    }
                } else {
                    items(joinedClasses) { classItem ->
                        StudentClassCard(
                            classItem = classItem,
                            onClick = { onSelectClass(classItem) }
                        )
                    }
                }

                // Badges Section
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Achievements & Badges",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        badges.forEach { badge ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (badge.earned) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                elevation = if (badge.earned) CardDefaults.cardElevation(defaultElevation = 2.dp) else CardDefaults.cardElevation(0.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (badge.earned) WarningAmber.copy(alpha = 0.18f) else Color.Gray.copy(alpha = 0.1f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = if (badge.earned) WarningAmber else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = badge.title,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (badge.earned) "Unlocked" else "Locked",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (badge.earned) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        } else {
            // Selected Class Student Workspace
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // Back button & header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onSelectClass(null) }, modifier = Modifier.testTag("student_back_btn")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text(
                                text = selectedClass.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Teacher: ${selectedClass.educatorName} • Code: ${selectedClass.code}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Assigned Flashcard Decks
                item {
                    Text(
                        text = "Interactive Flashcards (${classFlashcards.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (classFlashcards.isEmpty()) {
                    item {
                        Text(
                            text = "No study decks assigned for this class yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(classFlashcards) { deck ->
                        val cards = JsonParserHelper.parseFlashcards(deck.cardsJson)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(PrimaryBlue.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Style, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                                    }
                                    Column {
                                        Text(deck.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("${cards.size} Cards • 3D Flip Enabled", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Button(
                                    onClick = { onStartFlashcards(deck) },
                                    modifier = Modifier.testTag("start_deck_btn_${deck.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text("Study", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // Assigned Quizzes
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Assigned Adaptive Quizzes (${classQuizzes.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (classQuizzes.isEmpty()) {
                    item {
                        Text(
                            text = "No quizzes assigned for this class yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(classQuizzes) { quiz ->
                        val questions = JsonParserHelper.parseQuizQuestions(quiz.questionsJson)
                        val prevAttempt = studentAttempts.firstOrNull { it.quizId == quiz.id }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(AccentIndigo.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Quiz, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(22.dp))
                                    }
                                    Column {
                                        Text(quiz.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text(
                                            text = "${questions.size} Questions • ${quiz.difficulty} Difficulty" +
                                                if (prevAttempt != null) " • Best: ${prevAttempt.score}/${prevAttempt.totalQuestions}" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onStartQuiz(quiz) },
                                    modifier = Modifier.testTag("start_quiz_btn_${quiz.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (prevAttempt != null) MaterialTheme.colorScheme.secondary else PrimaryBlue
                                    )
                                ) {
                                    Text(if (prevAttempt != null) "Retake" else "Start Quiz", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // Course Reading Materials
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Course Reading Materials (${classMaterials.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (classMaterials.isEmpty()) {
                    item {
                        Text(
                            text = "No course reading materials uploaded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(classMaterials) { mat ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewingMaterial = mat },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text(mat.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("Topic: ${mat.topic}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text("Read", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = PrimaryBlue)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // Join Class Dialog
    if (showJoinDialog) {
        JoinClassDialog(
            onDismiss = { showJoinDialog = false },
            onConfirm = { code ->
                onJoinClass(code)
                showJoinDialog = false
            }
        )
    }

    // View Material Dialog
    viewingMaterial?.let { mat ->
        AlertDialog(
            onDismissRequest = { viewingMaterial = null },
            title = {
                Text(mat.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    item {
                        Text(
                            text = mat.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewingMaterial = null }) { Text("Close") }
            }
        )
    }
}

@Composable
fun StudentClassCard(
    classItem: ClassEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("student_class_card_${classItem.code}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(classItem.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(2.dp))
                Text("Instructor: ${classItem.educatorName}", style = MaterialTheme.typography.labelMedium, color = PrimaryBlue)
                Text("Class Code: ${classItem.code}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryBlue)
        }
    }
}

@Composable
fun JoinClassDialog(
    onDismiss: () -> Unit,
    onConfirm: (code: String) -> Unit
) {
    var codeInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Join Class", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ask your teacher for the 6-character class code, then enter it here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.uppercase() },
                    label = { Text("Class Code (e.g. XJ79K2)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_class_code_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (codeInput.isNotBlank()) onConfirm(codeInput.trim())
                },
                enabled = codeInput.isNotBlank(),
                modifier = Modifier.testTag("submit_join_class_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
