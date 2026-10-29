package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ConversationHistorySheet
import com.example.ui.components.DeleteConversationDialog
import com.example.ui.components.MentorChatBubble
import com.example.ui.components.SuggestedPromptsRow
import com.example.ui.components.ThinkingBubble
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.MentorChatViewModel

private val DEFAULT_STARTER_PROMPTS = listOf(
    "What should I learn next for my career?",
    "Explain my recommended roadmap",
    "Give me a technical interview question",
    "How do I optimize my ATS resume score?",
    "Help me debug a technical concept",
    "Explain recursion with a practical example"
)

/**
 * Full-screen ChatGPT-style conversational screen for the CGA AI Mentor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentorChatScreen(
    viewModel: MentorChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val tokens = LocalCGATokens.current
    val listState = rememberLazyListState()

    // Handle system back navigation
    BackHandler {
        when {
            uiState.isHistorySheetOpen -> viewModel.setHistorySheetOpen(false)
            uiState.deletingConversationId != null -> viewModel.setDeletingConversation(null)
            else -> onNavigateBack()
        }
    }

    // Refresh student context and proactive focus upon entering chat screen
    LaunchedEffect(Unit) {
        viewModel.refreshStudentContext()
    }

    // Auto-scroll to the newest message turn or during typewriter streaming
    LaunchedEffect(uiState.messages.size, uiState.isSendingMessage, uiState.typingDisplayedContent?.length) {
        val count = uiState.messages.size + (if (uiState.isSendingMessage) 1 else 0)
        if (count > 0) {
            listState.animateScrollToItem(count - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CGA AI Mentor",
                                color = tokens.textHeading,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tokens.infoContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AI ✨",
                                    color = tokens.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Text(
                            text = uiState.conversationTitle,
                            color = tokens.textMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("mentor_chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back",
                            tint = tokens.textHeading
                        )
                    }
                },
                actions = {
                    // New Chat Action
                    IconButton(
                        onClick = { viewModel.createNewConversation() },
                        modifier = Modifier.testTag("mentor_new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Start New Conversation",
                            tint = tokens.primary
                        )
                    }

                    // Conversation History Action
                    IconButton(
                        onClick = { viewModel.setHistorySheetOpen(true) },
                        modifier = Modifier.testTag("mentor_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View Conversation History",
                            tint = tokens.textHeading
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tokens.surface
                ),
                modifier = Modifier
                    .border(0.dp, Color.Transparent)
                    .testTag("mentor_chat_top_bar")
            )
        },
        containerColor = tokens.contentBackground,
        modifier = modifier
            .fillMaxSize()
            .testTag("mentor_chat_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Error Banner (if any)
            if (!uiState.error.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.warningContainer)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.error.orEmpty(),
                            color = tokens.onWarningContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss error",
                                tint = tokens.onWarningContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Message Body Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoadingMessages && !uiState.hasMessages && !uiState.isSendingMessage) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = tokens.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else if (!uiState.hasMessages && !uiState.isSendingMessage) {
                    // Empty Conversation Welcome State
                    EmptyConversationWelcomeView(
                        context = uiState.activeContext,
                        focusItems = uiState.proactiveFocusItems,
                        onPromptClick = { prompt ->
                            viewModel.sendMessage(prompt)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { msg ->
                            val isThisMsgTyping = msg.id == uiState.typingMessageId
                            val textToDisplay = if (isThisMsgTyping && uiState.typingDisplayedContent != null) {
                                uiState.typingDisplayedContent
                            } else {
                                msg.content
                            }
                            MentorChatBubble(
                                message = msg,
                                overrideContent = textToDisplay,
                                isTyping = isThisMsgTyping
                            )
                        }

                        if (uiState.isSendingMessage) {
                            item(key = "thinking_indicator") {
                                ThinkingBubble()
                            }
                        }
                    }
                }
            }

            // Suggested Prompts Row (shown if messages are few or empty)
            if (uiState.hasMessages && uiState.messages.size <= 2) {
                SuggestedPromptsRow(
                    prompts = DEFAULT_STARTER_PROMPTS.take(3),
                    onPromptClick = { prompt ->
                        viewModel.sendMessage(prompt)
                    },
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Bottom Input Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.surface)
                    .border(1.dp, tokens.borderMain)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("mentor_chat_input_bar")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        placeholder = {
                            Text(
                                text = "Ask anything (career, code, roadmap...)",
                                color = tokens.textMuted,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mentor_chat_input_field"),
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = tokens.contentBackground,
                            unfocusedContainerColor = tokens.contentBackground,
                            focusedBorderColor = tokens.primary,
                            unfocusedBorderColor = tokens.borderMain,
                            focusedTextColor = tokens.textHeading,
                            unfocusedTextColor = tokens.textHeading
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (uiState.canSendMessage) {
                                    viewModel.sendMessage()
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    IconButton(
                        onClick = { viewModel.sendMessage() },
                        enabled = uiState.canSendMessage,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (uiState.canSendMessage) tokens.primary else tokens.borderMain)
                            .testTag("mentor_chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (uiState.canSendMessage) Color.White else tokens.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Conversation History Sheet
    if (uiState.isHistorySheetOpen) {
        ConversationHistorySheet(
            conversations = uiState.conversations,
            selectedId = uiState.selectedConversationId,
            onSelect = { convId ->
                viewModel.selectConversation(convId)
            },
            onNewChat = {
                viewModel.createNewConversation()
            },
            onDeleteRequest = { convId ->
                viewModel.setDeletingConversation(convId)
            },
            onDismiss = {
                viewModel.setHistorySheetOpen(false)
            }
        )
    }

    // Delete Confirmation Dialog
    if (uiState.deletingConversationId != null) {
        DeleteConversationDialog(
            onConfirm = {
                uiState.deletingConversationId?.let { convId ->
                    viewModel.deleteConversation(convId)
                }
            },
            onDismiss = {
                viewModel.setDeletingConversation(null)
            }
        )
    }
}

/**
 * Welcoming empty state with persona introduction, verified focus points, and starter chips.
 */
