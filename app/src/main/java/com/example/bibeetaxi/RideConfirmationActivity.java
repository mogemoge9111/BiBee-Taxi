package com.example.bibeetaxi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RideConfirmationActivity extends AppCompatActivity {

    private ImageView ivDriverPhoto;
    private TextView tvDriverName, tvDriverCar, tvDriverRating, tvRideDetails;
    private Button btnConfirm, btnReject;
    private FirebaseFirestore db;
    private String acceptedRideId;
    private String rideId;
    private String driverId;
    private String passengerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_confirmation);

        ivDriverPhoto = findViewById(R.id.ivDriverPhoto);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvDriverCar = findViewById(R.id.tvDriverCar);
        tvDriverRating = findViewById(R.id.tvDriverRating);
        tvRideDetails = findViewById(R.id.tvRideDetails);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnReject = findViewById(R.id.btnReject);
        db = FirebaseFirestore.getInstance();

        acceptedRideId = getIntent().getStringExtra("acceptedRideId");
        rideId = getIntent().getStringExtra("rideId");
        driverId = getIntent().getStringExtra("driverId");
        passengerId = getIntent().getStringExtra("passengerId");

        loadDriverProfile(driverId);
        loadRideDetails(rideId);

        btnConfirm.setOnClickListener(v -> confirmRide());
        btnReject.setOnClickListener(v -> rejectRide());
    }

    private void loadDriverProfile(String driverId) {
        db.collection("users").document(driverId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvDriverName.setText(doc.getString("name") + " " + doc.getString("surname"));
                        tvDriverCar.setText(doc.getString("carModel") + " " + doc.getString("carNumber"));
                        Double rating = doc.getDouble("rating");
                        tvDriverRating.setText("Рейтинг: " + (rating != null ? rating : 0.0));
                        String photoUrl = doc.getString("photoUrl");
                        if (photoUrl != null) {
                            Glide.with(this).load(photoUrl).into(ivDriverPhoto);
                        }
                    }
                });
    }

    private void loadRideDetails(String rideId) {
        db.collection("ride_requests").document(rideId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String from = doc.getString("fromAddress");
                        String to = doc.getString("toAddress");
                        int maxPrice = doc.getLong("maxPrice").intValue();
                        tvRideDetails.setText(String.format("%s → %s\nМакс. цена: %d ₽", from, to, maxPrice));
                    }
                });
    }

    private void confirmRide() {
        // Меняем статус accepted_rides и ride_requests на confirmed
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "confirmed");

        db.collection("accepted_rides").document(acceptedRideId).update(updates);
        db.collection("ride_requests").document(rideId).update("status", "confirmed");

        // Открываем чат
        String chatId = driverId.compareTo(passengerId) < 0 ? driverId + "_" + passengerId : passengerId + "_" + driverId;
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("otherUserId", driverId);
        intent.putExtra("chatId", chatId);
        startActivity(intent);
        finish();
    }

    private void rejectRide() {
        db.collection("accepted_rides").document(acceptedRideId).delete();
        db.collection("ride_requests").document(rideId).update("status", "waiting", "driverId", null);
        Toast.makeText(this, "Заказ отклонён", Toast.LENGTH_SHORT).show();
        finish();
    }
}