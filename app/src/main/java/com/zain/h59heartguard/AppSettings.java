package com.zain.h59heartguard;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppSettings {
    public static final int HIGH_MIN = 80, HIGH_MAX = 200;
    public static final int LOW_MIN = 30, LOW_MAX = 90;
    public static final int CONFIRM_MIN = 1, CONFIRM_MAX = 10;
    public static final int DURATION_MIN = 0, DURATION_MAX = 60;
    public static final int COOLDOWN_MIN = 1, COOLDOWN_MAX = 60;
    public static final int REPEAT_MIN = 1, REPEAT_MAX = 3;
    public static final int REPEAT_DELAY_MIN = 3, REPEAT_DELAY_MAX = 30;

    private static final String PREFS = "h59_settings";
    private static final String KEY_HIGH = "high_bpm";
    private static final String KEY_LOW = "low_bpm";
    private static final String KEY_CONFIRM = "confirm_readings";
    private static final String KEY_DURATION = "duration_sec";
    private static final String KEY_COOLDOWN = "cooldown_min";
    private static final String KEY_REPEATS = "repeat_count";
    private static final String KEY_REPEAT_DELAY = "repeat_delay_sec";
    private static final String KEY_MEDICATION = "medication_wording";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_VIBRATION = "vibration_enabled";
    private static final String KEY_SIMULATION = "simulation_mode";
    private static final String KEY_AUDIO_PATH = "custom_audio_path";
    private static final String KEY_MONITORING = "monitoring_on";

    private final SharedPreferences prefs;

    public AppSettings(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public HeartRateAlertEngine.Config alertConfig() {
        return new HeartRateAlertEngine.Config(
                getHighBpm(),
                getLowBpm(),
                getConfirmReadings(),
                getDurationSec() * 1000L,
                getCooldownMin() * 60_000L);
    }

    public int getHighBpm() { return prefs.getInt(KEY_HIGH, 110); }
    public void setHighBpm(int value) { prefs.edit().putInt(KEY_HIGH, value).apply(); }

    public int getLowBpm() { return prefs.getInt(KEY_LOW, 50); }
    public void setLowBpm(int value) { prefs.edit().putInt(KEY_LOW, value).apply(); }

    public int getConfirmReadings() { return prefs.getInt(KEY_CONFIRM, 3); }
    public void setConfirmReadings(int value) { prefs.edit().putInt(KEY_CONFIRM, value).apply(); }

    public int getDurationSec() { return prefs.getInt(KEY_DURATION, 5); }
    public void setDurationSec(int value) { prefs.edit().putInt(KEY_DURATION, value).apply(); }

    public int getCooldownMin() { return prefs.getInt(KEY_COOLDOWN, 10); }
    public void setCooldownMin(int value) { prefs.edit().putInt(KEY_COOLDOWN, value).apply(); }

    public int getRepeatCount() { return prefs.getInt(KEY_REPEATS, 3); }
    public void setRepeatCount(int value) { prefs.edit().putInt(KEY_REPEATS, value).apply(); }

    public int getRepeatDelaySec() { return prefs.getInt(KEY_REPEAT_DELAY, 8); }
    public void setRepeatDelaySec(int value) { prefs.edit().putInt(KEY_REPEAT_DELAY, value).apply(); }

    public boolean isMedicationWording() { return prefs.getBoolean(KEY_MEDICATION, true); }
    public void setMedicationWording(boolean value) { prefs.edit().putBoolean(KEY_MEDICATION, value).apply(); }

    public boolean isSoundEnabled() { return prefs.getBoolean(KEY_SOUND, true); }
    public void setSoundEnabled(boolean value) { prefs.edit().putBoolean(KEY_SOUND, value).apply(); }

    public boolean isVibrationEnabled() { return prefs.getBoolean(KEY_VIBRATION, true); }
    public void setVibrationEnabled(boolean value) { prefs.edit().putBoolean(KEY_VIBRATION, value).apply(); }

    public boolean isSimulationMode() { return prefs.getBoolean(KEY_SIMULATION, false); }
    public void setSimulationMode(boolean value) { prefs.edit().putBoolean(KEY_SIMULATION, value).apply(); }

    public String getCustomAudioPath() { return prefs.getString(KEY_AUDIO_PATH, null); }
    public void setCustomAudioPath(String path) { prefs.edit().putString(KEY_AUDIO_PATH, path).apply(); }

    public boolean isMonitoringOn() { return prefs.getBoolean(KEY_MONITORING, false); }
    public void setMonitoringOn(boolean value) { prefs.edit().putBoolean(KEY_MONITORING, value).apply(); }
}
