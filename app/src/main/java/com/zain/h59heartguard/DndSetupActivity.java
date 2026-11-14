package com.zain.h59heartguard;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

public class DndSetupActivity extends AppCompatActivity {
    private TextView dndState, policyState;
    private MaterialButton policyButton;

    @Override protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_dnd);
        dndState = findViewById(R.id.dndState);
        policyState = findViewById(R.id.policyState);
        policyButton = findViewById(R.id.btnPolicy);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        policyButton.setOnClickListener(v -> openSettings(DndPermissions.policyAccessSettingsIntent()));
        findViewById(R.id.btnBattery).setOnClickListener(v -> openSettings(DndPermissions.batterySettingsIntent()));
    }

    @Override protected void onResume() {
        super.onResume();
        dndState.setText(DndPermissions.isDndActive(this) ? R.string.dnd_active : R.string.dnd_inactive);
        boolean granted = DndPermissions.isPolicyAccessGranted(this);
        policyState.setText(granted ? R.string.policy_granted : R.string.policy_not_granted);
        policyState.setTextColor(ContextCompat.getColor(this, granted ? R.color.green_400 : R.color.red_400));
        policyButton.setText(granted ? R.string.policy_manage : R.string.policy_grant);
    }

    private void openSettings(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.settings_not_available, Toast.LENGTH_LONG).show();
        }
    }
}
