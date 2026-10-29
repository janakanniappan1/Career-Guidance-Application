package com.example.data.repository

import com.example.domain.model.LeaderboardUser
import com.example.util.CareerCanonicalUtil

/**
 * Standard domain cohort data providing baseline peers for each canonical domain in CGA.
 * Used as a dependable baseline and fallback when the database RPC is syncing or offline.
 */
object DefaultLeaderboardCatalog {

    fun getDomainTitle(domainId: String): String {
        return CareerCanonicalUtil.getCareerTitle(domainId)
    }

    fun getBaselinePeersForDomain(domainId: String): List<LeaderboardUser> {
        val canonicalId = CareerCanonicalUtil.toCanonicalCareerId(domainId)
        val title = getDomainTitle(canonicalId)

        return when (canonicalId) {
            CareerCanonicalUtil.CAREER_AIML -> listOf(
                LeaderboardUser(
                    userId = "peer_aiml_1",
                    displayName = "Aarav Sharma",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "CSE - AI & ML",
                    yearOfStudy = "4th Year",
                    xp = 2850,
                    completionPercentage = 92,
                    streak = 24,
                    completedTasks = 23,
                    totalTasks = 25
                ),
                LeaderboardUser(
                    userId = "peer_aiml_2",
                    displayName = "Priya Patel",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Computer Science",
                    yearOfStudy = "3rd Year",
                    xp = 2420,
                    completionPercentage = 84,
                    streak = 19,
                    completedTasks = 21,
                    totalTasks = 25
                ),
                LeaderboardUser(
                    userId = "peer_aiml_3",
                    displayName = "Rohan Verma",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Information Technology",
                    yearOfStudy = "3rd Year",
                    xp = 1980,
                    completionPercentage = 76,
                    streak = 14,
                    completedTasks = 19,
                    totalTasks = 25
                ),
                LeaderboardUser(
                    userId = "peer_aiml_4",
                    displayName = "Ananya Iyer",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "CSE - Data Science",
                    yearOfStudy = "2nd Year",
                    xp = 1650,
                    completionPercentage = 68,
                    streak = 11,
                    completedTasks = 17,
                    totalTasks = 25
                ),
                LeaderboardUser(
                    userId = "peer_aiml_5",
                    displayName = "Karthik Raja",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Computer Science",
                    yearOfStudy = "3rd Year",
                    xp = 1320,
                    completionPercentage = 56,
                    streak = 8,
                    completedTasks = 14,
                    totalTasks = 25
                ),
                LeaderboardUser(
                    userId = "peer_aiml_6",
                    displayName = "Meera Nambiar",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Electronics & Comm.",
                    yearOfStudy = "4th Year",
                    xp = 950,
                    completionPercentage = 44,
                    streak = 5,
                    completedTasks = 11,
                    totalTasks = 25
                )
            )

            CareerCanonicalUtil.CAREER_SOFTWARE_DEV -> listOf(
                LeaderboardUser(
                    userId = "peer_sd_1",
                    displayName = "Vikram Aditya",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Computer Science",
                    yearOfStudy = "4th Year",
                    xp = 3100,
                    completionPercentage = 95,
                    streak = 28,
                    completedTasks = 19,
                    totalTasks = 20
                ),
                LeaderboardUser(
                    userId = "peer_sd_2",
                    displayName = "Sneha Nair",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Information Technology",
                    yearOfStudy = "3rd Year",
                    xp = 2650,
                    completionPercentage = 85,
                    streak = 21,
                    completedTasks = 17,
                    totalTasks = 20
                ),
                LeaderboardUser(
                    userId = "peer_sd_3",
                    displayName = "Aditya Kulkarni",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Computer Science",
                    yearOfStudy = "3rd Year",
                    xp = 2200,
                    completionPercentage = 75,
                    streak = 16,
                    completedTasks = 15,
                    totalTasks = 20
                ),
                LeaderboardUser(
                    userId = "peer_sd_4",
                    displayName = "Divya Krishnan",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Information Science",
                    yearOfStudy = "2nd Year",
                    xp = 1750,
                    completionPercentage = 65,
                    streak = 12,
                    completedTasks = 13,
                    totalTasks = 20
                ),
                LeaderboardUser(
                    userId = "peer_sd_5",
                    displayName = "Rahul Joshi",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Computer Science",
                    yearOfStudy = "4th Year",
                    xp = 1400,
                    completionPercentage = 55,
                    streak = 7,
                    completedTasks = 11,
                    totalTasks = 20
                ),
                LeaderboardUser(
                    userId = "peer_sd_6",
                    displayName = "Tanvi Deshmukh",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Electronics & CS",
                    yearOfStudy = "3rd Year",
                    xp = 1050,
                    completionPercentage = 45,
                    streak = 4,
                    completedTasks = 9,
                    totalTasks = 20
                )
            )

            CareerCanonicalUtil.CAREER_DATA_ANALYST -> listOf(
                LeaderboardUser(
                    userId = "peer_da_1",
                    displayName = "Pooja Hegde",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Information Technology",
                    yearOfStudy = "4th Year",
                    xp = 2700,
                    completionPercentage = 90,
                    streak = 22,
                    completedTasks = 18,
                    totalTasks = 20
                ),
                LeaderboardUser(
                    userId = "peer_da_2",
                    displayName = "Siddharth Rao",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "CSE - Data Analytics",
                    yearOfStudy = "3rd Year",
                    xp = 2300,
                    completionPercentage = 80,
                    streak = 17,
                    completedTasks = 16,
                    totalTasks = 20
                ),
                LeaderboardUser(
                    userId = "peer_da_3",
                    displayName = "Neha Gupta",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Mathematics & Computing",
                    yearOfStudy = "3rd Year",
                    xp = 1850,
                    completionPercentage = 70,
                    streak = 13,
                    completedTasks = 14,
                    totalTasks = 20
                ),
                LeaderboardUser(
                    userId = "peer_da_4",
                    displayName = "Akash Sundaram",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Computer Applications",
                    yearOfStudy = "2nd Year",
                    xp = 1500,
                    completionPercentage = 60,
                    streak = 9,
                    completedTasks = 12,
                    totalTasks = 20
                ),
                LeaderboardUser(
                    userId = "peer_da_5",
                    displayName = "Ritika Sen",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Commerce & Analytics",
                    yearOfStudy = "4th Year",
                    xp = 1100,
                    completionPercentage = 50,
                    streak = 6,
                    completedTasks = 10,
                    totalTasks = 20
                )
            )

            CareerCanonicalUtil.CAREER_DATA_SCIENTIST -> listOf(
                LeaderboardUser(
                    userId = "peer_ds_1",
                    displayName = "Manish Bhatia",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "CSE - Data Science",
                    yearOfStudy = "4th Year",
                    xp = 2900,
                    completionPercentage = 91,
                    streak = 25,
                    completedTasks = 20,
                    totalTasks = 22
                ),
                LeaderboardUser(
                    userId = "peer_ds_2",
                    displayName = "Swati Menon",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Computer Science",
                    yearOfStudy = "3rd Year",
                    xp = 2450,
                    completionPercentage = 82,
                    streak = 18,
                    completedTasks = 18,
                    totalTasks = 22
                ),
                LeaderboardUser(
                    userId = "peer_ds_3",
                    displayName = "Deepak Chawla",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Statistics & AI",
                    yearOfStudy = "3rd Year",
                    xp = 1950,
                    completionPercentage = 73,
                    streak = 14,
                    completedTasks = 16,
                    totalTasks = 22
                ),
                LeaderboardUser(
                    userId = "peer_ds_4",
                    displayName = "Isha Ganguly",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Information Technology",
                    yearOfStudy = "2nd Year",
                    xp = 1600,
                    completionPercentage = 64,
                    streak = 10,
                    completedTasks = 14,
                    totalTasks = 22
                )
            )

            CareerCanonicalUtil.CAREER_UIUX -> listOf(
                LeaderboardUser(
                    userId = "peer_ui_1",
                    displayName = "Maya Srinivasan",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Design & Media",
                    yearOfStudy = "4th Year",
                    xp = 2750,
                    completionPercentage = 94,
                    streak = 23,
                    completedTasks = 16,
                    totalTasks = 17
                ),
                LeaderboardUser(
                    userId = "peer_ui_2",
                    displayName = "Arjun Kapoor",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Computer Science",
                    yearOfStudy = "3rd Year",
                    xp = 2350,
                    completionPercentage = 82,
                    streak = 17,
                    completedTasks = 14,
                    totalTasks = 17
                ),
                LeaderboardUser(
                    userId = "peer_ui_3",
                    displayName = "Bhavna Jain",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Information Technology",
                    yearOfStudy = "3rd Year",
                    xp = 1900,
                    completionPercentage = 71,
                    streak = 13,
                    completedTasks = 12,
                    totalTasks = 17
                ),
                LeaderboardUser(
                    userId = "peer_ui_4",
                    displayName = "Sameer Wagle",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Human-Computer Interaction",
                    yearOfStudy = "2nd Year",
                    xp = 1450,
                    completionPercentage = 59,
                    streak = 9,
                    completedTasks = 10,
                    totalTasks = 17
                )
            )

            CareerCanonicalUtil.CAREER_CYBER_SECURITY -> listOf(
                LeaderboardUser(
                    userId = "peer_cyber_1",
                    displayName = "Harsh Vardhan",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "CSE - Cybersecurity",
                    yearOfStudy = "4th Year",
                    xp = 2800,
                    completionPercentage = 93,
                    streak = 26,
                    completedTasks = 14,
                    totalTasks = 15
                ),
                LeaderboardUser(
                    userId = "peer_cyber_2",
                    displayName = "Zoya Fatima",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Information Technology",
                    yearOfStudy = "3rd Year",
                    xp = 2400,
                    completionPercentage = 80,
                    streak = 18,
                    completedTasks = 12,
                    totalTasks = 15
                ),
                LeaderboardUser(
                    userId = "peer_cyber_3",
                    displayName = "Naveen Prasad",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Computer Science",
                    yearOfStudy = "3rd Year",
                    xp = 1920,
                    completionPercentage = 67,
                    streak = 12,
                    completedTasks = 10,
                    totalTasks = 15
                ),
                LeaderboardUser(
                    userId = "peer_cyber_4",
                    displayName = "Riya Chakraborty",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Computer Networks",
                    yearOfStudy = "2nd Year",
                    xp = 1550,
                    completionPercentage = 60,
                    streak = 8,
                    completedTasks = 9,
                    totalTasks = 15
                )
            )

            CareerCanonicalUtil.CAREER_CLOUD_ENGINEER -> listOf(
                LeaderboardUser(
                    userId = "peer_cloud_1",
                    displayName = "Kunal Rawat",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Computer Science",
                    yearOfStudy = "4th Year",
                    xp = 2950,
                    completionPercentage = 94,
                    streak = 27,
                    completedTasks = 16,
                    totalTasks = 17
                ),
                LeaderboardUser(
                    userId = "peer_cloud_2",
                    displayName = "Lavanya Murthy",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Information Technology",
                    yearOfStudy = "3rd Year",
                    xp = 2500,
                    completionPercentage = 82,
                    streak = 19,
                    completedTasks = 14,
                    totalTasks = 17
                ),
                LeaderboardUser(
                    userId = "peer_cloud_3",
                    displayName = "Gaurav Malhotra",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Cloud Computing & DevOps",
                    yearOfStudy = "3rd Year",
                    xp = 2050,
                    completionPercentage = 71,
                    streak = 15,
                    completedTasks = 12,
                    totalTasks = 17
                ),
                LeaderboardUser(
                    userId = "peer_cloud_4",
                    displayName = "Tara Mukherjee",
                    domainId = canonicalId,
                    domainTitle = title,
                    department = "Systems Engineering",
                    yearOfStudy = "2nd Year",
                    xp = 1600,
                    completionPercentage = 59,
                    streak = 10,
                    completedTasks = 10,
                    totalTasks = 17
                )
            )

            else -> emptyList()
        }
    }
}
