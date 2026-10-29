-- ==============================================================================
-- MIGRATION: CLEAN PRACTICE PROBLEM COMPLETION RPC
-- Date: 2026-09-19
-- Description: Updates public.complete_practice_problem to match the actual
-- schema of public.practice_problems (removing non-existent correct_index column
-- references) and safely enforcing atomic XP incrementation and single-solve tracking.
-- ==============================================================================

CREATE OR REPLACE FUNCTION public.complete_practice_problem(
    p_user_id uuid,
    p_problem_id text,
    p_selected_answer_index integer DEFAULT NULL,
    p_xp_reward integer DEFAULT 20
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_caller_id uuid;
    v_xp_reward integer;
    v_already_solved boolean;
    v_current_xp integer;
    v_new_xp integer;
    v_canonical_id text;
BEGIN
    -- 1. Security Check: Authenticated caller can only submit for their own user_id
    v_caller_id := auth.uid();
    IF v_caller_id IS NULL OR v_caller_id <> p_user_id THEN
        RAISE EXCEPTION 'Unauthorized: User can only complete problems for their own profile';
    END IF;

    -- Canonicalize legacy aliases if needed
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

    -- 2. Determine XP reward (standard 20 XP for catalog problems)
    v_xp_reward := COALESCE(p_xp_reward, 20);

    -- 3. Lock profile row for atomic update
    SELECT COALESCE(xp, 0)
    INTO v_current_xp
    FROM public.profiles
    WHERE id = p_user_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Profile not found for user %', p_user_id;
    END IF;

    -- 4. Check whether already solved by user in public.user_problem_progress (checking both id and alias)
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
            'is_correct', true,
            'xp_awarded', 0,
            'is_first_solve', false,
            'is_solved', true,
            'total_xp', v_current_xp,
            'message', 'Already completed'
        );
    ELSE
        -- First time solving: Upsert progress row using canonical ID
        INSERT INTO public.user_problem_progress (
            id, user_id, problem_id, is_solved, solved_at
        ) VALUES (
            gen_random_uuid(), p_user_id, v_canonical_id, true, now()
        )
        ON CONFLICT (user_id, problem_id)
        DO UPDATE SET is_solved = true, solved_at = now();

        -- Increment profile XP atomically
        UPDATE public.profiles
        SET xp = COALESCE(xp, 0) + v_xp_reward,
            updated_at = now()
        WHERE id = p_user_id
        RETURNING xp INTO v_new_xp;

        RETURN jsonb_build_object(
            'is_correct', true,
            'xp_awarded', v_xp_reward,
            'is_first_solve', true,
            'is_solved', true,
            'total_xp', v_new_xp,
            'message', 'First solve success'
        );
    END IF;
END;
$$;

GRANT EXECUTE ON FUNCTION public.complete_practice_problem(uuid, text, integer, integer) TO authenticated;
