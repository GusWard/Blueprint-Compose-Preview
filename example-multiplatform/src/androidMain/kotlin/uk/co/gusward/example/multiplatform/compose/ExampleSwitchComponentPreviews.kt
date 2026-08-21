package uk.co.gusward.example.multiplatform.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import uk.co.gusward.blueprint.compose.preview.preview.BlueprintPreview

@Preview(showBackground = true, name = "1. ExampleSwitchComponent (Normal)")
@Composable
fun ExampleSwitchComponentNormalPreview() {
    ExampleSwitchComponent()
}

@Preview(showBackground = true, name = "2. ExampleSwitchComponent (Blueprint)")
@Composable
fun ExampleSwitchComponentBlueprintPreview() {
    BlueprintPreview {
        ExampleSwitchComponent()
    }
}
