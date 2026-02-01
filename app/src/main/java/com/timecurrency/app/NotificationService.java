package com.timecurrency.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

public class NotificationService extends Service {

    public static final String ACTION_REFRESH = "ACTION_REFRESH";
    private static final String CHANNEL_ID = "TimeCurrencyChannel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("INCREMENT".equals(action)) {
                CurrencyManager.updateCurrency(this, 1);
            } else if ("DECREMENT".equals(action)) {
                CurrencyManager.updateCurrency(this, -1);
            }
        }
        
        int type = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
        }

        startForeground(NOTIFICATION_ID, buildNotification(), type);
        return START_STICKY;
    }

    private Notification buildNotification() {
        int amount = CurrencyManager.getCurrency(this);

        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingOpenApp = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent incIntent = new Intent(this, NotificationService.class);
        incIntent.setAction("INCREMENT");
        PendingIntent pendingInc = PendingIntent.getService(this, 1, incIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent decIntent = new Intent(this, NotificationService.class);
        decIntent.setAction("DECREMENT");
        PendingIntent pendingDec = PendingIntent.getService(this, 2, decIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // --- Custom View Setup ---
        RemoteViews customView = new RemoteViews(getPackageName(), R.layout.notification_custom);
        customView.setTextViewText(R.id.notif_amount, String.valueOf(amount));
        
        customView.setOnClickPendingIntent(R.id.notif_btn_plus, pendingInc);
        customView.setOnClickPendingIntent(R.id.notif_btn_minus, pendingDec);
        customView.setOnClickPendingIntent(R.id.notif_text_container, pendingOpenApp);
        customView.setOnClickPendingIntent(R.id.notif_icon, pendingOpenApp);

        // Apply colors to icons programmatically to ensure visibility and style
        // Primary Color (Light Purple) for Icon and Plus
        int colorPrimary = Color.parseColor("#D0BCFF"); 
        // Error Color (Soft Red) for Minus
        int colorError = Color.parseColor("#F2B8B5");

        Bitmap iconBmp = getBitmapFromVector(R.drawable.ic_notification, colorPrimary);
        Bitmap plusBmp = getBitmapFromVector(R.drawable.ic_plus, colorPrimary);
        Bitmap minusBmp = getBitmapFromVector(R.drawable.ic_minus, colorError);

        if (iconBmp != null) customView.setImageViewBitmap(R.id.notif_icon, iconBmp);
        if (plusBmp != null) customView.setImageViewBitmap(R.id.notif_btn_plus, plusBmp);
        if (minusBmp != null) customView.setImageViewBitmap(R.id.notif_btn_minus, minusBmp);

        // --- Notification Builder ---
        // We use DecoratedCustomViewStyle for native system decoration integration
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setCustomContentView(customView)
                // Also set for big view to ensure consistency when expanded
                .setCustomBigContentView(customView) 
                .setContentIntent(pendingOpenApp)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW) // Keeps it quiet
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        return builder.build();
    }
    
    // Helper: Safely convert Vector to Bitmap with tint
    private Bitmap getBitmapFromVector(@DrawableRes int resId, @ColorInt int tint) {
        try {
            Drawable drawable = ContextCompat.getDrawable(this, resId);
            if (drawable == null) return null;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                drawable = (DrawableCompat.wrap(drawable)).mutate();
            }
            
            DrawableCompat.setTint(drawable, tint);
            DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);

            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            if (width <= 0) width = 96;
            if (height <= 0) height = 96;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Currency Status",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows persistent time currency");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}