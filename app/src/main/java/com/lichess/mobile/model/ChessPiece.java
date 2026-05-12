package com.lichess.mobile.model;

public class ChessPiece {
    public static final int EMPTY = 0;
    public static final int WHITE = 1;
    public static final int BLACK = 2;

    public static final int PAWN   = 1;
    public static final int KNIGHT = 2;
    public static final int BISHOP = 3;
    public static final int ROOK   = 4;
    public static final int QUEEN  = 5;
    public static final int KING   = 6;

    public final int type;
    public final int color;

    public ChessPiece(int type, int color) {
        this.type = type;
        this.color = color;
    }

    public boolean isEmpty() {
        return type == EMPTY;
    }

    public boolean isWhite() {
        return color == WHITE;
    }

    public boolean isBlack() {
        return color == BLACK;
    }

    // Retorna símbolo Unicode da peça
    public String getSymbol() {
        if (color == WHITE) {
            switch (type) {
                case PAWN:   return "\u2659";
                case KNIGHT: return "\u2658";
                case BISHOP: return "\u2657";
                case ROOK:   return "\u2656";
                case QUEEN:  return "\u2655";
                case KING:   return "\u2654";
            }
        } else {
            switch (type) {
                case PAWN:   return "\u265F";
                case KNIGHT: return "\u265E";
                case BISHOP: return "\u265D";
                case ROOK:   return "\u265C";
                case QUEEN:  return "\u265B";
                case KING:   return "\u265A";
            }
        }
        return "";
    }

    // Retorna letra FEN da peça
    public char getFenChar() {
        char c;
        switch (type) {
            case PAWN:   c = 'p'; break;
            case KNIGHT: c = 'n'; break;
            case BISHOP: c = 'b'; break;
            case ROOK:   c = 'r'; break;
            case QUEEN:  c = 'q'; break;
            case KING:   c = 'k'; break;
            default: return '.';
        }
        return color == WHITE ? Character.toUpperCase(c) : c;
    }

    @Override
    public String toString() {
        return getSymbol();
    }
}
