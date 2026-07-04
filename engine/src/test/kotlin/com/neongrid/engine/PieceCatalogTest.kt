package com.neongrid.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PieceCatalogTest {

    @Test
    fun `every piece has normalized offsets`() {
        for (piece in PieceCatalog.ALL) {
            assertEquals(0, piece.cells.minOf { it.first }, piece.name)
            assertEquals(0, piece.cells.minOf { it.second }, piece.name)
        }
    }

    @Test
    fun `mask bit count matches cell count`() {
        for (piece in PieceCatalog.ALL) {
            assertEquals(piece.cellCount, java.lang.Long.bitCount(piece.baseMask), piece.name)
        }
    }

    @Test
    fun `placement count is (9-w)(9-h)`() {
        for (piece in PieceCatalog.ALL) {
            val expected = (9 - piece.width) * (9 - piece.height)
            assertEquals(expected, piece.placements.size, piece.name)
        }
    }

    @Test
    fun `no placement mask wraps across columns`() {
        // The classic bitboard bug: shifting a piece to the right edge wraps
        // its cells onto the next row. Verify every placement stays inside
        // the columns implied by its anchor.
        for (piece in PieceCatalog.ALL) {
            for (k in piece.placements.indices) {
                val anchor = piece.placementAnchors[k]
                val row = anchor / 8
                val col = anchor % 8
                val mask = piece.placements[k]
                assertEquals(piece.cellCount, java.lang.Long.bitCount(mask), piece.name)
                for (i in 0 until 64) {
                    if ((mask ushr i) and 1L == 1L) {
                        val r = i / 8
                        val c = i % 8
                        assertTrue(r in row until row + piece.height, "${piece.name} row wrap at anchor ($row,$col)")
                        assertTrue(c in col until col + piece.width, "${piece.name} col wrap at anchor ($row,$col)")
                    }
                }
            }
        }
    }

    @Test
    fun `ids are unique and sequential`() {
        val ids = PieceCatalog.ALL.map { it.id }
        assertEquals(ids.toSet().size, ids.size)
        assertEquals((0 until PieceCatalog.ALL.size).toList(), ids.sorted())
    }

    @Test
    fun `catalog is closed under rotation and four rotations return to self`() {
        for (piece in PieceCatalog.ALL) {
            var id = piece.id
            repeat(4) {
                id = PieceCatalog.rotatedId.getValue(id) // getValue throws if not closed
            }
            assertEquals(piece.id, id, "${piece.name}: 4 rotations should be identity")
            // Rotation preserves cell count and swaps dimensions.
            val rotated = PieceCatalog.rotated(piece.id)
            assertEquals(piece.cellCount, rotated.cellCount, piece.name)
            assertEquals(piece.width, rotated.height, piece.name)
            assertEquals(piece.height, rotated.width, piece.name)
        }
    }

    @Test
    fun `bar lookup tables find line finishers`() {
        assertEquals(3, PieceCatalog.horizontalBars.getValue(3).width)
        assertEquals(4, PieceCatalog.verticalBars.getValue(4).height)
        assertEquals(1, PieceCatalog.horizontalBars.getValue(1).cellCount) // 1x1 in both tables
    }
}
