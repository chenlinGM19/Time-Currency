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
        
        // Critical Fix: Use startForegroundService on Android O+ to prevent IllegalStateException
        // when app is in background (e.g., widget update)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
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