package com.example.bibeetaxi;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerChat;
    private EditText editMessage;
    private Button buttonSend;
    private TextView tvChatTitle;
    private View btnBack;

    private Button btnDriverArrived, btnPickupPassenger, btnCompleteRide;

    private ChatAdapter adapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private DatabaseReference chatRef;
    private String currentUserId;
    private String otherUserId;
    private String chatRoomId;
    private String rideId;
    private String driverId;
    private String passengerId;
    private String currentStatus;

    private FirebaseFirestore db;
    private ListenerRegistration rideListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerChat = findViewById(R.id.recycler_chat);
        editMessage = findViewById(R.id.edit_message);
        buttonSend = findViewById(R.id.button_send);
        tvChatTitle = findViewById(R.id.tvChatTitle);
        btnBack = findViewById(R.id.ivBack);
        btnDriverArrived = findViewById(R.id.btn_driver_arrived);
        btnPickupPassenger = findViewById(R.id.btn_pickup_passenger);
        btnCompleteRide = findViewById(R.id.btn_complete_ride);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        otherUserId = getIntent().getStringExtra("otherUserId");
        rideId = getIntent().getStringExtra("rideId");

        if (otherUserId == null) { finish(); return; }

        if (currentUserId.compareTo(otherUserId) < 0) {
            chatRoomId = currentUserId + "_" + otherUserId;
        } else {
            chatRoomId = otherUserId + "_" + currentUserId;
        }

        db = FirebaseFirestore.getInstance();
        if (rideId == null) {
            loadRideIdFromChatInfo();
        } else {
            startRideListener();
        }

        chatRef = FirebaseDatabase.getInstance().getReference("chats")
                .child(chatRoomId).child("messages");

        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(messageList, currentUserId);
        recyclerChat.setAdapter(adapter);

        loadOtherUserName();
        loadMessages();

        buttonSend.setOnClickListener(v -> {
            String text = editMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                editMessage.setText("");
            }
        });

        btnBack.setOnClickListener(v -> finish());

        tvChatTitle.setOnClickListener(v -> {
            Intent intent = new Intent(this, DriverProfileViewActivity.class);
            intent.putExtra("driverId", otherUserId);
            startActivity(intent);
        });

        btnDriverArrived.setOnClickListener(v -> updateRideStatus("driver_arrived"));
        btnPickupPassenger.setOnClickListener(v -> updateRideStatus("passenger_picked"));
        btnCompleteRide.setOnClickListener(v -> updateRideStatus("completed"));
    }

    private void loadRideIdFromChatInfo() {
        DatabaseReference infoRef = FirebaseDatabase.getInstance()
                .getReference("chats").child(chatRoomId).child("info");
        infoRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                rideId = snapshot.child("rideId").getValue(String.class);
                if (rideId != null) {
                    startRideListener();
                }
            }
        });
    }

    private void startRideListener() {
        if (rideId == null) return;
        rideListener = db.collection("ride_requests").document(rideId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    currentStatus = snapshot.getString("status");
                    driverId = snapshot.getString("driverId");
                    passengerId = snapshot.getString("passengerId");
                    updateButtons();

                    if ("completed".equals(currentStatus)) {
                        checkAndShowRating();
                    }
                });
    }

    private void updateButtons() {
        if (currentStatus == null) return;
        boolean isDriver = currentUserId.equals(driverId);

        btnDriverArrived.setVisibility(View.GONE);
        btnPickupPassenger.setVisibility(View.GONE);
        btnCompleteRide.setVisibility(View.GONE);

        switch (currentStatus) {
            case "accepted":
                if (isDriver) btnDriverArrived.setVisibility(View.VISIBLE);
                break;
            case "driver_arrived":
                if (isDriver) btnPickupPassenger.setVisibility(View.VISIBLE);
                break;
            case "passenger_picked":

                btnCompleteRide.setVisibility(View.VISIBLE);
                break;
            case "waiting":
                break;
        }
    }

    private void updateRideStatus(String newStatus) {
        if (rideId == null) return;
        db.collection("ride_requests").document(rideId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {

                    currentStatus = newStatus;
                    updateButtons();

                    notifyOtherUser(newStatus);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка обновления", Toast.LENGTH_SHORT).show());
    }

    private void notifyOtherUser(String status) {
        String otherUid = currentUserId.equals(driverId) ? passengerId : driverId;
        if (otherUid == null) return;

        String title = "BiBee Taxi";
        String message = "";
        switch (status) {
            case "driver_arrived": message = "Водитель на месте!"; break;
            case "passenger_picked": message = "Поехали! Водитель начал поездку."; break;
            case "completed":
                message = "Поездка завершена! Оставьте отзыв.";
                break;
        }


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MapApplication.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);


        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("otherUserId", otherUid);
        intent.putExtra("rideId", rideId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(rideId.hashCode(), builder.build());
    }

    private void checkAndShowRating() {

        db.collection("reviews")
                .whereEqualTo("rideId", rideId)
                .whereEqualTo("reviewerId", currentUserId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        showRatingDialog();
                    }
                });
    }

    private void showRatingDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rating, null);
        android.widget.RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText etComment = dialogView.findViewById(R.id.tvComment);

        new AlertDialog.Builder(this)
                .setTitle("Оцените поездку")
                .setView(dialogView)
                .setPositiveButton("Отправить", (dialog, which) -> {
                    float stars = ratingBar.getRating();
                    String comment = etComment.getText().toString().trim();
                    String revieweeId = currentUserId.equals(driverId) ? passengerId : driverId;

                    Map<String, Object> review = new HashMap<>();
                    review.put("reviewerId", currentUserId);
                    review.put("revieweeId", revieweeId);
                    review.put("rating", stars);
                    review.put("comment", comment);
                    review.put("rideId", rideId);
                    review.put("timestamp", System.currentTimeMillis());

                    db.collection("reviews").add(review)
                            .addOnSuccessListener(doc -> {
                                Toast.makeText(this, "Спасибо за отзыв!", Toast.LENGTH_SHORT).show();
                                updateUserRating(revieweeId);
                            });
                })
                .setNegativeButton("Позже", null)
                .show();
    }

    private void updateUserRating(String userId) {
        db.collection("reviews").whereEqualTo("revieweeId", userId).get()
                .addOnSuccessListener(snapshots -> {
                    double total = 0;
                    int count = snapshots.size();
                    for (DocumentSnapshot doc : snapshots) {
                        Double r = doc.getDouble("rating");
                        if (r != null) total += r;
                    }
                    double avg = count > 0 ? total / count : 0.0;
                    db.collection("users").document(userId).update("rating", avg);
                });
    }

    private void loadOtherUserName() {
        db.collection("users").document(otherUserId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    String surname = doc.getString("surname");
                    tvChatTitle.setText(((name != null) ? name : "") + " " + ((surname != null) ? surname : ""));
                });
    }

    private void sendMessage(String text) {
        String msgId = chatRef.push().getKey();
        ChatMessage msg = new ChatMessage(currentUserId, otherUserId, text, System.currentTimeMillis());
        if (msgId != null) chatRef.child(msgId).setValue(msg);
    }

    private void loadMessages() {
        chatRef.addChildEventListener(new ChildEventListener() {
            @Override public void onChildAdded(@NonNull DataSnapshot snapshot, String prev) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);
                if (msg != null) {
                    messageList.add(msg);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    recyclerChat.scrollToPosition(messageList.size() - 1);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String prev) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String prev) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (rideListener != null) rideListener.remove();
    }
}