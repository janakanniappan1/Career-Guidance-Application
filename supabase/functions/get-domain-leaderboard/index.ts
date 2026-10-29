// Supabase Edge Function: get-domain-leaderboard
// CGA (Career Guidance Application) - Real Domain-Wise Leaderboard Endpoint
// Secure server-side query returning real authenticated user rankings without exposing private credentials.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, GET, OPTIONS",
};

interface LeaderboardRequest {
  p_domain_id?: string;
  domain_id?: string;
  domain?: string;
}

const CANONICAL_CAREER_TITLES: Record<string, string> = {
  aiml_engineer: "AI / Machine Learning Engineer",
  software_dev: "Software Developer",
  data_analyst: "Data Analyst",
  data_scientist: "Data Scientist",
  uiux_designer: "UI/UX Designer",
  cyber_analyst: "Cybersecurity Analyst",
  cloud_engineer: "Cloud & DevOps Engineer",
};

function canonicalizeDomain(domainId?: string): string {
  if (!domainId || typeof domainId !== "string") return "aiml_engineer";
  const clean = domainId.trim().toLowerCase();
  if (clean.includes("aiml") || clean.includes("machine_learning") || clean.includes("ai")) return "aiml_engineer";
  if (clean.includes("software") || clean.includes("dev")) return "software_dev";
  if (clean.includes("analyst") || clean.includes("analytics")) return "data_analyst";
  if (clean.includes("data_scien") || clean.includes("scientist")) return "data_scientist";
  if (clean.includes("ui") || clean.includes("ux") || clean.includes("design")) return "uiux_designer";
  if (clean.includes("cyber") || clean.includes("security")) return "cyber_analyst";
  if (clean.includes("cloud") || clean.includes("devops")) return "cloud_engineer";
  return clean || "aiml_engineer";
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || Deno.env.get("SUPABASE_ANON_KEY") || "";

    if (!supabaseUrl || !serviceRoleKey) {
      console.error("Supabase environment configuration missing.");
      return new Response(
        JSON.stringify({ error: "Server configuration error" }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabase = createClient(supabaseUrl, serviceRoleKey);

    // Parse requested domain
    let rawDomain = "aiml_engineer";
    if (req.method === "POST") {
      try {
        const body: LeaderboardRequest = await req.json();
        rawDomain = body.p_domain_id || body.domain_id || body.domain || "aiml_engineer";
      } catch {
        // Fallback to URL search params
        const url = new URL(req.url);
        rawDomain = url.searchParams.get("p_domain_id") || url.searchParams.get("domain_id") || "aiml_engineer";
      }
    } else {
      const url = new URL(req.url);
      rawDomain = url.searchParams.get("p_domain_id") || url.searchParams.get("domain_id") || "aiml_engineer";
    }

    const canonicalDomain = canonicalizeDomain(rawDomain);
    const domainTitle = CANONICAL_CAREER_TITLES[canonicalDomain] || canonicalDomain;

    // 1. Calculate total tasks count for this domain's roadmap
    let totalTasks = 25;
    try {
      const { data: roadmaps } = await supabase
        .from("roadmaps")
        .select("id")
        .eq("career_id", canonicalDomain)
        .limit(1);

      if (roadmaps && roadmaps.length > 0) {
        const roadmapId = roadmaps[0].id;
        const { data: stages } = await supabase
          .from("roadmap_stages")
          .select("id")
          .eq("roadmap_id", roadmapId);

        if (stages && stages.length > 0) {
          const stageIds = stages.map((s: { id: string }) => s.id);
          const { count } = await supabase
            .from("roadmap_tasks")
            .select("id", { count: "exact", head: true })
            .in("stage_id", stageIds);

          if (typeof count === "number" && count > 0) {
            totalTasks = count;
          }
        }
      }
    } catch (roadmapErr) {
      console.warn("Non-fatal error resolving domain total tasks:", roadmapErr);
    }

    // 2. Query all real student profiles
    const { data: profiles, error: profileError } = await supabase
      .from("profiles")
      .select("id, name, department, year_of_study, career_interest, recommended_career_id, xp, current_streak");

    if (profileError) {
      console.error("Failed to query profiles:", profileError.message);
      return new Response(
        JSON.stringify({ error: "Unable to query profiles from database" }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 3. Filter profiles that belong to the requested domain
    const qualifyingProfiles = (profiles || []).filter((p: {
      id: string;
      name?: string;
      department?: string;
      year_of_study?: string;
      career_interest?: string;
      recommended_career_id?: string;
      xp?: number;
      current_streak?: number;
    }) => {
      if (!p.id) return false;
      const userDomain = canonicalizeDomain(p.recommended_career_id || p.career_interest);
      // If user is explicitly in this domain or has no domain set
      return userDomain === canonicalDomain || !p.recommended_career_id;
    });

    // 4. Query completed tasks for all qualifying users in this domain
    const userIds = qualifyingProfiles.map((p: { id: string }) => p.id);
    const userTaskCounts: Record<string, number> = {};

    if (userIds.length > 0) {
      try {
        const { data: taskProgress } = await supabase
          .from("user_task_progress")
          .select("user_id, is_completed")
          .in("user_id", userIds)
          .eq("is_completed", true);

        if (taskProgress) {
          for (const tp of taskProgress) {
            if (tp.user_id) {
              userTaskCounts[tp.user_id] = (userTaskCounts[tp.user_id] || 0) + 1;
            }
          }
        }
      } catch (taskErr) {
        console.warn("Non-fatal error querying user_task_progress:", taskErr);
      }
    }

    // 5. Build clean public response entries
    const leaderboardEntries = qualifyingProfiles.map((p: {
      id: string;
      name?: string;
      department?: string;
      year_of_study?: string;
      career_interest?: string;
      recommended_career_id?: string;
      xp?: number;
      current_streak?: number;
    }) => {
      const userXp = typeof p.xp === "number" ? p.xp : 0;
      const userStreak = typeof p.current_streak === "number" ? p.current_streak : 0;
      const completedCount = userTaskCounts[p.id] !== undefined
        ? userTaskCounts[p.id]
        : Math.min(totalTasks, Math.floor(userXp / 100));

      const completionPct = totalTasks > 0
        ? Math.min(100, Math.max(0, Math.round((completedCount / totalTasks) * 100)))
        : 0;

      return {
        user_id: p.id,
        display_name: p.name?.trim() || "Student",
        domain_id: canonicalDomain,
        domain_title: domainTitle,
        department: p.department?.trim() || "",
        year_of_study: p.year_of_study?.trim() || "",
        xp: userXp,
        completion_percentage: completionPct,
        streak: userStreak,
        completed_tasks: completedCount,
        total_tasks: totalTasks,
      };
    });

    return new Response(
      JSON.stringify(leaderboardEntries),
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
    console.error("Unhandled error in get-domain-leaderboard Edge Function:", message);
    return new Response(
      JSON.stringify({ error: "An unexpected error occurred while loading leaderboard" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
