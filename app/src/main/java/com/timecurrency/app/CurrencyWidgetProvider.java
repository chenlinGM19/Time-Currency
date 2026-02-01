package com.timecurrency.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.View;
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
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            WidgetSettingsHelper.deletePrefs(context, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();

        if (ACTION_INCREMENT.equals(action)) {
            // We need appWidgetId to know specifically which widget initiated this if we wanted per-widget logic,
            // but the currency is global.
            CurrencyManager.updateCurrency(context, 1);
        } else if (ACTION_DECREMENT.equals(action)) {
            CurrencyManager.updateCurrency(context, -1);
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), CurrencyWidgetProvider.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        int amount = CurrencyManager.getCurrency(context);
        
        // Load settings
        int style = WidgetSettingsHelper.loadStyle(context, appWidgetId);
        int alpha = WidgetSettingsHelper.loadTransparency(context, appWidgetId);
        String imagePath = WidgetSettingsHelper.loadImagePath(context, appWidgetId);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        views.setTextViewText(R.id.widget_amount, String.valueOf(amount));

        // --- 1. Apply Background Image ---
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                views.setImageViewBitmap(R.id.widget_bg_image, bitmap);
                views.setViewVisibility(R.id.widget_bg_image, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.widget_bg_image, View.GONE);
            }
        } else {
            views.setViewVisibility(R.id.widget_bg_image, View.GONE);
        }

        // --- 2. Apply Transparency & Background Color Layer ---
        // We set the alpha on the overlay view which contains the solid color/shape
        views.setInt(R.id.widget_bg_overlay, "setAlpha", alpha);

        // --- 3. Apply Styles ---
        // Reset defaults
        views.setViewVisibility(R.id.widget_btn_minus, View.VISIBLE);
        views.setViewVisibility(R.id.widget_btn_plus, View.VISIBLE);
        views.setViewVisibility(R.id.widget_click_overlay, View.GONE);
        views.setTextColor(R.id.widget_amount, Color.WHITE);
        views.setTextColor(R.id.widget_btn_plus, Color.WHITE);
        views.setTextColor(R.id.widget_btn_minus, Color.WHITE);
        
        // Default text size
        views.setFloat(R.id.widget_amount, "setTextSize", 24f);

        switch (style) {
            case 0: // Classic (Dark)
                views.setImageViewResource(R.id.widget_bg_overlay, R.drawable.widget_background); // Uses the dark xml shape
                break;
            case 1: // Light Theme
                // Create a dynamic light background or use a different drawable
                // Since we can't easily change drawable colors dynamically in RemoteViews without API 31,
                // we rely on tint or specific drawables. For simplicity, we use setInt for color filter if simple,
                // or just rely on the drawable logic.
                // Here we just tint the background drawable to white
                views.setInt(R.id.widget_bg_overlay, "setColorFilter", Color.WHITE);
                views.setTextColor(R.id.widget_amount, Color.BLACK);
                views.setTextColor(R.id.widget_btn_plus, Color.BLACK);
                views.setTextColor(R.id.widget_btn_minus, Color.BLACK);
                break;
            case 2: // Accent Color
                views.setInt(R.id.widget_bg_overlay, "setColorFilter", Color.parseColor("#03DAC6"));
                views.setTextColor(R.id.widget_amount, Color.BLACK);
                break;
            case 3: // Minimal (Text Only)
                views.setViewVisibility(R.id.widget_btn_minus, View.GONE);
                views.setViewVisibility(R.id.widget_btn_plus, View.GONE);
                views.setViewVisibility(R.id.widget_click_overlay, View.VISIBLE); // Allow clicking whole widget to open app
                break;
            case 4: // Big Number
                views.setFloat(R.id.widget_amount, "setTextSize", 48f);
                views.setInt(R.id.widget_bg_overlay, "setColorFilter", Color.BLACK);
                break;
            case 5: // Compact (1x1)
                 // Adjust layout parameters if possible, or just rely on responsive layouts.
                 // For 1x1, the default layout works but text might be large.
                 views.setFloat(R.id.widget_amount, "setTextSize", 16f);
                 views.setInt(R.id.widget_bg_overlay, "setColorFilter", Color.DKGRAY);
                break;
        }

        // --- 4. Setup Actions ---
        Intent incIntent = new Intent(context, CurrencyWidgetProvider.class);
        incIntent.setAction(ACTION_INCREMENT);
        // Important: Use unique request codes or setData to distinguish distinct pending intents if needed,
        // though here the Action is different so it's fine.
        PendingIntent pendingInc = PendingIntent.getBroadcast(context, 100, incIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_btn_plus, pendingInc);

        Intent decIntent = new Intent(context, CurrencyWidgetProvider.class);
        decIntent.setAction(ACTION_DECREMENT);
        PendingIntent pendingDec = PendingIntent.getBroadcast(context, 101, decIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_btn_minus, pendingDec);

        // Open App
        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingOpen = PendingIntent.getActivity(context, 102, openIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_amount, pendingOpen);
        views.setOnClickPendingIntent(R.id.widget_click_overlay, pendingOpen);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}