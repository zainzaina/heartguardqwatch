package com.zain.h59heartguard;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public final class DndPermissions {
    private DndPermissions() {
    }

    public static boolean isPolicyAccessGranted(Context context) {
        return manager(context).isNotificationPolicyAccessGranted();
    }

    public static boolean isDndActive(Context context) {
        return manager(context).getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL;
    }

    public static Intent policyAccessSettingsIntent() {
        return new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
    }

    public static Intent batterySettingsIntent() {
        return new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
    }

    private static NotificationManager manager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
