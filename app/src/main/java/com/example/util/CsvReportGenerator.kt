package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.ClassEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.JsonParserHelper
import com.example.data.model.MaterialEntity
import com.example.data.model.QuestionMetricEntity
import com.example.data.model.QuizAttemptEntity
import com.example.data.model.QuizEntity
import com.example.data.model.TopicWeaknessStat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvReportGenerator {

    /**
     * Generates a fully structured, multi-section CSV performance and diagnostics report
     * ready for external spreadsheet analysis (Excel, Google Sheets, Python/Pandas).
     */
    fun generateClassPerformanceCsv(
        classItem: ClassEntity,
        enrollments: List<EnrollmentEntity>,
        quizzes: List<QuizEntity>,
        attempts: List<QuizAttemptEntity>,
        weaknessStats: List<TopicWeaknessStat>,
        materials: List<MaterialEntity> = emptyList(),
        metrics: List<QuestionMetricEntity> = emptyList()
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateFileFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val sb = StringBuilder()

        // Cohort high-level metrics calculation
        val totalAttempts = attempts.size
        val avgCohortScore = if (totalAttempts > 0) {
            val sum = attempts.sumOf { (it.score.toDouble() / it.totalQuestions.coerceAtLeast(1)) * 100 }
            sum / totalAttempts
        } else 0.0

        val passCount = attempts.count {
            if (it.totalQuestions > 0) (it.score.toDouble() / it.totalQuestions) >= 0.7 else false
        }
        val passRate = if (totalAttempts > 0) (passCount.toDouble() / totalAttempts) * 100 else 0.0
        val highestScore = attempts.maxOfOrNull {
            if (it.totalQuestions > 0) ((it.score.toDouble() / it.totalQuestions) * 100).toInt() else 0
        } ?: 0

        // SECTION 0: METADATA & EXECUTIVE COHORT SUMMARY
        sb.appendLine("# ==============================================================================")
        sb.appendLine("# EDUPULSE AI - CLASS PERFORMANCE & DIAGNOSTICS REPORT")
        sb.appendLine("# ==============================================================================")
        sb.appendLine("Metric Name,Metric Value")
        sb.appendLine("Class Title,${escapeCsv(classItem.title)}")
        sb.appendLine("Class Join Code,${escapeCsv(classItem.code)}")
        sb.appendLine("Subject / Field,${escapeCsv(classItem.subject)}")
        sb.appendLine("Section / Cohort,${escapeCsv(classItem.section.ifEmpty { "General" })}")
        sb.appendLine("Instructor / Educator,${escapeCsv(classItem.educatorName)}")
        sb.appendLine("Enrolled Students,${enrollments.size}")
        sb.appendLine("Published Course Materials,${materials.size}")
        sb.appendLine("Assigned Quizzes,${quizzes.size}")
        sb.appendLine("Total Quiz Submissions,${attempts.size}")
        sb.appendLine("Cohort Mean Score,${String.format(Locale.US, "%.1f", avgCohortScore)}%")
        sb.appendLine("Pass Rate (>=70%),${String.format(Locale.US, "%.1f", passRate)}%")
        sb.appendLine("Highest Score Achieved,$highestScore%")
        sb.appendLine("Topics Monitored,${weaknessStats.size}")
        sb.appendLine("Report Generated Date,$dateFileFormat")
        sb.appendLine()

        // SECTION 1: DETAILED STUDENT QUIZ ATTEMPTS & SCORE METRICS
        sb.appendLine("# ==============================================================================")
        sb.appendLine("# SECTION 1: STUDENT QUIZ ATTEMPTS & ASSESSMENT LOGS")
        sb.appendLine("# ==============================================================================")
        sb.appendLine("Student Name,Student ID,Class Code,Quiz ID,Quiz Title,Difficulty,Score,Total Questions,Percentage (%),Duration (sec),Submission Timestamp")
        if (attempts.isEmpty()) {
            sb.appendLine("No quiz attempts recorded yet,,,,,,,,,,")
        } else {
            for (att in attempts) {
                val matchedQuiz = quizzes.find { it.id == att.quizId }
                val quizTitle = matchedQuiz?.title ?: "Assessment ${att.quizId}"
                val quizDiff = matchedQuiz?.difficulty ?: "Adaptive"
                val pct = if (att.totalQuestions > 0) {
                    String.format(Locale.US, "%.1f", (att.score.toDouble() / att.totalQuestions) * 100)
                } else "0.0"
                val dateStr = dateFormat.format(Date(att.completedAt))

                sb.appendLine(
                    "${escapeCsv(att.studentName)},${escapeCsv(att.studentId)},${escapeCsv(att.classCode)}," +
                    "${escapeCsv(att.quizId)},${escapeCsv(quizTitle)},${escapeCsv(quizDiff)},${att.score}," +
                    "${att.totalQuestions},${pct}%,${att.timeTakenSeconds},${escapeCsv(dateStr)}"
                )
            }
        }
        sb.appendLine()

        // SECTION 2: TOPIC CONCEPT WEAKNESS TRACKER & DIAGNOSTICS
        sb.appendLine("# ==============================================================================")
        sb.appendLine("# SECTION 2: CONCEPT WEAKNESS TRACKER & TOPIC FAILURE RATES")
        sb.appendLine("# ==============================================================================")
        sb.appendLine("Topic / Concept Tag,Total Attempts Evaluated,Incorrect Answers,Failure Rate (%),Proficiency Status,Recommended Educator Action")
        if (weaknessStats.isEmpty()) {
            sb.appendLine("No topic metrics recorded yet,,,,,")
        } else {
            for (stat in weaknessStats) {
                val status = when {
                    stat.failureRatePercent >= 50 -> "High Difficulty / Critical Attention"
                    stat.failureRatePercent >= 25 -> "Moderate Difficulty / Needs Practice"
                    else -> "Proficient / Mastered"
                }
                val action = when {
                    stat.failureRatePercent >= 50 -> "Deploy targeted review flashcards and hold remediation session"
                    stat.failureRatePercent >= 25 -> "Assign adaptive practice quiz before major milestone"
                    else -> "Core objectives met. Maintain weekly mastery check."
                }
                sb.appendLine(
                    "${escapeCsv(stat.topicTag)},${stat.totalAttempts},${stat.incorrectAttempts}," +
                    "${stat.failureRatePercent}%,${escapeCsv(status)},${escapeCsv(action)}"
                )
            }
        }
        sb.appendLine()

        // SECTION 3: ENROLLED STUDENT ROSTER SUMMARY & PERFORMANCE TIERS
        sb.appendLine("# ==============================================================================")
        sb.appendLine("# SECTION 3: ENROLLED STUDENT ROSTER & INDIVIDUAL PROGRESS")
        sb.appendLine("# ==============================================================================")
        sb.appendLine("Student Name,Student ID,Class Code,Quizzes Completed,Average Score (%),Best Score (%),Status Tier,Enrollment Date")
        if (enrollments.isEmpty()) {
            sb.appendLine("No students enrolled yet,,,,,,,")
        } else {
            for (enr in enrollments) {
                val studentAttempts = attempts.filter { it.studentId == enr.studentId }
                val count = studentAttempts.size
                val (avgStr, bestStr, tier) = if (count > 0) {
                    val pcts = studentAttempts.map {
                        if (it.totalQuestions > 0) (it.score.toDouble() / it.totalQuestions) * 100 else 0.0
                    }
                    val avg = pcts.average()
                    val best = pcts.maxOrNull() ?: 0.0
                    val tierStatus = when {
                        avg >= 85.0 -> "Exemplary (Grade A)"
                        avg >= 70.0 -> "Proficient (Grade B)"
                        avg >= 55.0 -> "Developing (Grade C)"
                        else -> "Requires Remediation"
                    }
                    Triple(
                        String.format(Locale.US, "%.1f%%", avg),
                        String.format(Locale.US, "%.1f%%", best),
                        tierStatus
                    )
                } else {
                    Triple("N/A", "N/A", "Pending First Submission")
                }
                val joinDate = dateFormat.format(Date(enr.joinedAt))

                sb.appendLine(
                    "${escapeCsv(enr.studentName)},${escapeCsv(enr.studentId)},${escapeCsv(enr.classCode)}," +
                    "$count,$avgStr,$bestStr,${escapeCsv(tier)},${escapeCsv(joinDate)}"
                )
            }
        }
        sb.appendLine()

        // SECTION 4: PUBLISHED COURSE MATERIALS & RAG KNOWLEDGE BASE
        if (materials.isNotEmpty()) {
            sb.appendLine("# ==============================================================================")
            sb.appendLine("# SECTION 4: COURSE MATERIALS & RAG RETRIEVAL SOURCES")
            sb.appendLine("# ==============================================================================")
            sb.appendLine("Material ID,Title,Topic Domain,Format,Character Count,Estimated Word Count,Uploaded At")
            for (mat in materials) {
                val words = mat.content.split("\\s+".toRegex()).size
                val upDate = dateFormat.format(Date(mat.uploadedAt))
                sb.appendLine(
                    "${escapeCsv(mat.id)},${escapeCsv(mat.title)},${escapeCsv(mat.topic)}," +
                    "${escapeCsv(mat.fileType)},${mat.content.length},$words,${escapeCsv(upDate)}"
                )
            }
            sb.appendLine()
        }

        // SECTION 5: ASSIGNED QUIZZES CATALOG
        if (quizzes.isNotEmpty()) {
            sb.appendLine("# ==============================================================================")
            sb.appendLine("# SECTION 5: ASSIGNED QUIZZES SPECIFICATION")
            sb.appendLine("# ==============================================================================")
            sb.appendLine("Quiz ID,Title,Difficulty Level,Questions Count,Assignment Status,Created At")
            for (qz in quizzes) {
                val qCount = JsonParserHelper.parseQuizQuestions(qz.questionsJson).size
                val qDate = dateFormat.format(Date(qz.createdAt))
                val status = if (qz.isAssigned) "Assigned to Students" else "Draft"
                sb.appendLine(
                    "${escapeCsv(qz.id)},${escapeCsv(qz.title)},${escapeCsv(qz.difficulty)}," +
                    "$qCount,${escapeCsv(status)},${escapeCsv(qDate)}"
                )
            }
            sb.appendLine()
        }

        return sb.toString()
    }

    /**
     * Saves the CSV report content to device app storage and returns the generated File object.
     */
    fun saveCsvToFile(context: Context, classCode: String, csvContent: String): File? {
        return try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "EduPulse_${classCode}_Performance_Report_$dateStr.csv"

            val reportsDir = File(context.getExternalFilesDir(null), "EduPulse_Reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            val targetFile = File(reportsDir, filename)
            FileOutputStream(targetFile).use { out ->
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            // Also keep a copy in cache directory for instant FileProvider sharing
            val cacheFile = File(context.cacheDir, filename)
            FileOutputStream(cacheFile).use { out ->
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves the report and shows a user-facing Toast confirmation with the file name.
     */
    fun saveCsvToDeviceWithToast(context: Context, classTitle: String, classCode: String, csvContent: String): File? {
        val file = saveCsvToFile(context, classCode, csvContent)
        if (file != null) {
            Toast.makeText(
                context,
                "Report saved: ${file.name} (${String.format(Locale.US, "%.1f KB", file.length() / 1024.0)})",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(context, "Failed to save CSV file to device storage.", Toast.LENGTH_SHORT).show()
        }
        return file
    }

    /**
     * Exports and shares the CSV performance report using FileProvider (for apps like Excel,
     * Google Sheets, Gmail, Drive) with a graceful fallback to text sharing.
     */
    fun shareCsvReport(context: Context, classTitle: String, classCode: String, csvContent: String) {
        val savedFile = saveCsvToFile(context, classCode, csvContent)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "EduPulse Performance Report: $classTitle ($classCode)")
            putExtra(Intent.EXTRA_TEXT, csvContent)

            if (savedFile != null) {
                try {
                    val contentUri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        savedFile
                    )
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    // Fall back to plain text payload if FileProvider resolution fails
                    e.printStackTrace()
                }
            }
        }

        val chooser = Intent.createChooser(intent, "Export Class Performance CSV Report ($classCode)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Copies the full CSV dataset to system clipboard.
     */
    fun copyCsvToClipboard(context: Context, classCode: String, csvContent: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("EduPulse Report $classCode", csvContent)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Class performance CSV report copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
