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

public class CreateRideRequestActivity extends AppCompatActivity {

    private EditText etFromAddress, etToAddress, etCity, etMaxPrice, etPassengers, etCargo;
    private Button btnSubmitRequest;
    private FirebaseFirestore db;
    private String passengerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ride_request);

        etFromAddress = findViewById(R.id.etFromAddress);
        etToAddress = findViewById(R.id.etToAddress);
        etCity = findViewById(R.id.etCity);
        etMaxPrice = findViewById(R.id.etMaxPrice);
        etPassengers = findViewById(R.id.etPassengers);
        etCargo = findViewById(R.id.etCargo);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);

        db = FirebaseFirestore.getInstance();
        passengerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        btnSubmitRequest.setOnClickListener(v -> submitRequest());
    }

    private void submitRequest() {
        String from = etFromAddress.getText().toString().trim();
        String to = etToAddress.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String maxPriceStr = etMaxPrice.getText().toString().trim();
        String passengers = etPassengers.getText().toString().trim();
        String cargo = etCargo.getText().toString().trim();

        if (from.isEmpty() || to.isEmpty() || city.isEmpty() || maxPriceStr.isEmpty()) {
            Toast.makeText(this, "Заполните обязательные поля", Toast.LENGTH_SHORT).show();
            return;
        }

        int maxPrice = Integer.parseInt(maxPriceStr);

        // Шутка при большой стоимости
        if (maxPrice > 100000) {
            Toast.makeText(this, "Ого, да вы богач! 🤑 Может, купите нам по кофе?", Toast.LENGTH_LONG).show();
        }

        Map<String, Object> request = new HashMap<>();
        request.put("passengerId", passengerId);
        request.put("fromAddress", from);
        request.put("toAddress", to);
        request.put("city", city);
        request.put("maxPrice", maxPrice);
        request.put("passengers", passengers);
        request.put("cargo", cargo);
        request.put("status", "waiting");
        request.put("timestamp", System.currentTimeMillis());

        db.collection("ride_requests").add(request)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Запрос отправлен!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}