package com.example.itproyek2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Patterns;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButton;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone;
    private AutoCompleteTextView actvRole;
    private MaterialButton btnSave;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ... (tema logic tetap sama)
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("is_dark_theme", true);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etName = findViewById(R.id.etEditName);
        etEmail = findViewById(R.id.etEditEmail);
        etPhone = findViewById(R.id.etEditPhone);
        actvRole = findViewById(R.id.actvEditRole);
        btnSave = findViewById(R.id.btnSimpanProfil);
        btnBack = findViewById(R.id.btnBackEdit);

        // Setup Dropdown Role
        String[] roles = {"Admin", "Pengurus Mushola"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roles);
        actvRole.setAdapter(adapter);

        // Load data saat ini
        etName.setText(prefs.getString("profile_name", "Sofiani"));
        etEmail.setText(prefs.getString("profile_email", "sofiani@gmail.com"));
        etPhone.setText(prefs.getString("profile_phone", "08XXXXXXXXXX"));
        actvRole.setText(prefs.getString("profile_role", "Admin / Pengurus Mushola"), false);

        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            saveProfileData();
        });
    }

    private void saveProfileData() {
        String newName = etName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();
        String newRole = actvRole.getText().toString().trim();

        // Validasi Nama
        if (newName.isEmpty()) {
            etName.setError("Nama tidak boleh kosong");
            return;
        }

        // Validasi Email (Standar @ dan .com)
        if (newEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            etEmail.setError("Masukkan email yang valid (contoh: user@gmail.com)");
            return;
        }

        // Validasi No HP (Minimal 10 angka)
        if (newPhone.isEmpty() || newPhone.length() < 10) {
            etPhone.setError("Masukkan nomor ponsel yang valid");
            return;
        }

        // Validasi Role
        if (newRole.isEmpty()) {
            Toast.makeText(this, "Pilih role terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simpan ke SharedPreferences
        SharedPreferences.Editor editor = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE).edit();
        editor.putString("profile_name", newName);
        editor.putString("profile_email", newEmail);
        editor.putString("profile_phone", newPhone);
        editor.putString("profile_role", newRole);
        editor.apply();

        Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
        finish();
    }
}
