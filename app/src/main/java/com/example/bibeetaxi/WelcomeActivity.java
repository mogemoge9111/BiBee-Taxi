package com.example.bibeetaxi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private Button btnPassenger, btnDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        btnPassenger = findViewById(R.id.btnPassenger);
        btnDriver = findViewById(R.id.btnDriver);

        btnPassenger.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            intent.putExtra("userType", "passenger");
            startActivity(intent);
        });

        btnDriver.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            intent.putExtra("userType", "driver");
            startActivity(intent);
        });
    }
}