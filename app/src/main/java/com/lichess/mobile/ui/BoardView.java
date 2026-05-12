package com.lichess.mobile.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import com.lichess.mobile.model.ChessBoard;
import com.lichess.mobile.model.ChessPiece;
import java.util.ArrayList;
import java.util.List;

public class BoardView extends View {

    // Paleta lichess original
    private static final int COLOR_LIGHT       = 0xFFF0D9B5;
    private static final int COLOR_DARK        = 0xFFB58863;
    private static final int COLOR_SELECTED    = 0xFF20B2AA;
    private static final int COLOR_LAST_MOVE   = 0x88CDD26A;
    private static final int COLOR_LEGAL       = 0x6020B2AA;
    private static final int COLOR_CHECK       = 0xCCFF4444;
    private static final int COLOR_COORD_LIGHT = 0xFFB58863;
    private static final int COLOR_COORD_DARK  = 0xFFF0D9B5;

    private Paint paintSquare  = new Paint();
    private Paint paintPiece   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintCoord   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintDot     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintHighlight = new Paint();

    private ChessBoard chessBoard;
    private int selectedRow = -1, selectedCol = -1;
    private List<int[]> legalMoves = new ArrayList<int[]>();
    private boolean flipped = false; // true = pretas em baixo

    public interface OnMoveListener {
        void onMove(int fromRow, int fromCol, int toRow, int toCol);
    }
    private OnMoveListener moveListener;

    public BoardView(Context context) {
        super(context);
        paintPiece.setTextAlign(Paint.Align.CENTER);
        paintCoord.setTextAlign(Paint.Align.LEFT);
        paintCoord.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        chessBoard = new ChessBoard();
        chessBoard.setupInitial();
    }

    public void setBoard(ChessBoard board) {
        this.chessBoard = board;
        clearSelection();
        invalidate();
    }

