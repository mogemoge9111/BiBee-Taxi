package com.example.bibeetaxi;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditRideRequestActivity extends AppCompatActivity {

    private TextInputLayout tilFrom, tilTo, tilCity, tilMaxPrice, tilPassengers, tilCargo;
    private EditText etFrom, etTo, etCity, etMaxPrice, etPassengers, etCargo;
    private Button btnSaveChanges;
    private FirebaseFirestore db;
    private String rideId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ride_request);

        tilFrom = findViewById(R.id.tilFrom);
        tilTo = findViewById(R.id.tilTo);
        tilCity = findViewById(R.id.tilCity);
        tilMaxPrice = findViewById(R.id.tilMaxPrice);
        tilPassengers = findViewById(R.id.tilPassengers);
        tilCargo = findViewById(R.id.tilCargo);

        etFrom = tilFrom.getEditText();
        etTo = tilTo.getEditText();
        etCity = tilCity.getEditText();
        etMaxPrice = tilMaxPrice.getEditText();
        etPassengers = tilPassengers.getEditText();
        etCargo = tilCargo.getEditText();

        btnSaveChanges = findViewById(R.id.btnSubmitRequest);
        btnSaveChanges.setText("Сохранить изменения");

        // Скрываем кнопки проверки адресов, они не нужны при редактировании
        findViewById(R.id.btnCheckFrom).setVisibility(Button.GONE);
        findViewById(R.id.btnCheckTo).setVisibility(Button.GONE);

        db = FirebaseFirestore.getInstance();
        rideId = getIntent().getStringExtra("rideId");

        if (rideId == null) {
            Toast.makeText(this, "Ошибка: ID заказа не передан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadRideData();

        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void loadRideData() {
        db.collection("ride_requests").document(rideId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etFrom.setText(documentSnapshot.getString("fromAddress"));
                        etTo.setText(documentSnapshot.getString("toAddress"));
                        etCity.setText(documentSnapshot.getString("city"));
                        Long maxPrice = documentSnapshot.getLong("maxPrice");
                        if (maxPrice != null) {
                            etMaxPrice.setText(String.valueOf(maxPrice));
                        }
                        etPassengers.setText(documentSnapshot.getString("passengers"));
                        etCargo.setText(documentSnapshot.getString("cargo"));
                    } else {
                        Toast.makeText(this, "Заказ не найден", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void saveChanges() {
        String from = etFrom.getText().toString().trim();
        String to = etTo.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String maxPriceStr = etMaxPrice.getText().toString().trim();
        String passengers = etPassengers.getText().toString().trim();
        String cargo = etCargo.getText().toString().trim();

        if (from.isEmpty() || to.isEmpty() || city.isEmpty() || maxPriceStr.isEmpty()) {
            Toast.makeText(this, "Заполните обязательные поля", Toast.LENGTH_SHORT).show();
            return;
        }

        int maxPrice;
        try {
            maxPrice = Integer.parseInt(maxPriceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Некорректная цена", Toast.LENGTH_SHORT).show();
            return;
        }

        if (maxPrice > 100000) {
            Toast.makeText(this, "Ого, да вы богач! 🤑 Может, купите нам по кофе?", Toast.LENGTH_LONG).show();
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fromAddress", from);
        updates.put("toAddress", to);
        updates.put("city", city);
        updates.put("maxPrice", maxPrice);
        updates.put("passengers", passengers);
        updates.put("cargo", cargo);

        db.collection("ride_requests").document(rideId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Изменения сохранены", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}