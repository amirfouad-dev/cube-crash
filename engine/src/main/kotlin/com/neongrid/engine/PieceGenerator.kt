package com.neongrid.engine

/**
 * Adaptive weighted piece generation with a hard fairness invariant: every
 * dealt piece is placeable *after* the piece ahead of it in the queue — the
 * pair [active, next] is always sequence-solvable (with line clears applied)
 * at the moment it is dealt. The player can still trap themselves by placing
 * the active piece badly — that's the skill.
 *
 * Per-difficulty behavior is driven entirely by [GenParams] (see
 * [Difficulty.gen]): higher tiers switch off the congestion bailout and
 * relief valve, deal more awkward shapes, weaken the line-finisher bonus,
 * and (ADVANCED) drop the joint-solvability guarantee to "fits the board at
 * deal time" so a greedy placement can genuinely trap you.
 */
object PieceGenerator {

    const val CONGESTION_FILL = 0.70f
    private const val CONGESTION_WEIGHT_FILL = 0.65f
    private const val RESAMPLE_ATTEMPTS = 6
    private const val SOLVER_NODE_BUDGET = 20_000
    /** Congested deals in a row (one per placement) before the relief valve opens. */
    const val RELIEF_PRESSURE = 5

    // Score bands: index by score thresholds.
    private val BAND_THRESHOLDS = longArrayOf(1_500, 6_000, 20_000)

    fun band(score: Long): Int {
        var b = 0
        for (t in BAND_THRESHOLDS) if (score >= t) b++
        return b
    }

    private fun bandMultiplier(band: Int, piece: Piece): Float {
        val bySize = when (piece.sizeClass) {
            SizeClass.SMALL -> floatArrayOf(1.2f, 1.0f, 0.8f, 0.7f)[band]
            SizeClass.MEDIUM -> 1.0f
            SizeClass.LARGE -> floatArrayOf(0.7f, 1.0f, 1.3f, 1.5f)[band]
        }
        val byAwkward = if (piece.awkward) floatArrayOf(0.5f, 0.8f, 1.1f, 1.4f)[band] else 1.0f
        return bySize * byAwkward
    }

    /**
     * Deal one piece. [precedingPieceId] is the piece that will be placed
     * before the dealt one (the current active piece), or null when dealing
     * the very first piece of a game; the dealt piece is guaranteed (best
     * effort within solver budget) to be placeable after it.
     */
    fun dealNext(
        board: Long,
        rng: GameRng,
        score: Long,
        precedingPieceId: Int?,
        recentPieceIds: List<Int>,
        pressure: Int,
        difficulty: Difficulty = Difficulty.BEGINNER,
    ): TrayPiece {
        val params = difficulty.gen
        val metrics = BoardAnalyzer.analyze(board)
        val band = band(score)
        val weights = FloatArray(PieceCatalog.ALL.size)
        for (piece in PieceCatalog.ALL) {
            var w = piece.baseWeight * bandMultiplier(band, piece)
            // Gentle opening: big pieces stay rare before the first band.
            if (band == 0 && piece.cellCount >= 5) w *= 0.6f
            if (piece.awkward && params.awkwardBoost != 1.0f) w *= params.awkwardBoost
            if (metrics.fillRatio > CONGESTION_WEIGHT_FILL) {
                w *= when {
                    piece.cellCount <= 3 -> params.congestionSmallBoost
                    piece.cellCount >= 6 -> params.congestionBigSuppress
                    else -> 1.0f
                }
            }
            if (metrics.nearFullLines.any { BoardAnalyzer.finishesLine(piece, it) }) {
                w *= params.finisherBonus
            }
            if (piece.id in recentPieceIds) w *= 0.4f
            if (!GameEngine.canPlace(board, piece)) w *= 0.05f
            weights[piece.id] = w
        }
        val preceding = precedingPieceId?.let { PieceCatalog.get(it) }

        var chosen: Piece? = null
        for (attempt in 0 until RESAMPLE_ATTEMPTS) {
            val candidate = roulette(weights, rng) ?: PieceCatalog.ALL[0]
            if (dealSolvable(board, preceding, candidate, params)) {
                chosen = candidate
                break
            }
        }
        if (chosen == null) chosen = bestSolvableFallback(board, preceding, weights, params)

        // Relief valve: sustained congestion → guarantee a line-finisher (or 1x1).
        if (params.reliefValve && pressure >= RELIEF_PRESSURE &&
            !finishesAnyLine(chosen, metrics) &&
            (preceding == null || !finishesAnyLine(preceding, metrics))
        ) {
            val relief = reliefPiece(metrics)
            if (dealSolvable(board, preceding, relief, params)) chosen = relief
        }

        return TrayPiece(chosen.id, (1 + rng.nextInt(GameEngine.COLOR_COUNT)).toByte())
    }

