package com.possgame.nexus.engine;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class GameState {
    public static final int SIZE = 5;
    public static final int EMPTY = 0;
    public static final int HUMAN = 1;
    public static final int AI = -1;

    public enum Outcome { ONGOING, HUMAN_WIN, AI_WIN, DRAW }

    private final int[] board;
    private final int turn;
    private final int moveCount;
    private final Outcome outcome;
    private final String reason;
    private final Map<String, Integer> visits;

    public GameState(int[] board, int turn, int moveCount, Outcome outcome,
                     String reason, Map<String, Integer> visits) {
        if (board == null || board.length != SIZE * SIZE) throw new IllegalArgumentException("Invalid board");
        for (int cell : board) {
            if (cell != EMPTY && cell != HUMAN && cell != AI) throw new IllegalArgumentException("Invalid piece");
        }
        if (turn != HUMAN && turn != AI || moveCount < 0 || outcome == null) {
            throw new IllegalArgumentException("Invalid game state");
        }
        this.board = Arrays.copyOf(board, board.length);
        this.turn = turn;
        this.moveCount = moveCount;
        this.outcome = outcome;
        this.reason = reason == null ? "" : reason;
        this.visits = visits == null ? new HashMap<String, Integer>() : new HashMap<>(visits);
    }

    public static GameState newGame() {
        int[] b = new int[25];
        for (int col : new int[] {0, 1, 3, 4}) {
            b[col] = AI;
            b[20 + col] = HUMAN;
        }
        return fromPosition(b, HUMAN);
    }

    public static GameState fromPosition(int[] board, int turn) {
        Map<String, Integer> history = new HashMap<>();
        history.put(positionKey(board, turn), 1);
        return new GameState(board, turn, 0, Outcome.ONGOING, "", history);
    }

    public static String positionKey(int[] board, int turn) {
        StringBuilder b = new StringBuilder(27);
        for (int piece : board) b.append((char) ('1' + piece));
        b.append(':').append(turn == HUMAN ? 'H' : 'A');
        return b.toString();
    }

    public int pieceAt(int row, int col) {
        return board[row * SIZE + col];
    }

    public int[] copyBoard() { return Arrays.copyOf(board, board.length); }
    public int turn() { return turn; }
    public int moveCount() { return moveCount; }
    public Outcome outcome() { return outcome; }
    public String reason() { return reason; }
    public boolean isOver() { return outcome != Outcome.ONGOING; }
    public Map<String, Integer> visits() { return Collections.unmodifiableMap(visits); }

    GameState withOutcome(Outcome updated, String why) {
        return new GameState(board, turn, moveCount, updated, why, visits);
    }
}
