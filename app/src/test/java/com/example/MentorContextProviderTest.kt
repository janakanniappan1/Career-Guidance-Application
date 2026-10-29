package com.example

import com.example.data.repository.MockAssessmentRepository
import com.example.data.repository.MockAuthRepository
import com.example.data.repository.MockCareerRepository
import com.example.data.repository.MockPracticeRepository
import com.example.data.repository.MockRoadmapRepository
import com.example.domain.coordinator.CGAMentorContextProvider
import com.example.domain.model.CGAMentorContext
import com.example.domain.model.CareerRecommendation
import com.example.domain.model.ResumeAnalysis
import com.example.domain.model.StudentProfile
import com.example.domain.repository.PracticeRepository
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MentorContextProviderTest {

    // TEST 1: Complete context builds correctly
    @Test
    fun test1_completeContextBuildsCorrectly() = runTest {
        val authRepo = MockAuthRepository()
        authRepo.updateProfile(
            StudentProfile(
                id = "usr_student_demo",
                name = "Student",
                email = "student@college.edu",
                department = "Computer Science & Engineering",
                yearOfStudy = "3rd Year",
                careerInterest = "Artificial Intelligence & Data Science",
                currentStreak = 3,
                xp = 450,
                isAssessmentCompleted = true,
                recommendedCareerId = "aiml_engineer"
            )
        )
        val assessmentRepo = MockAssessmentRepository()
        val careerRepo = MockCareerRepository(assessmentRepo)
        val roadmapRepo = MockRoadmapRepository(authRepo)
        val practiceRepo = object : PracticeRepository by MockPracticeRepository() {
            override suspend fun getResumeAnalysis(): ResumeAnalysis {
                return ResumeAnalysis(
                    atsScore = 82,
                    matchedSkills = listOf("Python", "SQL"),
                    missingSkills = listOf("Docker")
                )
            }
        }

        val provider = CGAMentorContextProvider(
            authRepository = authRepo,
            careerRepository = careerRepo,
            roadmapRepository = roadmapRepo,
            practiceRepository = practiceRepo
        )

        val context = provider.assembleVerifiedContext()
        assertNotNull(context)
        assertNotNull(context.studentName)
        assertNotNull(context.recommendedCareer)
        assertNotNull(context.currentRoadmapStage)
        assertNotNull(context.roadmapProgress)
        assertNotNull(context.solvedPracticeCount)
        assertNotNull(context.totalPracticeCount)
        assertNotNull(context.currentStreak)
        assertNotNull(context.xp)
        assertNotNull(context.resumeAtsScore)
    }

    // TEST 2: Missing resume data remains null (never fabricated)
    @Test
    fun test2_missingResumeDataRemainsNull() = runTest {
        val authRepo = MockAuthRepository()
        val emptyResumePracticeRepo = object : PracticeRepository by MockPracticeRepository() {
            override suspend fun getResumeAnalysis(): ResumeAnalysis {
                return ResumeAnalysis(
                    atsScore = 0,
                    matchedSkills = emptyList(),
                    missingSkills = emptyList()
                )
            }
        }

        val provider = CGAMentorContextProvider(
            authRepository = authRepo,
            practiceRepository = emptyResumePracticeRepo
        )

        val context = provider.assembleVerifiedContext()
        assertNull(context.resumeAtsScore)
        assertNull(context.resumeMatchedSkills)
        assertNull(context.resumeMissingSkills)
    }

    // TEST 3: Missing assessment remains null
    @Test
    fun test3_missingAssessmentRemainsNull() = runTest {
        val authRepo = MockAuthRepository()
        authRepo.updateProfile(StudentProfile(id = "user_1", name = "Test", email = "test@edu.com", isAssessmentCompleted = false, recommendedCareerId = null))

        val provider = CGAMentorContextProvider(
            authRepository = authRepo,
            careerRepository = null
        )

        val context = provider.assembleVerifiedContext()
        assertNull(context.matchPercentage)
    }

    // TEST 4: Missing roadmap remains null
    @Test
    fun test4_missingRoadmapRemainsNull() = runTest {
        val authRepo = MockAuthRepository()
        val provider = CGAMentorContextProvider(
            authRepository = authRepo,
            roadmapRepository = null
        )

        val context = provider.assembleVerifiedContext()
        assertNull(context.currentRoadmapStage)
        assertNull(context.roadmapProgress)
        assertNull(context.completedMilestones)
        assertNull(context.nextPendingMilestones)
    }

    // TEST 5: Missing practice data remains null
    @Test
    fun test5_missingPracticeDataRemainsNull() = runTest {
        val authRepo = MockAuthRepository()
        val provider = CGAMentorContextProvider(
            authRepository = authRepo,
            practiceRepository = null
        )

        val context = provider.assembleVerifiedContext()
        assertNull(context.solvedPracticeCount)
        assertNull(context.totalPracticeCount)
    }

    // TEST 6: Optional context failure does not throw or break context assembly
    @Test
    fun test6_optionalContextFailureDoesNotBreakAssembly() = runTest {
        val failingAuthRepo = MockAuthRepository()
        val failingPracticeRepo = object : PracticeRepository by MockPracticeRepository() {
            override suspend fun getResumeAnalysis(): ResumeAnalysis {
                throw RuntimeException("Network timeout on resume")
            }
            override fun getProblems(careerId: String?): kotlinx.coroutines.flow.Flow<List<com.example.domain.model.PracticeProblem>> = flow {
                throw RuntimeException("Database error")
            }
        }

        val provider = CGAMentorContextProvider(
            authRepository = failingAuthRepo,
            practiceRepository = failingPracticeRepo
        )

        // Should complete gracefully without throwing
        val context = provider.assembleVerifiedContext()
        assertNotNull(context)
        assertNull(context.resumeAtsScore)
        assertNull(context.solvedPracticeCount)
    }

    // TEST 7: Career question selects career context
    @Test
    fun test7_careerQuestionSelectsCareerContext() {
        val provider = CGAMentorContextProvider()
        val full = CGAMentorContext(
            studentName = "Alex",
            department = "CSE",
            yearOfStudy = "4th Year",
            careerInterest = "AI Engineering",
            recommendedCareer = "AI / Machine Learning Engineer",
            matchPercentage = 92,
            currentRoadmapStage = "Stage 2",
            roadmapProgress = 45,
            resumeAtsScore = 85
        )

        val selected = provider.selectRelevantContext(full, "What career opportunities exist in AI and Machine Learning?")
        assertNotNull(selected)
        assertEquals("Alex", selected?.studentName)
        assertEquals("AI / Machine Learning Engineer", selected?.recommendedCareer)
        assertEquals(92, selected?.matchPercentage)
        assertEquals("AI Engineering", selected?.careerInterest)
        assertNull(selected?.currentRoadmapStage)
        assertNull(selected?.resumeAtsScore)
    }

    // TEST 8: Roadmap question selects roadmap context
    @Test
    fun test8_roadmapQuestionSelectsRoadmapContext() {
        val provider = CGAMentorContextProvider()
        val full = CGAMentorContext(
            studentName = "Alex",
            recommendedCareer = "AI / Machine Learning Engineer",
            currentRoadmapStage = "Deep Learning & NLP",
            roadmapProgress = 60,
            completedMilestones = listOf("Linear Algebra", "Python"),
            nextPendingMilestones = listOf("PyTorch Foundations"),
            resumeAtsScore = 85
        )

        val selected = provider.selectRelevantContext(full, "What should I learn next in my roadmap checkpoint?")
        assertNotNull(selected)
        assertEquals("Deep Learning & NLP", selected?.currentRoadmapStage)
        assertEquals(60, selected?.roadmapProgress)
        assertEquals(listOf("PyTorch Foundations"), selected?.nextPendingMilestones)
        assertNull(selected?.resumeAtsScore)
    }

    // TEST 9: Practice question selects practice context
    @Test
    fun test9_practiceQuestionSelectsPracticeContext() {
        val provider = CGAMentorContextProvider()
        val full = CGAMentorContext(
            studentName = "Alex",
            recommendedCareer = "AI / Machine Learning Engineer",
            currentRoadmapStage = "Core DSA",
            solvedPracticeCount = 14,
            totalPracticeCount = 84,
            resumeAtsScore = 85
        )

        val selected = provider.selectRelevantContext(full, "Can you give me a coding interview problem on binary search?")
        assertNotNull(selected)
        assertEquals(14, selected?.solvedPracticeCount)
        assertEquals(84, selected?.totalPracticeCount)
        assertNull(selected?.resumeAtsScore)
    }

    // TEST 10: Resume question selects resume context
    @Test
    fun test10_resumeQuestionSelectsResumeContext() {
        val provider = CGAMentorContextProvider()
        val full = CGAMentorContext(
            studentName = "Alex",
            recommendedCareer = "AI / Machine Learning Engineer",
            resumeAtsScore = 78,
            resumeMatchedSkills = listOf("Python", "SQL"),
            resumeMissingSkills = listOf("Docker", "MLOps"),
            currentRoadmapStage = "Stage 3"
        )

        val selected = provider.selectRelevantContext(full, "How do I improve my resume ATS score and fix missing skills?")
        assertNotNull(selected)
        assertEquals(78, selected?.resumeAtsScore)
        assertEquals(listOf("Docker", "MLOps"), selected?.resumeMissingSkills)
        assertNull(selected?.currentRoadmapStage)
    }

    // TEST 11: General question does not unnecessarily include all context
    @Test
    fun test11_generalQuestionExcludesUnrelatedContext() {
        val provider = CGAMentorContextProvider()
        val full = CGAMentorContext(
            studentName = "Alex",
            recommendedCareer = "AI / Machine Learning Engineer",
            matchPercentage = 95,
            currentRoadmapStage = "Stage 3",
            roadmapProgress = 70,
            resumeAtsScore = 88,
            xp = 1500,
            currentStreak = 12
        )

        val selected = provider.selectRelevantContext(full, "What is the difference between TCP and UDP in networking?")
        assertNotNull(selected)
        assertEquals("Alex", selected?.studentName)
        assertNull(selected?.recommendedCareer)
        assertNull(selected?.currentRoadmapStage)
        assertNull(selected?.resumeAtsScore)
        assertNull(selected?.xp)
    }

    // TEST 12: Motivation / accountability selects streak and XP
    @Test
    fun test12_motivationAccountabilitySelectsStreakAndXp() {
        val provider = CGAMentorContextProvider()
        val full = CGAMentorContext(
            studentName = "Alex",
            currentStreak = 7,
            xp = 650,
            roadmapProgress = 40,
            solvedPracticeCount = 12,
            totalPracticeCount = 84,
            resumeAtsScore = 80
        )

        val selected = provider.selectRelevantContext(full, "How is my study streak and XP progress looking?")
        assertNotNull(selected)
        assertEquals(7, selected?.currentStreak)
        assertEquals(650, selected?.xp)
        assertEquals(40, selected?.roadmapProgress)
        assertNull(selected?.resumeAtsScore)
    }

    // TEST 13: No fabricated zero values for missing data
    @Test
    fun test13_noFabricatedZeroValues() = runTest {
        val unassessedProfile = StudentProfile(
            id = "user_new",
            name = "New User",
            email = "new@student.com",
            isAssessmentCompleted = false,
            currentStreak = 0,
            xp = 0
        )
        val authRepo = MockAuthRepository()
        authRepo.updateProfile(unassessedProfile)

        val provider = CGAMentorContextProvider(authRepository = authRepo)
        val context = provider.assembleVerifiedContext()

        // Missing/zero metrics should be null, not 0
        assertNull(context.currentStreak)
        assertNull(context.xp)
        assertNull(context.matchPercentage)
        assertNull(context.roadmapProgress)
    }

    // TEST 14: Proactive focus only appears when supported by verified data
    @Test
    fun test14_proactiveFocusDerivedFromVerifiedData() {
        val provider = CGAMentorContextProvider()

        // Case A: Has pending milestones and missing resume skills
        val contextWithData = CGAMentorContext(
            studentName = "Alex",
            recommendedCareer = "AI / Machine Learning Engineer",
            nextPendingMilestones = listOf("Learn PyTorch"),
            resumeMissingSkills = listOf("Docker", "MLOps"),
            currentStreak = 5,
            solvedPracticeCount = 10,
            totalPracticeCount = 50
        )
        val focusItems = provider.deriveProactiveFocusItems(contextWithData)
        assertTrue(focusItems.isNotEmpty())
        assertTrue(focusItems.any { it.source == "ROADMAP" && it.description.contains("PyTorch") })
        assertTrue(focusItems.any { it.source == "RESUME" && it.description.contains("Docker") })
        assertTrue(focusItems.any { it.source == "STREAK" && it.description.contains("5") })

        // Case B: Null context has zero focus items
        val emptyFocus = provider.deriveProactiveFocusItems(null)
        assertTrue(emptyFocus.isEmpty())
    }

    // TEST 15: "which topic I have recommended in our CGA" query selects career recommendation context
    @Test
    fun test15_whichTopicRecommendedQuerySelectsCareerContext() {
        val provider = CGAMentorContextProvider()
        val full = CGAMentorContext(
            studentName = "Rahul",
            department = "Computer Science",
            yearOfStudy = "3rd Year",
            careerInterest = "Artificial Intelligence",
            recommendedCareer = "AI / Machine Learning Engineer",
            matchPercentage = 88,
            currentRoadmapStage = "Core Machine Learning",
            roadmapProgress = 45,
            currentStreak = 4,
            xp = 350
        )

        val selected = provider.selectRelevantContext(full, "hey bro do you know which topic I have recommended in our CGA")
        assertNotNull(selected)
        assertEquals("Rahul", selected?.studentName)
        assertEquals("AI / Machine Learning Engineer", selected?.recommendedCareer)
        assertEquals(88, selected?.matchPercentage)
        assertEquals("Computer Science", selected?.department)
        assertEquals("3rd Year", selected?.yearOfStudy)
    }
}
