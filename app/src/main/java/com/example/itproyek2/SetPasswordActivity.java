package com.example.itproyek2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.textfield.TextInputEditText;

public class SetPasswordActivity extends AppCompatActivity {

    private TextInputEditText etNewPassword, etConfirmPassword;
    private Button btnSave;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("is_dark_theme", true);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_password);

        dbHelper = new DatabaseHelper(this);

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSave = findViewById(R.id.btnSavePassword);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (newPass.isEmpty()) {
                etNewPassword.setError("Password tidak boleh kosong");
                return;
            }

            if (newPass.length() < 6) {
                etNewPassword.setError("Password minimal 6 karakter");
                return;
            }

            if (!newPass.equals(confirmPass)) {
                etConfirmPassword.setError("Konfirmasi password tidak cocok");
                return;
            }

            String email = prefs.getString("profile_email", "");
            if (dbHelper.setManualPassword(email, newPass)) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("has_password", true);
                editor.apply();

                Toast.makeText(this, "Password berhasil disimpan!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Gagal menyimpan password", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
