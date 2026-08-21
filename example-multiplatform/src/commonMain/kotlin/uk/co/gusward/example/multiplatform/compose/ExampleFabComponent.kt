package uk.co.gusward.example.multiplatform.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.co.gusward.blueprint.compose.preview.preview.BlueprintPreview

@Composable
fun ExampleFabComponent() {
    Box(
        modifier = Modifier.wrapContentSize()
            .padding(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        FloatingActionButton(
            onClick = { /* do nothing */ },
        ) {
            Text("+")
        }
    }
}

@Composable
fun ExampleFabComponentNormalPreview() {
    ExampleFabComponent()
}

@Composable
fun ExampleFabComponentPreview() {
    BlueprintPreview {
        ExampleFabComponent()
    }
}
