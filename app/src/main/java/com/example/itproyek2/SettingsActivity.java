package com.example.itproyek2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchNotifLamp, switchNotifOvertime, switchNotifOverheat, switchNotifEnergy;
    private RadioGroup rgTheme;
    private RadioButton rbDark, rbLight;
    private ImageView btnBack;
    
    // Help & Device Status Views
    private TextView tvStatusDevice1, tvStatusDevice2;
    private ImageView ivDevice1, ivDevice2;
    private RelativeLayout layoutUserGuide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        btnBack = findViewById(R.id.btnBack);
        switchNotifLamp = findViewById(R.id.switchNotifLamp);
        switchNotifOvertime = findViewById(R.id.switchNotifOvertime);
        switchNotifOverheat = findViewById(R.id.switchNotifOverheat);
        switchNotifEnergy = findViewById(R.id.switchNotifEnergy);
        rgTheme = findViewById(R.id.rgTheme);
        rbDark = findViewById(R.id.rbDark);
        rbLight = findViewById(R.id.rbLight);
        
        tvStatusDevice1 = findViewById(R.id.tvStatusDevice1);
        tvStatusDevice2 = findViewById(R.id.tvStatusDevice2);
        ivDevice1 = findViewById(R.id.ivDevice1);
        ivDevice2 = findViewById(R.id.ivDevice2);
        layoutUserGuide = findViewById(R.id.layoutUserGuide);

        loadSettings();
        updateDeviceConnectionUi();

        btnBack.setOnClickListener(v -> finish());

        // Navigasi ke Panduan Pengguna
        layoutUserGuide.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, UserGuideActivity.class));
        });

        switchNotifLamp.setOnCheckedChangeListener((v, isChecked) -> saveSetting("notif_lamp", isChecked));
        switchNotifOvertime.setOnCheckedChangeListener((v, isChecked) -> saveSetting("notif_overtime", isChecked));
        switchNotifOverheat.setOnCheckedChangeListener((v, isChecked) -> saveSetting("notif_overheat", isChecked));
        switchNotifEnergy.setOnCheckedChangeListener((v, isChecked) -> saveSetting("notif_energy", isChecked));

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            saveSetting("is_dark_theme", (checkedId == R.id.rbDark));
        });

        setupBottomNav();
    }

    private void updateDeviceConnectionUi() {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        boolean isConnected = prefs.getBoolean("is_connected", true);

        if (isConnected) {
            tvStatusDevice1.setText("● Terhubung");
            tvStatusDevice1.setTextColor(Color.parseColor("#4CAF50"));
            ivDevice1.setAlpha(1.0f);
            
            tvStatusDevice2.setText("● Terhubung");
            tvStatusDevice2.setTextColor(Color.parseColor("#4CAF50"));
            ivDevice2.setAlpha(1.0f);
        } else {
            tvStatusDevice1.setText("○ Terputus");
            tvStatusDevice1.setTextColor(Color.parseColor("#F44336"));
            ivDevice1.setAlpha(0.3f);
            
            tvStatusDevice2.setText("○ Terputus");
            tvStatusDevice2.setTextColor(Color.parseColor("#F44336"));
            ivDevice2.setAlpha(0.3f);
        }
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        switchNotifLamp.setChecked(prefs.getBoolean("notif_lamp", true));
        switchNotifOvertime.setChecked(prefs.getBoolean("notif_overtime", true));
        switchNotifOverheat.setChecked(prefs.getBoolean("notif_overheat", true));
        switchNotifEnergy.setChecked(prefs.getBoolean("notif_energy", true));

        if (prefs.getBoolean("is_dark_theme", true)) rbDark.setChecked(true);
        else rbLight.setChecked(true);
    }

    private void saveSetting(String key, boolean value) {
        getSharedPreferences("SmartLampPrefs", MODE_PRIVATE).edit().putBoolean(key, value).apply();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavSettings);
        bottomNav.setSelectedItemId(R.id.nav_settings);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return id == R.id.nav_settings;
        });
    }
}
