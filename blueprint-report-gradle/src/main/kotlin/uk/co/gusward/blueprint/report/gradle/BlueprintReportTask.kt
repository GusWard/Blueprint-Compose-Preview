package uk.co.gusward.blueprint.report.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

abstract class BlueprintReportTask : DefaultTask() {

    @get:Optional
    @get:InputDirectory
    abstract val captureDir: DirectoryProperty

    @get:Internal
    abstract val manifestFile: RegularFileProperty

    @get:OutputFile
    abstract val outputHtml: RegularFileProperty

    @TaskAction
    fun report() {
        val file = manifestFile.orNull?.asFile
        if (file == null || !file.exists()) {
            logger.lifecycle("blueprintReport: no manifest found — did KSP run? (run compileDebugKotlin first)")
            return
        }
        val manifest: List<ManifestEntry> = com.google.gson.Gson().fromJson(
            file.readText(),
            object : com.google.gson.reflect.TypeToken<List<ManifestEntry>>() {}.type
        )
        val captureDirectory = if (captureDir.isPresent) captureDir.get().asFile else project.file("build/blueprint-captures")
        // Only include entries that were actually captured — entries excluded by the FQN filter
        // (or that failed to render) have no PNG files and should not appear in the report.
        val captured = manifest.filter { entry ->
            val pkgPath = entry.pkg.replace('.', '/')
            val funcName = entry.sourceFqn.substringAfterLast('.')
            captureDirectory.resolve("$pkgPath/${funcName}_plain.png").exists() ||
            captureDirectory.resolve("$pkgPath/${funcName}_blueprint.png").exists()
        }
        val html = HtmlReporter.generate(captureDirectory, captured)
        val out = outputHtml.get().asFile
        out.parentFile.mkdirs()
        out.writeText(html)
        println("blueprintReport: report written to ${out.absolutePath}")
    }
}
