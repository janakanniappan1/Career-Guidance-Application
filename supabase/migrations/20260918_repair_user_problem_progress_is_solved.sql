-- ==============================================================================
-- MIGRATION: REPAIR USER PROBLEM PROGRESS IS_SOLVED & VERIFY UNIQUE CONSTRAINT
-- Date: 2026-09-18
-- Description: Ensures existing completed progress rows with solved_at NOT NULL
-- have is_solved = true, and verifies the unique_user_problem constraint on
-- public.user_problem_progress(user_id, problem_id).
-- ==============================================================================

-- 1. Verify and create unique constraint on (user_id, problem_id) if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'unique_user_problem'
    ) THEN
        ALTER TABLE public.user_problem_progress
            ADD CONSTRAINT unique_user_problem UNIQUE (user_id, problem_id);
    END IF;
END $$;

-- 2. Repair existing completed progress rows where solved_at is set but is_solved is false
UPDATE public.user_problem_progress
SET is_solved = true
WHERE solved_at IS NOT NULL
  AND is_solved = false;
