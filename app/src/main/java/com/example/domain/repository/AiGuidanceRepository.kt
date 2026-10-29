package com.example.domain.repository

import com.example.domain.model.AiCareerGuidance
import com.example.domain.model.CareerGuidanceContext

/**
 * Repository interface for AI-powered personalized career guidance.
 * Requests explanation for recommended career from the server-side backend (Supabase Edge Function + Gemini).
 */
interface AiGuidanceRepository {
    suspend fun generateCareerGuidance(
        context: CareerGuidanceContext
    ): Result<AiCareerGuidance>
}
