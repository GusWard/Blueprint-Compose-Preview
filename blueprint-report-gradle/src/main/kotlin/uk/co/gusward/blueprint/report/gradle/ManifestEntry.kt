package uk.co.gusward.blueprint.report.gradle

data class ManifestEntry(
    val sourceFqn: String,
    val plainJvmFqn: String,
    val blueprintJvmFqn: String,
    val pkg: String,
    val previewName: String,
    val widthDp: Int = 0,
    val heightDp: Int = 0,
    val device: String = ""
)
