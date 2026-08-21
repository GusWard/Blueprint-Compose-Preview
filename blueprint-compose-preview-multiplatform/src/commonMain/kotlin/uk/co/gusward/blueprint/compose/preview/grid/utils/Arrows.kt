package uk.co.gusward.blueprint.compose.preview.grid.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import uk.co.gusward.blueprint.compose.preview.constants.Direction
import kotlin.math.min

fun createArrowPath(
    direction: Direction,
    tip: Offset,
    lengthOfLine: Float,
    path: Path = Path(),
) : Path = path.apply {

    val hypotenuseLength = min( lengthOfLine * 0.2f, 15f)
    val baseLength = min( lengthOfLine * 0.15f, 10f)

    moveTo(
        tip.x,
        tip.y,
    )
    when (direction) {
        Direction.Left -> {
            lineTo(
                tip.x + hypotenuseLength,
                tip.y - baseLength,
            )
            lineTo(
                tip.x + hypotenuseLength,
                tip.y + baseLength,
            )
        }

        Direction.Top -> {
            lineTo(
                tip.x + baseLength,
                tip.y + hypotenuseLength,
            )
            lineTo(
                tip.x - baseLength,
                tip.y + hypotenuseLength,
            )
        }

        Direction.Right -> {
            lineTo(
                tip.x - hypotenuseLength,
                tip.y - baseLength,
            )
            lineTo(
                tip.x - hypotenuseLength,
                tip.y + baseLength,
            )
        }

        Direction.Bottom -> {
            lineTo(
                tip.x + baseLength,
                tip.y - hypotenuseLength,
            )
            lineTo(
                tip.x - baseLength,
                tip.y - hypotenuseLength,
            )
        }
    }
    lineTo(
        tip.x,
        tip.y,
    )
    close()
}
