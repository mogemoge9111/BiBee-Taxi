package com.example.bibeetaxi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerChat;
    private EditText editMessage;
    private Button buttonSend;
    private TextView tvChatTitle;
    private ImageView ivBack;

    private ChatAdapter adapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private DatabaseReference chatRef;
    private String currentUserId;
    private String otherUserId;
    private String chatRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerChat = findViewById(R.id.recycler_chat);
        editMessage = findViewById(R.id.edit_message);
        buttonSend = findViewById(R.id.button_send);
        tvChatTitle = findViewById(R.id.tvChatTitle);
        ivBack = findViewById(R.id.ivBack);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        otherUserId = getIntent().getStringExtra("otherUserId");

        if (otherUserId == null) {
            Toast.makeText(this, "Ошибка: собеседник не указан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (currentUserId.compareTo(otherUserId) < 0) {
            chatRoomId = currentUserId + "_" + otherUserId;
        } else {
            chatRoomId = otherUserId + "_" + currentUserId;
        }

        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatRoomId).child("messages");

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

        ivBack.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, MainPassengerActivity.class);
            intent.putExtra("selected_tab", R.id.nav_chat);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        tvChatTitle.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, DriverProfileViewActivity.class);
            intent.putExtra("driverId", otherUserId);
            startActivity(intent);
        });
    }

    private void loadOtherUserName() {
        FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String surname = doc.getString("surname");
                        String displayName = (name != null ? name : "") + " " + (surname != null ? surname : "");
                        tvChatTitle.setText(displayName.trim().isEmpty() ? "Чат" : displayName);
                    }
                });
    }

    private void sendMessage(String text) {
        String messageId = chatRef.push().getKey();
        ChatMessage message = new ChatMessage(currentUserId, otherUserId, text, System.currentTimeMillis());
        if (messageId != null) {
            chatRef.child(messageId).setValue(message)
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка отправки", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadMessages() {
        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    messageList.add(message);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    recyclerChat.scrollToPosition(messageList.size() - 1);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Ошибка загрузки сообщений", Toast.LENGTH_SHORT).show();
            }
        });
    }
}