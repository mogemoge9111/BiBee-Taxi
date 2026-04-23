package com.example.bibeetaxi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class ChatFragment extends Fragment {

    private ListView lvChat;
    private EditText etMessage;
    private Button btnSend;
    private DatabaseReference dbRef;
    private List<ChatMessage> messages;
    private ArrayAdapter<String> adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        lvChat = view.findViewById(R.id.lvChat);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);

        messages = new ArrayList<>();
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        lvChat.setAdapter(adapter);

        dbRef = FirebaseDatabase.getInstance().getReference("support_chat").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        btnSend.setOnClickListener(v -> sendMessage());
        loadMessages();

        return view;
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        ChatMessage msg = new ChatMessage(FirebaseAuth.getInstance().getCurrentUser().getUid(), text, System.currentTimeMillis());
        dbRef.push().setValue(msg);
        etMessage.setText("");
    }

    private void loadMessages() {
        dbRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);
                messages.add(msg);
                adapter.add(msg.text);
                adapter.notifyDataSetChanged();
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    static class ChatMessage {
        public String userId;
        public String text;
        public long timestamp;

        public ChatMessage() {}
        public ChatMessage(String userId, String text, long timestamp) {
            this.userId = userId;
            this.text = text;
            this.timestamp = timestamp;
        }
    }
}