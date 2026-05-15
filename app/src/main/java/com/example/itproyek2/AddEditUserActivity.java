package com.example.itproyek2;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class AddEditUserActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPhone, etPassword;
    private AutoCompleteTextView actvRole;
    private Button btnSave;
    private TextView tvTitle;
    private DatabaseHelper dbHelper;
    private int userId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_user);

        dbHelper = new DatabaseHelper(this);

        tvTitle = findViewById(R.id.tvTitle);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        actvRole = findViewById(R.id.actvRole);
        btnSave = findViewById(R.id.btnSave);

        // Setup Role Dropdown
        String[] roles = {"Admin", "User"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roles);
        actvRole.setAdapter(adapter);

        if (getIntent().hasExtra("USER_ID")) {
            userId = getIntent().getIntExtra("USER_ID", -1);
            tvTitle.setText("Edit User");
            etName.setText(getIntent().getStringExtra("USER_NAME"));
            etEmail.setText(getIntent().getStringExtra("USER_EMAIL"));
            etPassword.setText(getIntent().getStringExtra("USER_PASSWORD"));
            etPhone.setText(getIntent().getStringExtra("USER_PHONE"));
            actvRole.setText(getIntent().getStringExtra("USER_ROLE"), false);
        } else {
            tvTitle.setText("Tambah User");
        }

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String role = actvRole.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || role.isEmpty()) {
                Toast.makeText(this, "Nama, Email, Password, dan Role wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (phone.isEmpty()) {
                // Tampilkan konfirmasi jika No HP kosong
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Konfirmasi")
                        .setMessage("Nomor ponsel kosong, tetap simpan?")
                        .setPositiveButton("Ya", (dialog, which) -> saveUserData(name, email, password, phone, role))
                        .setNegativeButton("Tidak", null)
                        .show();
            } else {
                saveUserData(name, email, password, phone, role);
            }
        });
    }

    private void saveUserData(String name, String email, String password, String phone, String role) {
        boolean success;
        if (userId == -1) {
            success = dbHelper.addUserByAdmin(name, email, password, phone, role);
        } else {
            success = dbHelper.updateUser(userId, name, email, password, phone, role, "");
        }

        if (success) {
            Toast.makeText(this, "User berhasil disimpan", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Gagal menyimpan user", Toast.LENGTH_SHORT).show();
        }
    }
}
