package uk.co.gusward.blueprint.compose.preview.preview

import androidx.compose.runtime.Composable
import uk.co.gusward.blueprint.compose.preview.items.BlueprintItemData

@Composable
expect fun rememberBlueprintItems(
    showInternalItems: Boolean,
    stabilityId: String
): Map<String, BlueprintItemData>
