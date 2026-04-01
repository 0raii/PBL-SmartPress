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
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchNotifLamp, switchNotifOvertime, switchNotifOverheat, switchNotifEnergy;
    private RadioGroup rgTheme;
    private RadioButton rbDark, rbLight;
    
    private TextView tvStatusDevice1, tvStatusDevice2;
    private ImageView ivDevice1, ivDevice2;
    private RelativeLayout layoutUserGuide, layoutLogout;

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
        setContentView(R.layout.activity_settings);

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
        layoutLogout = findViewById(R.id.layoutLogout);

        loadSettings();
        updateDeviceConnectionUi();

        layoutUserGuide.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, UserGuideActivity.class));
        });

        // Logika Logout dengan Konfirmasi
        layoutLogout.setOnClickListener(v -> showLogoutConfirmation());

        switchNotifLamp.setOnCheckedChangeListener((v, isChecked) -> saveSetting("notif_lamp", isChecked));
        switchNotifOvertime.setOnCheckedChangeListener((v, isChecked) -> saveSetting("notif_overtime", isChecked));
        switchNotifOverheat.setOnCheckedChangeListener((v, isChecked) -> saveSetting("notif_overheat", isChecked));
        switchNotifEnergy.setOnCheckedChangeListener((v, isChecked) -> saveSetting("notif_energy", isChecked));

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            boolean selectedIsDark = (checkedId == R.id.rbDark);
            saveSetting("is_dark_theme", selectedIsDark);
            
            if (selectedIsDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        setupBottomNav();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Keluar Akun")
                .setMessage("Apakah Anda yakin ingin keluar dari aplikasi SmartPress?")
                .setPositiveButton("Ya, Keluar", (dialog, which) -> {
                    // Hapus status login
                    SharedPreferences.Editor editor = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE).edit();
                    editor.putBoolean("is_logged_in", false);
                    editor.apply();

                    Toast.makeText(this, "Berhasil Keluar", Toast.LENGTH_SHORT).show();

                    // Pindah ke halaman Login
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Batal", null)
                .show();
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
        
        boolean isDark = prefs.getBoolean("is_dark_theme", true);
        if (isDark) rbDark.setChecked(true);
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
