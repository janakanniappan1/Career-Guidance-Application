package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CGAColors
import com.example.ui.theme.CGAGradients
import com.example.ui.theme.LocalCGATokens

/**
 * Official Centralized CGA Logo Component
 * Renders the official uploaded bitmap image (cga_logo_original.png) with ContentScale.Fit.
 */
@Composable
fun CgaLogo(
    modifier: Modifier = Modifier,
    contentDescription: String = "CGA Logo"
) {
    Image(
        painter = painterResource(id = R.drawable.cga_logo_original),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

/**
 * Button Visual Variants
 * - Primary: CGA Green #58CC02 with pressed shadow #46A302 (Original CGA Identity)
 * - Ai: Purple #7C3AED with pressed shadow #6D28D9 (AI guidance actions)
 * - Reward: Amber #F59E0B with pressed shadow #D97706 (XP / Streaks)
 * - Success: Green #58CC02 with pressed shadow #46A302 (Completion states)
 * - Destructive: Red #EF4444 with pressed shadow #B91C1C
 */
enum class CgaButtonVariant {
    Primary,
    Ai,
    Blue,
    Cyan,
    Reward,
    Success,
    Destructive,
    Retest
}

/**
 * Tactical Chunky Primary CGA Button
 * - Background: Dynamic based on variant (default Green #58CC02)
 * - Bottom border / shadow: 4px tactile depth (default #46A302)
 * - Corner radius: 16dp
 * - Font: 14sp 800 weight, UPPERCASE, letter spacing 0.5sp
 * - Micro-animation: Scale to ~0.98 and 3dp shift on press
 */
@Composable
fun CgaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: CgaButtonVariant = CgaButtonVariant.Primary,
    leadingIcon: ImageVector? = null,
    testTag: String = "cga_primary_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_scale"
    )
    val offsetY by animateDpAsState(
        targetValue = if (isPressed && enabled) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 80),
        label = "btn_offset"
    )

    val (baseBgColor, shadowColor) = if (enabled) {
        when (variant) {
            CgaButtonVariant.Primary -> CGAColors.Primary to CGAColors.PrimaryPressed
            CgaButtonVariant.Ai -> Color(0xFF7C3AED) to Color(0xFF6D28D9)
            CgaButtonVariant.Blue -> Color(0xFF2563EB) to Color(0xFF1D4ED8)
            CgaButtonVariant.Cyan -> Color(0xFF0891B2) to Color(0xFF0E7490)
            CgaButtonVariant.Reward -> CGAColors.GamificationOrange to CGAColors.DarkAmber
            CgaButtonVariant.Success -> CGAColors.SuccessGreen to CGAColors.SuccessGreenPressed
            CgaButtonVariant.Destructive -> Color(0xFFEF4444) to Color(0xFFB91C1C)
            CgaButtonVariant.Retest -> Color(0xFFB91C1C) to Color(0xFF991B1B)
        }
    } else {
        Color(0xFFCBD5E1) to Color(0xFF94A3B8)
    }

    val activeBgColor = if (isPressed && enabled && variant == CgaButtonVariant.Retest) {
        Color(0xFF991B1B)
    } else {
        baseBgColor
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(scale)
            .testTag(testTag)
    ) {
        // Bottom 3D shadow block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .offset(y = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shadowColor)
        )

        // Top button face
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .offset(y = offsetY)
                .clip(RoundedCornerShape(16.dp))
                .background(activeBgColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = text.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (text.length > 20) 12.sp else 13.sp,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Tactical Chunky Secondary CGA Button
 * - White/Surface background
 * - 2px main border, 4px bottom border
 * - Text: tokens.textHeading
 * - Micro-animation: subtle scale and 3dp shift on press
 */
@Composable
fun CgaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    testTag: String = "cga_secondary_button"
) {
    val tokens = LocalCGATokens.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_sec_scale"
    )
    val offsetY by animateDpAsState(
        targetValue = if (isPressed && enabled) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 80),
        label = "btn_sec_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(scale)
            .testTag(testTag)
    ) {
        // Bottom shadow block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .offset(y = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tokens.borderActive)
        )

        // Top face
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .offset(y = offsetY)
                .clip(RoundedCornerShape(16.dp))
                .background(tokens.surface)
                .border(2.dp, tokens.borderMain, RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = tokens.textHeading,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text.uppercase(),
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * CGA Tag / Pill Component
 * Background: #E8F4FF, Text: #0A66C2, Corner Radius: 12dp, 11sp, 800 weight
 */
@Composable
fun TagPill(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    textColor: Color? = null
) {
    val tokens = LocalCGATokens.current
    val effectiveBg = backgroundColor ?: tokens.infoContainer
    val effectiveText = textColor ?: tokens.onInfoContainer

    Box(
        modifier = modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(effectiveBg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = effectiveText,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * CGA Streak Badge
 * Adaptive gamified streak fire badge
 */
@Composable
fun StreakBadge(
    days: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val tokens = LocalCGATokens.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.streakBg)
            .border(2.dp, tokens.streakBorder, RoundedCornerShape(16.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("streak_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Streak Fire",
                tint = tokens.streakText,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "🔥 $days d",
                color = tokens.streakText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * CGA XP Badge
 * Adaptive gamified total XP star badge
 */
@Composable
fun XpBadge(
    xp: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val tokens = LocalCGATokens.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.warningContainer)
            .border(2.dp, tokens.onWarningContainer.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("dashboard_xp_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "XP Star",
                tint = tokens.onWarningContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$xp XP",
                color = tokens.onWarningContainer,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * CGA Motivational Progress Bar with Percentage
 * Track: #E5E7EB, Height: 12dp, Radius: 99dp, Gradient: #58CC02 -> #78E119
 */
@Composable
fun CgaProgressBar(
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    label: String? = null,
    showPercentage: Boolean = true,
    progressColor: Color? = null
) {
    val tokens = LocalCGATokens.current
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "cga_progress_anim"
    )
    val percentageInt = (clampedProgress * 100).toInt()

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null || showPercentage) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        color = tokens.textBody,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (showPercentage) {
                    Text(
                        text = "$percentageInt%",
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Track bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(tokens.progressTrack)
        ) {
            if (animatedProgress > 0f) {
                val fillModifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))

                Box(
                    modifier = if (progressColor != null) {
                        fillModifier.background(progressColor)
                    } else {
                        fillModifier.background(
                            Brush.horizontalGradient(
                                colors = listOf(CGAColors.ProgressStart, CGAColors.ProgressEnd)
                            )
                        )
                    }
                )
            }
        }
    }
}

/**
 * CGA Hero Card
 * - Background: Surface
 * - Border: 2dp #E5E5E5, Bottom border: 6dp #E5E5E5
 * - Corner radius: 24dp, Padding: 20dp
 */
@Composable
fun HeroCard(
    tagText: String,
    heading: String,
    progressLabel: String,
    progress: Float,
    ctaText: String,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(tokens.surface)
            .border(2.dp, tokens.borderMain, RoundedCornerShape(24.dp))
            .padding(20.dp)
            .testTag("cga_hero_card")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAi = tagText.contains("AI", ignoreCase = true)
                TagPill(
                    text = tagText,
                    backgroundColor = if (isAi) tokens.purpleContainer else tokens.infoContainer,
                    textColor = if (isAi) tokens.onPurpleContainer else tokens.onInfoContainer
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = CGAColors.Star,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = heading,
                color = tokens.textHeading,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            CgaProgressBar(
                progress = progress,
                label = progressLabel,
                showPercentage = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            CgaPrimaryButton(
                text = ctaText,
                onClick = onCtaClick,
                variant = if (ctaText.contains("VIEW YOUR ROADMAP", ignoreCase = true) || ctaText.contains("VIEW ROADMAP", ignoreCase = true)) CgaButtonVariant.Ai else CgaButtonVariant.Primary
            )
        }
    }
}

enum class FeatureColorType {
    PURPLE, BLUE, GREEN, ORANGE, CYAN
}

/**
 * Feature Grid Card (2-column grid layout)
 * - 20dp corner radius, 2dp border, 5dp bottom tactile border
 * - 42x42dp icon box
 */
@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    colorType: FeatureColorType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    val tokens = LocalCGATokens.current

    val (iconBg, iconTint) = when (colorType) {
        FeatureColorType.PURPLE -> tokens.purpleContainer to tokens.onPurpleContainer
        FeatureColorType.BLUE -> tokens.infoContainer to tokens.onInfoContainer
        FeatureColorType.GREEN -> tokens.successContainer to tokens.onSuccessContainer
        FeatureColorType.ORANGE -> tokens.warningContainer to tokens.onWarningContainer
        FeatureColorType.CYAN -> Color(0xFFECFEFF) to Color(0xFF0891B2)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.surface)
            .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
            .testTag("feature_card_${title.lowercase().replace(" ", "_")}")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // 42x42dp icon box
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (badgeText != null) {
                        val (badgeBg, badgeTextClr) = when {
                            badgeText.contains("AI", ignoreCase = true) -> tokens.purpleContainer to tokens.onPurpleContainer
                            badgeText.contains("XP", ignoreCase = true) -> tokens.warningContainer to tokens.onWarningContainer
                            badgeText.equals("STREAK", ignoreCase = true) -> tokens.warningContainer to tokens.onWarningContainer
                            badgeText.equals("COMPLETED", ignoreCase = true) -> tokens.successContainer to tokens.onSuccessContainer
                            else -> tokens.infoContainer to tokens.onInfoContainer
                        }
                        TagPill(
                            text = badgeText,
                            backgroundColor = badgeBg,
                            textColor = badgeTextClr,
                            modifier = Modifier.wrapContentSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = description,
                color = tokens.textMuted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * CGA Header TopBar
 * - Profile avatar: 44x44, corner radius 14dp, background #0A66C2, text #FFFFFF 800
 * - Greeting: uppercase, letter spacing 0.5sp, 700 weight
 * - Student name: 18sp, 800 weight
 * - Streak badge
 * - Safe drawing status bar padding
 */
@Composable
fun CgaHeader(
    studentName: String,
    greeting: String = "WELCOME BACK",
    streakDays: Int = 0,
    xp: Int = 0,
    onAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current

    Surface(
        color = tokens.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Profile avatar
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF2563EB))
                            .border(2.dp, Color.White, RoundedCornerShape(14.dp))
                            .clickable { onAvatarClick() }
                            .testTag("header_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = studentName.firstOrNull()?.uppercase() ?: "S",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f, fill = true)
                    ) {
                        Text(
                            text = greeting.uppercase(),
                            color = tokens.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = studentName,
                            color = tokens.textHeading,
                            fontSize = if (studentName.length > 22) 13.sp else if (studentName.length > 15) 15.sp else 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            lineHeight = if (studentName.length > 22) 17.sp else if (studentName.length > 15) 19.sp else 21.sp,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    XpBadge(xp = xp)
                    StreakBadge(days = streakDays)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(tokens.borderMain)
            )
        }
    }
}

/**
 * Standard Sub-Screen TopBar with Back Arrow and Title
 * - Safe drawing status bar padding
 */
@Composable
fun CgaTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    testTag: String = "cga_top_bar"
) {
    val tokens = LocalCGATokens.current

    Surface(
        color = tokens.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag(testTag),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("top_bar_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = tokens.textHeading
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = title,
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                actions?.invoke()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(tokens.borderMain)
            )
        }
    }
}

/**
 * CGA 5-Destination Bottom Navigation
 * - Home, Explore, Roadmaps, Practice, Profile
 * - M3 Active Pill Indicator & 48dp Touch Targets
 * - Window Insets navigationBarsPadding support
 */
@Composable
fun CgaBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current

    Surface(
        color = tokens.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.surface)
        ) {
            // Clean neutral subtle top separation border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(tokens.borderMain.copy(alpha = 0.5f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(66.dp)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .testTag("cga_bottom_nav"),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    label = "Home",
                    icon = Icons.Default.Home,
                    isSelected = currentRoute == "dashboard",
                    onClick = { onNavigate("dashboard") },
                    testTag = "nav_home",
                    modifier = Modifier.weight(1f)
                )

                BottomNavItem(
                    label = "Explore",
                    icon = Icons.Default.Explore,
                    isSelected = currentRoute == "explore",
                    onClick = { onNavigate("explore") },
                    testTag = "nav_explore",
                    modifier = Modifier.weight(1f)
                )

                BottomNavItem(
                    label = "Roadmaps",
                    icon = Icons.Default.AltRoute,
                    isSelected = currentRoute == "roadmap",
                    onClick = { onNavigate("roadmap") },
                    testTag = "nav_roadmap",
                    modifier = Modifier.weight(1f)
                )

                BottomNavItem(
                    label = "Practice",
                    icon = Icons.Default.School,
                    isSelected = currentRoute == "practice" || currentRoute == "quiz",
                    onClick = { onNavigate("practice") },
                    testTag = "nav_practice",
                    modifier = Modifier.weight(1f)
                )

                BottomNavItem(
                    label = "Profile",
                    icon = Icons.Default.Person,
                    isSelected = currentRoute == "profile" || currentRoute == "resume" || currentRoute == "portfolio" || currentRoute == "admin",
                    onClick = { onNavigate("profile") },
                    testTag = "nav_profile",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current
    
    // Modern 2026 neutral navigation palette: Charcoal / Slate Navy for active, soft gray for unselected
    val activeColor = if (tokens.isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val inactiveColor = if (tokens.isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val pillActiveBg = if (tokens.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)

    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "nav_content_color"
    )

    val animatedPillColor by animateColorAsState(
        targetValue = if (isSelected) pillActiveBg else Color.Transparent,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "nav_pill_color"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "nav_icon_scale"
    )

    val labelAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.75f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "nav_label_alpha"
    )

    val labelTranslationY by animateFloatAsState(
        targetValue = if (isSelected) 0f else 1.5f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "nav_label_translation_y"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(animatedPillColor)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = animatedContentColor,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = animatedContentColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.graphicsLayer {
                alpha = labelAlpha
                translationY = labelTranslationY
            }
        )
    }
}

/**
 * Mentor / Consultation Card
 * - 20dp corner radius, 2dp border, horizontal layout, verified check badge
 */
@Composable
fun MentorCard(
    name: String,
    role: String,
    company: String,
    initials: String,
    isGreenAvatar: Boolean = false,
    rating: String = "4.9",
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current

    val avatarBg = if (isGreenAvatar) tokens.successContainer else tokens.purpleContainer
    val avatarTint = if (isGreenAvatar) tokens.onSuccessContainer else tokens.onPurpleContainer

    Box(
        modifier = modifier
            .width(260.dp)
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.surface)
            .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag("mentor_card_${name.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                // Avatar 48x48dp, corner radius 14dp
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = avatarTint,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                // Verified badge 18x18 circular green with white check
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(CGAColors.Primary)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Verified",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$role • $company",
                    color = tokens.textMuted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = CGAColors.Star,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = rating,
                        color = tokens.textHeading,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Career Card
 * - White surface, 2dp border, 5dp bottom border, 20dp radius
 */
@Composable
fun CareerCard(
    title: String,
    description: String,
    matchPercentage: Int,
    salaryRange: String,
    icon: ImageVector,
    onViewRoadmap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.surface)
            .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
            .padding(18.dp)
            .testTag("career_card_${title.lowercase().replace(" ", "_")}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.infoContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = tokens.onInfoContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = title,
                            color = tokens.textHeading,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = salaryRange,
                            color = tokens.textMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                TagPill(
                    text = "$matchPercentage% MATCH",
                    backgroundColor = if (matchPercentage >= 80) tokens.successContainer else tokens.infoContainer,
                    textColor = if (matchPercentage >= 80) tokens.onSuccessContainer else tokens.onInfoContainer,
                    modifier = Modifier.wrapContentSize()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                color = tokens.textBody,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            CgaPrimaryButton(
                text = "EXPLORE ROADMAP",
                onClick = onViewRoadmap,
                variant = CgaButtonVariant.Blue
            )
        }
    }
}

/**
 * Skill Chip Pill
 */
@Composable
fun SkillChip(
    skill: String,
    modifier: Modifier = Modifier,
    accentColor: Color = CGAColors.Primary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(99.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = skill,
            color = accentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

/**
 * Assessment Option Card (Tactile Chunky Option)
 */
@Composable
fun AssessmentOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current

    val borderColor = if (isSelected) CGAColors.Primary else tokens.borderMain
    val shadowColor = if (isSelected) CGAColors.PrimaryPressed else tokens.borderActive
    val bgColor = if (isSelected) tokens.successContainer else tokens.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable { onClick() }
    ) {
        // Bottom 3D shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .offset(y = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shadowColor)
        )

        // Top button face
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .border(2.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = text,
                    color = tokens.textHeading,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            if (isSelected) CGAColors.Primary else tokens.borderActive,
                            CircleShape
                        )
                        .background(if (isSelected) CGAColors.Primary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Roadmap Stage / Task Item Card
 */
@Composable
fun RoadmapItem(
    title: String,
    description: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isLocked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current

    val borderColor = when {
        isCompleted -> CGAColors.Primary
        isCurrent -> CGAColors.Primary
        else -> tokens.borderMain
    }

    val iconBg = when {
        isCompleted -> tokens.successContainer
        isCurrent -> tokens.successContainer
        else -> tokens.progressTrack
    }

    val iconTint = when {
        isCompleted -> tokens.onSuccessContainer
        isCurrent -> tokens.onSuccessContainer
        else -> tokens.textMuted
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tokens.surface)
            .border(2.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(enabled = !isLocked) { onToggle() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isCompleted -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    isLocked -> Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    else -> Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(CGAColors.Primary)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isLocked) tokens.textMuted else tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = tokens.textMuted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }

            if (isCompleted) {
                TagPill(
                    text = "DONE",
                    backgroundColor = tokens.successContainer,
                    textColor = tokens.onSuccessContainer
                )
            } else if (isCurrent) {
                TagPill(
                    text = "ACTIVE",
                    backgroundColor = tokens.infoContainer,
                    textColor = tokens.onInfoContainer
                )
            }
        }
    }
}

/**
 * Achievement / Gamification Card
 */
@Composable
fun AchievementCard(
    title: String,
    description: String,
    progressText: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.surface)
            .border(2.dp, if (isUnlocked) tokens.streakBorder else tokens.borderMain, RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("achievement_card_${title.lowercase().replace(" ", "_")}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isUnlocked) tokens.streakBg else tokens.progressTrack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isUnlocked) CGAColors.GamificationOrange else tokens.textMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Text(
                    text = description,
                    color = tokens.textMuted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }

            TagPill(
                text = progressText,
                backgroundColor = if (isUnlocked) tokens.streakBg else tokens.progressTrack,
                textColor = if (isUnlocked) tokens.streakText else tokens.textMuted
            )
        }
    }
}

/**
 * MetricStatCard for dashboard and profile metrics
 */
@Composable
fun MetricStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = "metric_stat_card"
) {
    val tokens = LocalCGATokens.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.surface)
            .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
            .padding(14.dp)
            .testTag(testTag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = tokens.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = value,
                    color = tokens.textHeading,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * Aliases for compatibility
 */
@Composable
fun CgaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = CGAColors.Primary,
    testTag: String = "cga_primary_button"
) {
    CgaPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        testTag = testTag
    )
}

@Composable
fun CgaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "cga_secondary_button"
) {
    CgaSecondaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        testTag = testTag
    )
}

@Composable
fun CgaGamifiedHeader(
    streakDays: Int = 0,
    xpCount: Int = 450,
    careerGoal: String = "AI / ML Engineer",
    modifier: Modifier = Modifier
) {
    CgaHeader(
        studentName = "Alex Sharma",
        greeting = "Placement Goal: $careerGoal",
        streakDays = streakDays,
        modifier = modifier
    )
}

@Composable
fun CgaBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    CgaBottomNavigation(
        currentRoute = currentRoute,
        onNavigate = onNavigate
    )
}

