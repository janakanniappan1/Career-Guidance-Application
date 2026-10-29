package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class CGADesignTokens(
    val isDark: Boolean,
    val appBackground: Color,
    val contentBackground: Color,
    val surface: Color,
    val mutedSurface: Color,
    val borderMain: Color,
    val borderActive: Color,
    val textHeading: Color,
    val textBody: Color,
    val textMuted: Color,
    val primary: Color = CGAColors.Primary,
    val primaryPressed: Color = CGAColors.PrimaryPressed,
    val professionalBlue: Color = CGAColors.ProfessionalBlue,
    val electricPurple: Color = CGAColors.ElectricPurple,
    val gamificationOrange: Color = CGAColors.GamificationOrange,
    val progressTrack: Color,
    val infoContainer: Color = CGAColors.LightGreenIconBg,
    val onInfoContainer: Color = CGAColors.Primary,
    val successContainer: Color = CGAColors.LightGreenIconBg,
    val onSuccessContainer: Color = CGAColors.SuccessGreen,
    val warningContainer: Color = CGAColors.LightOrangeIconBg,
    val onWarningContainer: Color = CGAColors.GamificationOrange,
    val purpleContainer: Color = CGAColors.LightPurpleIconBg,
    val onPurpleContainer: Color = CGAColors.LightPurpleIcon,
    val streakBg: Color = CGAColors.StreakBg,
    val streakBorder: Color = CGAColors.StreakBorder,
    val streakText: Color = CGAColors.StreakIcon
)

typealias CGATokens = CGADesignTokens

val LocalCGATokens = staticCompositionLocalOf {
    CGADesignTokens(
        isDark = false,
        appBackground = CGAColors.AppBackground,
        contentBackground = CGAColors.ContentBackground,
        surface = CGAColors.Surface,
        mutedSurface = CGAColors.MutedSurface,
        borderMain = CGAColors.BorderMain,
        borderActive = CGAColors.BorderActive,
        textHeading = CGAColors.TextHeading,
        textBody = CGAColors.TextBody,
        textMuted = CGAColors.TextMuted,
        progressTrack = CGAColors.ProgressTrack
    )
}

private val LightColorScheme = lightColorScheme(
    primary = CGAColors.Primary,
    onPrimary = Color.White,
    primaryContainer = CGAColors.LightGreenIconBg,
    onPrimaryContainer = CGAColors.PrimaryPressed,
    secondary = CGAColors.PrimaryPressed,
    onSecondary = Color.White,
    secondaryContainer = CGAColors.LightGreenIconBg,
    onSecondaryContainer = CGAColors.Primary,
    tertiary = CGAColors.ElectricPurple,
    onTertiary = Color.White,
    background = CGAColors.AppBackground,
    surface = CGAColors.Surface,
    surfaceVariant = CGAColors.ContentBackground,
    onBackground = CGAColors.TextHeading,
    onSurface = CGAColors.TextHeading,
    onSurfaceVariant = CGAColors.TextBody,
    outline = CGAColors.BorderMain
)

private val DarkColorScheme = darkColorScheme(
    primary = CGAColors.Primary,
    onPrimary = Color.White,
    primaryContainer = CGAColors.DarkMutedSurface,
    onPrimaryContainer = Color.White,
    secondary = CGAColors.PrimaryPressed,
    onSecondary = Color.White,
    secondaryContainer = CGAColors.DarkMutedSurface,
    onSecondaryContainer = Color.White,
    tertiary = CGAColors.ElectricPurple,
    onTertiary = Color.White,
    background = CGAColors.DarkAppBackground,
    surface = CGAColors.DarkSurface,
    surfaceVariant = CGAColors.DarkContentBackground,
    onBackground = CGAColors.DarkTextHeading,
    onSurface = CGAColors.DarkTextHeading,
    onSurfaceVariant = CGAColors.DarkTextBody,
    outline = CGAColors.DarkBorderMain
)

@Composable
fun CGATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val tokens = if (darkTheme) {
        CGADesignTokens(
            isDark = true,
            appBackground = CGAColors.DarkAppBackground,
            contentBackground = CGAColors.DarkContentBackground,
            surface = CGAColors.DarkSurface,
            mutedSurface = CGAColors.DarkMutedSurface,
            borderMain = CGAColors.DarkBorderMain,
            borderActive = CGAColors.DarkBorderActive,
            textHeading = CGAColors.DarkTextHeading,
            textBody = CGAColors.DarkTextBody,
            textMuted = CGAColors.DarkTextMuted,
            progressTrack = CGAColors.DarkProgressTrack,
            infoContainer = Color(0xFF162842),
            onInfoContainer = Color(0xFF93C5FD),
            successContainer = Color(0xFF143820),
            onSuccessContainer = Color(0xFF4ADE80),
            warningContainer = Color(0xFF382512),
            onWarningContainer = Color(0xFFFBBF24),
            purpleContainer = Color(0xFF281F45),
            onPurpleContainer = Color(0xFFA78BFA),
            streakBg = Color(0xFF332408),
            streakBorder = Color(0xFF6B4B0A),
            streakText = Color(0xFFFFB74D)
        )
    } else {
        CGADesignTokens(
            isDark = false,
            appBackground = CGAColors.AppBackground,
            contentBackground = CGAColors.ContentBackground,
            surface = CGAColors.Surface,
            mutedSurface = CGAColors.MutedSurface,
            borderMain = CGAColors.BorderMain,
            borderActive = CGAColors.BorderActive,
            textHeading = CGAColors.TextHeading,
            textBody = CGAColors.TextBody,
            textMuted = CGAColors.TextMuted,
            progressTrack = CGAColors.ProgressTrack,
            infoContainer = CGAColors.LightGreenIconBg,
            onInfoContainer = CGAColors.Primary,
            successContainer = CGAColors.LightGreenIconBg,
            onSuccessContainer = CGAColors.SuccessGreen,
            warningContainer = CGAColors.LightOrangeIconBg,
            onWarningContainer = CGAColors.GamificationOrange,
            purpleContainer = CGAColors.LightPurpleIconBg,
            onPurpleContainer = CGAColors.LightPurpleIcon,
            streakBg = CGAColors.StreakBg,
            streakBorder = CGAColors.StreakBorder,
            streakText = CGAColors.StreakIcon
        )
    }

    CompositionLocalProvider(LocalCGATokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CGATypography,
            content = content
        )
    }
}

/**
 * Backward compatibility alias for existing project references
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CGATheme(darkTheme = darkTheme, content = content)
}
