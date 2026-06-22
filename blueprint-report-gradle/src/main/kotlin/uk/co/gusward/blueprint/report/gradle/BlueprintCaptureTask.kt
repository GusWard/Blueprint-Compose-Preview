package uk.co.gusward.blueprint.report.gradle

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.jvm.toolchain.JavaLauncher
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipOutputStream

abstract class BlueprintCaptureTask : DefaultTask() {

    @get:Internal
    abstract val manifestFile: org.gradle.api.file.RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:InputFiles
    @get:Classpath
    abstract val rendererClasspath: ConfigurableFileCollection

    /**
     * App's runtime dependencies — passed as `classPath` to the renderer so it can load
     * Compose, Material, and all other library classes used by the composables.
     */
    @get:InputFiles
    @get:Classpath
    abstract val appClasspath: ConfigurableFileCollection

    /**
     * The project's own compiled classes — passed as `projectClassPath` to the renderer.
     * Typically `build/tmp/kotlin-classes/debug`.
     */
    @get:InputFiles
    @get:Classpath
    abstract val projectClasspath: ConfigurableFileCollection

    /**
     * Processed resources APK (`resources-debug.ap_`) so the renderer can resolve @string/themes.
     * Marked `@Internal` to avoid overlapping-output validation with other AGP tasks.
     */
    @get:Internal
    abstract val resourceApkFile: org.gradle.api.file.RegularFileProperty

    /**
     * Unpacked Layoutlib directory from [PrepareLayoutlibTask]. Marked `@Internal` to avoid
     * hashing ~80 MB; up-to-date correctness comes from [PrepareLayoutlibTask]'s own inputs.
     */
    @get:Internal
    abstract val layoutlibDir: DirectoryProperty

    /**
     * Java 21+ launcher for the renderer subprocess. layoutlib 16.x requires Java 21;
     * configured automatically via Gradle toolchains. Falls back to the current JVM if absent.
     */
    @get:Nested
    @get:Optional
    abstract val javaLauncher: Property<JavaLauncher>

    @get:Input
    @get:Optional
    abstract val extensionFqnPatterns: ListProperty<String>

    private val cliFqnPatterns: MutableList<String> = mutableListOf()

    @Option(option = "fqn", description = "FQN filter pattern (repeatable, overrides DSL config)")
    fun addFqnPattern(pattern: String) { cliFqnPatterns += pattern }

    @TaskAction
    fun capture() {
        val manifest = readManifest()
        val patterns = effectivePatterns()
        val filtered = manifest.filter { FqnFilter.matches(it.sourceFqn, patterns) }

        if (filtered.isEmpty()) {
            logger.lifecycle("blueprintCapture: no previews matched filters")
            return
        }

        val out = outputDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()

        val (succeeded, failed) = runLayoutlib(filtered, out)
        logger.lifecycle("blueprintCapture: rendered ${succeeded.size} previews successfully")

        if (failed.isNotEmpty()) {
            logger.warn("blueprintCapture: ${failed.size} preview(s) failed to render — " +
                "check ${temporaryDir.resolve("renderer.log")} for details")
        }
    }

    private fun readManifest(): List<ManifestEntry> {
        val file = manifestFile.orNull?.asFile ?: return emptyList()
        if (!file.exists()) {
            logger.lifecycle("blueprintCapture: no manifest at ${file.absolutePath} — did KSP run?")
            return emptyList()
        }
        return Gson().fromJson(file.readText(), object : TypeToken<List<ManifestEntry>>() {}.type)
    }

