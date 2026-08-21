package uk.co.gusward.example.multiplatform.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import uk.co.gusward.blueprint.compose.preview.preview.BlueprintPreview

@Preview(showBackground = true, name = "1. ExampleSliderComponent (Normal)")
@Composable
fun ExampleSliderComponentNormalPreview() {
    ExampleSliderComponent()
}

@Preview(showBackground = true, name = "2. ExampleSliderComponent (Blueprint)")
@Composable
fun ExampleSliderComponentBlueprintPreview() {
    BlueprintPreview {
        ExampleSliderComponent()
    }
}
