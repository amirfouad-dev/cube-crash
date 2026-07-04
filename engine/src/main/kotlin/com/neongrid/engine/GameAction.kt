package com.neongrid.engine

sealed interface GameAction {
    /** Place tray piece in [slot] with its top-left cell at (anchorRow, anchorCol). */
    data class Place(val slot: Int, val anchorRow: Int, val anchorCol: Int) : GameAction
    /** Rotate tray piece in [slot] 90° clockwise. */
    data class Rotate(val slot: Int) : GameAction
    /** Timed-mode deadline expired before the active piece was placed. */
    data object TimeUp : GameAction
    data class NewGame(val seed: Long) : GameAction
}
