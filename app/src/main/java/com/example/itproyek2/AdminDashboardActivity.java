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
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("is_dark_theme", true);
        if (isDark) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvAdminName = findViewById(R.id.tvAdminName);
        cardManageUsers = findViewById(R.id.cardManageUsers);
        cardMonitoringIot = findViewById(R.id.cardMonitoringIot);
        cardLogout = findViewById(R.id.cardLogout);

        String name = prefs.getString("profile_name", "Administrator");
        tvAdminName.setText("Halo, " + name);

        cardManageUsers.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageUsersActivity.class));
        });

        cardMonitoringIot.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminMonitoringActivity.class));
        });

        cardLogout.setOnClickListener(v -> {
            boolean currentTheme = prefs.getBoolean("is_dark_theme", true);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.putBoolean("is_dark_theme", currentTheme);
            editor.apply();

            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
