package com.example.bibeetaxi;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListFragment extends Fragment {

    private RecyclerView recyclerChatList;
    private ChatListAdapter adapter;
    private List<ChatInfo> chatList = new ArrayList<>();
    private DatabaseReference chatsRef;
    private String currentUserId;
    private FirebaseFirestore firestore;
    private final Map<String, String> userNamesCache = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);
        recyclerChatList = view.findViewById(R.id.recyclerChatList);
        recyclerChatList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatListAdapter(chatList);
        recyclerChatList.setAdapter(adapter);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        firestore = FirebaseFirestore.getInstance();

        loadChats();

        adapter.setOnItemClickListener((otherUserId) -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("otherUserId", otherUserId);
            startActivity(intent);
        });

        return view;
    }

    private void loadChats() {
        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    if (chatId != null && chatId.contains(currentUserId)) {
                        String otherUserId = chatId.replace(currentUserId + "_", "").replace("_" + currentUserId, "");
                        if (otherUserId.equals(currentUserId)) continue;
                        ChatInfo info = new ChatInfo();
                        info.otherUserId = otherUserId;
                        info.displayName = "Загрузка...";
                        chatList.add(info);
                        loadUserName(info);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Можно показать Toast, но не обязательно
            }
        });
    }

    private void loadUserName(ChatInfo info) {
        if (userNamesCache.containsKey(info.otherUserId)) {
            info.displayName = userNamesCache.get(info.otherUserId);
            adapter.notifyDataSetChanged();
            return;
        }

        firestore.collection("users").document(info.otherUserId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    String surname = doc.getString("surname");
                    String displayName = (name != null ? name : "") + " " + (surname != null ? surname : "");
                    if (displayName.trim().isEmpty()) displayName = "Пользователь";
                    userNamesCache.put(info.otherUserId, displayName);
                    info.displayName = displayName;
                    adapter.notifyDataSetChanged();
                });
    }

    static class ChatInfo {
        String otherUserId;
        String displayName;
    }

    static class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

        private List<ChatInfo> list;
        private OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(String otherUserId);
        }

        ChatListAdapter(List<ChatInfo> list) {
            this.list = list;
        }

        void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatInfo info = list.get(position);
            holder.textView.setText(info.displayName);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(info.otherUserId);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
                textView.setPadding(32, 24, 32, 24);
                textView.setTextSize(16);
            }
        }
    }
}