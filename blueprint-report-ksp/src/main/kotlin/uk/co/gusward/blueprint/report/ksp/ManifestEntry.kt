package uk.co.gusward.blueprint.report.ksp

data class ManifestEntry(
    val sourceFqn: String,       // com.example.ui.PreviewButton (user-visible)
    val plainJvmFqn: String,     // com.example.ui.ButtonsKt.PreviewButton
    val blueprintJvmFqn: String, // com.example.ui.ButtonsKt.__Blueprint_PreviewButton
    val pkg: String,             // com.example.ui
    val previewName: String,     // from @Preview(name="..."), empty if not specified
    val widthDp: Int = 0,        // from @Preview(widthDp=...), 0 means unspecified
    val heightDp: Int = 0,       // from @Preview(heightDp=...), 0 means unspecified
    val device: String = ""      // from @Preview(device="..."), blank if not specified
)
