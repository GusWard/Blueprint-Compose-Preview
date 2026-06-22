package uk.co.gusward.blueprint.report.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.gson.Gson

private const val PREVIEW_ANNOTATION = "androidx.compose.ui.tooling.preview.Preview"

class BlueprintReportProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String> = emptyMap(),
) : SymbolProcessor {

    private val gson = Gson()
    private val entries = mutableListOf<ManifestEntry>()
    private val wrappersByPackage = mutableMapOf<String, MutableList<Triple<String, Int, Int>>>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation(PREVIEW_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach { fn -> processPreviewFunction(fn) }
        return emptyList()
    }

    override fun finish() {
        generateWrappers()
        if (entries.isEmpty()) return
        val manifestFile = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true),
            packageName = "",
            fileName = "blueprint-manifest",
            extensionName = "json"
        )
        manifestFile.bufferedWriter().use { it.write(gson.toJson(entries)) }
    }

    private fun processPreviewFunction(fn: KSFunctionDeclaration) {
        val pkg = fn.packageName.asString()
        val fnName = fn.simpleName.asString()
        val containingFile = fn.containingFile ?: return
        val fileBaseName = containingFile.fileName.removeSuffix(".kt")
        val facadeClass = "${pkg}.${fileBaseName}Kt"

        val previewAnnotation = fn.annotations.firstOrNull { ann ->
            ann.shortName.asString() == "Preview"
        }
        val args = previewAnnotation?.arguments
        val previewName = args
            ?.firstOrNull { it.name?.asString() == "name" }
            ?.value
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: ""
        val widthDp = args?.firstOrNull { it.name?.asString() == "widthDp" }?.value as? Int ?: 0
        val heightDp = args?.firstOrNull { it.name?.asString() == "heightDp" }?.value as? Int ?: 0
        // Capture the @Preview device verbatim so the renderer reproduces the exact density the IDE
        // uses (e.g. a dpi-less "spec:..." defaults to 420, while no device at all defaults to 440).
        val device = (args?.firstOrNull { it.name?.asString() == "device" }?.value as? String).orEmpty()

        entries += ManifestEntry(
            sourceFqn = "$pkg.$fnName",
            plainJvmFqn = "$facadeClass.$fnName",
            blueprintJvmFqn = "$pkg.BlueprintWrappersKt.__Blueprint_$fnName",
            pkg = pkg,
            previewName = previewName,
            widthDp = widthDp,
            heightDp = heightDp,
            device = device
        )
        wrappersByPackage.getOrPut(pkg) { mutableListOf() }.add(Triple(fnName, widthDp, heightDp))
    }

    private fun generateWrappers() {
        val backgroundAlpha = options["blueprint.backgroundAlpha"]?.toFloatOrNull() ?: 1f
        val contentAlpha = options["blueprint.contentAlpha"]?.toFloatOrNull() ?: 1f
        val previewCall = buildString {
            append("BlueprintPreview(")
            append("backgroundAlpha = ${backgroundAlpha}f, ")
            append("contentAlpha = ${contentAlpha}f")
            append(")")
        }

        wrappersByPackage.forEach { (pkg, fnNames) ->
            val file = codeGenerator.createNewFile(
                dependencies = Dependencies(aggregating = true),
                packageName = pkg,
                fileName = "BlueprintWrappers",
                extensionName = "kt"
            )
            file.bufferedWriter().use { writer ->
                writer.appendLine("package $pkg")
                writer.appendLine()
                writer.appendLine("import androidx.compose.ui.tooling.preview.Preview")
                writer.appendLine("import androidx.compose.runtime.Composable")
                writer.appendLine("import uk.co.gusward.blueprint.compose.preview.preview.BlueprintPreview")
                writer.appendLine()
                fnNames.forEach { (fnName, widthDp, heightDp) ->
                    val previewArgs = buildList {
                        if (widthDp > 0) add("widthDp = $widthDp")
                        if (heightDp > 0) add("heightDp = $heightDp")
                    }.joinToString(", ")
                    val previewAnnotation = if (previewArgs.isEmpty()) "@Preview" else "@Preview($previewArgs)"
                    writer.appendLine(previewAnnotation)
                    writer.appendLine("@Composable")
                    writer.appendLine("internal fun __Blueprint_$fnName() {")
                    writer.appendLine("    $previewCall { $fnName() }")
                    writer.appendLine("}")
                    writer.appendLine()
                }
            }
        }
    }
}
