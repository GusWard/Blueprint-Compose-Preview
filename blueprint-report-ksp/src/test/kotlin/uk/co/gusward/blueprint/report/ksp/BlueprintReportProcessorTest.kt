package uk.co.gusward.blueprint.report.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlueprintReportProcessorTest {

    private val gson = Gson()

    // Stub @Preview annotation so KSP can resolve it during compile-testing
    private val previewStub = SourceFile.kotlin(
        "Preview.kt", """
        package androidx.compose.ui.tooling.preview
        annotation class Preview(val name: String = "")
    """.trimIndent()
    )

    // Stub @Composable annotation to avoid unresolved reference
    private val composableStub = SourceFile.kotlin(
        "Composable.kt", """
        package androidx.compose.runtime
        annotation class Composable
    """.trimIndent()
    )

    @Test fun `discovers @Preview function and emits manifest entry`() {
        val source = SourceFile.kotlin("Buttons.kt", """
            package com.example.ui
            import androidx.compose.ui.tooling.preview.Preview
            import androidx.compose.runtime.Composable

            @Preview
            @Composable
            fun PreviewButton() {}
        """.trimIndent())

        val compilation = KotlinCompilation().apply {
            sources = listOf(previewStub, composableStub, source)
            symbolProcessorProviders = listOf(BlueprintReportProcessorProvider())
            inheritClassPath = true
        }
        val result = compilation.compile()

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val manifestFile = compilation.kspSourcesDir.walkTopDown()
            .firstOrNull { it.name == "blueprint-manifest.json" }
        assertTrue(manifestFile?.exists() == true, "manifest not generated")

        val entries: List<ManifestEntry> = gson.fromJson(
            manifestFile!!.readText(),
            object : TypeToken<List<ManifestEntry>>() {}.type
        )
        assertEquals(1, entries.size)
        val entry = entries.first()
        assertEquals("com.example.ui.PreviewButton", entry.sourceFqn)
        assertEquals("com.example.ui.ButtonsKt.PreviewButton", entry.plainJvmFqn)
        assertEquals("com.example.ui.BlueprintWrappersKt.__Blueprint_PreviewButton", entry.blueprintJvmFqn)
        assertEquals("com.example.ui", entry.pkg)
        assertEquals("PreviewButton", entry.previewName)
    }

    @Test fun `uses @Preview name parameter when present`() {
        val source = SourceFile.kotlin("Screen.kt", """
            package com.example
            import androidx.compose.ui.tooling.preview.Preview
            import androidx.compose.runtime.Composable

            @Preview(name = "Dark theme")
            @Composable
            fun PreviewScreenDark() {}
        """.trimIndent())

        val compilation = KotlinCompilation().apply {
            sources = listOf(previewStub, composableStub, source)
            symbolProcessorProviders = listOf(BlueprintReportProcessorProvider())
            inheritClassPath = true
        }
        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val manifestFile = compilation.kspSourcesDir.walkTopDown()
            .firstOrNull { it.name == "blueprint-manifest.json" }
        assertTrue(manifestFile?.exists() == true, "manifest not generated")

        val entries: List<ManifestEntry> = gson.fromJson(
            manifestFile!!.readText(),
            object : TypeToken<List<ManifestEntry>>() {}.type
        )
        assertEquals("Dark theme", entries.first().previewName)
    }

    @Test fun `generates __Blueprint_ wrapper composable`() {
        val source = SourceFile.kotlin("Buttons.kt", """
            package com.example.ui
            import androidx.compose.ui.tooling.preview.Preview
            import androidx.compose.runtime.Composable

            @Preview
            @Composable
            fun PreviewButton() {}
        """.trimIndent())

        val compilation = KotlinCompilation().apply {
            sources = listOf(previewStub, composableStub, source)
            symbolProcessorProviders = listOf(BlueprintReportProcessorProvider())
            inheritClassPath = true
        }
        compilation.compile()

        val wrapperFile = compilation.kspSourcesDir.walkTopDown()
            .firstOrNull { it.name.contains("BlueprintWrappers") }
        assertNotNull(wrapperFile, "wrapper file not generated")
        val content = wrapperFile.readText()
        assertTrue(content.contains("fun __Blueprint_PreviewButton"), "wrapper function missing")
        assertTrue(content.contains("BlueprintPreview"), "BlueprintPreview call missing")
        assertTrue(content.contains("PreviewButton()"), "original call missing")
    }
}
