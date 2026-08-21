package uk.co.gusward.blueprint.compose.preview.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.gusward.blueprint.compose.preview.constants.SemanticColors
import uk.co.gusward.blueprint.compose.preview.grid.BlueprintGrid
import uk.co.gusward.blueprint.compose.preview.items.BlueprintItemData
import uk.co.gusward.blueprint.compose.preview.utils.formatNumber
import uk.co.gusward.blueprint.compose.preview.utils.getCallSiteId

@Composable
fun BlueprintPreview(
    backgroundAlpha: Float = 1f,
    contentAlpha: Float = 1f,
    showInternalItems: Boolean = false,
    content: @Composable () -> Unit
) {
    val stabilityId = remember { getCallSiteId() }

    BlueprintTheme(backgroundAlpha = backgroundAlpha) {
        val blueprintItemDataState = rememberBlueprintItems(showInternalItems, stabilityId)

        Box(
            modifier = Modifier.clipToBounds()
        ) {
            Box(
                modifier = Modifier.alpha(contentAlpha)
            ) {
                content()
            }

            Box(modifier = Modifier.matchParentSize()) {
                BlueprintGrid(
                    gridSize = 24.dp,
                    blueprintItems = blueprintItemDataState,
                    stabilityId = stabilityId,
                    alpha = backgroundAlpha
                ) {
                    if (blueprintItemDataState.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "refresh to see blueprint ☝\uFE0F",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .semantics { testTag = "blueprint_fallback_text" }
                                    .background(SemanticColors.BlueprintBackground)
                                    .border(1.dp, Color.White)
                                    .padding(16.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .semantics { testTag = "blueprint_internal_overlay" }
                        ) {
                            blueprintItemDataState.values.forEach { item ->
                                PassiveBlueprintItemOverlay(item, backgroundAlpha)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassiveBlueprintItemOverlay(itemData: BlueprintItemData, backgroundAlpha: Float) {
    Box(
        modifier = Modifier
            .offset { IntOffset(itemData.position.x.toInt(), itemData.position.y.toInt()) }
            .size(
                width = LocalDensity.current.run { itemData.size.width.toDp() },
                height = LocalDensity.current.run { itemData.size.height.toDp() }
            )
            .clearAndSetSemantics { }
            .border(width = 2.dp, color = Color.White.copy(alpha = backgroundAlpha))
            .background(SemanticColors.BlueprintBackground.copy(alpha = backgroundAlpha))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            path.moveTo(size.width, 0f)
            path.lineTo(size.width, size.height)
            path.lineTo(0f, size.height)
            path.lineTo(0f, 0f)

            clipPath(path = path, clipOp = ClipOp.Intersect) {
                val spacing = size.width / 10
                var start = spacing
                do {
                    drawLine(
                        start = Offset(x = 0f, y = start),
                        end = Offset(x = start, y = 0f),
                        color = Color.White.copy(alpha = backgroundAlpha * 0.5f),
                        strokeWidth = 2f,
                    )
                    start += spacing
                } while (start < size.width + size.height)
            }
        }

        val itemSize = itemData.size
        val density = LocalDensity.current
        val itemDpHeight = density.run { itemSize.height.toDp() }
        val itemDpWidth = density.run { itemSize.width.toDp() }

        var fontSize = 12.sp
        var vPadding = 2.dp
        var hPadding = 2.dp
        
        val fullHeightNeeded = 26.dp
        val textOnlyHeight = 18.dp
        val fullWidthNeeded = 60.dp

        if (itemDpHeight < fullHeightNeeded) {
            if (itemDpHeight >= textOnlyHeight) {
                val progress = (itemDpHeight - textOnlyHeight) / (fullHeightNeeded - textOnlyHeight)
                vPadding = (2 * progress).dp
            } else {
                vPadding = 0.dp
                val heightScale = (itemDpHeight / textOnlyHeight).coerceAtLeast(0.4f)
                fontSize = (12 * heightScale).sp
                hPadding = (2 * heightScale).dp
            }
        }

        if (itemDpWidth < fullWidthNeeded) {
            val widthScale = (itemDpWidth / fullWidthNeeded).coerceAtLeast(0.4f)
            val currentFontValue = fontSize.value
            val widthScaledFontValue = 12 * widthScale
            if (widthScaledFontValue < currentFontValue) {
                fontSize = widthScaledFontValue.sp
                hPadding = (2 * (widthScaledFontValue / 12f)).dp
            }
        }

        val labelText = density.run {
            val width = formatNumber(itemSize.width.toDp().value)
            val height = formatNumber(itemSize.height.toDp().value)
            "${width}x${height}"
        }

        if (itemSize.width > (itemSize.height * 2)) {
            Row(
                modifier = Modifier
                    .background(SemanticColors.BlueprintBackground.copy(alpha = 1f))
                    .border(1.dp, Color.White.copy(alpha = 1f))
                    .padding(horizontal = hPadding, vertical = vPadding)
                    .align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    fontSize = fontSize,
                    color = Color.White.copy(alpha = 1f),
                    fontWeight = FontWeight.Medium,
                    text = labelText,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .background(SemanticColors.BlueprintBackground.copy(alpha = 1f))
                    .border(1.dp, Color.White.copy(alpha = 1f))
                    .padding(horizontal = hPadding, vertical = vPadding)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    fontSize = fontSize,
                    color = Color.White.copy(alpha = 1f),
                    fontWeight = FontWeight.Medium,
                    text = labelText,
                )
            }
        }
    }
}
