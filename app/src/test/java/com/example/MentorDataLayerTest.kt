package com.example

import com.example.data.model.ChatConversationDto
import com.example.data.model.ChatMessageContentDto
import com.example.data.model.ChatMessageDto
import com.example.data.model.SendMentorMessageRequestDto
import com.example.data.model.SendMentorMessageResponseDto
import com.example.data.repository.MockMentorRepository
import com.example.domain.model.CGAMentorContext
import com.example.domain.model.MentorChatMessage
import com.example.domain.model.MentorConversation
import com.example.domain.model.MentorRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MentorDataLayerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun test1_conversationModelMapping() {
        val dto = ChatConversationDto(
            id = "conv_123",
            userId = "user_456",
            title = "Technical Placement Guidance",
            createdAt = "2026-10-01T10:00:00Z",
            updatedAt = "2026-10-01T10:30:00Z"
        )
        val domain: MentorConversation = dto.toDomain()

        assertEquals("conv_123", domain.id)
        assertEquals("user_456", domain.userId)
        assertEquals("Technical Placement Guidance", domain.title)
        assertNotNull(domain.createdAt)
        assertNotNull(domain.updatedAt)

        val convertedDto = ChatConversationDto.fromDomain(domain)
        assertEquals("conv_123", convertedDto.id)
        assertEquals("user_456", convertedDto.userId)
        assertEquals("Technical Placement Guidance", convertedDto.title)
    }

    @Test
    fun test2_messageModelMapping() {
        val userDto = ChatMessageDto(
            id = "msg_1",
            conversationId = "conv_123",
            userId = "user_456",
            role = "user",
            content = "Explain binary search",
            createdAt = "2026-10-01T10:05:00Z"
        )
        val userDomain: MentorChatMessage = userDto.toDomain()
        assertEquals("msg_1", userDomain.id)
        assertEquals(MentorRole.USER, userDomain.role)
        assertEquals("Explain binary search", userDomain.content)

        val modelDto = ChatMessageDto(
            id = "msg_2",
            conversationId = "conv_123",
            userId = "user_456",
            role = "model",
            content = "Binary search divides the search interval in half...",
            createdAt = "2026-10-01T10:05:02Z"
        )
        val modelDomain: MentorChatMessage = modelDto.toDomain()
        assertEquals(MentorRole.MODEL, modelDomain.role)

        val reconvertedUser = ChatMessageDto.fromDomain(userDomain)
        assertEquals("user", reconvertedUser.role)
        val reconvertedModel = ChatMessageDto.fromDomain(modelDomain)
        assertEquals("model", reconvertedModel.role)
    }

    @Test
    fun test3_edgeFunctionRequestSerialization() {
        val context = CGAMentorContext(
            studentName = "Rahul",
            department = "CSE",
            yearOfStudy = "3rd Year",
            recommendedCareer = "AI / Machine Learning Engineer",
            matchPercentage = 88,
            currentStreak = 12,
            xp = 2400
        )
        val request = SendMentorMessageRequestDto(
            conversationId = "550e8400-e29b-41d4-a716-446655440000",
            message = "What projects should I build next?",
            context = context
        )

        val serialized = json.encodeToString(request)
        assertTrue(serialized.contains("550e8400-e29b-41d4-a716-446655440000"))
        assertTrue(serialized.contains("What projects should I build next?"))
        assertTrue(serialized.contains("Rahul"))
        assertTrue(serialized.contains("88"))
        assertTrue(serialized.contains("2400"))
    }

    @Test
    fun test4_successfulEdgeFunctionResponseParsing() {
        val rawJson = """
            {
              "success": true,
              "conversation_id": "550e8400-e29b-41d4-a716-446655440000",
              "message": {
                "role": "model",
                "content": "To build a strong portfolio, create an end-to-end ML deployment pipeline."
              }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<SendMentorMessageResponseDto>(rawJson)
        assertTrue(parsed.success)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", parsed.conversationId)
        assertNotNull(parsed.message)
        assertEquals("model", parsed.message?.role)
        assertEquals("To build a strong portfolio, create an end-to-end ML deployment pipeline.", parsed.message?.content)
    }

    @Test
    fun test5_errorResponseParsing() {
        val rawErrorJson = """
            {
              "success": false,
              "error": "Conversation not found or access denied."
            }
        """.trimIndent()

        val parsed = json.decodeFromString<SendMentorMessageResponseDto>(rawErrorJson)
        assertFalse(parsed.success)
        assertNull(parsed.message)
        assertEquals("Conversation not found or access denied.", parsed.error)
    }

    @Test
    fun test6_emptyMessageValidation() = runBlocking {
        val repository = MockMentorRepository("user_test")
        val conv = repository.createConversation("Test").getOrThrow()

        val emptyResult = repository.sendMessage(conv.id, "   ", null)
        assertTrue(emptyResult.isFailure)
        assertTrue(emptyResult.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun test7_conversationOrdering_updatedAtDesc() = runBlocking {
        val repository = MockMentorRepository("user_test")
        val conv1 = repository.createConversation("Alpha").getOrThrow()
        val conv2 = repository.createConversation("Beta").getOrThrow()

        // Sending a message to conv1 should update its timestamp and move it to top
        repository.sendMessage(conv1.id, "Hello Alpha", null)

        val convs = repository.getConversations().first()
        assertTrue(convs.isNotEmpty())
        assertEquals(conv1.id, convs.first().id)
    }

    @Test
    fun test8_messageOrdering_createdAtAsc() = runBlocking {
        val repository = MockMentorRepository("user_test")
        val conv = repository.createConversation("Ordering Test").getOrThrow()

        repository.sendMessage(conv.id, "First Question", null)
        repository.sendMessage(conv.id, "Second Question", null)

        val messages = repository.getMessages(conv.id).first()
        assertEquals(4, messages.size) // 2 user messages + 2 model replies
        assertEquals(MentorRole.USER, messages[0].role)
        assertEquals("First Question", messages[0].content)
        assertEquals(MentorRole.MODEL, messages[1].role)
        assertEquals(MentorRole.USER, messages[2].role)
        assertEquals("Second Question", messages[2].content)
        assertEquals(MentorRole.MODEL, messages[3].role)
    }

    @Test
    fun test9_contextSerializationWithNullableFields() {
        val sparseContext = CGAMentorContext(
            studentName = "Ananya",
            department = null,
            yearOfStudy = null,
            careerInterest = null,
            recommendedCareer = null,
            matchPercentage = null
        )

        val serialized = json.encodeToString(sparseContext)
        val deserialized = json.decodeFromString<CGAMentorContext>(serialized)

        assertEquals("Ananya", deserialized.studentName)
        assertNull(deserialized.department)
        assertNull(deserialized.matchPercentage)
        assertNull(deserialized.currentStreak)
    }

    @Test
    fun test10_edgeFunctionAtomicMessageContract() {
        // Confirms that SendMentorMessageRequestDto takes the single user message turn,
        // and SendMentorMessageResponseDto returns the generated model reply.
        val req = SendMentorMessageRequestDto(
            conversationId = "conv_1",
            message = "Explain dynamic programming"
        )
        val res = SendMentorMessageResponseDto(
            success = true,
            conversationId = "conv_1",
            message = ChatMessageContentDto(
                role = "model",
                content = "Dynamic programming is an optimization technique..."
            )
        )
        assertEquals("Explain dynamic programming", req.message)
        assertEquals("model", res.message?.role)
    }

    @Test
    fun test11_networkErrorYieldsFailureNotFakeAi() {
        val failedResponseJson = """
            {
              "success": false,
              "error": "The AI Mentor could not process your request at this moment. Please try again."
            }
        """.trimIndent()

        val parsed = json.decodeFromString<SendMentorMessageResponseDto>(failedResponseJson)
        assertFalse(parsed.success)
        assertEquals("The AI Mentor could not process your request at this moment. Please try again.", parsed.error)
    }

    @Test
    fun test12_roleParsingRobustness() {
        assertEquals(MentorRole.USER, MentorRole.fromString("user"))
        assertEquals(MentorRole.USER, MentorRole.fromString("USER"))
        assertEquals(MentorRole.MODEL, MentorRole.fromString("model"))
        assertEquals(MentorRole.MODEL, MentorRole.fromString("MODEL"))
        assertEquals(MentorRole.MODEL, MentorRole.fromString("assistant"))
        assertEquals(MentorRole.MODEL, MentorRole.fromString(null))
        assertEquals(MentorRole.MODEL, MentorRole.fromString("unknown_role"))
    }
}
