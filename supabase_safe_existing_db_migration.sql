-- ==============================================================================
-- CGA (Career Guidance Application) - IDEMPOTENT DATABASE MIGRATION FOR EXISTING DB
-- ==============================================================================
-- This migration is designed to safely run against an EXISTING or NEW Supabase instance.
-- DATA SAFETY GUARANTEES:
--  - NO DROP TABLE statements.
--  - NO TRUNCATE or DELETE statements.
--  - ALL table creations use CREATE TABLE IF NOT EXISTS.
--  - ALL column additions use ALTER TABLE ... ADD COLUMN IF NOT EXISTS.
--  - ALL indexes use CREATE INDEX IF NOT EXISTS.
--  - ALL RLS policies are idempotently dropped and recreated.
--  - Existing user data, progress, and profiles are completely preserved.
-- ==============================================================================

-- 0. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------------------------
-- 1. PROFILES TABLE (User Profile, Streak, XP, & Career State)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL DEFAULT 'Student',
    email TEXT NOT NULL DEFAULT '',
    department TEXT NOT NULL DEFAULT 'Computer Science',
    year_of_study TEXT NOT NULL DEFAULT '3rd Year',
    career_interest TEXT NOT NULL DEFAULT '',
    current_streak INTEGER NOT NULL DEFAULT 1,
    is_assessment_completed BOOLEAN NOT NULL DEFAULT false,
    recommended_career_id TEXT,
    daily_motivational_message TEXT DEFAULT 'Focus on progress, not perfection. Every skill learned is a step towards your dream career!',
    xp INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- Ensure all required columns exist in pre-existing profiles table
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS name TEXT NOT NULL DEFAULT 'Student';
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS email TEXT NOT NULL DEFAULT '';
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS department TEXT NOT NULL DEFAULT 'Computer Science';
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS year_of_study TEXT NOT NULL DEFAULT '3rd Year';
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS career_interest TEXT NOT NULL DEFAULT '';
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS current_streak INTEGER NOT NULL DEFAULT 1;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS is_assessment_completed BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS recommended_career_id TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS daily_motivational_message TEXT DEFAULT 'Focus on progress, not perfection. Every skill learned is a step towards your dream career!';
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS xp INTEGER NOT NULL DEFAULT 0;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now());
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now());

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own profile" ON public.profiles;
CREATE POLICY "Users can view own profile"
    ON public.profiles FOR SELECT
    USING (auth.uid() = id);

DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
CREATE POLICY "Users can insert own profile"
    ON public.profiles FOR INSERT
    WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- ------------------------------------------------------------------------------
-- 2. USER DAILY ACTIVITY TABLE (Daily Speed Test & Daily XP Tracking)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.user_daily_activity (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    activity_date DATE NOT NULL DEFAULT CURRENT_DATE,
    daily_test_completed BOOLEAN NOT NULL DEFAULT false,
    xp_earned INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.user_daily_activity ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE public.user_daily_activity ADD COLUMN IF NOT EXISTS activity_date DATE NOT NULL DEFAULT CURRENT_DATE;
ALTER TABLE public.user_daily_activity ADD COLUMN IF NOT EXISTS daily_test_completed BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE public.user_daily_activity ADD COLUMN IF NOT EXISTS xp_earned INTEGER NOT NULL DEFAULT 0;
ALTER TABLE public.user_daily_activity ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now());
ALTER TABLE public.user_daily_activity ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now());

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'unique_user_activity_date'
    ) THEN
        ALTER TABLE public.user_daily_activity
            ADD CONSTRAINT unique_user_activity_date UNIQUE (user_id, activity_date);
    END IF;
END $$;

ALTER TABLE public.user_daily_activity ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own daily activity" ON public.user_daily_activity;
CREATE POLICY "Users can view own daily activity"
    ON public.user_daily_activity FOR SELECT
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert/update own daily activity" ON public.user_daily_activity;
CREATE POLICY "Users can insert/update own daily activity"
    ON public.user_daily_activity FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ------------------------------------------------------------------------------
