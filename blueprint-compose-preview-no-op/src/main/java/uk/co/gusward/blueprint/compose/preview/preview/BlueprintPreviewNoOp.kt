package uk.co.gusward.blueprint.compose.preview.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * A no-op implementation of BlueprintPreview for production builds.
 * This ensures the content is emitted without applying any blueprint overlays.
 */
@Composable
fun BlueprintPreview(
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 1.0f,
    contentAlpha: Float = 1.0f,
    gridColor: Color = Color.Unspecified,
    refreshIntervalMs: Long = 0L,
    content: @Composable () -> Unit
) {
    // Simply emit the content in the no-op version
    content()
}

/**
 * A no-op implementation of blueprintId for production builds.
 * This returns the original modifier without doing anything.
 */
fun Modifier.blueprintId(id: String): Modifier = this
