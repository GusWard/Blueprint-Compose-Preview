package uk.co.gusward.blueprint.compose.preview.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import uk.co.gusward.blueprint.compose.preview.items.BlueprintItemData

@Composable
actual fun rememberBlueprintItems(
    showInternalItems: Boolean,
    stabilityId: String
): Map<String, BlueprintItemData> {
    // TODO: Implement semantics extraction for JVM (Desktop)
    return remember { emptyMap() }
}
