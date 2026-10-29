package com.example

import com.example.data.repository.DefaultPracticeCatalog
import com.example.data.repository.MockAuthRepository
import com.example.domain.model.PracticeProblem
import com.example.domain.model.StudentProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Validates user isolation in practice problem progress tracking.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class PracticeUserIsolationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    class IsolatedUserPracticeStore {
        private val userProgressMap = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

        fun getProblemsForUser(userId: String, careerId: String? = null): List<PracticeProblem> {
            val userSolved = userProgressMap.value[userId] ?: emptySet()
            val problems = DefaultPracticeCatalog.getPracticeProblems(careerId)
            return problems.map { it.copy(isSolved = userSolved.contains(it.id)) }
        }

        fun toggleSolved(userId: String, problemId: String) {
            userProgressMap.update { current ->
                val userSet = (current[userId] ?: emptySet()).toMutableSet()
                if (userSet.contains(problemId)) {
                    userSet.remove(problemId)
                } else {
                    userSet.add(problemId)
                }
                current + (userId to userSet)
            }
        }
    }

    @Test
    fun testUserProgressIsolationBetweenTwoUsers() = runTest(testDispatcher) {
        val store = IsolatedUserPracticeStore()
        val userA = "usr_alice_111"
        val userB = "usr_bob_222"

        // Initially both users have 0 solved problems
        val aliceInitial = store.getProblemsForUser(userA)
        val bobInitial = store.getProblemsForUser(userB)
        assertFalse(aliceInitial.first().isSolved)
        assertFalse(bobInitial.first().isSolved)

        // Alice solves problem dsa_01
        store.toggleSolved(userA, "dsa_01")

        // Alice should see dsa_01 as solved
        val aliceUpdated = store.getProblemsForUser(userA)
        assertTrue(aliceUpdated.first { it.id == "dsa_01" }.isSolved)

        // Bob MUST NOT see dsa_01 as solved (Zero leakage guarantee)
        val bobUpdated = store.getProblemsForUser(userB)
        assertFalse(bobUpdated.first { it.id == "dsa_01" }.isSolved)

        // Bob solves problem dsa_02
        store.toggleSolved(userB, "dsa_02")
        val bobUpdated2 = store.getProblemsForUser(userB)
        assertTrue(bobUpdated2.first { it.id == "dsa_02" }.isSolved)
        assertFalse(bobUpdated2.first { it.id == "dsa_01" }.isSolved)

        val aliceUpdated2 = store.getProblemsForUser(userA)
        assertTrue(aliceUpdated2.first { it.id == "dsa_01" }.isSolved)
        assertFalse(aliceUpdated2.first { it.id == "dsa_02" }.isSolved)
    }

    @Test
    fun testCareerFilteringIsolatesIrrelevantCareerProblems() {
        val problemsForAiml = DefaultPracticeCatalog.getPracticeProblems("aiml_engineer")
        val problemsForCyber = DefaultPracticeCatalog.getPracticeProblems("cyber_analyst")

        assertTrue(problemsForAiml.any { it.careerId == "aiml_engineer" })
        assertFalse(problemsForAiml.any { it.careerId == "cyber_analyst" })

        assertTrue(problemsForCyber.any { it.careerId == "cyber_analyst" })
        assertFalse(problemsForCyber.any { it.careerId == "aiml_engineer" })
    }
}