@Composable
fun EmptyConversationWelcomeView(
    context: com.example.domain.model.CGAMentorContext?,
    focusItems: List<com.example.domain.model.MentorFocusItem> = emptyList(),
    onPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Welcome Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(tokens.surface)
                .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .testTag("mentor_welcome_card")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(tokens.infoContainer)
                        .border(2.dp, tokens.borderMain, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = tokens.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Hi${if (!context?.studentName.isNullOrBlank()) " ${context?.studentName}" else ""}! I'm your CGA AI Mentor.",
                    color = tokens.textHeading,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "I'm here to help with your career milestones, roadmap checkpoints, technical coding problems, and placement interview preparation.",
                    color = tokens.textBody,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                // Optional Proactive Focus Card
                if (!context?.recommendedCareer.isNullOrBlank() || (context?.currentStreak ?: 0) > 0 || focusItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.contentBackground)
                            .border(1.dp, tokens.borderMain, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = CGAColors.GamificationOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "TODAY'S CONTEXT & FOCUS",
                                    color = tokens.textHeading,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            if (!context?.recommendedCareer.isNullOrBlank()) {
                                Text(
                                    text = "• Target Career: ${context?.recommendedCareer}",
                                    color = tokens.textBody,
                                    fontSize = 12.sp
                                )
                            }
                            if (!context?.currentRoadmapStage.isNullOrBlank()) {
                                Text(
                                    text = "• Active Stage: ${context?.currentRoadmapStage}",
                                    color = tokens.textBody,
                                    fontSize = 12.sp
                                )
                            }
                            if ((context?.currentStreak ?: 0) > 0) {
                                Text(
                                    text = "• Active Streak: ${context?.currentStreak} days",
                                    color = tokens.textBody,
                                    fontSize = 12.sp
                                )
                            }
                            val topFocus = focusItems.firstOrNull()
                            if (topFocus != null) {
                                Text(
                                    text = "• Next Focus: ${topFocus.title} — ${topFocus.description}",
                                    color = tokens.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Suggested Questions",
            color = tokens.textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        SuggestedPromptsRow(
            prompts = DEFAULT_STARTER_PROMPTS,
            onPromptClick = onPromptClick
        )
    }
}
