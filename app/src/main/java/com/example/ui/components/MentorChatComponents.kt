package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MentorChatMessage
import com.example.domain.model.MentorConversation
import com.example.domain.model.MentorRole
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

private val NUMBERED_LIST_REGEX = Regex("^(\\*{0,2})(\\d+)[.)](\\*{0,2})\\s+(.+)$")
private val INLINE_MARKDOWN_REGEX = Regex("(\\*\\*\\*([^*]+)\\*\\*\\*|\\*\\*([^*]+)\\*\\*|\\*([^*]+)\\*|`([^`]+)`)")

/**
 * Chat bubble displaying an individual user or AI message turn with native CGA styling.
 */
@Composable
fun MentorChatBubble(
    message: MentorChatMessage,
    modifier: Modifier = Modifier,
    overrideContent: String? = null,
    isTyping: Boolean = false
) {
    val tokens = LocalCGATokens.current
    val isUser = message.role == MentorRole.USER
    val displayContent = overrideContent ?: message.content

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.85f else 0.95f)
                .widthIn(max = 520.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                // AI Mentor Avatar Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(tokens.infoContainer)
                        .border(1.5.dp, tokens.borderMain, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Mentor",
                        tint = tokens.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Message Bubble Card
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(if (isUser) CGAColors.Primary else tokens.surface)
                    .border(
                        width = if (isUser) 0.dp else 1.5.dp,
                        color = if (isUser) Color.Transparent else tokens.borderMain,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag(if (isUser) "user_message_bubble" else "mentor_message_bubble")
            ) {
                Column {
                    FormattedChatText(
                        text = displayContent,
                        isUser = isUser,
                        isTyping = isTyping
                    )

                    // Optional timestamp
                    if (message.createdAt != null && message.createdAt > 0) {
                        val timeStr = formatTimestamp(message.createdAt)
                        Text(
                            text = timeStr,
                            color = if (isUser) Color.White.copy(alpha = 0.75f) else tokens.textMuted,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .align(if (isUser) Alignment.End else Alignment.Start)
                                .padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formats chat message content with clean paragraph, markdown, and code-snippet rendering.
 */
@Composable
fun FormattedChatText(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
    isTyping: Boolean = false
) {
    val tokens = LocalCGATokens.current
    val contentColor = if (isUser) Color.White else tokens.textHeading

    // 1. Separate code fences from standard markdown text
    val parts = text.split("```")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { index, section ->
            val isLastSection = index == parts.lastIndex
            if (index % 2 == 1) {
                // Code snippet section
                val lines = section.trim().lines()
                val lang = if (lines.isNotEmpty() && lines.first().all { it.isLetterOrDigit() }) lines.first() else ""
                val code = if (lang.isNotEmpty()) lines.drop(1).joinToString("\n") else section.trim()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isUser) Color(0xFF1E3800) else Color(0xFF1E1E24))
                        .border(1.dp, if (isUser) Color.Transparent else tokens.borderMain, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        if (lang.isNotBlank()) {
                            Text(
                                text = lang.uppercase(Locale.getDefault()),
                                color = Color(0xFFAAAAAA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Text(
                            text = code.ifBlank { section.trim() },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (isUser) Color(0xFFE2F7C2) else Color(0xFFF0F0F0),
                            lineHeight = 17.sp,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        )
                    }
                }
            } else if (section.isNotBlank() || (isTyping && isLastSection)) {
                // Regular markdown paragraphs, lists, and headings
                RenderMarkdownBlocks(
                    rawText = section.trim(),
                    isUser = isUser,
                    contentColor = contentColor,
                    isTyping = isTyping && isLastSection
                )
            }
        }
    }
}

@Composable
private fun RenderMarkdownBlocks(
    rawText: String,
    isUser: Boolean,
    contentColor: Color,
    isTyping: Boolean = false
) {
    val tokens = LocalCGATokens.current
    val paragraphs = rawText.split(Regex("\n\\s*\n"))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        paragraphs.forEachIndexed { pIndex, paragraph ->
            val isLastParagraph = pIndex == paragraphs.lastIndex
            val pTrimmed = paragraph.trim()
            if (pTrimmed.isNotEmpty() || (isTyping && isLastParagraph)) {
                val lines = pTrimmed.lines()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    lines.forEachIndexed { lIndex, line ->
                        val isLastLine = isLastParagraph && (lIndex == lines.lastIndex)
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() || (isTyping && isLastLine)) {
                            when {
                                trimmed.startsWith("### ") -> {
                                    val headerText = trimmed.removePrefix("### ").trim()
                                    Text(
                                        text = parseInlineMarkdown(headerText, isUser, contentColor),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUser) Color.White else tokens.textHeading,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                    )
                                }
                                trimmed.startsWith("## ") -> {
                                    val headerText = trimmed.removePrefix("## ").trim()
                                    Text(
                                        text = parseInlineMarkdown(headerText, isUser, contentColor),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUser) Color.White else tokens.textHeading,
                                        lineHeight = 20.sp,
                                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                    )
                                }
                                trimmed.startsWith("# ") -> {
                                    val headerText = trimmed.removePrefix("# ").trim()
                                    Text(
                                        text = parseInlineMarkdown(headerText, isUser, contentColor),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUser) Color.White else tokens.textHeading,
                                        lineHeight = 22.sp,
                                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                    )
                                }
                                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                                    val bulletText = trimmed.substring(2).trim()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "•",
                                            color = if (isUser) Color.White.copy(alpha = 0.9f) else tokens.primary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(end = 6.dp, start = 2.dp)
                                        )
                                        Row(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = parseInlineMarkdown(bulletText, isUser, contentColor),
                                                color = contentColor,
                                                fontSize = 14.sp,
                                                lineHeight = 20.sp
                                            )
                                            if (isTyping && isLastLine) {
                                                StreamingCursor(if (isUser) Color.White else tokens.primary)
                                            }
                                        }
                                    }
                                }
                                NUMBERED_LIST_REGEX.matches(trimmed) -> {
                                    val match = NUMBERED_LIST_REGEX.find(trimmed)
                                    val number = match?.groupValues?.get(2) ?: "1"
                                    val rawItemText = match?.groupValues?.get(4) ?: trimmed
                                    val itemText = if (match?.groupValues?.get(1) == "**" && !rawItemText.startsWith("**") && rawItemText.contains("**")) {
                                        "**$rawItemText"
                                    } else {
                                        rawItemText
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 6.dp, top = 2.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isUser) Color(0x33FFFFFF) else tokens.infoContainer)
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "$number.",
                                                color = if (isUser) Color.White else tokens.primary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Row(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = parseInlineMarkdown(itemText, isUser, contentColor),
                                                color = contentColor,
                                                fontSize = 14.sp,
                                                lineHeight = 20.sp
                                            )
                                            if (isTyping && isLastLine) {
                                                StreamingCursor(if (isUser) Color.White else tokens.primary)
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = parseInlineMarkdown(trimmed, isUser, contentColor),
                                            color = contentColor,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
                                        )
                                        if (isTyping && isLastLine) {
                                            StreamingCursor(if (isUser) Color.White else tokens.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingCursor(color: Color) {
    val transition = rememberInfiniteTransition(label = "cursor_blink")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )
    Text(
        text = " ▌",
        color = color.copy(alpha = alpha),
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp
    )
}

internal fun parseInlineMarkdown(
    text: String,
    isUser: Boolean,
    baseColor: Color
): AnnotatedString = buildAnnotatedString {
    var lastIndex = 0
    val matches = INLINE_MARKDOWN_REGEX.findAll(text)

    for (match in matches) {
        if (match.range.first > lastIndex) {
            append(text.substring(lastIndex, match.range.first))
        }

        val matchStr = match.value
        when {
            matchStr.startsWith("***") && matchStr.endsWith("***") && matchStr.length >= 6 -> {
                val content = matchStr.substring(3, matchStr.length - 3)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                    append(content)
                }
            }
            matchStr.startsWith("**") && matchStr.endsWith("**") && matchStr.length >= 4 -> {
                val content = matchStr.substring(2, matchStr.length - 2)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(content)
                }
            }
            matchStr.startsWith("*") && matchStr.endsWith("*") && matchStr.length >= 2 -> {
                val content = matchStr.substring(1, matchStr.length - 1)
                withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                    append(content)
                }
            }
            matchStr.startsWith("`") && matchStr.endsWith("`") && matchStr.length >= 2 -> {
                val content = matchStr.substring(1, matchStr.length - 1)
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        background = if (isUser) Color(0x33000000) else Color(0x1A000000)
                    )
                ) {
                    append(content)
                }
            }
            else -> append(matchStr)
        }
        lastIndex = match.range.last + 1
    }

    if (lastIndex < text.length) {
        val remaining = text.substring(lastIndex)
        // If in-progress streaming has an unclosed markdown tag, strip raw delimiters and style gracefully
        when {
            remaining.contains("***") -> {
                val idx = remaining.lastIndexOf("***")
                append(remaining.substring(0, idx))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                    append(remaining.substring(idx + 3))
                }
            }
            remaining.contains("**") -> {
                val idx = remaining.lastIndexOf("**")
                append(remaining.substring(0, idx))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(remaining.substring(idx + 2))
                }
            }
            remaining.startsWith("*") && remaining.length > 1 && !remaining.endsWith("*") -> {
                withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                    append(remaining.substring(1))
                }
            }
            else -> append(remaining)
        }
    }
}

