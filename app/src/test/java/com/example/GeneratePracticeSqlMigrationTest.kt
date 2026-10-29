package com.example

import com.example.data.repository.DefaultPracticeCatalog
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeneratePracticeSqlMigrationTest {

    @Test
    fun generateSqlMigration() {
        val problems = DefaultPracticeCatalog.GLOBAL_PRACTICE_PROBLEMS
        println("Extracting ${problems.size} canonical practice problems from DefaultPracticeCatalog...")

        val sb = StringBuilder()
        sb.appendLine("-- ==============================================================================")
        sb.appendLine("-- MIGRATION: SEED 84 CANONICAL PRACTICE PROBLEMS (METADATA ONLY)")
        sb.appendLine("-- Ensures all 84 canonical problem IDs from DefaultPracticeCatalog.kt exist")
        sb.appendLine("-- in public.practice_problems to satisfy foreign key constraints from")
        sb.appendLine("-- public.user_problem_progress.")
        sb.appendLine("-- Preserves existing p1-p10 legacy rows.")
        sb.appendLine("-- Does NOT add interactive options/correct_index columns.")
        sb.appendLine("-- ==============================================================================")
        sb.appendLine()
        sb.appendLine("-- 1. Ensure public.practice_problems table and columns exist with proper defaults")
        sb.appendLine("CREATE TABLE IF NOT EXISTS public.practice_problems (")
        sb.appendLine("    id TEXT PRIMARY KEY,")
        sb.appendLine("    title TEXT NOT NULL,")
        sb.appendLine("    category TEXT NOT NULL DEFAULT 'Coding',")
        sb.appendLine("    difficulty TEXT NOT NULL DEFAULT 'Medium',")
        sb.appendLine("    description TEXT DEFAULT '',")
        sb.appendLine("    career_id TEXT,")
        sb.appendLine("    topic TEXT,")
        sb.appendLine("    xp_reward INTEGER DEFAULT 20,")
        sb.appendLine("    acceptance_rate INTEGER DEFAULT 70,")
        sb.appendLine("    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())")
        sb.appendLine(");")
        sb.appendLine()
        sb.appendLine("ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS title TEXT;")
        sb.appendLine("ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS category TEXT DEFAULT 'Coding';")
        sb.appendLine("ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS difficulty TEXT DEFAULT 'Medium';")
        sb.appendLine("ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';")
        sb.appendLine("ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS career_id TEXT;")
        sb.appendLine("ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS topic TEXT;")
        sb.appendLine("ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS xp_reward INTEGER DEFAULT 20;")
        sb.appendLine("ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS acceptance_rate INTEGER DEFAULT 70;")
        sb.appendLine()
        sb.appendLine("-- 2. Ensure RLS and permissions")
        sb.appendLine("ALTER TABLE public.practice_problems ENABLE ROW LEVEL SECURITY;")
        sb.appendLine("DO \$\$")
        sb.appendLine("BEGIN")
        sb.appendLine("    IF NOT EXISTS (")
        sb.appendLine("        SELECT 1 FROM pg_policies ")
        sb.appendLine("        WHERE tablename = 'practice_problems' AND policyname = 'Public read access for practice_problems'")
        sb.appendLine("    ) THEN")
        sb.appendLine("        CREATE POLICY \"Public read access for practice_problems\" ON public.practice_problems FOR SELECT USING (true);")
        sb.appendLine("    END IF;")
        sb.appendLine("END \$\$;")
        sb.appendLine("GRANT SELECT ON public.practice_problems TO anon, authenticated;")
        sb.appendLine()
        sb.appendLine("-- 3. Insert or Update all 52 Canonical Practice Problems")
        sb.appendLine("INSERT INTO public.practice_problems (")
        sb.appendLine("    id, title, category, difficulty, description, career_id, topic, xp_reward, acceptance_rate")
        sb.appendLine(") VALUES")

        fun escapeSql(str: String?): String {
            if (str == null) return "NULL"
            return "'" + str.replace("'", "''") + "'"
        }

        val rows = problems.map { p ->
            val idSql = escapeSql(p.id)
            val titleSql = escapeSql(p.title)
            val catSql = escapeSql(p.category)
            val diffSql = escapeSql(p.difficulty)
            val descSql = escapeSql(p.description)
            val careerSql = escapeSql(p.careerId)
            val topicSql = escapeSql(p.topic)
            val xpSql = p.xpReward.toString()
            val accRate = when (p.difficulty.lowercase()) {
                "easy" -> 85
                "hard" -> 45
                else -> 68
            }.toString()

            "($idSql, $titleSql, $catSql, $diffSql, $descSql, $careerSql, $topicSql, $xpSql, $accRate)"
        }

        sb.appendLine(rows.joinToString(",\n"))
        sb.appendLine("ON CONFLICT (id) DO UPDATE SET")
        sb.appendLine("    title = EXCLUDED.title,")
        sb.appendLine("    category = EXCLUDED.category,")
        sb.appendLine("    difficulty = EXCLUDED.difficulty,")
        sb.appendLine("    description = EXCLUDED.description,")
        sb.appendLine("    career_id = EXCLUDED.career_id,")
        sb.appendLine("    topic = EXCLUDED.topic,")
        sb.appendLine("    xp_reward = EXCLUDED.xp_reward,")
        sb.appendLine("    acceptance_rate = EXCLUDED.acceptance_rate;")
        sb.appendLine()

        val targetPath = "supabase/migrations/20260921_seed_canonical_practice_problems.sql"
        val targetFile = File(targetPath)
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(sb.toString())
        println("Generated SQL migration at: ${targetFile.absolutePath} with ${problems.size} problems.")
    }
}
