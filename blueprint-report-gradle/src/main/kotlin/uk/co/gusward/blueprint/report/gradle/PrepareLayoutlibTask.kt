package uk.co.gusward.blueprint.report.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Unpacks the OS-specific `layoutlib-runtime` jar and `layoutlib-resources` jar from Maven
 * into a local directory that compose-preview-renderer expects as `layoutlibPath`.
 *
 * This lets the renderer run without any Android Studio installation.
 */
abstract class PrepareLayoutlibTask : DefaultTask() {

    /** The OS-classifier `layoutlib-runtime` jar (mac-arm/mac/linux/win). */
    @get:Classpath
    abstract val runtimeJar: ConfigurableFileCollection

    /** The `layoutlib-resources` jar — placed into `data/framework_res.jar`. */
    @get:Classpath
    abstract val resourcesJar: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val layoutlibDir: DirectoryProperty

    @get:Inject
    abstract val archives: ArchiveOperations

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun prepare() {
        val out = layoutlibDir.get().asFile
        val runtime = runtimeJar.files.firstOrNull { it.name.endsWith(".jar") }
            ?: error("blueprintCapture: no layoutlib-runtime jar resolved — check blueprintLayoutlibRuntime config")
        val resources = resourcesJar.files.firstOrNull { it.name.endsWith(".jar") }
            ?: error("blueprintCapture: no layoutlib-resources jar resolved — check blueprintLayoutlibResources config")

        fs.delete { it.delete(out) }
        fs.copy {
            it.from(archives.zipTree(runtime))
            it.into(out)
        }
        fs.copy {
            it.from(resources)
            it.into(out.resolve("data"))
            it.rename { "framework_res.jar" }
        }
        logger.lifecycle("blueprintCapture: prepared Layoutlib at ${out.absolutePath}")
    }
}
