package uk.co.gusward.example.multiplatform.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import uk.co.gusward.blueprint.compose.preview.preview.BlueprintPreview

@Preview(showBackground = true, name = "1. ExampleCheckboxComponent (Normal)")
@Composable
fun ExampleCheckboxComponentNormalPreview() {
    ExampleCheckboxComponent()
}

@Preview(showBackground = true, name = "2. ExampleCheckboxComponent (Blueprint)")
@Composable
fun ExampleCheckboxComponentBlueprintPreview() {
    BlueprintPreview {
        ExampleCheckboxComponent()
    }
}
