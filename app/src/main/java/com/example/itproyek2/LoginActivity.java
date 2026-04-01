package com.example.itproyek2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Terapkan tema sebelum super.onCreate
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("is_dark_theme", true);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Set Data Dummy ke Hint atau langsung isi untuk memudahkan demo
        // etEmail.setText("admin@gmail.com"); 
        // etPassword.setText("123456");

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi email dan kata sandi", Toast.LENGTH_SHORT).show();
            } else if (email.equals("admin@gmail.com") && password.equals("123456")) {
                Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show();
                prefs.edit().putBoolean("is_logged_in", true).apply();
                
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Email atau Kata Sandi salah!", Toast.LENGTH_SHORT).show();
            }
        });

        tvForgotPassword.setOnClickListener(v -> 
            Toast.makeText(this, "Fitur Reset Password belum tersedia", Toast.LENGTH_SHORT).show()
        );

        tvRegister.setOnClickListener(v -> 
            Toast.makeText(this, "Silakan gunakan admin@gmail.com / 123456", Toast.LENGTH_LONG).show()
        );
    }
}
