package com.lichess.mobile.model;

import java.util.ArrayList;
import java.util.List;

public class ChessBoard {

    public ChessPiece[][] board = new ChessPiece[8][8];
    public int currentTurn = ChessPiece.WHITE;

    // Direitos de roque
    public boolean whiteKingsideCastle  = true;
    public boolean whiteQueensideCastle = true;
    public boolean blackKingsideCastle  = true;
    public boolean blackQueensideCastle = true;

    // En passant: coluna alvo (-1 = nenhum)
    public int enPassantCol = -1;
    public int enPassantRow = -1;

    // Ultimo movimento para highlight
    public int lastFromRow = -1, lastFromCol = -1;
    public int lastToRow   = -1, lastToCol   = -1;

    public int halfMoveClock  = 0;
    public int fullMoveNumber = 1;

    // Status
    public boolean gameOver = false;
    public String  gameResult = ""; // "1-0", "0-1", "1/2-1/2"

    public ChessBoard() {
        initEmpty();
    }

    private void initEmpty() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                board[r][c] = new ChessPiece(ChessPiece.EMPTY, ChessPiece.EMPTY);
            }
        }
    }

    // Configura posição inicial
    public void setupInitial() {
        initEmpty();
        currentTurn = ChessPiece.WHITE;
        whiteKingsideCastle = whiteQueensideCastle = true;
        blackKingsideCastle = blackQueensideCastle = true;
        enPassantCol = -1; enPassantRow = -1;
        halfMoveClock = 0; fullMoveNumber = 1;
        gameOver = false; gameResult = "";
        lastFromRow = lastFromCol = lastToRow = lastToCol = -1;

        int[] backRank = {
            ChessPiece.ROOK, ChessPiece.KNIGHT, ChessPiece.BISHOP, ChessPiece.QUEEN,
            ChessPiece.KING, ChessPiece.BISHOP, ChessPiece.KNIGHT, ChessPiece.ROOK
        };
        for (int c = 0; c < 8; c++) {
            board[7][c] = new ChessPiece(backRank[c], ChessPiece.WHITE);
            board[6][c] = new ChessPiece(ChessPiece.PAWN, ChessPiece.WHITE);
            board[1][c] = new ChessPiece(ChessPiece.PAWN, ChessPiece.BLACK);
            board[0][c] = new ChessPiece(backRank[c], ChessPiece.BLACK);
        }
    }

    // Carrega FEN
    public void loadFen(String fen) {
        initEmpty();
        String[] parts = fen.split(" ");
        String[] rows = parts[0].split("/");
        for (int r = 0; r < 8; r++) {
            int c = 0;
            for (char ch : rows[r].toCharArray()) {
                if (Character.isDigit(ch)) {
                    c += (ch - '0');
                } else {
                    int color = Character.isUpperCase(ch) ? ChessPiece.WHITE : ChessPiece.BLACK;
                    int type;
                    switch (Character.toLowerCase(ch)) {
                        case 'p': type = ChessPiece.PAWN;   break;
                        case 'n': type = ChessPiece.KNIGHT; break;
                        case 'b': type = ChessPiece.BISHOP; break;
                        case 'r': type = ChessPiece.ROOK;   break;
                        case 'q': type = ChessPiece.QUEEN;  break;
                        case 'k': type = ChessPiece.KING;   break;
                        default:  type = ChessPiece.EMPTY;  break;
                    }
                    board[r][c] = new ChessPiece(type, color);
                    c++;
                }
            }
        }
        currentTurn = (parts.length > 1 && parts[1].equals("b")) ? ChessPiece.BLACK : ChessPiece.WHITE;
        if (parts.length > 2) {
            String castling = parts[2];
            whiteKingsideCastle  = castling.contains("K");
            whiteQueensideCastle = castling.contains("Q");
            blackKingsideCastle  = castling.contains("k");
            blackQueensideCastle = castling.contains("q");
        }
        if (parts.length > 3 && !parts[3].equals("-")) {
            enPassantCol = parts[3].charAt(0) - 'a';
            enPassantRow = 8 - (parts[3].charAt(1) - '0');
        }
        if (parts.length > 4) halfMoveClock  = Integer.parseInt(parts[4]);
        if (parts.length > 5) fullMoveNumber = Integer.parseInt(parts[5]);
    }

    // Gera FEN atual
    public String toFen() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 8; r++) {
            int empty = 0;
            for (int c = 0; c < 8; c++) {
                ChessPiece p = board[r][c];
                if (p.isEmpty()) {
                    empty++;
                } else {
                    if (empty > 0) { sb.append(empty); empty = 0; }
                    sb.append(p.getFenChar());
                }
            }
            if (empty > 0) sb.append(empty);
            if (r < 7) sb.append('/');
        }
        sb.append(' ').append(currentTurn == ChessPiece.WHITE ? 'w' : 'b');
        sb.append(' ');
        String castling = (whiteKingsideCastle ? "K" : "") + (whiteQueensideCastle ? "Q" : "")
                        + (blackKingsideCastle  ? "k" : "") + (blackQueensideCastle  ? "q" : "");
        sb.append(castling.isEmpty() ? "-" : castling);
        sb.append(' ');
        if (enPassantCol >= 0) {
            sb.append((char)('a' + enPassantCol)).append(8 - enPassantRow);
        } else {
            sb.append('-');
        }
        sb.append(' ').append(halfMoveClock).append(' ').append(fullMoveNumber);
        return sb.toString();
    }

    // Converte UCI string para movimento: "e2e4" -> [fromRow,fromCol,toRow,toCol,promo]
    public int[] uciToMove(String uci) {
        if (uci == null || uci.length() < 4) return null;
        int fromCol = uci.charAt(0) - 'a';
        int fromRow = 8 - (uci.charAt(1) - '0');
        int toCol   = uci.charAt(2) - 'a';
        int toRow   = 8 - (uci.charAt(3) - '0');
        int promo   = ChessPiece.QUEEN;
        if (uci.length() == 5) {
            switch (uci.charAt(4)) {
                case 'q': promo = ChessPiece.QUEEN;  break;
                case 'r': promo = ChessPiece.ROOK;   break;
                case 'b': promo = ChessPiece.BISHOP; break;
                case 'n': promo = ChessPiece.KNIGHT; break;
            }
        }
        return new int[]{fromRow, fromCol, toRow, toCol, promo};
    }

    // Aplica movimento no tabuleiro, retorna true se captura
    public boolean applyMove(int fromRow, int fromCol, int toRow, int toCol, int promoteTo) {
        ChessPiece piece = board[fromRow][fromCol];
        ChessPiece target = board[toRow][toCol];
        boolean isCapture = !target.isEmpty();

        // En passant capture
        boolean isEnPassant = false;
        if (piece.type == ChessPiece.PAWN && toCol == enPassantCol && toRow == enPassantRow) {
            isEnPassant = true;
            isCapture = true;
        }

        // Atualiza en passant para proximo turno
        enPassantCol = -1; enPassantRow = -1;
        if (piece.type == ChessPiece.PAWN && Math.abs(toRow - fromRow) == 2) {
            enPassantCol = fromCol;
            enPassantRow = (fromRow + toRow) / 2;
        }

        // Roque
        boolean isCastle = false;
        if (piece.type == ChessPiece.KING && Math.abs(toCol - fromCol) == 2) {
            isCastle = true;
            if (toCol == 6) { // kingside
                board[fromRow][5] = board[fromRow][7];
                board[fromRow][7] = new ChessPiece(ChessPiece.EMPTY, ChessPiece.EMPTY);
            } else { // queenside
                board[fromRow][3] = board[fromRow][0];
                board[fromRow][0] = new ChessPiece(ChessPiece.EMPTY, ChessPiece.EMPTY);
            }
        }

        // Direitos de roque
        if (piece.type == ChessPiece.KING) {
            if (piece.isWhite()) { whiteKingsideCastle = false; whiteQueensideCastle = false; }
            else                  { blackKingsideCastle  = false; blackQueensideCastle  = false; }
        }
        if (piece.type == ChessPiece.ROOK) {
            if (fromRow == 7 && fromCol == 7) whiteKingsideCastle  = false;
            if (fromRow == 7 && fromCol == 0) whiteQueensideCastle = false;
            if (fromRow == 0 && fromCol == 7) blackKingsideCastle  = false;
            if (fromRow == 0 && fromCol == 0) blackQueensideCastle = false;
        }

        // Move a peça
        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = new ChessPiece(ChessPiece.EMPTY, ChessPiece.EMPTY);

        // En passant: remove peão capturado
        if (isEnPassant) {
            board[fromRow][toCol] = new ChessPiece(ChessPiece.EMPTY, ChessPiece.EMPTY);
        }

        // Promoção
        if (piece.type == ChessPiece.PAWN && (toRow == 0 || toRow == 7)) {
            board[toRow][toCol] = new ChessPiece(promoteTo, piece.color);
        }

        // Atualiza contadores
        if (piece.type == ChessPiece.PAWN || isCapture) halfMoveClock = 0;
        else halfMoveClock++;
        if (currentTurn == ChessPiece.BLACK) fullMoveNumber++;

        lastFromRow = fromRow; lastFromCol = fromCol;
        lastToRow   = toRow;   lastToCol   = toCol;

        currentTurn = (currentTurn == ChessPiece.WHITE) ? ChessPiece.BLACK : ChessPiece.WHITE;
        return isCapture || isEnPassant;
    }

    // Aplica movimento UCI direto
    public boolean applyUciMove(String uci) {
        int[] m = uciToMove(uci);
        if (m == null) return false;
        return applyMove(m[0], m[1], m[2], m[3], m[4]);
    }

    // Encontra rei de uma cor
    public int[] findKing(int color) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c].type == ChessPiece.KING && board[r][c].color == color)
                    return new int[]{r, c};
        return new int[]{-1, -1};
    }

    // Verifica se posição [r,c] está atacada por 'attackerColor'
    public boolean isAttacked(int r, int c, int attackerColor) {
        // Peões
        int pawnDir = (attackerColor == ChessPiece.WHITE) ? 1 : -1;
        int pr = r + pawnDir;
        if (pr >= 0 && pr < 8) {
            if (c - 1 >= 0 && board[pr][c-1].type == ChessPiece.PAWN && board[pr][c-1].color == attackerColor) return true;
            if (c + 1 <  8 && board[pr][c+1].type == ChessPiece.PAWN && board[pr][c+1].color == attackerColor) return true;
        }
        // Cavalos
        int[][] knightMoves = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] km : knightMoves) {
            int nr = r + km[0], nc = c + km[1];
            if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8)
                if (board[nr][nc].type == ChessPiece.KNIGHT && board[nr][nc].color == attackerColor) return true;
        }
        // Bispo/Rainha (diagonais)
        int[][] diagDirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : diagDirs) {
            int nr = r + d[0], nc = c + d[1];
            while (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                ChessPiece p = board[nr][nc];
                if (!p.isEmpty()) {
                    if (p.color == attackerColor && (p.type == ChessPiece.BISHOP || p.type == ChessPiece.QUEEN)) return true;
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }
        // Torre/Rainha (retas)
        int[][] lineDirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : lineDirs) {
            int nr = r + d[0], nc = c + d[1];
            while (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                ChessPiece p = board[nr][nc];
                if (!p.isEmpty()) {
                    if (p.color == attackerColor && (p.type == ChessPiece.ROOK || p.type == ChessPiece.QUEEN)) return true;
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }
        // Rei
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8)
                    if (board[nr][nc].type == ChessPiece.KING && board[nr][nc].color == attackerColor) return true;
            }
        }
        return false;
    }

    public boolean isInCheck(int color) {
        int[] king = findKing(color);
        if (king[0] < 0) return false;
        int opponent = (color == ChessPiece.WHITE) ? ChessPiece.BLACK : ChessPiece.WHITE;
        return isAttacked(king[0], king[1], opponent);
    }

    // Verifica se o movimento deixa o próprio rei em xeque (movimento ilegal)
    private boolean moveLeavesKingInCheck(int fromRow, int fromCol, int toRow, int toCol, int promoteTo) {
        ChessPiece[][] backup = copyBoard();
        boolean epColBak = false; int epColSave = enPassantCol, epRowSave = enPassantRow;
        ChessPiece piece = board[fromRow][fromCol];
        int color = piece.color;

        // Simula movimento
        ChessPiece targetPiece = board[toRow][toCol];
        boolean isEnP = piece.type == ChessPiece.PAWN && toCol == enPassantCol && toRow == enPassantRow;

        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = new ChessPiece(ChessPiece.EMPTY, ChessPiece.EMPTY);
        if (isEnP) board[fromRow][toCol] = new ChessPiece(ChessPiece.EMPTY, ChessPiece.EMPTY);
        if (piece.type == ChessPiece.PAWN && (toRow == 0 || toRow == 7))
            board[toRow][toCol] = new ChessPiece(promoteTo, color);

        boolean inCheck = isInCheck(color);

        // Restaura
        board = backup;
        enPassantCol = epColSave; enPassantRow = epRowSave;
        return inCheck;
    }

    private ChessPiece[][] copyBoard() {
        ChessPiece[][] copy = new ChessPiece[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                copy[r][c] = board[r][c];
        return copy;
    }

    // Retorna lista de movimentos legais para peça em [row,col]
    public List<int[]> getLegalMoves(int row, int col) {
        List<int[]> moves = new ArrayList<int[]>();
        ChessPiece piece = board[row][col];
        if (piece.isEmpty() || piece.color != currentTurn) return moves;

        List<int[]> pseudo = getPseudoLegalMoves(row, col);
        for (int[] m : pseudo) {
            if (!moveLeavesKingInCheck(row, col, m[0], m[1], ChessPiece.QUEEN))
                moves.add(m);
        }
        return moves;
    }

    private List<int[]> getPseudoLegalMoves(int row, int col) {
        List<int[]> moves = new ArrayList<int[]>();
        ChessPiece piece = board[row][col];
        int color = piece.color;
        int opponent = (color == ChessPiece.WHITE) ? ChessPiece.BLACK : ChessPiece.WHITE;

        switch (piece.type) {
            case ChessPiece.PAWN: {
                int dir = (color == ChessPiece.WHITE) ? -1 : 1;
                int startRow = (color == ChessPiece.WHITE) ? 6 : 1;
                // Avança 1
                int nr = row + dir;
                if (nr >= 0 && nr < 8 && board[nr][col].isEmpty()) {
                    moves.add(new int[]{nr, col});
                    // Avança 2
                    if (row == startRow && board[row + 2*dir][col].isEmpty())
                        moves.add(new int[]{row + 2*dir, col});
                }
                // Capturas
                for (int dc : new int[]{-1, 1}) {
                    int nc = col + dc;
                    if (nc >= 0 && nc < 8 && nr >= 0 && nr < 8) {
                        if (!board[nr][nc].isEmpty() && board[nr][nc].color == opponent)
                            moves.add(new int[]{nr, nc});
                        // En passant
                        if (nc == enPassantCol && nr == enPassantRow)
                            moves.add(new int[]{nr, nc});
                    }
                }
                break;
            }
            case ChessPiece.KNIGHT: {
                int[][] km = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
                for (int[] m : km) {
                    int nr = row + m[0], nc = col + m[1];
                    if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8 && board[nr][nc].color != color)
                        moves.add(new int[]{nr, nc});
                }
                break;
            }
            case ChessPiece.BISHOP: {
                int[][] dirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
                addSlidingMoves(moves, row, col, dirs, color);
                break;
            }
            case ChessPiece.ROOK: {
                int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
                addSlidingMoves(moves, row, col, dirs, color);
                break;
            }
            case ChessPiece.QUEEN: {
                int[][] dirs = {{-1,-1},{-1,1},{1,-1},{1,1},{-1,0},{1,0},{0,-1},{0,1}};
                addSlidingMoves(moves, row, col, dirs, color);
                break;
            }
            case ChessPiece.KING: {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = row + dr, nc = col + dc;
                        if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8 && board[nr][nc].color != color)
                            moves.add(new int[]{nr, nc});
                    }
                }
                // Roque
                if (!isInCheck(color)) {
                    if (color == ChessPiece.WHITE && row == 7) {
                        if (whiteKingsideCastle && board[7][5].isEmpty() && board[7][6].isEmpty()
                                && !isAttacked(7,5,opponent) && !isAttacked(7,6,opponent))
                            moves.add(new int[]{7, 6});
                        if (whiteQueensideCastle && board[7][3].isEmpty() && board[7][2].isEmpty() && board[7][1].isEmpty()
                                && !isAttacked(7,3,opponent) && !isAttacked(7,2,opponent))
                            moves.add(new int[]{7, 2});
                    }
                    if (color == ChessPiece.BLACK && row == 0) {
                        if (blackKingsideCastle && board[0][5].isEmpty() && board[0][6].isEmpty()
                                && !isAttacked(0,5,opponent) && !isAttacked(0,6,opponent))
                            moves.add(new int[]{0, 6});
                        if (blackQueensideCastle && board[0][3].isEmpty() && board[0][2].isEmpty() && board[0][1].isEmpty()
                                && !isAttacked(0,3,opponent) && !isAttacked(0,2,opponent))
                            moves.add(new int[]{0, 2});
                    }
                }
                break;
            }
        }
        return moves;
    }

    private void addSlidingMoves(List<int[]> moves, int row, int col, int[][] dirs, int color) {
        for (int[] d : dirs) {
            int nr = row + d[0], nc = col + d[1];
            while (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                if (board[nr][nc].isEmpty()) {
                    moves.add(new int[]{nr, nc});
                } else {
                    if (board[nr][nc].color != color) moves.add(new int[]{nr, nc});
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }
    }

    // Verifica se a cor atual tem algum movimento legal
    public boolean hasAnyLegalMove() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c].color == currentTurn && !getLegalMoves(r, c).isEmpty())
                    return true;
        return false;
    }

    // Converte posição do tabuleiro para UCI: row,col -> "e4"
    public String squareToUci(int row, int col) {
        return "" + (char)('a' + col) + (8 - row);
    }

    public String moveToUci(int fromRow, int fromCol, int toRow, int toCol) {
        return squareToUci(fromRow, fromCol) + squareToUci(toRow, toCol);
    }
}
