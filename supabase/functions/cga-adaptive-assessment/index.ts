// Supabase Edge Function: cga-adaptive-assessment
// CGA (Career Guidance Application) - AI Learning Companion Adaptive Assessment
// Generates source-grounded assessment questions based on extracted Knowledge Base chunks.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface KnowledgeChunkPayload {
  chunkId: string;
  documentName: string;
  pageNumber: number;
  topic: string;
  concept: string;
  text: string;
}

interface AssessmentRequestBody {
  scope?: string;
  difficulty?: string;
  questionCount?: number;
  knowledgeChunks?: KnowledgeChunkPayload[];
}

interface RawQuestionDto {
  question?: string;
  type?: string;
  options?: string[];
  correctAnswer?: string;
  explanation?: string;
  topic?: string;
  concept?: string;
  difficulty?: string;
  sourceChunkId?: string;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({ success: false, error: "Method not allowed. Use POST." }),
      { status: 405, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }

  try {
    const rawBody = await req.text();
    let body: AssessmentRequestBody;
    try {
      body = JSON.parse(rawBody);
    } catch {
      return new Response(
        JSON.stringify({ success: false, error: "Invalid JSON request body." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const chunks = Array.isArray(body.knowledgeChunks) ? body.knowledgeChunks : [];
    if (chunks.length === 0) {
      return new Response(
        JSON.stringify({
          success: false,
          error: "Build a Knowledge Base before creating an assessment.",
        }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const requestedCount = Math.min(Math.max(body.questionCount || 5, 3), 15);
    const scope = (body.scope || "All Topics").trim();
    const difficulty = (body.difficulty || "Mixed").trim();

    // Map valid chunk IDs for verification
    const validChunkIds = new Set(chunks.map((c) => c.chunkId));

    const geminiApiKey = Deno.env.get("GEMINI_API_KEY");
    if (!geminiApiKey) {
      console.error("GEMINI_API_KEY is not configured.");
      return new Response(
        JSON.stringify({
          success: false,
          error: "AI Assessment service is temporarily unavailable. Please try again shortly.",
        }),
        { status: 503, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const geminiModel = Deno.env.get("GEMINI_MODEL") || "gemini-2.5-flash";

    const systemInstruction = `You are the AI Learning Companion assessment generator.
Your job is to generate adaptive assessment questions strictly from the supplied learning material context chunks.

RULES:
1. Generate questions ONLY from facts stated directly in the supplied context chunks.
2. Do NOT invent concepts, facts, or questions that cannot be answered using the provided text.
3. Every question MUST cite the exact sourceChunkId from which it was generated.
4. Support two question types: "MCQ" (Multiple Choice) and "SHORT_ANSWER". Prefer "MCQ" for at least 80% of questions.
5. For MCQ questions, provide EXACTLY 4 distinct options in the "options" array.
6. The "correctAnswer" MUST be an exact match to one of the 4 options for MCQ.
7. Provide a clear, educational "explanation" referencing the material.
8. Set the "topic", "concept", and "difficulty" ("Easy", "Medium", "Hard") according to the question.
9. Target count: ${requestedCount} questions. Scope: "${scope}". Desired difficulty level: "${difficulty}".

You MUST respond with valid JSON matching this schema:
{
  "questions": [
    {
      "question": "string (the question prompt)",
      "type": "MCQ" or "SHORT_ANSWER",
      "options": ["Option A", "Option B", "Option C", "Option D"],
      "correctAnswer": "Option A",
      "explanation": "string (explanation grounded in the source)",
      "topic": "string",
      "concept": "string",
      "difficulty": "Easy" | "Medium" | "Hard",
      "sourceChunkId": "string (must strictly match one of the provided chunk identifiers)"
    }
  ]
}`;

    // Format context chunks cleanly
    let contextBlock = "--- SUPPLIED LEARNING MATERIAL CHUNKS ---\n";
    chunks.forEach((chunk, index) => {
      contextBlock += `\nCHUNK ${index + 1} [ID: ${chunk.chunkId}]
Document: ${chunk.documentName} (Page ${chunk.pageNumber})
Topic: ${chunk.topic} | Concept: ${chunk.concept}
Text:
${chunk.text}
`;
    });
    contextBlock += "\n--- END CONTEXT ---\n";

    const promptText = `${contextBlock}\nGENERATE ${requestedCount} ASSESSMENT QUESTIONS (Scope: ${scope}, Difficulty: ${difficulty}).`;

    const geminiEndpoint = `https://generativelanguage.googleapis.com/v1beta/models/${geminiModel}:generateContent?key=${geminiApiKey}`;

    const geminiResponse = await fetch(geminiEndpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        system_instruction: {
          parts: [{ text: systemInstruction }],
        },
        contents: [
          {
            role: "user",
            parts: [{ text: promptText }],
          },
        ],
        generationConfig: {
          temperature: 0.2,
          maxOutputTokens: 2500,
          responseMimeType: "application/json",
        },
      }),
    });

    if (!geminiResponse.ok) {
      const errText = await geminiResponse.text();
      console.error(`Gemini API error ${geminiResponse.status}:`, errText);
      return new Response(
        JSON.stringify({
          success: false,
          error: "Failed to generate assessment questions from AI model.",
        }),
        { status: 502, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const geminiData = await geminiResponse.json();
    const rawContent = geminiData.candidates?.[0]?.content?.parts?.[0]?.text || "{}";

    let parsedResult: { questions?: RawQuestionDto[] };
    try {
      parsedResult = JSON.parse(rawContent);
    } catch {
      console.warn("Failed to parse JSON directly from Gemini:", rawContent);
      return new Response(
        JSON.stringify({
          success: false,
          error: "Unable to parse generated questions.",
        }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const rawQuestions = Array.isArray(parsedResult.questions) ? parsedResult.questions : [];
    const validQuestions: RawQuestionDto[] = [];

    // Filter server-side against hallucinated sourceChunkIds
    for (const q of rawQuestions) {
      if (
        q.question &&
        q.question.trim().length > 0 &&
        q.correctAnswer &&
        q.sourceChunkId &&
        validChunkIds.has(q.sourceChunkId)
      ) {
        validQuestions.push(q);
      }
    }

    return new Response(
      JSON.stringify({
        success: true,
        questions: validQuestions,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err: any) {
    console.error("Unhandled error in cga-adaptive-assessment:", err);
    return new Response(
      JSON.stringify({ success: false, error: err.message || "Internal server error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
