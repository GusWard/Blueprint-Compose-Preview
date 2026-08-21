package uk.co.gusward.example.multiplatform.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import uk.co.gusward.blueprint.compose.preview.preview.BlueprintPreview

@Preview(showBackground = true, name = "1. ExampleFabComponent (Normal)")
@Composable
fun ExampleFabComponentNormalPreview() {
    ExampleFabComponent()
}

@Preview(showBackground = true, name = "2. ExampleFabComponent (Blueprint)")
@Composable
fun ExampleFabComponentBlueprintPreview() {
    BlueprintPreview {
        ExampleFabComponent()
    }
}