/**
 * Animated thinking bubble showing 3 pulsing dots while the AI Mentor is reasoning.
 */
@Composable
fun ThinkingBubble(
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current
    val transition = rememberInfiniteTransition(label = "thinking_transition")
    val alpha1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val alpha3 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tokens.infoContainer)
                    .border(1.5.dp, tokens.borderMain, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = tokens.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
                    .background(tokens.surface)
                    .border(1.5.dp, tokens.borderMain, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("mentor_thinking_indicator")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "AI Mentor is thinking",
                        color = tokens.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(tokens.primary.copy(alpha = alpha1)))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(tokens.primary.copy(alpha = alpha2)))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(tokens.primary.copy(alpha = alpha3)))
                }
            }
        }
    }
}

/**
 * Horizontal row of suggested starter prompts.
 */
@Composable
fun SuggestedPromptsRow(
    prompts: List<String>,
    onPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        prompts.forEach { prompt ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.surface)
                    .border(1.5.dp, tokens.borderMain, RoundedCornerShape(16.dp))
                    .clickable { onPromptClick(prompt) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("suggested_prompt_chip")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = tokens.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = prompt,
                        color = tokens.textHeading,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

/**
 * Modal Bottom Sheet for exploring persistent conversation history threads.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistorySheet(
    conversations: List<MentorConversation>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteRequest: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.surface,
        modifier = modifier.testTag("conversation_history_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Conversation History",
                    color = tokens.textHeading,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // New Chat Button
                CgaPrimaryButton(
                    text = "+ New Chat",
                    onClick = onNewChat,
                    modifier = Modifier.testTag("history_new_chat_button")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No previous conversations found.",
                        color = tokens.textMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        val isSelected = conv.id == selectedId
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) tokens.infoContainer else tokens.contentBackground)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) tokens.primary else tokens.borderMain,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelect(conv.id) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("history_item_${conv.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        tint = if (isSelected) tokens.primary else tokens.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = conv.title,
                                            color = tokens.textHeading,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (conv.updatedAt != null) {
                                            Text(
                                                text = formatTimestamp(conv.updatedAt),
                                                color = tokens.textMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteRequest(conv.id) },
                                    modifier = Modifier.size(32.dp).testTag("delete_conv_${conv.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Conversation",
                                        tint = tokens.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Confirmation dialog for permanently deleting a conversation thread.
 */
@Composable
fun DeleteConversationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalCGATokens.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Conversation?",
                color = tokens.textHeading,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "This will permanently delete this conversation and its message history.",
                color = tokens.textBody,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = Color(0xFFBA1A1A), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = tokens.textHeading)
            }
        },
        containerColor = tokens.surface
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
