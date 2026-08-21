package uk.co.gusward.blueprint.compose.preview.grid.logic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import uk.co.gusward.blueprint.compose.preview.constants.Direction
import uk.co.gusward.blueprint.compose.preview.grid.BlueprintLine
import uk.co.gusward.blueprint.compose.preview.items.BlueprintItemData

fun calculateBlueprintLines(
    blueprintItems: Map<String, BlueprintItemData>,
    screenSize: Size,
    lineSpacingOffset: Float = 40f
): List<BlueprintLine> {
    val validBlueprintItems = blueprintItems.filterValues { it.size.width > 0 && it.size.height > 0 }
    val completedBlueprintLines = mutableListOf<BlueprintLine>()
    val connectedHorizontalPairs = mutableSetOf<Set<String>>()
    val connectedVerticalPairs = mutableSetOf<Set<String>>()

    validBlueprintItems.forEach { currentEntry ->

        /* VERTICAL connecting lines */
        validBlueprintItems.values.filter { other ->
            currentEntry.value != other && (currentEntry.value isDirectlyAbove other || currentEntry.value isDirectlyBelow other)
        }.forEach lineOfSightEntries@ { other ->

            val isAbove = currentEntry.value isDirectlyAbove other
            val itemPair = setOf(currentEntry.value.id, other.id)
            if (connectedVerticalPairs.contains(itemPair)) return@lineOfSightEntries
            connectedVerticalPairs.add(itemPair)

            val overlapStartX = maxOf(currentEntry.value.position.x, other.position.x)
            val overlapEndX = minOf(currentEntry.value.position.x + currentEntry.value.size.width, other.position.x + other.size.width)
            val baseVerticalLineX = (overlapStartX + overlapEndX) / 2
            
            val baseLine = BlueprintLine(
                start = Offset(
                    x = baseVerticalLineX,
                    y = currentEntry.value.position.y + if (isAbove) currentEntry.value.size.height else 0f,
                ),
                end = Offset(
                    x = baseVerticalLineX,
                    y = other.position.y + if (isAbove) 0f else other.size.height,
                )
            )
            if (completedBlueprintLines.contains(baseLine)) {
                return@lineOfSightEntries
            }

            var verticalLineX = baseVerticalLineX
            val overlaps = completedBlueprintLines.filter { 
                it.isVertical && it.start.x == verticalLineX &&
                maxOf(it.start.y, it.end.y) > minOf(currentEntry.value.position.y, other.position.y) &&
                minOf(it.start.y, it.end.y) < maxOf(currentEntry.value.position.y, other.position.y)
            }
            
            if (overlaps.isNotEmpty()) {
                verticalLineX += (overlaps.size * lineSpacingOffset)
            }

            val blueprintLine = BlueprintLine(
                start = Offset(
                    x = verticalLineX,
                    y = currentEntry.value.position.y
                            + if (isAbove) currentEntry.value.size.height else 0f,
                ),
                end = Offset(
                    x = verticalLineX,
                    y = other.position.y
                            + if (isAbove) 0f else other.size.height,
                )
            )

            val intersectsAnyBlueprintItem = validBlueprintItems
                .filter {
                    it.value != currentEntry.value && it.value != other && !it.value.contains(currentEntry.value)
                }
                .any {
                    blueprintLine.intersects(it.value)
                }

            if (intersectsAnyBlueprintItem) {
                return@lineOfSightEntries
            }

            completedBlueprintLines += blueprintLine
        }

        /* horizontal connecting lines */
        validBlueprintItems.values
            .filter { other ->
            currentEntry.value != other && (currentEntry.value isDirectlyLeftOf other || currentEntry.value isDirectlyRightOf other )
        }.forEach lineOfSightEntries@ { other ->

            val isLeft = currentEntry.value isDirectlyLeftOf other
            val itemPair = setOf(currentEntry.value.id, other.id)
            if (connectedHorizontalPairs.contains(itemPair)) return@lineOfSightEntries
            connectedHorizontalPairs.add(itemPair)

            val overlapStartY = maxOf(currentEntry.value.position.y, other.position.y)
            val overlapEndY = minOf(currentEntry.value.position.y + currentEntry.value.size.height, other.position.y + other.size.height)
            val baseHorizontalLineY = (overlapStartY + overlapEndY) / 2

            val baseLine = BlueprintLine(
                start = Offset(
                    x = currentEntry.value.position.x + if (isLeft) currentEntry.value.size.width else 0f,
                    y = baseHorizontalLineY,
                ),
                end = Offset(
                    x = other.position.x + if (isLeft) 0f else other.size.width,
                    y = baseHorizontalLineY,
                )
            )
            if (completedBlueprintLines.contains(baseLine)) {
                return@lineOfSightEntries
            }

            var horizontalLineY = baseHorizontalLineY
            val overlaps = completedBlueprintLines.filter { 
                it.isHorizontal && it.start.y == horizontalLineY &&
                maxOf(it.start.x, it.end.x) > minOf(currentEntry.value.position.x, other.position.x) &&
                minOf(it.start.x, it.end.x) < maxOf(currentEntry.value.position.x, other.position.x)
            }
            
            if (overlaps.isNotEmpty()) {
                horizontalLineY += (overlaps.size * lineSpacingOffset)
            }

            val blueprintLine = BlueprintLine(
                start = Offset(
                    x = currentEntry.value.position.x
                            + if (isLeft) currentEntry.value.size.width else 0f,
                    y = horizontalLineY,
                ),
                end = Offset(
                    x = other.position.x
                            + if (isLeft) 0f else  + other.size.width,
                    y = horizontalLineY,
                )
            )

            val intersectsAnyBlueprintItem = validBlueprintItems
                .filter {
                    it.value != currentEntry.value && it.value != other && !it.value.contains(currentEntry.value)
                }
                .any {
                    blueprintLine.intersects(it.value)
                }

            if (intersectsAnyBlueprintItem) {
                return@lineOfSightEntries
            }

            completedBlueprintLines += blueprintLine
        }

        /* direct lines of sight to parent */

        val parentDirectLeft = validBlueprintItems.values.none { other ->
            currentEntry.value isDirectlyRightOf other
        } && currentEntry.value.parentConnectionConfig
            .shouldConnectParent(Direction.Left)

        if (parentDirectLeft) {

            var horizontalLineY = currentEntry.value.position.y + (currentEntry.value.size.height / 2)

            val overlaps = completedBlueprintLines.filter { 
                it.isHorizontal && it.start.y == horizontalLineY &&
                maxOf(it.start.x, it.end.x) > minOf(0f, currentEntry.value.position.x) &&
                minOf(it.start.x, it.end.x) < maxOf(0f, currentEntry.value.position.x)
            }
            
            if (overlaps.isNotEmpty()) {
                horizontalLineY += (overlaps.size * lineSpacingOffset)
            }

            val blueprintLine = BlueprintLine(
                start = Offset(
                    x = currentEntry.value.position.x,
                    y = horizontalLineY,
                ),
                end = Offset(
                    x = 0f,
                    y = horizontalLineY,
                )
            )

            val intersectsAnyBlueprintItem = validBlueprintItems
                .filter { it.value != currentEntry.value && !it.value.contains(currentEntry.value) }
                .any { blueprintLine.intersects(it.value) }

            if (!intersectsAnyBlueprintItem) {
                completedBlueprintLines += blueprintLine
            }
        }

        val parentDirectTop = validBlueprintItems.values.none { other ->
            currentEntry.value isDirectlyBelow other
        } && currentEntry.value.parentConnectionConfig
            .shouldConnectParent(Direction.Top)

        if (parentDirectTop) {

            var verticalLineX = currentEntry.value.position.x + (currentEntry.value.size.width / 2)

            val overlaps = completedBlueprintLines.filter { 
                it.isVertical && it.start.x == verticalLineX &&
                maxOf(it.start.y, it.end.y) > minOf(0f, currentEntry.value.position.y) &&
                minOf(it.start.y, it.end.y) < maxOf(0f, currentEntry.value.position.y)
            }
            
            if (overlaps.isNotEmpty()) {
                verticalLineX += (overlaps.size * lineSpacingOffset)
            }

            val blueprintLine = BlueprintLine(
                start = Offset(
                    x = verticalLineX,
                    y = currentEntry.value.position.y,
                ),
                end = Offset(
                    x = verticalLineX,
                    y = 0f,
                )
            )

            val intersectsAnyBlueprintItem = validBlueprintItems
                .filter { it.value != currentEntry.value && !it.value.contains(currentEntry.value) }
                .any { blueprintLine.intersects(it.value) }

            if (!intersectsAnyBlueprintItem) {
                completedBlueprintLines += blueprintLine
            }
        }

        val parentDirectRight = validBlueprintItems.values.none { other ->
            currentEntry.value isDirectlyLeftOf other
        } && currentEntry.value.parentConnectionConfig
            .shouldConnectParent(Direction.Right)

        if (parentDirectRight) {

            var horizontalLineY = currentEntry.value.position.y + (currentEntry.value.size.height / 2)

            val overlaps = completedBlueprintLines.filter { 
                it.isHorizontal && it.start.y == horizontalLineY &&
                maxOf(it.start.x, it.end.x) > minOf(currentEntry.value.position.x + currentEntry.value.size.width, screenSize.width) &&
                minOf(it.start.x, it.end.x) < maxOf(currentEntry.value.position.x + currentEntry.value.size.width, screenSize.width)
            }
            
            if (overlaps.isNotEmpty()) {
                horizontalLineY += (overlaps.size * lineSpacingOffset)
            }

            val blueprintLine = BlueprintLine(
                start = Offset(
                    x = currentEntry.value.position.x + currentEntry.value.size.width,
                    y = horizontalLineY,
                ),
                end = Offset(
                    x = screenSize.width,
                    y = horizontalLineY,
                )
            )

            val intersectsAnyBlueprintItem = validBlueprintItems
                .filter { it.value != currentEntry.value && !it.value.contains(currentEntry.value) }
                .any { blueprintLine.intersects(it.value) }

            if (!intersectsAnyBlueprintItem) {
                completedBlueprintLines += blueprintLine
            }
        }

        val parentDirectBottom = validBlueprintItems.values.none { other ->
            currentEntry.value isDirectlyAbove other
        } && currentEntry.value.parentConnectionConfig
            .shouldConnectParent(Direction.Bottom)

        if (parentDirectBottom) {

            var verticalLineX = currentEntry.value.position.x + currentEntry.value.size.width / 2

            val overlaps = completedBlueprintLines.filter { 
                it.isVertical && it.start.x == verticalLineX &&
                maxOf(it.start.y, it.end.y) > minOf(currentEntry.value.position.y + currentEntry.value.size.height, screenSize.height) &&
                minOf(it.start.y, it.end.y) < maxOf(currentEntry.value.position.y + currentEntry.value.size.height, screenSize.height)
            }
            
            if (overlaps.isNotEmpty()) {
                verticalLineX += (overlaps.size * lineSpacingOffset)
            }

            val blueprintLine = BlueprintLine(
                start = Offset(
                    x = verticalLineX,
                    y = currentEntry.value.position.y + currentEntry.value.size.height,
                ),
                end = Offset(
                    x = verticalLineX,
                    y = screenSize.height,
                )
            )

            val intersectsAnyBlueprintItem = validBlueprintItems
                .filter { it.value != currentEntry.value && !it.value.contains(currentEntry.value) }
                .any { blueprintLine.intersects(it.value) }

            if (!intersectsAnyBlueprintItem) {
                completedBlueprintLines += blueprintLine
            }
        }
    }
    return completedBlueprintLines
}
