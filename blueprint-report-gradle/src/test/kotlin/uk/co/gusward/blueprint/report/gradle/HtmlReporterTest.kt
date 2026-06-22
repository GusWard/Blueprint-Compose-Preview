package uk.co.gusward.blueprint.report.gradle

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class HtmlReporterTest {

    private fun pinkPixelPng(): ByteArray {
        // Minimal valid 1x1 PNG (pink pixel) — avoids needing a real screenshot
        return java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwADhQGAWjR9awAAAABJRU5ErkJggg=="
        )
    }

    @Test fun `generates html containing preview name`() {
        val captureDir = createTempDir()
        val pkgDir = captureDir.resolve("com/example/ui").also { it.mkdirs() }
        pkgDir.resolve("PreviewButton_plain.png").writeBytes(pinkPixelPng())
        pkgDir.resolve("PreviewButton_blueprint.png").writeBytes(pinkPixelPng())

        val entries = listOf(
            ManifestEntry(
                sourceFqn = "com.example.ui.PreviewButton",
                plainJvmFqn = "com.example.ui.ButtonsKt.PreviewButton",
                blueprintJvmFqn = "com.example.ui.ButtonsKt.__Blueprint_PreviewButton",
                pkg = "com.example.ui",
                previewName = ""
            )
        )

        val html = HtmlReporter.generate(captureDir, entries)

        assertTrue(html.contains("PreviewButton"), "function name missing")
        assertTrue(html.contains("com.example.ui"), "package name missing")
        assertTrue(html.contains("data:image/png;base64,"), "base64 image missing")
        assertTrue(html.startsWith("<!DOCTYPE html>"), "not a valid HTML document")
    }

    @Test fun `shows function name in braces when preview name is specified`() {
        val captureDir = createTempDir()
        val pkgDir = captureDir.resolve("com/example/ui").also { it.mkdirs() }
        pkgDir.resolve("PreviewButton_plain.png").writeBytes(pinkPixelPng())
        pkgDir.resolve("PreviewButton_blueprint.png").writeBytes(pinkPixelPng())

        val entries = listOf(
            ManifestEntry(
                sourceFqn = "com.example.ui.PreviewButton",
                plainJvmFqn = "com.example.ui.ButtonsKt.PreviewButton",
                blueprintJvmFqn = "com.example.ui.ButtonsKt.__Blueprint_PreviewButton",
                pkg = "com.example.ui",
                previewName = "Primary Button"
            )
        )

        val html = HtmlReporter.generate(captureDir, entries)

        assertTrue(html.contains("Primary Button (PreviewButton)"), "named preview should include function name in braces")
    }

    @Test fun `groups entries by package`() {
        val captureDir = createTempDir()
        listOf("com/example/ui/buttons", "com/example/ui/cards").forEach {
            captureDir.resolve(it).mkdirs()
        }
        captureDir.resolve("com/example/ui/buttons/PreviewPrimary_plain.png").writeBytes(pinkPixelPng())
        captureDir.resolve("com/example/ui/buttons/PreviewPrimary_blueprint.png").writeBytes(pinkPixelPng())
        captureDir.resolve("com/example/ui/cards/PreviewCard_plain.png").writeBytes(pinkPixelPng())
        captureDir.resolve("com/example/ui/cards/PreviewCard_blueprint.png").writeBytes(pinkPixelPng())

        val entries = listOf(
            ManifestEntry("com.example.ui.buttons.PreviewPrimary", "", "", "com.example.ui.buttons", ""),
            ManifestEntry("com.example.ui.cards.PreviewCard", "", "", "com.example.ui.cards", "")
        )
        val html = HtmlReporter.generate(captureDir, entries)

        val buttonsIdx = html.indexOf("buttons")
        val cardsIdx = html.indexOf("cards")
        assertTrue(buttonsIdx < cardsIdx, "packages not in alphabetical order in HTML")
    }
}
