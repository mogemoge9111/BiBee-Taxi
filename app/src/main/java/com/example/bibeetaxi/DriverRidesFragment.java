package com.example.bibeetaxi;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverRidesFragment extends Fragment {

    private ListView lvRides;
    private ArrayAdapter<String> adapter;
    private List<String> rideDisplayList;
    private List<String> rideIdList;
    private List<String> passengerIdList;
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
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, rideDisplayList);
        lvRides.setAdapter(adapter);
        db = FirebaseFirestore.getInstance();
        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadAvailableRides();

        lvRides.setOnItemClickListener((parent, view1, position, id) -> {
            String rideId = rideIdList.get(position);
            String passengerId = passengerIdList.get(position);
            acceptRide(rideId, passengerId);
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
                    for (QueryDocumentSnapshot doc : value) {
                        String from = doc.getString("fromAddress");
                        String to = doc.getString("toAddress");
                        String city = doc.getString("city");
                        int maxPrice = doc.getLong("maxPrice") != null ? doc.getLong("maxPrice").intValue() : 0;
                        String passengerId = doc.getString("passengerId");
                        String display = city + ": " + from + " → " + to + " (до " + maxPrice + "₽)";
                        rideDisplayList.add(display);
                        rideIdList.add(doc.getId());
                        passengerIdList.add(passengerId);
                    }
                    adapter.notifyDataSetChanged();
                    if (rideDisplayList.isEmpty()) {
                        Toast.makeText(getContext(), "Нет доступных заказов", Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private void acceptRide(String rideId, String passengerId) {
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
                    createChat(driverId, passengerId);
                    Toast.makeText(getContext(), "Заказ принят. Ожидайте подтверждения пассажира.", Toast.LENGTH_SHORT).show();
                });
    }

    private void createChat(String driverId, String passengerId) {
        if (driverId.equals(passengerId)) {
            return;
        }
        String chatId = driverId.compareTo(passengerId) < 0 ? driverId + "_" + passengerId : passengerId + "_" + driverId;
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("info");
        Map<String, String> info = new HashMap<>();
        info.put("driverId", driverId);
        info.put("passengerId", passengerId);
        chatRef.setValue(info);
    }
}