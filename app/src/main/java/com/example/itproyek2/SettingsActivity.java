package com.example.itproyek2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchNotifLamp, switchNotifOvertime, switchNotifEnergy;
    private RadioGroup rgTheme;
    private RadioButton rbDark, rbLight;
    private MaterialButtonToggleGroup toggleThreshold;
    private RelativeLayout layoutTestNotif;
    
    private TextView tvStatusDevice1, tvProfileName, tvProfileEmail;
    private ImageView ivDevice1, ivProfileMain;
    private RelativeLayout layoutUserGuide, layoutContactSupport, layoutAbout, layoutLogout;
    private MaterialButton btnSetPassword;
    private DatabaseReference dbRef;
    
    private long lastTickTime = 0;
    private boolean isConnected = false;
    private final android.os.Handler offlineCheckHandler = new android.os.Handler();

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Refresh UI when coming back from EditProfileActivity
                updateProfileUi();
            }
    );

    private final ActivityResultLauncher<Intent> setPasswordLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                updateProfileUi();
            }
    );

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
        setContentView(R.layout.activity_settings);

        dbRef = FirebaseDatabase.getInstance("https://smartpress-ea81d-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("monitoring/perangkat_utama");
        initFirebaseListeners();

        switchNotifLamp = findViewById(R.id.switchNotifLamp);
        switchNotifOvertime = findViewById(R.id.switchNotifOvertime);
        switchNotifEnergy = findViewById(R.id.switchNotifEnergy);
        rgTheme = findViewById(R.id.rgTheme);
        rbDark = findViewById(R.id.rbDark);
        rbLight = findViewById(R.id.rbLight);
        toggleThreshold = findViewById(R.id.toggleThreshold);
        
        tvStatusDevice1 = findViewById(R.id.tvStatusDevice1);
        ivDevice1 = findViewById(R.id.ivDevice1);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        ivProfileMain = findViewById(R.id.ivProfileMain);
        layoutUserGuide = findViewById(R.id.layoutUserGuide);
        layoutContactSupport = findViewById(R.id.layoutContactSupport);
        layoutAbout = findViewById(R.id.layoutAbout);
        layoutLogout = findViewById(R.id.layoutLogout);
        layoutTestNotif = findViewById(R.id.layoutTestNotif);
        btnSetPassword = findViewById(R.id.btnSetPassword);

        loadSettings();

        layoutTestNotif.setOnClickListener(v -> showTestNotification());

        layoutUserGuide.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, UserGuideActivity.class));
        });

        layoutContactSupport.setOnClickListener(v -> showContactSupportDialog());

        layoutAbout.setOnClickListener(v -> showAboutDialog());

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            editProfileLauncher.launch(new Intent(SettingsActivity.this, EditProfileActivity.class));
        });

        btnSetPassword.setOnClickListener(v -> {
            setPasswordLauncher.launch(new Intent(SettingsActivity.this, SetPasswordActivity.class));
        });



        // buat logout pake konfirmasi
        layoutLogout.setOnClickListener(v -> showLogoutConfirmation());

        switchNotifLamp.setOnCheckedChangeListener((v, isChecked) -> saveSetting("notif_lamp", isChecked));
        switchNotifOvertime.setOnCheckedChangeListener((v, isChecked) -> saveSetting("notif_overtime", isChecked));
        switchNotifEnergy.setOnCheckedChangeListener((v, isChecked) -> saveSetting("notif_energy", isChecked));

        toggleThreshold.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                int threshold = 2500; // Level 2 (Standar)
                if (checkedId == R.id.btnLow) threshold = 1000; // Level 1 (Paling Sensitif)
                else if (checkedId == R.id.btnHigh) threshold = 4000; // Level 3 (Kurang Sensitif)
                
                getSharedPreferences("SmartLampPrefs", MODE_PRIVATE).edit()
                        .putInt("ldr_threshold", threshold).apply();
                
                // Kirim ke firebase juga agar ESP32 tahu
                dbRef.child("ldr_threshold").setValue(threshold);
            }
        });

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
        startOfflineCheckLoop();
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
        
        boolean hasPassword = prefs.getBoolean("has_password", true);
        if (!hasPassword) {
            btnSetPassword.setVisibility(android.view.View.VISIBLE);
        } else {
            btnSetPassword.setVisibility(android.view.View.GONE);
        }

        String savedImageUri = prefs.getString("profile_image_uri", null);
        if (savedImageUri != null && ivProfileMain != null) {
            // Force refresh ImageView by clearing current image and setting to null first
            ivProfileMain.setImageURI(null);
            ivProfileMain.setImageURI(Uri.parse(savedImageUri));
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("keluar akun")
                .setMessage("yakin nih mau keluar dari smartpress?")
                .setPositiveButton("ya, keluar", (dialog, which) -> {
                    // hapus session tapi simpan tema
                    SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
                    boolean currentTheme = prefs.getBoolean("is_dark_theme", true);
                    
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();
                    editor.putBoolean("is_dark_theme", currentTheme);
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

    private void showTestNotification() {
        String channelId = "SMART_LAMP_NOTIF";
        
        // Buat channel jika belum ada (Penting untuk Android 8+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId, "Smart Lamp Notifications", android.app.NotificationManager.IMPORTANCE_HIGH);
            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_logo_smartpress)
                .setContentTitle("Tes Notifikasi SmartPress")
                .setContentText("Ini adalah contoh notifikasi bar atas yang muncul saat status lampu berubah.")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL);

        android.app.NotificationManager manager = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(4, builder.build()); // ID 4 untuk tes
            Toast.makeText(this, "Notifikasi terkirim! Cek bagian atas HP Anda.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showContactSupportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hubungi Dukungan")
                .setMessage("Jika Anda mengalami kendala, silakan hubungi kami melalui:\n\n" +
                        "📧 Email: muhammad.raihan1@mhs.politala.ac.id\n" +
                        "📞 No. Telp: 081520427689\n" +
                        "💬 WhatsApp: +62 815 2042 7689\n\n" +
                        "Tim kami akan membantu Anda secepat mungkin.")
                .setPositiveButton("Tutup", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.tentang_aplikasi))
                .setMessage(getString(R.string.about_desc) + "\n\n" +
                        getString(R.string.developer_info))
                .setPositiveButton("Oke", null)
                .show();
    }

    private void initFirebaseListeners() {
        // Pantau status koneksi Lampu 1 melalui heartbeat tick
        dbRef.child("is_connected_tick").addValueEventListener(new ValueEventListener() {
            private Object lastValue = null;
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Object newValue = snapshot.getValue();
                    if (lastValue != null && !newValue.equals(lastValue)) {
                        isConnected = true;
                        lastTickTime = System.currentTimeMillis();
                        updateDeviceStatusUi();
                    }
                    lastValue = newValue;
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void startOfflineCheckLoop() {
        offlineCheckHandler.post(new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastTickTime > 20000) {
                    if (isConnected) {
                        isConnected = false;
                        updateDeviceStatusUi();
                    }
                }
                offlineCheckHandler.postDelayed(this, 5000);
            }
        });
    }

    private void updateDeviceStatusUi() {
        if (isConnected) {
            tvStatusDevice1.setText("● Terhubung");
            tvStatusDevice1.setTextColor(Color.parseColor("#4CAF50")); // Hijau
            ivDevice1.setAlpha(1.0f);
            ivDevice1.setColorFilter(null);
        } else {
            tvStatusDevice1.setText("○ Terputus");
            tvStatusDevice1.setTextColor(Color.parseColor("#F44336")); // Merah
            ivDevice1.setAlpha(0.5f);
            ivDevice1.setColorFilter(Color.GRAY, android.graphics.PorterDuff.Mode.SRC_IN);
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

        // Default awal: Level 1 (1000) - Paling Sensitif
        int threshold = prefs.getInt("ldr_threshold", 1000);
        if (threshold <= 1500) toggleThreshold.check(R.id.btnLow);
        else if (threshold >= 3500) toggleThreshold.check(R.id.btnHigh);
        else toggleThreshold.check(R.id.btnMid);
    }

    private void saveSetting(String key, boolean value) {
        getSharedPreferences("SmartLampPrefs", MODE_PRIVATE).edit().putBoolean(key, value).apply();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavSettings);
        bottomNav.setSelectedItemId(R.id.nav_settings);
        
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        String role = prefs.getString("profile_role", "User");
        
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                if ("Admin".equalsIgnoreCase(role)) {
                    startActivity(new Intent(this, AdminDashboardActivity.class));
                } else {
                    startActivity(new Intent(this, MainActivity.class));
                }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        offlineCheckHandler.removeCallbacksAndMessages(null);
    }
}