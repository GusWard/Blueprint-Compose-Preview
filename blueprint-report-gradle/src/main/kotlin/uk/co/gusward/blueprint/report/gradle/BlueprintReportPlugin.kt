package uk.co.gusward.blueprint.report.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService

class BlueprintReportPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("blueprintReport", BlueprintReportExtension::class.java)

        val manifestFile = project.layout.buildDirectory
            .file("generated/ksp/debug/resources/blueprint-manifest.json")
        val captureDir = project.layout.buildDirectory.dir("blueprint-captures")

        // Renderer JVM classpath: compose-preview-renderer fat jar + layoutlib framework classes
        val rendererConfig = project.configurations.create("blueprintRenderer") {
            it.isCanBeResolved = true; it.isCanBeConsumed = false; it.isVisible = false
        }
        project.dependencies.add("blueprintRenderer", "com.android.tools.compose:compose-preview-renderer:0.0.1-alpha15")
        project.dependencies.add("blueprintRenderer", "com.android.tools.layoutlib:layoutlib:$LAYOUTLIB_VERSION")

        // Layoutlib native data — unpacked by PrepareLayoutlibTask (no Android Studio needed)
        val runtimeConfig = project.configurations.create("blueprintLayoutlibRuntime") {
            it.isCanBeResolved = true; it.isCanBeConsumed = false; it.isVisible = false
        }
        val resourcesConfig = project.configurations.create("blueprintLayoutlibResources") {
            it.isCanBeResolved = true; it.isCanBeConsumed = false; it.isVisible = false
        }
        project.dependencies.add("blueprintLayoutlibRuntime",
            "com.android.tools.layoutlib:layoutlib-runtime:$LAYOUTLIB_VERSION:${layoutlibOsClassifier()}")
        project.dependencies.add("blueprintLayoutlibResources",
            "com.android.tools.layoutlib:layoutlib-resources:$LAYOUTLIB_VERSION")

        val prepareLayoutlib = project.tasks.register("blueprintPrepareLayoutlib", PrepareLayoutlibTask::class.java) {
            it.runtimeJar.from(runtimeConfig)
            it.resourcesJar.from(resourcesConfig)
            it.layoutlibDir.set(project.layout.buildDirectory.dir("blueprint-layoutlib"))
            it.group = "Blueprint"
            it.description = "Downloads and unpacks Layoutlib from Maven (no Android Studio required)"
        }

        val captureTask = project.tasks.register("blueprintCapture", BlueprintCaptureTask::class.java) {
            it.manifestFile.set(manifestFile)
            it.outputDir.set(captureDir)
            it.rendererClasspath.from(rendererConfig)
            it.layoutlibDir.set(prepareLayoutlib.flatMap { t -> t.layoutlibDir })
            it.extensionFqnPatterns.set(project.provider { extension.fqnPatterns })
            it.dependsOn(prepareLayoutlib)
            it.group = "Blueprint"
            it.description = "Renders @Preview composables to PNG pairs using compose-preview-renderer"

            // layoutlib 16.x requires Java 21; use a Gradle toolchain to find it automatically.
            val toolchains = project.extensions.findByType(JavaToolchainService::class.java)
            if (toolchains != null) {
                it.javaLauncher.set(
                    toolchains.launcherFor { spec ->
                        spec.languageVersion.set(JavaLanguageVersion.of(21))
                    }
                )
            }
        }

        project.tasks.register("blueprintReport", BlueprintReportTask::class.java) {
            it.captureDir.set(captureDir)
            it.manifestFile.set(manifestFile)
            it.outputHtml.set(project.layout.buildDirectory.file("reports/blueprint/blueprint-report.html"))
            it.dependsOn(captureTask)
            it.group = "Blueprint"
            it.description = "Generates HTML report from blueprint PNG pairs"
        }

        project.plugins.withId("com.android.application") { applyKspAndDeps(project, captureTask, extension) }
        project.plugins.withId("com.android.library") { applyKspAndDeps(project, captureTask, extension) }
    }

    private fun applyKspAndDeps(project: Project, captureTask: org.gradle.api.tasks.TaskProvider<BlueprintCaptureTask>, extension: BlueprintReportExtension) {
        if (!project.plugins.hasPlugin("com.google.devtools.ksp")) {
            project.plugins.apply("com.google.devtools.ksp")
        }
        project.dependencies.add("kspDebug", project.dependencies.create(
            project.dependencies.project(mapOf("path" to ":blueprint-report-ksp"))))

        project.afterEvaluate {
            val bgAlpha = extension.backgroundAlpha.coerceIn(0f, 1f).also { coerced ->
                if (extension.backgroundAlpha != coerced)
                    project.logger.warn("blueprintReport: backgroundAlpha ${extension.backgroundAlpha} is out of range [0, 1] — clamped to $coerced")
            }
            val ctAlpha = extension.contentAlpha.coerceIn(0f, 1f).also { coerced ->
                if (extension.contentAlpha != coerced)
                    project.logger.warn("blueprintReport: contentAlpha ${extension.contentAlpha} is out of range [0, 1] — clamped to $coerced")
            }
            val kspExt = project.extensions.findByName("ksp") ?: return@afterEvaluate
            try {
                val argMethod = kspExt.javaClass.getMethod("arg", String::class.java, String::class.java)
                argMethod.invoke(kspExt, "blueprint.backgroundAlpha", bgAlpha.toString())
                argMethod.invoke(kspExt, "blueprint.contentAlpha", ctAlpha.toString())
            } catch (e: Exception) {
                project.logger.warn("blueprintReport: could not forward alpha options to KSP — ${e.message}")
            }
        }

        captureTask.configure { task ->
            task.dependsOn("compileDebugUnitTestKotlin")
            // Real resource APK from AGP (only present when isIncludeAndroidResources=true).
            // If absent, BlueprintCaptureTask creates a minimal empty ZIP so the renderer starts.
            task.resourceApkFile.set(
                project.layout.buildDirectory.file("intermediates/linked_res_for_unit_tests/debug/linked-resources-debug.ap_")
            )

            // Runtime deps → renderer's classPath.
            // Use artifactView with artifactType=jar to avoid variant ambiguity with local Android modules.
            val jarAttr = org.gradle.api.attributes.Attribute.of("artifactType", String::class.java)
            val runtimeJars = project.configurations.named("debugRuntimeClasspath").map { config ->
                config.incoming.artifactView { view ->
                    view.isLenient = true
                    view.attributes.attribute(jarAttr, "jar")
                }.files
            }
            task.appClasspath.from(runtimeJars)

            // Compiled project classes → renderer's projectClassPath.
            // debugUnitTest R.jar contains ALL transitive library R classes (including
            // compose-ui's R$id fields) needed to load ComposeViewAdapter. It's generated
            // by compileDebugUnitTestKotlin.
            task.projectClasspath.from(project.layout.buildDirectory.dir("tmp/kotlin-classes/debug"))
            task.projectClasspath.from(
                project.layout.buildDirectory.file(
                    "intermediates/compile_and_runtime_not_namespaced_r_class_jar/debugUnitTest/R.jar"))
        }

        // Resource task name differs between app (processDebugResources) and library (packageDebugResources).
        project.afterEvaluate {
            val resourceTask = project.tasks.findByName("processDebugResources")
                ?: project.tasks.findByName("packageDebugResources")
            if (resourceTask != null) {
                captureTask.configure { task -> task.dependsOn(resourceTask) }
            }
        }
    }

    private fun layoutlibOsClassifier(): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        return when {
            "mac" in os || "darwin" in os -> if ("aarch64" in arch || "arm" in arch) "mac-arm" else "mac"
            "win" in os -> "win"
            else -> "linux"
        }
    }

    private companion object {
        const val LAYOUTLIB_VERSION = "16.2.4"
    }
}
