// Supabase Edge Function: generate-roadmap-guidance
// CGA (Career Guidance Application) - AI Feature 2: Personalized Roadmap Guidance
// Lightweight secure server-side bridge between Android App and Gemini API.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface StudentRoadmapContext {
  career: string;
  overallProgressPercentage: number;
  completedTasks: string[];
  pendingTasks: string[];
  currentStage?: string;
  nextTasks?: string[];
  skillCategories?: string[];
}

function generateSafeDeterministicRoadmapGuidance(context: {
  career: string;
  overallProgressPercentage?: number;
  completedTasks?: string[];
  pendingTasks?: string[];
  currentStage?: string;
  nextTasks?: string[];
}): string {
  const career = context.career || "Technical Career";
  const progress = context.overallProgressPercentage ?? 0;
  const stage = context.currentStage?.trim() || "";
  const completed = Array.isArray(context.completedTasks) ? context.completedTasks : [];
  const pending = Array.isArray(context.pendingTasks) ? context.pendingTasks : [];
  const next = Array.isArray(context.nextTasks) && context.nextTasks.length > 0
    ? context.nextTasks
    : pending;

  const stageMention = stage
    ? `You are currently progressing through ${stage} at ${progress}% completion.`
    : `You have achieved ${progress}% completion on your ${career} roadmap.`;

  const completedMention = completed.length > 0
    ? `Having completed checkpoints such as ${completed.slice(0, 2).join(" and ")}, you have built solid foundational momentum.`
    : `As you begin this curriculum, establishing structured weekly study blocks will help you build steady momentum.`;

  const nextTask = next[0] || pending[0];
  const nextMention = nextTask
    ? `Your immediate priority should be on mastering '${nextTask}'.`
    : `You have completed all primary roadmap checkpoints for this curriculum.`;

  return `${stageMention} ${completedMention} ${nextMention} Consistent daily practice on these active checkpoints will keep you on track for campus placements.`;
}

