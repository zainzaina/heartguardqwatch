package com.zain.h59heartguard;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import java.util.function.IntFunction;

public class SettingsActivity extends AppCompatActivity {
    private AppSettings settings;
    private Slider highSlider, lowSlider, confirmSlider, durationSlider, cooldownSlider, repeatsSlider, repeatDelaySlider;
    private MaterialSwitch medicationSwitch, soundSwitch, vibrationSwitch, simulationSwitch;
    private TextView errorText;

    @Override protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_settings);
        settings = new AppSettings(this);

        highSlider = bindSlider(R.id.highSlider, R.id.highValue, AppSettings.HIGH_MIN, AppSettings.HIGH_MAX,
                settings.getHighBpm(), v -> getString(R.string.value_bpm, v));
        lowSlider = bindSlider(R.id.lowSlider, R.id.lowValue, AppSettings.LOW_MIN, AppSettings.LOW_MAX,
                settings.getLowBpm(), v -> getString(R.string.value_bpm, v));
        confirmSlider = bindSlider(R.id.confirmSlider, R.id.confirmValue, AppSettings.CONFIRM_MIN, AppSettings.CONFIRM_MAX,
                settings.getConfirmReadings(), v -> getString(R.string.value_count, v));
        durationSlider = bindSlider(R.id.durationSlider, R.id.durationValue, AppSettings.DURATION_MIN, AppSettings.DURATION_MAX,
                settings.getDurationSec(), v -> getString(R.string.value_seconds, v));
        cooldownSlider = bindSlider(R.id.cooldownSlider, R.id.cooldownValue, AppSettings.COOLDOWN_MIN, AppSettings.COOLDOWN_MAX,
                settings.getCooldownMin(), v -> getString(R.string.value_minutes, v));
        repeatsSlider = bindSlider(R.id.repeatsSlider, R.id.repeatsValue, AppSettings.REPEAT_MIN, AppSettings.REPEAT_MAX,
                settings.getRepeatCount(), v -> getString(R.string.value_times, v));
        repeatDelaySlider = bindSlider(R.id.repeatDelaySlider, R.id.repeatDelayValue, AppSettings.REPEAT_DELAY_MIN,
                AppSettings.REPEAT_DELAY_MAX, settings.getRepeatDelaySec(), v -> getString(R.string.value_seconds, v));

        medicationSwitch = findViewById(R.id.switchMedication);
        soundSwitch = findViewById(R.id.switchSound);
        vibrationSwitch = findViewById(R.id.switchVibration);
        simulationSwitch = findViewById(R.id.switchSimulation);
        medicationSwitch.setChecked(settings.isMedicationWording());
        soundSwitch.setChecked(settings.isSoundEnabled());
        vibrationSwitch.setChecked(settings.isVibrationEnabled());
        simulationSwitch.setChecked(settings.isSimulationMode());

        errorText = findViewById(R.id.errorText);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> save());
    }

    private Slider bindSlider(int sliderId, int valueId, int min, int max, int value, IntFunction<String> format) {
        Slider slider = findViewById(sliderId);
        TextView label = findViewById(valueId);
        slider.setValueFrom(min);
        slider.setValueTo(max);
        slider.setStepSize(1f);
        slider.setValue(value);
        label.setText(format.apply(value));
        slider.addOnChangeListener((s, v, fromUser) -> label.setText(format.apply(Math.round(v))));
        return slider;
    }

    private void save() {
        int high = Math.round(highSlider.getValue());
        int low = Math.round(lowSlider.getValue());
        if (low >= high) {
            errorText.setVisibility(View.VISIBLE);
            return;
        }
        errorText.setVisibility(View.GONE);
        settings.setHighBpm(high);
        settings.setLowBpm(low);
        settings.setConfirmReadings(Math.round(confirmSlider.getValue()));
        settings.setDurationSec(Math.round(durationSlider.getValue()));
        settings.setCooldownMin(Math.round(cooldownSlider.getValue()));
        settings.setRepeatCount(Math.round(repeatsSlider.getValue()));
        settings.setRepeatDelaySec(Math.round(repeatDelaySlider.getValue()));
        settings.setMedicationWording(medicationSwitch.isChecked());
        settings.setSoundEnabled(soundSwitch.isChecked());
        settings.setVibrationEnabled(vibrationSwitch.isChecked());
        settings.setSimulationMode(simulationSwitch.isChecked());
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        finish();
    }
}
