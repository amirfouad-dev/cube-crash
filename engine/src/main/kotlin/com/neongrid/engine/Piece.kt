package com.neongrid.engine

enum class SizeClass { SMALL, MEDIUM, LARGE }

/**
 * A puzzle piece. Rotations are distinct catalog entries (there is no rotate
 * mechanic). All legal placements on an empty board are precomputed as masks.
 */
class Piece(
    val id: Int,
    val name: String,
    /** Normalized (row, col) offsets; min row and min col are 0. */
    val cells: List<Pair<Int, Int>>,
    val baseWeight: Float,
    val awkward: Boolean = false,
) {
    val height: Int = cells.maxOf { it.first } + 1
    val width: Int = cells.maxOf { it.second } + 1
    val cellCount: Int = cells.size

    val sizeClass: SizeClass = when {
        cellCount <= 2 -> SizeClass.SMALL
        cellCount <= 4 -> SizeClass.MEDIUM
        else -> SizeClass.LARGE
    }

    /** Mask with the piece anchored at (0,0). */
    val baseMask: Long = cells.fold(0L) { m, (r, c) -> m or Board.bit(r, c) }

    /** Anchor positions (row * 8 + col) for every in-bounds placement. */
    val placementAnchors: IntArray
    /** Placement masks parallel to [placementAnchors]. */
    val placements: LongArray

    init {
        val anchors = ArrayList<Int>()
        val masks = ArrayList<Long>()
        for (r in 0..(Board.SIZE - height)) {
            for (c in 0..(Board.SIZE - width)) {
                anchors.add(r * Board.SIZE + c)
                masks.add(baseMask shl (r * Board.SIZE + c))
            }
        }
        placementAnchors = anchors.toIntArray()
        placements = masks.toLongArray()
    }

    /** Mask at anchor (row, col). Only valid when 0 <= row <= 8-height, 0 <= col <= 8-width. */
    fun maskAt(row: Int, col: Int): Long = baseMask shl (row * Board.SIZE + col)

    fun inBounds(row: Int, col: Int): Boolean =
        row >= 0 && col >= 0 && row + height <= Board.SIZE && col + width <= Board.SIZE
}
