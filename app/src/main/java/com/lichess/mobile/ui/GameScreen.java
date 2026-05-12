package com.lichess.mobile.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.lichess.mobile.model.ChessBoard;
import com.lichess.mobile.model.ChessPiece;
import com.lichess.mobile.model.GameState;
import com.lichess.mobile.service.LichessApi;
import com.lichess.mobile.service.SoundService;
import org.json.JSONObject;

public class GameScreen extends FrameLayout {

    private BoardView boardView;
    private ClockView clockView;
    private MoveListView moveListView;
    private TextView tvPlayerTop, tvPlayerBot;
    private TextView tvStatus;
    private Button btnResign, btnDraw, btnFlip;
    private LinearLayout root;

    private GameState gameState;
    private ChessBoard chessBoard;
    private SoundService soundService;
    private LichessApi api;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean streamActive = false;
    private int moveCount = 0;

    // Listener para voltar ao menu
    public interface OnBackListener { void onBack(); }
    private OnBackListener backListener;

    public GameScreen(Context context, GameState state) {
        super(context);
        this.gameState = state;
        this.soundService = SoundService.getInstance(context);
        this.api = LichessApi.getInstance();
        this.chessBoard = new ChessBoard();
        chessBoard.setupInitial();
        buildUI(context);
        setupPlayers();
        startGameStream();
    }

    public void setOnBackListener(OnBackListener l) { this.backListener = l; }

    private void buildUI(Context ctx) {
        root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1A1A2E);

        // Jogador topo (oponente)
        tvPlayerTop = makePlayerLabel(ctx);
        root.addView(tvPlayerTop);

