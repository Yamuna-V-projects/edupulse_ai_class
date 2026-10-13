package com.example

import com.example.data.model.ClassEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.MaterialEntity
import com.example.data.model.QuestionMetricEntity
import com.example.data.model.QuizAttemptEntity
import com.example.data.model.QuizEntity
import com.example.data.model.TopicWeaknessStat
import com.example.util.CsvReportGenerator
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvReportGeneratorTest {

    @Test
    fun testGenerateClassPerformanceCsv_containsAllKeySections() {
        val classItem = ClassEntity(
            id = "c1",
            code = "BIO101",
            title = "AP Biology & Genetics",
            subject = "Biology",
            section = "Period 3",
            educatorId = "edu1",
            educatorName = "Dr. Aris Vance",
            description = "Cell biology, molecular genetics, and cellular respiration."
        )

        val enrollments = listOf(
            EnrollmentEntity("e1", "BIO101", "s1", "Maya Chen")
        )

        val materials = listOf(
            MaterialEntity("m1", "BIO101", "Cellular Respiration Notes", "Biochemistry", "Glycolysis and Krebs cycle.", "MARKDOWN")
        )

        val quizzes = listOf(
            QuizEntity(
                id = "q1",
                classCode = "BIO101",
                materialId = "m1",
                title = "Metabolic Pathways Quiz",
                difficulty = "Medium",
                questionsJson = """[{"id":1,"question":"Where does glycolysis occur?","options":["A","B"],"correctAnswer":"A","explanation":"Cytoplasm","topicTag":"Biochemistry"}]"""
            )
        )

        val attempts = listOf(
            QuizAttemptEntity(
                id = "att1",
                quizId = "q1",
                classCode = "BIO101",
                studentId = "s1",
                studentName = "Maya Chen",
                score = 1,
                totalQuestions = 1,
                timeTakenSeconds = 42,
                answersJson = "[]"
            )
        )

        val weaknessStats = listOf(
            TopicWeaknessStat(topicTag = "Biochemistry", totalAttempts = 1, incorrectAttempts = 0, failureRatePercent = 0)
        )

        val csv = CsvReportGenerator.generateClassPerformanceCsv(
            classItem = classItem,
            enrollments = enrollments,
            quizzes = quizzes,
            attempts = attempts,
            weaknessStats = weaknessStats,
            materials = materials
        )

        // Verify metadata header
        assertTrue("Contains Class Code", csv.contains("BIO101"))
        assertTrue("Contains Class Title", csv.contains("AP Biology & Genetics"))
        assertTrue("Contains Instructor", csv.contains("Dr. Aris Vance"))

        // Verify sections
        assertTrue("Contains Section 1 Header", csv.contains("SECTION 1: STUDENT QUIZ ATTEMPTS"))
        assertTrue("Contains Student Name Maya Chen", csv.contains("Maya Chen"))
        assertTrue("Contains 100.0%", csv.contains("100.0%"))

        assertTrue("Contains Section 2 Header", csv.contains("SECTION 2: CONCEPT WEAKNESS TRACKER"))
        assertTrue("Contains Topic Tag Biochemistry", csv.contains("Biochemistry"))

        assertTrue("Contains Section 3 Header", csv.contains("SECTION 3: ENROLLED STUDENT ROSTER"))

        assertTrue("Contains Section 4 Header", csv.contains("SECTION 4: COURSE MATERIALS"))
        assertTrue("Contains Material Title", csv.contains("Cellular Respiration Notes"))

        assertTrue("Contains Section 5 Header", csv.contains("SECTION 5: ASSIGNED QUIZZES"))
        assertTrue("Contains Quiz Title", csv.contains("Metabolic Pathways Quiz"))
    }
}
