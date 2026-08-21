package uk.co.gusward.example.multiplatform.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import uk.co.gusward.blueprint.compose.preview.preview.BlueprintPreview

@Preview(showBackground = true, name = "1. ExampleRadioButtonComponent (Normal)")
@Composable
fun ExampleRadioButtonComponentNormalPreview() {
    ExampleRadioButtonComponent()
}

@Preview(showBackground = true, name = "2. ExampleRadioButtonComponent (Blueprint)")
@Composable
fun ExampleRadioButtonComponentBlueprintPreview() {
    BlueprintPreview {
        ExampleRadioButtonComponent()
    }
}
