package com.timecurrency.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private TextView tvAmount;
    
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
        View btnAdd = findViewById(R.id.btnAdd);
        View btnMinus = findViewById(R.id.btnMinus);

        btnAdd.setOnClickListener(v -> updateCurrencyOptimistic(1));
        btnMinus.setOnClickListener(v -> updateCurrencyOptimistic(-1));

        checkPermissions();
        // Only start service if we have permission. 
        // Logic handled in checkPermissions or immediately if below Android 13.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || 
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            startForegroundServiceSafe();
        }
        
        refreshUI();
    }
    
    private void updateCurrencyOptimistic(int delta) {
        try {
            String currentStr = tvAmount.getText().toString();
            int current = Integer.parseInt(currentStr);
            tvAmount.setText(String.valueOf(current + delta));
        } catch (Exception e) {
            // Ignore
        }
        CurrencyManager.updateCurrency(this, delta);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(CurrencyManager.ACTION_UPDATE_UI);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(updateReceiver, filter);
        }
        refreshUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(updateReceiver);
        } catch (Exception e) {
            // Ignore
        }
    }

    private void refreshUI() {
        int amount = CurrencyManager.getCurrency(this);
        tvAmount.setText(String.valueOf(amount));
    }

    private void startForegroundServiceSafe() {
        try {
            Intent serviceIntent = new Intent(this, NotificationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            startForegroundServiceSafe();
        }
    }
}