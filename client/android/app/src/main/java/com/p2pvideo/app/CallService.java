package com.p2pvideo.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;

public class CallService extends Service {
    
    private static final String CHANNEL_ID = "ongoing_call_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final String ACTION_END_CALL = "com.p2pvideo.app.END_CALL";
    
    private Handler handler;
    private Runnable timerRunnable;
    private long startTime;
    private NotificationManager notificationManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        startTime = System.currentTimeMillis();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_END_CALL.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        
        startForeground(NOTIFICATION_ID, buildNotification("00:00"));
        startTimer();
        
        return START_STICKY;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Ongoing Call",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows ongoing video call duration");
            channel.setSound(null, null);
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification buildNotification(String duration) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Intent endCallIntent = new Intent(this, CallService.class);
        endCallIntent.setAction(ACTION_END_CALL);
        PendingIntent endCallPendingIntent = PendingIntent.getService(
            this, 1, endCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle("P2P Video Call")
            .setContentText("Ongoing call • " + duration)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "End Call", endCallPendingIntent);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
        }
        
        return builder.build();
    }
    
    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedMillis = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsedMillis / 1000);
                int minutes = seconds / 60;
                int hours = minutes / 60;
                
                String duration;
                if (hours > 0) {
                    duration = String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
                } else {
                    duration = String.format("%02d:%02d", minutes, seconds % 60);
                }
                
                if (notificationManager != null) {
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(duration));
                }
                
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timerRunnable);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }
        
        Intent broadcast = new Intent("com.p2pvideo.app.CALL_ENDED");
        sendBroadcast(broadcast);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
