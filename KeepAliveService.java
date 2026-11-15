package com.hutch.pingerlite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KeepAliveService extends Service {
    private ScheduledExecutorService scheduler;
    public static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        startForegroundServiceNotification();
        startLightPinger();
    }

    private void startForegroundServiceNotification() {
        String channelId = "PingerChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Network Keep Alive", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Intent stopIntent = new Intent(this, KeepAliveService.class);
        stopIntent.setAction("STOP_PINGER");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("Z Pinger Lite Active")
                .setContentText("Keeping 4G+/H+ Awake (Every 5s)")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .addAction(android.R.drawable.ic_media_pause, "STOP PINGER", stopPendingIntent)
                .build();

        startForeground(1, notification);
    }

    private void startLightPinger() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("8.8.8.8", 53), 2000);
                socket.close();
            } catch (Exception e) {
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP_PINGER".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        isRunning = true;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (scheduler != null) scheduler.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // මේ කොටසින් තමයි Notification Panel එකට Icon එක (Quick Tile) හදලා දෙන්නේ
    public static class PingerTile extends TileService {
        @Override
        public void onStartListening() {
            super.onStartListening();
            updateTileState();
        }

        @Override
        public void onClick() {
            super.onClick();
            Intent intent = new Intent(this, KeepAliveService.class);
            if (KeepAliveService.isRunning) {
                stopService(intent);
                KeepAliveService.isRunning = false;
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
                KeepAliveService.isRunning = true;
            }
            updateTileState();
        }

        private void updateTileState() {
            Tile tile = getQsTile();
            if (tile != null) {
                tile.setState(KeepAliveService.isRunning ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
                tile.setLabel(KeepAliveService.isRunning ? "Pinger ON" : "Z Pinger");
                tile.updateTile();
            }
        }
    }
}
