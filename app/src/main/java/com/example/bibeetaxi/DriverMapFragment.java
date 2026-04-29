package com.example.bibeetaxi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Geometry;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.VisibleRegionUtils;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.Session;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;

import java.util.HashMap;
import java.util.Map;

public class DriverMapFragment extends Fragment implements Session.SearchListener {

    private MapView mapView;
    private EditText etAddress;
    private Button btnSearchAddress, btnToggleAvailability;
    private FirebaseFirestore db;
    private String driverId;
    private boolean isAvailable = false;
    private SharedPreferences prefs;
    private SearchManager searchManager;
    private Session searchSession;
    private Point confirmedLocation;
    private PlacemarkMapObject driverPlacemark;
    private MapObjectCollection mapObjects;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MapKitFactory.getInstance().onStart();
        View view = inflater.inflate(R.layout.fragment_driver_map, container, false);

        mapView = view.findViewById(R.id.mapview);
        etAddress = view.findViewById(R.id.etAddress);
        btnSearchAddress = view.findViewById(R.id.btnSearchAddress);
        btnToggleAvailability = view.findViewById(R.id.btnToggleAvailability);

        db = FirebaseFirestore.getInstance();
        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        prefs = requireActivity().getSharedPreferences("DriverPrefs", 0);
        isAvailable = prefs.getBoolean("isAvailable", false);
        updateAvailabilityButton();

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);
        mapObjects = mapView.getMap().getMapObjects();

        mapView.getMap().move(
                new CameraPosition(new Point(55.751244, 37.618423), 11.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 0),
                null);

        btnSearchAddress.setOnClickListener(v -> {
            String query = etAddress.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(getContext(), "Введите адрес", Toast.LENGTH_SHORT).show();
                return;
            }
            submitSearch(query);
        });

        btnToggleAvailability.setOnClickListener(v -> {
            if (!isAvailable) {
                if (confirmedLocation == null) {
                    Toast.makeText(getContext(), "Сначала укажите корректный адрес", Toast.LENGTH_LONG).show();
                    return;
                }
                isAvailable = true;
                prefs.edit().putBoolean("isAvailable", true).apply();
                updateDriverStatus();
                Toast.makeText(getContext(), "Вы онлайн", Toast.LENGTH_SHORT).show();
            } else {
                isAvailable = false;
                prefs.edit().putBoolean("isAvailable", false).apply();
                updateDriverStatus();
                Toast.makeText(getContext(), "Вы офлайн", Toast.LENGTH_SHORT).show();
            }
            updateAvailabilityButton();
        });

        return view;
    }

    private void submitSearch(String query) {
        Geometry geometry = VisibleRegionUtils.toPolygon(mapView.getMap().getVisibleRegion());
        searchSession = searchManager.submit(query, geometry, new SearchOptions(), this);
    }

    private void updateAvailabilityButton() {
        btnToggleAvailability.setText(isAvailable ? "Выключить приём заказов" : "Готов принимать заказы");
    }

    @Override
    public void onSearchResponse(@NonNull com.yandex.mapkit.search.Response response) {
        if (response.getCollection().getChildren().isEmpty()) {
            Toast.makeText(getContext(), "Адрес не найден, попробуйте другой", Toast.LENGTH_SHORT).show();
            return;
        }
        Point point = response.getCollection().getChildren().get(0).getObj().getGeometry().get(0).getPoint();
        if (point != null) {
            confirmedLocation = point;
            mapObjects.clear();
            driverPlacemark = mapObjects.addPlacemark(point);

            driverPlacemark.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.driver_marker));
            IconStyle iconStyle = new IconStyle();
            iconStyle.setScale(0.2f);
            driverPlacemark.setIconStyle(iconStyle);
            driverPlacemark.setText(etAddress.getText().toString());

            mapView.getMap().move(
                    new CameraPosition(point, 14.0f, 0.0f, 0.0f),
                    new Animation(Animation.Type.SMOOTH, 1),
                    null);

            if (isAvailable) {
                updateDriverStatus();
            }
            Toast.makeText(getContext(), "Адрес найден и поставлен", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSearchError(@NonNull Error error) {
        Toast.makeText(getContext(), "Ошибка поиска адреса", Toast.LENGTH_SHORT).show();
    }

    private void updateDriverStatus() {
        Map<String, Object> driverData = new HashMap<>();
        driverData.put("isAvailable", isAvailable);
        driverData.put("driverId", driverId);
        if (confirmedLocation != null) {
            driverData.put("location", new GeoPoint(confirmedLocation.getLatitude(), confirmedLocation.getLongitude()));
        }
        if (isAvailable) {
            db.collection("drivers_available").document(driverId).set(driverData);
        } else {
            db.collection("drivers_available").document(driverId).delete();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }
}