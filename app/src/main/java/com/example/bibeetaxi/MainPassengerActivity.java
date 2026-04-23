package com.example.bibeetaxi;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainPassengerActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String passengerId;
    private boolean isConfirmationShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_passenger);

        db = FirebaseFirestore.getInstance();
        passengerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(navListener);

        // Сначала показываем карту (она загрузится с задержкой внутри MapFragment)
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new MapFragment())
                .commit();

        // Слушаем подтверждения
        listenForAcceptedRides();
    }

    private final BottomNavigationView.OnItemSelectedListener navListener =
            new BottomNavigationView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;
                    int id = item.getItemId();
                    if (id == R.id.nav_map) {
                        selectedFragment = new MapFragment();
                    } else if (id == R.id.nav_profile) {
                        selectedFragment = new ProfileFragment();
                    } else if (id == R.id.nav_chat) {
                        selectedFragment = new ChatListFragment();
                    } else if (id == R.id.nav_rides) {
                        selectedFragment = new RidesFragment();
                    }
                    if (selectedFragment != null) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, selectedFragment)
                                .commit();
                    }
                    return true;
                }
            };

    private void listenForAcceptedRides() {
        db.collection("accepted_rides")
                .whereEqualTo("passengerId", passengerId)
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || value.isEmpty()) return;
                    if (isConfirmationShowing) return;
                    for (QueryDocumentSnapshot doc : value) {
                        String acceptedRideId = doc.getId();
                        String rideId = doc.getString("rideId");
                        String driverId = doc.getString("driverId");

                        isConfirmationShowing = true;
                        Intent intent = new Intent(MainPassengerActivity.this, RideConfirmationActivity.class);
                        intent.putExtra("acceptedRideId", acceptedRideId);
                        intent.putExtra("rideId", rideId);
                        intent.putExtra("driverId", driverId);
                        intent.putExtra("passengerId", passengerId);
                        startActivity(intent);
                        break;
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isConfirmationShowing = false;
    }
}