Deno.serve(async (req: Request) => {
  // 1. Handle CORS Preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({ error: "Method not allowed. Only POST is supported." }),
      { status: 405, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }

  try {
    // 2. Validate Authorization & Authenticate User
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: "Unauthorized: Missing Authorization header" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";

    if (!supabaseUrl || !supabaseAnonKey) {
      console.error("Supabase environment configuration missing.");
      return new Response(
        JSON.stringify({ error: "Server configuration error" }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabase = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    });

    const { data: { user }, error: authError } = await supabase.auth.getUser();
    if (authError || !user) {
      return new Response(
        JSON.stringify({ error: "Unauthorized: Invalid or expired session" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 3. Parse and Validate Student Roadmap Context
    const body: StudentRoadmapContext = await req.json();
    if (!body || !body.career) {
      return new Response(
        JSON.stringify({ error: "Bad Request: Missing required 'career' in roadmap context" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 4. Safely query authenticated student's profile by user.id
    let studentDept = "";
    let studentYear = "";
    try {
      const { data: profile } = await supabase
        .from("profiles")
        .select("department, year_of_study")
        .eq("id", user.id)
        .maybeSingle();

      if (profile) {
        studentDept = profile.department?.trim() || "";
        studentYear = profile.year_of_study?.trim() || "";
      }
    } catch (dbErr) {
      console.warn("Could not query profile table directly, continuing with authenticated session:", dbErr);
    }

    const {
      career,
      overallProgressPercentage = 0,
      completedTasks = [],
      pendingTasks = [],
      currentStage = "",
      nextTasks = [],
      skillCategories = [],
    } = body;

    const contextForFallback = {
      career,
      overallProgressPercentage,
      completedTasks,
      pendingTasks,
      currentStage,
      nextTasks,
    };

    // 5. Retrieve Gemini API Key & Model from Server-Side Environment Secrets
    const geminiApiKey = Deno.env.get("GEMINI_API_KEY");
    if (!geminiApiKey) {
      console.warn("GEMINI_API_KEY secret is not configured; using deterministic roadmap guidance.");
      const fallbackGuidance = generateSafeDeterministicRoadmapGuidance(contextForFallback);
      return new Response(
        JSON.stringify({ guidance: fallbackGuidance }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const geminiModel = Deno.env.get("GEMINI_MODEL") || "gemini-3.5-flash";

    // 6. Controlled System Instruction (strictly bound to supplied roadmap data)
    const systemInstruction = `You are the personalized roadmap guidance assistant inside CGA.

Your job is to analyze the student's CURRENT roadmap progress and provide short, practical guidance.

Use ONLY the supplied student roadmap data.

The existing roadmap is the source of truth.

Do not create new roadmap tasks.

Do not change the student's recommended career.

Do not invent completed tasks.

Do not invent skills.

Do not invent achievements.

Do not invent experience.

Do not invent progress.

Do not recommend a task that does not exist in the supplied roadmap.

Prioritize the student's actual pending tasks.

Be encouraging, realistic, concise, and student-friendly.

Mention the student's current progress when useful.

Recommend the most appropriate next step using the supplied pending/current tasks.

Keep the response approximately 3–5 sentences.`;

    const completedSample = completedTasks.length > 0
      ? completedTasks.slice(0, 6).join(", ")
      : "None yet (just starting)";

    const nextTasksText = nextTasks.length > 0
      ? nextTasks.slice(0, 4).join(", ")
      : (pendingTasks.length > 0 ? pendingTasks.slice(0, 3).join(", ") : "All core tasks completed");

    const pendingSample = pendingTasks.length > 0
      ? pendingTasks.slice(0, 6).join(", ")
      : "None (Roadmap complete)";

    const skillsText = skillCategories.length > 0
      ? skillCategories.join(", ")
      : "Core Technical Domains";

    const deptInfo = studentDept ? ` (${studentDept}${studentYear ? `, ${studentYear}` : ""})` : "";

    const userPrompt = `Student Roadmap Context:
- Career Path: ${career}
- Student Background${deptInfo}
- Overall Roadmap Progress: ${overallProgressPercentage}%
- Current Stage: ${currentStage || "Foundational Skills"}
- Recently Completed Tasks: ${completedSample}
- Immediate Next Recommended Checkpoints: ${nextTasksText}
- Remaining Pending Tasks: ${pendingSample}
- Relevant Skill Domains: ${skillsText}

Based strictly on this student's actual progress, explain how they are progressing and what specific pending checkpoint they should prioritize next in 3 to 5 sentences.`;

    // 7. Call Gemini API via Server-Side REST Endpoint with strict timeout
    const geminiEndpoint = `https://generativelanguage.googleapis.com/v1beta/models/${geminiModel}:generateContent?key=${geminiApiKey}`;

    const requestBody = JSON.stringify({
      system_instruction: {
        parts: [{ text: systemInstruction }],
      },
      contents: [
        {
          role: "user",
          parts: [{ text: userPrompt }],
        },
      ],
      generationConfig: {
        temperature: 0.6,
        maxOutputTokens: 300,
      },
    });

    let geminiResponse: Response | null = null;
    try {
      geminiResponse = await fetch(geminiEndpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: requestBody,
        signal: AbortSignal.timeout(10000),
      });

      // Handle transient rate limits or high-demand spikes (429, 503) with a single retry
      if (geminiResponse && !geminiResponse.ok && (geminiResponse.status === 503 || geminiResponse.status === 429)) {
        console.warn(`Gemini returned status ${geminiResponse.status}. Retrying in 750ms...`);
        await new Promise((resolve) => setTimeout(resolve, 750));
        geminiResponse = await fetch(geminiEndpoint, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: requestBody,
          signal: AbortSignal.timeout(8000),
        });
      }
    } catch (fetchErr) {
      console.warn("Gemini fetch timed out or failed; falling back to deterministic guidance:", fetchErr);
      const fallbackGuidance = generateSafeDeterministicRoadmapGuidance(contextForFallback);
      return new Response(
        JSON.stringify({ guidance: fallbackGuidance }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (!geminiResponse || !geminiResponse.ok) {
      const errorText = geminiResponse ? await geminiResponse.text() : "No response";
      console.warn(`Gemini API call failed with status ${geminiResponse?.status}: ${errorText}; using fallback.`);
      const fallbackGuidance = generateSafeDeterministicRoadmapGuidance(contextForFallback);
      return new Response(
        JSON.stringify({ guidance: fallbackGuidance }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const geminiData = await geminiResponse.json();
    const candidate = geminiData.candidates?.[0];
    const generatedText = candidate?.content?.parts?.[0]?.text?.trim();

    if (!generatedText) {
      console.warn("Gemini returned empty candidate content; using fallback.");
      const fallbackGuidance = generateSafeDeterministicRoadmapGuidance(contextForFallback);
      return new Response(
        JSON.stringify({ guidance: fallbackGuidance }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 8. Return Clean Personalized Guidance
    return new Response(
      JSON.stringify({ guidance: generatedText }),
      {
        status: 200,
        headers: {
          ...corsHeaders,
          "Content-Type": "application/json",
        },
      }
    );
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    console.warn("Unhandled error in generate-roadmap-guidance Edge Function, using safe fallback:", message);
    const safeFallback = generateSafeDeterministicRoadmapGuidance({
      career: "Engineering & Technology",
    });
    return new Response(
      JSON.stringify({ guidance: safeFallback }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
