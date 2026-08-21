package uk.co.gusward.blueprint.compose.preview.items

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

data class BlueprintItemData(
    val id: String,
    val label: String,
    val position: Offset,
    val size: Size,
    val parentConnectionConfig: ParentConnectionConfig,
) {

    infix fun isDirectlyAbove(other: BlueprintItemData): Boolean {

        val bottom = position.y + size.height
        val centerBottom = position.x + size.width / 2
        val otherRight = other.position.x + other.size.width

        val otherIsBelow = other.position.y > bottom
        val otherInLineOfSight = centerBottom >= other.position.x && centerBottom <= otherRight

        return otherIsBelow && otherInLineOfSight
    }

    infix fun isDirectlyLeftOf(other: BlueprintItemData): Boolean {

        val right = position.x + size.width
        val centerVertical = position.y + size.height / 2
        val otherBottom = other.position.y + other.size.height

        val otherIsToTheRight = other.position.x > right
        val otherInLineOfSight = centerVertical >= other.position.y && centerVertical <= otherBottom

        return otherIsToTheRight && otherInLineOfSight
    }

    infix fun isDirectlyRightOf(other: BlueprintItemData): Boolean {

        val otherRight = other.position.x + other.size.width
        val centerVertical = position.y + (size.height / 2)
        val otherBottom = other.position.y + other.size.height

        val otherIsToTheLeft = otherRight < position.x
        val otherInLineOfSight = centerVertical >= other.position.y && centerVertical <= otherBottom

        return otherIsToTheLeft && otherInLineOfSight
    }

    infix fun isDirectlyBelow(other: BlueprintItemData): Boolean {

        val otherBottom = other.position.y + other.size.height
        val centerBottom = position.x + (size.width / 2)
        val otherRight = other.position.x + other.size.width

        val otherIsAbove = position.y > otherBottom
        val otherInLineOfSight = centerBottom >= other.position.x && centerBottom <= otherRight

        return otherIsAbove && otherInLineOfSight
    }

    fun contains(other: BlueprintItemData): Boolean {
        return other.position.x >= position.x &&
                other.position.y >= position.y &&
                (other.position.x + other.size.width) <= (position.x + size.width) &&
                (other.position.y + other.size.height) <= (position.y + size.height)
    }
}
