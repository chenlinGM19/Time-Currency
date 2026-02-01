package com.timecurrency.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class CurrencyManager {

    private static final String PREF_NAME = "TimeCurrencyPrefs";
    private static final String KEY_AMOUNT = "amount";
    public static final String ACTION_UPDATE_UI = "com.timecurrency.app.ACTION_UPDATE_UI";
    public static final String EXTRA_AMOUNT = "com.timecurrency.app.EXTRA_AMOUNT";

    public static int getCurrency(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_AMOUNT, 0);
    }

    public static void updateCurrency(Context context, int delta) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int current = prefs.getInt(KEY_AMOUNT, 0);
        int newValue = current + delta;
        
        prefs.edit().putInt(KEY_AMOUNT, newValue).apply();

        // Notify Service to update Notification
        Intent serviceIntent = new Intent(context, NotificationService.class);
        serviceIntent.setAction(NotificationService.ACTION_REFRESH);
        
        // Critical Fix: Use startForegroundService on Android O+ to prevent IllegalStateException.
        // Also wrap in try-catch because Android 12+ restricts starting services from the background (e.g., Widget click).
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            // If the app is restricted from starting the service (background), just log it.
            // The Widget and Activity UI will still update via broadcasts below.
            e.printStackTrace();
        }

        // Notify Widget to update
        Intent widgetIntent = new Intent(context, CurrencyWidgetProvider.class);
        widgetIntent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        context.sendBroadcast(widgetIntent);
        
        // Notify Activity with the new value directly for instant update
        Intent broadcastIntent = new Intent(ACTION_UPDATE_UI);
        broadcastIntent.putExtra(EXTRA_AMOUNT, newValue);
        context.sendBroadcast(broadcastIntent);
    }
}