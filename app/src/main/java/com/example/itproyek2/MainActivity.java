package com.example.itproyek2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

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

    private DatabaseReference dbRef;
    private boolean isLampOn, isAutoMode, isConnected = false;
    private long lastTickTime = 0;
    private final Handler offlineCheckHandler = new Handler();

    private boolean notifLamp, notifOvertime, notifEnergy;
    private int currentLdrValue = 0;
    private double totalKwh = 0;
    private long lampOnStartTime = 0;
    private boolean useManualLux = false;
    private boolean isDarkManualOverride = false;

    private Handler realtimeHandler = new Handler();
    private Random random = new Random();
    private DecimalFormat df = new DecimalFormat("0.00");
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private static final String CHANNEL_ID = "SMART_LAMP_NOTIF";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        // Inisialisasi Firebase
        dbRef = FirebaseDatabase.getInstance("https://smartpress-ea81d-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
        
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
                if (!isConnected) {
                    showToast("waduh, device lagi offline nih");
                    toggleGroupLamp.post(() -> toggleGroupLamp.check(isLampOn ? R.id.btnOn : R.id.btnOff));
                } else if (isAutoMode) {
                    showToast("matiin mode OTOMATIS dulu kalo mau manual");
                    toggleGroupLamp.post(() -> toggleGroupLamp.check(isLampOn ? R.id.btnOn : R.id.btnOff));
                } else {
                    updateLampState(checkedId == R.id.btnOn, "Kontrol Manual");
                }
            }
        });

        // klik status cahaya buat simulasi gelap terang
        layoutStatusCahaya.setOnClickListener(v -> {
            useManualLux = true;
            isDarkManualOverride = !isDarkManualOverride;
            int simulatedLdrValue = isDarkManualOverride ? 3500 : 500;
            showToast("demo: sensor set " + (isDarkManualOverride ? "GELAP" : "TERANG"));
            updateCahayaUi(simulatedLdrValue);
            
            if (isAutoMode && isConnected) {
                updateLampState(isDarkManualOverride, "Sensor Otomatis (Demo)");
            }
        });

        // ganti mode manual/otomatis
        toggleGroupMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (!isConnected) {
                    showToast("offilne bro, gak bisa ganti mode");
                    toggleGroupMode.post(() -> toggleGroupMode.check(isAutoMode ? R.id.btnAuto : R.id.btnManual));
                } else {
                    boolean newMode = (checkedId == R.id.btnAuto);
                    dbRef.child("auto_mode").setValue(newMode);
                    addLog("mode ganti ke " + (newMode ? "OTOMATIS" : "MANUAL"));
                }
            }
        });

        btnDetail.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DetailActivity.class));
        });

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
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Smart Lamp Notifications", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void sendSystemNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo_smartpress)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(random.nextInt(1000), builder.build());
    }

    private void syncLampToggle() {
        toggleGroupLamp.post(() -> toggleGroupLamp.check(isLampOn ? R.id.btnOn : R.id.btnOff));
    }

    private void loadAppState() {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        isLampOn = prefs.getBoolean("lamp_status", false);
        isAutoMode = prefs.getBoolean("auto_mode", false);
        totalKwh = Double.parseDouble(prefs.getString("total_kwh", "0.0").replace(",", "."));
        
        notifLamp = prefs.getBoolean("notif_lamp", true);
        notifOvertime = prefs.getBoolean("notif_overtime", true);
        notifEnergy = prefs.getBoolean("notif_energy", true);
    }

    private void saveAppState() {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
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
        
        updateCahayaUi(useManualLux ? (isDarkManualOverride ? 3500 : 500) : currentLdrValue); 
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
                        addLog("Lampu " + (isLampOn ? "MENYALA" : "MATI"));
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

        // Listener untuk Status Koneksi ESP32 (Heartbeat)
        dbRef.child("is_connected_tick").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    isConnected = true;
                    lastTickTime = System.currentTimeMillis();
                    applyUiState();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Loop pengecekan offline (Jika 7 detik tidak ada update dari ESP32 = Offline)
        offlineCheckHandler.post(new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastTickTime > 7000) {
                    if (isConnected) {
                        isConnected = false;
                        applyUiState();
                    }
                }
                offlineCheckHandler.postDelayed(this, 3000);
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
        String currentTime = timeFormat.format(new Date());
        tvLogAktivitas.setText("[" + currentTime + "] " + message);
        saveLogToHistory(message);
    }

    private void saveLogToHistory(String message) {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
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
                    // Update perhitungan energi tetap berjalan lokal 
                    // atau bisa diambil dari sensor PZEM jika sudah ada
                    calculateEnergyLocally();
                    checkAlerts();
                }
                realtimeHandler.postDelayed(this, 2000); 
            }
        }, 1000);
    }

    private void calculateEnergyLocally() {
        // Karena sensor tegangan belum ada, kita set nilai ke 0 sesuai permintaan
        double voltase = 0.0; 
        double arus = 0.0; 
        double currentWatt = 0.0;

        tvDaya.setText(df.format(currentWatt) + " Watt");

        tvKwhSummary.setText("Total Pemakaian Hari Ini: " + df.format(totalKwh) + " kWh");
        tvCostSummary.setText(currencyFormat.format(totalKwh * 1444.70));
        saveAppState();
    }

    private void updateCahayaUi(int ldrValue) {
        // Hitung persentase kecerahan (0 - 100%)
        int brightnessPercent = (int) ((1.0 - (ldrValue / 4095.0)) * 100);
        if (brightnessPercent < 0) brightnessPercent = 0;
        if (brightnessPercent > 100) brightnessPercent = 100;

        boolean isDark = ldrValue > 2000;
        
        tvKondisiCahaya.setText(isDark ? "Gelap (" + brightnessPercent + "%)" : "Terang (" + brightnessPercent + "%)");
        layoutStatusCahaya.setBackgroundResource(isDark ? R.drawable.status_bg_dark : R.drawable.status_bright_bg);
        ivKondisiIcon.setImageResource(isDark ? R.drawable.ic_star_on : R.drawable.ic_history);

        // Logika kontrol otomatis yang sebenarnya
        if (isAutoMode && isConnected) {
            updateLampState(isDark, "Sensor Otomatis");
        }
    }

    private void checkAlerts() {
        if (isLampOn && lampOnStartTime > 0 && notifOvertime) {
            long durationSec = (System.currentTimeMillis() - lampOnStartTime) / 1000;
            if (durationSec > 120) { 
                sendSystemNotification("Awas Kelamaan", "Lampu sudah nyala > 2 menit.");
                addLog("Peringatan: Overtime");
                lampOnStartTime = System.currentTimeMillis(); 
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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