    private fun runLayoutlib(entries: List<ManifestEntry>, outDir: File): Pair<List<ManifestEntry>, List<ManifestEntry>> {
        val workDir = temporaryDir.also { it.mkdirs() }
        val pngDir = workDir.resolve("png").also { it.mkdirs() }
        val resultsFile = workDir.resolve("results.json").also { it.delete() }
        val metaDir = workDir.resolve("meta").also { it.mkdirs() }
        val lDir = layoutlibDir.get().asFile

        val cpJoined = rendererClasspath.files.joinToString(File.pathSeparator) { it.absolutePath }
        if (cpJoined.isEmpty()) {
            logger.warn("blueprintCapture: renderer classpath is empty — skipping render")
            return Pair(emptyList(), entries)
        }

        val gson = Gson()
        val screenshotsList = entries.flatMap { entry ->
            val device = deviceSpec(entry)
            listOf(
                linkedMapOf(
                    "methodFQN" to entry.plainJvmFqn,
                    "methodParams" to emptyList<Any>(),
                    "previewParams" to linkedMapOf("apiLevel" to "36", "device" to device),
                    "previewId" to "${entry.sourceFqn}__plain"
                ),
                linkedMapOf(
                    "methodFQN" to entry.blueprintJvmFqn,
                    "methodParams" to emptyList<Any>(),
                    "previewParams" to linkedMapOf("apiLevel" to "36", "device" to device),
                    "previewId" to "${entry.sourceFqn}__blueprint"
                )
            )
        }
        // The renderer requires a non-empty resourceApkPath pointing to a valid ZIP.
        // Use the real AP_ if available; otherwise create an empty ZIP so the renderer starts.
        val resourceApkPath = resourceApkFile.orNull?.asFile?.takeIf { it.isFile }?.absolutePath
            ?: workDir.resolve("empty-resources.ap_").also { emptyApk ->
                ZipOutputStream(emptyApk.outputStream()).use { it.finish() }
            }.absolutePath

        val input = linkedMapOf<String, Any>(
            "fontsPath" to "",
            "layoutlibPath" to lDir.absolutePath,
            "metaDataFolder" to metaDir.absolutePath,
            "outputFolder" to pngDir.absolutePath,
            "classPath" to appClasspath.files.map { it.absolutePath },
            "projectClassPath" to projectClasspath.files.map { it.absolutePath },
            "namespace" to "",
            "resourceApkPath" to resourceApkPath,
            "resultsFilePath" to resultsFile.absolutePath,
            "screenshots" to screenshotsList
        )
        val inputFile = workDir.resolve("renderer-input.json")
        inputFile.writeText(gson.toJson(input))

        val javaBin = javaLauncher.orNull?.executablePath?.asFile?.absolutePath
            ?: File(File(System.getProperty("java.home"), "bin"), "java").absolutePath
        val process = ProcessBuilder(
            javaBin,
            "--enable-native-access=ALL-UNNAMED",
            "-cp", cpJoined,
            "com.android.tools.render.common.MainKt",
            inputFile.absolutePath
        ).redirectErrorStream(true)
         .redirectOutput(workDir.resolve("renderer.log"))
         .start()

        // The renderer keeps its JVM alive via non-daemon threads; poll results.json + kill when done.
        val wantedIds = screenshotsList.mapNotNull { it["previewId"] as? String }.toSet()
        val started = System.currentTimeMillis()
        var exitedOnOwn = false
        while (System.currentTimeMillis() - started < RENDER_TIMEOUT_MS) {
            if (process.waitFor(2, TimeUnit.SECONDS)) {
                exitedOnOwn = true
                break
            }
            val results = readResults(resultsFile)
            if (results.keys.containsAll(wantedIds)) break
        }
        if (process.isAlive) process.destroyForcibly()

        val elapsed = (System.currentTimeMillis() - started) / 1000
        val exitInfo = if (exitedOnOwn) "exit=${runCatching { process.exitValue() }.getOrDefault(-1)}" else "killed after completion"
        logger.lifecycle("blueprintCapture: renderer finished in ${elapsed}s ($exitInfo)")

        val byId = readResults(resultsFile)
        val succeeded = mutableListOf<ManifestEntry>()
        val failed = mutableListOf<ManifestEntry>()

        entries.forEach { entry ->
            val plainId = "${entry.sourceFqn}__plain"
            val bpId = "${entry.sourceFqn}__blueprint"
            val plainRes = byId[plainId]
            val bpRes = byId[bpId]
            val plainImg = plainRes?.imagePath?.let { pngDir.resolve(it) }
            val bpImg = bpRes?.imagePath?.let { pngDir.resolve(it) }

            if (isSuccess(plainRes, plainImg) && isSuccess(bpRes, bpImg)) {
                // Use the function name (last segment of sourceFqn) so multiple composables
                // with the same previewName don't overwrite each other.
                val funcName = entry.sourceFqn.substringAfterLast('.')
                val pkgPath = entry.pkg.replace('.', '/')
                val destPlain = outDir.resolve("$pkgPath/${funcName}_plain.png")
                val destBp = outDir.resolve("$pkgPath/${funcName}_blueprint.png")
                destPlain.parentFile.mkdirs()
                destBp.parentFile.mkdirs()
                plainImg!!.copyTo(destPlain, overwrite = true)
                bpImg!!.copyTo(destBp, overwrite = true)
                succeeded += entry
            } else {
                logRenderFailure(entry, plainId, plainRes, bpId, bpRes)
                failed += entry
            }
        }
        return Pair(succeeded, failed)
    }

