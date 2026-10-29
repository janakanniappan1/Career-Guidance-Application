-- ==============================================================================
-- MIGRATION: ATOMIC PROFILE XP INCREMENT RPC
-- Date: 2026-09-11
-- Description: Adds PostgreSQL atomic function public.increment_profile_xp
-- to eliminate client-side read-modify-write lost updates across devices.
-- ==============================================================================

CREATE OR REPLACE FUNCTION public.increment_profile_xp(
    p_user_id uuid,
    p_amount integer
)
RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_new_xp integer;
BEGIN
    -- 1. Security Check: Authenticated caller can only increment their own profile XP.
    IF auth.uid() IS NULL OR auth.uid() <> p_user_id THEN
        RAISE EXCEPTION 'Unauthorized: User can only update their own profile XP';
    END IF;

    -- 2. Input Validation: Strictly positive amount.
    IF p_amount IS NULL OR p_amount <= 0 THEN
        RAISE EXCEPTION 'Invalid XP amount: Amount must be strictly greater than zero';
    END IF;

    -- 3. Atomic Database Increment.
    UPDATE public.profiles
    SET
        xp = COALESCE(xp, 0) + p_amount,
        updated_at = now()
    WHERE id = p_user_id
    RETURNING xp INTO v_new_xp;

    -- 4. Profile existence check.
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Profile not found for user %', p_user_id;
    END IF;

    RETURN v_new_xp;
END;
$$;

-- Grant execution permission to authenticated users
GRANT EXECUTE ON FUNCTION public.increment_profile_xp(uuid, integer) TO authenticated;
