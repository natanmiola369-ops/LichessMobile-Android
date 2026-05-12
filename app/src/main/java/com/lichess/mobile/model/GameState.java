package com.lichess.mobile.model;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    public String gameId;
    public String playerColor; // "white" ou "black"
    public String status;      // "started", "mate", "resign", "draw", etc.
    public String winner;      // "white", "black" ou null
    public long whiteTimeMs;
    public long blackTimeMs;
    public List<String> moves = new ArrayList<String>();
    public boolean isMyTurn;
    public String lastMove;    // UCI do ultimo lance
    public boolean isCheck;
    public boolean isCheckmate;
    public boolean isStalemate;
    public boolean isDraw;
    public String wTitle;
    public String bTitle;
    public String wName;
    public String bName;
    public int wRating;
    public int bRating;

    // Para jogar contra Stockfish (offline/online)
    public boolean vsAI;
    public int aiLevel; // 1-8

    public GameState() {}
}
