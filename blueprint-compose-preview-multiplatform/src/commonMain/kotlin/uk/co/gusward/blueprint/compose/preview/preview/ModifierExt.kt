package uk.co.gusward.blueprint.compose.preview.preview

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag

/**
 * A passive modifier that forces a layout node to be included in the Semantics tree,
 * allowing PassiveBlueprintPreview to measure it even if it has no text or accessibility roles.
 */
fun Modifier.blueprintId(id: String): Modifier = this.semantics {
    testTag = id
}
