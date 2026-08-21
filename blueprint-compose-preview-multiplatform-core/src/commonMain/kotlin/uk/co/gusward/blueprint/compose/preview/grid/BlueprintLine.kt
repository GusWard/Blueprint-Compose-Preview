package uk.co.gusward.blueprint.compose.preview.grid

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import uk.co.gusward.blueprint.compose.preview.items.BlueprintItemData
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Lots of maths help from Gemini on this
 */
data class BlueprintLine(
    val start: Offset,
    val end: Offset,
) {

    override fun equals(other: Any?): Boolean {

        val otherLine = other as? BlueprintLine ?: return false

        val offsetsMatchExactly = start == otherLine.start && end == otherLine.end
        val offsetsMatchReversed = start == otherLine.end && end == otherLine.start

        return offsetsMatchExactly || offsetsMatchReversed
    }

    fun intersects(item: BlueprintItemData): Boolean {
        val topLeft = item.position
        val bottomRight = Offset(
            x = item.position.x + item.size.width,
            y = item.position.y + item.size.height,
        )
        return intersects(Rect(topLeft, bottomRight))
    }

    fun intersects(rect: Rect): Boolean {
        // Check if any endpoint is inside the rectangle
        if (isPointInside(start.x, start.y, rect.topLeft, rect.bottomRight) ||
            isPointInside(end.x, end.y, rect.topLeft, rect.bottomRight)) {
            return true
        }

        // Check intersection with each side of the rectangle
        return intersects(BlueprintLine(rect.topLeft, rect.topRight)) ||
                intersects(BlueprintLine(rect.bottomLeft, rect.bottomRight)) ||
                intersects(BlueprintLine(rect.topLeft, rect.bottomLeft)) ||
                intersects(BlueprintLine(rect.topRight, rect.bottomRight))
    }

    fun intersects(otherLine: BlueprintLine): Boolean {

        val denominator = (otherLine.end.y - otherLine.start.y) *
                (end.x - start.x) - (otherLine.end.x - otherLine.start.x) *
                (end.y - start.y)

        // Lines are parallel if denominator is close to zero
        if (abs(denominator) < 1e-6) {
            return false
        }

        val numerator1 = (otherLine.end.x - otherLine.start.x) * (start.y - otherLine.start.y) - (otherLine.end.y - otherLine.start.y) * (start.x - otherLine.start.x)
        val numerator2 = (end.x - start.x) * (start.y - otherLine.start.y) - (end.y - start.y) * (start.x - otherLine.start.x)

        // Check if intersection point is within line segments
        return (numerator1 / denominator in 0.0..1.0 && numerator2 / denominator >= 0 && numerator2 / denominator <= 1)
    }
    
    private fun isPointInside(x: Float, y: Float, topLeft: Offset, bottomRight: Offset): Boolean {
        return (x >= topLeft.x && x <= bottomRight.x && y >= topLeft.y && y <= bottomRight.y)
    }

    /**
     * √((x2 – x1)² + (y2 – y1)²)
     */
    val length by lazy {
        sqrt((end.x - start.x).pow(n = 2)
                + (end.y - start.y).pow(n = 2))
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        return result
    }


    val isVertical: Boolean = start.x == end.x

    val isHorizontal: Boolean = start.y == end.y

    val midPoint: Offset = Offset(
        x = (start.x + end.x) / 2,
        y = (start.y + end.y) / 2
    )
}
