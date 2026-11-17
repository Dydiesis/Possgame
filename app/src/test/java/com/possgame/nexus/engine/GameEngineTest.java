package com.possgame.nexus.engine;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class GameEngineTest {
    @Test public void openingHasFourPiecesAndLegalOrthogonalMove() {
        GameState game = GameState.newGame();
        assertEquals(GameState.HUMAN, game.turn());
        assertEquals(GameState.HUMAN, game.pieceAt(4, 0));
        assertEquals(GameState.AI, game.pieceAt(0, 0));
        assertTrue(GameEngine.isLegal(game, new Move(4, 0, 3, 0)));
        assertFalse(GameEngine.isLegal(game, new Move(4, 0, 3, 1)));
        assertFalse(GameEngine.isLegal(game, new Move(4, 0, 4, 1)));
        assertFalse(GameEngine.isLegal(game, new Move(4, 0, 2, 0)));
    }

    @Test public void illegalMoveDoesNotChangeBoardOrTurn() {
        GameState game = GameState.newGame();
        try { GameEngine.apply(game, new Move(4, 0, 3, 1)); fail("Invalid move accepted"); }
        catch (IllegalArgumentException expected) { }
        assertEquals(GameState.HUMAN, game.turn());
        assertEquals(0, game.moveCount());
        assertEquals(GameState.HUMAN, game.pieceAt(4, 0));
    }

    @Test public void captureRemovesEnemy() {
        int[] board = new int[25];
        board[2 * 5 + 2] = GameState.HUMAN;
        board[1 * 5 + 2] = GameState.AI;
        board[0] = GameState.AI;
        GameState next = GameEngine.apply(GameState.fromPosition(board, GameState.HUMAN),
                                          new Move(2, 2, 1, 2));
        assertEquals(GameState.HUMAN, next.pieceAt(1, 2));
        assertEquals(0, next.pieceAt(2, 2));
        assertEquals(GameState.AI, next.turn());
    }

    private GameState oneMoveFromThreePoints() {
        int[] board = new int[25];
        board[1 * 5 + 2] = GameState.HUMAN;
        board[2 * 5 + 2] = GameState.HUMAN;
        board[4 * 5 + 2] = GameState.HUMAN;
        board[0] = GameState.AI;
        return GameEngine.apply(GameState.fromPosition(board, GameState.HUMAN),
                                new Move(4, 2, 3, 2));
    }

    @Test public void occupyingThreePointsIsNotImmediateWin() {
        GameState next = oneMoveFromThreePoints();
        assertTrue(GameEngine.controlsAll(next.copyBoard(), GameState.HUMAN));
        assertEquals(GameState.Outcome.ONGOING, next.outcome());
        assertEquals(GameState.AI, next.turn());
    }

    @Test public void winOnlyAfterOpponentResponse() {
        GameState afterOccupation = oneMoveFromThreePoints();
        GameState afterResponse = GameEngine.apply(afterOccupation, new Move(0, 0, 0, 1));
        assertEquals(GameState.Outcome.HUMAN_WIN, afterResponse.outcome());
        assertEquals("Tre punti mantenuti", afterResponse.reason());
    }

    @Test public void opponentCanBreakControl() {
        int[] board = new int[25];
        board[1 * 5 + 2] = GameState.HUMAN;
        board[2 * 5 + 2] = GameState.HUMAN;
        board[4 * 5 + 2] = GameState.HUMAN;
        board[2 * 5 + 3] = GameState.AI;
        GameState afterOccupation = GameEngine.apply(
            GameState.fromPosition(board, GameState.HUMAN), new Move(4, 2, 3, 2));
        GameState broken = GameEngine.apply(afterOccupation, new Move(2, 3, 2, 2));
        assertEquals(GameState.Outcome.ONGOING, broken.outcome());
        assertEquals(GameState.HUMAN, broken.turn());
        assertFalse(GameEngine.controlsAll(broken.copyBoard(), GameState.HUMAN));
    }

    @Test public void eliminatingLastEnemyWinsImmediately() {
        int[] board = new int[25];
        board[2 * 5 + 2] = GameState.HUMAN;
        board[2 * 5 + 3] = GameState.AI;
        GameState win = GameEngine.apply(GameState.fromPosition(board, GameState.HUMAN),
                                         new Move(2, 2, 2, 3));
        assertEquals(GameState.Outcome.HUMAN_WIN, win.outcome());
    }

    @Test public void threefoldRepetitionIsDraw() {
        int[] board = new int[25];
        board[4 * 5] = GameState.HUMAN;
        board[4] = GameState.AI;
        GameState state = GameState.fromPosition(board, GameState.HUMAN);
        for (int i = 0; i < 2; i++) {
            state = GameEngine.apply(state, new Move(4, 0, 3, 0));
            state = GameEngine.apply(state, new Move(0, 4, 1, 4));
            state = GameEngine.apply(state, new Move(3, 0, 4, 0));
            state = GameEngine.apply(state, new Move(1, 4, 0, 4));
        }
        assertEquals(GameState.Outcome.DRAW, state.outcome());
    }

    @Test public void hundredMovesAreDraw() {
        int[] board = new int[25];
        board[4 * 5] = GameState.HUMAN;
        board[4] = GameState.AI;
        Map<String, Integer> visits = new HashMap<>();
        visits.put(GameState.positionKey(board, GameState.HUMAN), 1);
        GameState state = new GameState(board, GameState.HUMAN, 99,
                GameState.Outcome.ONGOING, "", visits);
        assertEquals(GameState.Outcome.DRAW,
                GameEngine.apply(state, new Move(4, 0, 3, 0)).outcome());
    }
}
