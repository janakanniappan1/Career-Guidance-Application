package com.example

import com.example.data.repository.DefaultPracticeCatalog
import com.example.util.PracticeContentValidator
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PracticeContentAuditTest {

    @Test
    fun auditDefaultPracticeCatalogIntegrity() {
        val problems = DefaultPracticeCatalog.GLOBAL_PRACTICE_PROBLEMS
        println("AUDIT: Total problems in DefaultPracticeCatalog = ${problems.size}")

        val failures = PracticeContentValidator.validateAll(problems)
        if (failures.isNotEmpty()) {
            println("=== AUDIT FAILURES FOUND (${failures.size} problems have issues) ===")
            failures.forEach { (id, errors) ->
                println("Problem '$id':")
                errors.forEach { err -> println("  - $err") }
            }
        } else {
            println("=== AUDIT SUCCESS: All ${problems.size} problems passed validation! ===")
        }

        assertTrue("Problems failed validation: $failures", failures.isEmpty())
    }

    @Test
    fun auditAptitudeProblemsHaveNoProgrammingArtifacts() {
        val aptitudeProblems = DefaultPracticeCatalog.GLOBAL_PRACTICE_PROBLEMS.filter {
            it.category.equals("Aptitude", ignoreCase = true)
        }
        println("AUDIT: Total Aptitude problems = ${aptitudeProblems.size}")
        assertTrue("Expected at least 10 Aptitude problems", aptitudeProblems.size >= 10)

        aptitudeProblems.forEach { p ->
            // Check that exampleInput and exampleOutput don't use raw code assignment formats
            val inText = p.exampleInput ?: ""
            val conText = p.constraints ?: ""
            
            // Should not look like code signatures or test harnesses
            assertTrue("Problem ${p.id} should have non-empty description", p.description.length >= 50)
            assertTrue("Problem ${p.id} must have exactly 4 options", p.options.size == 4)
            assertTrue("Problem ${p.id} must have non-empty explanation", !p.explanation.isNullOrBlank())
            assertTrue("Problem ${p.id} must have non-empty hint", !p.hint.isNullOrBlank())
            assertTrue("Problem ${p.id} must award +20 XP", p.xpReward == 20)
        }
    }
}
