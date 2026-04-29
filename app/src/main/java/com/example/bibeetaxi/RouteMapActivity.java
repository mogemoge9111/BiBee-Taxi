package com.example.bibeetaxi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Geometry;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.Session;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteMapActivity extends AppCompatActivity {

    private MapView mapView;
    private MapObjectCollection mapObjects;
    private String rideId, passengerId, driverId;
    private FirebaseFirestore db;

    private ImageView ivPassengerPhoto;
    private TextView tvPassengerName, tvFromTo, tvDistance, tvPrice, tvPassengers, tvCargo;
    private Button btnAccept;

    private SearchManager searchManager;
    private Session searchSession;

    private Point startPoint, endPoint;
    private String fromAddress, toAddress;
    private String passengers, cargo;
    private int maxPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapKitFactory.initialize(this);
        setContentView(R.layout.activity_route_map);

        mapView = findViewById(R.id.mapview);
        mapObjects = mapView.getMap().getMapObjects();
        btnAccept = findViewById(R.id.btnAcceptRide);

        ivPassengerPhoto = findViewById(R.id.ivPassengerPhoto);
        tvPassengerName = findViewById(R.id.tvPassengerName);
        tvFromTo = findViewById(R.id.tvFromTo);
        tvDistance = findViewById(R.id.tvDistance);
        tvPrice = findViewById(R.id.tvPrice);
        tvPassengers = findViewById(R.id.tvPassengers);
        tvCargo = findViewById(R.id.tvCargo);

        db = FirebaseFirestore.getInstance();
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);

        double fromLat = getIntent().getDoubleExtra("fromLat", 0);
        double fromLon = getIntent().getDoubleExtra("fromLon", 0);
        double toLat = getIntent().getDoubleExtra("toLat", 0);
        double toLon = getIntent().getDoubleExtra("toLon", 0);
        rideId = getIntent().getStringExtra("rideId");
        passengerId = getIntent().getStringExtra("passengerId");
        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        fromAddress = getIntent().getStringExtra("fromAddress");
        toAddress = getIntent().getStringExtra("toAddress");
        passengers = getIntent().getStringExtra("passengers");
        cargo = getIntent().getStringExtra("cargo");
        maxPrice = getIntent().getIntExtra("maxPrice", 0);


        tvFromTo.setText("От: " + (fromAddress != null ? fromAddress : "") + "\nДо: " + (toAddress != null ? toAddress : ""));
        tvPassengers.setText("Пассажиры: " + (passengers != null ? passengers : "1"));
        tvCargo.setText("Груз: " + (cargo != null ? cargo : "нет"));
        tvPrice.setText("Цена: " + maxPrice + " ₽");

        loadPassengerProfile(passengerId);


        if (fromLat != 0 && fromLon != 0 && toLat != 0 && toLon != 0) {
            startPoint = new Point(fromLat, fromLon);
            endPoint = new Point(toLat, toLon);
            drawRouteAndInfo();
        } else {

            if (fromAddress != null && !fromAddress.isEmpty()) {
                geocodeAddress(fromAddress, true);
            }
            if (toAddress != null && !toAddress.isEmpty()) {
                geocodeAddress(toAddress, false);
            }
        }

        btnAccept.setOnClickListener(v -> acceptRide());
    }

    private void geocodeAddress(String address, boolean isFrom) {
        Geometry geometry = Geometry.fromPoint(new Point(55.751244, 37.618423));
        searchSession = searchManager.submit(address, geometry, new SearchOptions(),
                new Session.SearchListener() {
                    @Override
                    public void onSearchResponse(@NonNull com.yandex.mapkit.search.Response response) {
                        if (!response.getCollection().getChildren().isEmpty()) {
                            Point point = response.getCollection().getChildren().get(0).getObj().getGeometry().get(0).getPoint();
                            if (isFrom) {
                                startPoint = point;
                            } else {
                                endPoint = point;
                            }
                            if (startPoint != null && endPoint != null) {
                                drawRouteAndInfo();
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("fromLat", startPoint.getLatitude());
                                updates.put("fromLon", startPoint.getLongitude());
                                updates.put("toLat", endPoint.getLatitude());
                                updates.put("toLon", endPoint.getLongitude());
                                db.collection("ride_requests").document(rideId).update(updates);
                            }
                        } else {
                            Toast.makeText(RouteMapActivity.this, "Не удалось найти адрес: " + address, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onSearchError(@NonNull Error error) {
                        Toast.makeText(RouteMapActivity.this, "Ошибка поиска адреса", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void drawRouteAndInfo() {
        if (mapObjects == null || startPoint == null || endPoint == null) {
            Toast.makeText(this, "Нет координат для отображения", Toast.LENGTH_SHORT).show();
            return;
        }


        mapObjects.clear();


        PlacemarkMapObject pickupPlacemark = mapObjects.addPlacemark(startPoint);
        pickupPlacemark.setIcon(ImageProvider.fromResource(this, R.drawable.marker));
        IconStyle style = new IconStyle();
        style.setScale(0.1f);
        pickupPlacemark.setIconStyle(style);
        pickupPlacemark.setText("Отправление");


        PlacemarkMapObject destPlacemark = mapObjects.addPlacemark(endPoint);
        destPlacemark.setIcon(ImageProvider.fromResource(this, R.drawable.marker));
        destPlacemark.setIconStyle(style);
        destPlacemark.setText("Назначение");


        List<Point> points = new ArrayList<>();
        points.add(startPoint);
        points.add(endPoint);
        Polyline polyline = new Polyline(points);
        PolylineMapObject polylineMapObject = mapObjects.addPolyline(polyline);
        polylineMapObject.setStrokeColor(Color.BLUE);
        polylineMapObject.setStrokeWidth(5.0f);


        Point center = new Point(
                (startPoint.getLatitude() + endPoint.getLatitude()) / 2,
                (startPoint.getLongitude() + endPoint.getLongitude()) / 2);
        mapView.getMap().move(
                new CameraPosition(center, 12, 0, 0),
                new Animation(Animation.Type.SMOOTH, 1),
                null);


        double distanceKm = haversineDistance(startPoint, endPoint);
        double distanceM = distanceKm * 1000;
        tvDistance.setText(String.format("Расстояние: %.0f м", distanceM));
    }

    private void loadPassengerProfile(String passengerId) {
        db.collection("users").document(passengerId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String surname = doc.getString("surname");
                        String fullName = (name != null ? name : "") + " " + (surname != null ? surname : "");
                        tvPassengerName.setText(fullName.trim().isEmpty() ? "Пассажир" : fullName);

                        String photoBase64 = doc.getString("photoBase64");
                        if (photoBase64 != null && !photoBase64.isEmpty()) {
                            byte[] decoded = Base64.decode(photoBase64, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                            ivPassengerPhoto.setImageBitmap(bitmap);
                        }
                    }
                });
    }

    private void acceptRide() {
        db.collection("ride_requests").document(rideId)
                .update("status", "accepted", "driverId", driverId)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> accepted = new HashMap<>();
                    accepted.put("rideId", rideId);
                    accepted.put("driverId", driverId);
                    accepted.put("passengerId", passengerId);
                    accepted.put("status", "accepted");
                    accepted.put("timestamp", System.currentTimeMillis());
                    db.collection("accepted_rides").add(accepted);

                    createChat(driverId, passengerId, rideId);
                    Toast.makeText(this, "Заказ принят. Ожидайте подтверждения пассажира.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createChat(String driverId, String passengerId, String rideId) {
        if (driverId.equals(passengerId)) return;
        String chatId = driverId.compareTo(passengerId) < 0 ? driverId + "_" + passengerId : passengerId + "_" + driverId;
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("info");
        Map<String, Object> info = new HashMap<>();
        info.put("driverId", driverId);
        info.put("passengerId", passengerId);
        info.put("rideId", rideId);
        chatRef.setValue(info);
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

    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }
}