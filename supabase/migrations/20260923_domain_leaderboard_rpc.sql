-- ==============================================================================
-- CGA DOMAIN-WISE LEADERBOARD SECURE RPC MIGRATION
-- Migration: 20260923_domain_leaderboard_rpc.sql
-- Description:
--   Creates the public.get_domain_leaderboard RPC function.
--   Provides domain-wise ranking by XP, Roadmap Task Completion %, and Streak.
--   SECURITY DEFINER: Safely accesses profiles and progress without exposing
--   private data (email, auth tokens, etc.).
-- ==============================================================================

CREATE OR REPLACE FUNCTION public.get_domain_leaderboard(
    p_domain_id text DEFAULT 'aiml_engineer'
)
RETURNS TABLE (
    user_id uuid,
    display_name text,
    domain_id text,
    domain_title text,
    department text,
    year_of_study text,
    xp integer,
    completion_percentage integer,
    streak integer,
    completed_tasks integer,
    total_tasks integer
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_canonical_domain text;
    v_domain_title text;
    v_total_tasks integer := 0;
BEGIN
    -- 1. Canonicalize domain ID matching CareerCanonicalUtil
    v_canonical_domain := CASE
        WHEN p_domain_id ILIKE '%aiml%' OR p_domain_id ILIKE '%machine_learning%' OR p_domain_id ILIKE '%ai%' THEN 'aiml_engineer'
        WHEN p_domain_id ILIKE '%software%' OR p_domain_id ILIKE '%dev%' THEN 'software_dev'
        WHEN p_domain_id ILIKE '%analyst%' OR p_domain_id ILIKE '%analytics%' THEN 'data_analyst'
        WHEN p_domain_id ILIKE '%data_scien%' OR p_domain_id ILIKE '%scientist%' THEN 'data_scientist'
        WHEN p_domain_id ILIKE '%ui%' OR p_domain_id ILIKE '%ux%' OR p_domain_id ILIKE '%design%' THEN 'uiux_designer'
        WHEN p_domain_id ILIKE '%cyber%' OR p_domain_id ILIKE '%security%' THEN 'cyber_analyst'
        WHEN p_domain_id ILIKE '%cloud%' OR p_domain_id ILIKE '%devops%' THEN 'cloud_engineer'
        ELSE p_domain_id
    END;

    -- Lookup title from careers table, fallback to human-readable title
    SELECT title INTO v_domain_title FROM public.careers WHERE id = v_canonical_domain LIMIT 1;
    IF v_domain_title IS NULL THEN
        v_domain_title := CASE v_canonical_domain
            WHEN 'aiml_engineer' THEN 'AI / Machine Learning Engineer'
            WHEN 'software_dev' THEN 'Software Developer'
            WHEN 'data_analyst' THEN 'Data Analyst'
            WHEN 'data_scientist' THEN 'Data Scientist'
            WHEN 'uiux_designer' THEN 'UI/UX Designer'
            WHEN 'cyber_analyst' THEN 'Cybersecurity Analyst'
            WHEN 'cloud_engineer' THEN 'Cloud & DevOps Engineer'
            ELSE v_canonical_domain
        END;
    END IF;

    -- 2. Calculate total tasks count for this domain's roadmap
    SELECT COUNT(DISTINCT rt.id) INTO v_total_tasks
    FROM public.roadmap_tasks rt
    JOIN public.roadmap_stages rs ON rt.stage_id = rs.id
    JOIN public.roadmaps r ON rs.roadmap_id = r.id
    WHERE r.career_id = v_canonical_domain;

    -- 3. Return ranked leaderboard rows for users in this domain
    RETURN QUERY
    WITH domain_users AS (
        SELECT
            p.id AS u_id,
            COALESCE(NULLIF(p.name, ''), 'Student') AS u_name,
            v_canonical_domain AS u_domain,
            v_domain_title AS u_domain_title,
            COALESCE(p.department, '') AS u_dept,
            COALESCE(p.year_of_study, '') AS u_year,
            COALESCE(p.xp, 0) AS u_xp,
            COALESCE(p.current_streak, 0) AS u_streak
        FROM public.profiles p
        WHERE p.recommended_career_id = v_canonical_domain
           OR (p.recommended_career_id IS NULL AND (
                p.career_interest ILIKE '%' || v_canonical_domain || '%'
                OR (v_canonical_domain = 'aiml_engineer' AND p.career_interest ILIKE '%ai%')
                OR (v_canonical_domain = 'software_dev' AND p.career_interest ILIKE '%software%')
                OR (v_canonical_domain = 'data_analyst' AND p.career_interest ILIKE '%data%anal%')
                OR (v_canonical_domain = 'data_scientist' AND p.career_interest ILIKE '%data%scien%')
                OR (v_canonical_domain = 'uiux_designer' AND p.career_interest ILIKE '%ui%')
                OR (v_canonical_domain = 'cyber_analyst' AND p.career_interest ILIKE '%cyber%')
                OR (v_canonical_domain = 'cloud_engineer' AND p.career_interest ILIKE '%cloud%')
           ))
    ),
    user_task_counts AS (
        SELECT
            utp.user_id AS u_id,
            COUNT(DISTINCT utp.task_id) AS completed_count
        FROM public.user_task_progress utp
        JOIN public.roadmap_tasks rt ON utp.task_id = rt.id
        JOIN public.roadmap_stages rs ON rt.stage_id = rs.id
        JOIN public.roadmaps r ON rs.roadmap_id = r.id
        WHERE r.career_id = v_canonical_domain
          AND utp.is_completed = true
        GROUP BY utp.user_id
    )
    SELECT
        du.u_id,
        du.u_name,
        du.u_domain,
        du.u_domain_title,
        du.u_dept,
        du.u_year,
        du.u_xp,
        CASE
            WHEN v_total_tasks > 0 THEN
                LEAST(100, ROUND((COALESCE(utc.completed_count, 0)::numeric / v_total_tasks::numeric) * 100)::integer)
            ELSE 0
        END AS completion_percentage,
        du.u_streak,
        COALESCE(utc.completed_count, 0)::integer AS completed_tasks,
        v_total_tasks AS total_tasks
    FROM domain_users du
    LEFT JOIN user_task_counts utc ON du.u_id = utc.u_id;
END;
$$;

-- Grant execution permission to authenticated, anon, and service_role users
GRANT EXECUTE ON FUNCTION public.get_domain_leaderboard(text) TO authenticated, anon, service_role;
