package com.example.itproyek2;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);

        // atur padding biar gak ketutup status bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // init view nya
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        MaterialCardView btnBack = findViewById(R.id.btnBack);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnBack.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            validateAndRegister();
        });
    }

    private void validateAndRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // cek kalo ada yg kosong
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || 
            TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            showToast("isi dulu semua kolomnya ya");
            return;
        }

        // cek format email bener gak
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("emailnya gak valid tuh");
            return;
        }

        // cek panjang pass nya
        if (password.length() < 6) {
            showToast("pass minimal 6 karakter ya");
            return;
        }

        // cek pass nya sama gak
        if (!password.equals(confirmPassword)) {
            showToast("konfirmasi pass gak cocok");
            return;
        }

        // kalo aman semua
        boolean registered = dbHelper.registerUser(name, email, password);
        if (registered) {
            Toast.makeText(this, "Akun " + name + " berhasil dibuat!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            showToast("Email sudah terdaftar, coba email lain.");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}