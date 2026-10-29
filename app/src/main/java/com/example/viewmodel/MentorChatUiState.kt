package com.example.viewmodel

import com.example.domain.model.CGAMentorContext
import com.example.domain.model.MentorChatMessage
import com.example.domain.model.MentorConversation
import com.example.domain.model.MentorFocusItem

/**
 * State representing the complete UI and data state for the CGA AI Mentor Chat screen.
 */
data class MentorChatUiState(
    val conversations: List<MentorConversation> = emptyList(),
    val selectedConversationId: String? = null,
    val selectedConversation: MentorConversation? = null,
    val messages: List<MentorChatMessage> = emptyList(),
    val isLoadingConversations: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val isSendingMessage: Boolean = false,
    val inputText: String = "",
    val error: String? = null,
    val activeContext: CGAMentorContext? = null,
    val proactiveFocusItems: List<MentorFocusItem> = emptyList(),
    val isHistorySheetOpen: Boolean = false,
    val deletingConversationId: String? = null,
    val typingMessageId: String? = null,
    val typingDisplayedContent: String? = null
) {
    val hasConversations: Boolean
        get() = conversations.isNotEmpty()

    val hasMessages: Boolean
        get() = messages.isNotEmpty()

    val canSendMessage: Boolean
        get() = inputText.trim().isNotBlank() && !isSendingMessage

    val conversationTitle: String
        get() = selectedConversation?.title?.ifBlank { "CGA AI Mentor" } ?: "CGA AI Mentor"
}