    /**
     * Can [candidate] be placed after [preceding] (in that order, clears
     * applied)? When [params] does not require sequence-solvability the check
     * relaxes to "fits the board as it stands at deal time".
     */
    private fun dealSolvable(
        board: Long,
        preceding: Piece?,
        candidate: Piece,
        params: GenParams,
    ): Boolean {
        if (!params.requireSequenceSolvable || preceding == null) {
            return GameEngine.canPlace(board, candidate)
        }
        return sequencePlaceable(board, preceding, candidate)
    }

    /**
     * True if some placement of [first] (with line clears applied) leaves
     * room for [second]. Order is fixed — this mirrors real gameplay where
     * the active piece must be placed before the preview.
     */
    fun sequencePlaceable(board: Long, first: Piece, second: Piece): Boolean {
        var budget = SOLVER_NODE_BUDGET
        for (mask in first.placements) {
            if ((mask and board) != 0L) continue
            if (budget-- <= 0) return false
            val after = Board.applyClears(board or mask)
            if (GameEngine.canPlace(after, second)) return true
        }
        return false
    }

    private fun roulette(weights: FloatArray, rng: GameRng): Piece? {
        var total = 0f
        for (w in weights) total += w
        if (total <= 0f) return null
        var pick = rng.nextFloat() * total
        for (piece in PieceCatalog.ALL) {
            pick -= weights[piece.id]
            if (pick <= 0f) return piece
        }
        return PieceCatalog.ALL.last()
    }

    /**
     * Fallback: the highest-weight piece that keeps the sequence solvable;
     * failing that, anything that fits the board now; failing that, 1x1.
     */
    private fun bestSolvableFallback(
        board: Long,
        preceding: Piece?,
        weights: FloatArray,
        params: GenParams,
    ): Piece {
        val ranked = PieceCatalog.ALL.sortedByDescending { weights[it.id] }
        for (piece in ranked) {
            if (dealSolvable(board, preceding, piece, params)) return piece
        }
        return ranked.firstOrNull { GameEngine.canPlace(board, it) } ?: PieceCatalog.ALL[0]
    }

    private fun finishesAnyLine(piece: Piece, metrics: BoardMetrics): Boolean =
        metrics.nearFullLines.any { BoardAnalyzer.finishesLine(piece, it) }

    private fun reliefPiece(metrics: BoardMetrics): Piece {
        val target = metrics.nearFullLines.maxByOrNull { it.filled }
        return target?.let { line ->
            BoardAnalyzer.contiguousGapLength(line)?.let { len ->
                if (line.isRow) PieceCatalog.horizontalBars[len] else PieceCatalog.verticalBars[len]
            }
        } ?: PieceCatalog.ALL[0] // 1x1
    }

    /**
     * DFS: can all pieces be placed in some order at some positions, with
     * line clears applied after each placement (matching real gameplay)?
     * Memoized per remaining-set; bounded by a node budget.
     */
    fun traySolvable(board: Long, pieces: List<Piece>): Boolean {
        val visited = Array(1 shl pieces.size) { HashSet<Long>() }
        var budget = SOLVER_NODE_BUDGET

        fun dfs(b: Long, remaining: Int): Boolean {
            if (remaining == 0) return true
            if (budget-- <= 0) return false
            if (!visited[remaining].add(b)) return false
            for (i in pieces.indices) {
                if ((remaining ushr i) and 1 == 0) continue
                val piece = pieces[i]
                for (mask in piece.placements) {
                    if ((mask and b) == 0L) {
                        val after = Board.applyClears(b or mask)
                        if (dfs(after, remaining and (1 shl i).inv())) return true
                    }
                }
            }
            return false
        }
        return dfs(board, (1 shl pieces.size) - 1)
    }
}
