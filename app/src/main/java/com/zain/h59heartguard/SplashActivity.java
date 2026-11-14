package com.zain.h59heartguard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DELAY_MS = 2000L;

    @Override protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_splash);
        View logo = findViewById(R.id.logo);
        logo.setScaleX(0.8f);
        logo.setScaleY(0.8f);
        logo.setAlpha(0f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(900).start();
        new Handler(Looper.getMainLooper()).postDelayed(this::openDashboard, SPLASH_DELAY_MS);
    }

    private void openDashboard() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
