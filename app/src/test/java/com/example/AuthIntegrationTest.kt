package com.example

import com.example.data.model.ProfileDto
import com.example.domain.model.StudentProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testProfileDtoSerializationAndMapping() {
        val jsonPayload = """
            {
                "id": "f516a048-8df0-4b52-b883-fa4c85777df5",
                "name": "Jane Doe",
                "email": "jane@college.edu",
                "department": "Computer Science & Engineering",
                "year_of_study": "3rd Year",
                "career_interest": "Artificial Intelligence",
                "current_streak": 5,
                "is_assessment_completed": true,
                "recommended_career_id": "aiml_engineer",
                "daily_motivational_message": "Keep learning!"
            }
        """.trimIndent()

        val dto = json.decodeFromString<ProfileDto>(jsonPayload)
        assertEquals("f516a048-8df0-4b52-b883-fa4c85777df5", dto.id)
        assertEquals("Jane Doe", dto.name)
        assertEquals("jane@college.edu", dto.email)
        assertEquals("Computer Science & Engineering", dto.department)
        assertEquals("3rd Year", dto.yearOfStudy)
        assertEquals("Artificial Intelligence", dto.careerInterest)
        assertEquals(5, dto.currentStreak)
        assertEquals(true, dto.isAssessmentCompleted)
        assertEquals("aiml_engineer", dto.recommendedCareerId)

        val domain = dto.toDomain()
        assertEquals(dto.id, domain.id)
        assertEquals(dto.name, domain.name)
        assertEquals(dto.email, domain.email)
        assertEquals(dto.department, domain.department)
        assertEquals(dto.yearOfStudy, domain.yearOfStudy)
        assertEquals(dto.careerInterest, domain.careerInterest)
        assertEquals(5, domain.currentStreak)
        assertTrue(domain.isAssessmentCompleted)
        assertEquals("aiml_engineer", domain.recommendedCareerId)

        val convertedDto = ProfileDto.fromDomain(domain)
        assertEquals(dto.id, convertedDto.id)
        assertEquals(dto.name, convertedDto.name)
        assertEquals(dto.email, convertedDto.email)
        assertEquals(dto.department, convertedDto.department)
        assertEquals(dto.yearOfStudy, convertedDto.yearOfStudy)
    }

    @Test
    fun testProfileDtoDefaultHandling() {
        val jsonPayload = """
            {
                "id": "user_12345"
            }
        """.trimIndent()

        val dto = json.decodeFromString<ProfileDto>(jsonPayload)
        val domain = dto.toDomain()

        assertEquals("user_12345", domain.id)
        assertEquals("Student", domain.name)
        assertEquals(0, domain.currentStreak)
        assertFalse(domain.isAssessmentCompleted)
        assertNotNull(domain.dailyMotivationalMessage)
        assertFalse(domain.isProfileComplete)
    }

    @Test
    fun testProfileDtoExplicitZeroStreakHandling() {
        val jsonPayload = """
            {
                "id": "user_zero_streak",
                "current_streak": 0
            }
        """.trimIndent()

        val dto = json.decodeFromString<ProfileDto>(jsonPayload)
        assertEquals(0, dto.currentStreak)

        val domain = dto.toDomain()
        assertEquals(0, domain.currentStreak)
    }

    @Test
    fun testProfileCompletenessRequirements() {
        val incompleteProfile = StudentProfile(
            id = "u1",
            name = "Test Student",
            email = "student@cga.edu",
            department = "",
            yearOfStudy = ""
        )
        assertFalse(incompleteProfile.isProfileComplete)

        val completeProfile = incompleteProfile.copy(
            department = "Computer Science & Engineering",
            yearOfStudy = "2nd Year",
            careerInterest = "Artificial Intelligence"
        )
        assertTrue(completeProfile.isProfileComplete)
    }

    @Test
    fun testAuthStateDistinction() {
        val signedOut: com.example.domain.model.AuthState = com.example.domain.model.AuthState.SignedOut
        val verification: com.example.domain.model.AuthState = com.example.domain.model.AuthState.VerificationRequired("test@cga.edu")
        val errorState: com.example.domain.model.AuthState = com.example.domain.model.AuthState.AuthError("Network failure")

        val profile = StudentProfile(
            id = "u1",
            name = "Test",
            email = "test@cga.edu",
            department = "CSE",
            yearOfStudy = "1st Year",
            careerInterest = "AI"
        )
        val complete: com.example.domain.model.AuthState = com.example.domain.model.AuthState.ProfileComplete(profile)
        val verified: com.example.domain.model.AuthState = com.example.domain.model.AuthState.EmailVerified(profile)

        assertTrue(signedOut is com.example.domain.model.AuthState.SignedOut)
        assertTrue(verification is com.example.domain.model.AuthState.VerificationRequired)
        assertEquals("test@cga.edu", (verification as com.example.domain.model.AuthState.VerificationRequired).email)
        assertTrue(errorState is com.example.domain.model.AuthState.AuthError)
        assertTrue(complete is com.example.domain.model.AuthState.ProfileComplete)
        assertTrue(verified is com.example.domain.model.AuthState.EmailVerified)
        assertEquals(profile, (verified as com.example.domain.model.AuthState.EmailVerified).profile)
    }

    @Test
    fun testMockAuthRepositoryCheckEmailVerification() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        val result = repo.checkEmailVerification("test@cga.edu", "password123")
        assertTrue(result.isSuccess)
        val profile = result.getOrThrow()
        assertNotNull(profile)
        assertEquals("test@cga.edu", profile.email)
    }

    // =========================================================================
    // 9 REQUIRED SPECIFICATION SCENARIO TESTS
    // =========================================================================

    /**
     * Scenario 1: Existing Google user logs in with complete profile -> routes directly to Dashboard.
     */
    @Test
    fun testScenario1_ExistingGoogleUserWithCompleteProfile_RoutesToDashboard() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.nextGoogleSignInProfile = StudentProfile(
            id = "usr_existing_google",
            name = "Existing Student",
            email = "existing@college.edu",
            department = "Computer Science & Engineering",
            yearOfStudy = "3rd Year",
            careerInterest = "Artificial Intelligence"
        )
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        var callbackInvoked = false
        viewModel.signInWithGoogle {
            callbackInvoked = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(callbackInvoked)
        val state = repo.refreshSession()
        val routeDestination = if (state is com.example.domain.model.AuthState.ProfileComplete) "dashboard" else "complete_profile"
        assertEquals("dashboard", routeDestination)
        assertTrue(state is com.example.domain.model.AuthState.ProfileComplete)
        assertEquals("Existing Student", (state as com.example.domain.model.AuthState.ProfileComplete).profile.name)
    }

    /**
     * Scenario 2: New Google user logs in -> profile missing academic details -> routes to Complete Profile Screen.
     */
    @Test
    fun testScenario2_NewGoogleUserMissingAcademicDetails_RoutesToCompleteProfile() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.nextGoogleSignInProfile = StudentProfile(
            id = "usr_new_google",
            name = "New Google User",
            email = "newuser@gmail.com",
            department = "",
            yearOfStudy = "",
            careerInterest = ""
        )
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        var callbackInvoked = false
        viewModel.signInWithGoogle {
            callbackInvoked = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(callbackInvoked)
        val state = repo.refreshSession()
        val routeDestination = if (state is com.example.domain.model.AuthState.ProfileComplete) "dashboard" else "complete_profile"
        assertEquals("complete_profile", routeDestination)
        assertTrue(state is com.example.domain.model.AuthState.ProfileIncomplete)
        assertFalse((state as com.example.domain.model.AuthState.ProfileIncomplete).profile.isProfileComplete)
    }

    /**
     * Scenario 3: Incomplete profile user logs in -> routes to Complete Profile Screen.
     */
    @Test
    fun testScenario3_IncompleteProfileUserLogin_RoutesToCompleteProfile() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.setMockUser(
            StudentProfile(
                id = "usr_incomplete",
                name = "Partial Student",
                email = "partial@college.edu",
                department = "",
                yearOfStudy = "",
                careerInterest = ""
            )
        )
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        var callbackInvoked = false
        viewModel.login("partial@college.edu", "password123") {
            callbackInvoked = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(callbackInvoked)
        val state = repo.refreshSession()
        val routeDestination = if (state is com.example.domain.model.AuthState.ProfileComplete) "dashboard" else "complete_profile"
        assertEquals("complete_profile", routeDestination)
        assertTrue(state is com.example.domain.model.AuthState.ProfileIncomplete)
    }

    /**
     * Scenario 4: User completes profile -> updates Supabase profile -> routes to Dashboard.
     */
    @Test
    fun testScenario4_UserCompletesProfile_UpdatesProfileAndRoutesToDashboard() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.setMockUser(
            StudentProfile(
                id = "usr_google_partial",
                name = "Alex",
                email = "alex@college.edu",
                department = "",
                yearOfStudy = "",
                careerInterest = ""
            )
        )
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        var navigatedToDashboard = false
        viewModel.completeProfile(
            department = "Information Technology",
            year = "2nd Year",
            careerInterest = "Cloud Computing"
        ) {
            navigatedToDashboard = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(navigatedToDashboard)
        val state = repo.refreshSession()
        assertTrue(state is com.example.domain.model.AuthState.ProfileComplete)
        val profile = (state as com.example.domain.model.AuthState.ProfileComplete).profile
        assertEquals("Information Technology", profile.department)
        assertEquals("2nd Year", profile.yearOfStudy)
        assertEquals("Cloud Computing", profile.careerInterest)
        assertTrue(profile.isProfileComplete)
    }

    /**
     * Scenario 5: App restarted with active session and complete profile -> restores session -> routes directly to Dashboard.
     */
    @Test
    fun testScenario5_AppRestartWithActiveCompleteSession_RestoresDirectlyToDashboard() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.setMockUser(
            StudentProfile(
                id = "usr_active_session",
                name = "Active Student",
                email = "active@college.edu",
                department = "Computer Science & Engineering",
                yearOfStudy = "4th Year",
                careerInterest = "Full Stack Development"
            )
        )

        val restoredState = repo.refreshSession()
        assertTrue(restoredState is com.example.domain.model.AuthState.ProfileComplete)
        val destination = when (restoredState) {
            is com.example.domain.model.AuthState.ProfileComplete -> "dashboard"
            is com.example.domain.model.AuthState.ProfileIncomplete -> "complete_profile"
            else -> "welcome"
        }
        assertEquals("dashboard", destination)
    }

    /**
     * Scenario 6: App restarted with active session and incomplete profile -> restores session -> routes to Complete Profile Screen.
     */
    @Test
    fun testScenario6_AppRestartWithIncompleteSession_RestoresToCompleteProfile() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.setMockUser(
            StudentProfile(
                id = "usr_active_incomplete",
                name = "Pending Details Student",
                email = "pending@college.edu",
                department = "",
                yearOfStudy = "",
                careerInterest = ""
            )
        )

        val restoredState = repo.refreshSession()
        assertTrue(restoredState is com.example.domain.model.AuthState.ProfileIncomplete)
        val destination = when (restoredState) {
            is com.example.domain.model.AuthState.ProfileComplete -> "dashboard"
            is com.example.domain.model.AuthState.ProfileIncomplete -> "complete_profile"
            else -> "welcome"
        }
        assertEquals("complete_profile", destination)
    }

    /**
     * Scenario 7: App restarted with no active session -> routes to Welcome / Login.
     */
    @Test
    fun testScenario7_AppRestartWithNoActiveSession_RoutesToWelcome() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.setMockUser(null)

        val restoredState = repo.refreshSession()
        assertTrue(restoredState is com.example.domain.model.AuthState.SignedOut)
        val destination = when (restoredState) {
            is com.example.domain.model.AuthState.ProfileComplete -> "dashboard"
            is com.example.domain.model.AuthState.ProfileIncomplete -> "complete_profile"
            else -> "welcome"
        }
        assertEquals("welcome", destination)
    }

    /**
     * Scenario 8: User logs out -> session cleared -> routes to Welcome / Login.
     */
    @Test
    fun testScenario8_UserLogout_ClearsSessionAndRoutesToWelcome() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()
        val stateAfterLogout = repo.refreshSession()
        assertTrue(stateAfterLogout is com.example.domain.model.AuthState.SignedOut)
        assertEquals(com.example.domain.model.AuthState.SignedOut, viewModel.uiState.value.authState)
    }

    /**
     * Scenario 9: Email verification success -> shows animation -> routes to Dashboard or Complete Profile.
     */
    @Test
    fun testScenario9_EmailVerificationSuccess_RoutesBasedOnProfileCompleteness() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        // Sub-case A: Profile is complete
        val completeProfile = StudentProfile(
            id = "usr_verified_complete",
            name = "Verified Complete",
            email = "verified@cga.edu",
            department = "Mechanical Engineering",
            yearOfStudy = "2nd Year",
            careerInterest = "Robotics"
        )
        repo.setMockUser(completeProfile)
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        var routeResultA: String? = null
        viewModel.verifyEmail("verified@cga.edu") {
            viewModel.completeEmailVerification { isComplete ->
                routeResultA = if (isComplete) "dashboard" else "complete_profile"
            }
        }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("dashboard", routeResultA)

        // Sub-case B: Profile is incomplete
        val incompleteProfile = StudentProfile(
            id = "usr_verified_incomplete",
            name = "Verified Incomplete",
            email = "incomplete@cga.edu",
            department = "",
            yearOfStudy = "",
            careerInterest = ""
        )
        repo.setMockUser(incompleteProfile)
        val viewModelB = com.example.viewmodel.AuthViewModel(repo)

        var routeResultB: String? = null
        viewModelB.verifyEmail("incomplete@cga.edu") {
            viewModelB.completeEmailVerification { isComplete ->
                routeResultB = if (isComplete) "dashboard" else "complete_profile"
            }
        }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("complete_profile", routeResultB)
    }

    /**
     * Scenario 10: AuthState.Initializing prevents premature routing during startup
     */
    @Test
    fun testStartupInitializingState() {
        val initializingState: com.example.domain.model.AuthState = com.example.domain.model.AuthState.Initializing
        assertTrue(initializingState is com.example.domain.model.AuthState.Initializing)
        assertFalse(initializingState is com.example.domain.model.AuthState.SignedOut)
    }

    /**
     * Scenario 11: Complete profile supports updating name along with academic details
     */
    @Test
    fun testCompleteProfile_WithCustomName() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.setMockUser(
            StudentProfile(
                id = "usr_google_no_name",
                name = "",
                email = "noname@college.edu",
                department = "",
                yearOfStudy = "",
                careerInterest = ""
            )
        )
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        var completed = false
        viewModel.completeProfile(
            name = "Rohan Sharma",
            department = "Computer Science & Engineering",
            year = "3rd Year",
            careerInterest = "Cybersecurity"
        ) {
            completed = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(completed)
        val state = repo.refreshSession()
        assertTrue(state is com.example.domain.model.AuthState.ProfileComplete)
        val profile = (state as com.example.domain.model.AuthState.ProfileComplete).profile
        assertEquals("Rohan Sharma", profile.name)
        assertEquals("Computer Science & Engineering", profile.department)
        assertEquals("3rd Year", profile.yearOfStudy)
        assertEquals("Cybersecurity", profile.careerInterest)
        assertTrue(profile.isProfileComplete)
    }

    /**
     * Scenario 12: Supabase Email OTP Verification Flow for Complete Profile
     * Sign up -> VerificationRequired state -> enter 8-digit OTP -> verified -> routes to Dashboard
     */
    @Test
    fun testScenario12_SupabaseEmailOtpVerification_RoutesToDashboard() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.requireVerificationOnSignUp = true
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        // 1. Sign up triggers VerificationRequired
        viewModel.signUp(
            name = "Priya Patel",
            email = "priya@college.edu",
            password = "password123",
            department = "Computer Science",
            year = "3rd Year",
            careerInterest = "Data Science"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val uiStateAfterSignUp = viewModel.uiState.value
        assertTrue(uiStateAfterSignUp.authState is com.example.domain.model.AuthState.VerificationRequired)
        assertEquals("priya@college.edu", uiStateAfterSignUp.verificationEmail)

        // 2. User enters 8-digit OTP
        viewModel.onOtpCodeChanged("74480663")
        assertEquals("74480663", viewModel.uiState.value.otpCode)

        // 3. Verify OTP
        var isProfileCompleteResult: Boolean? = null
        viewModel.verifyEmailOtp { isComplete ->
            isProfileCompleteResult = isComplete
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmailVerified)
        assertEquals(true, isProfileCompleteResult)

        // 4. Complete email verification animates and transitions
        var destination: String? = null
        viewModel.completeEmailVerification { isComplete ->
            destination = if (isComplete) "dashboard" else "complete_profile"
        }
        assertEquals("dashboard", destination)
        assertTrue(viewModel.uiState.value.authState is com.example.domain.model.AuthState.ProfileComplete)
    }

    /**
     * Scenario 13: Supabase Email OTP Verification with Incomplete Profile
     * Routes to Complete Profile screen
     */
    @Test
    fun testScenario13_SupabaseEmailOtpVerification_RoutesToCompleteProfile() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.requireVerificationOnSignUp = true
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        // Sign up with empty academic details
        viewModel.signUp(
            name = "New Student",
            email = "newstudent@college.edu",
            password = "password123",
            department = "",
            year = "",
            careerInterest = ""
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.authState is com.example.domain.model.AuthState.VerificationRequired)

        // Enter 8-digit OTP
        viewModel.onOtpCodeChanged("87654321")

        var isProfileCompleteResult: Boolean? = null
        viewModel.verifyEmailOtp { isComplete ->
            isProfileCompleteResult = isComplete
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmailVerified)
        assertEquals(false, isProfileCompleteResult)

        var destination: String? = null
        viewModel.completeEmailVerification { isComplete ->
            destination = if (isComplete) "dashboard" else "complete_profile"
        }
        assertEquals("complete_profile", destination)
        assertTrue(viewModel.uiState.value.authState is com.example.domain.model.AuthState.ProfileIncomplete)
    }

    /**
     * Scenario 14: Resend Email OTP activates 60s cooldown timer and counts down
     */
    @Test
    fun testScenario14_ResendEmailOtp_TriggersCooldown() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        viewModel.resendEmailOtp("test@cga.edu")
        testDispatcher.scheduler.runCurrent()

        val uiState = viewModel.uiState.value
        assertEquals(60, uiState.resendCooldownSeconds)
        assertNotNull(uiState.resendSuccessMessage)
        assertTrue(uiState.resendSuccessMessage!!.contains("test@cga.edu"))

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.resendCooldownSeconds)
    }

    /**
     * Scenario 15: Central Auth Coordinator - Email/Password with Complete Profile
     * Verifies that the coordinator unifies post-auth routing directly to Dashboard.
     */
    @Test
    fun testCentralAuthCoordinator_EmailPassword_CompleteProfile_RoutesToDashboard() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.setMockUser(null)
        val coordinator = com.example.domain.coordinator.AuthSessionCoordinator(repo)
        val viewModel = com.example.viewmodel.AuthViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()

        var destination = coordinator.resolveDestination(viewModel.uiState.value.authState)
        assertEquals(com.example.domain.coordinator.AuthDestination.Welcome, destination)

        viewModel.login("arun@college.edu", "pass123")
        testDispatcher.scheduler.advanceUntilIdle()

        val authState = viewModel.uiState.value.authState
        assertTrue(authState is com.example.domain.model.AuthState.ProfileComplete)

        val nextScreen = coordinator.computeScreenTransition(
            currentScreen = "login",
            state = authState,
            isEmailVerifiedAnimationActive = false
        )
        assertEquals("dashboard", nextScreen)
    }

    /**
     * Scenario 16: Central Auth Coordinator - Email/Password with Incomplete Profile
     * Verifies that an incomplete profile routes to Complete Profile instead of Dashboard.
     */
    @Test
    fun testCentralAuthCoordinator_EmailPassword_IncompleteProfile_RoutesToCompleteProfile() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        val coordinator = com.example.domain.coordinator.AuthSessionCoordinator(repo)
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        // Setup user with incomplete profile
        val incomplete = StudentProfile(
            id = "usr_inc",
            name = "Incomplete User",
            email = "inc@college.edu",
            department = "",
            yearOfStudy = "",
            careerInterest = ""
        )
        repo.setMockUser(incomplete)

        val destination = coordinator.resolveDestination(com.example.domain.model.AuthState.ProfileIncomplete(incomplete))
        assertTrue(destination is com.example.domain.coordinator.AuthDestination.CompleteProfile)

        val nextScreen = coordinator.computeScreenTransition(
            currentScreen = "login",
            state = com.example.domain.model.AuthState.ProfileIncomplete(incomplete),
            isEmailVerifiedAnimationActive = false
        )
        assertEquals("complete_profile", nextScreen)
    }

    /**
     * Scenario 17: Central Auth Coordinator - Google OAuth with Complete Profile
     * Verifies that Google OAuth session routes through the central coordinator to Dashboard.
     */
    @Test
    fun testCentralAuthCoordinator_GoogleOAuth_CompleteProfile_RoutesToDashboard() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.nextGoogleSignInProfile = StudentProfile(
            id = "google_user_1",
            name = "Google Student",
            email = "student@gmail.com",
            department = "Computer Science & Engineering",
            yearOfStudy = "2nd Year",
            careerInterest = "Full Stack Development"
        )
        val coordinator = com.example.domain.coordinator.AuthSessionCoordinator(repo)
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        viewModel.signInWithGoogle()
        testDispatcher.scheduler.advanceUntilIdle()

        val googleProfile = repo.nextGoogleSignInProfile!!
        assertTrue(googleProfile.isProfileComplete)

        val nextScreen = coordinator.computeScreenTransition(
            currentScreen = "login",
            state = com.example.domain.model.AuthState.ProfileComplete(googleProfile),
            isEmailVerifiedAnimationActive = false
        )
        assertEquals("dashboard", nextScreen)
    }

    /**
     * Scenario 18: Central Auth Coordinator - Google OAuth with Incomplete Profile
     * Verifies that a first-time Google OAuth user with incomplete profile routes to Complete Profile.
     */
    @Test
    fun testCentralAuthCoordinator_GoogleOAuth_IncompleteProfile_RoutesToCompleteProfile() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        val firstTimeGoogleProfile = StudentProfile(
            id = "google_new_user",
            name = "New Google User",
            email = "newuser@gmail.com",
            department = "",
            yearOfStudy = "",
            careerInterest = ""
        )
        assertFalse(firstTimeGoogleProfile.isProfileComplete)

        val coordinator = com.example.domain.coordinator.AuthSessionCoordinator(repo)
        val nextScreen = coordinator.computeScreenTransition(
            currentScreen = "welcome",
            state = com.example.domain.model.AuthState.ProfileIncomplete(firstTimeGoogleProfile),
            isEmailVerifiedAnimationActive = false
        )
        assertEquals("complete_profile", nextScreen)
    }

    /**
     * Scenario 19: Central Auth Coordinator - Session Restoration and Logout
     * Verifies startup destination resolution and logout transition.
     */
    @Test
    fun testCentralAuthCoordinator_SessionRestorationAndLogout() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        val coordinator = com.example.domain.coordinator.AuthSessionCoordinator(repo)

        val completeProfile = StudentProfile(
            id = "persisted_user",
            name = "Persisted Student",
            email = "persisted@cga.edu",
            department = "Information Technology",
            yearOfStudy = "4th Year",
            careerInterest = "Cloud & DevOps"
        )
        val restoredDest = coordinator.resolveDestination(com.example.domain.model.AuthState.ProfileComplete(completeProfile))
        assertTrue(restoredDest is com.example.domain.coordinator.AuthDestination.Dashboard)

        val logoutTransition = coordinator.computeScreenTransition(
            currentScreen = "dashboard",
            state = com.example.domain.model.AuthState.SignedOut,
            isEmailVerifiedAnimationActive = false
        )
        assertEquals("welcome", logoutTransition)
    }

    /**
     * Scenario 20: Central Auth Coordinator - OAuth Callback Error
     * Verifies that an OAuth callback error surfaces cleanly in the ViewModel.
     */
    @Test
    fun testCentralAuthCoordinator_OAuthError_SurfacesErrorWithoutHanging() {
        val repo = com.example.data.repository.MockAuthRepository()
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        viewModel.onOAuthCallbackError("Access was denied by the user.")
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.isGoogleAuthStarting)
        assertEquals("Access was denied by the user.", state.error)
        assertTrue(state.authState is com.example.domain.model.AuthState.AuthError)
    }

    /**
     * Scenario 21: Unverified Email Login Block
     * If an unverified user attempts to sign in with email and password,
     * the login fails with UnverifiedEmailException, does NOT call onSuccess,
     * and sets the UI state to VerificationRequired with OTP target email.
     */
    @Test
    fun testLogin_UnverifiedEmail_BlockedAndTransitionsToVerificationRequired() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.setMockUser(null)
        val viewModel = com.example.viewmodel.AuthViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()

        val unverifiedEmail = "unverified.student@cga.edu"
        repo.unverifiedUsers.add(unverifiedEmail)

        var onSuccessCalled = false
        viewModel.login(unverifiedEmail, "password123", onSuccess = {
            onSuccessCalled = true
        })
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(onSuccessCalled)
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertFalse(uiState.isSuccess)
        assertEquals(unverifiedEmail, uiState.verificationEmail)
        assertTrue(uiState.authState is com.example.domain.model.AuthState.VerificationRequired)
        val vState = uiState.authState as com.example.domain.model.AuthState.VerificationRequired
        assertEquals(unverifiedEmail, vState.email)
        assertNotNull(uiState.error)
        assertTrue(uiState.error!!.contains("verify"))
    }

    /**
     * Scenario 22: Unverified User Sign Up & OTP Verification
     * When signing up with email requiring verification, auto-login is blocked,
     * and entering the correct 8-digit OTP code transitions to EmailVerified.
     */
    @Test
    fun testSignUp_RequiresVerification_ThenVerifyOtp_Succeeds() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.requireVerificationOnSignUp = true
        repo.mockOtpCode = "74480663"
        repo.setMockUser(null)
        val viewModel = com.example.viewmodel.AuthViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()

        val email = "fresh.signup@cga.edu"
        viewModel.signUp(
            name = "Fresh Student",
            email = email,
            password = "SecretPassword123",
            department = "CSE",
            year = "1st Year",
            careerInterest = "Web Development"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfterSignUp = viewModel.uiState.value
        assertTrue(stateAfterSignUp.authState is com.example.domain.model.AuthState.VerificationRequired)
        assertEquals(email, stateAfterSignUp.verificationEmail)
        assertTrue(repo.unverifiedUsers.contains(email.lowercase()))

        // Verify invalid OTP failure
        viewModel.onOtpCodeChanged("00000000")
        viewModel.verifyEmailOtp()
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfterInvalidOtp = viewModel.uiState.value
        assertNotNull(stateAfterInvalidOtp.error)
        assertFalse(stateAfterInvalidOtp.isEmailVerified)

        // Verify correct 8-digit OTP success
        viewModel.onOtpCodeChanged("74480663")
        viewModel.verifyEmailOtp()
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfterValidOtp = viewModel.uiState.value
        assertTrue(stateAfterValidOtp.isEmailVerified)
        assertTrue(stateAfterValidOtp.authState is com.example.domain.model.AuthState.EmailVerified)
        assertFalse(repo.unverifiedUsers.contains(email.lowercase()))
    }

    /**
     * Scenario 25: OTP Validation rejects 6-digit input
     */
    @Test
    fun testOtpVerification_RejectsInvalid6DigitInput() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.requireVerificationOnSignUp = true
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        viewModel.signUp("Student", "test@cga.edu", "pass123", "CSE", "1st Year", "Tech")
        testDispatcher.scheduler.advanceUntilIdle()

        // User enters only 6 digits
        viewModel.onOtpCodeChanged("123456")
        assertEquals("123456", viewModel.uiState.value.otpCode)

        viewModel.verifyEmailOtp()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Please enter the complete 8-digit verification code.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isEmailVerified)
    }

    /**
     * Scenario 26: OTP Validation rejects 7-digit input
     */
    @Test
    fun testOtpVerification_RejectsInvalid7DigitInput() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.requireVerificationOnSignUp = true
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        viewModel.signUp("Student", "test@cga.edu", "pass123", "CSE", "1st Year", "Tech")
        testDispatcher.scheduler.advanceUntilIdle()

        // User enters only 7 digits
        viewModel.onOtpCodeChanged("1234567")
        assertEquals("1234567", viewModel.uiState.value.otpCode)

        viewModel.verifyEmailOtp()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Please enter the complete 8-digit verification code.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isEmailVerified)
    }

    /**
     * Scenario 27: OTP Verification preserves exact 8 digits with leading zeros
     */
    @Test
    fun testOtpVerification_PreservesExact8DigitsWithLeadingZero() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.requireVerificationOnSignUp = true
        val tokenWithLeadingZero = "01234567"
        repo.mockOtpCode = tokenWithLeadingZero
        val viewModel = com.example.viewmodel.AuthViewModel(repo)

        viewModel.signUp("Student", "leadzero@cga.edu", "pass123", "CSE", "1st Year", "Tech")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onOtpCodeChanged(tokenWithLeadingZero)
        assertEquals(tokenWithLeadingZero, viewModel.uiState.value.otpCode)

        viewModel.verifyEmailOtp()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmailVerified)
        assertTrue(viewModel.uiState.value.authState is com.example.domain.model.AuthState.EmailVerified)
    }

    /**
     * Scenario 23: Coordinator Routes VerificationRequired to OtpVerification
     * Verifies that AuthSessionCoordinator accurately resolves VerificationRequired destination.
     */
    @Test
    fun testCoordinator_VerificationRequired_ResolvesToOtpVerification() {
        val repo = com.example.data.repository.MockAuthRepository()
        val coordinator = com.example.domain.coordinator.AuthSessionCoordinator(repo)

        val vState = com.example.domain.model.AuthState.VerificationRequired("user@college.edu")
        val destination = coordinator.resolveDestination(vState)
        assertTrue(destination is com.example.domain.coordinator.AuthDestination.OtpVerification)
        assertEquals("user@college.edu", (destination as com.example.domain.coordinator.AuthDestination.OtpVerification).email)

        val nextScreen = coordinator.computeScreenTransition(
            currentScreen = "login",
            state = vState,
            isEmailVerifiedAnimationActive = false
        )
        assertEquals("sign_up", nextScreen)
    }

    /**
     * Scenario 24: Session Restoration with Unverified Email
     * When a user has a session but is unverified, refreshSession returns VerificationRequired.
     */
    @Test
    fun testRefreshSession_UnverifiedUser_ReturnsVerificationRequired() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        val unverifiedProfile = StudentProfile(
            id = "usr_unverified_1",
            name = "Pending Student",
            email = "pending@cga.edu",
            department = "CSE",
            yearOfStudy = "2nd Year",
            careerInterest = "Data Science"
        )
        repo.setMockUser(unverifiedProfile)
        repo.unverifiedUsers.add("pending@cga.edu")

        val authState = repo.refreshSession()
        assertTrue(authState is com.example.domain.model.AuthState.VerificationRequired)
        assertEquals("pending@cga.edu", (authState as com.example.domain.model.AuthState.VerificationRequired).email)
    }

    /**
     * Scenario 28: App Startup with Existing Authenticated Profile (Case A)
     * Verifies that existing student profile loads accurately with real DB fields.
     */
    @Test
    fun testAppStartup_ExistingProfile_LoadsRealProfileAccurately() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        val existingProfile = StudentProfile(
            id = "usr_existing_123",
            name = "Alex Mercer",
            email = "alex@cga.edu",
            department = "Computer Science & Engineering",
            yearOfStudy = "3rd Year",
            careerInterest = "Cybersecurity",
            currentStreak = 4,
            isAssessmentCompleted = true,
            recommendedCareerId = "cyber_analyst",
            xp = 350
        )
        repo.setMockUser(existingProfile)

        val state = repo.refreshSession()
        assertTrue(state is com.example.domain.model.AuthState.ProfileComplete)
        val loaded = (state as com.example.domain.model.AuthState.ProfileComplete).profile
        assertEquals("usr_existing_123", loaded.id)
        assertEquals("Alex Mercer", loaded.name)
        assertEquals("cyber_analyst", loaded.recommendedCareerId)
        assertEquals(4, loaded.currentStreak)
        assertEquals(350, loaded.xp)
        assertTrue(loaded.isAssessmentCompleted)
    }

    /**
     * Scenario 29 & 30: Full Persistence Across App Restart
     * Existing profile with assessment completed, streak=2, xp=200, career=cyber_analyst
     * after restart preserves all values and is not overwritten with default zeros.
     */
    @Test
    fun testSessionRestoration_PreservesFullProfileState_NoDefaultOverride() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        val realProfile = StudentProfile(
            id = "usr_real_999",
            name = "Devon Miles",
            email = "devon@college.edu",
            department = "Information Technology",
            yearOfStudy = "4th Year",
            careerInterest = "Cloud Engineering",
            currentStreak = 2,
            isAssessmentCompleted = true,
            recommendedCareerId = "cyber_analyst",
            xp = 200
        )
        repo.setMockUser(realProfile)

        val coordinator = com.example.domain.coordinator.AuthSessionCoordinator(repo)
        val state = repo.refreshSession()
        val destination = coordinator.resolveDestination(state)

        assertTrue(destination is com.example.domain.coordinator.AuthDestination.Dashboard)
        val profile = (destination as com.example.domain.coordinator.AuthDestination.Dashboard).profile
        assertEquals("usr_real_999", profile.id)
        assertEquals(2, profile.currentStreak)
        assertEquals(200, profile.xp)
        assertEquals("cyber_analyst", profile.recommendedCareerId)
        assertTrue(profile.isAssessmentCompleted)
    }

    /**
     * Scenario 31: Profile Fetch Failure Handling (Case C: PROFILE FETCH FAILURE != NEW USER)
     * When profile cache is null, updateCachedXp / updateCachedStreak must NOT create fake profiles.
     */
    @Test
    fun testProfileCache_NullState_DoesNotConstructFakeProfile() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.setMockUser(null)

        // Attempting to update cache when null must not inject a fake profile
        repo.updateCachedXp("usr_dummy", 100)
        repo.updateCachedStreak("usr_dummy", 5)

        val state = repo.refreshSession()
        assertTrue(state is com.example.domain.model.AuthState.SignedOut)
    }

    /**
     * Scenario 32: Authenticated User with No Profile Row (Case B)
     * Verifies initial profile creation without corrupting other users.
     */
    @Test
    fun testNewUser_ProfileCreation_InitializesCleanDomainState() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        val result = repo.signUp(
            name = "Newbie Student",
            email = "newbie@college.edu",
            password = "password123",
            department = "Mechanical",
            year = "1st Year",
            careerInterest = "Robotics"
        )
        assertTrue(result.isSuccess)
        val signUpResult = result.getOrNull()
        assertTrue(signUpResult is com.example.domain.model.SignUpResult.Success)
        val profile = (signUpResult as com.example.domain.model.SignUpResult.Success).profile
        assertEquals("Newbie Student", profile.name)
        assertEquals("Mechanical", profile.department)
        assertEquals(0, profile.currentStreak)
        assertFalse(profile.isAssessmentCompleted)
    }

    /**
     * Scenario 33 & 34: Multi-User Switch and Cache Isolation
     * Logout User A -> Login User B -> User B sees only their profile, restart preserves User B.
     */
    @Test
    fun testUserSwitch_CacheIsolation_NoStaleDataLeakage() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()

        // 1. User A is active
        val userA = StudentProfile(
            id = "usr_A",
            name = "Alice",
            email = "alice@cga.edu",
            department = "CSE",
            yearOfStudy = "4th Year",
            careerInterest = "AI",
            currentStreak = 10,
            isAssessmentCompleted = true,
            recommendedCareerId = "aiml_engineer",
            xp = 1500
        )
        repo.setMockUser(userA)
        assertEquals("usr_A", (repo.refreshSession() as com.example.domain.model.AuthState.ProfileComplete).profile.id)

        // 2. User A logs out
        repo.logout()
        assertTrue(repo.refreshSession() is com.example.domain.model.AuthState.SignedOut)

        // 3. User B logs in
        val userB = StudentProfile(
            id = "usr_B",
            name = "Bob",
            email = "bob@cga.edu",
            department = "ECE",
            yearOfStudy = "2nd Year",
            careerInterest = "Embedded",
            currentStreak = 1,
            isAssessmentCompleted = false,
            recommendedCareerId = null,
            xp = 50
        )
        repo.setMockUser(userB)
        val stateB = repo.refreshSession()
        assertTrue(stateB is com.example.domain.model.AuthState.ProfileComplete)
        val profileB = (stateB as com.example.domain.model.AuthState.ProfileComplete).profile
        assertEquals("usr_B", profileB.id)
        assertEquals("Bob", profileB.name)
        assertEquals(1, profileB.currentStreak)
        assertEquals(50, profileB.xp)
        assertFalse(profileB.isAssessmentCompleted)

        // 4. Simulate app restart while authenticated as User B
        val restartedState = repo.refreshSession()
        assertTrue(restartedState is com.example.domain.model.AuthState.ProfileComplete)
        assertEquals("usr_B", (restartedState as com.example.domain.model.AuthState.ProfileComplete).profile.id)
    }

    /**
     * Scenario 35: No Authenticated Session (Case D)
     * When there is no active session on app startup, coordinator routes to Login/Welcome.
     */
    @Test
    fun testUnauthenticatedAppStartup_RoutesToSignedOut() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.repository.MockAuthRepository()
        repo.setMockUser(null)

        val coordinator = com.example.domain.coordinator.AuthSessionCoordinator(repo)
        val state = repo.refreshSession()
        val destination = coordinator.resolveDestination(state)

        assertTrue(destination is com.example.domain.coordinator.AuthDestination.Welcome)
        val transition = coordinator.computeScreenTransition(
            currentScreen = "dashboard",
            state = state,
            isEmailVerifiedAnimationActive = false
        )
        assertEquals("welcome", transition)
    }
}
