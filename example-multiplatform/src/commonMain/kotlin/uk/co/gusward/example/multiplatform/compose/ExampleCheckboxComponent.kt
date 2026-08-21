package uk.co.gusward.example.multiplatform.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Checkbox
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
fun ExampleCheckboxComponent() {
    var checked by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.wrapContentSize()
            .padding(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { checked = it }
            )
            Text(
                text = "Accept terms"
            )
        }
    }
}

@Composable
fun ExampleCheckboxComponentNormalPreview() {
    ExampleCheckboxComponent()
}

@Composable
fun ExampleCheckboxComponentPreview() {
    BlueprintPreview {
        ExampleCheckboxComponent()
    }
}
