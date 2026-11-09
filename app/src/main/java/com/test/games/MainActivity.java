package com.test.games;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * MUNDO ABERTO 3D (OpenGL ES 2.0) - tudo neste arquivo.
 *
 * Recursos:
 *  - Terreno infinito procedural (montanhas, colinas, lagos) gerado em chunks
 *  - Iluminação com sol, lua, luz ambiente, neblina e ciclo dia/noite
 *  - Céu com nuvens animadas, estrelas, sol e lua
 *  - Água com ondas, reflexo do céu e brilho do sol
 *  - Árvores (pinheiros e carvalhos), arbustos e pedras com colisão
 *  - Joystick flutuante analógico + olhar por arrasto + botão de pulo
 *  - Câmera em 1ª, 3ª (atrás) e 2ª pessoa (na frente), com avatar do jogador
 *  - Mão em primeira pessoa
 *  - Tela de entrada com 2 mundos: Mundo Aberto e Mundo de Teste (personagem demo com auras)
 *  - Partículas (discos macios) + auras em TABELA (19 prontas + 1 aleatória infinita)
 *  - PODERES: bola de fogo, meteoro, chuva de meteoros, raio do céu, nova de gelo, terremoto,
 *    tornado, laser, escudo, buraco negro, cura e explosão (funcionam nos dois mundos)
 *
 * Organização do arquivo:
 *   1. MainActivity (ciclo de vida)
 *   2. Config  (todos os números ajustáveis)
 *   3. Util / Noise / Terrain (matemática e geração do mundo)
 *   4. MeshBuilder (monta árvores, pedras...)
 *   5. GameState (jogador, física, entrada)
 *   6. Shaders
 *   7. WorldRenderer (OpenGL)
 *   8. (dentro do WorldRenderer) personagem, mão, câmera e arena de teste
 *   9. Particles / Auras / AuraFX / Spells
 *  10. HudView (joystick e botões)  11. LoginView (escolha do mundo)
 */
public class MainActivity extends Activity {

    private GameState game;
    private GLSurfaceView glView;

    // =========================================================
    // 1. CICLO DE VIDA
    // =========================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        int flags = WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        getWindow().setFlags(flags, flags);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        game = new GameState();
        Sfx.init();

        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setEGLConfigChooser(8, 8, 8, 0, 24, 0);
        glView.setRenderer(new WorldRenderer(game));
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        if (Config.RENDER_SCALE < 0.99f) {
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(dm);
            int w = Math.max(dm.widthPixels, dm.heightPixels);
            int h = Math.min(dm.widthPixels, dm.heightPixels);
            glView.getHolder().setFixedSize((int) (w * Config.RENDER_SCALE), (int) (h * Config.RENDER_SCALE));
        }

        FrameLayout root = new FrameLayout(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        root.addView(glView, lp);
        root.addView(new HudView(this, game), lp);
        root.addView(new LoginView(this, game), lp);
        setContentView(root);
        // se outra tela (login de verdade) abrir esta Activity com putExtra("mundo", 0 ou 1), pula o menu
        int start = getIntent() != null ? getIntent().getIntExtra("mundo", -1) : -1;
        if (start == 0 || start == 1) game.enterWorld(start);
        hideSystemUi();
        unlockFps();
    }

    /** Pede a maior taxa de atualizacao da tela (90/120/144 Hz) quando o aparelho tem. */
    private void unlockFps() {
        if (!Config.UNLOCK_FPS) return;
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                Display d = getWindowManager().getDefaultDisplay();
                Display.Mode cur = d.getMode();
                Display.Mode best = cur;
                for (Display.Mode m : d.getSupportedModes()) {
                    if (m.getPhysicalWidth() == cur.getPhysicalWidth() && m.getPhysicalHeight() == cur.getPhysicalHeight()
                            && m.getRefreshRate() > best.getRefreshRate()) best = m;
                }
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.preferredDisplayModeId = best.getModeId();
                lp.preferredRefreshRate = best.getRefreshRate();
                getWindow().setAttributes(lp);
            }
        } catch (Throwable ignored) {
        }
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glView.onPause();
        Sfx.setPaused(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
        Sfx.setPaused(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Sfx.release();
    }

    // NOTE: Full source is very large. The complete original file must be uploaded by the user or via a more suitable method.
    // The rest of the classes (Config, Util, Noise, Terrain, MeshBuilder, GameState, Shaders, WorldRenderer, Particles, Auras, Spells, Enemies, Sfx, HudView, LoginView) are in the original MainActivity.java provided by the user.
}
