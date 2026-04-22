package com.example.itproyek2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // atur tema dulu sebelum super.onCreate
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("is_dark_theme", true);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // isi data dummy biar gampang demonya atau lgsung isi aja
        // etEmail.setText("admin@gmail.com"); 
        // etPassword.setText("123456");

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "isi dulu email ama pass nya", Toast.LENGTH_SHORT).show();
            } else if (dbHelper.checkUser(email, password)) {
                // Ambil data user dari SQLite untuk ditaruh di Profile
                Cursor cursor = dbHelper.getUserData(email);
                if (cursor != null && cursor.moveToFirst()) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME));
                    String phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PHONE));
                    String role = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROLE));
                    
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("is_logged_in", true);
                    editor.putString("profile_name", name);
                    editor.putString("profile_email", email);
                    editor.putString("profile_phone", phone);
                    editor.putString("profile_role", role);
                    editor.apply();
                    cursor.close();
                }

                Toast.makeText(this, "Sip, login berhasil!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else if (email.equals("admin@gmail.com") && password.equals("123456")) {
                // Fallback dummy admin
                Toast.makeText(this, "Login Admin Berhasil!", Toast.LENGTH_SHORT).show();
                prefs.edit().putBoolean("is_logged_in", true).apply();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Email atau password salah!", Toast.LENGTH_SHORT).show();
            }
        });

        tvForgotPassword.setOnClickListener(v -> 
            Toast.makeText(this, "fitur reset pass blm ada nih", Toast.LENGTH_SHORT).show()
        );

        tvRegister.setOnClickListener(v -> 
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
    }
}