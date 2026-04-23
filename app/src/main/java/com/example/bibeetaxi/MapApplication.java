package com.example.bibeetaxi;

import android.app.Application;
import com.yandex.mapkit.MapKitFactory;

public class MapApplication extends Application {
    private final String MAPKIT_API_KEY = "02ce040e-f7ef-481e-b807-93a882aa6447";

    @Override
    public void onCreate() {
        super.onCreate();
        MapKitFactory.setApiKey(MAPKIT_API_KEY);
        MapKitFactory.initialize(this);
    }
}