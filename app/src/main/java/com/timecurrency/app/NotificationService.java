package com.timecurrency.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

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
        // Essential: call startForeground immediately to prevent crash on Android 8+
        startForeground(NOTIFICATION_ID, buildNotification(), 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC : 0);

        if (intent != null) {
            String action = intent.getAction();
            if ("INCREMENT".equals(action)) {
                CurrencyManager.updateCurrency(this, 1);
            } else if ("DECREMENT".equals(action)) {
                CurrencyManager.updateCurrency(this, -1);
            } else if (ACTION_REFRESH.equals(action)) {
                // Just update the notification (already done by startForeground above)
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.notify(NOTIFICATION_ID, buildNotification());
                }
            }
        }
            
        return START_STICKY;
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // This ensures the service restarts if the user swipes the app away from recents
        Intent restartServiceIntent = new Intent(getApplicationContext(), NotificationService.class);
        PendingIntent restartServicePendingIntent = PendingIntent.getService(
                getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_IMMUTABLE);
        
        android.app.AlarmManager alarmService = (android.app.AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
        if (alarmService != null) {
            alarmService.set(
                android.app.AlarmManager.ELAPSED_REALTIME,
                android.os.SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent);
        }

        super.onTaskRemoved(rootIntent);
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

        // Custom View Setup
        RemoteViews customView = new RemoteViews(getPackageName(), R.layout.notification_custom);
        customView.setTextViewText(R.id.notif_amount, String.valueOf(amount));
        
        // Set Listeners
        customView.setOnClickPendingIntent(R.id.notif_btn_plus, pendingInc);
        customView.setOnClickPendingIntent(R.id.notif_btn_minus, pendingDec);
        customView.setOnClickPendingIntent(R.id.notif_amount, pendingOpenApp);
        
        // Programmatic Coloring (Crash Proof)
        // Icon -> Cyan
        customView.setInt(R.id.notif_icon, "setColorFilter", Color.parseColor("#03DAC6"));
        // Plus -> Cyan
        customView.setInt(R.id.notif_btn_plus, "setColorFilter", Color.parseColor("#03DAC6"));
        // Minus -> Red/Pink
        customView.setInt(R.id.notif_btn_minus, "setColorFilter", Color.parseColor("#CF6679"));

        // Use the same view for expanded and collapsed to keep it consistent and small
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setCustomContentView(customView)
                .setCustomBigContentView(customView) 
                .setContentIntent(pendingOpenApp)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Shows content on lock screen
                .setPriority(NotificationCompat.PRIORITY_MAX) // Helps keep it at the top
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle()); // Wraps it in standard container

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Currency Status",
                    NotificationManager.IMPORTANCE_LOW // Low sound, but MAX priority in builder handles visual
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