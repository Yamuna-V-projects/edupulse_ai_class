package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ClassEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.FlashcardDeckEntity
import com.example.data.model.JsonParserHelper
import com.example.data.model.MaterialEntity
import com.example.data.model.QuestionMetricEntity
import com.example.data.model.QuizAttemptEntity
import com.example.data.model.QuizEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Database(
    entities = [
        UserEntity::class,
        ClassEntity::class,
        EnrollmentEntity::class,
        MaterialEntity::class,
        FlashcardDeckEntity::class,
        QuizEntity::class,
        QuizAttemptEntity::class,
        QuestionMetricEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class EduPulseDatabase : RoomDatabase() {

    abstract fun eduPulseDao(): EduPulseDao

    companion object {
        @Volatile
        private var INSTANCE: EduPulseDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): EduPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EduPulseDatabase::class.java,
                    "edupulse_database.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedInitialData(database.eduPulseDao())
                    }
                }
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedInitialData(database.eduPulseDao())
                    }
                }
            }
        }

        suspend fun seedInitialData(dao: EduPulseDao) {
            // Seed Users
            val educator = UserEntity(
                id = "edu_vance",
                name = "Dr. Aris Vance",
                email = "aris.vance@university.edu",
                role = "EDUCATOR",
                avatarColor = 0xFF2563EB
            )
            val student1 = UserEntity(
                id = "stu_maya",
                name = "Maya Chen",
                email = "maya.chen@student.edu",
                role = "STUDENT",
                avatarColor = 0xFF0D9488
            )
            val student2 = UserEntity(
                id = "stu_leo",
                name = "Leo Patel",
                email = "leo.p@student.edu",
                role = "STUDENT",
                avatarColor = 0xFFF59E0B
            )
            val student3 = UserEntity(
                id = "stu_sara",
                name = "Sara Ortiz",
                email = "sara.o@student.edu",
                role = "STUDENT",
                avatarColor = 0xFF8B5CF6
            )

            dao.insertUser(educator)
            dao.insertUser(student1)
            dao.insertUser(student2)
            dao.insertUser(student3)

            // Seed Class 1: Physics 101
            val class1 = ClassEntity(
                id = "class_phy101",
                code = "XJ79K2",
                title = "Physics 101: Mechanics & Dynamics",
                subject = "Physics",
                section = "Sec A",
                educatorId = educator.id,
                educatorName = educator.name,
                description = "Fundamental principles of classical mechanics, Newton's laws of motion, inertial reference frames, friction, and kinetic momentum."
            )
            dao.insertClass(class1)

            // Enrollments for Class 1
            dao.insertEnrollment(EnrollmentEntity("enr_1", class1.code, student1.id, student1.name))
            dao.insertEnrollment(EnrollmentEntity("enr_2", class1.code, student2.id, student2.name))
            dao.insertEnrollment(EnrollmentEntity("enr_3", class1.code, student3.id, student3.name))

            // Seed Material for Class 1
            val physicsContent = """
                # Lecture 3: Newton's Laws of Motion & Dynamics

                ## 1. Newton's First Law (Law of Inertia)
                An object continues in its state of rest, or of uniform motion in a straight line, unless acted upon by a net external force. An inertial reference frame is one in which Newton's first law holds true without introducing fictitious forces.

                ## 2. Newton's Second Law (Force and Acceleration)
                The acceleration of an object is directly proportional to the net force acting upon it and inversely proportional to its mass: F_net = m * a. In terms of momentum (p = m*v), force is the time rate of change of momentum (F = dp/dt).

                ## 3. Newton's Third Law (Action & Reaction Pairs)
                When one body exerts a force on a second body, the second body simultaneously exerts a force equal in magnitude and opposite in direction on the first body. Crucially, action-reaction pairs never act on the same object! They act on two different interacting bodies, meaning they never cancel each other out in a single body's free-body diagram.

                ## 4. Friction & Normal Force
                Static friction prevents motion up to a maximum threshold f_s_max = μ_s * N. Kinetic friction f_k = μ_k * N opposes relative motion once sliding begins. The coefficient of kinetic friction is typically lower than static friction.

                ## 5. Work and Energy Relations
                The work-energy theorem states that the net work done on an object equals the change in its kinetic energy: W_net = ΔK = 1/2 m(v_f^2 - v_i^2).
            """.trimIndent()

            val material1 = MaterialEntity(
                id = "mat_phy_01",
                classCode = class1.code,
                title = "Unit 3: Newton's Laws & Force Interactions",
                topic = "Classical Mechanics",
                content = physicsContent,
                fileType = "MARKDOWN"
            )
            dao.insertMaterial(material1)

            // Seed Flashcards for Class 1
            val flashcardsJson = JSONArray().apply {
                put(JSONObject().apply {
                    put("front", "What defines an Inertial Reference Frame?")
                    put("back", "A reference frame where Newton's First Law holds true without needing fictitious forces (i.e. zero acceleration).")
                })
                put(JSONObject().apply {
                    put("front", "Why don't Action-Reaction pairs cancel out?")
                    put("back", "Because they act on two DIFFERENT objects, not on the same object's free-body diagram.")
                })
                put(JSONObject().apply {
                    put("front", "What is the relation between static and kinetic friction coefficients?")
                    put("back", "Static friction coefficient (μ_s) is generally higher than kinetic friction coefficient (μ_k).")
                })
                put(JSONObject().apply {
                    put("front", "State the general mathematical form of Newton's Second Law.")
                    put("back", "F_net = dp/dt (Force is the time rate of change of momentum, or m*a for constant mass).")
                })
                put(JSONObject().apply {
                    put("front", "What does the Work-Energy Theorem assert?")
                    put("back", "The net work done on an object equals the change in its kinetic energy (W_net = ΔK).")
                })
            }.toString()

            val deck1 = FlashcardDeckEntity(
                id = "deck_phy_01",
                classCode = class1.code,
                materialId = material1.id,
                title = "Mechanics Mastery Flashcards",
                cardsJson = flashcardsJson,
                isAssigned = true
            )
            dao.insertFlashcardDeck(deck1)

            // Seed Quiz for Class 1
            val quizQuestionsJson = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 1)
                    put("question", "A book rests on a table. What is the Newton's Third Law reaction force to the gravitational force of Earth pulling down on the book?")
                    put("options", JSONArray().apply {
                        put("A: The normal force exerted by the table upward on the book")
                        put("B: The gravitational force of the book pulling upward on the Earth")
                        put("C: The contact friction between book and table surface")
                        put("D: There is no reaction force because the book is stationary")
                    })
                    put("correct_answer", "B: The gravitational force of the book pulling upward on the Earth")
                    put("explanation", "Newton's Third Law reaction pairs must involve the same two interacting bodies (Earth and Book). The Earth pulls down on the book, so the reaction is the book pulling up on Earth.")
                    put("topic_tag", "Newton's Third Law")
                })
                put(JSONObject().apply {
                    put("id", 2)
                    put("question", "Why do passengers appear to lurch forward when a bus suddenly brakes?")
                    put("options", JSONArray().apply {
                        put("A: A mysterious forward force is created by braking")
                        put("B: Friction with the seat pushes them forward")
                        put("C: Their inertia keeps them moving at original velocity until a net force acts")
                        put("D: Air pressure inside the bus increases rapidly")
                    })
                    put("correct_answer", "C: Their inertia keeps them moving at original velocity until a net force acts")
                    put("explanation", "Newton's First Law (Law of Inertia) explains that an object maintains uniform velocity unless an external force acts to decelerate it.")
                    put("topic_tag", "Inertial Reference Frames")
                })
                put(JSONObject().apply {
                    put("id", 3)
                    put("question", "A 10 kg box requires 40 N of horizontal force to start sliding, but only 30 N to keep moving at constant velocity. What is μ_s (g = 10 m/s²)?")
                    put("options", JSONArray().apply {
                        put("A: 0.30")
                        put("B: 0.40")
                        put("C: 0.70")
                        put("D: 0.10")
                    })
                    put("correct_answer", "B: 0.40")
                    put("explanation", "Normal force N = m*g = 10*10 = 100 N. Maximum static friction is 40 N. Therefore μ_s = f_s_max / N = 40 / 100 = 0.40.")
                    put("topic_tag", "Frictional Forces")
                })
                put(JSONObject().apply {
                    put("id", 4)
                    put("question", "A horse pulls a cart. If the horse exerts a 500 N force on the cart, and the cart exerts a 500 N force back on the horse, how can the cart accelerate?")
                    put("options", JSONArray().apply {
                        put("A: Action occurs slightly before reaction in time")
                        put("B: The horse pulls harder than the cart pulls back")
                        put("C: The third law forces act on different bodies; cart acceleration depends on net force on the cart")
                        put("D: The wheels eliminate reaction force")
                    })
                    put("correct_answer", "C: The third law forces act on different bodies; cart acceleration depends on net force on the cart")
                    put("explanation", "Action-reaction pairs act on different objects. The cart accelerates because the forward force from the horse exceeds ground friction on the cart.")
                    put("topic_tag", "Newton's Third Law")
                })
                put(JSONObject().apply {
                    put("id", 5)
                    put("question", "In which frame of reference can fictitious (inertial) forces like centrifugal force appear?")
                    put("options", JSONArray().apply {
                        put("A: In all inertial frames")
                        put("B: Only in non-inertial (accelerating or rotating) frames")
                        put("C: Only in frames moving at light speed")
                        put("D: Only in isolated vacuum chambers")
                    })
                    put("correct_answer", "B: Only in non-inertial (accelerating or rotating) frames")
                    put("explanation", "Fictitious forces arise when applying Newton's laws inside a reference frame that is itself accelerating or rotating relative to an inertial frame.")
                    put("topic_tag", "Inertial Reference Frames")
                })
            }.toString()

            val quiz1 = QuizEntity(
                id = "quiz_phy_01",
                classCode = class1.code,
                materialId = material1.id,
                title = "Quiz 1: Dynamics & Frame Principles",
                difficulty = "Medium",
                questionsJson = quizQuestionsJson,
                isAssigned = true
            )
            dao.insertQuiz(quiz1)

            // Seed student quiz attempts showing concept weakness in "Newton's Third Law"
            // Student 2 (Leo): failed Q1 and Q4 (both Newton's Third Law)
            val leoAttempt = QuizAttemptEntity(
                id = "att_leo_01",
                quizId = quiz1.id,
                classCode = class1.code,
                studentId = student2.id,
                studentName = student2.name,
                score = 3,
                totalQuestions = 5,
                timeTakenSeconds = 145,
                answersJson = "[]"
            )
            dao.insertAttempt(leoAttempt)
            dao.insertMetrics(listOf(
                QuestionMetricEntity("qm_l1", leoAttempt.id, quiz1.id, class1.code, student2.id, 1, "Newton's Third Law", false),
                QuestionMetricEntity("qm_l2", leoAttempt.id, quiz1.id, class1.code, student2.id, 2, "Inertial Reference Frames", true),
                QuestionMetricEntity("qm_l3", leoAttempt.id, quiz1.id, class1.code, student2.id, 3, "Frictional Forces", true),
                QuestionMetricEntity("qm_l4", leoAttempt.id, quiz1.id, class1.code, student2.id, 4, "Newton's Third Law", false),
                QuestionMetricEntity("qm_l5", leoAttempt.id, quiz1.id, class1.code, student2.id, 5, "Inertial Reference Frames", true)
            ))

            // Student 3 (Sara): failed Q1 and Q5
            val saraAttempt = QuizAttemptEntity(
                id = "att_sara_01",
                quizId = quiz1.id,
                classCode = class1.code,
                studentId = student3.id,
                studentName = student3.name,
                score = 3,
                totalQuestions = 5,
                timeTakenSeconds = 180,
                answersJson = "[]"
            )
            dao.insertAttempt(saraAttempt)
            dao.insertMetrics(listOf(
                QuestionMetricEntity("qm_s1", saraAttempt.id, quiz1.id, class1.code, student3.id, 1, "Newton's Third Law", false),
                QuestionMetricEntity("qm_s2", saraAttempt.id, quiz1.id, class1.code, student3.id, 2, "Inertial Reference Frames", true),
                QuestionMetricEntity("qm_s3", saraAttempt.id, quiz1.id, class1.code, student3.id, 3, "Frictional Forces", true),
                QuestionMetricEntity("qm_s4", saraAttempt.id, quiz1.id, class1.code, student3.id, 4, "Newton's Third Law", false),
                QuestionMetricEntity("qm_s5", saraAttempt.id, quiz1.id, class1.code, student3.id, 5, "Inertial Reference Frames", false)
            ))

            // Seed Class 2: CS 106
            val class2 = ClassEntity(
                id = "class_cs106",
                code = "CS42B1",
                title = "CS 106: Data Structures & Algorithms",
                subject = "Computer Science",
                section = "Lab 2",
                educatorId = educator.id,
                educatorName = educator.name,
                description = "Binary trees, Graph search strategies (BFS/DFS), Time complexity asymptotic notation, and Priority Queues."
            )
            dao.insertClass(class2)
            dao.insertEnrollment(EnrollmentEntity("enr_cs_1", class2.code, student1.id, student1.name))

            val csContent = """
                # Topic 4: Tree & Graph Data Structures

                ## 1. Binary Search Trees (BST)
                A Binary Search Tree is a node-based binary tree data structure with the property that the left subtree of a node contains only keys lesser than the node's key, and the right subtree contains only keys greater. In a balanced BST, search, insert, and delete take O(log N) time. In degenerate cases (like inserting pre-sorted keys), it degrades to O(N).

                ## 2. Graph Traversal: BFS vs DFS
                Breadth-First Search (BFS) explores vertices neighbor-by-neighbor in layers using a FIFO Queue. It guarantees finding the shortest path on unweighted graphs.
                Depth-First Search (DFS) explores as deep as possible along each branch before backtracking, implemented using recursion or an explicit LIFO Stack.

                ## 3. Asymptotic Analysis
                Big-O describes an upper bound on growth rate. O(1) is constant, O(log N) logarithmic, O(N) linear, O(N log N) linearithmic (MergeSort/HeapSort), and O(N^2) quadratic.
            """.trimIndent()

            val material2 = MaterialEntity(
                id = "mat_cs_01",
                classCode = class2.code,
                title = "Module 4: Trees, Graphs & Complexity",
                topic = "Data Structures",
                content = csContent,
                fileType = "MARKDOWN"
            )
            dao.insertMaterial(material2)
        }
    }
}
