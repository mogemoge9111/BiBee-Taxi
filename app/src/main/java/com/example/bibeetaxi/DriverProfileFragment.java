package com.example.bibeetaxi;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DriverProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView ivProfilePhoto;
    private EditText etName, etSurname, etCarModel, etCarNumber;
    private TextView tvRating;
    private Button btnChangePhoto, btnSaveProfile, btnLogout, btnSupport;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_profile, container, false);

        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto);
        etName = view.findViewById(R.id.etName);
        etSurname = view.findViewById(R.id.etSurname);
        etCarModel = view.findViewById(R.id.etCarModel);
        etCarNumber = view.findViewById(R.id.etCarNumber);
        tvRating = view.findViewById(R.id.tvRating);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnSupport = view.findViewById(R.id.btnSupport);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        loadUserData();

        btnChangePhoto.setOnClickListener(v -> openFileChooser());
        btnSaveProfile.setOnClickListener(v -> saveUserData());
        btnLogout.setOnClickListener(v -> showLogoutDialog());
        btnSupport.setOnClickListener(v -> startActivity(new Intent(getActivity(), SupportActivity.class)));

        return view;
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Выберите фото"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                ivProfilePhoto.setImageBitmap(bitmap);
                savePhotoToFirestore(base64Image);
            } catch (IOException e) {
                Toast.makeText(getContext(), "Ошибка загрузки фото", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void savePhotoToFirestore(String base64Image) {
        db.collection("users").document(currentUserId)
                .update("photoBase64", base64Image)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Фото сохранено", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show());
    }

    private void saveUserData() {
        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String carModel = etCarModel.getText().toString().trim();
        String carNumber = etCarNumber.getText().toString().trim();

        if (name.isEmpty() || surname.isEmpty() || carModel.isEmpty() || carNumber.isEmpty()) {
            Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        DriverProfile profile = new DriverProfile(name, surname, mAuth.getCurrentUser().getEmail(), "driver", carModel, carNumber);
        db.collection("users").document(currentUserId)
                .set(profile, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Профиль сохранён", Toast.LENGTH_SHORT).show());
    }

    private void loadUserData() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etName.setText(documentSnapshot.getString("name"));
                        etSurname.setText(documentSnapshot.getString("surname"));
                        etCarModel.setText(documentSnapshot.getString("carModel"));
                        etCarNumber.setText(documentSnapshot.getString("carNumber"));
                        String base64Photo = documentSnapshot.getString("photoBase64");
                        if (base64Photo != null && !base64Photo.isEmpty()) {
                            byte[] decodedString = Base64.decode(base64Photo, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivProfilePhoto.setImageBitmap(decodedByte);
                        }
                    }
                });
        loadRating();
    }

    private void loadRating() {
        db.collection("reviews").whereEqualTo("revieweeId", currentUserId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double total = 0;
                    int count = queryDocumentSnapshots.size();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Double rating = doc.getDouble("rating");
                        if (rating != null) total += rating;
                    }
                    double avg = count > 0 ? total / count : 0.0;
                    tvRating.setText(String.format("Рейтинг: %.1f", avg));
                });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Выход")
                .setMessage("Вы точно хотите выйти?")
                .setPositiveButton("Да", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    static class DriverProfile {
        public String name, surname, email, userType, carModel, carNumber;
        public DriverProfile() {}
        public DriverProfile(String name, String surname, String email, String userType, String carModel, String carNumber) {
            this.name = name;
            this.surname = surname;
            this.email = email;
            this.userType = userType;
            this.carModel = carModel;
            this.carNumber = carNumber;
        }
    }
}