package com.example.bibeetaxi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

public class DriverProfileViewActivity extends AppCompatActivity {

    private ImageView ivDriverPhoto;
    private TextView tvDriverName, tvDriverCar, tvDriverRating;
    private Button btnChatWithDriver;
    private FirebaseFirestore db;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_profile_view);

        ivDriverPhoto = findViewById(R.id.iv_driver_photo);
        tvDriverName = findViewById(R.id.tv_driver_name);
        tvDriverCar = findViewById(R.id.tv_driver_car);
        tvDriverRating = findViewById(R.id.tv_driver_rating);
        btnChatWithDriver = findViewById(R.id.btn_chat_with_driver);
        btnChatWithDriver.setText("Вернуться в чат");

        db = FirebaseFirestore.getInstance();
        driverId = getIntent().getStringExtra("driverId");

        loadDriverProfile();

        btnChatWithDriver.setOnClickListener(v -> {
            Intent intent = new Intent(DriverProfileViewActivity.this, ChatActivity.class);
            intent.putExtra("otherUserId", driverId);
            startActivity(intent);
            finish();
        });
    }

    private void loadDriverProfile() {
        db.collection("users").document(driverId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String surname = doc.getString("surname");
                        tvDriverName.setText((name != null ? name : "") + " " + (surname != null ? surname : ""));

                        String carModel = doc.getString("carModel");
                        String carNumber = doc.getString("carNumber");
                        tvDriverCar.setText((carModel != null ? carModel : "") + " " + (carNumber != null ? carNumber : ""));

                        String photoUrl = doc.getString("photoUrl");
                        if (photoUrl != null) Glide.with(this).load(photoUrl).into(ivDriverPhoto);

                        Double rating = doc.getDouble("rating");
                        tvDriverRating.setText("Рейтинг: " + (rating != null ? rating : "0.0"));
                    }
                });
    }
}