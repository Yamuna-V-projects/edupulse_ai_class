package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizEntity
import com.example.data.model.QuizQuestion
import com.example.data.model.StudentAnswerRecord
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun QuizScreen(
    quiz: QuizEntity,
    questions: List<QuizQuestion>,
    currentIndex: Int,
    selectedOption: String?,
    isAnswerSubmitted: Boolean,
    score: Int,
    recordedAnswers: List<StudentAnswerRecord>,
    isQuizFinished: Boolean,
    onSelectOption: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalQuestions = questions.size
    val currentQuestion = questions.getOrNull(currentIndex)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Quiz Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose, modifier = Modifier.testTag("close_quiz_button")) {
                Icon(Icons.Default.Close, contentDescription = "Close Quiz")
            }
            Text(
                text = quiz.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Badge(containerColor = PrimaryBlue.copy(alpha = 0.15f), contentColor = PrimaryBlue) {
                Text(
                    text = if (!isQuizFinished) "${currentIndex + 1} / $totalQuestions" else "Finished",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = {
                if (isQuizFinished) 1f
                else if (totalQuestions > 0) (currentIndex + 1).toFloat() / totalQuestions
                else 0f
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = PrimaryBlue,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isQuizFinished) {
            // Quiz Results Screen
            QuizResultsView(
                score = score,
                totalQuestions = totalQuestions,
                recordedAnswers = recordedAnswers,
                questions = questions,
                onClose = onClose
            )
        } else if (currentQuestion != null) {
            // Active Question View
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Question Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Topic: ${currentQuestion.topicTag}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Badge(containerColor = WarningAmber.copy(alpha = 0.2f), contentColor = WarningAmber) {
                                    Text("${quiz.difficulty} Level", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = currentQuestion.question,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 28.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Options List
                item {
                    Text(
                        text = "Choose the best answer:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(currentQuestion.options) { option ->
                    val isSelected = selectedOption == option
                    val isCorrectOption = option.trim() == currentQuestion.correctAnswer.trim()

                    val (containerColor, borderColor, textColor) = when {
                        isAnswerSubmitted && isCorrectOption -> Triple(
                            SuccessGreen.copy(alpha = 0.15f),
                            SuccessGreen,
                            SuccessGreen
                        )
                        isAnswerSubmitted && isSelected && !isCorrectOption -> Triple(
                            ErrorRose.copy(alpha = 0.15f),
                            ErrorRose,
                            ErrorRose
                        )
                        isSelected -> Triple(
                            PrimaryBlue.copy(alpha = 0.12f),
                            PrimaryBlue,
                            PrimaryBlue
                        )
                        else -> Triple(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isAnswerSubmitted) { onSelectOption(option) }
                            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                            .testTag("quiz_option_${option.take(3)}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )

                            if (isAnswerSubmitted) {
                                if (isCorrectOption) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = SuccessGreen)
                                } else if (isSelected) {
                                    Icon(Icons.Default.HighlightOff, contentDescription = "Incorrect", tint = ErrorRose)
                                }
                            }
                        }
                    }
                }

                // Immediate Grounded Explanation Card
                if (isAnswerSubmitted) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "RAG Grounded Explanation",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentQuestion.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            if (!isAnswerSubmitted) {
                Button(
                    onClick = onSubmitAnswer,
                    enabled = selectedOption != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_answer_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Check Answer", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            } else {
                Button(
                    onClick = onNextQuestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("next_question_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentIndex < totalQuestions - 1) PrimaryBlue else SuccessGreen
                    )
                ) {
                    Text(
                        text = if (currentIndex < totalQuestions - 1) "Next Question" else "Complete Quiz",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun QuizResultsView(
    score: Int,
    totalQuestions: Int,
    recordedAnswers: List<StudentAnswerRecord>,
    questions: List<QuizQuestion>,
    onClose: () -> Unit
) {
    val percentage = if (totalQuestions > 0) ((score.toFloat() / totalQuestions) * 100).toInt() else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        if (percentage >= 70) SuccessGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = if (percentage >= 70) SuccessGreen else WarningAmber,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (percentage >= 80) "Outstanding Mastery!" else if (percentage >= 60) "Good Work!" else "Review Needed",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Final Score: $score / $totalQuestions ($percentage%)",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryBlue
                )
            )
            Text(
                text = "Results and question metrics recorded to SQLite database.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Breakdown of missed questions
        val missed = recordedAnswers.filter { !it.isCorrect }
        if (missed.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Questions Needing Review (${missed.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            items(missed) { ans ->
                val q = questions.find { it.id == ans.questionId }
                if (q != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Topic: ${ans.topicTag}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = ErrorRose
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = q.question,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Correct Answer: ${q.correctAnswer}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SuccessGreen
                            )
                            Text(
                                text = "Explanation: ${q.explanation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("quiz_finish_return_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Return to Classroom Hub")
            }
        }
    }
}
