package com.example.bibeetaxi;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<RideHistoryItem> items = new ArrayList<>();
    private FirebaseFirestore db;
    private String passengerId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        recyclerView = view.findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(items, this::onItemClick);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        passengerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadHistory();

        return view;
    }

    private void loadHistory() {
        db.collection("ride_requests")
                .whereEqualTo("passengerId", passengerId)
                .orderBy("timestamp")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    items.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        String from = doc.getString("fromAddress");
                        String to = doc.getString("toAddress");
                        String status = doc.getString("status");
                        String rideId = doc.getId();
                        items.add(new RideHistoryItem(rideId, from, to, status));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void onItemClick(RideHistoryItem item) {
        if ("waiting".equals(item.status) || "accepted".equals(item.status)) {
            showEditDialog(item);
        } else {
            Toast.makeText(getContext(), "Завершённый заказ нельзя редактировать", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditDialog(RideHistoryItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Редактировать заказ");
        builder.setMessage("Изменить адрес подачи или назначения?");
        builder.setPositiveButton("Да", (dialog, which) -> {
            Intent intent = new Intent(getActivity(), EditRideRequestActivity.class);
            intent.putExtra("rideId", item.rideId);
            startActivity(intent);
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    static class RideHistoryItem {
        String rideId;
        String from;
        String to;
        String status;

        RideHistoryItem(String rideId, String from, String to, String status) {
            this.rideId = rideId;
            this.from = from;
            this.to = to;
            this.status = status;
        }

        boolean isActive() {
            return "waiting".equals(status) || "accepted".equals(status);
        }
    }

    static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        private List<RideHistoryItem> list;
        private OnItemClickListener listener;

        interface OnItemClickListener {
            void onClick(RideHistoryItem item);
        }

        HistoryAdapter(List<RideHistoryItem> list, OnItemClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ride_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RideHistoryItem item = list.get(position);
            holder.tvRoute.setText(item.from + " → " + item.to);
            holder.tvStatus.setText(item.isActive() ? "Активный" : "Завершён");
            holder.tvStatus.setTextColor(item.isActive() ? Color.GREEN : Color.RED);
            holder.itemView.setOnClickListener(v -> listener.onClick(item));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRoute, tvStatus;
            ViewHolder(View itemView) {
                super(itemView);
                tvRoute = itemView.findViewById(R.id.tvRoute);
                tvStatus = itemView.findViewById(R.id.tvStatus);
            }
        }
    }
}