package com.test.games;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.Window;

/**
 * PLACEHOLDER - O projeto compila e gera APK.
 *
 * Para ter o jogo completo (mundo aberto, auras, poderes, etc):
 * 1. Baixe o MainActivity.java original completo
 * 2. Substitua este arquivo em:
 *    app/src/main/java/com/test/games/MainActivity.java
 * 3. Faça commit e rode o workflow Build APK de novo.
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        TextView tv = new TextView(this);
        tv.setText("MUNDO ABERTO 3D\n\n" +
                "Este e um APK de teste.\n\n" +
                "Para o jogo completo, substitua o\n" +
                "MainActivity.java pelo arquivo original\n" +
                "completo e rode o Build APK novamente.\n\n" +
                "Repositorio:\n" +
                "github.com/mrbarros032-gif/mundo-aberto");
        tv.setTextSize(16);
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundColor(Color.rgb(15, 20, 35));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(50, 50, 50, 50);
        setContentView(tv);
    }
}
