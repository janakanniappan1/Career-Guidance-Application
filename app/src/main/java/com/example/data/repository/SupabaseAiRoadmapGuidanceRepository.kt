package com.example.data.repository

import android.util.Log
import com.example.data.remote.SupabaseClientProvider
import com.example.domain.model.AiRoadmapGuidance
import com.example.domain.model.RoadmapGuidanceContext
import com.example.domain.repository.AiRoadmapGuidanceRepository
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
 * Implementation of AiRoadmapGuidanceRepository that calls the server-side Supabase Edge Function
 * `generate-roadmap-guidance` which communicates securely with the Gemini API.
 *
 * Security:
 * - No Gemini API key resides on the Android client.
 * - Authenticated Supabase session token is transmitted via Bearer Authorization header.
 * - Handles offline, network timeouts, edge function unavailability, and non-200 responses gracefully.
 */
class SupabaseAiRoadmapGuidanceRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()
) : AiRoadmapGuidanceRepository {

    companion object {
        private const val TAG = "AiRoadmapGuidanceRepo"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    override suspend fun generateRoadmapGuidance(
        context: RoadmapGuidanceContext
    ): Result<AiRoadmapGuidance> = withContext(ioDispatcher) {
        val cleanCompleted = context.completedTasks
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("and", ignoreCase = true) }

        val cleanPending = context.pendingTasks
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("and", ignoreCase = true) }

        val cleanNext = context.nextTasks
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("and", ignoreCase = true) }

        val cleanContext = context.copy(
            completedTasks = cleanCompleted,
            pendingTasks = cleanPending,
            nextTasks = cleanNext
        )

        try {
            val baseUrl = SupabaseClientProvider.effectiveUrl.trimEnd('/')
            val endpointUrl = "$baseUrl/functions/v1/generate-roadmap-guidance"
            val anonKey = SupabaseClientProvider.effectivePublishableKey

            // Retrieve current authenticated session token
            val sessionToken = client.auth.currentSessionOrNull()?.accessToken
                ?: client.auth.currentAccessTokenOrNull()
                ?: anonKey

            val payloadJson = JSONObject().apply {
                put("career", cleanContext.career)
                put("overallProgressPercentage", cleanContext.overallProgressPercentage)
                put("completedTasks", JSONArray(cleanContext.completedTasks))
                put("pendingTasks", JSONArray(cleanContext.pendingTasks))
                put("currentStage", cleanContext.currentStage)
                put("nextTasks", JSONArray(cleanContext.nextTasks))
                put("skillCategories", JSONArray(cleanContext.skillCategories))
            }

            val requestBody = payloadJson.toString().toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(endpointUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $sessionToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            Log.d(TAG, "Calling Supabase Edge Function for roadmap '${cleanContext.career}' at $endpointUrl")

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val parsedGuidance = extractGuidanceFromJson(responseBody)
                    val sanitized = sanitizeRoadmapGuidance(parsedGuidance, cleanContext)
                    if (sanitized.isNotBlank()) {
                        Log.d(TAG, "Successfully received personalized AI roadmap guidance for ${cleanContext.career}")
                        return@withContext Result.success(AiRoadmapGuidance(guidance = sanitized))
                    }
                } else {
                    Log.w(TAG, "Edge function call returned HTTP ${response.code}: $responseBody")
                }

                // Fallback to deterministic guidance
                val fallback = generateDeterministicRoadmapGuidance(cleanContext)
                Result.success(AiRoadmapGuidance(guidance = fallback))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Remote AI roadmap guidance unavailable (${e.javaClass.simpleName}: ${e.message}), gracefully using deterministic fallback")
            val fallback = generateDeterministicRoadmapGuidance(cleanContext)
            Result.success(AiRoadmapGuidance(guidance = fallback))
        }
    }

    private fun extractGuidanceFromJson(rawJson: String): String {
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

    private fun sanitizeRoadmapGuidance(raw: String, context: RoadmapGuidanceContext): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json", ignoreCase = true)) {
            cleaned = cleaned.removePrefix("```json").removePrefix("```JSON").trim()
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").trim()
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trim()
        }

        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length >= 2) {
            cleaned = cleaned.substring(1, cleaned.length - 1).trim()
        }

        cleaned = cleaned.replace(Regex("^(?:and\\s+|&\\s+|,\\s+)+", RegexOption.IGNORE_CASE), "").trim()
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

        if (cleaned.length >= 25 && !cleaned.startsWith("and ", ignoreCase = true)) {
            return cleaned
        }

        return generateDeterministicRoadmapGuidance(context)
    }

    private fun generateDeterministicRoadmapGuidance(context: RoadmapGuidanceContext): String {
        val stageMention = if (context.currentStage.isNotBlank()) {
            "You are currently progressing through ${context.currentStage} at ${context.overallProgressPercentage}% completion."
        } else {
            "You have achieved ${context.overallProgressPercentage}% completion on your ${context.career} roadmap."
        }

        val completedMention = if (context.completedTasks.isNotEmpty()) {
            val taskSample = if (context.completedTasks.size >= 2) {
                "${context.completedTasks[0]} and ${context.completedTasks[1]}"
            } else {
                context.completedTasks[0]
            }
            "Having completed checkpoints such as $taskSample, you have built solid foundational momentum."
        } else {
            "As you begin this curriculum, establishing structured weekly study blocks will help you build steady momentum."
        }

        val nextTask = context.nextTasks.firstOrNull() ?: context.pendingTasks.firstOrNull()
        val nextMention = if (!nextTask.isNullOrBlank()) {
            "Your immediate priority should be on mastering '$nextTask'."
        } else {
            "You have completed all primary roadmap checkpoints for this curriculum."
        }

        return "$stageMention $completedMention $nextMention Consistent daily practice on these active checkpoints will keep you on track for campus placements."
    }
}
