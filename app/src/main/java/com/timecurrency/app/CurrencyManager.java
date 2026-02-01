package com.timecurrency.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

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

        // 1. Notify Widget
        Intent widgetIntent = new Intent(context, CurrencyWidgetProvider.class);
        widgetIntent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        context.sendBroadcast(widgetIntent);
        
        // 2. Notify Activity UI
        Intent broadcastIntent = new Intent(ACTION_UPDATE_UI);
        broadcastIntent.putExtra(EXTRA_AMOUNT, newValue);
        context.sendBroadcast(broadcastIntent);

        // 3. Notify Notification (Directly, no Service start)
        // This is safe to call from background (Receiver) or foreground (Activity)
        NotificationService.refreshNotification(context);
    }
}