package uk.co.gusward.blueprint.compose.preview.grid.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import uk.co.gusward.blueprint.compose.preview.constants.Direction
import uk.co.gusward.blueprint.compose.preview.constants.SemanticColors
import uk.co.gusward.blueprint.compose.preview.grid.logic.calculateBlueprintLines
import uk.co.gusward.blueprint.compose.preview.items.BlueprintItemData
import java.text.DecimalFormat
import kotlin.math.min

/**
 * Single source of truth for rendering a blueprint item box: fill, diagonal hatching,
 * border, and the centred WxH size label (with tiered scaling for small items).
 *
 * Drawn from a [DrawScope] so the SAME code serves both the live composable overlay and the
 * headless/inspection renderer — which must draw during the draw phase because Layoutlib captures
 * a single frame and never pumps the recomposition that would otherwise add per-item composables.
 */
internal fun DrawScope.drawBlueprintItem(
    item: BlueprintItemData,
    backgroundAlpha: Float,
    textMeasurer: TextMeasurer,
    decimalFormat: DecimalFormat,
) {
    val pos = item.position
    val sz = item.size

    drawRect(
        color = SemanticColors.BlueprintBackground.copy(alpha = backgroundAlpha),
        topLeft = pos,
        size = sz,
    )

    // Diagonal hatching, clipped to the item bounds.
    val hatchSpacing = sz.width / 10
    if (hatchSpacing > 0f) {
        clipRect(
            left = pos.x,
            top = pos.y,
            right = pos.x + sz.width,
            bottom = pos.y + sz.height,
        ) {
            var start = hatchSpacing
            do {
                drawLine(
                    start = Offset(x = pos.x, y = pos.y + start),
                    end = Offset(x = pos.x + start, y = pos.y),
                    color = Color.White.copy(alpha = backgroundAlpha * 0.5f),
                    strokeWidth = 2f,
                )
                start += hatchSpacing
            } while (start < sz.width + sz.height)
        }
    }

    drawRect(
        color = Color.White.copy(alpha = backgroundAlpha),
        topLeft = pos,
        size = sz,
        style = Stroke(width = 2.dp.toPx()),
    )

    // Tiered scaling: shrink vertical padding first, then the font, so the label stays legible
    // inside small items.
    val itemDpHeight = sz.height.toDp()
    val itemDpWidth = sz.width.toDp()

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

    val label = "${decimalFormat.format(itemDpWidth.value)}x${decimalFormat.format(itemDpHeight.value)}"
    val measured = textMeasurer.measure(
        text = AnnotatedString(label),
        style = TextStyle(color = Color.White, fontSize = fontSize, fontWeight = FontWeight.Medium),
    )

    val hPadPx = hPadding.toPx()
    val vPadPx = vPadding.toPx()
    val labelBgSize = Size(
        width = measured.size.width + 2 * hPadPx,
        height = measured.size.height + 2 * vPadPx,
    )
    val labelBgTopLeft = Offset(
        x = pos.x + (sz.width - labelBgSize.width) / 2f,
        y = pos.y + (sz.height - labelBgSize.height) / 2f,
    )
    drawRect(
        color = SemanticColors.BlueprintBackground,
        topLeft = labelBgTopLeft,
        size = labelBgSize,
    )
    drawRect(
        color = Color.White,
        topLeft = labelBgTopLeft,
        size = labelBgSize,
        style = Stroke(width = 1.dp.toPx()),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(labelBgTopLeft.x + hPadPx, labelBgTopLeft.y + vPadPx),
    )
}

/**
 * Single source of truth for the spacing lines (arrows + measurement labels) between items.
 * Computes the lines from [items] against the actual [canvasSize], then draws each with arrowheads
 * and a collision-avoided label. Shared by the live grid and the headless/inspection renderer.
 */
internal fun DrawScope.drawBlueprintSpacingLines(
    items: Map<String, BlueprintItemData>,
    canvasSize: Size,
    textMeasurer: TextMeasurer,
    decimalFormat: DecimalFormat,
    alpha: Float,
) {
    if (items.isEmpty()) return

    val lines = calculateBlueprintLines(items, canvasSize)
    val backgroundColor = SemanticColors.BlueprintBackground
    val measuredLineWidth = 4f
    val minimumScale = 0.05f

    lines.forEach { line ->
        val width = min(measuredLineWidth, line.length * minimumScale)
        drawLine(
            start = line.start,
            end = line.end,
            color = Color.White.copy(alpha = alpha),
            strokeWidth = width,
        )

        if (line.isVertical) {
            val isStartAboveEnd = line.start.y < line.end.y
            drawOutline(
                outline = Outline.Generic(
                    createArrowPath(Direction.Top, if (isStartAboveEnd) line.start else line.end, line.length)
                ),
                color = Color.White.copy(alpha = alpha),
            )
            drawOutline(
                outline = Outline.Generic(
                    createArrowPath(Direction.Bottom, if (isStartAboveEnd) line.end else line.start, line.length)
                ),
                color = Color.White.copy(alpha = alpha),
            )
        } else if (line.isHorizontal) {
            val isStartLeftOfEnd = line.start.x < line.end.x
            drawOutline(
                outline = Outline.Generic(
                    createArrowPath(Direction.Left, if (isStartLeftOfEnd) line.start else line.end, line.length)
                ),
                color = Color.White.copy(alpha = alpha),
            )
            drawOutline(
                outline = Outline.Generic(
                    createArrowPath(Direction.Right, if (isStartLeftOfEnd) line.end else line.start, line.length)
                ),
                color = Color.White.copy(alpha = alpha),
            )
        }

        val measuredText = textMeasurer.measure(
            text = AnnotatedString(
                text = decimalFormat.format(line.length.toDp().value),
                spanStyle = SpanStyle(fontSize = min(line.length * 0.15f, 12f).sp),
            ),
            style = TextStyle(Color.White),
        )

        val baseTextTopLeft = if (line.isVertical) {
            line.midPoint + Offset(x = 12f, y = -1f * (measuredText.size.height / 2f))
        } else {
            line.midPoint + Offset(x = -1f * (measuredText.size.width / 2f), y = 3f)
        }

        var textTopLeft = baseTextTopLeft
        val textSize = measuredText.size.toSize()

        val collision = lines.any {
            it != line && it.intersects(
                Rect(textTopLeft, Offset(textTopLeft.x + textSize.width, textTopLeft.y + textSize.height))
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
                    val candidateRect = Rect(
                        candidateTopLeft,
                        Offset(candidateTopLeft.x + textSize.width, candidateTopLeft.y + textSize.height),
                    )
                    !lines.any { it != line && it.intersects(candidateRect) }
                }
                if (bestCandidate != null) {
                    textTopLeft = baseTextTopLeft + bestCandidate
                    break
                }
            }
        }

        drawBlueprintLineLabel(measuredText, textTopLeft, backgroundColor)
    }
}
