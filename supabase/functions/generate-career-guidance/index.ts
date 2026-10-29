// Supabase Edge Function: generate-career-guidance
// CGA (Career Guidance Application) - S5 Mini Project
// Lightweight secure server-side bridge between Android App and Gemini API.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface StudentCareerContext {
  name?: string;
  department?: string;
  year?: string;
  careerInterest?: string;
  career: string;
  matchPercentage: number;
  careerSkills?: string[];
}

function generateSafeDeterministicGuidance(context: {
  career: string;
  matchPercentage?: number;
  department?: string;
  year?: string;
  careerSkills?: string[];
}): string {
  const careerName = (context.career || "Software & Technology").trim();
  const matchStr = context.matchPercentage && context.matchPercentage > 0
    ? `${context.matchPercentage}%`
    : "a strong";
  const dept = (context.department || "").trim();
  const year = (context.year || "").trim();
  const rawYear = year ? (year.toLowerCase().includes("year") ? year : `${year}-year`) : "";
  const skills = Array.isArray(context.careerSkills)
    ? context.careerSkills.filter((s) => typeof s === "string" && s.trim().length > 0 && s.toLowerCase() !== "and")
    : [];

  const skillText = skills.length >= 2
    ? `${skills[0]} and ${skills[1]}`
    : (skills.length === 1 ? skills[0] : "");

  if (dept && rawYear && skillText) {
    return `Your ${matchStr} compatibility match with ${careerName} is a solid starting point, particularly with your focus on ${skillText}. As a ${rawYear} ${dept} student, strengthening core domain foundations and building practical portfolio projects will help you advance confidently toward this career.`;
  } else if (dept && skillText) {
    return `Your ${matchStr} compatibility match with ${careerName} aligns well with your studies in ${dept} and your interest in ${skillText}. Focus on hands-on practical exercises and building small projects that showcase your applied abilities.`;
  } else if (skillText) {
    return `Your ${matchStr} compatibility match with ${careerName} gives you a clear direction to build toward, especially when backed by skills like ${skillText}. Applying these concepts to real-world problems will help you move steadily toward this role.`;
  } else if (dept && rawYear) {
    return `Your ${matchStr} compatibility match with ${careerName} gives you an encouraging roadmap to follow. As a ${rawYear} ${dept} student, taking on milestone challenges and consistent daily learning will help you gain valuable momentum.`;
  } else {
    return `Your ${matchStr} compatibility match with ${careerName} provides a clear target for your career development. Consistent practice, completing recommended roadmap milestones, and working on practical projects will help you progress toward this path.`;
  }
}

