package com.example.itproyek2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvAdminName;
    private MaterialCardView cardManageUsers, cardMonitoringIot, cardLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvAdminName = findViewById(R.id.tvAdminName);
        cardManageUsers = findViewById(R.id.cardManageUsers);
        cardMonitoringIot = findViewById(R.id.cardMonitoringIot);
        cardLogout = findViewById(R.id.cardLogout);

        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        String name = prefs.getString("profile_name", "Administrator");
        tvAdminName.setText("Halo, " + name);

        cardManageUsers.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageUsersActivity.class));
        });

        cardMonitoringIot.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminMonitoringActivity.class));
        });

        cardLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
