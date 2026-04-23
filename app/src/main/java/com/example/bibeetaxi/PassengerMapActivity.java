package com.example.bibeetaxi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.VisibleRegionUtils;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.Session;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.mapkit.user_location.UserLocationObjectListener;
import com.yandex.mapkit.user_location.UserLocationView;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;

import java.util.Locale;

public class PassengerMapActivity extends AppCompatActivity implements Session.SearchListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private MapView mapView;
    private EditText etSearch;
    private Button btnSearch, btnSetPickup, btnSetDestination, btnCalculatePrice, btnCreateRequest;
    private TextView tvPriceInfo;
    private SearchManager searchManager;
    private Session searchSession;
    private MapObjectCollection mapObjects;
    private FirebaseFirestore db;
    private ListenerRegistration driversListener;

    private Point pickupPoint, destinationPoint;
    private PlacemarkMapObject pickupPlacemark, destinationPlacemark;
    private UserLocationLayer userLocationLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapKitFactory.initialize(this);
        setContentView(R.layout.activity_passenger_map);

        mapView = findViewById(R.id.mapview);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnSetPickup = findViewById(R.id.btnSetPickup);
        btnSetDestination = findViewById(R.id.btnSetDestination);
        btnCalculatePrice = findViewById(R.id.btnCalculatePrice);
        btnCreateRequest = findViewById(R.id.btnCreateRequest);
        tvPriceInfo = findViewById(R.id.tvPriceInfo);

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);
        db = FirebaseFirestore.getInstance();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            setupUserLocation();
        }

        mapView.getMap().move(
                new CameraPosition(new Point(55.751244, 37.618423), 11.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 0),
                null);

        mapObjects = mapView.getMap().getMapObjects();

        mapView.getMap().addInputListener(new InputListener() {
            @Override
            public void onMapTap(@NonNull com.yandex.mapkit.map.Map map, @NonNull Point point) {
                if (btnSetPickup.isSelected()) {
                    setPickupPoint(point);
                    btnSetPickup.setSelected(false);
                } else if (btnSetDestination.isSelected()) {
                    setDestinationPoint(point);
                    btnSetDestination.setSelected(false);
                } else {
                    reverseGeocode(point);
                }
            }

            @Override
            public void onMapLongTap(@NonNull com.yandex.mapkit.map.Map map, @NonNull Point point) {}
        });

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                submitSearch(query);
            }
        });

        btnSetPickup.setOnClickListener(v -> {
            btnSetPickup.setSelected(true);
            btnSetDestination.setSelected(false);
            Toast.makeText(this, "Нажмите на карту, чтобы выбрать точку отправления", Toast.LENGTH_SHORT).show();
        });

        btnSetDestination.setOnClickListener(v -> {
            btnSetDestination.setSelected(true);
            btnSetPickup.setSelected(false);
            Toast.makeText(this, "Нажмите на карту, чтобы выбрать точку назначения", Toast.LENGTH_SHORT).show();
        });

        btnCalculatePrice.setOnClickListener(v -> {
            if (pickupPoint != null && destinationPoint != null) {
                calculatePriceAndDistance();
            } else {
                Toast.makeText(this, "Сначала выберите точки отправления и назначения", Toast.LENGTH_SHORT).show();
            }
        });

        btnCreateRequest.setOnClickListener(v -> {
            startActivity(new Intent(PassengerMapActivity.this, CreateRideRequestActivity.class));
        });

        loadAvailableDrivers();
    }

    private void setupUserLocation() {
        userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapView.getMapWindow());
        userLocationLayer.setVisible(true);
        userLocationLayer.setHeadingEnabled(true);
        userLocationLayer.setObjectListener(new UserLocationObjectListener() {
            @Override
            public void onObjectAdded(@NonNull UserLocationView userLocationView) {}
            @Override
            public void onObjectRemoved(@NonNull UserLocationView userLocationView) {}
            @Override
            public void onObjectUpdated(@NonNull UserLocationView userLocationView, @NonNull com.yandex.mapkit.layers.ObjectEvent event) {
                if (userLocationLayer.cameraPosition() != null) {
                    mapView.getMap().move(userLocationLayer.cameraPosition(), new Animation(Animation.Type.SMOOTH, 1), null);
                }
            }
        });
    }

    private void setPickupPoint(Point point) {
        pickupPoint = point;
        if (pickupPlacemark != null) mapObjects.remove(pickupPlacemark);
        pickupPlacemark = mapObjects.addPlacemark(point);
        pickupPlacemark.setIcon(ImageProvider.fromResource(this, R.drawable.ic_pickup));
        pickupPlacemark.setText("Отправление");
        reverseGeocodeForAddress(point, true);
    }

    private void setDestinationPoint(Point point) {
        destinationPoint = point;
        if (destinationPlacemark != null) mapObjects.remove(destinationPlacemark);
        destinationPlacemark = mapObjects.addPlacemark(point);
        destinationPlacemark.setIcon(ImageProvider.fromResource(this, R.drawable.ic_destination));
        destinationPlacemark.setText("Назначение");
        reverseGeocodeForAddress(point, false);
    }

    private void reverseGeocode(Point point) {
        searchManager.submit(point, 18, new SearchOptions(),
                new Session.SearchListener() {
                    @Override
                    public void onSearchResponse(@NonNull com.yandex.mapkit.search.Response response) {
                        if (response.getCollection().getChildren().isEmpty()) return;
                        etSearch.setText(response.getCollection().getChildren().get(0).getObj().getName());
                    }
                    @Override
                    public void onSearchError(@NonNull Error error) {}
                });
    }

    private void reverseGeocodeForAddress(Point point, boolean isPickup) {
        searchManager.submit(point, 18, new SearchOptions(),
                new Session.SearchListener() {
                    @Override
                    public void onSearchResponse(@NonNull com.yandex.mapkit.search.Response response) {
                        if (response.getCollection().getChildren().isEmpty()) return;
                        String address = response.getCollection().getChildren().get(0).getObj().getName();
                        if (isPickup) pickupPlacemark.setText(address);
                        else destinationPlacemark.setText(address);
                    }
                    @Override
                    public void onSearchError(@NonNull Error error) {}
                });
    }

    private void submitSearch(String query) {
        searchSession = searchManager.submit(query,
                VisibleRegionUtils.toPolygon(mapView.getMap().getVisibleRegion()),
                new SearchOptions(), this);
    }

    @Override
    public void onSearchResponse(@NonNull com.yandex.mapkit.search.Response response) {
        if (response.getCollection().getChildren().isEmpty()) {
            Toast.makeText(this, "Ничего не найдено", Toast.LENGTH_SHORT).show();
            return;
        }
        Point resultPoint = response.getCollection().getChildren().get(0).getObj().getGeometry().get(0).getPoint();
        if (resultPoint != null) {
            mapView.getMap().move(new CameraPosition(resultPoint, 14.0f, 0.0f, 0.0f),
                    new Animation(Animation.Type.SMOOTH, 1), null);
            setDestinationPoint(resultPoint);
        }
    }

    @Override
    public void onSearchError(@NonNull Error error) {}

    private void calculatePriceAndDistance() {
        if (pickupPoint == null || destinationPoint == null) return;

        double distanceKm = haversineDistance(pickupPoint, destinationPoint);
        int price = calculatePrice(distanceKm);
        int minutes = (int) (distanceKm * 3);

        String info = String.format(Locale.getDefault(),
                "Расстояние: %.1f км\nСтоимость: %d ₽\nПримерное время: %d мин",
                distanceKm, price, minutes);
        tvPriceInfo.setText(info);
    }

    private double haversineDistance(Point p1, Point p2) {
        final int R = 6371;
        double lat1 = Math.toRadians(p1.getLatitude());
        double lon1 = Math.toRadians(p1.getLongitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double lon2 = Math.toRadians(p2.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private int calculatePrice(double distanceKm) {
        int basePrice = 100;
        int pricePerKm = 30;
        return basePrice + (int)(distanceKm * pricePerKm);
    }

    private void loadAvailableDrivers() {
        driversListener = db.collection("drivers_available")
                .whereEqualTo("isAvailable", true)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    mapObjects.clear();
                    for (var doc : value.getDocuments()) {
                        GeoPoint loc = doc.getGeoPoint("location");
                        if (loc != null) {
                            Point point = new Point(loc.getLatitude(), loc.getLongitude());
                            PlacemarkMapObject placemark = mapObjects.addPlacemark(point);
                            placemark.setIcon(ImageProvider.fromResource(this, R.drawable.ic_car));
                            String driverName = doc.getString("name");
                            placemark.setText("🚖 " + (driverName != null ? driverName : "Водитель"));
                            placemark.addTapListener((mapObject, tapPoint) -> {
                                startActivity(new Intent(PassengerMapActivity.this, DriverProfileViewActivity.class)
                                        .putExtra("driverId", doc.getId()));
                                return true;
                            });
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupUserLocation();
        }
    }

    @Override protected void onStart() { super.onStart(); MapKitFactory.getInstance().onStart(); mapView.onStart(); }
    @Override protected void onStop() { mapView.onStop(); MapKitFactory.getInstance().onStop(); super.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); if (driversListener != null) driversListener.remove(); }
}