package com.neongrid.engine

/**
 * The full piece set. Rotations are separate entries; one chirality of the
 * L-tetromino only (keeps the catalog tight, matches genre convention).
 */
object PieceCatalog {

    private var nextId = 0
    private fun piece(
        name: String,
        weight: Float,
        awkward: Boolean = false,
        vararg cells: Pair<Int, Int>,
    ) = Piece(nextId++, name, cells.toList(), weight, awkward)

    private fun rect(name: String, rows: Int, cols: Int, weight: Float): Piece {
        val cells = ArrayList<Pair<Int, Int>>()
        for (r in 0 until rows) for (c in 0 until cols) cells.add(r to c)
        return piece(name, weight, false, *cells.toTypedArray())
    }

    val ALL: List<Piece> = buildList {
        // Single
        add(rect("1x1", 1, 1, 0.6f))
        // Bars
        add(rect("1x2", 1, 2, 1.0f))
        add(rect("2x1", 2, 1, 1.0f))
        add(rect("1x3", 1, 3, 1.0f))
        add(rect("3x1", 3, 1, 1.0f))
        // Long bars
        add(rect("1x4", 1, 4, 0.8f))
        add(rect("4x1", 4, 1, 0.8f))
        add(rect("1x5", 1, 5, 0.5f))
        add(rect("5x1", 5, 1, 0.5f))
        // Squares
        add(rect("2x2", 2, 2, 1.0f))
        add(rect("3x3", 3, 3, 0.45f))
        // Rects
        add(rect("2x3", 2, 3, 0.6f))
        add(rect("3x2", 3, 2, 0.6f))
        // L-trominoes (4 rotations)
        add(piece("L3_0", 0.9f, false, 0 to 0, 1 to 0, 1 to 1))
        add(piece("L3_90", 0.9f, false, 0 to 0, 0 to 1, 1 to 0))
        add(piece("L3_180", 0.9f, false, 0 to 0, 0 to 1, 1 to 1))
        add(piece("L3_270", 0.9f, false, 0 to 1, 1 to 0, 1 to 1))
        // L-tetromino (one chirality, 4 rotations)
        add(piece("L4_0", 0.7f, false, 0 to 0, 1 to 0, 2 to 0, 2 to 1))
        add(piece("L4_90", 0.7f, false, 1 to 0, 1 to 1, 1 to 2, 0 to 2)) // normalized below
        add(piece("L4_180", 0.7f, false, 0 to 0, 0 to 1, 1 to 1, 2 to 1))
        add(piece("L4_270", 0.7f, false, 0 to 0, 0 to 1, 0 to 2, 1 to 0))
        // S + Z (awkward)
        add(piece("S_H", 0.5f, true, 0 to 1, 0 to 2, 1 to 0, 1 to 1))
        add(piece("S_V", 0.5f, true, 0 to 0, 1 to 0, 1 to 1, 2 to 1))
        add(piece("Z_H", 0.5f, true, 0 to 0, 0 to 1, 1 to 1, 1 to 2))
        add(piece("Z_V", 0.5f, true, 0 to 1, 1 to 0, 1 to 1, 2 to 0))
        // T-tetromino (4 rotations)
        add(piece("T_0", 0.6f, false, 0 to 0, 0 to 1, 0 to 2, 1 to 1))
        add(piece("T_90", 0.6f, false, 0 to 0, 1 to 0, 2 to 0, 1 to 1))
        add(piece("T_180", 0.6f, false, 1 to 0, 1 to 1, 1 to 2, 0 to 1))
        add(piece("T_270", 0.6f, false, 0 to 1, 1 to 1, 2 to 1, 1 to 0))
        // Big 3x3 corners, 5 cells (awkward)
        add(piece("C5_0", 0.5f, true, 0 to 0, 0 to 1, 0 to 2, 1 to 0, 2 to 0))
        add(piece("C5_90", 0.5f, true, 0 to 0, 0 to 1, 0 to 2, 1 to 2, 2 to 2))
        add(piece("C5_180", 0.5f, true, 0 to 2, 1 to 2, 2 to 0, 2 to 1, 2 to 2))
        add(piece("C5_270", 0.5f, true, 0 to 0, 1 to 0, 2 to 0, 2 to 1, 2 to 2))
    }

    val byId: Map<Int, Piece> = ALL.associateBy { it.id }

    fun get(id: Int): Piece = byId.getValue(id)

    /** 1xN / Nx1 bars indexed by length, for line-finisher lookups. */
    val horizontalBars: Map<Int, Piece> = ALL.filter { it.height == 1 }.associateBy { it.width }
    val verticalBars: Map<Int, Piece> = ALL.filter { it.width == 1 }.associateBy { it.height }

    /**
     * pieceId -> id of the same shape rotated 90° clockwise. The catalog is
     * closed under rotation (every rotation of every shape is an entry), so
     * the rotate mechanic is just an id swap.
     */
    val rotatedId: Map<Int, Int> = ALL.associate { p ->
        // (r, c) -> (c, H-1-r); result is already normalized.
        val rotated = p.cells.map { (r, c) -> c to (p.height - 1 - r) }.toSet()
        val match = ALL.first { it.cells.toSet() == rotated }
        p.id to match.id
    }

    fun rotated(id: Int): Piece = get(rotatedId.getValue(id))
}
