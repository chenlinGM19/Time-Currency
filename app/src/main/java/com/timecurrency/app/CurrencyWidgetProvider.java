package com.timecurrency.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
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
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        updateAppWidget(context, appWidgetManager, appWidgetId);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
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
        int layoutMode = WidgetSettingsHelper.loadLayoutMode(context, appWidgetId);
        int alpha = WidgetSettingsHelper.loadTransparency(context, appWidgetId);
        String imagePath = WidgetSettingsHelper.loadImagePath(context, appWidgetId);
        int radiusDp = WidgetSettingsHelper.loadCornerRadius(context, appWidgetId);

        // Select Layout based on mode
        int layoutId;
        switch (layoutMode) {
            case 1: layoutId = R.layout.widget_layout_sidebar_right; break;
            case 2: layoutId = R.layout.widget_layout_sidebar_left; break;
            case 3: layoutId = R.layout.widget_layout_vertical; break;
            case 4: layoutId = R.layout.widget_layout_bar_bottom; break;
            case 5: layoutId = R.layout.widget_layout_bar_top; break;
            case 6: layoutId = R.layout.widget_layout_corners; break;
            default: layoutId = R.layout.widget_layout; break; // Default Horizontal
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
        views.setTextViewText(R.id.widget_amount, String.valueOf(amount));

        // --- Calculate Dimensions for Dynamic Bitmaps ---
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minWidthDp = 160;
        int minHeightDp = 80;
        if (options != null) {
            minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160);
            minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
        }
        
        // Convert DP to PX
        float density = context.getResources().getDisplayMetrics().density;
        int widthPx = (int) (minWidthDp * density);
        int heightPx = (int) (minHeightDp * density);
        float radiusPx = radiusDp * density;
        
        // Safety check for dimensions
        if (widthPx <= 0) widthPx = 400;
        if (heightPx <= 0) heightPx = 200;

        // --- 1. Apply Dynamic Background Overlay (Shape + Radius) ---
        // We create a white rounded bitmap. The Styles below will Tint it via setColorFilter.
        // This ensures the color layer also respects the R-radius transparency.
        Bitmap overlayBitmap = createSmartRoundedBitmap(null, widthPx, heightPx, radiusPx);
        views.setImageViewBitmap(R.id.widget_bg_overlay, overlayBitmap);
        views.setInt(R.id.widget_bg_overlay, "setAlpha", alpha);

        // --- 2. Apply Background Image ---
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                // Crop and Round the user image to match the overlay perfectly
                Bitmap roundedImage = createSmartRoundedBitmap(bitmap, widthPx, heightPx, radiusPx);
                views.setImageViewBitmap(R.id.widget_bg_image, roundedImage);
                views.setViewVisibility(R.id.widget_bg_image, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.widget_bg_image, View.GONE);
            }
        } else {
            views.setViewVisibility(R.id.widget_bg_image, View.GONE);
        }

        // --- 3. Apply Styles ---
        // Reset defaults
        views.setViewVisibility(R.id.widget_btn_minus, View.VISIBLE);
        views.setViewVisibility(R.id.widget_btn_plus, View.VISIBLE);
        views.setViewVisibility(R.id.widget_click_overlay, View.GONE);
        views.setTextColor(R.id.widget_amount, Color.WHITE);
        views.setTextColor(R.id.widget_btn_plus, Color.WHITE);
        views.setTextColor(R.id.widget_btn_minus, Color.WHITE);
        views.setInt(R.id.widget_btn_config, "setColorFilter", Color.WHITE);
        
        // Default text size
        views.setFloat(R.id.widget_amount, "setTextSize", 24f);

        switch (style) {
            case 0: // Classic (Dark)
                // Now using dynamic white bitmap, so we must tint it Dark Gray.
                views.setInt(R.id.widget_bg_overlay, "setColorFilter", Color.parseColor("#212121"));
                break;
            case 1: // Light Theme
                views.setInt(R.id.widget_bg_overlay, "setColorFilter", Color.WHITE);
                views.setTextColor(R.id.widget_amount, Color.BLACK);
                views.setTextColor(R.id.widget_btn_plus, Color.BLACK);
                views.setTextColor(R.id.widget_btn_minus, Color.BLACK);
                views.setInt(R.id.widget_btn_config, "setColorFilter", Color.BLACK);
                break;
            case 2: // Accent Color
                views.setInt(R.id.widget_bg_overlay, "setColorFilter", Color.parseColor("#03DAC6"));
                views.setTextColor(R.id.widget_amount, Color.BLACK);
                views.setInt(R.id.widget_btn_config, "setColorFilter", Color.BLACK);
                break;
            case 3: // Minimal (Text Only)
                views.setViewVisibility(R.id.widget_btn_minus, View.GONE);
                views.setViewVisibility(R.id.widget_btn_plus, View.GONE);
                views.setViewVisibility(R.id.widget_click_overlay, View.VISIBLE);
                // Also tint the bg for consistency, though user might set alpha to 0
                views.setInt(R.id.widget_bg_overlay, "setColorFilter", Color.parseColor("#212121"));
                break;
            case 4: // Big Number
                views.setFloat(R.id.widget_amount, "setTextSize", 48f);
                views.setInt(R.id.widget_bg_overlay, "setColorFilter", Color.BLACK);
                break;
            case 5: // Compact (1x1)
                 views.setFloat(R.id.widget_amount, "setTextSize", 16f);
                 views.setInt(R.id.widget_bg_overlay, "setColorFilter", Color.DKGRAY);
                break;
        }

        // --- 4. Setup Actions ---
        Intent incIntent = new Intent(context, CurrencyWidgetProvider.class);
        incIntent.setAction(ACTION_INCREMENT);
        PendingIntent pendingInc = PendingIntent.getBroadcast(context, 100, incIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_btn_plus, pendingInc);

        Intent decIntent = new Intent(context, CurrencyWidgetProvider.class);
        decIntent.setAction(ACTION_DECREMENT);
        PendingIntent pendingDec = PendingIntent.getBroadcast(context, 101, decIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_btn_minus, pendingDec);

        // Config Button (Open Settings for THIS widget)
        Intent configIntent = new Intent(context, WidgetConfigActivity.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        configIntent.setData(Uri.parse(configIntent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent pendingConfig = PendingIntent.getActivity(context, appWidgetId, configIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_btn_config, pendingConfig);

        // Open App
        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingOpen = PendingIntent.getActivity(context, 102, openIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_amount, pendingOpen);
        views.setOnClickPendingIntent(R.id.widget_click_overlay, pendingOpen);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    
    /**
     * Creates a bitmap of dimensions (targetWidth, targetHeight).
     * 1. Clears canvas to TRANSPARENT.
     * 2. Draws a rounded mask.
     * 3. Uses SRC_IN to composite the image or solid white into the mask.
     * This guarantees transparency outside the R-radius.
     */
    public static Bitmap createSmartRoundedBitmap(Bitmap source, int targetWidth, int targetHeight, float radiusPx) {
        Bitmap output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        
        RectF rectF = new RectF(0, 0, targetWidth, targetHeight);
        
        // 1. Start with completely transparent canvas
        canvas.drawColor(Color.TRANSPARENT);
        
        // 2. Draw rounded mask (Opaque solid)
        paint.setColor(0xFF000000); // Color doesn't matter for the mask, just alpha
        canvas.drawRoundRect(rectF, radiusPx, radiusPx, paint);
        
        // 3. Composite Source
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        
        if (source != null) {
            // Center Crop Logic
            float scale;
            float dx = 0, dy = 0;
            
            if (source.getWidth() * targetHeight > targetWidth * source.getHeight()) {
                scale = (float) targetHeight / (float) source.getHeight();
                dx = (targetWidth - source.getWidth() * scale) * 0.5f;
            } else {
                scale = (float) targetWidth / (float) source.getWidth();
                dy = (targetHeight - source.getHeight() * scale) * 0.5f;
            }
            
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.setScale(scale, scale);
            matrix.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
            
            canvas.drawBitmap(source, matrix, paint);
        } else {
            // Fill with White so it can be tinted by setColorFilter in RemoteViews
            paint.setColor(0xFFFFFFFF);
            canvas.drawRect(rectF, paint);
        }
        
        return output;
    }
}