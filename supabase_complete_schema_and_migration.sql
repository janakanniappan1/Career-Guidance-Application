-- ==============================================================================
-- CGA (Career Guidance Application) - COMPLETE SUPABASE DATABASE SCHEMA & RLS POLICIES
-- ==============================================================================
-- This script provides the complete single source of truth schema for CGA.
-- It includes all tables, constraints, foreign keys, and Row Level Security (RLS) policies.
-- ==============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

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
    xp INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own profile"
    ON public.profiles FOR SELECT
    USING (auth.uid() = id);

CREATE POLICY "Users can insert own profile"
    ON public.profiles FOR INSERT
    WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- ------------------------------------------------------------------------------
-- 2. USER DAILY ACTIVITY TABLE (Streak & Daily Speed Test Completion)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.user_daily_activity (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    activity_date DATE NOT NULL DEFAULT CURRENT_DATE,
    daily_test_completed BOOLEAN NOT NULL DEFAULT false,
    xp_earned INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT unique_user_activity_date UNIQUE (user_id, activity_date)
);

ALTER TABLE public.user_daily_activity ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own daily activity"
    ON public.user_daily_activity FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert/update own daily activity"
    ON public.user_daily_activity FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ------------------------------------------------------------------------------
-- 3. CAREERS TABLE (Canonical Career Catalog)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.careers (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    demand_level TEXT NOT NULL DEFAULT 'HIGH',
    avg_salary_lpa NUMERIC NOT NULL DEFAULT 12.0,
    job_roles TEXT[] NOT NULL DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.careers ENABLE ROW LEVEL SECURITY;

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
    description TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.roadmaps ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public read access for roadmaps" ON public.roadmaps FOR SELECT USING (true);

CREATE TABLE IF NOT EXISTS public.roadmap_stages (
    id TEXT PRIMARY KEY,
    roadmap_id TEXT NOT NULL REFERENCES public.roadmaps(id) ON DELETE CASCADE,
    stage_name TEXT NOT NULL,
    stage_order INTEGER NOT NULL DEFAULT 1,
    subtitle TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.roadmap_stages ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public read access for roadmap_stages" ON public.roadmap_stages FOR SELECT USING (true);

CREATE TABLE IF NOT EXISTS public.roadmap_tasks (
    id TEXT PRIMARY KEY,
    stage_id TEXT NOT NULL REFERENCES public.roadmap_stages(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    skill_category TEXT NOT NULL DEFAULT 'Core Skills',
    estimated_hours INTEGER NOT NULL DEFAULT 10,
    resource_type TEXT NOT NULL DEFAULT 'Course',
    task_order INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.roadmap_tasks ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public read access for roadmap_tasks" ON public.roadmap_tasks FOR SELECT USING (true);

CREATE TABLE IF NOT EXISTS public.user_task_progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    task_id TEXT NOT NULL REFERENCES public.roadmap_tasks(id) ON DELETE CASCADE,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT unique_user_task UNIQUE (user_id, task_id)
);

ALTER TABLE public.user_task_progress ENABLE ROW LEVEL SECURITY;

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
    difficulty TEXT NOT NULL DEFAULT 'MEDIUM',
    category TEXT NOT NULL DEFAULT 'Algorithms',
    career_id TEXT,
    acceptance_rate INTEGER NOT NULL DEFAULT 70,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.practice_problems ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public read access for practice_problems" ON public.practice_problems FOR SELECT USING (true);

CREATE TABLE IF NOT EXISTS public.user_problem_progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    problem_id TEXT NOT NULL REFERENCES public.practice_problems(id) ON DELETE CASCADE,
    is_solved BOOLEAN NOT NULL DEFAULT false,
    solved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT unique_user_problem UNIQUE (user_id, problem_id)
);

ALTER TABLE public.user_problem_progress ENABLE ROW LEVEL SECURITY;

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
    correct_option_index INTEGER NOT NULL DEFAULT 0,
    explanation TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.quiz_questions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public read access for quiz_questions" ON public.quiz_questions FOR SELECT USING (true);

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

ALTER TABLE public.quiz_attempts ENABLE ROW LEVEL SECURITY;

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
    question TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'General',
    difficulty TEXT NOT NULL DEFAULT 'MEDIUM',
    sample_answer TEXT NOT NULL DEFAULT '',
    key_points TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.interview_questions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public read access for interview_questions" ON public.interview_questions FOR SELECT USING (true);

-- ------------------------------------------------------------------------------
-- 8. STUDENT PROJECTS TABLE
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.student_projects (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    tech_stack TEXT[] NOT NULL DEFAULT '{}',
    github_url TEXT,
    live_demo_url TEXT,
    stars INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.student_projects ENABLE ROW LEVEL SECURITY;

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
    question_text TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.assessment_questions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public read access for assessment_questions" ON public.assessment_questions FOR SELECT USING (true);

CREATE TABLE IF NOT EXISTS public.assessment_options (
    id TEXT PRIMARY KEY,
    question_id INTEGER NOT NULL REFERENCES public.assessment_questions(id) ON DELETE CASCADE,
    option_text TEXT NOT NULL,
    order_index INTEGER NOT NULL DEFAULT 0,
    score_mapping JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.assessment_options ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public read access for assessment_options" ON public.assessment_options FOR SELECT USING (true);

CREATE TABLE IF NOT EXISTS public.assessment_answers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    question_id INTEGER NOT NULL REFERENCES public.assessment_questions(id) ON DELETE CASCADE,
    selected_option_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT unique_user_assessment_answer UNIQUE (user_id, question_id)
);

ALTER TABLE public.assessment_answers ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own assessment answers"
    ON public.assessment_answers FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE TABLE IF NOT EXISTS public.assessment_attempts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    top_career_id TEXT NOT NULL,
    match_percentage INTEGER NOT NULL DEFAULT 0,
    category_scores JSONB NOT NULL DEFAULT '{}'::jsonb,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.assessment_attempts ENABLE ROW LEVEL SECURITY;

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

ALTER TABLE public.resume_analyses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own resume analyses"
    ON public.resume_analyses FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
