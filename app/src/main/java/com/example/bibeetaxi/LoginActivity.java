package com.example.bibeetaxi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnAction;
    private TextView tvToggle;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isLoginMode = true;
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userType = getIntent().getStringExtra("userType");
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnAction = findViewById(R.id.btnAction);
        tvToggle = findViewById(R.id.tvToggle);
        progressBar = findViewById(R.id.progressBar);

        updateUI();

        btnAction.setOnClickListener(v -> {
            if (isLoginMode) {
                loginUser();
            } else {
                registerUser();
            }
        });

        tvToggle.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUI();
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserTypeAndProceed(currentUser.getUid());
        }
    }

    private void updateUI() {
        if (isLoginMode) {
            btnAction.setText("Войти");
            tvToggle.setText("Нет аккаунта? Зарегистрироваться");
        } else {
            btnAction.setText("Зарегистрироваться");
            tvToggle.setText("Уже есть аккаунт? Войти");
        }
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserTypeAndProceed(user.getUid());
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Ошибка входа: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            db.collection("users").document(user.getUid())
                                    .set(new UserProfile(user.getEmail(), userType))
                                    .addOnSuccessListener(aVoid -> navigateToNextScreen())
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(LoginActivity.this, "Ошибка сохранения профиля", Toast.LENGTH_SHORT).show();
                                        navigateToNextScreen(); // Всё равно идём дальше
                                    });
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Ошибка регистрации: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserTypeAndProceed(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String type = documentSnapshot.getString("userType");
                    if (type != null) {
                        userType = type;
                    }
                    navigateToNextScreen();
                })
                .addOnFailureListener(e -> navigateToNextScreen());
    }

    private void navigateToNextScreen() {
        if ("passenger".equals(userType)) {
            startActivity(new Intent(LoginActivity.this, MainPassengerActivity.class));
        } else {
            startActivity(new Intent(LoginActivity.this, MainDriverActivity.class));
        }
        finish();
    }

    private void setLoading(boolean loading) {
        btnAction.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        tvToggle.setEnabled(!loading);
        progressBar.setVisibility(loading ? ProgressBar.VISIBLE : ProgressBar.GONE);
    }

    public static class UserProfile {
        public String email;
        public String userType;

        public UserProfile() {}
        public UserProfile(String email, String userType) {
            this.email = email;
            this.userType = userType;
        }
    }
}