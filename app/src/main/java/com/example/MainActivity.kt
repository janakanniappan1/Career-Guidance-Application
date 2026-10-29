package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.example.data.pdf.PdfExtractorImpl
import com.example.data.repository.LocalKnowledgeBaseRepository
import com.example.data.repository.LocalLearningMaterialRepository
import com.example.data.repository.SupabaseLearningTutorRepository
import com.example.data.remote.SupabaseClientProvider
import com.example.data.repository.SupabaseAssessmentRepository
import com.example.data.repository.SupabaseCareerRepository
import com.example.data.repository.MockAuthRepository
import com.example.data.repository.SupabaseAiGuidanceRepository
import com.example.data.repository.SupabaseAiRoadmapGuidanceRepository
import com.example.data.repository.SupabaseAuthRepository
import com.example.data.repository.SupabaseLeaderboardRepository
import com.example.data.repository.SupabaseMentorRepository
import com.example.data.repository.SupabasePracticeRepository
import com.example.data.repository.SupabaseRoadmapRepository
import com.example.domain.coordinator.AuthSessionCoordinator
import com.example.domain.model.AuthState
import com.example.ui.screens.CompleteProfileScreen
import com.example.util.CareerCanonicalUtil
import io.github.jan.supabase.auth.handleDeeplinks
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AssessmentResultScreen
import com.example.ui.screens.AssessmentScreen
import com.example.ui.screens.CareerDetailScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.LearningCompanionScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MentorChatScreen
import com.example.ui.screens.PortfolioScreen
import com.example.ui.screens.PracticeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.ResumeScreen
import com.example.ui.screens.RoadmapScreen
import com.example.ui.screens.SignUpScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.CGATheme
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.AssessmentViewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.CareerViewModel
import com.example.viewmodel.DashboardViewModel
import com.example.viewmodel.LeaderboardViewModel
import com.example.viewmodel.LearningCompanionViewModel
import com.example.viewmodel.MentorChatViewModel
import com.example.viewmodel.PracticeViewModel
import com.example.viewmodel.RoadmapViewModel

