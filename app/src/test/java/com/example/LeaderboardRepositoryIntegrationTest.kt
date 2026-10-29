package com.example

import com.example.data.model.LeaderboardEntryDto
import com.example.domain.model.RankingMode
import com.example.util.CareerCanonicalUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardRepositoryIntegrationTest {

    @Test
    fun testLeaderboardDtoMappingAndSanitization() {
        val dtos = listOf(
            LeaderboardEntryDto(
                userId = "user-1",
                displayName = "Alice",
                domainId = "aiml_engineer",
                domainTitle = "AI / Machine Learning Engineer",
                department = "CSE",
                yearOfStudy = "4th Year",
                xp = 1200,
                completionPercentage = 80,
                streak = 5,
                completedTasks = 16,
                totalTasks = 20
            ),
            LeaderboardEntryDto(
                userId = "user-2",
                displayName = "Bob",
                domainId = "aiml_engineer",
                domainTitle = "AI / Machine Learning Engineer",
                department = "ECE",
                yearOfStudy = "3rd Year",
                xp = 1500,
                completionPercentage = 60,
                streak = 10,
                completedTasks = 12,
                totalTasks = 20
            ),
            LeaderboardEntryDto(
                userId = "user-3",
                displayName = "Charlie",
                domainId = "aiml_engineer",
                domainTitle = "AI / Machine Learning Engineer",
                department = "IT",
                yearOfStudy = "2nd Year",
                xp = 1200,
                completionPercentage = 90,
                streak = 3,
                completedTasks = 18,
                totalTasks = 20
            )
        )

        // 1. Current user identification
        val currentUserId = "user-1"
        val domainUsers = dtos.map { it.toDomain(currentUserId) }

        assertTrue(domainUsers.first { it.userId == "user-1" }.isCurrentUser)
        assertFalse(domainUsers.first { it.userId == "user-2" }.isCurrentUser)

        // 2. XP ranking: XP DESC, then completion DESC, then streak DESC
        val xpSorted = domainUsers.sortedWith(
            compareByDescending<com.example.domain.model.LeaderboardUser> { it.xp }
                .thenByDescending { it.completionPercentage }
                .thenByDescending { it.streak }
                .thenBy { it.displayName.lowercase() }
                .thenBy { it.userId }
        )
        assertEquals("user-2", xpSorted[0].userId) // 1500 XP
        assertEquals("user-3", xpSorted[1].userId) // 1200 XP, 90%
        assertEquals("user-1", xpSorted[2].userId) // 1200 XP, 80%

        // 3. Completion ranking: completion DESC, then XP DESC, then streak DESC
        val completionSorted = domainUsers.sortedWith(
            compareByDescending<com.example.domain.model.LeaderboardUser> { it.completionPercentage }
                .thenByDescending { it.xp }
                .thenByDescending { it.streak }
                .thenBy { it.displayName.lowercase() }
                .thenBy { it.userId }
        )
        assertEquals("user-3", completionSorted[0].userId) // 90%
        assertEquals("user-1", completionSorted[1].userId) // 80%
        assertEquals("user-2", completionSorted[2].userId) // 60%

        // 4. Streak ranking: streak DESC, then XP DESC, then completion DESC
        val streakSorted = domainUsers.sortedWith(
            compareByDescending<com.example.domain.model.LeaderboardUser> { it.streak }
                .thenByDescending { it.xp }
                .thenByDescending { it.completionPercentage }
                .thenBy { it.displayName.lowercase() }
                .thenBy { it.userId }
        )
        assertEquals("user-2", streakSorted[0].userId) // 10 days
        assertEquals("user-1", streakSorted[1].userId) // 5 days
        assertEquals("user-3", streakSorted[2].userId) // 3 days
    }

    @Test
    fun testBoundaryAndSanitizationConditions() {
        val rawDto = LeaderboardEntryDto(
            userId = "edge-case-user",
            displayName = "",
            domainId = "aiml_engineer",
            xp = -50,
            completionPercentage = 150,
            streak = -2,
            completedTasks = 25,
            totalTasks = 20
        )

        val user = rawDto.toDomain("edge-case-user")
        assertEquals("Student", user.displayName)
        assertTrue(user.completionPercentage in 0..100)
        assertEquals(100, user.completionPercentage)
        assertEquals(0, user.streak)
        assertTrue(user.isCurrentUser)
    }

    @Test
    fun testCanonicalDomainMatching() {
        val canonicalAIML = CareerCanonicalUtil.toCanonicalCareerId("aiml")
        assertEquals(CareerCanonicalUtil.CAREER_AIML, canonicalAIML)
        val canonicalDev = CareerCanonicalUtil.toCanonicalCareerId("software_development")
        assertEquals(CareerCanonicalUtil.CAREER_SOFTWARE_DEV, canonicalDev)
    }
}
