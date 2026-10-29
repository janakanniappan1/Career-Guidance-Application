package com.example.data.repository

import android.util.Log
import com.example.data.remote.SupabaseClientProvider
import com.example.domain.model.AiCareerGuidance
import com.example.domain.model.CareerGuidanceContext
import com.example.domain.repository.AiGuidanceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Implementation of AiGuidanceRepository that calls the server-side Supabase Edge Function
 * `generate-career-guidance` which communicates securely with the Gemini API.
 *
 * Security:
 * - No Gemini API key resides on the Android client.
 * - Authenticated Supabase session token is transmitted via Bearer Authorization header.
 * - Anon publishable key is supplied as the apikey header.
 * - Handles offline, network timeouts, edge function unavailability, and non-200 responses gracefully.
 */
class SupabaseAiGuidanceRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()
) : AiGuidanceRepository {

    companion object {
        private const val TAG = "AiGuidanceRepo"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    override suspend fun generateCareerGuidance(
        context: CareerGuidanceContext
    ): Result<AiCareerGuidance> = withContext(ioDispatcher) {
        val cleanSkills = context.careerSkills
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("and", ignoreCase = true) }

        val cleanContext = context.copy(careerSkills = cleanSkills)

        try {
            val baseUrl = SupabaseClientProvider.effectiveUrl.trimEnd('/')
            val endpointUrl = "$baseUrl/functions/v1/generate-career-guidance"
            val anonKey = SupabaseClientProvider.effectivePublishableKey

            // Retrieve current authenticated session token if available
            val sessionToken = client.auth.currentSessionOrNull()?.accessToken
                ?: client.auth.currentAccessTokenOrNull()
                ?: anonKey

            val payloadJson = JSONObject().apply {
                put("department", cleanContext.department)
                put("year", cleanContext.year)
                put("careerInterest", cleanContext.careerInterest)
                put("career", cleanContext.career)
                put("matchPercentage", cleanContext.matchPercentage)
                put("careerSkills", JSONArray(cleanContext.careerSkills))
            }

            val requestBody = payloadJson.toString().toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(endpointUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $sessionToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            Log.d(TAG, "Calling Supabase Edge Function for career '${cleanContext.career}' at $endpointUrl")

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val parsedExplanation = extractExplanationFromJson(responseBody)
                    val sanitized = sanitizeCareerGuidance(parsedExplanation, cleanContext)
                    if (sanitized.isNotBlank()) {
                        Log.d(TAG, "Successfully received personalized AI guidance for ${cleanContext.career}")
                        return@withContext Result.success(AiCareerGuidance(explanation = sanitized))
                    }
                } else {
                    Log.w(TAG, "Edge function call returned HTTP ${response.code}: $responseBody")
                }

                // If response is unparseable or edge function is unavailable, return deterministic career guidance
                val fallback = generateDeterministicCareerGuidance(cleanContext)
                Result.success(AiCareerGuidance(explanation = fallback))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Remote AI career guidance unavailable (${e.javaClass.simpleName}: ${e.message}), gracefully using deterministic fallback")
            val fallback = generateDeterministicCareerGuidance(cleanContext)
            Result.success(AiCareerGuidance(explanation = fallback))
        }
    }

    private fun extractExplanationFromJson(rawJson: String): String {
        var text = rawJson.trim()
        if (text.startsWith("```json", ignoreCase = true)) {
            text = text.removePrefix("```json").removePrefix("```JSON").trim()
        }
        if (text.startsWith("```")) {
            text = text.removePrefix("```").trim()
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```").trim()
        }

        return try {
            val jsonObject = JSONObject(text)
            jsonObject.optString("guidance", jsonObject.optString("explanation", jsonObject.optString("text", "")))
        } catch (_: Exception) {
            text
        }
    }

    private fun sanitizeCareerGuidance(raw: String, context: CareerGuidanceContext): String {
        var cleaned = raw.trim()

        // Strip markdown code fences if any
        if (cleaned.startsWith("```json", ignoreCase = true)) {
            cleaned = cleaned.removePrefix("```json").removePrefix("```JSON").trim()
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").trim()
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trim()
        }

        // If the string itself is a JSON object, extract guidance or explanation
        try {
            if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
                val jsonObject = JSONObject(cleaned)
                cleaned = jsonObject.optString("guidance", jsonObject.optString("explanation", "")).trim()
            }
        } catch (_: Exception) {
            // Keep current cleaned string
        }

        // Strip surrounding JSON quotes if present
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length >= 2) {
            cleaned = cleaned.substring(1, cleaned.length - 1).trim()
        }

        // Instruction / meta-text leakage patterns to reject
        val forbiddenPatterns = listOf(
            Regex("\\bSentence\\s*\\d+\\b", RegexOption.IGNORE_CASE),
            Regex("\\bEncouragement\\b", RegexOption.IGNORE_CASE),
            Regex("\\bConnection\\b", RegexOption.IGNORE_CASE),
            Regex("\\bNext\\s*step\\s*:", RegexOption.IGNORE_CASE),
            Regex("\\bInstructions?\\s*:", RegexOption.IGNORE_CASE),
            Regex("\\bPrompt\\s*:", RegexOption.IGNORE_CASE),
            Regex("\\bAnalysis\\s*:", RegexOption.IGNORE_CASE),
            Regex("\\bReasoning\\s*:", RegexOption.IGNORE_CASE),
            Regex("\\bOutput\\s*:", RegexOption.IGNORE_CASE),
            Regex("\\bJSON\\s*:", RegexOption.IGNORE_CASE),
            Regex("^\\s*(?:Here\\s+is|Here’s|Heres)\\b", RegexOption.IGNORE_CASE),
            Regex("\\(\\s*(?:Encouragement|Connection|Next step)[^)]*\\)", RegexOption.IGNORE_CASE)
        )

        for (pattern in forbiddenPatterns) {
            if (pattern.containsMatchIn(cleaned)) {
                Log.w(TAG, "Rejected guidance containing prompt/instruction leakage matching: ${pattern.pattern}")
                return generateDeterministicCareerGuidance(context)
            }
        }

        // Reject markdown headers or bullet point lists
        if (Regex("^#+\\s+", RegexOption.MULTILINE).containsMatchIn(cleaned) ||
            Regex("^\\s*[-*•]\\s+", RegexOption.MULTILINE).containsMatchIn(cleaned)
        ) {
            Log.w(TAG, "Rejected guidance containing markdown headers or bullet points")
            return generateDeterministicCareerGuidance(context)
        }

        // Clean orphan conjunctions and trailing artifacts
        cleaned = cleaned.replace(Regex("^(?:and\\s+|&\\s+|,\\s+)+", RegexOption.IGNORE_CASE), "").trim()
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

        // Check if text is sufficiently complete and not malformed
        if (cleaned.length >= 35 && !cleaned.startsWith("and ", ignoreCase = true) && !cleaned.startsWith("& ", ignoreCase = true)) {
            return cleaned
        }

        return generateDeterministicCareerGuidance(context)
    }

    private fun generateDeterministicCareerGuidance(context: CareerGuidanceContext): String {
        val careerName = context.career.ifBlank { "Software & Technology" }.trim()
        val matchStr = if (context.matchPercentage > 0) "${context.matchPercentage}%" else "a strong"
        val dept = context.department.trim()
        val year = context.year.trim()
        val rawYear = if (year.isNotBlank()) {
            if (year.contains("year", ignoreCase = true)) year else "$year-year"
        } else ""

        val skills = context.careerSkills
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("and", ignoreCase = true) }

        val skillText = when {
            skills.size >= 2 -> "${skills[0]} and ${skills[1]}"
            skills.size == 1 -> skills[0]
            else -> ""
        }

        return when {
            dept.isNotBlank() && rawYear.isNotBlank() && skillText.isNotBlank() ->
                "Your $matchStr compatibility match with $careerName is a solid starting point, particularly with your focus on $skillText. As a $rawYear $dept student, strengthening core domain foundations and building practical portfolio projects will help you advance confidently toward this career."
            dept.isNotBlank() && skillText.isNotBlank() ->
                "Your $matchStr compatibility match with $careerName aligns well with your studies in $dept and your interest in $skillText. Focus on hands-on practical exercises and building small projects that showcase your applied abilities."
            skillText.isNotBlank() ->
                "Your $matchStr compatibility match with $careerName gives you a clear direction to build toward, especially when backed by skills like $skillText. Applying these concepts to real-world problems will help you move steadily toward this role."
            dept.isNotBlank() && rawYear.isNotBlank() ->
                "Your $matchStr compatibility match with $careerName gives you an encouraging roadmap to follow. As a $rawYear $dept student, taking on milestone challenges and consistent daily learning will help you gain valuable momentum."
            else ->
                "Your $matchStr compatibility match with $careerName provides a clear target for your career development. Consistent practice, completing recommended roadmap milestones, and working on practical projects will help you progress toward this path."
        }
    }
}
