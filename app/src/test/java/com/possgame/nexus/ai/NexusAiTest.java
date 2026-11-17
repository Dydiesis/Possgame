package com.possgame.nexus.ai;

import com.possgame.nexus.engine.GameEngine;
import com.possgame.nexus.engine.GameState;
import com.possgame.nexus.engine.Move;
import org.junit.Test;

import static org.junit.Assert.*;

/** Each offline difficulty must return a move accepted by the same rules engine. */
public class NexusAiTest {
    @Test public void allDifficultiesReturnLegalMove() {
        GameState turnOne = GameState.newGame();
        GameState aiTurn = GameEngine.apply(turnOne, new Move(4, 0, 3, 0));
        assertEquals(GameState.AI, aiTurn.turn());
        for (int difficulty = 0; difficulty <= 2; difficulty++) {
            Move move = new NexusAi().choose(aiTurn, difficulty);
            assertNotNull("Difficulty " + difficulty, move);
            assertTrue("Illegal AI move at difficulty " + difficulty,
                    GameEngine.isLegal(aiTurn, move));
        }
    }

    @Test public void aiMoveDoesNotMutateInputState() {
        GameState state = GameEngine.apply(GameState.newGame(), new Move(4, 0, 3, 0));
        int[] original = state.copyBoard();
        int moveCount = state.moveCount();
        Move aiMove = new NexusAi().choose(state, 2);
        assertNotNull(aiMove);
        assertArrayEquals(original, state.copyBoard());
        assertEquals(moveCount, state.moveCount());
        assertEquals(GameState.AI, state.turn());
    }

    @Test public void noMoveReturnsNullOnFinishedGame() {
        int[] board = new int[25];
        board[12] = GameState.HUMAN;
        GameState state = GameState.fromPosition(board, GameState.HUMAN);
        GameState finished = GameEngine.resolveTurnStart(state);
        assertTrue(finished.isOver());
        assertNull(new NexusAi().choose(finished, 2));
    }
}
