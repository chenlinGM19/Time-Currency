package com.timecurrency.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
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
        if (intent != null) {
            String action = intent.getAction();
            // Handle button clicks from notification
            if ("INCREMENT".equals(action)) {
                CurrencyManager.updateCurrency(this, 1);
            } else if ("DECREMENT".equals(action)) {
                CurrencyManager.updateCurrency(this, -1);
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification(), 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC : 0);
        return START_STICKY;
    }

    private Notification buildNotification() {
        int amount = CurrencyManager.getCurrency(this);

        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingOpenApp = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE);

        // Intents for buttons
        Intent incIntent = new Intent(this, NotificationService.class);
        incIntent.setAction("INCREMENT");
        PendingIntent pendingInc = PendingIntent.getService(this, 1, incIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent decIntent = new Intent(this, NotificationService.class);
        decIntent.setAction("DECREMENT");
        PendingIntent pendingDec = PendingIntent.getService(this, 2, decIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Using standard Notification style for better compatibility, but updating text
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Time Currency")
                .setContentText("Total: " + amount)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingOpenApp)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
                .addAction(R.drawable.ic_minus, "-1", pendingDec)
                .addAction(R.drawable.ic_plus, "+1", pendingInc)
                .setPriority(NotificationCompat.PRIORITY_LOW); // No sound/popup for updates

        return builder.build();
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