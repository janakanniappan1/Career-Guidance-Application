// Supabase Edge Function: cga-learning-tutor
// CGA (Career Guidance Application) - Source-Grounded AI Learning Companion Tutor
// Secure server-side bridge connecting Android App, Knowledge Base chunks, and Gemini API.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface ContextChunkDto {
  chunkId: string;
  documentName: string;
  pageNumber: number;
  text: string;
}

interface TutorRequestBody {
  question: string;
  contextChunks?: ContextChunkDto[];
}

interface ValidatedSourceDto {
  chunkId: string;
  documentName: string;
  pageNumber: number;
}

interface TutorResponseBody {
  success: boolean;
  answer: string;
  grounded: boolean;
  sources: ValidatedSourceDto[];
  unsupportedReason?: string;
  error?: string;
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
    let body: TutorRequestBody;
    try {
      body = JSON.parse(rawBody);
    } catch {
      return new Response(
        JSON.stringify({ success: false, error: "Invalid JSON request body." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const question = (body.question || "").trim();
    if (!question) {
      return new Response(
        JSON.stringify({ success: false, error: "Question cannot be empty." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const contextChunks = Array.isArray(body.contextChunks) ? body.contextChunks : [];

    // If no context chunks are supplied, refuse grounded answer immediately
    if (contextChunks.length === 0) {
      return new Response(
        JSON.stringify({
          success: true,
          answer: "I couldn't find this topic in your uploaded learning material.",
          grounded: false,
          sources: [],
          unsupportedReason: "No relevant content found in uploaded materials.",
        }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Map of valid chunk IDs provided in context
    const validChunkMap = new Map<string, ContextChunkDto>();
    for (const chunk of contextChunks) {
      if (chunk.chunkId) {
        validChunkMap.set(chunk.chunkId, chunk);
      }
    }

    // Retrieve server-side Gemini API key
    const geminiApiKey = Deno.env.get("GEMINI_API_KEY");
    if (!geminiApiKey) {
      console.error("GEMINI_API_KEY secret is not configured on server.");
      return new Response(
        JSON.stringify({
          success: false,
          error: "AI Tutor backend service is temporarily unavailable. Please try again shortly.",
        }),
        { status: 503, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const geminiModel = Deno.env.get("GEMINI_MODEL") || "gemini-2.5-flash";

    // System instruction enforcing strict source grounding
    const systemInstruction = `You are the AI Learning Companion tutor.

Answer the student's question using ONLY the supplied learning material context.

Do not invent facts that are not supported by the supplied context.

If the context does not contain enough information to answer confidently, state that the material does not provide enough information.

Do not use outside knowledge silently.

Keep explanations understandable for a college student.

When making a claim, associate it with the relevant source chunk identifier.

You must respond with valid JSON matching this schema:
{
  "answer": "string (the explanation grounded in the context)",
  "grounded": boolean (true if answered using context, false if material lacks info),
  "sources": [
    {
      "chunkId": "string (must match one of the provided chunk identifiers)",
      "documentName": "string",
      "pageNumber": number
    }
  ]
}`;

    // Format context chunks cleanly separated from question
    let contextBlock = "--- SUPPLIED LEARNING MATERIAL CONTEXT ---\n";
    contextChunks.forEach((chunk, index) => {
      contextBlock += `\nSOURCE ${index + 1} [ID: ${chunk.chunkId}]
Document: ${chunk.documentName}
Page: ${chunk.pageNumber}
Content:
${chunk.text}
`;
    });
    contextBlock += "\n--- END CONTEXT ---\n";

    const promptText = `${contextBlock}\nQUESTION:\n${question}`;

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
          temperature: 0.2, // Low temperature for factual precision
          maxOutputTokens: 1200,
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
          error: "Failed to communicate with AI Tutor model.",
        }),
        { status: 502, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const geminiData = await geminiResponse.json();
    const rawContent = geminiData.candidates?.[0]?.content?.parts?.[0]?.text || "{}";

    let parsedResult: { answer?: string; grounded?: boolean; sources?: ValidatedSourceDto[] };
    try {
      parsedResult = JSON.parse(rawContent);
    } catch {
      // Fallback clean extraction
      console.warn("Could not parse JSON directly from Gemini, using raw text fallback");
      parsedResult = {
        answer: rawContent.trim(),
        grounded: true,
        sources: contextChunks.map((c) => ({
          chunkId: c.chunkId,
          documentName: c.documentName,
          pageNumber: c.pageNumber,
        })),
      };
    }

    const answer = (parsedResult.answer || "").trim() || "No explanation could be generated from the material.";
    let isGrounded = parsedResult.grounded === true;

    // Strict validation: Validate returned source IDs against chunks actually provided
    const validatedSources: ValidatedSourceDto[] = [];
    if (Array.isArray(parsedResult.sources)) {
      for (const s of parsedResult.sources) {
        if (s.chunkId && validChunkMap.has(s.chunkId)) {
          const original = validChunkMap.get(s.chunkId)!;
          validatedSources.push({
            chunkId: original.chunkId,
            documentName: original.documentName,
            pageNumber: original.pageNumber,
          });
        }
      }
    }

    // If Gemini claims grounded but provided 0 valid sources, fallback to provided context chunks that matched query
    if (isGrounded && validatedSources.length === 0 && contextChunks.length > 0) {
      validatedSources.push({
        chunkId: contextChunks[0].chunkId,
        documentName: contextChunks[0].documentName,
        pageNumber: contextChunks[0].pageNumber,
      });
    }

    // If grounded is true but no validated source exists, ground status becomes false
    if (validatedSources.length === 0) {
      isGrounded = false;
    }

    const responsePayload: TutorResponseBody = {
      success: true,
      answer: answer,
      grounded: isGrounded,
      sources: validatedSources,
      unsupportedReason: isGrounded ? undefined : "The uploaded material does not contain sufficient details to answer this query.",
    };

    return new Response(JSON.stringify(responsePayload), {
      status: 200,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err: any) {
    console.error("Unhandled error in cga-learning-tutor:", err);
    return new Response(
      JSON.stringify({ success: false, error: err.message || "Internal server error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
