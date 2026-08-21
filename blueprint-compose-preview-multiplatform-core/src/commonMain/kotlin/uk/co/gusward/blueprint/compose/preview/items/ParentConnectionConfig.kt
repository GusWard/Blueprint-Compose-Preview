package uk.co.gusward.blueprint.compose.preview.items

import uk.co.gusward.blueprint.compose.preview.constants.Direction

/**
 * configures which sides of a BlueprintItem should draw connection lines to
 * its parent layout where possible.
 *
 * Blueprint items will never draw lines through other blueprint items regardless of this setting.
 */
sealed class ParentConnectionConfig {

    abstract fun shouldConnectParent(dir: Direction): Boolean
}

/**
 * draw all possible lines.
 */
data object WherePossible : ParentConnectionConfig() {
    override fun shouldConnectParent(dir: Direction) = true
}

/**
 * draw no lines.
 */
data object None : ParentConnectionConfig() {
    override fun shouldConnectParent(dir: Direction) = false
}

/**
 * configure specific sides, all sides default to WherePossible.
 */
data class Specific(
    val left: ParentConnectionConfig = WherePossible,
    val top: ParentConnectionConfig = WherePossible,
    val right: ParentConnectionConfig = WherePossible,
    val bottom: ParentConnectionConfig = WherePossible,
) : ParentConnectionConfig() {
    override fun shouldConnectParent(dir: Direction): Boolean = when(dir) {
        Direction.Left -> left == WherePossible
        Direction.Top -> top == WherePossible
        Direction.Right -> right == WherePossible
        Direction.Bottom -> bottom == WherePossible
    }
}
