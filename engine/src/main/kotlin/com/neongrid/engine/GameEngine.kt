package com.neongrid.engine

data class Transition(val state: GameState, val events: List<GameEvent>)

object GameEngine {

    const val COLOR_COUNT = 5

    fun newGame(seed: Long, difficulty: Difficulty = Difficulty.BEGINNER): Transition {
        val rng = GameRng(seed)
        val current = PieceGenerator.dealNext(
            board = 0L, rng = rng, score = 0,
            precedingPieceId = null, recentPieceIds = emptyList(), pressure = 0,
            difficulty = difficulty,
        )
        val next = PieceGenerator.dealNext(
            board = 0L, rng = rng, score = 0,
            precedingPieceId = current.pieceId, recentPieceIds = listOf(current.pieceId), pressure = 0,
            difficulty = difficulty,
        )
        val tray = listOf<TrayPiece?>(current, next)
        val state = GameState(
            board = 0L,
            cellColors = ByteArray(Board.CELLS),
            tray = tray,
            score = 0,
            comboStreak = 0,
            placementsSinceClear = 0,
            piecesPlaced = 0,
            linesClearedTotal = 0,
            maxCombo = 0,
            rngState = rng.state,
            pressure = 0,
            isGameOver = false,
        )
        return Transition(state, listOf(GameEvent.TrayRefilled(tray.filterNotNull())))
    }

    fun reduce(
        state: GameState,
        action: GameAction,
        difficulty: Difficulty = Difficulty.BEGINNER,
    ): Transition = when (action) {
        is GameAction.NewGame -> newGame(action.seed, difficulty)
        is GameAction.Place -> place(state, action, difficulty)
        is GameAction.Rotate -> rotate(state, action)
        is GameAction.TimeUp -> timeUp(state)
    }

    private fun timeUp(state: GameState): Transition {
        if (state.isGameOver) return Transition(state, emptyList())
        return Transition(state.copy(isGameOver = true), listOf(GameEvent.GameOver))
    }

    fun canPlace(board: Long, piece: Piece): Boolean =
        piece.placements.any { (it and board) == 0L }

    /** True if the piece fits in ANY of its four rotations. */
    fun canPlaceAnyRotation(board: Long, pieceId: Int): Boolean {
        var id = pieceId
        repeat(4) {
            if (canPlace(board, PieceCatalog.get(id))) return true
            id = PieceCatalog.rotatedId.getValue(id)
        }
        return false
    }

    private fun rotate(state: GameState, action: GameAction.Rotate): Transition {
        if (state.isGameOver) return Transition(state, emptyList())
        if (action.slot != GameState.ACTIVE_SLOT) return Transition(state, emptyList())
        val trayPiece = state.tray.getOrNull(action.slot) ?: return Transition(state, emptyList())
        val newId = PieceCatalog.rotatedId.getValue(trayPiece.pieceId)
        if (newId == trayPiece.pieceId) return Transition(state, emptyList()) // symmetric piece
        val tray = state.tray.toMutableList().also { it[action.slot] = trayPiece.copy(pieceId = newId) }
        return Transition(
            state.copy(tray = tray),
            listOf(GameEvent.PieceRotated(action.slot, newId)),
        )
    }

    fun isValidAt(board: Long, piece: Piece, row: Int, col: Int): Boolean =
        piece.inBounds(row, col) && Board.fits(board, piece.maskAt(row, col))

    /** Line masks (rows+cols) that would complete if [placement] were OR'd in. */
    fun wouldClear(board: Long, placement: Long): Long {
        val after = board or placement
        var mask = 0L
        for (m in Board.ROW_MASKS) if (Board.isFull(after, m)) mask = mask or m
        for (m in Board.COL_MASKS) if (Board.isFull(after, m)) mask = mask or m
        return mask
    }

