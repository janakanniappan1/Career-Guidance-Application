package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * CGA - Career Guidance Application Exact Design System Colors
 * Source of Truth from CGA Design Specification
 */
object CGAColors {
    // BACKGROUNDS & SURFACES (Original Clean Palette)
    val AppBackground = Color(0xFFFFFFFF)
    val ContentBackground = Color(0xFFF7F9FA)
    val OuterBackground = Color(0xFFF0F3F4)
    val Surface = Color(0xFFFFFFFF)
    val MutedSurface = Color(0xFFF7F9FA)

    // PRIMARY CGA GREEN BRANDING (#58CC02, dark #46A302, light #78E119)
    val Primary = Color(0xFF58CC02)
    val PrimaryPressed = Color(0xFF46A302)
    val PrimaryLight = Color(0xFF78E119)
    val LightGreen = Color(0xFFE8F8D8)
    val GreenBorder = Color(0xFFB8EE86)

    // ACTION BUTTON SEMANTIC SYSTEM (Original Green Identity)
    val ActionBlue = Color(0xFF58CC02)
    val ActionBluePressed = Color(0xFF46A302)
    val ActionBlueLight = Color(0xFF78E119)

    // Purple for AI-specific actions
    val ActionPurple = Color(0xFF7C3AED)
    val ActionPurplePressed = Color(0xFF6D28D9)

    // PURPLE / INDIGO — AI
    val ElectricPurple = Color(0xFF7C3AED)
    val DeepPurple = Color(0xFF6D28D9)
    val LightPurpleIconBg = Color(0xFFF5F3FF)
    val LightPurpleIcon = Color(0xFF7C3AED)
    val AiPurpleGradientEnd = Color(0xFFA855F7)

    // CYAN — INFORMATION + PROGRESS
    val Cyan = Color(0xFF0891B2)
    val LightCyan = Color(0xFFECFEFF)
    val CyanGradientEnd = Color(0xFF06B6D4)

    // AMBER — XP + STREAK + GAMIFICATION
    val GamificationOrange = Color(0xFFF59E0B)
    val DarkAmber = Color(0xFFD97706)
    val LightAmber = Color(0xFFFFFBEB)
    val RewardGradientEnd = Color(0xFFFBBF24)

    // GREEN — SUCCESS / COMPLETED ONLY (#58CC02 / #46A302)
    val BrandGreen = Color(0xFF58CC02)
    val SuccessGreen = Color(0xFF58CC02)
    val SuccessGreenPressed = Color(0xFF46A302)
    val SuccessGreenLight = Color(0xFFE8F8D8)

    // PINK — SPECIAL HIGHLIGHTS ONLY
    val Pink = Color(0xFFEC4899)
    val LightPink = Color(0xFFFDF2F8)

    // YELLOW — ATTENTION / HIGHLIGHT ONLY
    val Yellow = Color(0xFFEAB308)
    val LightYellow = Color(0xFFFEFCE8)

    // ACCENTS / COMPATIBILITY
    val ProfessionalBlue = Color(0xFF0A66C2)
    val LightBlue = Color(0xFFEFF6FF)
    val BlueBorder = Color(0xFFBFDBFE)
    val PrimaryBlueGradientEnd = Color(0xFF3B82F6)

    // TEXT
    val TextHeading = Color(0xFF1E293B)
    val TextBody = Color(0xFF475569)
    val TextMuted = Color(0xFF94A3B8)
    val TextSecondary = Color(0xFF64748B)

    // BORDERS
    val BorderMain = Color(0xFFE2E8F0)
    val BorderActive = Color(0xFFCBD5E1)

    // PROGRESS (Vibrant Green for milestones & progress)
    val ProgressTrack = Color(0xFFE2E8F0)
    val ProgressStart = Color(0xFF58CC02)
    val ProgressEnd = Color(0xFF78E119)

    // SPECIAL / FEATURE BACKGROUNDS & ICONS
    val LightBlueTagBg = Color(0xFFEFF6FF)
    val LightBlueTagText = Color(0xFF0A66C2)

    val LightBlueIconBg = Color(0xFFEFF6FF)
    val LightBlueIcon = Color(0xFF0A66C2)

    // Completed & Success states (Original Green)
    val LightGreenIconBg = Color(0xFFE8F8D8)
    val LightGreenIcon = Color(0xFF58CC02)

    val LightOrangeIconBg = Color(0xFFFFFBEB)
    val LightOrangeIcon = Color(0xFFD97706)

    // STREAK & XP
    val StreakBg = Color(0xFFFFFBEB)
    val StreakBorder = Color(0xFFFDE68A)
    val StreakIcon = Color(0xFFD97706)
    val Star = Color(0xFFF59E0B)

    // MENTOR AVATARS
    val MentorPurpleBg = Color(0xFFF5F3FF)
    val MentorPurpleIcon = Color(0xFF7C3AED)
    val MentorGreenBg = Color(0xFFE8F8D8)
    val MentorGreenIcon = Color(0xFF58CC02)

    // DEVICE & NEUTRALS
    val DeviceFrame = Color(0xFF1E293B)
    val White = Color(0xFFFFFFFF)

    // ALIASES FOR COMPATIBILITY
    val Purple = ElectricPurple
    val DarkHeading = TextHeading
    val MutedBody = TextBody
    val BorderLight = BorderMain
    val Error = Color(0xFFEF4444)
    val LightError = Color(0xFFFEF2F2)

    // DARK MODE EQUIVALENTS
    val DarkAppBackground = Color(0xFF121820)
    val DarkContentBackground = Color(0xFF182230)
    val DarkSurface = Color(0xFF1A2636)
    val DarkMutedSurface = Color(0xFF243447)
    val DarkBorderMain = Color(0xFF2E425A)
    val DarkBorderActive = Color(0xFF3B5370)
    val DarkTextHeading = Color(0xFFF8FAFC)
    val DarkTextBody = Color(0xFFCBD5E1)
    val DarkTextMuted = Color(0xFF94A3B8)
    val DarkProgressTrack = Color(0xFF2E425A)
}

/**
 * CGA Gradient System (Section 11)
 * Subtle, modern gradients for tactile depth and technological polish
 */
object CGAGradients {
    // Primary CGA Green: #58CC02 -> #78E119
    val PrimaryBlue = Brush.linearGradient(
        listOf(Color(0xFF58CC02), Color(0xFF78E119))
    )

    // AI Purple: #7C3AED -> #A855F7
    val AiPurple = Brush.linearGradient(
        listOf(Color(0xFF7C3AED), Color(0xFFA855F7))
    )

    // Cyan / Learning: #0891B2 -> #06B6D4
    val CyanLearning = Brush.linearGradient(
        listOf(Color(0xFF0891B2), Color(0xFF06B6D4))
    )

    // Reward / Gamification: #F59E0B -> #FBBF24
    val RewardAmber = Brush.linearGradient(
        listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
    )

    // Neutral Light Card Gradient
    val NeutralLight = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))
    )
}

