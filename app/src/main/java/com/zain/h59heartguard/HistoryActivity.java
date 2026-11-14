package com.zain.h59heartguard;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {
    private static final int HISTORY_LIMIT = 200;
    private static final String CSV_HEADER = "id,timestamp,bpm,threshold_bpm,type,acknowledged\n";

    private AlertHistoryDb history;
    private HistoryAdapter adapter;
    private View emptyText;
    private MaterialButton exportButton;

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"),
            this::onExportTarget);

    @Override protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_history);
        history = new AlertHistoryDb(this);
        adapter = new HistoryAdapter();
        RecyclerView list = findViewById(R.id.historyList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        emptyText = findViewById(R.id.historyEmpty);
        exportButton = findViewById(R.id.btnExport);
        exportButton.setOnClickListener(v -> exportLauncher.launch(exportFileName()));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override protected void onResume() {
        super.onResume();
        List<AlertEvent> events = history.recent(HISTORY_LIMIT);
        adapter.submit(events);
        emptyText.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
        exportButton.setEnabled(!events.isEmpty());
    }

    @Override protected void onDestroy() {
        history.close();
        super.onDestroy();
    }

    private void onExportTarget(Uri uri) {
        if (uri == null) return;
        String csv = buildCsv(history.recent(Integer.MAX_VALUE));
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IOException("No output stream");
            out.write(csv.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, R.string.export_done, Toast.LENGTH_SHORT).show();
        } catch (IOException | SecurityException e) {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_LONG).show();
        }
    }

    private static String buildCsv(List<AlertEvent> events) {
        StringBuilder csv = new StringBuilder(CSV_HEADER);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        for (AlertEvent event : events) {
            csv.append(event.getId()).append(',')
                    .append(format.format(new Date(event.getTimestampMs()))).append(',')
                    .append(event.getBpm()).append(',')
                    .append(event.getThresholdBpm()).append(',')
                    .append(event.getType()).append(',')
                    .append(event.isAcknowledged() ? 1 : 0).append('\n');
        }
        return csv.toString();
    }

    private static String exportFileName() {
        return "h59_alerts_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date()) + ".csv";
    }
}
