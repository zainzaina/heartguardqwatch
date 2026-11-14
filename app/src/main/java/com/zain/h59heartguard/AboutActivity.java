package com.zain.h59heartguard;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_about);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
