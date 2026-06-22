package uk.co.gusward.blueprint.compose.preview.grid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import uk.co.gusward.blueprint.compose.preview.constants.SemanticColors
import uk.co.gusward.blueprint.compose.preview.grid.utils.drawBlueprintSpacingLines
import uk.co.gusward.blueprint.compose.preview.items.BlueprintItemData
import java.text.DecimalFormat

@Composable
internal fun BlueprintGrid(
    gridSize: Dp,
    blueprintItems: Map<String, BlueprintItemData>,
    alpha: Float = 1f,
    content: @Composable () -> Unit
) {
    val spacing = LocalDensity.current.run { gridSize.toPx() }
    val textMeasurer = rememberTextMeasurer()
    val decimalFormat = remember { DecimalFormat("0.##") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SemanticColors.BlueprintBackground.copy(alpha = alpha))
    ) {
        /* draw grid */
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            path.moveTo(size.width, 0f)
            path.lineTo(size.width, size.height)
            path.lineTo(0f, size.height)
            path.lineTo(0f, 0f)

            clipPath(path = path, clipOp = ClipOp.Intersect) {
                var start = spacing
                val gridColor = Color.White.copy(alpha = alpha * 0.5f)
                do {
                    drawLine(
                        start = Offset(x = 0f, y = start),
                        end = Offset(x = size.width, y = start),
                        color = gridColor,
                        strokeWidth = 1f,
                    )
                    drawLine(
                        start = Offset(x = start, y = 0f),
                        end = Offset(x = start, y = size.height),
                        color = gridColor,
                        strokeWidth = 1f,
                    )
                    start += spacing
                } while (start < size.width + size.height)
            }
        }

        /* display content FIRST so the measuring lines draw over the top of it */
        content()

        /* draw blueprint items' measured spacing lines */
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBlueprintSpacingLines(
                items = blueprintItems,
                canvasSize = size,
                textMeasurer = textMeasurer,
                decimalFormat = decimalFormat,
                alpha = alpha,
            )
        }
    }
}
