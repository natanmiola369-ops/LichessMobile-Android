package com.lichess.mobile.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.lichess.mobile.model.GameState;
import com.lichess.mobile.service.SoundService;

public class MainActivity extends AppCompatActivity {

    private FrameLayout container;
    private HomeScreen homeScreen;
    private GameScreen gameScreen;
    private OfflineGameScreen offlineScreen;

    // Qual tela está ativa
    private static final int SCREEN_HOME    = 0;
    private static final int SCREEN_GAME    = 1;
    private static final int SCREEN_OFFLINE = 2;
    private int currentScreen = SCREEN_HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        container = new FrameLayout(this);
        container.setId(View.generateViewId());
        setContentView(container);

        // Inicializa sons
        SoundService.getInstance(this);

        showHome();
    }

    private void showHome() {
        currentScreen = SCREEN_HOME;
        container.removeAllViews();

        homeScreen = new HomeScreen(this);
        homeScreen.setOnStartGame(new HomeScreen.OnStartGame() {
            public void onStartGame(GameState state) {
                if (state.gameId != null) {
                    showGame(state);
                } else {
                    showOffline();
                }
            }
        });

        container.addView(homeScreen, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void showGame(GameState state) {
        currentScreen = SCREEN_GAME;
        container.removeAllViews();

        gameScreen = new GameScreen(this, state);
        gameScreen.setOnBackListener(new GameScreen.OnBackListener() {
            public void onBack() { showHome(); }
        });

        container.addView(gameScreen, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void showOffline() {
        currentScreen = SCREEN_OFFLINE;
        container.removeAllViews();

        offlineScreen = new OfflineGameScreen(this);
        offlineScreen.setOnBackListener(new OfflineGameScreen.OnBackListener() {
            public void onBack() { showHome(); }
        });

        container.addView(offlineScreen, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void onBackPressed() {
        if (currentScreen != SCREEN_HOME) {
            showHome();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SoundService.getInstance(this).release();
    }
}
