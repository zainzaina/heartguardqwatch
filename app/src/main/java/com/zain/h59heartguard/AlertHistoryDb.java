package com.zain.h59heartguard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public final class AlertHistoryDb extends SQLiteOpenHelper {
    private static final String NAME = "h59_history.db";
    private static final int VERSION = 1;
    private static final String TABLE = "alert_history";

    public AlertHistoryDb(Context context) {
        super(context.getApplicationContext(), NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "timestamp_ms INTEGER NOT NULL, "
                + "bpm INTEGER NOT NULL, "
                + "threshold_bpm INTEGER NOT NULL, "
                + "alert_type TEXT NOT NULL, "
                + "acknowledged INTEGER NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public long insert(long timestampMs, int bpm, int thresholdBpm, String type) {
        ContentValues values = new ContentValues();
        values.put("timestamp_ms", timestampMs);
        values.put("bpm", bpm);
        values.put("threshold_bpm", thresholdBpm);
        values.put("alert_type", type);
        return getWritableDatabase().insert(TABLE, null, values);
    }

    public void acknowledge(long id) {
        ContentValues values = new ContentValues();
        values.put("acknowledged", 1);
        getWritableDatabase().update(TABLE, values, "id = ?", new String[]{String.valueOf(id)});
    }

    public List<AlertEvent> recent(int limit) {
        List<AlertEvent> events = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE, null, null, null, null, null,
                "timestamp_ms DESC", String.valueOf(limit))) {
            while (c.moveToNext()) {
                events.add(new AlertEvent(
                        c.getLong(0),
                        c.getLong(1),
                        c.getInt(2),
                        c.getInt(3),
                        c.getString(4),
                        c.getInt(5) == 1));
            }
        }
        return events;
    }
}
