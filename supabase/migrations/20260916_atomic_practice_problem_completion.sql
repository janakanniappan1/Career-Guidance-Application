-- ==============================================================================
-- MIGRATION: ATOMIC PRACTICE PROBLEM COMPLETION RPC
-- Date: 2026-09-16
-- Description: Adds PostgreSQL atomic function public.complete_practice_problem
-- to guarantee idempotent problem completion, first-solve-only XP rewarding,
-- user isolation, and atomic profile XP synchronization.
-- ==============================================================================

CREATE OR REPLACE FUNCTION public.complete_practice_problem(
    p_user_id uuid,
    p_problem_id text,
    p_selected_answer_index integer DEFAULT NULL
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_caller_id uuid;
    v_xp_reward integer;
    v_correct_index integer;
    v_already_solved boolean;
    v_current_xp integer;
    v_new_xp integer;
    v_is_correct boolean;
BEGIN
    -- 1. Security Check: Authenticated caller can only submit for their own user_id
    v_caller_id := auth.uid();
    IF v_caller_id IS NULL OR v_caller_id <> p_user_id THEN
        RAISE EXCEPTION 'Unauthorized: User can only complete problems for their own profile';
    END IF;

    -- 2. Lookup the practice problem
    SELECT xp_reward, correct_index
    INTO v_xp_reward, v_correct_index
    FROM public.practice_problems
    WHERE id = p_problem_id;

    -- If problem record not found, raise exception (never assume 20 XP)
    IF NOT FOUND OR v_xp_reward IS NULL THEN
        RAISE EXCEPTION 'Practice problem not found: %', p_problem_id;
    END IF;

    -- 3. Check correctness if selected_answer_index is supplied
    IF p_selected_answer_index IS NOT NULL THEN
        v_is_correct := (p_selected_answer_index = v_correct_index);
    ELSE
        v_is_correct := true;
    END IF;

    -- 4. Lock profile row for atomic update
    SELECT COALESCE(xp, 0)
    INTO v_current_xp
    FROM public.profiles
    WHERE id = p_user_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Profile not found for user %', p_user_id;
    END IF;

    IF NOT v_is_correct THEN
        RETURN jsonb_build_object(
            'is_correct', false,
            'xp_awarded', 0,
            'is_first_solve', false,
            'is_solved', false,
            'total_xp', v_current_xp,
            'message', 'Incorrect answer'
        );
    END IF;

    -- 5. Check whether already solved by user
    SELECT EXISTS (
        SELECT 1
        FROM public.user_problem_progress
        WHERE user_id = p_user_id
          AND problem_id = p_problem_id
          AND is_solved = true
    ) INTO v_already_solved;

    IF v_already_solved THEN
        -- Problem was already solved previously: 0 XP awarded, leave progress unchanged
        RETURN jsonb_build_object(
            'is_correct', true,
            'xp_awarded', 0,
            'is_first_solve', false,
            'is_solved', true,
            'total_xp', v_current_xp,
            'message', 'Already completed'
        );
    ELSE
        -- First time solving: Upsert progress row
        INSERT INTO public.user_problem_progress (
            id, user_id, problem_id, is_solved, solved_at
        ) VALUES (
            gen_random_uuid(), p_user_id, p_problem_id, true, now()
        )
        ON CONFLICT (user_id, problem_id)
        DO UPDATE SET is_solved = true, solved_at = now();

        -- Increment profile XP atomically by problem's exact xp_reward
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

-- Grant execution permissions
GRANT EXECUTE ON FUNCTION public.complete_practice_problem(uuid, text, integer) TO authenticated;
