package uk.co.gusward.blueprint.compose.preview.grid.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.toSize

fun DrawScope.drawBlueprintLineLabel(
    textLayoutResult: TextLayoutResult,
    textTopLeft: Offset,
    backgroundColor: Color,
) {
    val textSize = textLayoutResult.size.toSize()

    drawRect(
        color = backgroundColor.copy(alpha = 1f),
        topLeft = textTopLeft,
        size = textSize,
    )

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = textTopLeft,
    )
}
