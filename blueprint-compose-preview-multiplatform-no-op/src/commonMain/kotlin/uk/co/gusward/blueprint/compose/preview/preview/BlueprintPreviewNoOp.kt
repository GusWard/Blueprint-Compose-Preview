package uk.co.gusward.blueprint.compose.preview.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BlueprintPreview(
    backgroundAlpha: Float = 1.0f,
    contentAlpha: Float = 1.0f,
    showInternalItems: Boolean = false,
    content: @Composable () -> Unit
) {
    content()
}

fun Modifier.blueprintId(id: String): Modifier = this
