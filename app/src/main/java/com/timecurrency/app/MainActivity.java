package com.timecurrency.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private TextView tvAmount;
    
    // Receiver for updates coming from Service/Widget
    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(CurrencyManager.EXTRA_AMOUNT)) {
                int amount = intent.getIntExtra(CurrencyManager.EXTRA_AMOUNT, 0);
                tvAmount.setText(String.valueOf(amount));
            } else {
                refreshUI();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvAmount = findViewById(R.id.tvAmount);
        Button btnAdd = findViewById(R.id.btnAdd);
        Button btnMinus = findViewById(R.id.btnMinus);

        // Optimistic UI Update: Update text immediately for instant response
        btnAdd.setOnClickListener(v -> {
            updateCurrencyUI(1);
        });

        btnMinus.setOnClickListener(v -> {
            updateCurrencyUI(-1);
        });

        checkPermissions();
        startForegroundService();
        refreshUI();
    }

    private void updateCurrencyUI(int delta) {
        // 1. Get current displayed value to update UI instantly
        try {
            String text = tvAmount.getText().toString();
            if (text == null || text.isEmpty() || text.equals("--")) {
                text = "0";
            }
            int current = Integer.parseInt(text);
            tvAmount.setText(String.valueOf(current + delta));
        } catch (NumberFormatException e) {
            // Fallback if text is weird, just don't crash and let the background update handle it
            e.printStackTrace();
        }

        // 2. Perform actual data save and broadcast
        CurrencyManager.updateCurrency(this, delta);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             registerReceiver(updateReceiver, new IntentFilter(CurrencyManager.ACTION_UPDATE_UI), Context.RECEIVER_NOT_EXPORTED);
        } else {
             registerReceiver(updateReceiver, new IntentFilter(CurrencyManager.ACTION_UPDATE_UI));
        }
        refreshUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateReceiver);
    }

    private void refreshUI() {
        int amount = CurrencyManager.getCurrency(this);
        tvAmount.setText(String.valueOf(amount));
    }

    private void startForegroundService() {
        Intent serviceIntent = new Intent(this, NotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startForegroundService();
        }
    }
}