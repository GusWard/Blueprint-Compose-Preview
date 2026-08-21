package uk.co.gusward.example.multiplatform.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.co.gusward.blueprint.compose.preview.preview.BlueprintPreview

@Composable
fun ExampleSliderComponent() {
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }

    Box(
        modifier = Modifier.wrapContentSize()
            .padding(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it }
        )
    }
}

@Composable
fun ExampleSliderComponentNormalPreview() {
    ExampleSliderComponent()
}

@Composable
fun ExampleSliderComponentPreview() {
    BlueprintPreview {
        ExampleSliderComponent()
    }
}