function sanitizeAndValidateGuidance(
  rawText: string,
  context: {
    career: string;
    matchPercentage?: number;
    department?: string;
    year?: string;
    careerSkills?: string[];
  }
): string {
  if (!rawText || typeof rawText !== "string") {
    return generateSafeDeterministicGuidance(context);
  }

  let cleaned = rawText.trim();

  // Strip markdown code fences if wrapped
  if (cleaned.toLowerCase().startsWith("```json")) {
    cleaned = cleaned.substring(7).trim();
  } else if (cleaned.startsWith("```")) {
    cleaned = cleaned.substring(3).trim();
  }
  if (cleaned.endsWith("```")) {
    cleaned = cleaned.substring(0, cleaned.length - 3).trim();
  }

  // Attempt to parse structured JSON if raw text is a JSON payload
  try {
    if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
      const parsed = JSON.parse(cleaned);
      if (parsed && typeof parsed.guidance === "string" && parsed.guidance.trim()) {
        cleaned = parsed.guidance.trim();
      } else if (parsed && typeof parsed.explanation === "string" && parsed.explanation.trim()) {
        cleaned = parsed.explanation.trim();
      }
    }
  } catch (_) {
    // Continue with string sanitization
  }

  // Strip surrounding quotes
  if (cleaned.startsWith('"') && cleaned.endsWith('"') && cleaned.length >= 2) {
    cleaned = cleaned.substring(1, cleaned.length - 1).trim();
  }

  // Instruction and meta-text leakage patterns to strictly reject
  const forbiddenPatterns = [
    /\bSentence\s*\d+\b/i,
    /\bEncouragement\b/i,
    /\bConnection\b/i,
    /\bNext\s*step\s*:/i,
    /\bInstructions?\s*:/i,
    /\bPrompt\s*:/i,
    /\bAnalysis\s*:/i,
    /\bReasoning\s*:/i,
    /\bOutput\s*:/i,
    /\bJSON\s*:/i,
    /^\s*(?:Here\s+is|Here’s|Heres)\b/i,
    /\(\s*(?:Encouragement|Connection|Next step)[^)]*\)/i,
  ];

  for (const pattern of forbiddenPatterns) {
    if (pattern.test(cleaned)) {
      console.warn("Guidance rejected due to meta-instruction leakage matching pattern:", pattern.toString());
      return generateSafeDeterministicGuidance(context);
    }
  }

  // Reject markdown headings or bulleted lists
  if (/^#+\s+/m.test(cleaned) || /^\s*[-*•]\s+/m.test(cleaned)) {
    console.warn("Guidance rejected due to markdown formatting/bullets.");
    return generateSafeDeterministicGuidance(context);
  }

  // Clean orphan conjunctions and normalize whitespace
  cleaned = cleaned.replace(/^(?:and\s+|&\s+|,\s+)+/i, "").trim();
  cleaned = cleaned.replace(/\s+/g, " ").trim();

  if (
    cleaned.length >= 35 &&
    !cleaned.toLowerCase().startsWith("and ") &&
    !cleaned.toLowerCase().startsWith("& ")
  ) {
    return cleaned;
  }

  return generateSafeDeterministicGuidance(context);
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

    // 3. Parse and Validate Student Context
    const body: StudentCareerContext = await req.json();
    if (!body || !body.career) {
      return new Response(
        JSON.stringify({ error: "Bad Request: Missing required 'career' context" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 4. Safely query authenticated student's profile from database
    let verifiedDept = body.department?.trim() || "";
    let verifiedYear = body.year?.trim() || "";
    let verifiedInterest = body.careerInterest?.trim() || "";

    try {
      const { data: profile } = await supabase
        .from("profiles")
        .select("name, department, year_of_study, career_interest")
        .eq("id", user.id)
        .maybeSingle();

      if (profile) {
        if (profile.department && profile.department.trim()) {
          verifiedDept = profile.department.trim();
        }
        if (profile.year_of_study && profile.year_of_study.trim()) {
          verifiedYear = profile.year_of_study.trim();
        }
        if (profile.career_interest && profile.career_interest.trim()) {
          verifiedInterest = profile.career_interest.trim();
        }
      }
    } catch (dbErr) {
      console.warn("Could not query profile table directly, relying on validated session payload:", dbErr);
    }

    const { career, matchPercentage, careerSkills } = body;
    const cleanSkills = Array.isArray(careerSkills)
      ? careerSkills
          .map((s) => (typeof s === "string" ? s.trim() : ""))
          .filter((s) => s.length > 0 && s.toLowerCase() !== "and")
          .slice(0, 5)
      : [];

    const contextForFallback = {
      career,
      matchPercentage,
      department: verifiedDept,
      year: verifiedYear,
      careerSkills: cleanSkills,
    };

    // 5. Retrieve Gemini API Key & Model from Server-Side Environment Secrets
    const geminiApiKey = Deno.env.get("GEMINI_API_KEY");
    if (!geminiApiKey) {
      console.warn("GEMINI_API_KEY secret is not configured; using deterministic fallback.");
      const fallbackGuidance = generateSafeDeterministicGuidance(contextForFallback);
      return new Response(
        JSON.stringify({ guidance: fallbackGuidance }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const geminiModel = Deno.env.get("GEMINI_MODEL") || "gemini-3.5-flash";

    // 6. Build Strict Structured System Instruction & Delimited User Context
    const systemInstruction = `You are the personalized career guidance assistant inside a student career application.

Generate ONE short, natural paragraph directly addressed to the student.

Use the supplied student context to make the guidance genuinely personalized.

Rules:
- Write 2 to 4 natural sentences.
- Mention the recommended career naturally.
- Use the match percentage when available.
- Connect the guidance to the student's department/year when useful.
- Mention relevant skills when available.
- Give one realistic next step.
- Be encouraging but not exaggerated.
- Be concise.
- Do not repeat the entire student profile.
- Do not invent achievements, skills, experience, or certifications.
- Do not make promises about future success.
- Do not use headings.
- Do not use numbered sentences.
- Do not write 'Sentence 1', 'Sentence 2', etc.
- Do not describe your instructions.
- Do not mention this prompt.
- Do not output analysis or reasoning.
- Do not output markdown.
- Do not output code.
- Return JSON matching exactly:

{
  "guidance": "..."
}

The value of 'guidance' must contain only the final student-facing paragraph.`;

    const skillsText = cleanSkills.length > 0
      ? cleanSkills.join(", ")
      : "technical problem solving and core domain skills";

    const matchText = matchPercentage && matchPercentage > 0 ? `${matchPercentage}%` : "Strong alignment";

    // Strictly delimit user data as data only
    const userPrompt = `--- BEGIN STUDENT DATA ---
Recommended Career: ${career}
Match Percentage: ${matchText}
Department: ${verifiedDept || "Engineering & Technology"}
Year of Study: ${verifiedYear || "Undergraduate"}
Career Interest: ${verifiedInterest || "Not specified"}
Key Skills: ${skillsText}
--- END STUDENT DATA ---

Return only the required JSON containing the natural personalized paragraph in 'guidance'.`;

    // 7. Call Gemini API with Structured Output Request
    const geminiEndpoint = `https://generativelanguage.googleapis.com/v1beta/models/${geminiModel}:generateContent?key=${geminiApiKey}`;
    const geminiResponse = await fetch(geminiEndpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
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
          temperature: 0.5,
          maxOutputTokens: 250,
          responseMimeType: "application/json",
        },
      }),
    });

    if (!geminiResponse.ok) {
      const errorText = await geminiResponse.text();
      console.warn(`Gemini API call returned status ${geminiResponse.status}: ${errorText}; falling back.`);
      const fallbackGuidance = generateSafeDeterministicGuidance(contextForFallback);
      return new Response(
        JSON.stringify({ guidance: fallbackGuidance }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const geminiData = await geminiResponse.json();
    const candidate = geminiData.candidates?.[0];
    const generatedRawText = candidate?.content?.parts?.[0]?.text?.trim() || "";

    // 8. Sanitize, Validate, and Reject any Prompt/Template Leakage
    const finalGuidance = sanitizeAndValidateGuidance(generatedRawText, contextForFallback);

    // 9. Return strictly structured JSON
    return new Response(
      JSON.stringify({ guidance: finalGuidance }),
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
    console.error("Unhandled error in generate-career-guidance Edge Function:", message);
    const safeFallback = generateSafeDeterministicGuidance({
      career: "Software & Technology",
    });
    return new Response(
      JSON.stringify({ guidance: safeFallback }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
