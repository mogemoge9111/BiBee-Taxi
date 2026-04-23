package com.example.bibeetaxi;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RidesFragment extends Fragment {

    private RecyclerView recyclerView;
    private RidesAdapter adapter;
    private List<RideItem> items = new ArrayList<>();
    private FirebaseFirestore db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rides, container, false);
        recyclerView = view.findViewById(R.id.recycler_rides);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RidesAdapter(items);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadRides();

        return view;
    }

    private void loadRides() {
        db.collection("ride_requests")
                .whereEqualTo("passengerId", userId)
                .orderBy("timestamp")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    items.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        RideItem item = new RideItem();
                        item.rideId = doc.getId();
                        item.from = doc.getString("fromAddress");
                        item.to = doc.getString("toAddress");
                        item.status = doc.getString("status");
                        item.driverId = doc.getString("driverId");
                        item.maxPrice = doc.getLong("maxPrice") != null ? doc.getLong("maxPrice").intValue() : 0;
                        items.add(item);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void onItemClick(RideItem item) {
        switch (item.status) {
            case "waiting":
            case "accepted":
                if (item.status.equals("waiting")) {
                    showEditDialog(item);
                } else {
                    Toast.makeText(getContext(), "Заказ уже принят водителем, ожидайте подтверждения", Toast.LENGTH_SHORT).show();
                }
                break;
            case "in_progress":
                // В работе – ничего не делаем
                break;
            case "rejected":
                showResumeDialog(item);
                break;
            case "completed":
                showRatingDialog(item, "driver");
                break;
        }
    }

    private void showEditDialog(RideItem item) {
        Intent intent = new Intent(getActivity(), EditRideRequestActivity.class);
        intent.putExtra("rideId", item.rideId);
        startActivity(intent);
    }

    private void showResumeDialog(RideItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Возобновить заказ?")
                .setMessage("Заказ будет снова отправлен водителям")
                .setPositiveButton("Да", (dialog, which) -> {
                    db.collection("ride_requests").document(item.rideId)
                            .update("status", "waiting", "driverId", null);
                    Toast.makeText(getContext(), "Заказ возобновлён", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showRatingDialog(RideItem item, String targetType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rating, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        TextView tvComment = dialogView.findViewById(R.id.tvComment);

        builder.setView(dialogView)
                .setTitle("Оцените " + (targetType.equals("driver") ? "водителя" : "пассажира"))
                .setPositiveButton("Отправить", (dialog, which) -> {
                    float rating = ratingBar.getRating();
                    String comment = tvComment.getText().toString().trim();

                    String targetId = targetType.equals("driver") ? item.driverId : userId;

                    Map<String, Object> review = new HashMap<>();
                    review.put("reviewerId", userId);
                    review.put("revieweeId", targetId);
                    review.put("rating", rating);
                    review.put("comment", comment);
                    review.put("timestamp", System.currentTimeMillis());
                    review.put("rideId", item.rideId);

                    db.collection("reviews").add(review)
                            .addOnSuccessListener(docRef -> {
                                Toast.makeText(getContext(), "Спасибо за отзыв!", Toast.LENGTH_SHORT).show();
                                updateUserRating(targetId);
                            });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void updateUserRating(String userId) {
        db.collection("reviews").whereEqualTo("revieweeId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double total = 0;
                    int count = queryDocumentSnapshots.size();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double rating = doc.getDouble("rating");
                        if (rating != null) total += rating;
                    }
                    double avg = count > 0 ? total / count : 0.0;
                    db.collection("users").document(userId).update("rating", avg);
                });
    }

    static class RideItem {
        String rideId;
        String from;
        String to;
        String status;
        String driverId;
        int maxPrice;
    }

    class RidesAdapter extends RecyclerView.Adapter<RidesAdapter.ViewHolder> {

        private List<RideItem> list;

        RidesAdapter(List<RideItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ride, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RideItem item = list.get(position);
            holder.tvRoute.setText(item.from + " → " + item.to);
            holder.tvStatus.setText(getStatusText(item.status));
            holder.tvStatus.setTextColor(getStatusColor(item.status));
            holder.tvPrice.setText(item.maxPrice + " ₽");

            if (item.driverId != null) {
                holder.btnChat.setVisibility(View.VISIBLE);
                holder.btnChat.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    intent.putExtra("otherUserId", item.driverId);
                    startActivity(intent);
                });
            } else {
                holder.btnChat.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(list.get(pos));
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRoute, tvStatus, tvPrice;
            Button btnChat;

            ViewHolder(View itemView) {
                super(itemView);
                tvRoute = itemView.findViewById(R.id.tvRoute);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvPrice = itemView.findViewById(R.id.tvPrice);
                btnChat = itemView.findViewById(R.id.btnChat);
            }
        }

        private String getStatusText(String status) {
            switch (status) {
                case "waiting": return "Ожидает";
                case "accepted": return "Принят";
                case "in_progress": return "В работе";
                case "rejected": return "Отклонён";
                case "completed": return "Завершён";
                default: return status;
            }
        }

        private int getStatusColor(String status) {
            switch (status) {
                case "waiting":
                case "accepted":
                    return Color.BLUE;
                case "in_progress":
                    return Color.YELLOW;
                case "rejected":
                    return Color.RED;
                case "completed":
                    return Color.GREEN;
                default:
                    return Color.GRAY;
            }
        }
    }
}