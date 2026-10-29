-- ==============================================================================
-- CGA (Career Guidance Application) - AI MENTOR PERSISTENT CHAT TABLES & RLS
-- Migration: 20261001_cga_chat_tables.sql
-- Description:
--   Creates persistent conversation and message storage for the CGA AI Mentor.
--   Enforces strict Row Level Security (RLS) and cross-user conversation isolation.
-- ==============================================================================

-- 0. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------------------------
-- 1. CHAT CONVERSATIONS TABLE
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.chat_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL DEFAULT 'New Conversation',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- Index for fast user conversation listing ordered by recent activity
CREATE INDEX IF NOT EXISTS idx_chat_conversations_user_updated
    ON public.chat_conversations (user_id, updated_at DESC);

-- Enable RLS
ALTER TABLE public.chat_conversations ENABLE ROW LEVEL SECURITY;

-- Policies for chat_conversations
DROP POLICY IF EXISTS "Users can view own chat conversations" ON public.chat_conversations;
CREATE POLICY "Users can view own chat conversations"
    ON public.chat_conversations FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert own chat conversations" ON public.chat_conversations;
CREATE POLICY "Users can insert own chat conversations"
    ON public.chat_conversations FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own chat conversations" ON public.chat_conversations;
CREATE POLICY "Users can update own chat conversations"
    ON public.chat_conversations FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own chat conversations" ON public.chat_conversations;
CREATE POLICY "Users can delete own chat conversations"
    ON public.chat_conversations FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);


-- ------------------------------------------------------------------------------
-- 2. CHAT MESSAGES TABLE
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES public.chat_conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('user', 'model')),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- Indexes for fast chronological message querying and cascade deletion support
CREATE INDEX IF NOT EXISTS idx_chat_messages_conv_created
    ON public.chat_messages (conversation_id, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_chat_messages_user_id
    ON public.chat_messages (user_id);

-- Enable RLS
ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;

-- Policies for chat_messages with strict cross-user conversation isolation
DROP POLICY IF EXISTS "Users can view messages of own conversations" ON public.chat_messages;
CREATE POLICY "Users can view messages of own conversations"
    ON public.chat_messages FOR SELECT
    TO authenticated
    USING (
        auth.uid() = user_id
        AND EXISTS (
            SELECT 1 FROM public.chat_conversations c
            WHERE c.id = chat_messages.conversation_id
              AND c.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Users can insert messages into own conversations" ON public.chat_messages;
CREATE POLICY "Users can insert messages into own conversations"
    ON public.chat_messages FOR INSERT
    TO authenticated
    WITH CHECK (
        auth.uid() = user_id
        AND EXISTS (
            SELECT 1 FROM public.chat_conversations c
            WHERE c.id = chat_messages.conversation_id
              AND c.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Users can delete messages of own conversations" ON public.chat_messages;
CREATE POLICY "Users can delete messages of own conversations"
    ON public.chat_messages FOR DELETE
    TO authenticated
    USING (
        auth.uid() = user_id
        AND EXISTS (
            SELECT 1 FROM public.chat_conversations c
            WHERE c.id = chat_messages.conversation_id
              AND c.user_id = auth.uid()
        )
    );


-- ------------------------------------------------------------------------------
-- 3. AUTOMATIC UPDATED_AT TRIGGERS
-- ------------------------------------------------------------------------------

-- Function: update chat_conversations.updated_at on conversation direct update
CREATE OR REPLACE FUNCTION public.handle_chat_conversation_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = timezone('utc'::text, now());
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_chat_conversations_touch ON public.chat_conversations;
CREATE TRIGGER trg_chat_conversations_touch
    BEFORE UPDATE ON public.chat_conversations
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_chat_conversation_updated_at();

-- Function: update parent chat_conversations.updated_at when a new message is inserted
CREATE OR REPLACE FUNCTION public.handle_chat_message_touch_conversation()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    UPDATE public.chat_conversations
    SET updated_at = timezone('utc'::text, now())
    WHERE id = NEW.conversation_id;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_chat_messages_touch_conversation ON public.chat_messages;
CREATE TRIGGER trg_chat_messages_touch_conversation
    AFTER INSERT ON public.chat_messages
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_chat_message_touch_conversation();
