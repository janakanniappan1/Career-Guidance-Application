-- ============================================================
-- CGA: REAL DAILY ACTIVITY + STREAK SYSTEM
-- Safe migration
--
-- Does NOT:
--   - delete existing activity
--   - delete progress
--   - modify XP
--   - modify roadmap data
--   - modify quiz history
--
-- Streak source of truth:
--   user_daily_activity
--   profiles.current_streak
--
-- Date standard:
--   Asia/Kolkata
-- ============================================================


-- ------------------------------------------------------------
-- 1. Make 0 the correct default for a new profile
-- ------------------------------------------------------------

ALTER TABLE public.profiles
ALTER COLUMN current_streak SET DEFAULT 0;


-- ------------------------------------------------------------
-- 2. Create atomic activity + streak RPC
-- ------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.record_user_activity(
    p_user_id UUID
)
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_caller_id UUID;
    v_target_date DATE;
    v_current_streak INTEGER;
    v_new_streak INTEGER;
    v_already_active_today BOOLEAN;
    v_last_active_date DATE;
BEGIN

    -- --------------------------------------------------------
    -- Security: only authenticated user can record their own
    -- activity.
    -- --------------------------------------------------------

    v_caller_id := auth.uid();

    IF v_caller_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    IF v_caller_id <> p_user_id THEN
        RAISE EXCEPTION 'Unauthorized activity request';
    END IF;


    -- --------------------------------------------------------
    -- Use India local calendar date.
    -- Do NOT trust a date supplied by the Android client.
    -- --------------------------------------------------------

    v_target_date :=
        (now() AT TIME ZONE 'Asia/Kolkata')::DATE;


    -- --------------------------------------------------------
    -- Lock the profile row.
    --
    -- This prevents two simultaneous activity requests from
    -- incrementing the streak twice.
    -- --------------------------------------------------------

    SELECT COALESCE(current_streak, 0)
    INTO v_current_streak
    FROM public.profiles
    WHERE id = p_user_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Profile not found';
    END IF;


    -- --------------------------------------------------------
    -- Check whether activity has already been recorded today.
    -- --------------------------------------------------------

    SELECT EXISTS (
        SELECT 1
        FROM public.user_daily_activity
        WHERE user_id = p_user_id
          AND activity_date = v_target_date
    )
    INTO v_already_active_today;


    -- --------------------------------------------------------
    -- SAME DAY:
    --
    -- Activity already exists, so do not increase streak.
    -- --------------------------------------------------------

    IF v_already_active_today THEN

        -- If existing activity says today is active but the
        -- profile somehow has 0, safely recover to 1.
        IF v_current_streak <= 0 THEN

            UPDATE public.profiles
            SET current_streak = 1,
                updated_at = now()
            WHERE id = p_user_id;

            RETURN 1;

        END IF;

        RETURN v_current_streak;

    END IF;


    -- --------------------------------------------------------
    -- FIRST ACTIVITY OF TODAY:
    --
    -- Record the active calendar date.
    --
    -- We intentionally do not overwrite:
    -- daily_test_completed
    -- xp_earned
    --
    -- if a row somehow appears between the check and insert.
    -- --------------------------------------------------------

    INSERT INTO public.user_daily_activity (
        user_id,
        activity_date,
        daily_test_completed,
        xp_earned,
        created_at,
        updated_at
    )
    VALUES (
        p_user_id,
        v_target_date,
        false,
        0,
        now(),
        now()
    )
    ON CONFLICT (user_id, activity_date)
    DO NOTHING;


    -- --------------------------------------------------------
    -- Find the most recent activity BEFORE today.
    -- --------------------------------------------------------

    SELECT MAX(activity_date)
    INTO v_last_active_date
    FROM public.user_daily_activity
    WHERE user_id = p_user_id
      AND activity_date < v_target_date;


    -- --------------------------------------------------------
    -- Calculate streak.
    -- --------------------------------------------------------

    IF v_last_active_date IS NULL THEN

        -- First activity ever.
        v_new_streak := 1;

    ELSIF v_last_active_date =
          (v_target_date - INTERVAL '1 day')::DATE THEN

        -- User was active yesterday.
        --
        -- If the stored streak is valid, increase it.
        -- If it is 0/corrupted, recover to 2.
        IF v_current_streak > 0 THEN
            v_new_streak := v_current_streak + 1;
        ELSE
            v_new_streak := 2;
        END IF;

    ELSE

        -- One or more days were missed.
        v_new_streak := 1;

    END IF;


    -- --------------------------------------------------------
    -- Save authoritative streak.
    -- --------------------------------------------------------

    UPDATE public.profiles
    SET current_streak = v_new_streak,
        updated_at = now()
    WHERE id = p_user_id;


    -- --------------------------------------------------------
    -- Return new streak to Android.
    -- --------------------------------------------------------

    RETURN v_new_streak;

END;
$$;


-- ------------------------------------------------------------
-- 3. Allow authenticated users to call the RPC
-- ------------------------------------------------------------

GRANT EXECUTE
ON FUNCTION public.record_user_activity(UUID)
TO authenticated;


-- ------------------------------------------------------------
-- 4. Optional safety:
-- Remove execute permission from anonymous users.
-- ------------------------------------------------------------

REVOKE EXECUTE
ON FUNCTION public.record_user_activity(UUID)
FROM anon;
