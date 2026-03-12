package com.example.itproyek2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
    private ImageView ivLampIllustration, ivKondisiIcon;
    private LinearLayout layoutStatusCahaya;
    private MaterialButtonToggleGroup toggleGroupLamp, toggleGroupMode;
    private Button btnDetail;

    private boolean isLampOn = true;
    private boolean isAutoMode = false;
    private double totalKwh = 0.45; // Simulated starting value
    
    private final Handler realtimeHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final DecimalFormat df = new DecimalFormat("#.##");
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize NumberFormat for no decimals in currency
        currencyFormat.setMaximumFractionDigits(0);

        // Initialize Views
        tvStatusLampu = findViewById(R.id.tvStatusLampu);
        tvKondisiCahaya = findViewById(R.id.tvKondisiCahaya);
        tvLogAktivitas = findViewById(R.id.tvLogAktivitas);
        tvDaya = findViewById(R.id.tvDaya);
        tvCostSummary = findViewById(R.id.tvCostSummary);
        tvKwhSummary = findViewById(R.id.tvKwhSummary);
        
        ivLampIllustration = findViewById(R.id.ivLampIllustration);
        ivKondisiIcon = findViewById(R.id.ivKondisiIcon);
        layoutStatusCahaya = findViewById(R.id.layoutStatusCahaya);
        toggleGroupLamp = findViewById(R.id.toggleGroupLamp);
        toggleGroupMode = findViewById(R.id.toggleGroupMode);
        btnDetail = findViewById(R.id.btnDetail);
        
        // Initial Selection
        toggleGroupLamp.check(R.id.btnOn);
        toggleGroupMode.check(R.id.btnManual);

        // Control Lamp Logic
        toggleGroupLamp.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (isAutoMode) {
                    showToast("Matikan Mode OTOMATIS untuk kontrol manual");
                    group.post(() -> group.check(isLampOn ? R.id.btnOn : R.id.btnOff));
                } else {
                    updateLampState(checkedId == R.id.btnOn, "Manual");
                }
            }
        });

        toggleGroupMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                isAutoMode = (checkedId == R.id.btnAuto);
                addLog("Mode diubah ke " + (isAutoMode ? "OTOMATIS" : "MANUAL"));
            }
        });

        btnDetail.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            startActivity(intent);
        });

        startRealtimeSimulation();
    }

    private void updateLampState(boolean on, String triggerSource) {
        if (isLampOn != on) {
            isLampOn = on;
            tvStatusLampu.setText(on ? "ON" : "OFF");
            ivLampIllustration.setColorFilter(ContextCompat.getColor(this, on ? R.color.accent_yellow : R.color.text_secondary));
            if (on && toggleGroupLamp.getCheckedButtonId() != R.id.btnOn) toggleGroupLamp.check(R.id.btnOn);
            else if (!on && toggleGroupLamp.getCheckedButtonId() != R.id.btnOff) toggleGroupLamp.check(R.id.btnOff);
            addLog("Lampu " + (on ? "HIDUP" : "MATI") + " (" + triggerSource + ")");
        }
    }

    private void addLog(String message) {
        String currentTime = timeFormat.format(new Date());
        tvLogAktivitas.setText("[" + currentTime + "] " + message);
    }

    private void startRealtimeSimulation() {
        realtimeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                simulateIoTData();
                realtimeHandler.postDelayed(this, 2000); 
            }
        }, 1000);
    }

    private void simulateIoTData() {
        // 1. Light Condition Simulation
        int lux = random.nextInt(1000);
        boolean isDark = lux < 300;
        
        if (isDark) {
            tvKondisiCahaya.setText("Gelap");
            tvKondisiCahaya.setTextColor(ContextCompat.getColor(this, R.color.white));
            ivKondisiIcon.setImageResource(R.drawable.ic_star_on); 
            ivKondisiIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
            layoutStatusCahaya.setBackgroundResource(R.drawable.status_bg_dark);
        } else {
            tvKondisiCahaya.setText("Terang");
            tvKondisiCahaya.setTextColor(ContextCompat.getColor(this, R.color.bg_dark));
            ivKondisiIcon.setImageResource(R.drawable.ic_star_on);
            ivKondisiIcon.setColorFilter(ContextCompat.getColor(this, R.color.bg_dark));
            layoutStatusCahaya.setBackgroundResource(R.drawable.status_bright_bg);
        }

        if (isAutoMode) {
            if (isDark && !isLampOn) updateLampState(true, "Auto Sensor");
            else if (!isDark && isLampOn) updateLampState(false, "Auto Sensor");
        }

        // 2. Consumption Summary Calculation
        if (isLampOn) {
            double currentWatt = 45 + random.nextDouble() * 10;
            totalKwh += (currentWatt / 1000.0) * (2.0 / 3600.0);
            
            tvDaya.setText(df.format(currentWatt) + " Watt");
            tvCostSummary.setText(currencyFormat.format(totalKwh * 1444.70));
            tvKwhSummary.setText("Total Pemakaian Hari Ini: " + df.format(totalKwh) + " kWh");
        } else {
            tvDaya.setText("0 Watt");
            tvCostSummary.setText(currencyFormat.format(totalKwh * 1444.70));
            tvKwhSummary.setText("Total Pemakaian Hari Ini: " + df.format(totalKwh) + " kWh");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realtimeHandler.removeCallbacksAndMessages(null);
    }
}
