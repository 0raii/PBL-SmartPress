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

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || role.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success;
            if (userId == -1) {
                // Add new user
                success = dbHelper.addUserByAdmin(name, email, password, phone, role);
            } else {
                // Update existing user (photo passed as empty string as requested to remove it)
                success = dbHelper.updateUser(userId, name, email, password, phone, role, "");
            }

            if (success) {
                Toast.makeText(this, "User saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error saving user", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
