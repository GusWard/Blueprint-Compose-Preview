package uk.co.gusward.example.multiplatform.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import uk.co.gusward.blueprint.compose.preview.preview.BlueprintPreview

@Preview(showBackground = true, name = "1. ExampleTextFieldComponent (Normal)")
@Composable
fun ExampleTextFieldComponentNormalPreview() {
    ExampleTextFieldComponent()
}

@Preview(showBackground = true, name = "2. ExampleTextFieldComponent (Blueprint)")
@Composable
fun ExampleTextFieldComponentBlueprintPreview() {
    BlueprintPreview {
        ExampleTextFieldComponent()
    }
}
