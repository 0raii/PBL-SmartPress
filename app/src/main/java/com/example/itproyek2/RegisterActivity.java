package com.example.itproyek2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        MaterialCardView btnBack = findViewById(R.id.btnBack);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnBack.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            Toast.makeText(this, "Akun berhasil dibuat!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
