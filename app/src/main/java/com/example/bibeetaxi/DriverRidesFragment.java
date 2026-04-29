package com.example.bibeetaxi;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DriverRidesFragment extends Fragment {

    private ListView lvRides;
    private ArrayAdapter<String> adapter;
    private List<String> rideDisplayList;
    private List<String> rideIdList;
    private List<String> passengerIdList;
    private List<Double> fromLatList, fromLonList, toLatList, toLonList;
    private FirebaseFirestore db;
    private String driverId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_rides, container, false);
        lvRides = view.findViewById(R.id.lvRides);

        rideDisplayList = new ArrayList<>();
        rideIdList = new ArrayList<>();
        passengerIdList = new ArrayList<>();
        fromLatList = new ArrayList<>();
        fromLonList = new ArrayList<>();
        toLatList = new ArrayList<>();
        toLonList = new ArrayList<>();

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, rideDisplayList);
        lvRides.setAdapter(adapter);
        db = FirebaseFirestore.getInstance();
        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadAvailableRides();

        lvRides.setOnItemClickListener((parent, view1, position, id) -> {
            String rideId = rideIdList.get(position);
            String passengerId = passengerIdList.get(position);
            double fromLat = fromLatList.get(position);
            double fromLon = fromLonList.get(position);
            double toLat = toLatList.get(position);
            double toLon = toLonList.get(position);

            Intent intent = new Intent(getActivity(), RouteMapActivity.class);
            intent.putExtra("rideId", rideId);
            intent.putExtra("passengerId", passengerId);
            intent.putExtra("fromLat", fromLat);
            intent.putExtra("fromLon", fromLon);
            intent.putExtra("toLat", toLat);
            intent.putExtra("toLon", toLon);
            startActivity(intent);
        });

        return view;
    }

    private void loadAvailableRides() {
        db.collection("ride_requests")
                .whereEqualTo("status", "waiting")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    rideDisplayList.clear();
                    rideIdList.clear();
                    passengerIdList.clear();
                    fromLatList.clear();
                    fromLonList.clear();
                    toLatList.clear();
                    toLonList.clear();

                    for (QueryDocumentSnapshot doc : value) {
                        String from = doc.getString("fromAddress");
                        String to = doc.getString("toAddress");
                        String city = doc.getString("city");
                        int maxPrice = doc.getLong("maxPrice") != null ? doc.getLong("maxPrice").intValue() : 0;
                        String passengerId = doc.getString("passengerId");

                        double fromLat = doc.getDouble("fromLat") != null ? doc.getDouble("fromLat") : 0;
                        double fromLon = doc.getDouble("fromLon") != null ? doc.getDouble("fromLon") : 0;
                        double toLat = doc.getDouble("toLat") != null ? doc.getDouble("toLat") : 0;
                        double toLon = doc.getDouble("toLon") != null ? doc.getDouble("toLon") : 0;

                        String display = city + ": " + from + " → " + to + " (до " + maxPrice + "₽)";
                        rideDisplayList.add(display);
                        rideIdList.add(doc.getId());
                        passengerIdList.add(passengerId);
                        fromLatList.add(fromLat);
                        fromLonList.add(fromLon);
                        toLatList.add(toLat);
                        toLonList.add(toLon);
                    }
                    adapter.notifyDataSetChanged();
                    // Исправление: Toast только если есть контекст
                    if (rideDisplayList.isEmpty() && isAdded()) {
                        Toast.makeText(getContext(), "Нет доступных заказов", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}