-- 3. CAREERS CATALOG TABLE
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.careers (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    role_overview TEXT DEFAULT '',
    average_salary TEXT DEFAULT '',
    job_growth TEXT DEFAULT '',
    description TEXT DEFAULT '',
    reason_for_recommendation TEXT DEFAULT '',
    required_skills TEXT[] DEFAULT '{}',
    demand_level TEXT DEFAULT 'HIGH',
    avg_salary_lpa NUMERIC DEFAULT 12.0,
    job_roles TEXT[] DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.careers ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE public.careers ADD COLUMN IF NOT EXISTS role_overview TEXT DEFAULT '';
ALTER TABLE public.careers ADD COLUMN IF NOT EXISTS average_salary TEXT DEFAULT '';
ALTER TABLE public.careers ADD COLUMN IF NOT EXISTS job_growth TEXT DEFAULT '';
ALTER TABLE public.careers ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';
ALTER TABLE public.careers ADD COLUMN IF NOT EXISTS reason_for_recommendation TEXT DEFAULT '';
ALTER TABLE public.careers ADD COLUMN IF NOT EXISTS required_skills TEXT[] DEFAULT '{}';
ALTER TABLE public.careers ADD COLUMN IF NOT EXISTS demand_level TEXT DEFAULT 'HIGH';
ALTER TABLE public.careers ADD COLUMN IF NOT EXISTS avg_salary_lpa NUMERIC DEFAULT 12.0;
ALTER TABLE public.careers ADD COLUMN IF NOT EXISTS job_roles TEXT[] DEFAULT '{}';
ALTER TABLE public.careers ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;
ALTER TABLE public.careers ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

ALTER TABLE public.careers ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read access for careers" ON public.careers;
CREATE POLICY "Public read access for careers"
    ON public.careers FOR SELECT
    USING (true);

-- ------------------------------------------------------------------------------
-- 4. ROADMAPS & ROADMAP TASKS
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.roadmaps (
    id TEXT PRIMARY KEY,
    career_id TEXT NOT NULL REFERENCES public.careers(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.roadmaps ADD COLUMN IF NOT EXISTS career_id TEXT;
ALTER TABLE public.roadmaps ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE public.roadmaps ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';
ALTER TABLE public.roadmaps ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());
ALTER TABLE public.roadmaps ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

ALTER TABLE public.roadmaps ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read access for roadmaps" ON public.roadmaps;
CREATE POLICY "Public read access for roadmaps"
    ON public.roadmaps FOR SELECT
    USING (true);

CREATE TABLE IF NOT EXISTS public.roadmap_stages (
    id TEXT PRIMARY KEY,
    roadmap_id TEXT NOT NULL REFERENCES public.roadmaps(id) ON DELETE CASCADE,
    stage_name TEXT NOT NULL,
    stage_order INTEGER NOT NULL DEFAULT 1,
    subtitle TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.roadmap_stages ADD COLUMN IF NOT EXISTS roadmap_id TEXT;
ALTER TABLE public.roadmap_stages ADD COLUMN IF NOT EXISTS stage_name TEXT;
ALTER TABLE public.roadmap_stages ADD COLUMN IF NOT EXISTS stage_order INTEGER DEFAULT 1;
ALTER TABLE public.roadmap_stages ADD COLUMN IF NOT EXISTS subtitle TEXT DEFAULT '';
ALTER TABLE public.roadmap_stages ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

ALTER TABLE public.roadmap_stages ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read access for roadmap_stages" ON public.roadmap_stages;
CREATE POLICY "Public read access for roadmap_stages"
    ON public.roadmap_stages FOR SELECT
    USING (true);

CREATE TABLE IF NOT EXISTS public.roadmap_tasks (
    id TEXT PRIMARY KEY,
    stage_id TEXT NOT NULL REFERENCES public.roadmap_stages(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    skill_category TEXT DEFAULT 'Core Skills',
    estimated_hours INTEGER DEFAULT 10,
    resource_type TEXT DEFAULT 'Course',
    task_order INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.roadmap_tasks ADD COLUMN IF NOT EXISTS stage_id TEXT;
ALTER TABLE public.roadmap_tasks ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE public.roadmap_tasks ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';
ALTER TABLE public.roadmap_tasks ADD COLUMN IF NOT EXISTS skill_category TEXT DEFAULT 'Core Skills';
ALTER TABLE public.roadmap_tasks ADD COLUMN IF NOT EXISTS estimated_hours INTEGER DEFAULT 10;
ALTER TABLE public.roadmap_tasks ADD COLUMN IF NOT EXISTS resource_type TEXT DEFAULT 'Course';
ALTER TABLE public.roadmap_tasks ADD COLUMN IF NOT EXISTS task_order INTEGER DEFAULT 1;
ALTER TABLE public.roadmap_tasks ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

ALTER TABLE public.roadmap_tasks ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read access for roadmap_tasks" ON public.roadmap_tasks;
CREATE POLICY "Public read access for roadmap_tasks"
    ON public.roadmap_tasks FOR SELECT
    USING (true);

CREATE TABLE IF NOT EXISTS public.user_task_progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    task_id TEXT NOT NULL REFERENCES public.roadmap_tasks(id) ON DELETE CASCADE,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.user_task_progress ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE public.user_task_progress ADD COLUMN IF NOT EXISTS task_id TEXT;
ALTER TABLE public.user_task_progress ADD COLUMN IF NOT EXISTS is_completed BOOLEAN DEFAULT false;
ALTER TABLE public.user_task_progress ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;
ALTER TABLE public.user_task_progress ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());
ALTER TABLE public.user_task_progress ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'unique_user_task'
    ) THEN
        ALTER TABLE public.user_task_progress
            ADD CONSTRAINT unique_user_task UNIQUE (user_id, task_id);
    END IF;
END $$;

ALTER TABLE public.user_task_progress ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage own task progress" ON public.user_task_progress;
CREATE POLICY "Users can manage own task progress"
    ON public.user_task_progress FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ------------------------------------------------------------------------------
-- 5. PRACTICE PROBLEMS & USER PROGRESS
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.practice_problems (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'Coding',
    difficulty TEXT NOT NULL DEFAULT 'Medium',
    description TEXT DEFAULT '',
    career_id TEXT,
    topic TEXT,
    xp_reward INTEGER DEFAULT 20,
    acceptance_rate INTEGER DEFAULT 70,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS category TEXT DEFAULT 'Coding';
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS difficulty TEXT DEFAULT 'Medium';
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS career_id TEXT;
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS topic TEXT;
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS xp_reward INTEGER DEFAULT 20;
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS acceptance_rate INTEGER DEFAULT 70;
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

ALTER TABLE public.practice_problems ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read access for practice_problems" ON public.practice_problems;
CREATE POLICY "Public read access for practice_problems"
    ON public.practice_problems FOR SELECT
    USING (true);

CREATE TABLE IF NOT EXISTS public.user_problem_progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    problem_id TEXT NOT NULL REFERENCES public.practice_problems(id) ON DELETE CASCADE,
    is_solved BOOLEAN NOT NULL DEFAULT false,
    solved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.user_problem_progress ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE public.user_problem_progress ADD COLUMN IF NOT EXISTS problem_id TEXT;
ALTER TABLE public.user_problem_progress ADD COLUMN IF NOT EXISTS is_solved BOOLEAN DEFAULT false;
ALTER TABLE public.user_problem_progress ADD COLUMN IF NOT EXISTS solved_at TIMESTAMPTZ;
ALTER TABLE public.user_problem_progress ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'unique_user_problem'
    ) THEN
        ALTER TABLE public.user_problem_progress
            ADD CONSTRAINT unique_user_problem UNIQUE (user_id, problem_id);
    END IF;
END $$;

ALTER TABLE public.user_problem_progress ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage own problem progress" ON public.user_problem_progress;
CREATE POLICY "Users can manage own problem progress"
    ON public.user_problem_progress FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ------------------------------------------------------------------------------
-- 6. QUIZ QUESTIONS & QUIZ ATTEMPTS
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.quiz_questions (
    id TEXT PRIMARY KEY,
    career_id TEXT,
    topic TEXT NOT NULL DEFAULT 'general',
    question TEXT NOT NULL,
    options TEXT[] NOT NULL DEFAULT '{}',
    correct_index INTEGER NOT NULL DEFAULT 0,
    explanation TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.quiz_questions ADD COLUMN IF NOT EXISTS career_id TEXT;
ALTER TABLE public.quiz_questions ADD COLUMN IF NOT EXISTS topic TEXT DEFAULT 'general';
ALTER TABLE public.quiz_questions ADD COLUMN IF NOT EXISTS question TEXT;
ALTER TABLE public.quiz_questions ADD COLUMN IF NOT EXISTS options TEXT[] DEFAULT '{}';
ALTER TABLE public.quiz_questions ADD COLUMN IF NOT EXISTS correct_index INTEGER DEFAULT 0;
ALTER TABLE public.quiz_questions ADD COLUMN IF NOT EXISTS explanation TEXT DEFAULT '';
ALTER TABLE public.quiz_questions ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

ALTER TABLE public.quiz_questions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read access for quiz_questions" ON public.quiz_questions;
CREATE POLICY "Public read access for quiz_questions"
    ON public.quiz_questions FOR SELECT
    USING (true);

CREATE TABLE IF NOT EXISTS public.quiz_attempts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    career_id TEXT,
    topic TEXT NOT NULL DEFAULT 'general',
    score INTEGER NOT NULL DEFAULT 0,
    total_questions INTEGER NOT NULL DEFAULT 5,
    xp_earned INTEGER NOT NULL DEFAULT 0,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.quiz_attempts ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE public.quiz_attempts ADD COLUMN IF NOT EXISTS career_id TEXT;
ALTER TABLE public.quiz_attempts ADD COLUMN IF NOT EXISTS topic TEXT DEFAULT 'general';
ALTER TABLE public.quiz_attempts ADD COLUMN IF NOT EXISTS score INTEGER DEFAULT 0;
ALTER TABLE public.quiz_attempts ADD COLUMN IF NOT EXISTS total_questions INTEGER DEFAULT 5;
ALTER TABLE public.quiz_attempts ADD COLUMN IF NOT EXISTS xp_earned INTEGER DEFAULT 0;
ALTER TABLE public.quiz_attempts ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

ALTER TABLE public.quiz_attempts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage own quiz attempts" ON public.quiz_attempts;
CREATE POLICY "Users can manage own quiz attempts"
    ON public.quiz_attempts FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ------------------------------------------------------------------------------
-- 7. INTERVIEW QUESTIONS
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.interview_questions (
    id TEXT PRIMARY KEY,
    career_id TEXT,
    category TEXT DEFAULT 'Technical',
    question TEXT NOT NULL,
    answer_key TEXT DEFAULT '',
    tip TEXT DEFAULT '',
    topic TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.interview_questions ADD COLUMN IF NOT EXISTS career_id TEXT;
ALTER TABLE public.interview_questions ADD COLUMN IF NOT EXISTS category TEXT DEFAULT 'Technical';
ALTER TABLE public.interview_questions ADD COLUMN IF NOT EXISTS question TEXT;
ALTER TABLE public.interview_questions ADD COLUMN IF NOT EXISTS answer_key TEXT DEFAULT '';
ALTER TABLE public.interview_questions ADD COLUMN IF NOT EXISTS tip TEXT DEFAULT '';
ALTER TABLE public.interview_questions ADD COLUMN IF NOT EXISTS topic TEXT;
ALTER TABLE public.interview_questions ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

ALTER TABLE public.interview_questions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read access for interview_questions" ON public.interview_questions;
CREATE POLICY "Public read access for interview_questions"
    ON public.interview_questions FOR SELECT
    USING (true);

-- ------------------------------------------------------------------------------
-- 8. STUDENT PROJECTS TABLE
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.student_projects (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    tech_stack TEXT[] DEFAULT '{}',
    github_url TEXT DEFAULT '',
    live_demo_url TEXT,
    stars INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.student_projects ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE public.student_projects ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE public.student_projects ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';
ALTER TABLE public.student_projects ADD COLUMN IF NOT EXISTS tech_stack TEXT[] DEFAULT '{}';
ALTER TABLE public.student_projects ADD COLUMN IF NOT EXISTS github_url TEXT DEFAULT '';
ALTER TABLE public.student_projects ADD COLUMN IF NOT EXISTS live_demo_url TEXT;
ALTER TABLE public.student_projects ADD COLUMN IF NOT EXISTS stars INTEGER DEFAULT 0;
ALTER TABLE public.student_projects ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

ALTER TABLE public.student_projects ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage own student projects" ON public.student_projects;
CREATE POLICY "Users can manage own student projects"
    ON public.student_projects FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ------------------------------------------------------------------------------
-- 9. ASSESSMENT QUESTIONS, OPTIONS, ANSWERS & ATTEMPTS
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.assessment_questions (
    id INTEGER PRIMARY KEY,
    question_number INTEGER NOT NULL,
    category TEXT NOT NULL,
    question_text TEXT,
    text TEXT,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.assessment_questions ADD COLUMN IF NOT EXISTS question_number INTEGER;
ALTER TABLE public.assessment_questions ADD COLUMN IF NOT EXISTS category TEXT;
ALTER TABLE public.assessment_questions ADD COLUMN IF NOT EXISTS question_text TEXT;
ALTER TABLE public.assessment_questions ADD COLUMN IF NOT EXISTS text TEXT;
ALTER TABLE public.assessment_questions ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE public.assessment_questions ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;
ALTER TABLE public.assessment_questions ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

ALTER TABLE public.assessment_questions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read access for assessment_questions" ON public.assessment_questions;
CREATE POLICY "Public read access for assessment_questions"
    ON public.assessment_questions FOR SELECT
    USING (true);

CREATE TABLE IF NOT EXISTS public.assessment_options (
    id TEXT PRIMARY KEY,
    question_id INTEGER NOT NULL REFERENCES public.assessment_questions(id) ON DELETE CASCADE,
    option_text TEXT,
    text TEXT,
    scores JSONB NOT NULL DEFAULT '{}'::jsonb,
    score_mapping JSONB NOT NULL DEFAULT '{}'::jsonb,
    order_index INTEGER DEFAULT 0,
    option_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.assessment_options ADD COLUMN IF NOT EXISTS question_id INTEGER;
ALTER TABLE public.assessment_options ADD COLUMN IF NOT EXISTS option_text TEXT;
ALTER TABLE public.assessment_options ADD COLUMN IF NOT EXISTS text TEXT;
ALTER TABLE public.assessment_options ADD COLUMN IF NOT EXISTS scores JSONB DEFAULT '{}'::jsonb;
ALTER TABLE public.assessment_options ADD COLUMN IF NOT EXISTS score_mapping JSONB DEFAULT '{}'::jsonb;
ALTER TABLE public.assessment_options ADD COLUMN IF NOT EXISTS order_index INTEGER DEFAULT 0;
ALTER TABLE public.assessment_options ADD COLUMN IF NOT EXISTS option_order INTEGER DEFAULT 0;
ALTER TABLE public.assessment_options ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

ALTER TABLE public.assessment_options ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read access for assessment_options" ON public.assessment_options;
CREATE POLICY "Public read access for assessment_options"
    ON public.assessment_options FOR SELECT
    USING (true);

CREATE TABLE IF NOT EXISTS public.assessment_answers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    question_id INTEGER NOT NULL REFERENCES public.assessment_questions(id) ON DELETE CASCADE,
    selected_option_id TEXT,
    option_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.assessment_answers ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE public.assessment_answers ADD COLUMN IF NOT EXISTS question_id INTEGER;
ALTER TABLE public.assessment_answers ADD COLUMN IF NOT EXISTS selected_option_id TEXT;
ALTER TABLE public.assessment_answers ADD COLUMN IF NOT EXISTS option_id TEXT;
ALTER TABLE public.assessment_answers ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'unique_user_assessment_answer'
    ) THEN
        ALTER TABLE public.assessment_answers
            ADD CONSTRAINT unique_user_assessment_answer UNIQUE (user_id, question_id);
    END IF;
END $$;

ALTER TABLE public.assessment_answers ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage own assessment answers" ON public.assessment_answers;
CREATE POLICY "Users can manage own assessment answers"
    ON public.assessment_answers FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE TABLE IF NOT EXISTS public.assessment_attempts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    answers JSONB NOT NULL DEFAULT '{}'::jsonb,
    top_career_id TEXT NOT NULL,
    match_percentage INTEGER NOT NULL DEFAULT 0,
    category_scores JSONB DEFAULT '{}'::jsonb,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.assessment_attempts ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE public.assessment_attempts ADD COLUMN IF NOT EXISTS answers JSONB DEFAULT '{}'::jsonb;
ALTER TABLE public.assessment_attempts ADD COLUMN IF NOT EXISTS top_career_id TEXT;
ALTER TABLE public.assessment_attempts ADD COLUMN IF NOT EXISTS match_percentage INTEGER DEFAULT 0;
ALTER TABLE public.assessment_attempts ADD COLUMN IF NOT EXISTS category_scores JSONB DEFAULT '{}'::jsonb;
ALTER TABLE public.assessment_attempts ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

ALTER TABLE public.assessment_attempts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view and insert own assessment attempts" ON public.assessment_attempts;
CREATE POLICY "Users can view and insert own assessment attempts"
    ON public.assessment_attempts FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ------------------------------------------------------------------------------
-- 10. RESUME ANALYSES TABLE
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.resume_analyses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    ats_score INTEGER NOT NULL DEFAULT 0,
    target_career TEXT NOT NULL DEFAULT '',
    matched_skills TEXT[] NOT NULL DEFAULT '{}',
    missing_skills TEXT[] NOT NULL DEFAULT '{}',
    tips TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.resume_analyses ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE public.resume_analyses ADD COLUMN IF NOT EXISTS ats_score INTEGER DEFAULT 0;
ALTER TABLE public.resume_analyses ADD COLUMN IF NOT EXISTS target_career TEXT DEFAULT '';
ALTER TABLE public.resume_analyses ADD COLUMN IF NOT EXISTS matched_skills TEXT[] DEFAULT '{}';
ALTER TABLE public.resume_analyses ADD COLUMN IF NOT EXISTS missing_skills TEXT[] DEFAULT '{}';
ALTER TABLE public.resume_analyses ADD COLUMN IF NOT EXISTS tips TEXT[] DEFAULT '{}';
ALTER TABLE public.resume_analyses ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());

ALTER TABLE public.resume_analyses ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage own resume analyses" ON public.resume_analyses;
CREATE POLICY "Users can manage own resume analyses"
    ON public.resume_analyses FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ------------------------------------------------------------------------------
-- 11. INDEXES FOR PERFORMANCE & FAST LOOKUPS
-- ------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_profiles_xp ON public.profiles(xp);
CREATE INDEX IF NOT EXISTS idx_user_daily_activity_lookup ON public.user_daily_activity(user_id, activity_date);
CREATE INDEX IF NOT EXISTS idx_user_task_progress_lookup ON public.user_task_progress(user_id, task_id);
CREATE INDEX IF NOT EXISTS idx_user_problem_progress_lookup ON public.user_problem_progress(user_id, problem_id);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_user_topic ON public.quiz_attempts(user_id, topic);
CREATE INDEX IF NOT EXISTS idx_student_projects_user ON public.student_projects(user_id);
CREATE INDEX IF NOT EXISTS idx_assessment_answers_user ON public.assessment_answers(user_id, question_id);
CREATE INDEX IF NOT EXISTS idx_assessment_attempts_user ON public.assessment_attempts(user_id);
CREATE INDEX IF NOT EXISTS idx_resume_analyses_user ON public.resume_analyses(user_id);
CREATE INDEX IF NOT EXISTS idx_roadmaps_career ON public.roadmaps(career_id);
CREATE INDEX IF NOT EXISTS idx_roadmap_stages_roadmap ON public.roadmap_stages(roadmap_id);
CREATE INDEX IF NOT EXISTS idx_roadmap_tasks_stage ON public.roadmap_tasks(stage_id);
CREATE INDEX IF NOT EXISTS idx_practice_problems_career ON public.practice_problems(career_id);
CREATE INDEX IF NOT EXISTS idx_quiz_questions_career_topic ON public.quiz_questions(career_id, topic);
CREATE INDEX IF NOT EXISTS idx_interview_questions_career ON public.interview_questions(career_id);
CREATE INDEX IF NOT EXISTS idx_profiles_rec_career ON public.profiles(recommended_career_id);
CREATE INDEX IF NOT EXISTS idx_profiles_career_interest ON public.profiles(career_interest);
CREATE INDEX IF NOT EXISTS idx_user_task_progress_completed ON public.user_task_progress(user_id, is_completed);

-- ------------------------------------------------------------------------------
-- 12. DYNAMIC DOMAIN LEADERBOARD RPC
-- ------------------------------------------------------------------------------
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
