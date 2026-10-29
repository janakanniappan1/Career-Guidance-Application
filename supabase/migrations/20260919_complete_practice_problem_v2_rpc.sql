-- ==============================================================================
-- MIGRATION: COMPLETE PRACTICE PROBLEM V2 RPC
-- Date: 2026-09-19
-- Description: Authoritative RPC for Practice Problem completion.
-- Accepts p_user_id, p_problem_id, p_xp_reward (defaults to 20).
-- Enforces single-solve idempotency, atomic XP incrementation, and progress recording.
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
    v_xp_reward integer;
    v_already_solved boolean;
    v_current_xp integer;
    v_new_xp integer;
    v_canonical_id text;
    v_category text;
BEGIN
    -- 1. Security Check: Authenticated caller can only submit for their own user_id
    v_caller_id := auth.uid();
    IF v_caller_id IS NOT NULL AND v_caller_id <> p_user_id THEN
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

    -- 3. Lock profile row for atomic update (with auto-create fallback)
    SELECT COALESCE(xp, 0)
    INTO v_current_xp
    FROM public.profiles
    WHERE id = p_user_id
    FOR UPDATE;

    IF NOT FOUND THEN
        INSERT INTO public.profiles (id, email, full_name, xp, created_at, updated_at)
        VALUES (p_user_id, '', 'User', 0, now(), now())
        ON CONFLICT (id) DO NOTHING;

        SELECT COALESCE(xp, 0)
        INTO v_current_xp
        FROM public.profiles
        WHERE id = p_user_id
        FOR UPDATE;
        
        v_current_xp := COALESCE(v_current_xp, 0);
    END IF;

    -- 4. Check whether already solved by user in public.user_problem_progress
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
    ELSE
        -- Ensure problem metadata row exists in practice_problems to satisfy FK constraint if present
        v_category := CASE
            WHEN v_canonical_id LIKE 'dsa_%' OR v_canonical_id LIKE 'aiml_%' THEN 'Coding'
            WHEN v_canonical_id LIKE 'apt_%' THEN 'Aptitude'
            WHEN v_canonical_id LIKE 'iv_%' THEN 'Interview'
            ELSE 'Technical'
        END;

        INSERT INTO public.practice_problems (id, title, category, difficulty, description, xp_reward)
        VALUES (v_canonical_id, v_canonical_id, v_category, 'Medium', 'Practice problem', v_xp_reward)
        ON CONFLICT (id) DO NOTHING;

        -- First time solving: Upsert progress row using canonical ID
        INSERT INTO public.user_problem_progress (
            id, user_id, problem_id, is_solved, solved_at
        ) VALUES (
            gen_random_uuid(), p_user_id, v_canonical_id, true, now()
        )
        ON CONFLICT (user_id, problem_id)
        DO UPDATE SET is_solved = true, solved_at = now();

        -- Increment profile XP atomically using exact 2-argument signature
        PERFORM public.increment_profile_xp(p_user_id, v_xp_reward);

        SELECT COALESCE(xp, 0)
        INTO v_new_xp
        FROM public.profiles
        WHERE id = p_user_id;

        -- Record activity for streak tracking safely
        BEGIN
            INSERT INTO public.user_daily_activity (
                id, user_id, activity_date, daily_test_completed, xp_earned, created_at, updated_at
            ) VALUES (
                gen_random_uuid(), p_user_id, CURRENT_DATE, false, v_xp_reward, now(), now()
            )
            ON CONFLICT (user_id, activity_date)
            DO UPDATE SET
                xp_earned = public.user_daily_activity.xp_earned + v_xp_reward,
                updated_at = now();
        EXCEPTION WHEN OTHERS THEN
            NULL;
        END;

        RETURN jsonb_build_object(
            'success', true,
            'newly_solved', true,
            'xp_awarded', v_xp_reward,
            'problem_id', v_canonical_id,
            'total_xp', v_new_xp,
            'message', 'First solve success'
        );
    END IF;
END;
$$;

GRANT EXECUTE ON FUNCTION public.complete_practice_problem_v2(uuid, text, integer) TO authenticated, anon;
