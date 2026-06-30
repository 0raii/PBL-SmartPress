package com.example.itproyek2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatusLampu, tvKondisiCahaya, tvLogAktivitas;
    private TextView tvDaya, tvCostSummary, tvKwhSummary;
    private TextView tvEspStatus, tvWifiStatus;
    private ImageView ivLampIllustration, ivKondisiIcon;
    private View bulbGlow;
    private LinearLayout layoutStatusCahaya;
    private MaterialButtonToggleGroup toggleGroupLamp, toggleGroupMode;
    private Button btnDetail;
    private MaterialSwitch switchTimer;
    private Button btnStartTime, btnEndTime;

    private DatabaseReference dbRef;
    private DatabaseHelper dbHelper;
    private boolean isLampOn, isAutoMode, isConnected = false; 
    private boolean isTimerEnabled = false;
    private long lastTickTime = 0;
    private static boolean hasShownOfflineToastOnce = false;
    private final Handler offlineCheckHandler = new Handler();

    private boolean notifLamp, notifOvertime, notifEnergy;
    private int currentLdrValue = 0;
    private int currentLumen = 0;
    private int ldrThreshold = 2500;
    private String timerOnStr = "18:00";
    private String timerOffStr = "06:00";
    private double totalKwh = 0;
    private double currentVoltage = 0, currentArus = 0, currentWatt = 0;
    private long lampOnStartTime = 0;

    private Handler realtimeHandler = new Handler();
    private SecureRandom random = new SecureRandom();
    private DecimalFormat df = new DecimalFormat("0.00");
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private static final String CHANNEL_ID = "SMART_LAMP_NOTIF";
    private static final int NOTIF_ID_LAMP = 1;
    private static final int NOTIF_ID_PROTECTION = 2;
    private static final int NOTIF_ID_SCHEDULE = 3;
    private static final int NOTIF_ID_TEST = 4;
    private static final int NOTIF_ID_ENERGY = 5;

    private boolean isAppInForeground = false;
    private boolean isProtectionAlertSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
        boolean isDark = prefs.getBoolean("is_dark_theme", true);
        if (isDark) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init views
        tvStatusLampu = findViewById(R.id.tvStatusLampu);
        tvKondisiCahaya = findViewById(R.id.tvKondisiCahaya);
        tvLogAktivitas = findViewById(R.id.tvLogAktivitas);
        tvDaya = findViewById(R.id.tvDaya);
        tvCostSummary = findViewById(R.id.tvCostSummary);
        tvKwhSummary = findViewById(R.id.tvKwhSummary);
        tvEspStatus = findViewById(R.id.tvEspStatus);
        tvWifiStatus = findViewById(R.id.tvWifiStatus);
        ivLampIllustration = findViewById(R.id.ivLampIllustration);
        ivKondisiIcon = findViewById(R.id.ivKondisiIcon);
        bulbGlow = findViewById(R.id.bulbGlow);
        layoutStatusCahaya = findViewById(R.id.layoutStatusCahaya);
        toggleGroupLamp = findViewById(R.id.toggleGroupLamp);
        toggleGroupMode = findViewById(R.id.toggleGroupMode);
        btnDetail = findViewById(R.id.btnDetail);
        switchTimer = findViewById(R.id.switchTimer);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndTime = findViewById(R.id.btnEndTime);

        dbHelper = new DatabaseHelper(this);

        // Inisialisasi Firebase - SEMUA USER konek ke perangkat yang sama
        // Agar mudah untuk pengguna (Orang Tua)
        String targetPath = "monitoring/perangkat_utama";
        
        dbRef = FirebaseDatabase.getInstance("https://smartpress-ea81d-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference(targetPath);
        
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        initFirebaseListeners();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown Error";
                        showToast("Firebase Error: " + error);
                    }
                });

        createNotificationChannel();
        loadAppState();
        applyUiState();

        // ganti status lampu on/off
        toggleGroupLamp.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (isAutoMode) {
                    showToast("Matiin mode OTOMATIS dulu kalo mau manual");
                    toggleGroupLamp.post(() -> toggleGroupLamp.check(isLampOn ? R.id.btnOn : R.id.btnOff));
                } else {
                    if (isTimerEnabled) {
                        showToast("Penjadwalan Aktif: Kontrol manual akan menunda jadwal sementara");
                    }
                    updateLampState(checkedId == R.id.btnOn, "Kontrol Manual");
                    if (!isConnected) {
                        showToast("Perintah dikirim (Perangkat sedang offline)");
                    }
                }
            }
        });

        // ganti mode manual/otomatis
        toggleGroupMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                boolean newMode = (checkedId == R.id.btnAuto);
                dbRef.child("auto_mode").setValue(newMode);
                addLog("Mode ganti ke " + (newMode ? "OTOMATIS" : "MANUAL"));
                if (!isConnected) {
                    showToast("Pengaturan mode dikirim (Perangkat sedang offline)");
                }
            }
        });

        btnDetail.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DetailActivity.class));
        });

        switchTimer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dbRef.child("timer_enabled").setValue(isChecked);
            addLog("Jadwal lampu " + (isChecked ? "DIAKTIFKAN" : "DIMATIKAN"));
            
            if (isChecked && isAutoMode) {
                showToast("Mode Otomatis ditangguhkan selama Penjadwalan aktif");
            }
            if (!isConnected) {
                showToast("Pengaturan jadwal dikirim (Perangkat sedang offline)");
            }
        });

        btnStartTime.setOnClickListener(v -> showTimePicker(true));
        btnEndTime.setOnClickListener(v -> showTimePicker(false));

        // navigasi bawah
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            if (id == R.id.nav_report) {
                startActivity(new Intent(this, ReportActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        startRealtimeSimulation();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Smart Lamp Notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifikasi untuk status lampu dan penggunaan energi");
            channel.enableLights(true);
            channel.setLightColor(Color.YELLOW);
            channel.enableVibration(true);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void sendSystemNotification(String title, String message) {
        sendSystemNotification(title, message, random.nextInt(1000));
    }

    private void sendSystemNotification(String title, String message, int id) {
        // Jangan kirim notifikasi jika user sedang membuka aplikasi (agar tidak mengganggu)
        if (isAppInForeground) return;

        // Cek permission untuk Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo_smartpress)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(id, builder.build());
    }

    private void syncLampToggle() {
        toggleGroupLamp.post(() -> toggleGroupLamp.check(isLampOn ? R.id.btnOn : R.id.btnOff));
    }

    private void loadAppState() {
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
        isLampOn = prefs.getBoolean("lamp_status", false);
        isAutoMode = prefs.getBoolean("auto_mode", false);
        totalKwh = Double.parseDouble(prefs.getString("total_kwh", "0.0").replace(",", "."));
        
        notifLamp = prefs.getBoolean("notif_lamp", true);
        notifOvertime = prefs.getBoolean("notif_overtime", true);
        notifEnergy = prefs.getBoolean("notif_energy", true);
    }

    private void saveAppState() {
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("lamp_status", isLampOn);
        editor.putBoolean("auto_mode", isAutoMode);
        editor.putString("total_kwh", String.format(Locale.US, "%.4f", totalKwh));
        editor.apply();
    }

    private void applyUiState() {
        tvStatusLampu.setText(isLampOn ? "ON" : "OFF");
        tvStatusLampu.setTextColor(ContextCompat.getColor(this, isLampOn ? R.color.accent_yellow : R.color.text_sub));
        ivLampIllustration.setImageResource(isLampOn ? R.drawable.ic_lamp_on : R.drawable.ic_power);
        ivLampIllustration.setColorFilter(ContextCompat.getColor(this, isLampOn ? R.color.accent_yellow : R.color.text_sub));
        bulbGlow.setVisibility(isLampOn ? View.VISIBLE : View.GONE);
        
        toggleGroupLamp.post(() -> toggleGroupLamp.check(isLampOn ? R.id.btnOn : R.id.btnOff));
        toggleGroupMode.post(() -> toggleGroupMode.check(isAutoMode ? R.id.btnAuto : R.id.btnManual));

        tvEspStatus.setText(isConnected ? "ONLINE" : "OFFLINE");
        tvEspStatus.setTextColor(Color.parseColor(isConnected ? "#4CAF50" : "#F44336"));
        tvWifiStatus.setText(isConnected ? "CONNECTED" : "DISCONNECTED");
        tvWifiStatus.setTextColor(Color.parseColor(isConnected ? "#4CAF50" : "#F44336"));

        if (!isConnected) {
            tvDaya.setText("0.00 Watt");
            tvKwhSummary.setText("Total Pemakaian Hari Ini: 0.00 kWh");
            tvCostSummary.setText(currencyFormat.format(0));
            tvStatusLampu.setText("OFFLINE");
            tvStatusLampu.setTextColor(Color.parseColor("#F44336"));
        } else {
            tvDaya.setText(df.format(currentWatt) + " Watt");
            tvKwhSummary.setText("Total Pemakaian Hari Ini: " + df.format(totalKwh) + " kWh");
            tvCostSummary.setText(currencyFormat.format(totalKwh * 1500.0)); // Gunakan default 1500
        }
        
        updateCahayaUi(currentLdrValue);

        switchTimer.setChecked(isTimerEnabled);
        btnStartTime.setText(timerOnStr);
        btnEndTime.setText(timerOffStr);

        // Kontrol akses tombol jam berdasarkan status switch
        btnStartTime.setEnabled(isTimerEnabled);
        btnEndTime.setEnabled(isTimerEnabled);
        btnStartTime.setAlpha(isTimerEnabled ? 1.0f : 0.5f);
        btnEndTime.setAlpha(isTimerEnabled ? 1.0f : 0.5f);
    }

    private void initFirebaseListeners() {
        // Listener untuk Status Lampu
        dbRef.child("lamp_status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    boolean status = snapshot.getValue(Boolean.class);
                    if (isLampOn != status) {
                        isLampOn = status;
                        if (isLampOn) lampOnStartTime = System.currentTimeMillis();
                        applyUiState();
                        
                        String source = "Sistem";
                        if (isAutoMode) source = "Otomatis";
                        else if (isTimerEnabled) source = "Jadwal";
                        else source = "Manual/Alat";

                        addLog("Lampu " + (isLampOn ? "MENYALA" : "MATI") + " (" + source + ")");

                        // NOTIFIKASI PENTING: Perubahan status via Otomatis atau Jadwal
                        if (notifLamp) {
                            if (isAutoMode) {
                                sendSystemNotification("Mode Otomatis Beraksi", "Lampu " + (isLampOn ? "Dinyalakan" : "Dimatikan") + " karena sensor cahaya.", NOTIF_ID_LAMP);
                            } else if (isTimerEnabled) {
                                sendSystemNotification("Jadwal Berjalan", "Lampu " + (isLampOn ? "Dinyalakan" : "Dimatikan") + " sesuai jadwal yang Anda atur.", NOTIF_ID_SCHEDULE);
                            }
                        }
                        
                        // Log ke SQLite untuk Report jika ini otomatis/jadwal
                        if (isConnected && (isAutoMode || isTimerEnabled)) {
                            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(MainActivity.this);
                            int userId = prefs.getInt("profile_id", -1);
                            if (userId != -1) {
                                dbHelper.addAutoLog(userId, isLampOn ? "HIDUP" : "MATI", currentLumen, currentWatt);
                                dbHelper.updateDailyStats(userId, new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()), totalKwh, 1);
                            }
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Listener untuk Mode Otomatis
        dbRef.child("auto_mode").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    isAutoMode = snapshot.getValue(Boolean.class);
                    applyUiState();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Listener untuk Sensor Cahaya
        dbRef.child("sensor_lux").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentLdrValue = snapshot.getValue(Integer.class);
                    updateCahayaUi(currentLdrValue);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Listener untuk PZEM (Voltage, Current, Power, Energy)
        dbRef.child("sensor_voltage").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) { if(s.exists()) currentVoltage = s.getValue(Double.class); applyUiState(); }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
        dbRef.child("sensor_current").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) { if(s.exists()) currentArus = s.getValue(Double.class); applyUiState(); }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
        dbRef.child("sensor_power").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) { if(s.exists()) currentWatt = s.getValue(Double.class); applyUiState(); }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
        dbRef.child("sensor_energy").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) { if(s.exists()) totalKwh = s.getValue(Double.class); applyUiState(); }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        // Listener untuk Threshold LDR
        dbRef.child("ldr_threshold").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ldrThreshold = snapshot.getValue(Integer.class);
                    updateCahayaUi(currentLdrValue);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Listener untuk Timer
        dbRef.child("timer_off").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) { if(s.exists()) { timerOffStr = s.getValue(String.class); applyUiState(); } }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        // Listener untuk Timer Enabled
        dbRef.child("timer_enabled").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    isTimerEnabled = snapshot.getValue(Boolean.class);
                    applyUiState();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Listener untuk Timer On
        dbRef.child("timer_on").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) { if(s.exists()) { timerOnStr = s.getValue(String.class); applyUiState(); } }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        // Listener untuk Status Koneksi ESP32 (Heartbeat)
        dbRef.child("is_connected_tick").addValueEventListener(new ValueEventListener() {
            private Object lastValue = null;
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Object newValue = snapshot.getValue();
                    // Hanya anggap online jika nilai tick berubah (artinya ada aktifitas baru)
                    if (lastValue != null && !newValue.equals(lastValue)) {
                        isConnected = true;
                        lastTickTime = System.currentTimeMillis();
                        applyUiState();
                    }
                    lastValue = newValue;
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Loop pengecekan offline (Jika 20 detik tidak ada update dari ESP32 = Offline)
        offlineCheckHandler.post(new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastTickTime > 20000) {
                    if (isConnected) {
                        isConnected = false;
                        applyUiState();
                        
                        if (!hasShownOfflineToastOnce) {
                            showToast("Perangkat Terputus (Offline)");
                            lastTickTime = 0; // Reset lastTickTime to avoid immediate online status on re-entry
                            hasShownOfflineToastOnce = true;
                        }
                    }
                } else {
                    // Jika kembali Online, reset flag toast agar bisa muncul lagi nanti jika putus lagi
                    if (!isConnected && lastTickTime != 0) {
                        isConnected = true;
                        hasShownOfflineToastOnce = false;
                        applyUiState();
                    }
                }
                offlineCheckHandler.postDelayed(this, 5000);
            }
        });
    }

    private void updateLampState(boolean turnOn, String source) {
        if (isLampOn == turnOn) return;
        
        // Kirim perintah ke Firebase
        dbRef.child("lamp_status").setValue(turnOn);
        
        // Data lokal akan diupdate otomatis oleh Listener di atas
    }

    private void addLog(String message) {
        if (!isConnected) return; // Jangan simpan aktifitas jika perangkat offline

        String currentTime = timeFormat.format(new Date());
        tvLogAktivitas.setText("[" + currentTime + "] " + message);
        saveLogToHistory(message);
    }

    private void saveLogToHistory(String message) {
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
        String history = prefs.getString("history_data", "");
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        
        int iconType = 0;
        String msg = message.toUpperCase();
        if (msg.contains("HIDUP")) iconType = 1;
        else if (msg.contains("MATI")) iconType = 2;
        else if (msg.contains("OTOMATIS")) iconType = 3;
        else if (msg.contains("MANUAL")) iconType = 4;
        else if (msg.contains("PROTEKSI")) iconType = 5;

        String newEntry = message + "|" + "Hari Ini " + currentTime + "|" + iconType + ";";
        prefs.edit().putString("history_data", history + newEntry).apply();
    }

    private void startRealtimeSimulation() {
        realtimeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    calculateEnergyLocally();
                    checkAlerts();
                    // checkTimers() dihapus karena sudah ditangani ESP32
                }
                realtimeHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void calculateEnergyLocally() {
        // Karena sensor PZEM belum ada/belum kirim data, nilai default adalah 0 dari inisialisasi variabel
        // Jika sudah ada data dari Firebase, listener akan mengupdate variabel secara otomatis.

        if (isConnected) {
            tvDaya.setText(df.format(currentWatt) + " Watt");
            tvKwhSummary.setText("Total Pemakaian Hari Ini: " + df.format(totalKwh) + " kWh");
            
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
            double tariff = prefs.getFloat("electricity_tariff", 1500.0f);
            
            // Menampilkan estimasi biaya berdasarkan totalKwh dari PZEM
            tvCostSummary.setText(currencyFormat.format(totalKwh * tariff));

            // NOTIFIKASI ENERGI (Contoh: setiap 1 kWh)
            if (notifEnergy && totalKwh > 0 && Math.floor(totalKwh) > Math.floor(totalKwh - (currentWatt / 3600000.0))) {
                 sendSystemNotification("Laporan Penggunaan", "Pemakaian listrik telah mencapai " + Math.floor(totalKwh) + " kWh.", NOTIF_ID_ENERGY);
            }

            saveAppState();
        }
    }

    private void updateCahayaUi(int ldrValue) {
        // Konversi ADC (0-4095) ke Persen (0-100%)
        // 4095 (Gelap) -> 0%
        // 0 (Terang) -> 100%
        int percent = (int) (((4095.0 - ldrValue) / 4095.0) * 100);
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;

        boolean isDark = ldrValue > ldrThreshold;
        
        tvKondisiCahaya.setText(percent + "%");
        tvKondisiCahaya.setTextColor(isDark ? Color.WHITE : Color.BLACK);
        layoutStatusCahaya.setBackgroundResource(isDark ? R.drawable.status_bg_dark : R.drawable.status_bright_bg);
        ivKondisiIcon.setImageResource(isDark ? R.drawable.ic_star_on : R.drawable.ic_history);
        ivKondisiIcon.setColorFilter(isDark ? Color.WHITE : Color.BLACK);

        // UI Feedback untuk status kontrol
        if (isTimerEnabled) {
            tvStatusLampu.setText(isLampOn ? "ON (Jadwal)" : "OFF (Jadwal)");
        } else if (isAutoMode) {
            tvStatusLampu.setText(isLampOn ? "ON (Otomatis)" : "OFF (Otomatis)");
        } else {
            tvStatusLampu.setText(isLampOn ? "ON" : "OFF");
        }

        // Logika kontrol otomatis sekarang sepenuhnya ditangani oleh ESP32.
        // Aplikasi hanya bertugas memantau status dan menampilkan data.
    }

    private void showTimePicker(boolean isStartTime) {
        String currentTime = isStartTime ? timerOnStr : timerOffStr;
        String[] parts = currentTime.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(isStartTime ? "Pilih Jam Mulai" : "Pilih Jam Selesai")
                .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            int selectedHour = picker.getHour();
            int selectedMinute = picker.getMinute();
            String selectedTime = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute);
            
            // Logika Validasi Waktu
            java.util.Calendar now = java.util.Calendar.getInstance();
            int currentHour = now.get(java.util.Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(java.util.Calendar.MINUTE);

            if (isStartTime) {
                // 1. Cek apakah jam mulai sudah lewat dari jam sekarang
                if (selectedHour < currentHour || (selectedHour == currentHour && selectedMinute <= currentMinute)) {
                    showToast("Jam mulai tidak boleh kurang dari jam sekarang (" + String.format(Locale.US, "%02d:%02d", currentHour, currentMinute) + ")");
                    return;
                }
                dbRef.child("timer_on").setValue(selectedTime);
            } else {
                // 2. Cek apakah jam selesai lebih kecil dari jam mulai
                String[] startParts = timerOnStr.split(":");
                int startHour = Integer.parseInt(startParts[0]);
                int startMin = Integer.parseInt(startParts[1]);
                
                if (selectedHour < startHour || (selectedHour == startHour && selectedMinute <= startMin)) {
                    showToast("Jam selesai harus lebih besar dari jam mulai (" + timerOnStr + ")");
                    return;
                }
                dbRef.child("timer_off").setValue(selectedTime);
            }
            showToast("Jadwal " + (isStartTime ? "mulai" : "selesai") + " diatur ke " + selectedTime);
        });

        picker.show(getSupportFragmentManager(), "MATERIAL_TIME_PICKER");
    }

    private void checkAlerts() {
        // 1. PROTEKSI LISTRIK (SANGAT PENTING)
        // Jika tegangan > 245V atau arus > 1A (Misal lampu konslet/tidak wajar)
        if (isConnected) {
            if (currentVoltage > 245.0) {
                if (!isProtectionAlertSent) {
                    sendSystemNotification("⚠️ PERINGATAN TEGANGAN", "Tegangan listrik tidak stabil (" + currentVoltage + "V). Segera cek sambungan!", NOTIF_ID_PROTECTION);
                    isProtectionAlertSent = true;
                }
            } else if (currentArus > 1.0) {
                 if (!isProtectionAlertSent) {
                    sendSystemNotification("⚠️ PERINGATAN ARUS", "Beban arus tidak wajar (" + currentArus + "A). Bahaya korsleting!", NOTIF_ID_PROTECTION);
                    isProtectionAlertSent = true;
                }
            } else {
                isProtectionAlertSent = false; // Reset jika sudah stabil
            }
        }

        // 2. PERINGATAN DURASI (OVERTIME)
        // Kita kurangi frekuensinya, hanya kirim jika benar-benar sudah sangat lama (misal 5 jam)
        if (isLampOn && lampOnStartTime > 0 && notifOvertime) {
            long durationSec = (System.currentTimeMillis() - lampOnStartTime) / 1000;
            if (durationSec > 18000) { // 5 Jam
                sendSystemNotification("Peringatan Pemakaian", "Lampu sudah menyala lebih dari 5 jam. Pastikan dimatikan jika tidak perlu.", NOTIF_ID_LAMP);
                lampOnStartTime = System.currentTimeMillis(); 
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isAppInForeground = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isAppInForeground = false;
    }

    // TEST COMMIT - tidak mengubah fungsi
    @Override
    protected void onResume() {
        super.onResume();
        loadAppState();
        applyUiState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realtimeHandler.removeCallbacksAndMessages(null);
    }
}
