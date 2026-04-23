package com.example.bibeetaxi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.*;

public class SavedAddressesFragment extends Fragment {

    private ListView lvAddresses;
    private EditText etNewAddress;
    private Button btnAdd;
    private ArrayAdapter<String> adapter;
    private List<String> addresses;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_addresses, container, false);

        lvAddresses = view.findViewById(R.id.lvAddresses);
        etNewAddress = view.findViewById(R.id.etNewAddress);
        btnAdd = view.findViewById(R.id.btnAdd);

        prefs = getActivity().getSharedPreferences("BiBeeTaxiPrefs", 0);
        loadAddresses();

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, addresses);
        lvAddresses.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            String newAddr = etNewAddress.getText().toString().trim();
            if (!newAddr.isEmpty()) {
                addresses.add(newAddr);
                saveAddresses();
                adapter.notifyDataSetChanged();
                etNewAddress.setText("");
            }
        });

        lvAddresses.setOnItemClickListener((parent, view1, position, id) -> {
            // Можно реализовать выбор адреса для быстрого построения маршрута
            String selected = addresses.get(position);
            Toast.makeText(getContext(), "Выбран: " + selected, Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void loadAddresses() {
        Set<String> set = prefs.getStringSet("saved_addresses", new HashSet<>());
        addresses = new ArrayList<>(set);
    }

    private void saveAddresses() {
        prefs.edit().putStringSet("saved_addresses", new HashSet<>(addresses)).apply();
    }
}