    private fun logRenderFailure(
        entry: ManifestEntry,
        plainId: String, plainRes: ShotResult?,
        bpId: String, bpRes: ShotResult?
    ) {
        listOf(plainId to plainRes, bpId to bpRes).forEach { (id, res) ->
            val why = when {
                res == null -> "no result in results.json"
                res.missingClasses.any { it.endsWith("ComposeViewAdapter") } ->
                    "missing preview host ComposeViewAdapter — add debugImplementation(\"androidx.compose.ui:ui-tooling\")"
                res.brokenClasses.isNotEmpty() -> "broken classes: ${res.brokenClasses.take(3).joinToString()}"
                res.missingClasses.isNotEmpty() -> "missing classes: ${res.missingClasses.take(3).joinToString()}"
                res.problems.isNotEmpty() -> res.problems.first()
                res.status != null && res.status != "SUCCESS" -> "status=${res.status}"
                else -> "no PNG produced"
            }
            logger.warn("blueprintCapture: '$id' — $why")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun readResults(file: File): Map<String, ShotResult> {
        if (!file.isFile) return emptyMap()
        return try {
            val root = Gson().fromJson(file.readText(), Map::class.java) as? Map<String, Any> ?: return emptyMap()
            val arr = root["screenshotResults"] as? List<Map<String, Any>> ?: return emptyMap()
            arr.mapNotNull { entry ->
                val id = entry["previewId"] as? String ?: return@mapNotNull null
                val imagePath = entry["imagePath"] as? String
                val error = entry["error"] as? Map<String, Any>
                val status = error?.get("status") as? String
                val brokenClasses = (error?.get("brokenClasses") as? List<Map<String, Any>>)
                    ?.mapNotNull { it["className"] as? String } ?: emptyList()
                val missingClasses = (error?.get("missingClasses") as? List<Any>)
                    ?.mapNotNull {
                        when (it) {
                            is String -> it
                            is Map<*, *> -> it["className"] as? String
                            else -> null
                        }
                    } ?: emptyList()
                val problems = (error?.get("problems") as? List<Map<String, Any>>)
                    ?.mapNotNull { it["stackTrace"] as? String ?: it["html"] as? String } ?: emptyList()
                id to ShotResult(imagePath, status, brokenClasses, missingClasses, problems)
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun isSuccess(result: ShotResult?, image: File?): Boolean =
        result != null &&
            (result.status == null || result.status == "SUCCESS") &&
            result.brokenClasses.isEmpty() &&
            result.problems.isEmpty() &&
            result.missingClasses.none { it.endsWith("ComposeViewAdapter") } &&
            image != null && image.isFile && image.length() > 0L

    private data class ShotResult(
        val imagePath: String?,
        val status: String?,
        val brokenClasses: List<String>,
        val missingClasses: List<String>,
        val problems: List<String>
    )

    internal fun effectivePatterns(): List<String> =
        if (cliFqnPatterns.isNotEmpty()) cliFqnPatterns else extensionFqnPatterns.get()

    private companion object {
        const val DEFAULT_DEVICE = "spec:width=411dp,height=891dp,dpi=440"
        const val RENDER_TIMEOUT_MS = 10 * 60 * 1000L

        /**
         * The device spec handed to the renderer for a preview. The renderer (Layoutlib) reads the
         * same device-spec language Android Studio does, so reproducing the IDE's density is just a
         * matter of feeding it the same device string the IDE would use:
         *
         *  - An explicit `@Preview(device = "...")` is passed through verbatim. A dpi-less
         *    `spec:width=...,height=...` defaults to dpi=420 (density 2.625) in both; `id:pixel_5`
         *    and friends resolve identically too.
         *  - With no device, we emulate Android Studio's default preview device: 411x891 @ dpi=440
         *    (density 2.75). 440 — not the spec language's 420 — is what AS uses when a @Preview
         *    names no device, so anything else makes paddings like 100.dp snap to noise (100.19dp).
         */
        fun deviceSpec(entry: ManifestEntry): String {
            if (entry.device.isNotBlank()) return entry.device
            val w = if (entry.widthDp > 0) entry.widthDp else 411
            val h = if (entry.heightDp > 0) entry.heightDp else 891
            return "spec:width=${w}dp,height=${h}dp,dpi=440"
        }
    }
}
