package com.example.itproyek2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

    private boolean isLampOn;
    private boolean isAutoMode;
    private boolean isConnected = true;
    private boolean isDarkManualOverride = false;
    private boolean useManualLux = false;
    private double totalKwh;
    private long lampOnStartTime = 0;
    
    private boolean notifLamp, notifOvertime, notifOverheat, notifEnergy;

    private final Handler realtimeHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final DecimalFormat df = new DecimalFormat("#.##");
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private static final String CHANNEL_ID = "SMART_LAMP_NOTIF";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // cek tema yg disimpen dulu
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        boolean isDarkTheme = prefs.getBoolean("is_dark_theme", true);
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        // atur biar gak nabrak bar atas
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        currencyFormat.setMaximumFractionDigits(0);

        // hubungin id layout ke variabel
        tvStatusLampu = findViewById(R.id.tvStatusLampu);
        tvKondisiCahaya = findViewById(R.id.tvKondisiCahaya);
        tvLogAktivitas = findViewById(R.id.tvLogAktivitas);
        tvDaya = findViewById(R.id.tvDaya);
        tvCostSummary = findViewById(R.id.tvCostSummary);
        tvKwhSummary = findViewById(R.id.tvKwhSummary);
        tvEspStatus = findViewById(R.id.tvEspStatus);
        tvWifiStatus = findViewById(R.id.tvWifiStatus);
        
        ivLampIllustration = findViewById(R.id.ivLampIllustration);
        bulbGlow = findViewById(R.id.bulbGlow);
        ivKondisiIcon = findViewById(R.id.ivKondisiIcon);
        layoutStatusCahaya = findViewById(R.id.layoutStatusCahaya);
        toggleGroupLamp = findViewById(R.id.toggleGroupLamp);
        toggleGroupMode = findViewById(R.id.toggleGroupMode);
        btnDetail = findViewById(R.id.btnDetail);

        loadAppState();
        applyUiState();

        // klik tombol on/off lampu
        toggleGroupLamp.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (!isConnected) {
                    showToast("yah lagi offline perangkatnya!");
                    syncLampToggle();
                } else if (isAutoMode) {
                    showToast("matiin dulu mode otomatisnya ya");
                    syncLampToggle();
                } else {
                    updateLampState(checkedId == R.id.btnOn, "Manual");
                }
            }
        });

        // simulasi klik status buat benerin koneksi
        tvEspStatus.setOnClickListener(v -> {
            if (!isConnected) {
                showToast("nyoba konek lagi...");
                new Handler().postDelayed(() -> {
                    isConnected = true;
                    saveAppState();
                    applyUiState();
                    addLog("info: konek lagi cuy");
                    showToast("siip udah online");
                }, 1500);
            } else {
                isConnected = false;
                saveAppState();
                applyUiState();
                addLog("peringatan: dc nih (demo)");
                showToast("mode offline aktif (demo)");
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
            CharSequence name = "Smart Lamp Channel";
            String description = "notif penting buat lampu";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(Color.YELLOW);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendSystemNotification(String title, String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_lamp_on)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    // cocokin status tombol sama aslinya
    private void syncLampToggle() {
        toggleGroupLamp.post(() -> toggleGroupLamp.check(isLampOn ? R.id.btnOn : R.id.btnOff));
    }

    // ambil settingan yg pernah disimpen
    private void loadAppState() {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        isLampOn = prefs.getBoolean("is_lamp_on", true);
        isAutoMode = prefs.getBoolean("is_auto_mode", false);
        isConnected = prefs.getBoolean("is_connected", true);
        String kwhStr = prefs.getString("total_kwh", "0.45");
        try {
            totalKwh = Double.parseDouble(kwhStr.replace(",", "."));
        } catch (Exception e) {
            totalKwh = 0.45;
        }
        
        notifLamp = prefs.getBoolean("notif_lamp", true);
        notifOvertime = prefs.getBoolean("notif_overtime", true);
        notifOverheat = prefs.getBoolean("notif_overheat", true);
        notifEnergy = prefs.getBoolean("notif_energy", true);

        if (isLampOn) lampOnStartTime = System.currentTimeMillis();
    }

    // simpen settingan skrg
    private void saveAppState() {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_lamp_on", isLampOn);
        editor.putBoolean("is_auto_mode", isAutoMode);
        editor.putBoolean("is_connected", isConnected);
        editor.putString("total_kwh", df.format(totalKwh));
        editor.apply();
    }

    // update tampilan sesuai status
    private void applyUiState() {
        tvStatusLampu.setText(isLampOn ? "HIDUP" : "MATI");
        int accentColor = ContextCompat.getColor(this, isLampOn ? R.color.accent_yellow : R.color.text_secondary);
        ivLampIllustration.setColorFilter(accentColor);
        if (bulbGlow != null) bulbGlow.setVisibility(isLampOn ? View.VISIBLE : View.GONE);
        
        syncLampToggle();
        toggleGroupMode.check(isAutoMode ? R.id.btnAuto : R.id.btnManual);
        
        if (isConnected) {
            tvEspStatus.setText("Online");
            tvEspStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
            tvWifiStatus.setText("Terhubung");
            tvWifiStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
            ivLampIllustration.setAlpha(1.0f);
        } else {
            tvEspStatus.setText("Offline");
            tvEspStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvWifiStatus.setText("Terputus");
            tvWifiStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            ivLampIllustration.setAlpha(0.3f);
            if (bulbGlow != null) bulbGlow.setVisibility(View.GONE);
            tvStatusLampu.setText("OFFLINE");
            tvDaya.setText("- Watt");
        }
    }

    // ganti status lampu
    private void updateLampState(boolean on, String triggerSource) {
        if (isLampOn != on) {
            isLampOn = on;
            lampOnStartTime = isLampOn ? System.currentTimeMillis() : 0;
            saveAppState();
            applyUiState();
            addLog("lampu " + (on ? "HIDUP" : "MATI") + " (" + triggerSource + ")");
            if (notifLamp) showToast("notif: lampu " + (on ? "nyala" : "mati"));
        }
    }

    // nambahin log aktivitas
    private void addLog(String message) {
        String currentTime = timeFormat.format(new Date());
        tvLogAktivitas.setText("[" + currentTime + "] " + message);
        saveLogToHistory(message);
    }

    // simpen log biar bisa diliat di riwayat
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
        else if (msg.contains("OFFLINE")) iconType = 5;

        String newEntry = message + "|" + "Hari Ini " + currentTime + "|" + iconType + ";";
        prefs.edit().putString("history_data", history + newEntry).apply();
    }

    // mulai simulasi data tiap bbrp detik
    private void startRealtimeSimulation() {
        realtimeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // simulasi dc bntar biar kaya beneran
                if (!useManualLux && random.nextInt(200) < 1) { 
                    isConnected = !isConnected;
                    saveAppState();
                    runOnUiThread(() -> applyUiState());
                    if (!isConnected) {
                        addLog("peringatan: koneksi putus (offline)");
                        sendSystemNotification("putus koneksi", "perangkat esp32 gak bisa dikontak nih.");
                    } else {
                        addLog("info: konek lagi");
                    }
                }

                if (isConnected) {
                    simulateIoTData();
                    checkAlerts();
                }
                realtimeHandler.postDelayed(this, 2000); 
            }
        }, 1000);
    }

    // simulasi data iot masuk
    private void simulateIoTData() {
        boolean isDarkNow;
        if (useManualLux) {
            isDarkNow = isDarkManualOverride;
        } else {
            int lux = random.nextInt(1000);
            isDarkNow = lux < 300;
        }
        
        updateCahayaUi(isDarkNow);

        if (isAutoMode) {
            if (isDarkNow && !isLampOn) updateLampState(true, "Sensor Otomatis");
            else if (!isDarkNow && isLampOn) updateLampState(false, "Sensor Otomatis");
        }

        if (isLampOn) {
            double currentWatt = 45 + random.nextDouble() * 10;
            totalKwh += (currentWatt / 1000.0) * (2.0 / 3600.0);
            tvDaya.setText(df.format(currentWatt) + " Watt");
        } else {
            tvDaya.setText("0 Watt");
        }
        
        tvCostSummary.setText(currencyFormat.format(totalKwh * 1444.70));
        tvKwhSummary.setText("total pemakaian hari ini: " + df.format(totalKwh) + " kWh");
        saveAppState();
    }

    // update UI buat kondisi cahaya
    private void updateCahayaUi(boolean isDark) {
        tvKondisiCahaya.setText(isDark ? "Gelap" : "Terang");
        tvKondisiCahaya.setTextColor(ContextCompat.getColor(this, isDark ? R.color.white : R.color.bg_dark));
        ivKondisiIcon.setColorFilter(ContextCompat.getColor(this, isDark ? R.color.white : R.color.bg_dark));
        layoutStatusCahaya.setBackgroundResource(isDark ? R.drawable.status_bg_dark : R.drawable.status_bright_bg);
        ivKondisiIcon.setImageResource(isDark ? R.drawable.ic_star_on : R.drawable.ic_history);
    }

    // cek kalo ada yg gak beres
    private void checkAlerts() {
        if (isLampOn && lampOnStartTime > 0 && notifOvertime) {
            long durationSec = (System.currentTimeMillis() - lampOnStartTime) / 1000;
            if (durationSec > 120) { // set 2 menit buat demo
                sendSystemNotification("awas kelamaan", "lampu nyala udah lebih dari 2 menit lho.");
                addLog("peringatan: overtime terdeteksi");
                lampOnStartTime = System.currentTimeMillis(); 
            }
        }

        float temp = 30 + random.nextFloat() * 20;
        if (temp > 48 && notifOverheat) {
            sendSystemNotification("panas nih", "suhu esp32 sampe " + df.format(temp) + "°C.");
            addLog("peringatan: overheat " + df.format(temp) + "°C");
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