package com.example

import androidx.compose.ui.graphics.Color
import com.example.data.repository.MockMentorRepository
import com.example.domain.model.CGAMentorContext
import com.example.domain.model.MentorChatMessage
import com.example.domain.model.MentorConversation
import com.example.domain.model.MentorRole
import com.example.domain.repository.MentorRepository
import com.example.ui.components.parseInlineMarkdown
import com.example.viewmodel.MentorChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MentorChatUxTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. Sending a message does NOT set isLoadingMessages = true (no full-screen loading)
    @Test
    fun test1_sendingMessageDoesNotShowFullScreenLoading() = runTest(testDispatcher) {
        val baseRepo = MockMentorRepository("user_test")
        val mockRepo = object : MentorRepository by baseRepo {
            override suspend fun sendMessage(
                conversationId: String,
                message: String,
                context: CGAMentorContext?
            ): Result<MentorChatMessage> {
                kotlinx.coroutines.delay(100)
                return baseRepo.sendMessage(conversationId, message, context)
            }
        }
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        viewModel.onInputTextChanged("How do I prepare for technical interviews?")
        viewModel.sendMessage()
        testScheduler.runCurrent()

        // While sending is in flight:
        val stateWhileSending = viewModel.uiState.value
        assertTrue("isSendingMessage must be true while send is in flight", stateWhileSending.isSendingMessage)
        assertFalse("isLoadingMessages must remain false while sending", stateWhileSending.isLoadingMessages)
        assertTrue("Optimistic user message must appear immediately", stateWhileSending.hasMessages)
        assertEquals(3, stateWhileSending.messages.size) // 2 demo messages + 1 optimistic user message

        testScheduler.advanceUntilIdle()

        val stateAfter = viewModel.uiState.value
        assertFalse(stateAfter.isSendingMessage)
        assertFalse(stateAfter.isLoadingMessages)
        assertNull(stateAfter.typingMessageId) // Finished typewriter
        assertEquals(4, stateAfter.messages.size) // 2 demo + 1 user + 1 model
    }

    // 2. Typewriter animation progressively reveals text and finishes at exact response
    @Test
    fun test2_typewriterEffectProgressesAndCompletes() = runTest(testDispatcher) {
        val expectedReply = "Here is your recommended step-by-step career path for AI & Machine Learning."
        val customRepo = object : MentorRepository {
            override suspend fun createConversation(title: String): Result<MentorConversation> =
                Result.success(MentorConversation("conv_test", "user_test", title))
            override fun getConversations(): Flow<List<MentorConversation>> =
                flowOf(listOf(MentorConversation("conv_test", "user_test", "Test Chat")))
            override suspend fun getConversation(conversationId: String): Result<MentorConversation> =
                Result.success(MentorConversation("conv_test", "user_test", "Test Chat"))
            override fun getMessages(conversationId: String): Flow<List<MentorChatMessage>> =
                flowOf(emptyList())
            override suspend fun sendMessage(
                conversationId: String,
                message: String,
                context: CGAMentorContext?
            ): Result<MentorChatMessage> = Result.success(
                MentorChatMessage(
                    id = "msg_model_100",
                    conversationId = conversationId,
                    userId = "user_test",
                    role = MentorRole.MODEL,
                    content = expectedReply,
                    createdAt = System.currentTimeMillis()
                )
            )
            override suspend fun deleteConversation(conversationId: String): Result<Unit> =
                Result.success(Unit)
        }

        val viewModel = MentorChatViewModel(customRepo)
        testScheduler.advanceUntilIdle()

        viewModel.onInputTextChanged("Give me an AI roadmap")
        viewModel.sendMessage()

        // Advance only slightly to check in-flight typewriter state
        testScheduler.advanceTimeBy(50)
        val inFlightState = viewModel.uiState.value
        assertNotNull(inFlightState.typingMessageId)
        assertEquals("msg_model_100", inFlightState.typingMessageId)
        val partial = inFlightState.typingDisplayedContent
        assertNotNull(partial)
        assertTrue("Partial content should be shorter than full text while typing", partial!!.length <= expectedReply.length)

        // Advance to completion
        testScheduler.advanceUntilIdle()
        val finishedState = viewModel.uiState.value
        assertNull(finishedState.typingMessageId)
        assertNull(finishedState.typingDisplayedContent)
        assertEquals(expectedReply, finishedState.messages.last().content)
    }

    // 3. Typewriter cancellation on new message or conversation switch
    @Test
    fun test3_typewriterCancellationOnSwitch() = runTest(testDispatcher) {
        val mockRepo = MockMentorRepository("user_test")
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        viewModel.sendMessage("First question")
        testScheduler.advanceTimeBy(30)

        // Switch conversation while typing
        viewModel.selectConversation("conv_demo_1")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.typingMessageId)
        assertNull(state.typingDisplayedContent)
    }

    // 4. Historical messages do not replay typewriter effect
    @Test
    fun test4_historicalMessagesDoNotReplayTypewriter() = runTest(testDispatcher) {
        val mockRepo = MockMentorRepository("user_test")
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasMessages)
        assertNull("Historical messages must not have active typingMessageId", state.typingMessageId)
        assertNull("Historical messages must not have typingDisplayedContent", state.typingDisplayedContent)
    }

    // 5. Send failure keeps conversation visible with error banner
    @Test
    fun test5_sendFailureKeepsConversationVisible() = runTest(testDispatcher) {
        val failingRepo = object : MentorRepository {
            override suspend fun createConversation(title: String): Result<MentorConversation> =
                Result.success(MentorConversation("conv_1", "user_1", title))
            override fun getConversations(): Flow<List<MentorConversation>> =
                flowOf(listOf(MentorConversation("conv_1", "user_1", "Existing Chat")))
            override suspend fun getConversation(conversationId: String): Result<MentorConversation> =
                Result.success(MentorConversation("conv_1", "user_1", "Existing Chat"))
            override fun getMessages(conversationId: String): Flow<List<MentorChatMessage>> =
                flowOf(listOf(
                    MentorChatMessage("m1", "conv_1", "user_1", MentorRole.USER, "Old user message", System.currentTimeMillis()),
                    MentorChatMessage("m2", "conv_1", "user_1", MentorRole.MODEL, "Old model reply", System.currentTimeMillis())
                ))
            override suspend fun sendMessage(
                conversationId: String,
                message: String,
                context: CGAMentorContext?
            ): Result<MentorChatMessage> = Result.failure(Exception("Network timeout"))
            override suspend fun deleteConversation(conversationId: String): Result<Unit> =
                Result.success(Unit)
        }

        val viewModel = MentorChatViewModel(failingRepo)
        testScheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onInputTextChanged("Will fail")
        viewModel.sendMessage()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSendingMessage)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Network timeout"))
        assertEquals("Existing messages must remain visible on failure", 2, state.messages.size)
        assertFalse("isLoadingMessages must remain false on send failure", state.isLoadingMessages)
    }

    // 6. Markdown bold parser removes raw ** and styles correctly
    @Test
    fun test6_markdownBoldParsing() {
        val raw = "Focus on **Data Structures** and **Algorithms** today."
        val annotated = parseInlineMarkdown(raw, isUser = false, baseColor = Color.Black)
        val plainText = annotated.text

        assertFalse("Plain text must not contain raw ** characters", plainText.contains("**"))
        assertEquals("Focus on Data Structures and Algorithms today.", plainText)
        assertTrue("AnnotatedString should contain bold span styles", annotated.spanStyles.isNotEmpty())
    }

    // 7. Markdown inline code parser renders monospace without raw backticks
    @Test
    fun test7_markdownInlineCodeParsing() {
        val raw = "Use `val x = 10` in Kotlin."
        val annotated = parseInlineMarkdown(raw, isUser = false, baseColor = Color.Black)
        val plainText = annotated.text

        assertFalse("Plain text must not contain raw backticks", plainText.contains("`"))
        assertEquals("Use val x = 10 in Kotlin.", plainText)
    }

    // 8. In-progress typing with open ** does not leak raw asterisks
    @Test
    fun test8_inProgressTypingUnclosedBold() {
        val inProgress = "Here is **Today's"
        val annotated = parseInlineMarkdown(inProgress, isUser = false, baseColor = Color.Black)
        val plainText = annotated.text

        assertFalse("In-progress typing must strip raw ** prefix", plainText.contains("**"))
        assertEquals("Here is Today's", plainText)
    }

    // 9. Raw bold markdown keywords (**text**, **Plan**, **Important**) are cleanly parsed without raw asterisks
    @Test
    fun test9_boldKeywordsCleanRendering() {
        val raw1 = "**text**"
        val annotated1 = parseInlineMarkdown(raw1, isUser = false, baseColor = Color.Black)
        assertEquals("text", annotated1.text)
        assertFalse(annotated1.text.contains("**"))
        assertTrue(annotated1.spanStyles.isNotEmpty())

        val raw2 = "**Plan**"
        val annotated2 = parseInlineMarkdown(raw2, isUser = false, baseColor = Color.Black)
        assertEquals("Plan", annotated2.text)
        assertFalse(annotated2.text.contains("**"))

        val raw3 = "**Important**"
        val annotated3 = parseInlineMarkdown(raw3, isUser = false, baseColor = Color.Black)
        assertEquals("Important", annotated3.text)
        assertFalse(annotated3.text.contains("**"))

        val raw4 = "**Important:** Here is the career action item."
        val annotated4 = parseInlineMarkdown(raw4, isUser = false, baseColor = Color.Black)
        assertEquals("Important: Here is the career action item.", annotated4.text)
        assertFalse(annotated4.text.contains("**"))
    }

    // 10. Triple asterisk (***bold and italic***) renders cleanly without raw asterisks
    @Test
    fun test10_boldItalicParsing() {
        val raw = "This is ***critical advice*** for interviews."
        val annotated = parseInlineMarkdown(raw, isUser = false, baseColor = Color.Black)
        assertEquals("This is critical advice for interviews.", annotated.text)
        assertFalse(annotated.text.contains("*"))
        assertTrue(annotated.spanStyles.isNotEmpty())
    }
}