    public ChessBoard getBoard() { return chessBoard; }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        invalidate();
    }

    public void setOnMoveListener(OnMoveListener l) { this.moveListener = l; }

    public void clearSelection() {
        selectedRow = -1; selectedCol = -1;
        legalMoves.clear();
        invalidate();
    }

    private float squareSize() {
        return Math.min(getWidth(), getHeight()) / 8f;
    }

    // Converte row/col do tabuleiro para row/col da tela (levando em conta flip)
    private int displayRow(int row) { return flipped ? (7 - row) : row; }
    private int displayCol(int col) { return flipped ? (7 - col) : col; }
    private int boardRow(int dRow)  { return flipped ? (7 - dRow) : dRow; }
    private int boardCol(int dCol)  { return flipped ? (7 - dCol) : dCol; }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int size = Math.min(MeasureSpec.getSize(widthSpec), MeasureSpec.getSize(heightSpec));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float sq = squareSize();
        paintPiece.setTextSize(sq * 0.78f);
        paintCoord.setTextSize(sq * 0.22f);

        for (int dr = 0; dr < 8; dr++) {
            for (int dc = 0; dc < 8; dc++) {
                int r = boardRow(dr);
                int c = boardCol(dc);
                float x = dc * sq;
                float y = dr * sq;

                // Cor base do quadrado
                boolean isLight = (r + c) % 2 == 0;
                paintSquare.setColor(isLight ? COLOR_LIGHT : COLOR_DARK);
                canvas.drawRect(x, y, x + sq, y + sq, paintSquare);

                // Highlight último lance
                if ((r == chessBoard.lastFromRow && c == chessBoard.lastFromCol)
                        || (r == chessBoard.lastToRow && c == chessBoard.lastToCol)) {
                    paintHighlight.setColor(COLOR_LAST_MOVE);
                    canvas.drawRect(x, y, x + sq, y + sq, paintHighlight);
                }

                // Highlight seleção
                if (r == selectedRow && c == selectedCol) {
                    paintHighlight.setColor(COLOR_SELECTED);
                    canvas.drawRect(x, y, x + sq, y + sq, paintHighlight);
                }

                // Highlight xeque
                ChessPiece p = chessBoard.board[r][c];
                if (p.type == ChessPiece.KING && chessBoard.isInCheck(p.color)
                        && p.color == chessBoard.currentTurn) {
                    paintHighlight.setColor(COLOR_CHECK);
                    canvas.drawRect(x, y, x + sq, y + sq, paintHighlight);
                }

                // Movimentos legais
                if (isLegalTarget(r, c)) {
                    if (!chessBoard.board[r][c].isEmpty()) {
                        // Quadrado com peça capturável: anel
                        paintDot.setColor(COLOR_LEGAL);
                        paintDot.setStyle(Paint.Style.STROKE);
                        paintDot.setStrokeWidth(sq * 0.1f);
                        canvas.drawCircle(x + sq/2, y + sq/2, sq * 0.44f, paintDot);
                        paintDot.setStyle(Paint.Style.FILL);
                    } else {
                        // Ponto no centro
                        paintDot.setColor(COLOR_LEGAL);
                        paintDot.setStyle(Paint.Style.FILL);
                        canvas.drawCircle(x + sq/2, y + sq/2, sq * 0.15f, paintDot);
                    }
                }

                // Coordenadas (letras e números nas bordas)
                if (dc == 0) {
                    // Número (linha)
                    paintCoord.setColor(isLight ? COLOR_COORD_LIGHT : COLOR_COORD_DARK);
                    String num = String.valueOf(flipped ? (r + 1) : (8 - r));
                    canvas.drawText(num, x + sq * 0.04f, y + sq * 0.28f, paintCoord);
                }
                if (dr == 7) {
                    // Letra (coluna)
                    paintCoord.setColor(isLight ? COLOR_COORD_LIGHT : COLOR_COORD_DARK);
                    String letter = String.valueOf((char)('a' + (flipped ? (7 - c) : c)));
                    canvas.drawText(letter, x + sq * 0.72f, y + sq * 0.98f, paintCoord);
                }

                // Peça
                if (!p.isEmpty()) {
                    paintPiece.setColor(p.isWhite() ? Color.WHITE : Color.BLACK);
                    // Sombra suave para legibilidade
                    paintPiece.setShadowLayer(sq * 0.06f, sq * 0.02f, sq * 0.02f,
                            p.isWhite() ? 0x88000000 : 0x88FFFFFF);
                    float pieceX = x + sq / 2;
                    float pieceY = y + sq * 0.82f;
                    canvas.drawText(p.getSymbol(), pieceX, pieceY, paintPiece);
                }
            }
        }
    }

    private boolean isLegalTarget(int row, int col) {
        for (int[] m : legalMoves)
            if (m[0] == row && m[1] == col) return true;
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float sq = squareSize();
        int dc = (int)(event.getX() / sq);
        int dr = (int)(event.getY() / sq);
        if (dc < 0 || dc > 7 || dr < 0 || dr > 7) return true;

        int r = boardRow(dr);
        int c = boardCol(dc);

        if (selectedRow >= 0) {
            // Verifica se o toque é em um movimento legal
            if (isLegalTarget(r, c)) {
                int fr = selectedRow, fc = selectedCol;
                clearSelection();
                if (moveListener != null) moveListener.onMove(fr, fc, r, c);
                return true;
            }
            // Toca em outra peça própria
            if (!chessBoard.board[r][c].isEmpty()
                    && chessBoard.board[r][c].color == chessBoard.currentTurn) {
                select(r, c);
                return true;
            }
            clearSelection();
        } else {
            // Seleciona peça
            if (!chessBoard.board[r][c].isEmpty()
                    && chessBoard.board[r][c].color == chessBoard.currentTurn) {
                select(r, c);
            }
        }
        return true;
    }

    private void select(int r, int c) {
        selectedRow = r; selectedCol = c;
        legalMoves = chessBoard.getLegalMoves(r, c);
        invalidate();
    }
}
