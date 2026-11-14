package com.zain.h59heartguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;

public final class AlertNotifier {
    public static final int MONITOR_NOTIFICATION_ID = 42;
    public static final int ALERT_NOTIFICATION_ID = 43;
    private static final String MONITOR_CHANNEL_ID = "h59_monitor";
    private static final String ALERT_CHANNEL_ID = "h59_alerts";

    private final Context context;
    private final NotificationManager manager;

    public AlertNotifier(Context context) {
        this.context = context.getApplicationContext();
        manager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
    }

    private void createChannels() {
        NotificationChannel monitor = new NotificationChannel(
                MONITOR_CHANNEL_ID,
                context.getString(R.string.channel_monitor),
                NotificationManager.IMPORTANCE_LOW);

        NotificationChannel alerts = new NotificationChannel(
                ALERT_CHANNEL_ID,
                context.getString(R.string.channel_alerts),
                NotificationManager.IMPORTANCE_HIGH);
        alerts.setDescription(context.getString(R.string.channel_alerts_desc));
        alerts.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        alerts.enableVibration(true);
        alerts.setBypassDnd(true);

        manager.createNotificationChannel(monitor);
        manager.createNotificationChannel(alerts);
    }

    public Notification monitorNotification(String text) {
        return new Notification.Builder(context, MONITOR_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_heart)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(openAppIntent())
                .build();
    }

    public void showMonitorStatus(String text) {
        manager.notify(MONITOR_NOTIFICATION_ID, monitorNotification(text));
    }

    public void showAlert(long alertId, boolean high, int bpm, boolean medicationWording) {
        String title = context.getString(high ? R.string.alert_high_title : R.string.alert_low_title);
        String body;
        if (!high) {
            body = context.getString(R.string.alert_low_body, bpm);
        } else if (medicationWording) {
            body = context.getString(R.string.alert_high_body_medication, bpm);
        } else {
            body = context.getString(R.string.alert_high_body_neutral, bpm);
        }

        Notification.Action acknowledge = new Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_heart),
                context.getString(R.string.action_acknowledge),
                acknowledgePendingIntent(alertId)).build();

        Notification notification = new Notification.Builder(context, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_heart)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setContentIntent(openAppIntent())
                .addAction(acknowledge)
                .build();
        manager.notify(ALERT_NOTIFICATION_ID, notification);
    }

    public void cancelAlert() {
        manager.cancel(ALERT_NOTIFICATION_ID);
    }

    private PendingIntent openAppIntent() {
        return PendingIntent.getActivity(
                context,
                0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent acknowledgePendingIntent(long alertId) {
        Intent intent = new Intent(context, H59MonitorService.class)
                .setAction(H59MonitorService.ACTION_ACKNOWLEDGE)
                .putExtra(H59MonitorService.EXTRA_ALERT_ID, alertId);
        return PendingIntent.getService(
                context,
                (int) alertId,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
