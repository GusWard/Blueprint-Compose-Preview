package uk.co.gusward.example.multiplatform.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import uk.co.gusward.blueprint.compose.preview.preview.BlueprintPreview

@Preview(showBackground = true, name = "1. ExampleComplexDashboardComponent (Normal)")
@Composable
fun ExampleComplexDashboardComponentNormalPreview() {
    ExampleComplexDashboardComponent()
}

@Preview(showBackground = true, name = "2. ExampleComplexDashboardComponent (Blueprint)")
@Composable
fun ExampleComplexDashboardComponentBlueprintPreview() {
    BlueprintPreview {
        ExampleComplexDashboardComponent()
    }
}
