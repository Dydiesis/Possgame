package com.possgame.nexus.storage;

import android.content.Context;
import android.content.SharedPreferences;
import com.possgame.nexus.engine.GameState;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Local-only game persistence. Rules/history are preserved across app restarts. */
public final class GameStorage {
    private static final String FILE = "nexus_local_game";
    private static final String KEY = "game_v1";

    private GameStorage() {}

    public static void save(Context context, GameState game, int difficulty) {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            JSONArray board = new JSONArray();
            for (int cell : game.copyBoard()) board.put(cell);
            root.put("board", board);
            root.put("turn", game.turn());
            root.put("moveCount", game.moveCount());
            root.put("outcome", game.outcome().name());
            root.put("reason", game.reason());
            root.put("difficulty", difficulty);

            JSONObject history = new JSONObject();
            for (Map.Entry<String, Integer> item : game.visits().entrySet()) {
                history.put(item.getKey(), item.getValue());
            }
            root.put("visits", history);
            preferences(context).edit().putString(KEY, root.toString()).apply();
        } catch (JSONException ignored) {
            // No partial save is written.
        }
    }

    public static LoadedGame load(Context context) {
        String data = preferences(context).getString(KEY, null);
        if (data == null) return null;
        try {
            JSONObject root = new JSONObject(data);
            if (root.getInt("version") != 1) return null;
            JSONArray storedBoard = root.getJSONArray("board");
            if (storedBoard.length() != 25) return null;
            int[] board = new int[25];
            for (int i = 0; i < 25; i++) board[i] = storedBoard.getInt(i);
            JSONObject savedVisits = root.getJSONObject("visits");
            Map<String, Integer> visits = new HashMap<>();
            Iterator<String> keys = savedVisits.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                int value = savedVisits.getInt(key);
                if (value > 0) visits.put(key, value);
            }
            int turn = root.getInt("turn");
            int moveCount = root.getInt("moveCount");
            GameState.Outcome outcome = GameState.Outcome.valueOf(root.getString("outcome"));
            GameState state = new GameState(board, turn, moveCount, outcome,
                    root.optString("reason", ""), visits);
            int difficulty = Math.max(0, Math.min(2, root.optInt("difficulty", 1)));
            return new LoadedGame(state, difficulty);
        } catch (JSONException | IllegalArgumentException ex) {
            // A corrupted or incompatible save can be safely replaced.
            return null;
        }
    }

    public static void clear(Context context) {
        preferences(context).edit().remove(KEY).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static final class LoadedGame {
        public final GameState game;
        public final int difficulty;
        public LoadedGame(GameState game, int difficulty) {
            this.game = game;
            this.difficulty = difficulty;
        }
    }
}