class MainActivity : ComponentActivity() {
    private val authRepository by lazy { SupabaseAuthRepository() }
    private val authViewModel by lazy { AuthViewModel(authRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseClientProvider.init(applicationContext)
        handleAuthDeeplink(intent)
        enableEdgeToEdge()
        setContent {
            CGATheme {
                val tokens = LocalCGATokens.current
                DisposableEffect(tokens.isDark) {
                    enableEdgeToEdge(
                        statusBarStyle = if (tokens.isDark) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT
                            )
                        },
                        navigationBarStyle = if (tokens.isDark) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT
                            )
                        }
                    )
                    onDispose {}
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = tokens.contentBackground
                ) {
                    CgaApp(
                        authRepository = authRepository,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthDeeplink(intent)
    }

    private fun handleAuthDeeplink(intent: Intent?) {
        if (intent == null) return
        val data = intent.data ?: return
        Log.d("MainActivity", "handleAuthDeeplink received URI: $data (scheme=${data.scheme}, host=${data.host})")
        if (data.scheme?.equals("cga", ignoreCase = true) == true && data.host?.equals("auth-callback", ignoreCase = true) == true) {
            Log.d("MainActivity", "Processing Supabase auth callback: $data")
            val errorParam = data.getQueryParameter("error")
            val errorDesc = data.getQueryParameter("error_description")
            if (errorParam != null || errorDesc != null) {
                val errorMsg = errorDesc ?: errorParam ?: "Google authentication was not completed."
                Log.w("MainActivity", "OAuth error parameter received in callback: $errorMsg")
                authViewModel.onOAuthCallbackError(errorMsg)
                return
            }

            try {
                SupabaseClientProvider.client.handleDeeplinks(intent) { session ->
                    Log.d("MainActivity", "Deep link auth success for user: ${session.user?.id}")
                }
            } catch (e: Throwable) {
                Log.e("MainActivity", "Failed to handle deep link", e)
                authViewModel.onOAuthCallbackError("OAuth exchange failed: ${e.message}")
            }
        }
    }
}

@Composable
fun CgaApp(
    authRepository: SupabaseAuthRepository = remember { SupabaseAuthRepository() },
    authViewModel: AuthViewModel = remember(authRepository) { AuthViewModel(authRepository) }
) {
    val authCoordinator = remember(authRepository) { AuthSessionCoordinator(authRepository) }

    // Repository Layer (Supabase Auth, Assessment, Career, and Roadmap live on Supabase PostgREST)
    val assessmentRepository = remember { SupabaseAssessmentRepository() }
    val careerRepository = remember { SupabaseCareerRepository(assessmentRepository) }
    val roadmapRepository = remember { SupabaseRoadmapRepository(authRepository) }
    val practiceRepository = remember { SupabasePracticeRepository(authRepository) }
    val aiGuidanceRepository = remember { SupabaseAiGuidanceRepository() }
    val aiRoadmapGuidanceRepository = remember { SupabaseAiRoadmapGuidanceRepository() }

    // ViewModel Layer (MVVM)
    val dashboardViewModel = remember {
        DashboardViewModel(authRepository, careerRepository, roadmapRepository, practiceRepository)
    }
    val assessmentViewModel = remember {
        AssessmentViewModel(assessmentRepository, careerRepository, authRepository)
    }
    val careerViewModel = remember { CareerViewModel(careerRepository, aiGuidanceRepository, authRepository) }
    val roadmapViewModel = remember {
        RoadmapViewModel(roadmapRepository, aiRoadmapGuidanceRepository, authRepository)
    }
    val practiceViewModel = remember { PracticeViewModel(practiceRepository, careerRepository, authRepository) }
    val leaderboardRepository = remember {
        SupabaseLeaderboardRepository(authRepository, roadmapRepository)
    }
    val leaderboardViewModel = remember {
        LeaderboardViewModel(leaderboardRepository, authRepository)
    }
    val mentorRepository = remember {
        SupabaseMentorRepository(authRepository)
    }
    val mentorChatViewModel = remember {
        MentorChatViewModel(mentorRepository, authRepository, careerRepository, roadmapRepository, practiceRepository)
    }
    val context = LocalContext.current
    val learningMaterialRepository = remember {
        LocalLearningMaterialRepository(
            pdfExtractor = PdfExtractorImpl(context),
            pptxExtractor = com.example.data.pptx.PptxExtractorImpl(context),
            videoExtractor = com.example.data.video.VideoExtractorImpl(context),
            storageDir = context.filesDir
        )
    }
    val knowledgeBaseRepository = remember {
        LocalKnowledgeBaseRepository(storageDir = context.filesDir)
    }
    val learningTutorRepository = remember {
        SupabaseLearningTutorRepository()
    }
    val adaptiveAssessmentRepository = remember {
        com.example.data.repository.SupabaseAdaptiveAssessmentRepository()
    }
    val learnerModelRepository = remember {
        com.example.data.repository.LocalLearnerModelRepository(storageDir = context.filesDir)
    }
    val learningCompanionViewModel = remember {
        LearningCompanionViewModel(
            authRepository,
            learningMaterialRepository,
            knowledgeBaseRepository,
            learningTutorRepository,
            adaptiveAssessmentRepository,
            learnerModelRepository
        )
    }

    // Authentication session observer
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // Navigation state
    var currentScreen by rememberSaveable { mutableStateOf("splash") }
    var selectedCareerId by rememberSaveable { mutableStateOf<String?>(null) }
    val backStack = remember { mutableStateListOf<String>() }

    fun navigateTo(screen: String) {
        backStack.add(currentScreen)
        currentScreen = screen
    }

    fun navigateBack() {
        if (backStack.isNotEmpty()) {
            currentScreen = backStack.removeAt(backStack.size - 1)
        } else {
            currentScreen = "dashboard"
        }
    }

    // Centralized Authentication-driven navigation reacting to verified sessions, profile completeness, and sign out
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            mentorChatViewModel.refreshStudentContext()
        }
    }

    LaunchedEffect(authState) {
        val nextScreen = authCoordinator.computeScreenTransition(
            currentScreen = currentScreen,
            state = authState,
            isEmailVerifiedAnimationActive = authViewModel.uiState.value.isEmailVerified
        )
        if (nextScreen != null) {
            backStack.clear()
            currentScreen = nextScreen
        }
    }

    BackHandler(enabled = currentScreen != "splash" && currentScreen != "welcome" && currentScreen != "dashboard" && currentScreen != "complete_profile") {
        navigateBack()
    }

    Crossfade(
        targetState = currentScreen,
        animationSpec = tween(280),
        label = "screen_crossfade"
    ) { screen ->
        when (screen) {
            "splash" -> {
                SplashScreen(
                    authState = authState,
                    onNavigate = { destination ->
                        backStack.clear()
                        currentScreen = destination
                    }
                )
            }

        "welcome" -> {
            WelcomeScreen(
                onNavigateToLogin = { 
                    authViewModel.clearError()
                    navigateTo("login") 
                },
                onNavigateToSignUp = { 
                    authViewModel.clearError()
                    navigateTo("sign_up") 
                }
            )
        }

        "login" -> {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateBack = { 
                    authViewModel.clearError()
                    navigateBack() 
                },
                onNavigateToDashboard = {
                    backStack.clear()
                    if (currentUser?.isProfileComplete == true) {
                        currentScreen = "dashboard"
                    } else {
                        currentScreen = "complete_profile"
                    }
                },
                onNavigateToSignUp = { 
                    authViewModel.clearError()
                    navigateTo("sign_up") 
                }
            )
        }

        "sign_up" -> {
            SignUpScreen(
                authViewModel = authViewModel,
                onNavigateBack = { 
                    authViewModel.clearError()
                    navigateBack() 
                },
                onNavigateToDashboard = {
                    backStack.clear()
                    currentScreen = "dashboard"
                },
                onNavigateToCompleteProfile = {
                    backStack.clear()
                    currentScreen = "complete_profile"
                },
                onNavigateToLogin = { 
                    authViewModel.clearError()
                    navigateTo("login") 
                }
            )
        }

        "complete_profile" -> {
            CompleteProfileScreen(
                authViewModel = authViewModel,
                onNavigateToDashboard = {
                    backStack.clear()
                    currentScreen = "dashboard"
                }
            )
        }

        "dashboard" -> {
            DashboardScreen(
                dashboardViewModel = dashboardViewModel,
                authViewModel = authViewModel,
                onNavigateToAssessment = { navigateTo("assessment") },
                onNavigateToResults = { navigateTo("results") },
                onNavigateToRoadmap = { careerId ->
                    selectedCareerId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
                    navigateTo("roadmap")
                },
                onNavigateToPractice = { navigateTo("practice") },
                onNavigateToExplore = { navigateTo("explore") },
                onNavigateToProfile = { navigateTo("profile") },
                onNavigateToQuiz = {
                    practiceViewModel.startSpeedChallenge()
                    navigateTo("quiz")
                },
                onNavigateToResume = { navigateTo("resume") },
                onNavigateToPortfolio = { navigateTo("portfolio") },
                onNavigateToLeaderboard = { navigateTo("leaderboard") },
                onNavigateToMentorChat = { navigateTo("mentor_chat") },
                onNavigateToLearningCompanion = { navigateTo("learning_companion") },
                onLogout = {
                    roadmapRepository.clearSession()
                    roadmapViewModel.clearState()
                    selectedCareerId = null
                    backStack.clear()
                    currentScreen = "welcome"
                }
            )
        }

        "assessment" -> {
            AssessmentScreen(
                assessmentViewModel = assessmentViewModel,
                onNavigateBack = { navigateBack() },
                onAssessmentComplete = { recommendation ->
                    careerViewModel.setRecommendation(recommendation)
                    selectedCareerId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(recommendation.careerId)
                    currentScreen = "results"
                }
            )
        }

        "results" -> {
            AssessmentResultScreen(
                careerViewModel = careerViewModel,
                onNavigateBack = {
                    backStack.clear()
                    currentScreen = "dashboard"
                },
                onNavigateToRoadmap = { careerId ->
                    selectedCareerId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
                    navigateTo("roadmap")
                },
                onNavigateToCareerDetails = { careerId ->
                    selectedCareerId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
                    navigateTo("career_details")
                }
            )
        }

        "career_details" -> {
            CareerDetailScreen(
                careerId = selectedCareerId ?: CareerCanonicalUtil.toCanonicalCareerId(currentUser?.recommendedCareerId),
                careerViewModel = careerViewModel,
                onNavigateBack = { navigateBack() },
                onNavigateToRoadmap = { careerId ->
                    selectedCareerId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
                    navigateTo("roadmap")
                }
            )
        }

        "roadmap" -> {
            RoadmapScreen(
                careerId = selectedCareerId ?: CareerCanonicalUtil.toCanonicalCareerIdOrNull(currentUser?.recommendedCareerId),
                roadmapViewModel = roadmapViewModel,
                onNavigateBack = { navigateBack() },
                onNavigateToHome = {
                    backStack.clear()
                    currentScreen = "dashboard"
                },
                onNavigateToPractice = { navigateTo("practice") },
                onNavigateToExplore = { navigateTo("explore") },
                onNavigateToProfile = { navigateTo("profile") },
                onNavigateToAssessment = { navigateTo("assessment") }
            )
        }

        "explore" -> {
            ExploreScreen(
                careerViewModel = careerViewModel,
                onNavigateBack = { navigateBack() },
                onNavigateToHome = {
                    backStack.clear()
                    currentScreen = "dashboard"
                },
                onNavigateToRoadmap = { careerId ->
                    selectedCareerId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
                    navigateTo("roadmap")
                },
                onNavigateToPractice = { navigateTo("practice") },
                onNavigateToProfile = { navigateTo("profile") },
                onNavigateToCareerDetails = { careerId ->
                    selectedCareerId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
                    navigateTo("career_details")
                }
            )
        }

        "practice" -> {
            PracticeScreen(
                practiceViewModel = practiceViewModel,
                careerId = selectedCareerId ?: CareerCanonicalUtil.toCanonicalCareerIdOrNull(currentUser?.recommendedCareerId),
                onNavigateBack = { navigateBack() },
                onNavigateToHome = {
                    backStack.clear()
                    currentScreen = "dashboard"
                },
                onNavigateToRoadmap = { careerId ->
                    selectedCareerId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
                    navigateTo("roadmap")
                },
                onNavigateToExplore = { navigateTo("explore") },
                onNavigateToProfile = { navigateTo("profile") },
                onNavigateToQuiz = { navigateTo("quiz") }
            )
        }

        "quiz" -> {
            QuizScreen(
                practiceViewModel = practiceViewModel,
                onNavigateBack = { navigateBack() }
            )
        }

        "profile" -> {
            ProfileScreen(
                dashboardViewModel = dashboardViewModel,
                authViewModel = authViewModel,
                onNavigateBack = { navigateBack() },
                onNavigateToHome = {
                    backStack.clear()
                    currentScreen = "dashboard"
                },
                onNavigateToRoadmap = { careerId ->
                    selectedCareerId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
                    navigateTo("roadmap")
                },
                onNavigateToPractice = { navigateTo("practice") },
                onNavigateToExplore = { navigateTo("explore") },
                onNavigateToResume = { navigateTo("resume") },
                onNavigateToPortfolio = { navigateTo("portfolio") },
                onNavigateToAdmin = { navigateTo("admin") },
                onNavigateToLeaderboard = { navigateTo("leaderboard") },
                onNavigateToLearningCompanion = { navigateTo("learning_companion") },
                onLogout = {
                    roadmapRepository.clearSession()
                    roadmapViewModel.clearState()
                    selectedCareerId = null
                    backStack.clear()
                    currentScreen = "welcome"
                }
            )
        }

        "leaderboard" -> {
            LeaderboardScreen(
                leaderboardViewModel = leaderboardViewModel,
                onNavigateBack = { navigateBack() },
                onNavigateToHome = {
                    backStack.clear()
                    currentScreen = "dashboard"
                },
                onNavigateToRoadmap = { careerId ->
                    selectedCareerId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
                    navigateTo("roadmap")
                },
                onNavigateToPractice = { navigateTo("practice") },
                onNavigateToExplore = { navigateTo("explore") },
                onNavigateToProfile = { navigateTo("profile") }
            )
        }

        "admin" -> {
            AdminScreen(
                practiceViewModel = practiceViewModel,
                onNavigateBack = { navigateBack() }
            )
        }

        "resume" -> {
            ResumeScreen(
                practiceViewModel = practiceViewModel,
                onNavigateBack = { navigateBack() }
            )
        }

        "portfolio" -> {
            PortfolioScreen(
                practiceViewModel = practiceViewModel,
                onNavigateBack = { navigateBack() }
            )
        }

        "mentor_chat" -> {
            MentorChatScreen(
                viewModel = mentorChatViewModel,
                onNavigateBack = { navigateBack() }
            )
        }

        "learning_companion" -> {
            LearningCompanionScreen(
                viewModel = learningCompanionViewModel,
                onNavigateBack = { navigateBack() },
                onNavigateToDashboard = {
                    backStack.clear()
                    currentScreen = "dashboard"
                },
                onNavigateToRoadmap = { careerId ->
                    selectedCareerId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
                    navigateTo("roadmap")
                },
                onNavigateToPractice = { navigateTo("practice") },
                onNavigateToExplore = { navigateTo("explore") },
                onNavigateToProfile = { navigateTo("profile") }
            )
        }
    }
}
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
