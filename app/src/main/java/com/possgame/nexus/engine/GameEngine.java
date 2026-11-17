package com.possgame.nexus.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GameEngine {
    private GameEngine() {}

    public static final int[][] POINTS = {{1, 2}, {2, 2}, {3, 2}};
    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    public static List<Move> legalMoves(GameState state) {
        if (state.isOver()) return new ArrayList<>();
        return legalMovesFor(state.copyBoard(), state.turn());
    }

    public static List<Move> legalMovesFor(int[] board, int player) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 5; r++) for (int c = 0; c < 5; c++) {
            if (board[r * 5 + c] != player) continue;
            for (int[] d : DIRECTIONS) {
                int nr = r + d[0], nc = c + d[1];
                if (inside(nr, nc) && board[nr * 5 + nc] != player) {
                    moves.add(new Move(r, c, nr, nc));
                }
            }
        }
        return moves;
    }

    private static boolean inside(int row, int col) {
        return row >= 0 && row < 5 && col >= 0 && col < 5;
    }

    public static boolean isLegal(GameState state, Move move) {
        if (state.isOver() || move == null
            || !inside(move.fromRow, move.fromCol)
            || !inside(move.toRow, move.toCol)) return false;
        if (Math.abs(move.fromRow - move.toRow) + Math.abs(move.fromCol - move.toCol) != 1) return false;
        return state.pieceAt(move.fromRow, move.fromCol) == state.turn()
            && state.pieceAt(move.toRow, move.toCol) != state.turn();
    }

    public static GameState apply(GameState state, Move move) {
        if (!isLegal(state, move)) throw new IllegalArgumentException("Illegal move: " + move);
        int[] board = state.copyBoard();
        board[move.toRow * 5 + move.toCol] = state.turn();
        board[move.fromRow * 5 + move.fromCol] = 0;
        int next = -state.turn();

        Map<String, Integer> history = new HashMap<>(state.visits());
        String key = GameState.positionKey(board, next);
        history.put(key, history.getOrDefault(key, 0) + 1);
        GameState updated = new GameState(board, next, state.moveCount() + 1,
                                          GameState.Outcome.ONGOING, "", history);
        return resolveTurnStart(updated);
    }

    /**
     * Called when the new player is about to act, never immediately after
     * that same player occupied the three points. This grants a response turn.
     * Priority: elimination > delayed point control > no legal moves > draw.
     */
    public static GameState resolveTurnStart(GameState state) {
        if (state.isOver()) return state;
        int[] board = state.copyBoard();
        int humans = 0, ais = 0;
        for (int piece : board) {
            if (piece == GameState.HUMAN) humans++;
            if (piece == GameState.AI) ais++;
        }
        if (humans == 0 && ais == 0) return state.withOutcome(GameState.Outcome.DRAW, "Nessuna pedina");
        if (humans == 0) return state.withOutcome(GameState.Outcome.AI_WIN, "Eliminazione");
        if (ais == 0) return state.withOutcome(GameState.Outcome.HUMAN_WIN, "Eliminazione");

        if (controlsAll(board, state.turn())) {
            return state.withOutcome(winner(state.turn()), "Tre punti mantenuti");
        }
        if (legalMovesFor(board, state.turn()).isEmpty()) {
            return state.withOutcome(winner(-state.turn()), "Blocco: nessuna mossa legale");
        }
        if (state.visits().getOrDefault(GameState.positionKey(board, state.turn()), 0) >= 3) {
            return state.withOutcome(GameState.Outcome.DRAW, "Posizione ripetuta tre volte");
        }
        if (state.moveCount() >= 100) {
            return state.withOutcome(GameState.Outcome.DRAW, "Limite di 100 mosse");
        }
        return state;
    }

    public static boolean controlsAll(int[] board, int player) {
        for (int[] point : POINTS) {
            if (board[point[0] * 5 + point[1]] != player) return false;
        }
        return true;
    }

    public static boolean isPoint(int row, int col) {
        for (int[] point : POINTS) {
            if (point[0] == row && point[1] == col) return true;
        }
        return false;
    }

    private static GameState.Outcome winner(int side) {
        return side == GameState.HUMAN ? GameState.Outcome.HUMAN_WIN : GameState.Outcome.AI_WIN;
    }
}
