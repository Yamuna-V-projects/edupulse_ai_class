package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassEntity
import com.example.data.model.QuizAttemptEntity
import com.example.data.model.StudentBadge
import com.example.data.model.TopicWeaknessStat
import com.example.ui.components.ConceptWeaknessTracker
import com.example.ui.components.StatMetricCard
import com.example.util.CsvReportGenerator
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AppRole

@Composable
fun AnalyticsScreen(
    currentRole: AppRole,
    classes: List<ClassEntity>,
    selectedClass: ClassEntity?,
    classAttempts: List<QuizAttemptEntity>,
    weaknessStats: List<TopicWeaknessStat>,
    studentAttempts: List<QuizAttemptEntity>,
    badges: List<StudentBadge>,
    onExportCsv: ((ClassEntity) -> Unit)? = null,
    onSaveCsvLocally: ((ClassEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            // Dashboard Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentRole == AppRole.EDUCATOR) "Classroom Learning Diagnostics" else "My Personal Progress Tracker",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (currentRole == AppRole.EDUCATOR) {
                            "Aggregated performance analytics, topic weakness distributions, and AI remediation guidance across student cohorts."
                        } else {
                            "Personal score trajectories, topic competencies, and unlocked achievement badges."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (currentRole == AppRole.EDUCATOR) {
            // Educator View Analytics
            val totalAttempts = classAttempts.size
            val avgScore = if (totalAttempts > 0) {
                classAttempts.map { (it.score.toDouble() / it.totalQuestions) * 100 }.average().toInt()
            } else 0

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Cohort Average",
                        value = "$avgScore%",
                        subtitle = "Across all quizzes",
                        icon = Icons.Default.TrendingUp,
                        iconTint = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Total Submissions",
                        value = totalAttempts.toString(),
                        subtitle = "Recorded attempts",
                        icon = Icons.Default.School,
                        iconTint = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // CSV Export Card
            item {
                val context = LocalContext.current
                var activeExportClass by remember(classes, selectedClass) {
                    mutableStateOf(selectedClass ?: classes.firstOrNull())
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Export Performance CSV Report",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Download multi-section CSV with student attempts, score breakdowns, and topic failure diagnostics for external analysis in Google Sheets or Excel.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (classes.size > 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Select Class Cohort:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                classes.forEach { cls ->
                                    val isSelected = activeExportClass?.id == cls.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { activeExportClass = cls },
                                        label = { Text("${cls.title} (${cls.code})", style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                            selectedLabelColor = PrimaryBlue
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val targetClass = activeExportClass ?: classes.firstOrNull()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (targetClass != null) {
                                        if (onSaveCsvLocally != null) {
                                            onSaveCsvLocally(targetClass)
                                        } else {
                                            val csv = CsvReportGenerator.generateClassPerformanceCsv(
                                                classItem = targetClass,
                                                enrollments = emptyList(),
                                                quizzes = emptyList(),
                                                attempts = classAttempts,
                                                weaknessStats = weaknessStats
                                            )
                                            CsvReportGenerator.saveCsvToDeviceWithToast(context, targetClass.title, targetClass.code, csv)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("analytics_save_csv_btn"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save File", style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = {
                                    if (targetClass != null) {
                                        if (onExportCsv != null) {
                                            onExportCsv(targetClass)
                                        } else {
                                            val csv = CsvReportGenerator.generateClassPerformanceCsv(
                                                classItem = targetClass,
                                                enrollments = emptyList(),
                                                quizzes = emptyList(),
                                                attempts = classAttempts,
                                                weaknessStats = weaknessStats
                                            )
                                            CsvReportGenerator.shareCsvReport(context, targetClass.title, targetClass.code, csv)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("analytics_export_csv_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export & Share", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // AI Remediation Insight
            item {
                val highestWeakness = weaknessStats.firstOrNull()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Remediation Suggestion",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (highestWeakness != null && highestWeakness.failureRatePercent >= 40) {
                                    "Diagnostics reveal ${highestWeakness.failureRatePercent}% failure rate on '${highestWeakness.topicTag}'. Recommendation: Assign targeted Review Flashcards and conduct a 10-minute focus session before the midterm."
                                } else {
                                    "Cohort performance is stable. Continue publishing weekly adaptive quizzes to maintain mastery."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Concept Weakness Tracker
            item {
                ConceptWeaknessTracker(stats = weaknessStats)
            }

            // Recent attempts log
            item {
                Text(
                    text = "Recent Student Submissions (${classAttempts.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (classAttempts.isEmpty()) {
                item {
                    Text(
                        text = "No submissions logged yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(classAttempts.take(6)) { att ->
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
                            Column {
                                Text(att.studentName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = "Class: ${att.classCode} • Time: ${att.timeTakenSeconds}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val pct = ((att.score.toDouble() / att.totalQuestions) * 100).toInt()
                            Badge(
                                containerColor = if (pct >= 70) SuccessGreen.copy(alpha = 0.15f) else ErrorRose.copy(alpha = 0.15f),
                                contentColor = if (pct >= 70) SuccessGreen else ErrorRose
                            ) {
                                Text("${att.score}/${att.totalQuestions} ($pct%)", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        } else {
            // Student View Analytics
            val myTotal = studentAttempts.size
            val myAvg = if (myTotal > 0) {
                studentAttempts.map { (it.score.toDouble() / it.totalQuestions) * 100 }.average().toInt()
            } else 0

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "My Average Score",
                        value = "$myAvg%",
                        subtitle = "Overall test grade",
                        icon = Icons.Default.TrendingUp,
                        iconTint = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Quizzes Finished",
                        value = myTotal.toString(),
                        subtitle = "Personal tests",
                        icon = Icons.Default.EmojiEvents,
                        iconTint = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Earned Badges Showcase
            item {
                Text(
                    text = "My Unlocked Badges",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(badges) { badge ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (badge.earned) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (badge.earned) WarningAmber.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = if (badge.earned) WarningAmber else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(badge.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text(badge.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Badge(
                            containerColor = if (badge.earned) SuccessGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                            contentColor = if (badge.earned) SuccessGreen else Color.Gray
                        ) {
                            Text(if (badge.earned) "Earned" else "Locked", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            // Score History
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Personal Quiz History (${studentAttempts.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (studentAttempts.isEmpty()) {
                item {
                    Text(
                        text = "No quizzes completed yet. Go to your enrolled classes to start your first test!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(studentAttempts) { att ->
                    val pct = ((att.score.toDouble() / att.totalQuestions) * 100).toInt()
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
                            Column {
                                Text("Class: ${att.classCode}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Runtime: ${att.timeTakenSeconds}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Badge(
                                containerColor = if (pct >= 70) SuccessGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f),
                                contentColor = if (pct >= 70) SuccessGreen else WarningAmber
                            ) {
                                Text("${att.score}/${att.totalQuestions} ($pct%)", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
