package com.zain.h59heartguard;

import android.text.format.DateFormat;

public final class AlertEvent {
    public static final String TYPE_HIGH = "HIGH";
    public static final String TYPE_LOW = "LOW";

    private final long id;
    private final long timestampMs;
    private final int bpm;
    private final int thresholdBpm;
    private final String type;
    private final boolean acknowledged;

    public AlertEvent(long id, long timestampMs, int bpm, int thresholdBpm, String type, boolean acknowledged) {
        this.id = id;
        this.timestampMs = timestampMs;
        this.bpm = bpm;
        this.thresholdBpm = thresholdBpm;
        this.type = type;
        this.acknowledged = acknowledged;
    }

    public long getId() { return id; }
    public long getTimestampMs() { return timestampMs; }
    public int getBpm() { return bpm; }
    public int getThresholdBpm() { return thresholdBpm; }
    public String getType() { return type; }
    public boolean isAcknowledged() { return acknowledged; }
    public boolean isHigh() { return TYPE_HIGH.equals(type); }

    public static String formatTimestamp(long timestampMs) {
        return DateFormat.format("dd/MM/yyyy  HH:mm", timestampMs).toString();
    }
}
