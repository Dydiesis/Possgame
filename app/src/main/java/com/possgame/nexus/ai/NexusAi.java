package com.possgame.nexus.ai;

import com.possgame.nexus.engine.GameEngine;
import com.possgame.nexus.engine.GameState;
import com.possgame.nexus.engine.Move;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Offline opponent: random, short search, or deeper alpha-beta search. */
public final class NexusAi {
    private static final int WIN_SCORE = 100000;
    private final Random random = new Random();
    private int visitedNodes;
    private int nodeLimit;

    public Move choose(GameState game, int level) {
        List<Move> legal = GameEngine.legalMoves(game);
        if (legal.isEmpty()) return null;
        if (level <= 0) return legal.get(random.nextInt(legal.size()));
        nodeLimit = level == 1 ? 3000 : 25000;
        int depth = level == 1 ? 2 : 4;
        visitedNodes = 0;
        orderMoves(legal, game);

        Move best = legal.get(0);
        int bestScore = Integer.MIN_VALUE;
        int equal = 0;
        for (Move move : legal) {
            GameState next = GameEngine.apply(game, move);
            int score = search(next, depth - 1, -WIN_SCORE * 2, WIN_SCORE * 2);
            if (score > bestScore) {
                bestScore = score;
                best = move;
                equal = 1;
            } else if (score == bestScore && random.nextInt(++equal) == 0) {
                best = move;
            }
        }
        return best;
    }

    private int search(GameState game, int depth, int alpha, int beta) {
        visitedNodes++;
        if (game.isOver() || depth <= 0 || visitedNodes >= nodeLimit
                || Thread.currentThread().isInterrupted()) {
            return score(game);
        }
        List<Move> moves = GameEngine.legalMoves(game);
        if (moves.isEmpty()) return score(game);
        orderMoves(moves, game);
        boolean maximizer = game.turn() == GameState.AI;
        int best = maximizer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Move move : moves) {
            GameState after = GameEngine.apply(game, move);
            int value = search(after, depth - 1, alpha, beta);
            if (maximizer) {
                best = Math.max(best, value);
                alpha = Math.max(alpha, best);
            } else {
                best = Math.min(best, value);
                beta = Math.min(beta, best);
            }
            if (beta <= alpha || visitedNodes >= nodeLimit
                    || Thread.currentThread().isInterrupted()) break;
        }
        return best;
    }

    private static void orderMoves(List<Move> moves, GameState game) {
        moves.sort(Comparator.comparingInt((Move move) -> {
            int target = game.pieceAt(move.toRow, move.toCol);
            int value = target == -game.turn() ? 100 : 0;
            if (GameEngine.isPoint(move.toRow, move.toCol)) value += 40;
            return value;
        }).reversed());
    }

    private static int score(GameState game) {
        switch (game.outcome()) {
            case AI_WIN: return WIN_SCORE - game.moveCount();
            case HUMAN_WIN: return -WIN_SCORE + game.moveCount();
            case DRAW: return 0;
            default: break;
        }
        int[] board = game.copyBoard();
        int pieces = 0, objectives = 0, proximity = 0;
        for (int cell : board) pieces += cell;
        for (int[] point : GameEngine.POINTS) {
            int p = point[0] * 5 + point[1];
            objectives += board[p];
            int aiDistance = 10;
            int humanDistance = 10;
            for (int pos = 0; pos < 25; pos++) {
                int dist = Math.abs(pos / 5 - point[0]) + Math.abs(pos % 5 - point[1]);
                if (board[pos] == GameState.AI) aiDistance = Math.min(aiDistance, dist);
                if (board[pos] == GameState.HUMAN) humanDistance = Math.min(humanDistance, dist);
            }
            proximity += humanDistance - aiDistance;
        }
        // Board uses AI=-1, so negate counts to favor the AI.
        return -pieces * 70 - objectives * 110 + proximity * 6;
    }
}
