package com.example.bibeetaxi;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.yandex.mapkit.MapKitFactory;

public class MapApplication extends Application {
    public static final String CHANNEL_ID = "ride_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Поездки",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Уведомления о статусе поездки");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        MapKitFactory.setApiKey("02ce040e-f7ef-481e-b807-93a882aa6447");
        MapKitFactory.initialize(this);
    }
}