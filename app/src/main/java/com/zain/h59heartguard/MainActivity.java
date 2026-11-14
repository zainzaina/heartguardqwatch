package com.zain.h59heartguard;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private AppSettings settings;
    private AlertHistoryDb history;
    private TextView statusBadge, statusDescription, bpmValue, watchValue, thresholdValue, lastAlertValue, monitoringText, simulationBadge;
    private View heartIcon;
    private MaterialSwitch monitoringSwitch;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> onPermissionsResult());

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            renderState(intent);
        }
    };

    @Override protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_main);
        settings = new AppSettings(this);
        history = new AlertHistoryDb(this);

        statusBadge = findViewById(R.id.statusBadge);
        statusDescription = findViewById(R.id.statusDescription);
        bpmValue = findViewById(R.id.bpmValue);
        heartIcon = findViewById(R.id.heartIcon);
        watchValue = findViewById(R.id.watchValue);
        thresholdValue = findViewById(R.id.thresholdValue);
        lastAlertValue = findViewById(R.id.lastAlertValue);
        monitoringText = findViewById(R.id.monitoringText);
        simulationBadge = findViewById(R.id.simulationBadge);
        monitoringSwitch = findViewById(R.id.monitoringSwitch);

        monitoringSwitch.setOnClickListener(v -> onMonitoringToggled());
        findViewById(R.id.menuSettings).setOnClickListener(v -> open(SettingsActivity.class));
        findViewById(R.id.menuVoice).setOnClickListener(v -> open(VoiceSettingsActivity.class));
        findViewById(R.id.menuDnd).setOnClickListener(v -> open(DndSetupActivity.class));
        findViewById(R.id.menuHistory).setOnClickListener(v -> open(HistoryActivity.class));
        findViewById(R.id.menuAbout).setOnClickListener(v -> open(AboutActivity.class));
    }

    @Override protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(this, stateReceiver,
                new IntentFilter(H59MonitorService.ACTION_STATE), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override protected void onStop() {
        unregisterReceiver(stateReceiver);
        super.onStop();
    }

    @Override protected void onResume() {
        super.onResume();
        monitoringSwitch.setChecked(settings.isMonitoringOn());
        monitoringText.setText(settings.isMonitoringOn() ? R.string.monitoring_on : R.string.monitoring_off);
        thresholdValue.setText(getString(R.string.threshold_format, settings.getHighBpm()));
        simulationBadge.setVisibility(settings.isSimulationMode() ? View.VISIBLE : View.GONE);
        List<AlertEvent> latest = history.recent(1);
        lastAlertValue.setText(latest.isEmpty()
                ? getString(R.string.no_alert_yet)
                : AlertEvent.formatTimestamp(latest.get(0).getTimestampMs()));
    }

    @Override protected void onDestroy() {
        history.close();
        super.onDestroy();
    }

    private void renderState(Intent intent) {
        H59MonitorService.LinkState state = H59MonitorService.LinkState.valueOf(
                intent.getStringExtra(H59MonitorService.EXTRA_STATE));
        int bpm = intent.getIntExtra(H59MonitorService.EXTRA_BPM, 0);
        boolean warning = intent.getBooleanExtra(H59MonitorService.EXTRA_WARNING, false);
        long lastAlertAt = intent.getLongExtra(H59MonitorService.EXTRA_LAST_ALERT_AT, 0L);
        boolean live = state == H59MonitorService.LinkState.CONNECTED || state == H59MonitorService.LinkState.SIMULATING;
        boolean hasReading = live && bpm > 0;

        bpmValue.setText(hasReading ? String.valueOf(bpm) : "--");
        bpmValue.setTextColor(ContextCompat.getColor(this,
                warning && hasReading ? R.color.red_400 : R.color.text_primary));
        if (hasReading) pulseHeart();

        watchValue.setText(watchLabel(state, intent.getStringExtra(H59MonitorService.EXTRA_WATCH)));
        if (lastAlertAt > 0) lastAlertValue.setText(AlertEvent.formatTimestamp(lastAlertAt));
        renderStatus(state, warning);
    }

    private String watchLabel(H59MonitorService.LinkState state, String watch) {
        switch (state) {
            case CONNECTED: return watch;
            case SIMULATING: return getString(R.string.watch_simulated);
            default: return getString(R.string.watch_none);
        }
    }

    private void renderStatus(H59MonitorService.LinkState state, boolean warning) {
        int badge;
        int description;
        int color;
        switch (state) {
            case CONNECTED:
            case SIMULATING:
                badge = warning ? R.string.badge_warning : R.string.badge_normal;
                description = state == H59MonitorService.LinkState.SIMULATING ? R.string.desc_simulating
                        : warning ? R.string.desc_warning : R.string.desc_normal;
                color = warning ? R.color.red_400 : R.color.green_400;
                break;
            case CONNECTING:
                badge = R.string.badge_connecting;
                description = R.string.desc_connecting;
                color = R.color.amber_400;
                break;
            case DISCONNECTED:
                badge = R.string.badge_disconnected;
                description = R.string.desc_disconnected;
                color = R.color.amber_400;
                break;
            default:
                badge = R.string.badge_stopped;
                description = R.string.desc_stopped;
                color = R.color.text_muted;
                break;
        }
        statusBadge.setText(badge);
        statusBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, color)));
        statusDescription.setText(description);
    }

    private void pulseHeart() {
        heartIcon.animate().cancel();
        heartIcon.animate()
                .scaleX(1.15f).scaleY(1.15f).setDuration(120)
                .withEndAction(() -> heartIcon.animate().scaleX(1f).scaleY(1f).setDuration(260).start())
                .start();
    }

    private void onMonitoringToggled() {
        if (monitoringSwitch.isChecked()) {
            String[] missing = permissionsToRequest();
            if (missing.length == 0) startMonitoring();
            else permissionLauncher.launch(missing);
        } else {
            stopMonitoring();
        }
    }

    private void onPermissionsResult() {
        if (hasBluetoothPermissions()) {
            startMonitoring();
        } else {
            monitoringSwitch.setChecked(false);
            Toast.makeText(this, R.string.permissions_required, Toast.LENGTH_LONG).show();
        }
    }

    private void startMonitoring() {
        settings.setMonitoringOn(true);
        monitoringText.setText(R.string.monitoring_on);
        ContextCompat.startForegroundService(this,
                new Intent(this, H59MonitorService.class).setAction(H59MonitorService.ACTION_START));
    }

    private void stopMonitoring() {
        settings.setMonitoringOn(false);
        monitoringText.setText(R.string.monitoring_off);
        startService(new Intent(this, H59MonitorService.class).setAction(H59MonitorService.ACTION_STOP));
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return granted(Manifest.permission.BLUETOOTH_SCAN) && granted(Manifest.permission.BLUETOOTH_CONNECT);
        }
        return granted(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private String[] permissionsToRequest() {
        List<String> missing = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!granted(Manifest.permission.BLUETOOTH_SCAN)) missing.add(Manifest.permission.BLUETOOTH_SCAN);
            if (!granted(Manifest.permission.BLUETOOTH_CONNECT)) missing.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else if (!granted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !granted(Manifest.permission.POST_NOTIFICATIONS)) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return missing.toArray(new String[0]);
    }

    private boolean granted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void open(Class<?> screen) {
        startActivity(new Intent(this, screen));
    }
}
