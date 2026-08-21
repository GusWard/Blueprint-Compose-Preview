package uk.co.gusward.blueprint.compose.preview.grid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import uk.co.gusward.blueprint.compose.preview.constants.Direction
import uk.co.gusward.blueprint.compose.preview.constants.SemanticColors
import uk.co.gusward.blueprint.compose.preview.grid.logic.calculateBlueprintLines
import uk.co.gusward.blueprint.compose.preview.grid.utils.createArrowPath
import uk.co.gusward.blueprint.compose.preview.grid.utils.drawBlueprintLineLabel
import uk.co.gusward.blueprint.compose.preview.items.BlueprintItemData
import uk.co.gusward.blueprint.compose.preview.utils.formatNumber
import kotlin.math.min

private val staticGridSizeCache = mutableMapOf<String, Size>()

internal data class BlueprintLineWithLabel(
    val line: BlueprintLine,
    val textLayoutResult: TextLayoutResult,
    val textTopLeft: Offset,
)

@Composable
fun BlueprintGrid(
    gridSize: Dp,
    blueprintItems: Map<String, BlueprintItemData>,
    stabilityId: String,
    alpha: Float = 1f,
    content: @Composable () -> Unit
) {

    val spacing = LocalDensity.current.run { gridSize.toPx() }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Box(
        modifier = Modifier.fillMaxSize().background(SemanticColors.BlueprintBackground.copy(alpha = alpha))
    ) {
        var size by remember { mutableStateOf(staticGridSizeCache[stabilityId] ?: Size.Zero) }
        
        Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { 
            if (it.size.width > 200 && it.size.height > 200) {
                val newSize = it.size.toSize()
                if (size != newSize) {
                    size = newSize
                    staticGridSizeCache[stabilityId] = newSize
                }
            }
        }) {
        val screenSize = size
        
        val cachedBlueprintLinesAndLabels = remember(blueprintItems, screenSize, textMeasurer, density.density, density.fontScale) {
            if (blueprintItems.isEmpty()) return@remember emptyList()
            
            val lines = calculateBlueprintLines(blueprintItems, screenSize)
            
            lines.map { line ->
                val measuredText = textMeasurer.measure(
                    text = AnnotatedString(
                        text = formatNumber(density.run { line.length.toDp().value }),
                        spanStyle = SpanStyle(
                            fontSize = min((line.length * 0.15f), 12f).sp
                        )
                    ),
                    style = TextStyle(Color.White),
                )
                
                val baseTextTopLeft = if (line.isVertical) {
                    line.midPoint + Offset(
                        x = 12f,
                        y = -1f * (measuredText.size.height / 2f),
                    )
                } else {
                    line.midPoint + Offset(
                        x = -1f * (measuredText.size.width / 2f),
                        y = 3f,
                    )
                }
                
                var textTopLeft = baseTextTopLeft
                val textSize = measuredText.size.toSize()

                val collision = lines.any { 
                    it != line && it.intersects(
                        androidx.compose.ui.geometry.Rect(
                            textTopLeft, 
                            Offset(textTopLeft.x + textSize.width, textTopLeft.y + textSize.height)
                        )
                    ) 
                }

                if (collision) {
                    val step = 10f
                    val maxSteps = 10
                    for (i in 1..maxSteps) {
                        val offsetValue = i * step
                        val candidates = if (line.isVertical) {
                            listOf(Offset(0f, -offsetValue), Offset(0f, offsetValue))
                        } else {
                            listOf(Offset(-offsetValue, 0f), Offset(offsetValue, 0f))
                        }

                        val bestCandidate = candidates.firstOrNull { candidateOffset ->
                            val candidateTopLeft = baseTextTopLeft + candidateOffset
                            val candidateRect = androidx.compose.ui.geometry.Rect(
                                candidateTopLeft,
                                Offset(candidateTopLeft.x + textSize.width, candidateTopLeft.y + textSize.height)
                            )
                            !lines.any { it != line && it.intersects(candidateRect) }
                        }

                        if (bestCandidate != null) {
                            textTopLeft = baseTextTopLeft + bestCandidate
                            break
                        }
                    }
                }
                
                BlueprintLineWithLabel(line, measuredText, textTopLeft)
            }
        }

        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            val path = Path()
            path.moveTo(size.width, 0f)
            path.lineTo(size.width, size.height)
            path.lineTo(0f, size.height)
            path.lineTo(0f, 0f)

            clipPath(
                path = path,
                clipOp = ClipOp.Intersect
            ) {
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

        content()

        val backgroundColor = SemanticColors.BlueprintBackground

        val measuredLineWidth = 4f
        val minimumScale = 0.05f

        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            cachedBlueprintLinesAndLabels.forEach { (blueprintLine, textLayoutResult, textTopLeft) ->
                val width = min(measuredLineWidth, blueprintLine.length * minimumScale)
                drawLine(
                    start = blueprintLine.start,
                    end = blueprintLine.end,
                    color = Color.White.copy(alpha = alpha),
                    strokeWidth = width,
                )

                if (blueprintLine.isVertical) {
                    val isStartAboveEnd = blueprintLine.start.y < blueprintLine.end.y
                    drawOutline(
                        outline = Outline.Generic(createArrowPath(Direction.Top, if (isStartAboveEnd) blueprintLine.start else blueprintLine.end, blueprintLine.length)),
                        color = Color.White.copy(alpha = alpha),
                    )
                    drawOutline(
                        outline = Outline.Generic(createArrowPath(Direction.Bottom, if (isStartAboveEnd) blueprintLine.end else blueprintLine.start, blueprintLine.length)),
                        color = Color.White.copy(alpha = alpha),
                    )
                } else if (blueprintLine.isHorizontal) {
                    val isStartLeftOfEnd = blueprintLine.start.x < blueprintLine.end.x
                    drawOutline(
                        outline = Outline.Generic(createArrowPath(Direction.Left, if (isStartLeftOfEnd) blueprintLine.start else blueprintLine.end, blueprintLine.length)),
                        color = Color.White.copy(alpha = alpha),
                    )
                    drawOutline(
                        outline = Outline.Generic(createArrowPath(Direction.Right, if (isStartLeftOfEnd) blueprintLine.end else blueprintLine.start, blueprintLine.length)),
                        color = Color.White.copy(alpha = alpha),
                    )
                }
                drawBlueprintLineLabel(textLayoutResult, textTopLeft, backgroundColor)
            }
        }
    }
}
}