        // Relógio
        clockView = new ClockView(ctx);
        LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 80);
        clockLp.setMargins(8, 4, 8, 4);
        clockView.setLayoutParams(clockLp);
        root.addView(clockView);

        // Tabuleiro
        boardView = new BoardView(ctx);
        LinearLayout.LayoutParams boardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        boardLp.gravity = Gravity.CENTER_HORIZONTAL;
        boardView.setLayoutParams(boardLp);
        boardView.setBoard(chessBoard);
        boardView.setOnMoveListener(new BoardView.OnMoveListener() {
            public void onMove(int fr, int fc, int tr, int tc) {
                handlePlayerMove(fr, fc, tr, tc);
            }
        });
        root.addView(boardView);

        // Lista de lances
        moveListView = new MoveListView(ctx);
        LinearLayout.LayoutParams moveLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 50);
        moveLp.setMargins(8, 4, 8, 4);
        moveListView.setLayoutParams(moveLp);
        moveListView.setBackgroundColor(0xFF16213E);
        root.addView(moveListView);

        // Status
        tvStatus = new TextView(ctx);
        tvStatus.setTextColor(0xFFCCCCCC);
        tvStatus.setTextSize(13f);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(8, 4, 8, 4);
        tvStatus.setText("Conectando...");
        root.addView(tvStatus);

        // Jogador baixo (eu)
        tvPlayerBot = makePlayerLabel(ctx);
        root.addView(tvPlayerBot);

        // Botões de ação
        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);

        btnFlip   = makeBtn(ctx, "Girar", 0xFF444466);
        btnDraw   = makeBtn(ctx, "Empate", 0xFF446644);
        btnResign = makeBtn(ctx, "Resignar", 0xFF664444);

        btnFlip.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                boardView.setFlipped(!gameState.playerColor.equals("white"));
            }
        });
        btnDraw.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { offerDraw(); }
        });
        btnResign.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { confirmResign(); }
        });

        btnRow.addView(btnFlip);
        btnRow.addView(btnDraw);
        btnRow.addView(btnResign);
        root.addView(btnRow);

        // Botão voltar
        Button btnBack = makeBtn(ctx, "Voltar", 0xFF333355);
        btnBack.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                streamActive = false;
                if (backListener != null) backListener.onBack();
            }
        });
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        backLp.setMargins(16, 4, 16, 8);
        btnBack.setLayoutParams(backLp);
        root.addView(btnBack);

        // Relógio de tempo baixo
        clockView.setLowTimeListener(new ClockView.LowTimeListener() {
            public void onLowTime(boolean isWhite) {
                boolean isMine = (isWhite && gameState.playerColor.equals("white"))
                              || (!isWhite && gameState.playerColor.equals("black"));
                if (isMine) soundService.playLowTime();
            }
        });

        addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private TextView makePlayerLabel(Context ctx) {
        TextView tv = new TextView(ctx);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(15f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(12, 6, 12, 2);
        return tv;
    }

    private Button makeBtn(Context ctx, String text, int bg) {
        Button btn = new Button(ctx);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(13f);
        btn.setBackgroundColor(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(4, 4, 4, 4);
        btn.setLayoutParams(lp);
        btn.setPadding(8, 8, 8, 8);
        return btn;
    }

    private void setupPlayers() {
        boolean iAmWhite = gameState.playerColor.equals("white");
        String myName  = gameState.playerColor.equals("white") ? gameState.wName : gameState.bName;
        String oppName = gameState.playerColor.equals("white") ? gameState.bName : gameState.wName;
        int myRating   = iAmWhite ? gameState.wRating : gameState.bRating;
        int oppRating  = iAmWhite ? gameState.bRating : gameState.wRating;

        tvPlayerTop.setText(oppName + (oppRating > 0 ? " (" + oppRating + ")" : ""));
        tvPlayerBot.setText(myName  + (myRating  > 0 ? " (" + myRating  + ")" : ""));

        // Pretas ficam em baixo se eu for preto
        boardView.setFlipped(!iAmWhite);
    }

    private void startGameStream() {
        if (gameState.gameId == null) return;
        streamActive = true;
        tvStatus.setText("Aguardando...");

        api.streamGame(gameState.gameId, new LichessApi.StreamCallback() {
            public void onLine(String line) {
                if (!streamActive) return;
                try {
                    JSONObject json = new JSONObject(line);
                    String type = json.optString("type");
                    if (type.equals("gameFull")) {
                        handleGameFull(json);
                    } else if (type.equals("gameState")) {
                        handleGameStateUpdate(json);
                    }
                } catch (Exception e) {
                    // ignora linhas malformadas
                }
            }
            public void onDone() { streamActive = false; }
            public void onError(String err) {
                streamActive = false;
                tvStatus.setText("Erro de conexao");
            }
        });
    }

    private void handleGameFull(JSONObject json) throws Exception {
        // Extrair info do jogo completo
        JSONObject state = json.optJSONObject("state");
        if (state != null) {
            applyMovesFromString(state.optString("moves", ""));
            long wTime = state.optLong("wtime", 300000);
            long bTime = state.optLong("btime", 300000);
            clockView.setTimes(wTime, bTime);

            String status = state.optString("status", "started");
            if (status.equals("started")) {
                soundService.playDong();
                startClockForCurrentTurn();
                tvStatus.setText(chessBoard.currentTurn == ChessPiece.WHITE ? "Vez das brancas" : "Vez das pretas");
            } else {
                handleGameEnd(state);
            }
        }
    }

    private void handleGameStateUpdate(JSONObject state) throws Exception {
        String movesStr = state.optString("moves", "");
        String[] movesArr = movesStr.trim().isEmpty() ? new String[0] : movesStr.trim().split(" ");
        int newCount = movesArr.length;

        if (newCount > moveCount) {
            // Aplica apenas os novos lances
            for (int i = moveCount; i < newCount; i++) {
                String uci = movesArr[i];
                boolean wasCapture = chessBoard.applyUciMove(uci);

                // Som
                if (wasCapture) soundService.playCapture();
                else soundService.playMove();

                // Adiciona à lista de lances (SAN simplificado = UCI por ora)
                boolean isWhiteMove = (i % 2 == 0);
                int moveNum = i / 2 + 1;
                moveListView.addMove(moveNum, uci, isWhiteMove);
                moveCount++;
            }
            boardView.invalidate();
        }

        long wTime = state.optLong("wtime", 0);
        long bTime = state.optLong("btime", 0);
        if (wTime > 0 || bTime > 0) clockView.setTimes(wTime, bTime);

        String status = state.optString("status", "started");
        if (!status.equals("started") && !status.equals("created")) {
            handleGameEnd(state);
        } else {
            startClockForCurrentTurn();
            boolean isMyTurn = (chessBoard.currentTurn == ChessPiece.WHITE && gameState.playerColor.equals("white"))
                            || (chessBoard.currentTurn == ChessPiece.BLACK && gameState.playerColor.equals("black"));
            tvStatus.setText(isMyTurn ? "Sua vez!" : "Aguardando oponente...");
        }
    }

    private void applyMovesFromString(String movesStr) {
        chessBoard.setupInitial();
        moveListView.clear();
        moveCount = 0;
        if (movesStr == null || movesStr.trim().isEmpty()) return;
        String[] moves = movesStr.trim().split(" ");
        for (int i = 0; i < moves.length; i++) {
            boolean wasCapture = chessBoard.applyUciMove(moves[i]);
            boolean isWhiteMove = (i % 2 == 0);
            int moveNum = i / 2 + 1;
            moveListView.addMove(moveNum, moves[i], isWhiteMove);
            moveCount++;
        }
        boardView.invalidate();
    }

    private void startClockForCurrentTurn() {
        if (chessBoard.currentTurn == ChessPiece.WHITE) clockView.startWhite();
        else clockView.startBlack();
    }

    private void handleGameEnd(JSONObject state) throws Exception {
        clockView.stopAll();
        String status = state.optString("status", "");
        String winner = state.optString("winner", "");
        String msg;
        switch (status) {
            case "mate":     msg = winner.equals("white") ? "Xeque-mate! Brancas vencem" : "Xeque-mate! Pretas vencem"; break;
            case "resign":   msg = winner.equals("white") ? "Pretas resignaram. Brancas vencem" : "Brancas resignaram. Pretas vencem"; break;
            case "outoftime":msg = winner.equals("white") ? "Tempo esgotado. Brancas vencem" : "Tempo esgotado. Pretas vencem"; break;
            case "draw":     msg = "Empate"; break;
            case "stalemate":msg = "Empate por afogamento"; break;
            case "aborted":  msg = "Jogo abortado"; break;
            default:         msg = "Jogo encerrado: " + status;
        }
        tvStatus.setText(msg);
        soundService.playConfirmation();

        // Desativa botões
        btnResign.setEnabled(false);
        btnDraw.setEnabled(false);
    }

    private void handlePlayerMove(int fromRow, int fromCol, int toRow, int toCol) {
        // Verifica se é minha vez
        boolean isMyTurn = (chessBoard.currentTurn == ChessPiece.WHITE && gameState.playerColor.equals("white"))
                        || (chessBoard.currentTurn == ChessPiece.BLACK && gameState.playerColor.equals("black"));
        if (!isMyTurn) {
            soundService.playError();
            return;
        }

        // Promoção: por simplicidade, sempre promove a dama
        ChessPiece piece = chessBoard.board[fromRow][fromCol];
        boolean isPromotion = piece.type == ChessPiece.PAWN
                && ((toRow == 0 && piece.isWhite()) || (toRow == 7 && piece.isBlack()));

        String uci = chessBoard.moveToUci(fromRow, fromCol, toRow, toCol);
        if (isPromotion) uci += "q";

        final String finalUci = uci;

        // Aplica localmente (otimista)
        boolean capture = chessBoard.applyMove(fromRow, fromCol, toRow, toCol, ChessPiece.QUEEN);
        if (capture) soundService.playCapture();
        else soundService.playMove();

        boolean isWhiteMove = (chessBoard.currentTurn == ChessPiece.BLACK); // turno já avançou
        int moveNum = moveCount / 2 + 1;
        moveListView.addMove(moveNum, finalUci, isWhiteMove);
        moveCount++;
        boardView.invalidate();
        startClockForCurrentTurn();
        tvStatus.setText("Aguardando oponente...");

        // Envia para lichess
        api.makeMove(gameState.gameId, finalUci, new LichessApi.Callback() {
            public void onSuccess(JSONObject json) {
                // Confirmado
            }
            public void onError(String err) {
                tvStatus.setText("Erro ao enviar lance: " + err);
                soundService.playError();
            }
        });
    }

    private void confirmResign() {
        new AlertDialog.Builder(getContext())
            .setTitle("Resignar")
            .setMessage("Tem certeza que deseja resignar?")
            .setPositiveButton("Sim", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int w) {
                    api.resignGame(gameState.gameId, new LichessApi.Callback() {
                        public void onSuccess(JSONObject j) {}
                        public void onError(String e) {}
                    });
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void offerDraw() {
        new AlertDialog.Builder(getContext())
            .setTitle("Propor Empate")
            .setMessage("Propor empate ao oponente?")
            .setPositiveButton("Sim", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int w) {
                    api.offerDraw(gameState.gameId, new LichessApi.Callback() {
                        public void onSuccess(JSONObject j) { tvStatus.setText("Empate proposto..."); }
                        public void onError(String e) {}
                    });
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }
}
