package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassEntity
import com.example.util.CsvReportGenerator
import com.example.data.model.EnrollmentEntity
import com.example.data.model.FlashcardDeckEntity
import com.example.data.model.FlashcardItem
import com.example.data.model.JsonParserHelper
import com.example.data.model.MaterialEntity
import com.example.data.model.QuestionMetricEntity
import com.example.data.model.QuizAttemptEntity
import com.example.data.model.QuizEntity
import com.example.data.model.QuizQuestion
import com.example.data.model.TopicWeaknessStat
import com.example.data.rag.TextChunk
import com.example.ui.components.ClassCodeCard
import com.example.ui.components.ConceptWeaknessTracker
import com.example.ui.components.RAGChunksViewer
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.GenerationMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducatorClassDetailScreen(
    classItem: ClassEntity,
    materials: List<MaterialEntity>,
    flashcardDecks: List<FlashcardDeckEntity>,
    quizzes: List<QuizEntity>,
    enrollments: List<EnrollmentEntity>,
    attempts: List<QuizAttemptEntity>,
    metrics: List<QuestionMetricEntity>,
    weaknessStats: List<TopicWeaknessStat>,
    generationMode: GenerationMode,
    isGenerating: Boolean,
    generatedFlashcards: List<FlashcardItem>?,
    generatedQuiz: List<QuizQuestion>?,
    groundedChunks: List<TextChunk>,
    onBack: () -> Unit,
    onUploadMaterial: (title: String, topic: String, content: String) -> Unit,
    onPrepareGeneration: (MaterialEntity, GenerationMode, String) -> Unit,
    onPublishAndAssign: () -> Unit,
    onDismissGeneration: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddMaterialDialog by remember { mutableStateOf(false) }
    var showDifficultyPickerForMaterial by remember { mutableStateOf<MaterialEntity?>(null) }
    var showExportCsvDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Class Header & Back Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("back_to_classes_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Classes"
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = classItem.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${classItem.subject} • ${enrollments.size} Students Enrolled",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { showExportCsvDialog = true },
                modifier = Modifier.testTag("export_csv_header_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Export Performance Report CSV",
                    tint = PrimaryBlue
                )
            }
        }

        // Prominent Class Join Code Card
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            ClassCodeCard(classCode = classItem.code)
        }

        // Secondary Tabs
        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Course Materials (${materials.size})", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Assignments", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Analytics & Roster", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        // Main Tab Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            when (selectedTab) {
                0 -> {
                    // Materials Tab
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Uploaded Course Materials",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Button(
                                onClick = { showAddMaterialDialog = true },
                                modifier = Modifier.testTag("upload_material_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Material", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    if (materials.isEmpty()) {
                        item {
                            EmptyMaterialsCard(onAddClicked = { showAddMaterialDialog = true })
                        }
                    } else {
                        items(materials) { mat ->
                            MaterialCard(
                                material = mat,
                                onGenerateFlashcards = {
                                    onPrepareGeneration(mat, GenerationMode.FLASHCARDS, "Medium")
                                },
                                onGenerateQuiz = {
                                    showDifficultyPickerForMaterial = mat
                                }
                            )
                        }
                    }
                }
                1 -> {
                    // Assignments Tab (Flashcards and Quizzes)
                    item {
                        Text(
                            text = "Assigned Flashcard Decks (${flashcardDecks.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (flashcardDecks.isEmpty()) {
                        item {
                            Text(
                                text = "No flashcard decks published yet. Generate a deck from Course Materials to assign to students.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(flashcardDecks) { deck ->
                            val cards = JsonParserHelper.parseFlashcards(deck.cardsJson)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryBlue.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Style, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                        }
                                        Column {
                                            Text(deck.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            Text("${cards.size} Interactive Cards • Published to Class", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Badge(containerColor = SuccessGreen.copy(alpha = 0.15f), contentColor = SuccessGreen) {
                                        Text("Assigned", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Assigned Adaptive Quizzes (${quizzes.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (quizzes.isEmpty()) {
                        item {
                            Text(
                                text = "No quizzes published yet. Generate an adaptive quiz from Course Materials to assign to students.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(quizzes) { quiz ->
                            val questions = JsonParserHelper.parseQuizQuestions(quiz.questionsJson)
                            val quizAttempts = attempts.filter { it.quizId == quiz.id }
                            val avgScore = if (quizAttempts.isNotEmpty()) {
                                quizAttempts.map { it.score.toDouble() / it.totalQuestions * 100 }.average().toInt()
                            } else null

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                .clip(CircleShape)
                                                .background(AccentIndigo.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Quiz, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(20.dp))
                                            }
                                            Column {
                                                Text(quiz.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                                Text(
                                                    text = "${questions.size} Questions • Difficulty: ${quiz.difficulty}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Badge(containerColor = SuccessGreen.copy(alpha = 0.15f), contentColor = SuccessGreen) {
                                            Text("Live", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }

                                    if (avgScore != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Class Average: $avgScore% (${quizAttempts.size} attempts)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = PrimaryBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Analytics & Roster Tab
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Class Performance Export",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Export student scores, test attempts, and concept failure rates as CSV.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { showExportCsvDialog = true },
                                    modifier = Modifier.testTag("export_csv_roster_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export CSV")
                                }
                            }
                        }
                    }

                    item {
                        ConceptWeaknessTracker(stats = weaknessStats)
                    }

                    // Enrolled Students Roster
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enrolled Students Roster (${enrollments.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (enrollments.isEmpty()) {
                        item {
                            Text(
                                text = "No students enrolled yet. Share Join Code '${classItem.code}' with your class.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(enrollments) { enr ->
                            val studentAttempts = attempts.filter { it.studentId == enr.studentId }
                            val totalAttempts = studentAttempts.size
                            val lastAttempt = studentAttempts.firstOrNull()

                            Card(
                                modifier = Modifier.fillMaxWidth(),
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
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryBlue.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.People, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                        }
                                        Column {
                                            Text(enr.studentName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(
                                                text = if (lastAttempt != null) {
                                                    "Completed ${totalAttempts} quiz(zes) • Last Score: ${lastAttempt.score}/${lastAttempt.totalQuestions}"
                                                } else {
                                                    "Enrolled • No quizzes attempted yet"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Add Material Dialog
    if (showAddMaterialDialog) {
        AddMaterialDialog(
            classCode = classItem.code,
            onDismiss = { showAddMaterialDialog = false },
            onConfirm = { title, topic, content ->
                onUploadMaterial(title, topic, content)
                showAddMaterialDialog = false
            }
        )
    }

    // Difficulty Picker Dialog for Quiz Generation
    showDifficultyPickerForMaterial?.let { mat ->
        DifficultyPickerDialog(
            materialTitle = mat.title,
            onDismiss = { showDifficultyPickerForMaterial = null },
            onSelectDifficulty = { diff ->
                onPrepareGeneration(mat, GenerationMode.QUIZ, diff)
                showDifficultyPickerForMaterial = null
            }
        )
    }

    // RAG Generation Overlay / Modal
    if (generationMode != GenerationMode.NONE) {
        GenerationPreviewModal(
            mode = generationMode,
            isGenerating = isGenerating,
            flashcards = generatedFlashcards,
            quiz = generatedQuiz,
            chunks = groundedChunks,
            onPublish = onPublishAndAssign,
            onDismiss = onDismissGeneration
        )
    }

    // Export CSV Dialog
    if (showExportCsvDialog) {
        ExportCsvDialog(
            classItem = classItem,
            enrollments = enrollments,
            materials = materials,
            quizzes = quizzes,
            attempts = attempts,
            weaknessStats = weaknessStats,
            metrics = metrics,
            onDismiss = { showExportCsvDialog = false }
        )
    }
}

@Composable
fun MaterialCard(
    material: MaterialEntity,
    onGenerateFlashcards: () -> Unit,
    onGenerateQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = material.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Topic: ${material.topic} • ${material.fileType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue
                    )
                }
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text("Grounded Source", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = material.content.take(160) + if (material.content.length > 160) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons for Generation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onGenerateFlashcards,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("generate_flashcards_btn_${material.id}"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Study Deck", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onGenerateQuiz,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("generate_quiz_btn_${material.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adaptive Quiz", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun EmptyMaterialsCard(onAddClicked: () -> Unit) {
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
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No course materials uploaded",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Upload a lecture syllabus or textbook notes to enable RAG flashcard and quiz generation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAddClicked,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Add Course Material")
            }
        }
    }
}

@Composable
fun AddMaterialDialog(
    classCode: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, topic: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    // Quick fill presets
    val presetOptions = listOf(
        "Physics: Thermodynamics & Entropy" to """
            # Thermodynamics & Heat Transfer
            ## 1. Zeroth Law
            If two thermodynamic systems are each in thermal equilibrium with a third, then they are in thermal equilibrium with each other. This defines the empirical temperature scale.
            ## 2. First Law (Energy Conservation)
            The change in internal energy of a closed system is equal to the heat added to the system minus the work done by the system: ΔU = Q - W.
            ## 3. Second Law (Entropy)
            In an isolated system, natural processes spontaneously evolve toward states of maximum entropy. Heat cannot spontaneously flow from a colder body to a hotter body without external work.
        """.trimIndent(),
        "Chemistry: Molecular Orbital Theory" to """
            # Molecular Orbital Theory
            ## 1. Linear Combination of Atomic Orbitals (LCAO)
            Atomic orbitals combine constructively to form bonding molecular orbitals (lower energy) and destructively to form anti-bonding molecular orbitals (higher energy, marked with *).
            ## 2. Bond Order
            Bond order is defined as (Number of bonding electrons - Number of anti-bonding electrons) / 2. A bond order of 0 indicates that a stable molecule will not form.
        """.trimIndent()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Upload / Assign Course Material",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Upload textbook notes, syllabus, or lecture excerpts for Class $classCode:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Preset Fill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetOptions.forEach { (presetName, presetContent) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                title = presetName
                                topic = presetName.substringBefore(":")
                                content = presetContent
                            },
                            label = { Text(presetName.substringBefore(":"), fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Material Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("material_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Subject Topic Tag") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Lecture / Textbook Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("material_content_input"),
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onConfirm(title, topic.ifEmpty { "General" }, content)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                modifier = Modifier.testTag("submit_material_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save Material")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DifficultyPickerDialog(
    materialTitle: String,
    onDismiss: () -> Unit,
    onSelectDifficulty: (String) -> Unit
) {
    val difficulties = listOf("Easy", "Medium", "Hard")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Adaptive Quiz Difficulty",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Generating quiz for '$materialTitle'. The RAG engine will calibrate question complexity accordingly:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                difficulties.forEach { diff ->
                    OutlinedButton(
                        onClick = { onSelectDifficulty(diff) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_diff_$diff"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "$diff Difficulty",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GenerationPreviewModal(
    mode: GenerationMode,
    isGenerating: Boolean,
    flashcards: List<FlashcardItem>?,
    quiz: List<QuizQuestion>?,
    chunks: List<TextChunk>,
    onPublish: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (mode == GenerationMode.FLASHCARDS) "Study Deck Studio" else "Adaptive Quiz Studio",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryBlue)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Running RAG Context Grounding...",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Extracting text chunks & generating structured assessment items via Gemini...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Chunks viewer
                    if (chunks.isNotEmpty()) {
                        RAGChunksViewer(chunks = chunks)
                    }

                    // Content preview
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (mode == GenerationMode.FLASHCARDS && flashcards != null) {
                            item {
                                Text(
                                    text = "Generated Flashcards (${flashcards.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            items(flashcards) { card ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Q: ${card.front}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("A: ${card.back}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else if (mode == GenerationMode.QUIZ && quiz != null) {
                            item {
                                Text(
                                    text = "Generated Quiz Questions (${quiz.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            items(quiz) { q ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Q${q.id}: ${q.question}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Correct: ${q.correctAnswer}", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                                        Text("Explanation: ${q.explanation}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Tag: ${q.topicTag}", style = MaterialTheme.typography.labelSmall, color = PrimaryBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isGenerating && (flashcards != null || quiz != null)) {
                Button(
                    onClick = onPublish,
                    modifier = Modifier.testTag("publish_and_assign_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Publish & Assign to Class")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ExportCsvDialog(
    classItem: ClassEntity,
    enrollments: List<EnrollmentEntity>,
    materials: List<MaterialEntity> = emptyList(),
    quizzes: List<QuizEntity>,
    attempts: List<QuizAttemptEntity>,
    weaknessStats: List<TopicWeaknessStat>,
    metrics: List<QuestionMetricEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val csvContent = remember(classItem, enrollments, materials, quizzes, attempts, weaknessStats, metrics) {
        CsvReportGenerator.generateClassPerformanceCsv(
            classItem = classItem,
            enrollments = enrollments,
            quizzes = quizzes,
            attempts = attempts,
            weaknessStats = weaknessStats,
            materials = materials,
            metrics = metrics
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Export Performance CSV",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "EduPulse_${classItem.code}_Report.csv",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlue
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Report generated for ${classItem.title} (Code: ${classItem.code}). Includes cohort averages, student attempts, topic failure rates, and course curriculum specs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Badge(containerColor = PrimaryBlue.copy(alpha = 0.12f), contentColor = PrimaryBlue) {
                        Text("${enrollments.size} Students", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Badge(containerColor = SuccessGreen.copy(alpha = 0.12f), contentColor = SuccessGreen) {
                        Text("${attempts.size} Submissions", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Badge(containerColor = WarningAmber.copy(alpha = 0.12f), contentColor = WarningAmber) {
                        Text("${weaknessStats.size} Topics", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(modifier = Modifier.padding(10.dp)) {
                        item {
                            Text(
                                text = csvContent,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            CsvReportGenerator.saveCsvToDeviceWithToast(context, classItem.title, classItem.code, csvContent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_csv_dialog_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save File", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            CsvReportGenerator.copyCsvToClipboard(context, classItem.code, csvContent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_csv_dialog_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Button(
                    onClick = {
                        CsvReportGenerator.shareCsvReport(context, classItem.title, classItem.code, csvContent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_csv_dialog_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export & Open in Excel / Sheets", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        dismissButton = {}
    )
}

