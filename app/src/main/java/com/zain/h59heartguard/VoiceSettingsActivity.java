package com.zain.h59heartguard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class VoiceSettingsActivity extends AppCompatActivity {
    private static final long MAX_AUDIO_BYTES = 5L * 1024 * 1024;
    private static final int BUFFER_SIZE = 8192;
    private static final String STAGED_FILE = "staged_alert_audio";
    private static final String CUSTOM_FILE = "custom_alert_audio";

    private AppSettings settings;
    private AlertVoicePlayer player;
    private File stagedFile;
    private File customFile;
    private boolean defaultPending;
    private TextView currentLabel, pendingLabel, messageText, errorText;

    private final ActivityResultLauncher<String[]> pickAudio = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::onAudioPicked);

    @Override protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_voice);
        settings = new AppSettings(this);
        player = new AlertVoicePlayer(this);
        stagedFile = new File(getFilesDir(), STAGED_FILE);
        customFile = new File(getFilesDir(), CUSTOM_FILE);
        stagedFile.delete();

        currentLabel = findViewById(R.id.voiceCurrent);
        pendingLabel = findViewById(R.id.voicePending);
        messageText = findViewById(R.id.voiceMessage);
        errorText = findViewById(R.id.voiceError);
        messageText.setText(AlertVoicePlayer.message(this, true, settings.isMedicationWording()));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPick).setOnClickListener(v ->
                pickAudio.launch(new String[]{"audio/mpeg", "audio/wav", "audio/x-wav"}));
        findViewById(R.id.btnPreview).setOnClickListener(v -> preview());
        findViewById(R.id.btnDefault).setOnClickListener(v -> useDefault());
        findViewById(R.id.btnSaveVoice).setOnClickListener(v -> save());
        renderCurrent();
    }

    @Override protected void onDestroy() {
        player.release();
        super.onDestroy();
    }

    private void onAudioPicked(Uri uri) {
        if (uri == null) return;
        if (!copyToStaging(uri)) {
            stagedFile.delete();
            showError();
            renderCurrent();
            return;
        }
        defaultPending = false;
        errorText.setVisibility(View.GONE);
        renderCurrent();
    }

    private boolean copyToStaging(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(stagedFile)) {
            if (in == null) return false;
            byte[] buffer = new byte[BUFFER_SIZE];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_AUDIO_BYTES) return false;
                out.write(buffer, 0, read);
            }
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        }
    }

    private void preview() {
        String path = null;
        if (!defaultPending) {
            if (stagedFile.exists()) path = stagedFile.getAbsolutePath();
            else if (settings.getCustomAudioPath() != null && customFile.exists()) path = customFile.getAbsolutePath();
        }
        player.play(path, AlertVoicePlayer.message(this, true, settings.isMedicationWording()), () -> { });
    }

    private void useDefault() {
        defaultPending = true;
        stagedFile.delete();
        errorText.setVisibility(View.GONE);
        renderCurrent();
    }

    private void save() {
        if (defaultPending) {
            settings.setCustomAudioPath(null);
            customFile.delete();
        } else if (stagedFile.exists()) {
            if (!stagedFile.renameTo(customFile)) {
                showError();
                return;
            }
            settings.setCustomAudioPath(customFile.getAbsolutePath());
        }
        Toast.makeText(this, R.string.voice_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void renderCurrent() {
        boolean hasCustom = settings.getCustomAudioPath() != null && customFile.exists();
        currentLabel.setText(hasCustom ? R.string.voice_current_custom : R.string.voice_current_default);
        if (defaultPending) {
            pendingLabel.setText(R.string.voice_pending_default);
            pendingLabel.setVisibility(View.VISIBLE);
        } else if (stagedFile.exists()) {
            pendingLabel.setText(R.string.voice_pending_file);
            pendingLabel.setVisibility(View.VISIBLE);
        } else {
            pendingLabel.setVisibility(View.GONE);
        }
    }

    private void showError() {
        errorText.setText(R.string.voice_error_file);
        errorText.setVisibility(View.VISIBLE);
    }
}
