package com.example

import com.example.data.repository.MockAssessmentRepository
import com.example.data.repository.MockCareerRepository
import com.example.data.repository.MockPracticeRepository
import com.example.data.repository.MockRoadmapRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAssessmentQuestionsCount() = runBlocking {
        val repo = MockAssessmentRepository()
        val questions = repo.getAssessmentQuestions()
        assertEquals(15, questions.size)
    }

    @Test
    fun testCareerRecommendationCalculation() = runBlocking {
        val assessmentRepo = MockAssessmentRepository()
        val careerRepo = MockCareerRepository(assessmentRepo)

        // Simulate choosing option 1 for all questions (AI/ML leaning options)
        val answers = (1..15).associateWith { "q${it}_opt1" }
        val recommendation = careerRepo.calculateRecommendation(answers)

        assertNotNull(recommendation)
        assertTrue(recommendation.matchPercentage in 60..100)
        assertTrue(recommendation.recommendedSkills.isNotEmpty())
    }

    @Test
    fun testRoadmapTaskToggle() = runBlocking {
        val roadmapRepo = MockRoadmapRepository()
        val roadmap = roadmapRepo.getRoadmap("aiml_engineer").first()

        val firstTask = roadmap.stages.first().tasks.first()
        val originalStatus = firstTask.isCompleted

        roadmapRepo.toggleTask("aiml_engineer", "stage_1", firstTask.id)
        val updatedRoadmap = roadmapRepo.getRoadmap("aiml_engineer").first()
        val updatedTask = updatedRoadmap.stages.first().tasks.first()

        assertEquals(!originalStatus, updatedTask.isCompleted)
    }

    @Test
    fun testPracticeProblemToggle() = runBlocking {
        val practiceRepo = MockPracticeRepository(initialSolvedProblemIds = emptySet())
        val problems = practiceRepo.getProblems().first()
        val firstProblem = problems.first()
        val originalStatus = firstProblem.isSolved
        assertEquals(false, originalStatus)

        practiceRepo.toggleProblemSolved(firstProblem.id)
        val updatedProblems = practiceRepo.getProblems().first()
        val updatedFirst = updatedProblems.first { it.id == firstProblem.id }

        assertEquals(true, updatedFirst.isSolved)
    }
}
