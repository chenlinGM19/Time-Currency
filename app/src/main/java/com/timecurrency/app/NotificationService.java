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
            if ("INCREMENT".equals(action)) {
                CurrencyManager.updateCurrency(this, 1);
            } else if ("DECREMENT".equals(action)) {
                CurrencyManager.updateCurrency(this, -1);
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification(), 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC : 0);
            
        // START_STICKY ensures the system recreates the service if it's killed for memory
        return START_STICKY;
    }
    
    // Ensure persistence when app is swiped away from recent tasks
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), NotificationService.class);
        PendingIntent restartServicePendingIntent = PendingIntent.getService(
                getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_IMMUTABLE);
        
        // Use AlarmManager to restart after 1 second
        android.app.AlarmManager alarmService = (android.app.AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
        alarmService.set(
            android.app.AlarmManager.ELAPSED_REALTIME,
            android.os.SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent);

        super.onTaskRemoved(rootIntent);
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

        // Create Custom View
        RemoteViews customView = new RemoteViews(getPackageName(), R.layout.notification_custom);
        customView.setTextViewText(R.id.notif_amount, String.valueOf(amount));
        customView.setOnClickPendingIntent(R.id.notif_btn_plus, pendingInc);
        customView.setOnClickPendingIntent(R.id.notif_btn_minus, pendingDec);
        customView.setOnClickPendingIntent(R.id.notif_amount, pendingOpenApp); // Clicking text opens app

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setCustomContentView(customView) // Use custom layout for collapsed state
                .setCustomBigContentView(customView) // Use same layout for expanded state
                .setContentIntent(pendingOpenApp)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle()) // Ensures it fits native look
                .setPriority(NotificationCompat.PRIORITY_MAX); // High priority to show up on lock screen

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Currency Status",
                    NotificationManager.IMPORTANCE_LOW // Low sound importance, but layout handles visibility
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