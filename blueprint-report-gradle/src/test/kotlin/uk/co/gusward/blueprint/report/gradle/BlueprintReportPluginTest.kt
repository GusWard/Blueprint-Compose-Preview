package uk.co.gusward.blueprint.report.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlueprintReportPluginTest {

    @Test fun `plugin registers blueprintReport task`() {
        val projectDir = createTempDir()
        projectDir.resolve("settings.gradle.kts").writeText("")
        projectDir.resolve("build.gradle.kts").writeText("""
            plugins {
                id("uk.co.gusward.blueprint-report")
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--all")
            .build()

        assert(result.output.contains("blueprintReport")) {
            "blueprintReport task not found in task list"
        }
        assert(result.output.contains("blueprintCapture")) {
            "blueprintCapture task not found in task list"
        }
    }

    @Test fun `blueprintReport task produces html file`() {
        val projectDir = createTempDir()
        // Minimal Android-free project that the plugin can attach to
        // settings.gradle.kts is empty — plugin is injected via withPluginClasspath()
        projectDir.resolve("settings.gradle.kts").writeText("")
        projectDir.resolve("build.gradle.kts").writeText("""
            plugins { id("uk.co.gusward.blueprint-report") }
        """.trimIndent())
        // Fake manifest so the task has something to read
        val manifestDir = projectDir.resolve("build/generated/blueprint").also { it.mkdirs() }
        manifestDir.resolve("blueprint-manifest.json").writeText("[]")

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("blueprintReport", "--stacktrace")
            .build()

        val html = projectDir.resolve("build/reports/blueprint/blueprint-report.html")
        assertTrue(html.exists(), "HTML report not generated")
        assertTrue(html.readText().contains("Blueprint Report"), "HTML missing title")
    }
}
