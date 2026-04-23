package com.example.bibeetaxi;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.location.FilteringMode;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationListener;
import com.yandex.mapkit.location.LocationManager;
import com.yandex.mapkit.location.LocationStatus;
import com.yandex.mapkit.location.Purpose;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;

import java.util.HashMap;
import java.util.Map;

public class DriverMapFragment extends Fragment implements LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;
    private static final String PREFS_NAME = "DriverPrefs";
    private static final String KEY_AVAILABLE = "isAvailable";

    private MapView mapView;
    private Button btnToggleAvailability;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private DatabaseReference onlineRef, driverLocationRef;
    private String driverId;
    private boolean isAvailable = false;
    private LocationManager locationManager;
    private SharedPreferences prefs;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_map, container, false);

        mapView = view.findViewById(R.id.mapview);
        btnToggleAvailability = view.findViewById(R.id.btn_toggle_availability);
        progressBar = view.findViewById(R.id.progressBar);

        // Сначала скрываем карту, показываем прогресс
        mapView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        db = FirebaseFirestore.getInstance();
        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isAvailable = prefs.getBoolean(KEY_AVAILABLE, false);
        updateButtonText();

        onlineRef = FirebaseDatabase.getInstance().getReference("drivers_online").child(driverId);
        driverLocationRef = FirebaseDatabase.getInstance().getReference("drivers_locations").child(driverId);

        // Откладываем инициализацию карты на 100 мс
        handler.postDelayed(() -> {
            if (!isAdded()) return;

            MapKitFactory.getInstance().onStart();
            mapView.onStart();
            mapView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            // Проверка и запрос разрешений геолокации
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                setupLocationManager();
            }

            // Начальная позиция камеры
            mapView.getMap().move(
                    new CameraPosition(new Point(55.751244, 37.618423), 11.0f, 0.0f, 0.0f),
                    new Animation(Animation.Type.SMOOTH, 0),
                    null);

        }, 100);

        btnToggleAvailability.setOnClickListener(v -> {
            isAvailable = !isAvailable;
            prefs.edit().putBoolean(KEY_AVAILABLE, isAvailable).apply();
            updateButtonText();
            updateDriverStatus();
            Toast.makeText(getContext(), isAvailable ? "Вы онлайн" : "Вы офлайн", Toast.LENGTH_SHORT).show();
        });

        updateDriverStatus();

        return view;
    }

    private void updateButtonText() {
        btnToggleAvailability.setText(isAvailable ? "Выключить приём заказов" : "Готов принимать заказы");
    }

    private void setupLocationManager() {
        locationManager = MapKitFactory.getInstance().createLocationManager();
        locationManager.subscribeForLocationUpdates(
                0.0, 1000, 10.0, true,
                FilteringMode.OFF, Purpose.GENERAL, this);
    }

    private void updateDriverStatus() {
        Map<String, Object> driverData = new HashMap<>();
        driverData.put("isAvailable", isAvailable);
        driverData.put("driverId", driverId);

        if (isAvailable) {
            db.collection("drivers_available").document(driverId).set(driverData);
            onlineRef.setValue(true);
            driverLocationRef.setValue(new HashMap<String, Object>() {{
                put("latitude", 0);
                put("longitude", 0);
            }});
            onlineRef.onDisconnect().removeValue();
            driverLocationRef.onDisconnect().removeValue();
        } else {
            db.collection("drivers_available").document(driverId).delete();
            onlineRef.removeValue();
            driverLocationRef.removeValue();
        }
    }

    @Override
    public void onLocationUpdated(@NonNull Location location) {
        if (mapView == null) return;
        Point point = location.getPosition();
        mapView.getMap().move(
                new CameraPosition(point, 16.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 1), null);

        mapView.getMap().getMapObjects().addPlacemark(point)
                .setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_my_location));

        if (isAvailable) {
            db.collection("drivers_available").document(driverId)
                    .update("location", new GeoPoint(point.getLatitude(), point.getLongitude()));
            driverLocationRef.setValue(new HashMap<String, Object>() {{
                put("latitude", point.getLatitude());
                put("longitude", point.getLongitude());
            }});
        }
    }

    @Override
    public void onLocationStatusUpdated(@NonNull LocationStatus locationStatus) {}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupLocationManager();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null && mapView.getVisibility() == View.VISIBLE) {
            MapKitFactory.getInstance().onStart();
            mapView.onStart();
        }
    }

    @Override
    public void onStop() {
        if (mapView != null) {
            mapView.onStop();
            MapKitFactory.getInstance().onStop();
        }
        if (locationManager != null) {
            locationManager.unsubscribe(this);
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        if (isAvailable) {
            db.collection("drivers_available").document(driverId).delete();
            onlineRef.removeValue();
            driverLocationRef.removeValue();
        }
        super.onDestroyView();
    }
}