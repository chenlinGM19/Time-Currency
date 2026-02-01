package com.timecurrency.app;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    // For Exporting
    private final ActivityResultLauncher<String> createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) {
                    exportDataToUri(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Edge to Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        setContentView(R.layout.activity_history);
        
        // Apply insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        loadData();

        findViewById(R.id.btnExport).setOnClickListener(v -> {
            String fileName = "time_currency_export_" + System.currentTimeMillis() + ".json";
            createDocumentLauncher.launch(fileName);
        });
    }

    private void loadData() {
        List<TransactionItem> items = new ArrayList<>();
        try (Cursor cursor = TransactionDbHelper.getAllTransactions(this)) {
            while (cursor.moveToNext()) {
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(TransactionDbHelper.COLUMN_TIMESTAMP));
                int delta = cursor.getInt(cursor.getColumnIndexOrThrow(TransactionDbHelper.COLUMN_DELTA));
                items.add(new TransactionItem(timestamp, delta));
            }
        }
        adapter = new TransactionAdapter(items);
        recyclerView.setAdapter(adapter);
    }
    
    private void exportDataToUri(android.net.Uri uri) {
        new Thread(() -> {
            try {
                JSONArray jsonArray = new JSONArray();
                try (Cursor cursor = TransactionDbHelper.getAllTransactions(this)) {
                    while (cursor.moveToNext()) {
                        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(TransactionDbHelper.COLUMN_TIMESTAMP));
                        int delta = cursor.getInt(cursor.getColumnIndexOrThrow(TransactionDbHelper.COLUMN_DELTA));
                        int totalSnapshot = cursor.getInt(cursor.getColumnIndexOrThrow(TransactionDbHelper.COLUMN_TOTAL_SNAPSHOT));
                        
                        JSONObject obj = new JSONObject();
                        obj.put("timestamp", timestamp);
                        obj.put("dateTime", dateFormat.format(new Date(timestamp)));
                        obj.put("delta", delta);
                        obj.put("totalSnapshot", totalSnapshot);
                        jsonArray.put(obj);
                    }
                }
                
                String jsonString = jsonArray.toString(4); // Indent 4 spaces
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    os.write(jsonString.getBytes());
                }
                
                runOnUiThread(() -> Toast.makeText(this, "Export Successful", Toast.LENGTH_SHORT).show());
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Export Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // --- Inner Classes for List ---

    private static class TransactionItem {
        long timestamp;
        int delta;

        TransactionItem(long timestamp, int delta) {
            this.timestamp = timestamp;
            this.delta = delta;
        }
    }

    private class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
        private final List<TransactionItem> list;

        TransactionAdapter(List<TransactionItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TransactionItem item = list.get(position);
            holder.tvDate.setText(dateFormat.format(new Date(item.timestamp)));
            String sign = item.delta > 0 ? "+" : "";
            holder.tvDelta.setText(sign + item.delta);
            holder.tvDelta.setTextColor(item.delta > 0 ? 
                    getColor(R.color.md_theme_dark_primary) : getColor(R.color.md_theme_dark_error));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvDelta;

            ViewHolder(View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvDelta = itemView.findViewById(R.id.tvDelta);
            }
        }
    }
}