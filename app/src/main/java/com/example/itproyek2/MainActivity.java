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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatusLampu, tvKondisiCahaya, tvLogAktivitas;
    private TextView tvDaya, tvCostSummary, tvKwhSummary;
    private TextView tvEspStatus, tvWifiStatus;
    private ImageView ivLampIllustration, ivKondisiIcon;
    private View bulbGlow;
    private LinearLayout layoutStatusCahaya;
    private MaterialButtonToggleGroup toggleGroupLamp, toggleGroupMode;
    private Button btnDetail;

    private boolean isLampOn, isAutoMode, isConnected = true;
    private boolean isDarkManualOverride = false, useManualLux = false;
    private double totalKwh;
    private long lampOnStartTime = 0;

    private boolean notifLamp, notifOvertime, notifEnergy;

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
            showToast("demo: sensor kita set " + (isDarkManualOverride ? "GELAP" : "TERANG"));
            updateCahayaUi(isDarkManualOverride);
            
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
                    isAutoMode = (checkedId == R.id.btnAuto);
                    saveAppState();
                    addLog("mode ganti ke " + (isAutoMode ? "OTOMATIS" : "MANUAL"));
                    
                    if (isAutoMode) {
                        boolean isDarkNow = useManualLux ? isDarkManualOverride : (random.nextInt(1000) < 300);
                        updateLampState(isDarkNow, "Sensor Otomatis");
                    }
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
        
        updateCahayaUi(false); 
    }

    private void updateLampState(boolean turnOn, String source) {
        if (isLampOn == turnOn) return;
        
        isLampOn = turnOn;
        if (isLampOn) lampOnStartTime = System.currentTimeMillis();
        else lampOnStartTime = 0;

        addLog("lampu di" + (isLampOn ? "HIDUPKAN" : "MATIKAN") + " via " + source);
        if (notifLamp) sendSystemNotification("Status Lampu", "Lampu sekarang " + (isLampOn ? "MENYALA" : "MATI"));
        
        applyUiState();
        saveAppState();
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
                    simulateIoTData();
                    checkAlerts();
                }
                realtimeHandler.postDelayed(this, 2000); 
            }
        }, 1000);
    }

    private void simulateIoTData() {
        boolean isDarkNow = useManualLux ? isDarkManualOverride : (random.nextInt(1000) < 300);
        updateCahayaUi(isDarkNow);

        if (isAutoMode) {
            if (isDarkNow && !isLampOn) updateLampState(true, "Sensor Otomatis");
            else if (!isDarkNow && isLampOn) updateLampState(false, "Sensor Otomatis");
        }

        double voltase = 215 + random.nextDouble() * 15; // Range 215 - 230V (Lebih stabil)
        double arus = isLampOn ? (0.2 + random.nextDouble() * 0.2) : 0; // Range 0.2 - 0.4A (Dibawah limit 0.5A)

        if (isLampOn) {
            if (voltase > 240.0) {
                updateLampState(false, "Proteksi Tegangan");
                sendSystemNotification("Bahaya Listrik!", "Tegangan tinggi (" + df.format(voltase) + "V). Lampu dimatikan.");
            } else if (arus > 0.5) {
                updateLampState(false, "Proteksi Arus");
                sendSystemNotification("Bahaya Arus!", "Arus berlebih (" + df.format(arus) + "A). Lampu dimatikan.");
            }

            double currentWatt = voltase * arus;
            totalKwh += (currentWatt / 1000.0) * (2.0 / 3600.0);
            tvDaya.setText(df.format(currentWatt) + " Watt");
        } else {
            tvDaya.setText("0 Watt");
        }

        tvKwhSummary.setText("Total Pemakaian Hari Ini: " + df.format(totalKwh) + " kWh");
        tvCostSummary.setText(currencyFormat.format(totalKwh * 1444.70));
        saveAppState();
    }

    private void updateCahayaUi(boolean isDark) {
        tvKondisiCahaya.setText(isDark ? "Gelap" : "Terang");
        layoutStatusCahaya.setBackgroundResource(isDark ? R.drawable.status_bg_dark : R.drawable.status_bright_bg);
        ivKondisiIcon.setImageResource(isDark ? R.drawable.ic_star_on : R.drawable.ic_history);
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