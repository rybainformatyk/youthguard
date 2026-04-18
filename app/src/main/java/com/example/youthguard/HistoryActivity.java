package com.example.antyspamer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.historyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        loadHistory();
    }

    private void loadHistory() {
        List<DatabaseHelper.AlertItem> history = dbHelper.getAllHistory();
        recyclerView.setAdapter(new HistoryAdapter(history));
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<DatabaseHelper.AlertItem> list;

        public HistoryAdapter(List<DatabaseHelper.AlertItem> list) { this.list = list; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DatabaseHelper.AlertItem item = list.get(position);
            holder.keyword.setText(item.keyword.toUpperCase());
            holder.timestamp.setText(item.timestamp.toUpperCase() + " | LOG_ID: " + item.id);
            holder.context.setText(item.context);
            holder.status.setText(item.status);

            int color = ContextCompat.getColor(HistoryActivity.this, R.color.primary);
            
            if ("CONFIRMED".equals(item.status)) {
                color = ContextCompat.getColor(HistoryActivity.this, R.color.status_confirmed_text);
            } else if ("REJECTED".equals(item.status)) {
                color = ContextCompat.getColor(HistoryActivity.this, R.color.status_rejected_text);
            } else {
                color = ContextCompat.getColor(HistoryActivity.this, R.color.status_pending_text);
            }

            holder.statusIndicator.setBackgroundColor(color);
            holder.status.setTextColor(color);
            holder.keyword.setTextColor(color);
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView keyword, timestamp, context, status;
            View statusIndicator;
            public ViewHolder(@NonNull View v) {
                super(v);
                keyword = v.findViewById(R.id.historyKeyword);
                timestamp = v.findViewById(R.id.historyTimestamp);
                context = v.findViewById(R.id.historyContext);
                status = v.findViewById(R.id.historyStatus);
                statusIndicator = v.findViewById(R.id.statusIndicatorBar);
            }
        }
    }
}
