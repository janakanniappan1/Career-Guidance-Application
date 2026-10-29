-- ==============================================================================
-- MIGRATION: FIX COMPLETE_PRACTICE_PROBLEM_V2 AUTHORITATIVE IMPLEMENTATION
-- Date: 2026-09-21
-- Description: Fixes complete_practice_problem_v2 to invoke:
-- 1. public.increment_profile_xp(p_user_id, v_xp_reward) [2-argument signature]
-- 2. public.record_user_activity(p_user_id) [1-argument signature]
-- 3. Strict validation: profile must exist in profiles table (raises 'Profile not found')
-- 4. Canonical problem must exist in practice_problems (raises 'Practice problem ... not found')
-- 5. Explicit user_problem_progress insert/upsert with created_at = now(), updated_at = now()
-- ==============================================================================

CREATE OR REPLACE FUNCTION public.complete_practice_problem_v2(
    p_user_id uuid,
    p_problem_id text,
    p_xp_reward integer DEFAULT 20
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_caller_id uuid;
    v_canonical_id text;
    v_xp_reward integer;
    v_already_solved boolean;
    v_current_xp integer;
    v_new_xp integer;
BEGIN
    -- 1. Security Check: Authenticated caller can only submit for their own user_id
    v_caller_id := auth.uid();
    IF v_caller_id IS NOT NULL AND v_caller_id <> p_user_id THEN
        RAISE EXCEPTION 'Unauthorized: User can only complete problems for their own profile';
    END IF;

    -- 2. Canonical problem validation & alias mapping
    v_canonical_id := CASE p_problem_id
        WHEN 'p1' THEN 'dsa_01'
        WHEN 'p2' THEN 'dsa_02'
        WHEN 'p3' THEN 'dsa_03'
        WHEN 'p4' THEN 'dsa_04'
        WHEN 'p5' THEN 'dsa_05'
        WHEN 'p6' THEN 'aiml_01'
        WHEN 'p7' THEN 'aiml_02'
        WHEN 'p8' THEN 'aiml_03'
        WHEN 'p9' THEN 'aiml_04'
        WHEN 'p10' THEN 'aiml_05'
        ELSE p_problem_id
    END;

    IF NOT EXISTS (
        SELECT 1 FROM public.practice_problems WHERE id = v_canonical_id
    ) THEN
        RAISE EXCEPTION 'Practice problem % not found', v_canonical_id;
    END IF;

    -- Determine XP reward (standard 20 XP for practice problems)
    v_xp_reward := COALESCE(p_xp_reward, 20);

    -- 3. Profile lock & validation
    SELECT COALESCE(xp, 0)
    INTO v_current_xp
    FROM public.profiles
    WHERE id = p_user_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Profile not found';
    END IF;

    -- 4. Check existing solved state
    SELECT EXISTS (
        SELECT 1
        FROM public.user_problem_progress
        WHERE user_id = p_user_id
          AND (problem_id = p_problem_id OR problem_id = v_canonical_id)
          AND is_solved = true
    ) INTO v_already_solved;

    IF v_already_solved THEN
        -- Already completed: return 0 XP
        RETURN jsonb_build_object(
            'success', true,
            'newly_solved', false,
            'xp_awarded', 0,
            'problem_id', v_canonical_id,
            'total_xp', v_current_xp,
            'message', 'Already completed'
        );
    END IF;

    -- 5. Permanent user_problem_progress insert/upsert
    INSERT INTO public.user_problem_progress (
        id,
        user_id,
        problem_id,
        is_solved,
        solved_at,
        created_at,
        updated_at
    ) VALUES (
        gen_random_uuid(),
        p_user_id,
        v_canonical_id,
        true,
        now(),
        now(),
        now()
    )
    ON CONFLICT (user_id, problem_id)
    DO UPDATE SET
        is_solved = true,
        solved_at = now(),
        updated_at = now();

    -- 6. Increment profile XP atomically using 2-argument signature
    PERFORM public.increment_profile_xp(
        p_user_id,
        v_xp_reward
    );

    SELECT COALESCE(xp, 0)
    INTO v_new_xp
    FROM public.profiles
    WHERE id = p_user_id;

    -- 7. Record user activity (1-argument signature, owns streak/date logic)
    BEGIN
        PERFORM public.record_user_activity(p_user_id);
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;

    -- 8. Return JSON success
    RETURN jsonb_build_object(
        'success', true,
        'newly_solved', true,
        'xp_awarded', v_xp_reward,
        'problem_id', v_canonical_id,
        'total_xp', v_new_xp,
        'message', 'First solve success'
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.complete_practice_problem_v2(uuid, text, integer) TO authenticated, anon;
