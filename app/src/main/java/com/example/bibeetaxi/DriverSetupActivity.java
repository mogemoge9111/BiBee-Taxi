package com.example.bibeetaxi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DriverSetupActivity extends AppCompatActivity {

    private EditText etName, etSurname, etCarModel, etCarNumber, etCurrentLocation;
    private Button btnSaveAndContinue;

    private FirebaseFirestore db;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_setup);

        etName = findViewById(R.id.etName);
        etSurname = findViewById(R.id.etSurname);
        etCarModel = findViewById(R.id.etCarModel);
        etCarNumber = findViewById(R.id.etCarNumber);
        etCurrentLocation = findViewById(R.id.etCurrentLocation);
        btnSaveAndContinue = findViewById(R.id.btnSaveAndContinue);

        db = FirebaseFirestore.getInstance();
        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        btnSaveAndContinue.setOnClickListener(v -> saveDriverProfile());
    }

    private void saveDriverProfile() {
        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String carModel = etCarModel.getText().toString().trim();
        String carNumber = etCarNumber.getText().toString().trim();
        String location = etCurrentLocation.getText().toString().trim();

        if (name.isEmpty() || surname.isEmpty() || carModel.isEmpty() || carNumber.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> driverProfile = new HashMap<>();
        driverProfile.put("name", name);
        driverProfile.put("surname", surname);
        driverProfile.put("carModel", carModel);
        driverProfile.put("carNumber", carNumber);
        driverProfile.put("currentLocation", location);
        driverProfile.put("userType", "driver");
        driverProfile.put("rating", 0.0);

        db.collection("users").document(driverId)
                .set(driverProfile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(DriverSetupActivity.this, "Профиль сохранён!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(DriverSetupActivity.this, MainDriverActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DriverSetupActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}