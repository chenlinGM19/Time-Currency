package com.timecurrency.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class CurrencyWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_INCREMENT = "com.timecurrency.app.ACTION_INCREMENT";
    public static final String ACTION_DECREMENT = "com.timecurrency.app.ACTION_DECREMENT";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();

        if (ACTION_INCREMENT.equals(action)) {
            CurrencyManager.updateCurrency(context, 1);
        } else if (ACTION_DECREMENT.equals(action)) {
            CurrencyManager.updateCurrency(context, -1);
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            // Force update all widgets
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), CurrencyWidgetProvider.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        int amount = CurrencyManager.getCurrency(context);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        views.setTextViewText(R.id.widget_amount, String.valueOf(amount));

        // Setup Buttons
        Intent incIntent = new Intent(context, CurrencyWidgetProvider.class);
        incIntent.setAction(ACTION_INCREMENT);
        PendingIntent pendingInc = PendingIntent.getBroadcast(context, 0, incIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_btn_plus, pendingInc);

        Intent decIntent = new Intent(context, CurrencyWidgetProvider.class);
        decIntent.setAction(ACTION_DECREMENT);
        PendingIntent pendingDec = PendingIntent.getBroadcast(context, 1, decIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_btn_minus, pendingDec);

        // Open App on text click
        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingOpen = PendingIntent.getActivity(context, 2, openIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_layout_root, pendingOpen);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}