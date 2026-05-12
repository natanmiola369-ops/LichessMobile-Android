package com.lichess.mobile.service;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LichessApi {

    private static final String BASE = "https://lichess.org";
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler  = new Handler(Looper.getMainLooper());
    private String token = null;

    private static LichessApi instance;
    public static LichessApi getInstance() {
        if (instance == null) instance = new LichessApi();
        return instance;
    }

    public void setToken(String token) { this.token = token; }
    public String getToken() { return token; }
    public boolean isLoggedIn() { return token != null && !token.isEmpty(); }

    public interface Callback {
        void onSuccess(JSONObject json);
        void onError(String error);
    }

    public interface StreamCallback {
        void onLine(String line);
        void onDone();
        void onError(String error);
    }

    private static final String UA = "LichessMobile/1.0 Android (github.com/lichess-org/mobile)";

    // GET simples
    private void get(final String path, final Callback cb) {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    URL url = new URL(BASE + path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestProperty("User-Agent", UA);
                    if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();
                        final JSONObject json = new JSONObject(sb.toString());
                        mainHandler.post(new Runnable() { public void run() { cb.onSuccess(json); } });
                    } else {
                        String errBody = "";
                        try {
                            java.io.InputStream es = conn.getErrorStream();
                            if (es != null) {
                                BufferedReader br2 = new BufferedReader(new InputStreamReader(es, "UTF-8"));
                                StringBuilder sb2 = new StringBuilder();
                                String l2;
                                while ((l2 = br2.readLine()) != null) sb2.append(l2);
                                br2.close();
                                errBody = sb2.toString();
                            }
                        } catch (Exception ignored) {}
                        final String err = "HTTP " + code + (errBody.isEmpty() ? "" : " | " + errBody);
                        mainHandler.post(new Runnable() { public void run() { cb.onError(err); } });
                    }
                    conn.disconnect();
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() { public void run() { cb.onError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()); } });
                }
            }
        });
    }

    // POST com body
    private void post(final String path, final String body, final Callback cb) {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    URL url = new URL(BASE + path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setRequestProperty("User-Agent", UA);
                    if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);

                    if (body != null && !body.isEmpty()) {
                        OutputStream os = conn.getOutputStream();
                        os.write(body.getBytes("UTF-8"));
                        os.close();
                    }

                    int code = conn.getResponseCode();
                    if (code == 200 || code == 201) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();
                        final JSONObject json = new JSONObject(sb.toString());
                        mainHandler.post(new Runnable() { public void run() { cb.onSuccess(json); } });
                    } else {
                        String errBody = "";
                        try {
                            java.io.InputStream es = conn.getErrorStream();
                            if (es != null) {
                                BufferedReader br2 = new BufferedReader(new InputStreamReader(es, "UTF-8"));
                                StringBuilder sb2 = new StringBuilder();
                                String l2;
                                while ((l2 = br2.readLine()) != null) sb2.append(l2);
                                br2.close();
                                errBody = sb2.toString();
                            }
                        } catch (Exception ignored) {}
                        final String err = "HTTP " + code + (errBody.isEmpty() ? "" : " | " + errBody);
                        mainHandler.post(new Runnable() { public void run() { cb.onError(err); } });
                    }
                    conn.disconnect();
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() { public void run() { cb.onError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()); } });
                }
            }
        });
    }

    // Stream NDJSON (para eventos de jogo em tempo real)
    public void stream(final String path, final StreamCallback cb) {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    URL url = new URL(BASE + path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/x-ndjson");
                    conn.setRequestProperty("User-Agent", UA);
                    if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(120000); // 2 min timeout

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line;
                        while ((line = br.readLine()) != null) {
                            final String l = line;
                            if (!l.trim().isEmpty()) {
                                mainHandler.post(new Runnable() { public void run() { cb.onLine(l); } });
                            }
                        }
                        br.close();
                        mainHandler.post(new Runnable() { public void run() { cb.onDone(); } });
                    } else {
                        final String err = "HTTP " + code;
                        mainHandler.post(new Runnable() { public void run() { cb.onError(err); } });
                    }
                    conn.disconnect();
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() { public void run() { cb.onError(e.getMessage()); } });
                }
            }
        });
    }

    // === API endpoints ===

    public void getProfile(Callback cb) {
        get("/api/account", cb);
    }

    // Cria jogo contra Stockfish
    public void playStockfish(int level, String clock, Callback cb) {
        // level 1-8, clock ex: "5+0" (minutos+incremento)
        String[] parts = clock.split("\\+");
        int limit = Integer.parseInt(parts[0]) * 60;
        int incr  = Integer.parseInt(parts[1]);
        post("/api/challenge/ai",
             "level=" + level + "&clock.limit=" + limit + "&clock.increment=" + incr + "&color=random",
             cb);
    }

    // Faz uma jogada
    public void makeMove(String gameId, String uciMove, Callback cb) {
        post("/api/board/game/" + gameId + "/move/" + uciMove, "", cb);
    }

    // Stream de estado do jogo
    public void streamGame(String gameId, StreamCallback cb) {
        stream("/api/board/game/stream/" + gameId, cb);
    }

    // Stream de eventos da conta (novos jogos, challenges, etc.)
    public void streamEvents(StreamCallback cb) {
        stream("/api/stream/event", cb);
    }

    // Busca jogos recentes do usuário
    public void getRecentGames(String username, int max, Callback cb) {
        get("/api/games/user/" + username + "?max=" + max + "&pgnInJson=false&clocks=false&evals=false", cb);
    }

    // Busca perfil de um usuário
    public void getUser(String username, Callback cb) {
        get("/api/user/" + username, cb);
    }

    // Aborta jogo
    public void abortGame(String gameId, Callback cb) {
        post("/api/board/game/" + gameId + "/abort", "", cb);
    }

    // Resigna jogo
    public void resignGame(String gameId, Callback cb) {
        post("/api/board/game/" + gameId + "/resign", "", cb);
    }

    // Propõe empate
    public void offerDraw(String gameId, Callback cb) {
        post("/api/board/game/" + gameId + "/draw/yes", "", cb);
    }

    // Leaderboard
    public void getLeaderboard(Callback cb) {
        get("/api/player", cb);
    }
}
