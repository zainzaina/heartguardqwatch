package com.zain.h59heartguard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

final class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {
    private final List<AlertEvent> items = new ArrayList<>();

    void submit(List<AlertEvent> events) {
        items.clear();
        items.addAll(events);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final TextView bpm, type, details, ack;

        Holder(View view) {
            super(view);
            bpm = view.findViewById(R.id.itemBpm);
            type = view.findViewById(R.id.itemType);
            details = view.findViewById(R.id.itemDetails);
            ack = view.findViewById(R.id.itemAck);
        }

        void bind(AlertEvent event) {
            android.content.Context context = itemView.getContext();
            bpm.setText(String.valueOf(event.getBpm()));
            bpm.setTextColor(ContextCompat.getColor(context, event.isHigh() ? R.color.red_400 : R.color.blue_300));
            type.setText(event.isHigh() ? R.string.history_type_high : R.string.history_type_low);
            details.setText(context.getString(R.string.history_details,
                    event.getThresholdBpm(), AlertEvent.formatTimestamp(event.getTimestampMs())));
            ack.setText(event.isAcknowledged() ? R.string.history_acknowledged : R.string.history_not_acknowledged);
            ack.setTextColor(ContextCompat.getColor(context,
                    event.isAcknowledged() ? R.color.green_400 : R.color.amber_400));
        }
    }
}
