package com.example.bibeetaxi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yandex.mapkit.geometry.Geometry;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.Session;
import com.yandex.runtime.Error;

import java.util.HashMap;
import java.util.Map;

public class CreateRideRequestActivity extends AppCompatActivity implements Session.SearchListener {

    private TextInputLayout tilFrom, tilTo, tilCity, tilMaxPrice, tilPassengers, tilCargo;
    private EditText etFrom, etTo, etCity, etMaxPrice, etPassengers, etCargo;
    private Button btnCheckFrom, btnCheckTo, btnSubmitRequest;

    private FirebaseFirestore db;
    private String passengerId;
    private SearchManager searchManager;
    private Session searchSession;
    private boolean fromChecked = false, toChecked = false;
    private Point fromPoint, toPoint;
    private boolean checkingFrom = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ride_request);


        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

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

        btnCheckFrom = findViewById(R.id.btnCheckFrom);
        btnCheckTo = findViewById(R.id.btnCheckTo);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);

        db = FirebaseFirestore.getInstance();
        passengerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);

        Intent intent = getIntent();
        if (intent.hasExtra("fromAddress")) {
            etFrom.setText(intent.getStringExtra("fromAddress"));
            fromChecked = true;
            btnCheckFrom.setText("✓ Адрес найден");
        }
        if (intent.hasExtra("toAddress")) {
            etTo.setText(intent.getStringExtra("toAddress"));
            toChecked = true;
            btnCheckTo.setText("✓ Адрес найден");
        }
        if (intent.hasExtra("city")) {
            etCity.setText(intent.getStringExtra("city"));
        }
        if (intent.hasExtra("maxPrice")) {
            int maxPrice = intent.getIntExtra("maxPrice", 0);
            if (maxPrice > 0) {
                etMaxPrice.setText(String.valueOf(maxPrice));
            }
        }

        btnCheckFrom.setOnClickListener(v -> {
            String addr = etFrom.getText().toString().trim();
            if (addr.isEmpty()) {
                tilFrom.setError("Введите адрес");
                return;
            }
            checkingFrom = true;
            checkAddress(addr);
        });

        btnCheckTo.setOnClickListener(v -> {
            String addr = etTo.getText().toString().trim();
            if (addr.isEmpty()) {
                tilTo.setError("Введите адрес");
                return;
            }
            checkingFrom = false;
            checkAddress(addr);
        });

        btnSubmitRequest.setOnClickListener(v -> submitRequest());
    }

    private void checkAddress(String address) {
        Geometry geometry = Geometry.fromPoint(new Point(55.751244, 37.618423));
        searchSession = searchManager.submit(address, geometry, new SearchOptions(), this);
    }

    @Override
    public void onSearchResponse(@NonNull com.yandex.mapkit.search.Response response) {
        if (response.getCollection().getChildren().isEmpty()) {
            Toast.makeText(this, "Адрес не найден", Toast.LENGTH_SHORT).show();
            if (checkingFrom) tilFrom.setError("Адрес не найден");
            else tilTo.setError("Адрес не найден");
            return;
        }
        Point point = response.getCollection().getChildren().get(0).getObj().getGeometry().get(0).getPoint();
        if (checkingFrom) {
            fromPoint = point;
            fromChecked = true;
            tilFrom.setError(null);
            btnCheckFrom.setText("✓ Адрес найден");
        } else {
            toPoint = point;
            toChecked = true;
            tilTo.setError(null);
            btnCheckTo.setText("✓ Адрес найден");
        }
    }

    @Override
    public void onSearchError(@NonNull Error error) {
        Toast.makeText(this, "Ошибка поиска", Toast.LENGTH_SHORT).show();
    }

    private void submitRequest() {
        tilFrom.setError(null);
        tilTo.setError(null);
        tilCity.setError(null);
        tilMaxPrice.setError(null);

        String from = etFrom.getText().toString().trim();
        String to = etTo.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String maxPriceStr = etMaxPrice.getText().toString().trim();

        if (TextUtils.isEmpty(from) || TextUtils.isEmpty(to) || TextUtils.isEmpty(city) || TextUtils.isEmpty(maxPriceStr)) {
            Toast.makeText(this, "Заполните обязательные поля", Toast.LENGTH_SHORT).show();
            return;
        }
        int maxPrice;
        try {
            maxPrice = Integer.parseInt(maxPriceStr);
            if (maxPrice <= 0) { tilMaxPrice.setError("Цена > 0"); return; }
        } catch (NumberFormatException e) {
            tilMaxPrice.setError("Введите число");
            return;
        }

        if (!fromChecked) { tilFrom.setError("Проверьте адрес отправления"); return; }
        if (!toChecked) { tilTo.setError("Проверьте адрес назначения"); return; }

        Map<String, Object> request = new HashMap<>();
        request.put("passengerId", passengerId);
        request.put("fromAddress", from);
        request.put("toAddress", to);
        request.put("city", city);
        request.put("maxPrice", maxPrice);
        if (fromPoint != null) {
            request.put("fromLat", fromPoint.getLatitude());
            request.put("fromLon", fromPoint.getLongitude());
        }
        if (toPoint != null) {
            request.put("toLat", toPoint.getLatitude());
            request.put("toLon", toPoint.getLongitude());
        }
        request.put("status", "waiting");
        request.put("timestamp", System.currentTimeMillis());

        db.collection("ride_requests").add(request)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Заказ создан", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show());
    }
}