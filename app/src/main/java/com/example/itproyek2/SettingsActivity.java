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

    private SwitchCompat switchNotifLamp, switchNotifOvertime, switchNotifEnergy;
    private RadioGroup rgTheme;
    private RadioButton rbDark, rbLight;
    
    private TextView tvStatusDevice1, tvStatusDevice2, tvProfileName, tvProfileEmail;
    private ImageView ivDevice1, ivDevice2;
    private RelativeLayout layoutUserGuide, layoutContactSupport, layoutAbout, layoutLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // pasang tema dulu sebelum super.onCreate
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
        switchNotifEnergy = findViewById(R.id.switchNotifEnergy);
        rgTheme = findViewById(R.id.rgTheme);
        rbDark = findViewById(R.id.rbDark);
        rbLight = findViewById(R.id.rbLight);
        
        tvStatusDevice1 = findViewById(R.id.tvStatusDevice1);
        tvStatusDevice2 = findViewById(R.id.tvStatusDevice2);
        ivDevice1 = findViewById(R.id.ivDevice1);
        ivDevice2 = findViewById(R.id.ivDevice2);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        layoutUserGuide = findViewById(R.id.layoutUserGuide);
        layoutContactSupport = findViewById(R.id.layoutContactSupport);
        layoutAbout = findViewById(R.id.layoutAbout);
        layoutLogout = findViewById(R.id.layoutLogout);

        loadSettings();
        updateDeviceConnectionUi();

        layoutUserGuide.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, UserGuideActivity.class));
        });

        layoutContactSupport.setOnClickListener(v -> showContactSupportDialog());

        layoutAbout.setOnClickListener(v -> showAboutDialog());

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, EditProfileActivity.class));
        });



        // buat logout pake konfirmasi
        layoutLogout.setOnClickListener(v -> showLogoutConfirmation());

        switchNotifLamp.setOnCheckedChangeListener((v, isChecked) -> saveSetting("notif_lamp", isChecked));
        switchNotifOvertime.setOnCheckedChangeListener((v, isChecked) -> saveSetting("notif_overtime", isChecked));
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

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
        updateProfileUi();
    }

    private void updateProfileUi() {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        tvProfileName.setText(prefs.getString("profile_name", "Sofiani"));
        tvProfileEmail.setText(prefs.getString("profile_email", "sofiani@gmail.com"));
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("keluar akun")
                .setMessage("yakin nih mau keluar dari smartpress?")
                .setPositiveButton("ya, keluar", (dialog, which) -> {
                    // hapus status login nya
                    SharedPreferences.Editor editor = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE).edit();
                    editor.putBoolean("is_logged_in", false);
                    editor.apply();

                    Toast.makeText(this, "oke udah keluar", Toast.LENGTH_SHORT).show();

                    // balik ke login
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("batal", null)
                .show();
    }

    private void showContactSupportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hubungi Dukungan")
                .setMessage("Jika Anda mengalami kendala, silakan hubungi kami melalui:\n\n" +
                        "📧 Email: support@smartpress.id\n" +
                        "📞 No. Telp: 0812-3456-7890\n" +
                        "💬 WhatsApp: +62 812 3456 7890\n\n" +
                        "Tim kami akan membantu Anda secepat mungkin.")
                .setPositiveButton("Tutup", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.tentang_aplikasi))
                .setMessage(getString(R.string.about_desc) + "\n\n" +
                        getString(R.string.developer_info) + "\n" +
                        "Versi Aplikasi: " + getString(R.string.app_version))
                .setPositiveButton("Oke", null)
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

    // ambil settingan dr sharedprefs
    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        switchNotifLamp.setChecked(prefs.getBoolean("notif_lamp", true));
        switchNotifOvertime.setChecked(prefs.getBoolean("notif_overtime", true));
        switchNotifEnergy.setChecked(prefs.getBoolean("notif_energy", true));
        
        boolean isDark = prefs.getBoolean("is_dark_theme", true);
        if (isDark) rbDark.setChecked(true);
        else rbLight.setChecked(true);
    }

    // simpen settingan ke sharedprefs
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
            } else if (id == R.id.nav_report) {
                startActivity(new Intent(this, ReportActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return id == R.id.nav_settings;
        });
    }
}