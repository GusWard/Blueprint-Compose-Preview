package uk.co.gusward.example.multiplatform.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
fun ExampleSwitchComponent() {
    var checked by remember { mutableStateOf(true) }
    
    Box(
        modifier = Modifier.wrapContentSize()
            .padding(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Enable feature")
            Switch(
                checked = checked,
                onCheckedChange = { checked = it }
            )
        }
    }
}

@Composable
fun ExampleSwitchComponentNormalPreview() {
    ExampleSwitchComponent()
}

@Composable
fun ExampleSwitchComponentPreview() {
    BlueprintPreview {
        ExampleSwitchComponent()
    }
}