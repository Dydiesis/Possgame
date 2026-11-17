package com.possgame.nexus;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.possgame.nexus.ai.NexusAi;
import com.possgame.nexus.engine.GameEngine;
import com.possgame.nexus.engine.GameState;
import com.possgame.nexus.engine.Move;
import com.possgame.nexus.storage.GameStorage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Android UI; the engine, opponent and saved-game format remain independent. */
public final class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(14, 22, 38);
    private static final int SURFACE = Color.rgb(27, 40, 62);
    private static final int LIGHT = Color.rgb(237, 243, 249);
    private static final int MUTED = Color.rgb(168, 188, 207);
    private static final int HUMAN_COLOR = Color.rgb(80, 170, 244);
    private static final int AI_COLOR = Color.rgb(250, 116, 112);
    private static final int GOLD = Color.rgb(243, 201, 100);
    private static final int GREEN = Color.rgb(106, 222, 176);

    private final Button[] squares = new Button[25];
    private final ExecutorService aiWorker = Executors.newSingleThreadExecutor();
    private GameState game;
    private int difficulty = 1;
    private int selected = -1;
    private int aiSequence = 0;
    private boolean thinking = false;
    private boolean destroyed = false;

    private TextView heading;
    private TextView status;
    private TextView details;
    private Spinner difficultyPicker;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        GameStorage.LoadedGame restored = GameStorage.load(this);
        if (restored != null) {
            game = restored.game;
            difficulty = restored.difficulty;
        } else {
            game = GameState.newGame();
        }
        buildInterface();
        showBoard();
        if (!game.isOver() && game.turn() == GameState.AI) performAiTurn();
    }

    private int dp(float pixels) {
        return Math.round(pixels * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable shape(int fill, int line, int thickness, int corner) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(corner));
        drawable.setStroke(dp(thickness), line);
        return drawable;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setGravity(Gravity.CENTER);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams verticalParams(int marginTop) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(marginTop);
        return params;
    }

    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(14), dp(14), dp(18));
        scroll.addView(content);
        setContentView(scroll);

        heading = label("NEXUS", 28, LIGHT, true);
        heading.setLetterSpacing(0.13f);
        content.addView(heading, verticalParams(0));

        TextView subtitle = label("TRE PUNTI. UNA MOSSA. OGNI TURNO CONTA.", 11, MUTED, false);
        content.addView(subtitle, verticalParams(2));

        status = label("", 19, LIGHT, true);
        status.setPadding(dp(4), dp(12), dp(4), dp(12));
        status.setBackground(shape(SURFACE, SURFACE, 1, 12));
        content.addView(status, verticalParams(16));

        details = label("", 12, MUTED, false);
        content.addView(details, verticalParams(10));

        GridLayout board = new GridLayout(this);
        board.setColumnCount(5);
        board.setRowCount(5);
        content.addView(board, verticalParams(15));

        int available = getResources().getDisplayMetrics().widthPixels - dp(28);
        int size = (available - dp(5 * 4)) / 5;
        for (int index = 0; index < squares.length; index++) {
            final int cell = index;
            Button square = new Button(this);
            square.setAllCaps(false);
            square.setTextSize(27);
            square.setPadding(0, 0, 0, 0);
            square.setMinHeight(0);
            square.setMinimumHeight(0);
            square.setMinWidth(0);
            square.setMinimumWidth(0);
            square.setOnClickListener(view -> onSquareTapped(cell));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
            board.addView(square, params);
            squares[index] = square;
        }

        TextView legend = label("●  BLU: TU       ●  CORALLO: IA       ◆  ORO: OBIETTIVO", 12, LIGHT, false);
        content.addView(legend, verticalParams(12));

        TextView instruction = label(
                "Tocca una tua pedina, poi una casella vicina. Puoi catturare una pedina avversaria. " +
                "Per vincere, mantieni tutti e tre i punti fino all'inizio del tuo prossimo turno.",
                13, MUTED, false);
        instruction.setGravity(Gravity.CENTER);
        instruction.setPadding(dp(4), dp(8), dp(4), dp(8));
        content.addView(instruction, verticalParams(5));

        TextView difficultyLabel = label("DIFFICOLTÀ AVVERSARIA", 13, LIGHT, true);
        content.addView(difficultyLabel, verticalParams(14));

        difficultyPicker = new Spinner(this);
        String[] levels = {"FACILE", "MEDIA", "DIFFICILE"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, levels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultyPicker.setAdapter(adapter);
        difficultyPicker.setSelection(difficulty);
        difficultyPicker.setBackground(shape(SURFACE, MUTED, 1, 10));
        difficultyPicker.setPadding(dp(12), dp(4), dp(12), dp(4));
        difficultyPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                difficulty = position;
                GameStorage.save(MainActivity.this, game, difficulty);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        content.addView(difficultyPicker, verticalParams(7));

        Button restart = new Button(this);
        restart.setAllCaps(false);
        restart.setText("NUOVA PARTITA");
        restart.setTextColor(BACKGROUND);
        restart.setTextSize(16);
        restart.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        restart.setBackground(shape(GREEN, GREEN, 1, 12));
        restart.setOnClickListener(v -> {
            aiSequence++;
            thinking = false;
            selected = -1;
            game = GameState.newGame();
            GameStorage.clear(this);
            GameStorage.save(this, game, difficulty);
            showBoard();
        });
        content.addView(restart, verticalParams(17));

        TextView footer = label("Offline • Salvataggio automatico • 100 mosse massimo", 11, MUTED, false);
        content.addView(footer, verticalParams(11));
    }

    private void onSquareTapped(int index) {
        if (thinking || game.isOver() || game.turn() != GameState.HUMAN) return;
        int piece = game.copyBoard()[index];
        if (piece == GameState.HUMAN) {
            selected = index;
            showBoard();
            return;
        }
        if (selected == -1) return;
        Move move = new Move(selected / 5, selected % 5, index / 5, index % 5);
        if (!GameEngine.isLegal(game, move)) {
            status.setText("Mossa non consentita");
            return;
        }
        game = GameEngine.apply(game, move);
        selected = -1;
        GameStorage.save(this, game, difficulty);
        showBoard();
        if (!game.isOver() && game.turn() == GameState.AI) performAiTurn();
    }

    private void showBoard() {
        if (game == null) return;
        if (game.isOver()) {
            switch (game.outcome()) {
                case HUMAN_WIN: status.setText("HAI VINTO!"); break;
                case AI_WIN: status.setText("VINCE L'AVVERSARIA"); break;
                case DRAW: status.setText("PARTITA PATTA"); break;
                default: break;
            }
        } else if (thinking) {
            status.setText("L'avversaria sta pensando…");
        } else if (game.turn() == GameState.HUMAN) {
            status.setText("È IL TUO TURNO");
        } else {
            status.setText("TURNO DELL'AVVERSARIA");
        }

        int humanControl = 0, aiControl = 0;
        for (int[] point : GameEngine.POINTS) {
            int owner = game.pieceAt(point[0], point[1]);
            if (owner == GameState.HUMAN) humanControl++;
            else if (owner == GameState.AI) aiControl++;
        }
        if (game.isOver()) {
            details.setText(game.reason() + "  •  " + game.moveCount() + " mosse");
        } else {
            details.setText("Punti controllati: TU " + humanControl + "/3  •  IA "
                    + aiControl + "/3     |     Mossa " + (game.moveCount() + 1) + "/100");
        }

        List<Move> available = selected < 0 ? java.util.Collections.emptyList()
                : GameEngine.legalMoves(game);
        for (int i = 0; i < squares.length; i++) {
            int row = i / 5;
            int col = i % 5;
            int value = game.pieceAt(row, col);
            boolean point = GameEngine.isPoint(row, col);
            boolean target = false;
            for (Move move : available) {
                if (move.fromRow == selected / 5 && move.fromCol == selected % 5
                        && move.toRow == row && move.toCol == col) {
                    target = true;
                    break;
                }
            }
            int fill = (row + col) % 2 == 0 ? Color.rgb(39, 57, 80) : Color.rgb(34, 49, 71);
            int border = i == selected ? GREEN : target ? GREEN : point ? GOLD : SURFACE;
            int stroke = i == selected || target ? 3 : point ? 2 : 1;
            squares[i].setBackground(shape(fill, border, stroke, 9));
            squares[i].setText(value == GameState.HUMAN ? "●" :
                    value == GameState.AI ? "●" : point ? "◆" : target ? "·" : "");
            squares[i].setTextColor(value == GameState.HUMAN ? HUMAN_COLOR :
                    value == GameState.AI ? AI_COLOR : point ? GOLD : GREEN);
            String occupant = value == GameState.HUMAN ? "pedina del giocatore"
                    : value == GameState.AI ? "pedina avversaria" : "vuota";
            squares[i].setContentDescription("Riga " + (row + 1) + ", colonna " + (col + 1)
                    + ", " + occupant + (point ? ", punto strategico" : "")
                    + (target ? ", mossa possibile" : ""));
            squares[i].setEnabled(!thinking && !game.isOver() && game.turn() == GameState.HUMAN);
        }
    }

    private void performAiTurn() {
        if (destroyed || thinking || game.isOver() || game.turn() != GameState.AI) return;
        thinking = true;
        selected = -1;
        showBoard();
        final GameState snapshot = game;
        final int selectedDifficulty = difficulty;
        final int sequence = ++aiSequence;
        aiWorker.execute(() -> {
            Move choice = new NexusAi().choose(snapshot, selectedDifficulty);
            runOnUiThread(() -> {
                if (destroyed || sequence != aiSequence || game != snapshot) return;
                thinking = false;
                if (choice != null && GameEngine.isLegal(game, choice)) {
                    game = GameEngine.apply(game, choice);
                }
                GameStorage.save(MainActivity.this, game, difficulty);
                showBoard();
            });
        });
    }

    @Override protected void onPause() {
        if (game != null) GameStorage.save(this, game, difficulty);
        super.onPause();
    }

    @Override protected void onDestroy() {
        destroyed = true;
        aiSequence++;
        aiWorker.shutdownNow();
        super.onDestroy();
    }
}