    private fun place(
        state: GameState,
        action: GameAction.Place,
        difficulty: Difficulty,
    ): Transition {
        if (state.isGameOver) return Transition(state, emptyList())
        if (action.slot != GameState.ACTIVE_SLOT) return Transition(state, emptyList())
        val trayPiece = state.tray.getOrNull(action.slot) ?: return Transition(state, emptyList())
        val piece = PieceCatalog.get(trayPiece.pieceId)
        if (!isValidAt(state.board, piece, action.anchorRow, action.anchorCol)) {
            return Transition(state, emptyList())
        }

        val events = ArrayList<GameEvent>(6)
        val placement = piece.maskAt(action.anchorRow, action.anchorCol)
        var board = state.board or placement
        val colors = state.cellColors.copyOf()
        for (i in 0 until Board.CELLS) {
            if ((placement ushr i) and 1L == 1L) colors[i] = trayPiece.colorId
        }
        events.add(GameEvent.PiecePlaced(placement, trayPiece.colorId))

        var score = state.score + piece.cellCount
        events.add(GameEvent.ScoreAwarded(piece.cellCount.toLong(), placement))

        // Clear detection on the post-placement board.
        val fullRows = Board.fullRows(board)
        val fullCols = Board.fullCols(board)
        val lines = fullRows.size + fullCols.size

        var comboStreak = state.comboStreak
        var placementsSinceClear = state.placementsSinceClear
        var linesClearedTotal = state.linesClearedTotal

        if (lines > 0) {
            var clearedMask = 0L
            for (r in fullRows) clearedMask = clearedMask or Board.ROW_MASKS[r]
            for (c in fullCols) clearedMask = clearedMask or Board.COL_MASKS[c]
            val colorSnapshot = colors.copyOf()
            board = board and clearedMask.inv()
            for (i in 0 until Board.CELLS) {
                if ((clearedMask ushr i) and 1L == 1L) colors[i] = 0
            }

            comboStreak += 1
            placementsSinceClear = 0
            linesClearedTotal += lines
            val points = Scoring.clearPoints(lines, comboStreak)
            score += points
            events.add(
                GameEvent.LinesCleared(
                    fullRows, fullCols, clearedMask, comboStreak, points,
                    colorSnapshot = colorSnapshot,
                    originCell = java.lang.Long.numberOfTrailingZeros(placement),
                ),
            )
            events.add(GameEvent.ComboChanged(comboStreak))
            events.add(GameEvent.ScoreAwarded(points, clearedMask))

            if (board == 0L) {
                score += Scoring.ALL_CLEAR_BONUS
                events.add(GameEvent.AllClear)
                events.add(GameEvent.ScoreAwarded(Scoring.ALL_CLEAR_BONUS, 0L))
            }
        } else {
            placementsSinceClear += 1
            if (comboStreak > 0 && placementsSinceClear >= Scoring.STREAK_GRACE) {
                comboStreak = 0
                events.add(GameEvent.ComboChanged(0))
            }
        }

        // The preview becomes the active piece; deal a fresh preview behind it.
        val nextUp = state.tray.getOrNull(GameState.NEXT_SLOT)
        val rng = GameRng(state.rngState)
        val pressure =
            if (Board.fillRatio(board) > PieceGenerator.CONGESTION_FILL) state.pressure + 1 else 0
        val dealt = PieceGenerator.dealNext(
            board = board,
            rng = rng,
            score = score,
            precedingPieceId = nextUp?.pieceId,
            recentPieceIds = listOfNotNull(trayPiece.pieceId, nextUp?.pieceId),
            pressure = pressure,
            difficulty = difficulty,
        )
        val tray = listOf(nextUp, dealt)
        val rngState = rng.state
        events.add(GameEvent.TrayRefilled(listOf(dealt)))

        // Game over: the new active piece fits in no rotation.
        val isGameOver = nextUp == null || !canPlaceAnyRotation(board, nextUp.pieceId)
        if (isGameOver) events.add(GameEvent.GameOver)

        val newState = state.copy(
            board = board,
            cellColors = colors,
            tray = tray,
            score = score,
            comboStreak = comboStreak,
            placementsSinceClear = placementsSinceClear,
            piecesPlaced = state.piecesPlaced + 1,
            linesClearedTotal = linesClearedTotal,
            maxCombo = maxOf(state.maxCombo, comboStreak),
            rngState = rngState,
            pressure = pressure,
            isGameOver = isGameOver,
        )
        return Transition(newState, events)
    }
}
