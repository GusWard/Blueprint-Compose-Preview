package uk.co.gusward.example.multiplatform.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import uk.co.gusward.blueprint.compose.preview.preview.BlueprintPreview

@Preview(showBackground = true, name = "1. ExampleButtonComponent (Normal)")
@Composable
fun ExampleButtonComponentNormalPreview() {
    ExampleButtonComponent()
}

@Preview(showBackground = true, name = "2. ExampleButtonComponent (Blueprint)")
@Composable
fun ExampleButtonComponentBlueprintPreview() {
    BlueprintPreview {
        ExampleButtonComponent()
    }
}
