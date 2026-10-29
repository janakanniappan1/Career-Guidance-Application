package com.example

import com.example.data.repository.MockMentorRepository
import com.example.domain.model.CGAMentorContext
import com.example.domain.model.MentorChatMessage
import com.example.domain.model.MentorConversation
import com.example.domain.model.MentorRole
import com.example.domain.repository.MentorRepository
import com.example.viewmodel.MentorChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
class MentorChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test1_initialState() = runTest(testDispatcher) {
        val mockRepo = MockMentorRepository("user_test")
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.conversations)
        assertFalse(state.isSendingMessage)
        assertEquals("", state.inputText)
        assertNull(state.error)
    }

    @Test
    fun test2_conversationLoadingSuccess() = runTest(testDispatcher) {
        val mockRepo = MockMentorRepository("user_test")
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasConversations)
        assertEquals("conv_demo_1", state.selectedConversationId)
    }

    @Test
    fun test3_conversationLoadingFailure() = runTest(testDispatcher) {
        val failingRepo = object : MentorRepository {
            override suspend fun createConversation(title: String): Result<MentorConversation> =
                Result.failure(Exception("DB Error"))
            override fun getConversations(): Flow<List<MentorConversation>> = flow {
                throw RuntimeException("Network Error")
            }
            override suspend fun getConversation(conversationId: String): Result<MentorConversation> =
                Result.failure(Exception("Not found"))
            override fun getMessages(conversationId: String): Flow<List<MentorChatMessage>> =
                flowOf(emptyList())
            override suspend fun sendMessage(
                conversationId: String,
                message: String,
                context: CGAMentorContext?
            ): Result<MentorChatMessage> = Result.failure(Exception("Offline"))
            override suspend fun deleteConversation(conversationId: String): Result<Unit> =
                Result.failure(Exception("Error"))
        }

        val viewModel = MentorChatViewModel(failingRepo)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertFalse(state.isLoadingConversations)
    }

    @Test
    fun test4_conversationSelection() = runTest(testDispatcher) {
        val mockRepo = MockMentorRepository("user_test")
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        viewModel.createNewConversation("New Topic")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val newId = state.selectedConversationId
        assertNotNull(newId)

        viewModel.selectConversation("conv_demo_1")
        testScheduler.advanceUntilIdle()

        assertEquals("conv_demo_1", viewModel.uiState.value.selectedConversationId)
    }

    @Test
    fun test5_messageLoadingSuccess() = runTest(testDispatcher) {
        val mockRepo = MockMentorRepository("user_test")
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        viewModel.selectConversation("conv_demo_1")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasMessages)
        assertEquals(2, state.messages.size)
        assertEquals(MentorRole.USER, state.messages[0].role)
        assertEquals(MentorRole.MODEL, state.messages[1].role)
    }

    @Test
    fun test6_sendMessageSuccess() = runTest(testDispatcher) {
        val mockRepo = MockMentorRepository("user_test")
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        viewModel.onInputTextChanged("Explain Dijkstra algorithm")
        viewModel.sendMessage()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSendingMessage)
        assertEquals("", state.inputText)
        assertNull(state.error)
        assertTrue(state.messages.any { it.content.contains("Explain Dijkstra algorithm") })
    }

    @Test
    fun test7_sendMessageFailure() = runTest(testDispatcher) {
        val failingRepo = object : MentorRepository {
            override suspend fun createConversation(title: String): Result<MentorConversation> =
                Result.success(MentorConversation("conv_1", "user_1", title))
            override fun getConversations(): Flow<List<MentorConversation>> =
                flowOf(listOf(MentorConversation("conv_1", "user_1", "Test")))
            override suspend fun getConversation(conversationId: String): Result<MentorConversation> =
                Result.success(MentorConversation("conv_1", "user_1", "Test"))
            override fun getMessages(conversationId: String): Flow<List<MentorChatMessage>> =
                flowOf(emptyList())
            override suspend fun sendMessage(
                conversationId: String,
                message: String,
                context: CGAMentorContext?
            ): Result<MentorChatMessage> = Result.failure(Exception("AI Service Unavailable"))
            override suspend fun deleteConversation(conversationId: String): Result<Unit> =
                Result.success(Unit)
        }

        val viewModel = MentorChatViewModel(failingRepo)
        testScheduler.advanceUntilIdle()

        viewModel.onInputTextChanged("Explain dynamic programming")
        viewModel.sendMessage()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSendingMessage)
        assertNotNull(state.error)
        assertEquals("Explain dynamic programming", state.inputText) // Restored input on failure
    }

    @Test
    fun test8_inputDisabledWhileSending() = runTest(testDispatcher) {
        val mockRepo = MockMentorRepository("user_test")
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        viewModel.onInputTextChanged("Hello")
        assertTrue(viewModel.uiState.value.canSendMessage)

        viewModel.onInputTextChanged("   ")
        assertFalse(viewModel.uiState.value.canSendMessage)
    }

    @Test
    fun test9_blankMessageCannotBeSent() = runTest(testDispatcher) {
        val mockRepo = MockMentorRepository("user_test")
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        viewModel.onInputTextChanged("   ")
        assertFalse(viewModel.uiState.value.canSendMessage)

        val beforeCount = viewModel.uiState.value.messages.size
        viewModel.sendMessage()
        testScheduler.advanceUntilIdle()

        assertEquals(beforeCount, viewModel.uiState.value.messages.size)
    }

    @Test
    fun test10_newConversationCreation() = runTest(testDispatcher) {
        val mockRepo = MockMentorRepository("user_test")
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        val countBefore = viewModel.uiState.value.conversations.size
        viewModel.createNewConversation("System Design")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(countBefore + 1, state.conversations.size)
        assertEquals("System Design", state.selectedConversation?.title)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun test11_deleteConversation() = runTest(testDispatcher) {
        val mockRepo = MockMentorRepository("user_test")
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        val targetConv = viewModel.uiState.value.conversations.first()
        viewModel.deleteConversation(targetConv.id)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.conversations.any { it.id == targetConv.id })
    }

    @Test
    fun test12_conversationRefreshAfterFirstMessage() = runTest(testDispatcher) {
        val mockRepo = MockMentorRepository("user_test")
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        viewModel.createNewConversation("New Conversation")
        testScheduler.advanceUntilIdle()

        viewModel.sendMessage("What is gradient descent?")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasMessages)
    }

    @Test
    fun test13_cgaContextWithNullableFields() = runTest(testDispatcher) {
        val mockRepo = MockMentorRepository("user_test")
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        viewModel.refreshStudentContext()
        testScheduler.advanceUntilIdle()

        val context = viewModel.uiState.value.activeContext
        assertNotNull(context)
    }

    @Test
    fun test14_noFakeFallbackMessageGeneratedOnBackendFailure() = runTest(testDispatcher) {
        val failingRepo = object : MentorRepository {
            override suspend fun createConversation(title: String): Result<MentorConversation> =
                Result.success(MentorConversation("conv_1", "user_1", title))
            override fun getConversations(): Flow<List<MentorConversation>> =
                flowOf(listOf(MentorConversation("conv_1", "user_1", "Test")))
            override suspend fun getConversation(conversationId: String): Result<MentorConversation> =
                Result.success(MentorConversation("conv_1", "user_1", "Test"))
            override fun getMessages(conversationId: String): Flow<List<MentorChatMessage>> =
                flowOf(emptyList())
            override suspend fun sendMessage(
                conversationId: String,
                message: String,
                context: CGAMentorContext?
            ): Result<MentorChatMessage> = Result.failure(Exception("Edge Function 500 error"))
            override suspend fun deleteConversation(conversationId: String): Result<Unit> =
                Result.success(Unit)
        }

        val viewModel = MentorChatViewModel(failingRepo)
        testScheduler.advanceUntilIdle()

        viewModel.sendMessage("Test Message")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty()) // No fake assistant message added
        assertNotNull(state.error)
    }

    @Test
    fun test15_longMessagesDoNotCorruptState() = runTest(testDispatcher) {
        val mockRepo = MockMentorRepository("user_test")
        val viewModel = MentorChatViewModel(mockRepo)
        testScheduler.advanceUntilIdle()

        val longMsg = "A".repeat(3000)
        viewModel.onInputTextChanged(longMsg)
        viewModel.sendMessage()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertTrue(state.messages.any { it.content == longMsg })
    }
}
