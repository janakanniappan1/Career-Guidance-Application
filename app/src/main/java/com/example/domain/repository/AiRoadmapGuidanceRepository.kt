package com.example.domain.repository

import com.example.domain.model.AiRoadmapGuidance
import com.example.domain.model.RoadmapGuidanceContext

/**
 * Repository interface for AI-powered personalized roadmap guidance.
 * Analyzes the student's actual individual roadmap progress and requests next-step guidance
 * from the server-side backend (Supabase Edge Function + Gemini).
 */
interface AiRoadmapGuidanceRepository {
    suspend fun generateRoadmapGuidance(
        context: RoadmapGuidanceContext
    ): Result<AiRoadmapGuidance>
}
