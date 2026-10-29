package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.coordinator.CGAMentorContextProvider
import com.example.domain.model.CGAMentorContext
import com.example.domain.model.MentorChatMessage
import com.example.domain.model.MentorConversation
import com.example.domain.model.MentorRole
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CareerRepository
import com.example.domain.repository.MentorRepository
import com.example.domain.repository.PracticeRepository
import com.example.domain.repository.RoadmapRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing UI state, persistent message histories, multi-turn AI interactions,
 * and contextual prompt assembly for the CGA AI Mentor.
 */
class MentorChatViewModel(
    private val mentorRepository: MentorRepository,
    private val authRepository: AuthRepository? = null,
    private val careerRepository: CareerRepository? = null,
    private val roadmapRepository: RoadmapRepository? = null,
    private val practiceRepository: PracticeRepository? = null,
    private val contextProvider: CGAMentorContextProvider = CGAMentorContextProvider(
        authRepository = authRepository,
        careerRepository = careerRepository,
        roadmapRepository = roadmapRepository,
        practiceRepository = practiceRepository
    )
) : ViewModel() {

    companion object {
        private const val TAG = "MentorChatVM"
    }

    private val _uiState = MutableStateFlow(MentorChatUiState())
    val uiState: StateFlow<MentorChatUiState> = _uiState.asStateFlow()

    private var messagesObservationJob: Job? = null
    private var conversationsObservationJob: Job? = null
    private var typewriterJob: Job? = null

    init {
        loadConversations()
        refreshStudentContext()
    }

    /**
     * Initializes conversation observation from Supabase.
     */
    fun loadConversations() {
        conversationsObservationJob?.cancel()
        if (_uiState.value.conversations.isEmpty()) {
            _uiState.update { it.copy(isLoadingConversations = true, error = null) }
        }

        conversationsObservationJob = viewModelScope.launch {
            mentorRepository.getConversations()
                .catch { e ->
                    Log.e(TAG, "Failed observing conversations: ${e.message}", e)
                    _uiState.update {
                        it.copy(
                            isLoadingConversations = false,
                            error = "Unable to load conversations. Tap retry to reload."
                        )
                    }
                }
                .collect { conversationsList ->
                    _uiState.update { state ->
                        val currentSelectedId = state.selectedConversationId
                        val activeConv = conversationsList.firstOrNull { it.id == currentSelectedId }
                            ?: conversationsList.firstOrNull()

                        state.copy(
                            conversations = conversationsList,
                            selectedConversationId = activeConv?.id ?: currentSelectedId,
                            selectedConversation = activeConv,
                            isLoadingConversations = false
                        )
                    }

                    // If a conversation is selected and we are not already observing its messages, start observing
                    val selectedId = _uiState.value.selectedConversationId
                    if (!selectedId.isNullOrBlank() && messagesObservationJob == null && !_uiState.value.isSendingMessage) {
                        observeMessages(selectedId)
                    }
                }
        }
    }

    /**
     * Selects an active conversation and observes its message history.
     */
    fun selectConversation(conversationId: String) {
        if (conversationId.isBlank()) return
        val currentSelected = _uiState.value.selectedConversationId
        if (currentSelected == conversationId && _uiState.value.messages.isNotEmpty()) {
            _uiState.update { it.copy(isHistorySheetOpen = false) }
            return
        }

        typewriterJob?.cancel()
        messagesObservationJob?.cancel()
        val conv = _uiState.value.conversations.firstOrNull { it.id == conversationId }
        _uiState.update {
            it.copy(
                selectedConversationId = conversationId,
                selectedConversation = conv,
                messages = emptyList(),
                typingMessageId = null,
                typingDisplayedContent = null,
                isHistorySheetOpen = false,
                isLoadingMessages = true,
                error = null
            )
        }
        observeMessages(conversationId)
    }

    /**
     * Observes message stream for a specific conversation ID.
     */
    private fun observeMessages(conversationId: String) {
        messagesObservationJob?.cancel()
        if (_uiState.value.messages.isEmpty() && !_uiState.value.isSendingMessage) {
            _uiState.update { it.copy(isLoadingMessages = true) }
        }

        messagesObservationJob = viewModelScope.launch {
            mentorRepository.getMessages(conversationId)
                .catch { e ->
                    Log.e(TAG, "Failed loading messages for conv $conversationId: ${e.message}", e)
                    _uiState.update {
                        it.copy(
                            isLoadingMessages = false,
                            error = "Could not load messages for this conversation."
                        )
                    }
                }
                .collect { msgList ->
                    _uiState.update { state ->
                        // While sending is in progress or typewriter is actively displaying, do not clobber in-memory state
                        if (state.isSendingMessage || state.typingMessageId != null) {
                            state.copy(isLoadingMessages = false)
                        } else {
                            state.copy(
                                messages = msgList,
                                isLoadingMessages = false
                            )
                        }
                    }
                }
        }
    }

    /**
     * Creates a new conversation thread and immediately activates it.
     */
    fun createNewConversation(title: String = "New Conversation") {
        typewriterJob?.cancel()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingMessages = false,
                    error = null,
                    isHistorySheetOpen = false,
                    typingMessageId = null,
                    typingDisplayedContent = null
                )
            }
            val result = mentorRepository.createConversation(title)
            result.onSuccess { newConv ->
                _uiState.update {
                    it.copy(
                        selectedConversationId = newConv.id,
                        selectedConversation = newConv,
                        messages = emptyList(),
                        isLoadingMessages = false
                    )
                }
                observeMessages(newConv.id)
                loadConversations()
            }.onFailure { e ->
                Log.e(TAG, "Failed creating new conversation: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoadingMessages = false,
                        error = "Failed to create new conversation: ${e.message ?: "Please try again."}"
                    )
                }
            }
        }
    }

    /**
     * Updates the input field value.
     */
    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * Sends a user message to the active conversation or creates one if none exists.
     */
    fun sendMessage(promptOverride: String? = null) {
        val rawMessage = promptOverride ?: _uiState.value.inputText
        val cleanMessage = rawMessage.trim()
        if (cleanMessage.isBlank() || _uiState.value.isSendingMessage) return

        viewModelScope.launch {
            var activeConvId = _uiState.value.selectedConversationId

            // If no conversation exists yet, create one on-demand before sending
            if (activeConvId.isNullOrBlank()) {
                val createResult = mentorRepository.createConversation("New Conversation")
                if (createResult.isSuccess) {
                    val newConv = createResult.getOrThrow()
                    activeConvId = newConv.id
                    _uiState.update {
                        it.copy(
                            selectedConversationId = newConv.id,
                            selectedConversation = newConv,
                            isLoadingMessages = false
                        )
                    }
                } else {
                    val err = createResult.exceptionOrNull()?.message ?: "Unable to initialize conversation"
                    _uiState.update { it.copy(error = err) }
                    return@launch
                }
            }

            // Optimistically insert user message into local view state while sending
            val optimisticUserMsg = MentorChatMessage(
                id = "temp_user_${System.currentTimeMillis()}",
                conversationId = activeConvId,
                userId = "",
                role = MentorRole.USER,
                content = cleanMessage,
                createdAt = System.currentTimeMillis()
            )

            _uiState.update {
                it.copy(
                    inputText = "",
                    isSendingMessage = true,
                    error = null,
                    messages = it.messages + optimisticUserMsg
                )
            }

            // Refresh context and select relevant intent-targeted context
            val fullContext = try {
                contextProvider.assembleVerifiedContext()
            } catch (_: Throwable) {
                _uiState.value.activeContext
            } ?: _uiState.value.activeContext

            if (fullContext != null) {
                _uiState.update { it.copy(activeContext = fullContext) }
            }

            val targetedContext = contextProvider.selectRelevantContext(fullContext, cleanMessage)

            val sendResult = mentorRepository.sendMessage(
                conversationId = activeConvId,
                message = cleanMessage,
                context = targetedContext
            )

            sendResult.onSuccess { modelMsg ->
                typewriterJob?.cancel()

                // Append the model response and initialize typewriter state
                _uiState.update { state ->
                    state.copy(
                        isSendingMessage = false,
                        error = null,
                        messages = state.messages + modelMsg,
                        typingMessageId = modelMsg.id,
                        typingDisplayedContent = ""
                    )
                }

                // Progressive presentation typewriter effect
                typewriterJob = viewModelScope.launch {
                    val fullText = modelMsg.content
                    val chunkSize = when {
                        fullText.length > 800 -> 4
                        fullText.length > 400 -> 3
                        fullText.length > 150 -> 2
                        else -> 1
                    }
                    val delayMs = when {
                        fullText.length > 800 -> 16L
                        fullText.length > 400 -> 18L
                        else -> 22L
                    }
                    var currentIndex = 0
                    while (currentIndex < fullText.length) {
                        currentIndex = (currentIndex + chunkSize).coerceAtMost(fullText.length)
                        val partial = fullText.substring(0, currentIndex)
                        _uiState.update {
                            it.copy(
                                typingMessageId = modelMsg.id,
                                typingDisplayedContent = partial
                            )
                        }
                        delay(delayMs)
                    }
                    _uiState.update {
                        it.copy(
                            typingMessageId = null,
                            typingDisplayedContent = null
                        )
                    }
                    loadConversations()
                }
            }.onFailure { e ->
                Log.e(TAG, "Send message failed: ${e.message}", e)
                // On failure, keep conversation intact, remove optimistic user message, and restore input
                _uiState.update { state ->
                    state.copy(
                        isSendingMessage = false,
                        inputText = if (promptOverride == null) cleanMessage else state.inputText,
                        error = e.message ?: "AI Mentor was unable to respond. Please try again.",
                        messages = state.messages.filterNot { it.id == optimisticUserMsg.id }
                    )
                }
            }
        }
    }

    /**
     * Deletes a conversation thread.
     */
    fun deleteConversation(conversationId: String) {
        typewriterJob?.cancel()
        viewModelScope.launch {
            val result = mentorRepository.deleteConversation(conversationId)
            result.onSuccess {
                _uiState.update { state ->
                    val remaining = state.conversations.filterNot { it.id == conversationId }
                    val nextSelected = if (state.selectedConversationId == conversationId) {
                        remaining.firstOrNull()?.id
                    } else {
                        state.selectedConversationId
                    }
                    state.copy(
                        conversations = remaining,
                        selectedConversationId = nextSelected,
                        selectedConversation = remaining.firstOrNull { it.id == nextSelected },
                        deletingConversationId = null
                    )
                }
                val newSelected = _uiState.value.selectedConversationId
                if (!newSelected.isNullOrBlank()) {
                    observeMessages(newSelected)
                } else {
                    _uiState.update { it.copy(messages = emptyList()) }
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to delete conversation: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Failed to delete conversation: ${e.message}",
                        deletingConversationId = null
                    )
                }
            }
        }
    }

    /**
     * Toggles the conversation history modal sheet.
     */
    fun setHistorySheetOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isHistorySheetOpen = isOpen) }
        if (isOpen) {
            loadConversations()
        }
    }

    /**
     * Sets or clears the conversation deletion confirmation dialog target.
     */
    fun setDeletingConversation(conversationId: String?) {
        _uiState.update { it.copy(deletingConversationId = conversationId) }
    }

    /**
     * Clears current error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Assembles available student context from verified repositories and derives proactive focus items.
     */
    fun refreshStudentContext() {
        viewModelScope.launch {
            try {
                val fullContext = contextProvider.assembleVerifiedContext()
                val focusItems = contextProvider.deriveProactiveFocusItems(fullContext)
                _uiState.update {
                    it.copy(
                        activeContext = fullContext,
                        proactiveFocusItems = focusItems
                    )
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Context refresh non-fatal error: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        typewriterJob?.cancel()
        messagesObservationJob?.cancel()
        conversationsObservationJob?.cancel()
    }
}
