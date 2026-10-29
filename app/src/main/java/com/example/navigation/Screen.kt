package com.example.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object SignUp : Screen("sign_up")
    object Dashboard : Screen("dashboard")
    object Assessment : Screen("assessment")
    object AssessmentResults : Screen("assessment_results")
    object Roadmap : Screen("roadmap")
    object Explore : Screen("explore")
    object Practice : Screen("practice")
    object Profile : Screen("profile")
    object Admin : Screen("admin")
    object Quiz : Screen("quiz")
    object Resume : Screen("resume")
    object Portfolio : Screen("portfolio")
    object CareerDetails : Screen("career_details")
    object Leaderboard : Screen("leaderboard")
    object MentorChat : Screen("mentor_chat")
    object LearningCompanion : Screen("learning_companion")
}
