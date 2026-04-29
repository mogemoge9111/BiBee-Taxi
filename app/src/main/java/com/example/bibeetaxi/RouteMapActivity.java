package com.example.bibeetaxi;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.mapview.MapView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteMapActivity extends AppCompatActivity {

    private MapView mapView;
    private MapObjectCollection mapObjects;
    private String rideId, passengerId, driverId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapKitFactory.initialize(this);
        setContentView(R.layout.activity_route_map);

        mapView = findViewById(R.id.mapview);
        mapObjects = mapView.getMap().getMapObjects();
        Button btnAccept = findViewById(R.id.btnAcceptRide);
        db = FirebaseFirestore.getInstance();

        double fromLat = getIntent().getDoubleExtra("fromLat", 0);
        double fromLon = getIntent().getDoubleExtra("fromLon", 0);
        double toLat = getIntent().getDoubleExtra("toLat", 0);
        double toLon = getIntent().getDoubleExtra("toLon", 0);
        rideId = getIntent().getStringExtra("rideId");
        passengerId = getIntent().getStringExtra("passengerId");
        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Point start = new Point(fromLat, fromLon);
        Point end = new Point(toLat, toLon);

        mapObjects.addPlacemark(start).setText("Отправление");
        mapObjects.addPlacemark(end).setText("Назначение");

        List<Point> points = new ArrayList<>();
        points.add(start);
        points.add(end);
        Polyline polyline = new Polyline(points);
        PolylineMapObject polylineMapObject = mapObjects.addPolyline(polyline);
        polylineMapObject.setStrokeColor(Color.BLUE);
        polylineMapObject.setStrokeWidth(5.0f);

        Point center = new Point((fromLat + toLat) / 2, (fromLon + toLon) / 2);
        mapView.getMap().move(
                new CameraPosition(center, 10, 0, 0),
                new Animation(Animation.Type.SMOOTH, 1),
                null);

        btnAccept.setOnClickListener(v -> acceptRide());
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
        info.put("rideId", rideId);          // <-- вот это было пропущено
        chatRef.setValue(info);
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