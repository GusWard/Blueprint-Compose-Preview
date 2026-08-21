package uk.co.gusward.example.multiplatform.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.co.gusward.blueprint.compose.preview.preview.BlueprintPreview

@Composable
fun ExampleTextFieldComponent() {
    var text by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier.wrapContentSize()
            .padding(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Enter Destination") }
        )
    }
}

@Composable
fun ExampleTextFieldComponentNormalPreview() {
    ExampleTextFieldComponent()
}

@Composable
fun ExampleTextFieldComponentPreview() {
    BlueprintPreview {
        ExampleTextFieldComponent()
    }
}