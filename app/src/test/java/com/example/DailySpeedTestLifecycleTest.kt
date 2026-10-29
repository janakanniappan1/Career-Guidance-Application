package com.example

import com.example.data.model.QuizQuestionDto
import com.example.domain.model.PracticeConstants
import com.example.domain.model.QuizAttempt
import com.example.domain.model.QuizMode
import com.example.domain.model.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.util.Random

/**
 * Automated regression test suite for Daily Speed Test Lifecycle:
 * 1. Daily test completion state evaluation & date/profile isolation.
 * 2. Idempotent XP award logic.
 * 3. Question selection prioritization and parsing resilience.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class DailySpeedTestLifecycleTest {

    private val testDispatcher = StandardTestDispatcher()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Mock storage simulation matching Supabase schema logic
    data class SimulatedDailyActivity(
        val userId: String,
        val activityDate: String,
        val dailyTestCompleted: Boolean,
        val createdAt: Long
    )

    data class SimulatedQuizAttempt(
        val userId: String,
        val topic: String,
        val score: Int,
        val totalQuestions: Int,
        val xpEarned: Int,
        val completedAt: Long,
        val completedDate: String
    )

    class SimulatedDailyTestEngine {
        val dailyActivities = mutableListOf<SimulatedDailyActivity>()
        val quizAttempts = mutableListOf<SimulatedQuizAttempt>()
        val userProfiles = mutableMapOf<String, Long>() // userId -> profileCreatedAt

        fun registerProfile(userId: String, createdAt: Long = System.currentTimeMillis()) {
            userProfiles[userId] = createdAt
        }

        fun deleteProfile(userId: String) {
            userProfiles.remove(userId)
            // Note: In Supabase, if profile is deleted without cascade, orphaned activities stay
        }

        fun isDailyTestCompletedToday(userId: String, todayDate: String = LocalDate.now(ZoneId.systemDefault()).toString()): Boolean {
            val profileCreatedAt = userProfiles[userId] ?: return false

            // 1. Authoritative check on dailyActivities
            val hasAuthoritative = dailyActivities.any {
                it.userId == userId &&
                it.activityDate == todayDate &&
                it.dailyTestCompleted &&
                it.createdAt >= profileCreatedAt // Profile lifecycle isolation
            }
            if (hasAuthoritative) return true

            // 2. Fallback check on quiz attempts
            return quizAttempts.any {
                it.userId == userId &&
                it.topic == PracticeConstants.TOPIC_DAILY_SPEED_TEST &&
                it.completedDate == todayDate &&
                it.completedAt >= profileCreatedAt // Profile lifecycle isolation
            }
        }

        fun recordAttempt(
            userId: String,
            score: Int,
            totalQuestions: Int,
            todayDate: String = LocalDate.now(ZoneId.systemDefault()).toString(),
            timestamp: Long = System.currentTimeMillis()
        ): Int {
            val alreadyCompleted = isDailyTestCompletedToday(userId, todayDate)
            val awardedXp = if (alreadyCompleted) {
                0 // Idempotency protection: do not award duplicate XP on the same day
            } else {
                QuizMode.DAILY_SPEED_TEST.calculateEarnedXp(score, totalQuestions)
            }

            quizAttempts.add(
                SimulatedQuizAttempt(
                    userId = userId,
                    topic = PracticeConstants.TOPIC_DAILY_SPEED_TEST,
                    score = score,
                    totalQuestions = totalQuestions,
                    xpEarned = awardedXp,
                    completedAt = timestamp,
                    completedDate = todayDate
                )
            )

            dailyActivities.add(
                SimulatedDailyActivity(
                    userId = userId,
                    activityDate = todayDate,
                    dailyTestCompleted = true,
                    createdAt = timestamp
                )
            )

            return awardedXp
        }
    }

    // --- TEST 1: Profile Recreation & User Isolation ---

    @Test
    fun testProfileRecreationResetsDailyTestCompletionState() = runTest(testDispatcher) {
        val engine = SimulatedDailyTestEngine()
        val userId = "janak.al24@bitsathy.ac.in"
        val today = LocalDate.now(ZoneId.systemDefault()).toString()

        // 1. User registers and completes Daily Speed Test
        val originalCreatedAt = 1000L
        engine.registerProfile(userId, createdAt = originalCreatedAt)

        assertFalse(engine.isDailyTestCompletedToday(userId, today))

        val xpEarned = engine.recordAttempt(userId, score = 5, totalQuestions = 5, todayDate = today, timestamp = 1500L)
        assertEquals(50, xpEarned)
        assertTrue(engine.isDailyTestCompletedToday(userId, today))

        // 2. User profile row is deleted from Supabase
        engine.deleteProfile(userId)
        assertFalse("Deleted profile should evaluate to uncompleted/false", engine.isDailyTestCompletedToday(userId, today))

        // 3. User logs in again, creating a NEW profile row with new created_at timestamp
        val newProfileCreatedAt = 2000L
        engine.registerProfile(userId, createdAt = newProfileCreatedAt)

        // The recreated profile MUST NOT see the old completion from timestamp 1500L
        assertFalse(
            "Recreated user MUST see Daily Speed Test as NOT completed today",
            engine.isDailyTestCompletedToday(userId, today)
        )

        // 4. User can now complete the new Daily Speed Test and earn XP
        val newXpEarned = engine.recordAttempt(userId, score = 5, totalQuestions = 5, todayDate = today, timestamp = 2500L)
        assertEquals(50, newXpEarned)
        assertTrue(engine.isDailyTestCompletedToday(userId, today))
    }

    @Test
    fun testUserIsolationBetweenTwoDifferentUsers() = runTest(testDispatcher) {
        val engine = SimulatedDailyTestEngine()
        val userA = "user_alice_101"
        val userB = "user_bob_202"
        val today = LocalDate.now(ZoneId.systemDefault()).toString()

        engine.registerProfile(userA)
        engine.registerProfile(userB)

        // User A completes the test
        engine.recordAttempt(userA, score = 4, totalQuestions = 5, todayDate = today)
        assertTrue(engine.isDailyTestCompletedToday(userA, today))

        // User B MUST NOT be affected
        assertFalse("User B must not see User A's completion state", engine.isDailyTestCompletedToday(userB, today))

        // User B completes test
        val bobXp = engine.recordAttempt(userB, score = 5, totalQuestions = 5, todayDate = today)
        assertEquals(50, bobXp)
        assertTrue(engine.isDailyTestCompletedToday(userB, today))
    }

    @Test
    fun testCalendarDateIsolation() = runTest(testDispatcher) {
        val engine = SimulatedDailyTestEngine()
        val userId = "test_user_calendar"
        val yesterday = "2026-09-09"
        val today = "2026-09-10"
        val tomorrow = "2026-09-11"

        engine.registerProfile(userId, createdAt = 1000L)

        // Complete test on yesterday
        engine.recordAttempt(userId, score = 5, totalQuestions = 5, todayDate = yesterday, timestamp = 1200L)
        assertTrue(engine.isDailyTestCompletedToday(userId, yesterday))
        assertFalse("Today should not be marked completed just because yesterday was", engine.isDailyTestCompletedToday(userId, today))

        // Complete test on today
        val todayXp = engine.recordAttempt(userId, score = 5, totalQuestions = 5, todayDate = today, timestamp = 2000L)
        assertEquals(50, todayXp)
        assertTrue(engine.isDailyTestCompletedToday(userId, today))
        assertFalse("Tomorrow should not be marked completed", engine.isDailyTestCompletedToday(userId, tomorrow))
    }

    // --- TEST 2: Idempotent XP Award Logic ---

    @Test
    fun testIdempotentXpAwardLogicOnSameDay() = runTest(testDispatcher) {
        val engine = SimulatedDailyTestEngine()
        val userId = "test_user_xp_idempotency"
        val today = LocalDate.now(ZoneId.systemDefault()).toString()

        engine.registerProfile(userId)

        // First attempt on today: earns full proportional XP (5/5 = 50 XP)
        val firstAttemptXp = engine.recordAttempt(userId, score = 5, totalQuestions = 5, todayDate = today)
        assertEquals(50, firstAttemptXp)

        // Second attempt on same day: awards 0 XP to prevent exploit
        val secondAttemptXp = engine.recordAttempt(userId, score = 5, totalQuestions = 5, todayDate = today)
        assertEquals(0, secondAttemptXp)

        // Third attempt with lower score: still 0 XP
        val thirdAttemptXp = engine.recordAttempt(userId, score = 3, totalQuestions = 5, todayDate = today)
        assertEquals(0, thirdAttemptXp)
    }

    @Test
    fun testProportionalXpCalculation() {
        val maxScore = QuizMode.DAILY_SPEED_TEST.calculateEarnedXp(correctAnswers = 5, totalQuestions = 5)
        assertEquals(50, maxScore)

        val fourScore = QuizMode.DAILY_SPEED_TEST.calculateEarnedXp(correctAnswers = 4, totalQuestions = 5)
        assertEquals(40, fourScore)

        val threeScore = QuizMode.DAILY_SPEED_TEST.calculateEarnedXp(correctAnswers = 3, totalQuestions = 5)
        assertEquals(30, threeScore)

        val zeroScore = QuizMode.DAILY_SPEED_TEST.calculateEarnedXp(correctAnswers = 0, totalQuestions = 5)
        assertEquals(0, zeroScore)
    }

    // --- TEST 3: Question Selection & Parsing Resilience ---

    @Test
    fun testQuestionSelectionPrioritizationAndFiveQuestionRequirement() {
        val questions = listOf(
            QuizQuestion("q1", "What is Big-O of binary search?", listOf("O(1)", "O(log n)", "O(n)", "O(n^2)"), 1, "Logarithmic", "software_dev", "DSA"),
            QuizQuestion("q2", "What is an interface?", listOf("Class", "Contract", "Variable", "Loop"), 1, "Contract", "software_dev", "OOP"),
            QuizQuestion("q3", "What is normalization?", listOf("Speed", "Eliminate redundancy", "Index", "Cache"), 1, "Redundancy", "data_analyst", "DB"),
            QuizQuestion("q4", "What is 15% of 80?", listOf("10", "12", "14", "16"), 1, "12", "all", "Aptitude"),
            QuizQuestion("q5", "Solve: 2x = 10", listOf("2", "3", "4", "5"), 3, "5", "all", "Aptitude"),
            QuizQuestion("q6", "What is an IP address?", listOf("Name", "Identifier", "Cable", "Router"), 1, "Identifier", "general", "Networking"),
            QuizQuestion("q7", "Explain recursion", listOf("Loop", "Function calling itself", "Stack", "Heap"), 1, "Self calling", "software_dev", "DSA")
        )

        // Daily Speed Test selection logic simulation
        val canonicalCareer = "software_dev"
        val userId = "janak.al24@bitsathy.ac.in"
        val todayDate = "2026-09-10"

        val careerMatching = questions.filter { it.careerId == canonicalCareer }
        val generalQuestions = questions.filter { it.careerId == "all" || it.careerId == "general" }
        val otherQuestions = questions - careerMatching.toSet() - generalQuestions.toSet()

        val candidatePool = (careerMatching + generalQuestions + otherQuestions).distinctBy { it.id }

        // Must have at least 5 questions
        assertTrue(candidatePool.size >= 5)

        val seed = (userId + "_" + todayDate).hashCode().toLong()
        val selected = candidatePool.shuffled(Random(seed)).take(5)

        assertEquals("Daily Speed Test must select exactly 5 questions", 5, selected.size)

        // Deterministic stability check: same user + date yields identical 5 questions
        val selectedAgain = candidatePool.shuffled(Random(seed)).take(5)
        assertEquals(selected.map { it.id }, selectedAgain.map { it.id })

        // Different user on same date gets an isolated pseudo-random selection
        val otherSeed = ("other_user_101" + "_" + todayDate).hashCode().toLong()
        val otherSelected = candidatePool.shuffled(Random(otherSeed)).take(5)
        assertEquals(5, otherSelected.size)
    }

    @Test
    fun testQuizQuestionDtoFlexibleOptionsParsing() {
        // 1. JSON Array of strings format
        val jsonArrayPayload = """
            {
                "id": "q_test_1",
                "question": "What is Kotlin?",
                "options": ["A language", "A fruit", "A database", "An OS"],
                "correct_index": 0,
                "explanation": "Modern programming language",
                "career_id": "mobile_dev",
                "topic": "daily_speed_test"
            }
        """.trimIndent()

        val decoded1 = json.decodeFromString<QuizQuestionDto>(jsonArrayPayload)
        val domain1 = decoded1.toDomain()
        assertEquals("q_test_1", domain1.id)
        assertEquals(4, domain1.options.size)
        assertEquals("A language", domain1.options[0])
        assertEquals(0, domain1.correctIndex)

        // 2. Comma-separated string format
        val stringPayload = """
            {
                "id": "q_test_2",
                "question": "What is RAM?",
                "options": "Primary Memory, Secondary Memory, Cache, Disk",
                "correct_index": 0,
                "explanation": "Random Access Memory",
                "career_id": "all",
                "topic": "hardware"
            }
        """.trimIndent()

        val decoded2 = json.decodeFromString<QuizQuestionDto>(stringPayload)
        val domain2 = decoded2.toDomain()
        assertEquals("q_test_2", domain2.id)
        assertEquals(4, domain2.options.size)
        assertEquals("Primary Memory", domain2.options[0])
        assertEquals("Secondary Memory", domain2.options[1])

        // 3. Null options or missing fields: gracefully handled with empty list
        val missingPayload = """
            {
                "id": "q_test_3",
                "question": "Empty options question"
            }
        """.trimIndent()

        val decoded3 = json.decodeFromString<QuizQuestionDto>(missingPayload)
        val domain3 = decoded3.toDomain()
        assertEquals("q_test_3", domain3.id)
        assertTrue(domain3.options.isEmpty())
        assertFalse("Empty options cannot be a valid playable question", domain3.options.size >= 2)
    }
}
