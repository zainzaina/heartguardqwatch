package com.zain.h59heartguard;

public final class HeartRateAlertEngine {
    private static final long MAX_READING_GAP_MS = 20_000L;

    public enum Event { NONE, HIGH, LOW }

    public static final class Config {
        final int highBpm;
        final int lowBpm;
        final int confirmReadings;
        final long minDurationMs;
        final long cooldownMs;

        public Config(int highBpm, int lowBpm, int confirmReadings, long minDurationMs, long cooldownMs) {
            this.highBpm = highBpm;
            this.lowBpm = lowBpm;
            this.confirmReadings = confirmReadings;
            this.minDurationMs = minDurationMs;
            this.cooldownMs = cooldownMs;
        }
    }

    private static final class Streak {
        int count;
        long startedAt;
        boolean alerted;

        void reset() {
            count = 0;
            alerted = false;
        }
    }

    private final Streak high = new Streak();
    private final Streak low = new Streak();
    private long lastReadingAt;
    private long lastAlertAt;

    public static boolean isOutOfRange(int bpm, Config config) {
        return bpm >= config.highBpm || bpm <= config.lowBpm;
    }

    public Event onReading(int bpm, long now, Config config) {
        if (lastReadingAt != 0 && now - lastReadingAt > MAX_READING_GAP_MS) {
            high.reset();
            low.reset();
        }
        lastReadingAt = now;

        if (bpm >= config.highBpm) {
            low.reset();
            return trigger(high, Event.HIGH, now, config);
        }
        if (bpm <= config.lowBpm) {
            high.reset();
            return trigger(low, Event.LOW, now, config);
        }
        high.reset();
        low.reset();
        return Event.NONE;
    }

    private Event trigger(Streak streak, Event event, long now, Config config) {
        if (streak.count == 0) streak.startedAt = now;
        streak.count++;
        boolean cooldownOver = lastAlertAt == 0 || now - lastAlertAt >= config.cooldownMs;
        if (streak.alerted
                || streak.count < config.confirmReadings
                || now - streak.startedAt < config.minDurationMs
                || !cooldownOver) {
            return Event.NONE;
        }
        streak.alerted = true;
        lastAlertAt = now;
        return event;
    }
}
