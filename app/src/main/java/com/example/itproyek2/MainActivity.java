package com.example.itproyek2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatusLampu, tvKondisiCahaya, tvNotification;
    private TextView tvDaya, tvArus, tvTegangan, tvEstimasiBiaya;
    private ImageView ivLampIcon;
    private MaterialButtonToggleGroup toggleGroupLamp, toggleGroupMode;
    private Button btnDetail;

    private boolean isLampOn = true;
    private boolean isAutoMode = false;
    private boolean isOverload = false;
    
    // Constants for features
    private static final double TARIF_PER_KWH = 1444.70; // Tarif PLN R1
    private static final double MAX_WATT_LIMIT = 100.0; // Limit for Overload Alert
    
    private final Handler realtimeHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final DecimalFormat df = new DecimalFormat("#.##");
    private final DecimalFormat currencyDf = new DecimalFormat("#,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Apply Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        tvStatusLampu = findViewById(R.id.tvStatusLampu);
        tvKondisiCahaya = findViewById(R.id.tvKondisiCahaya);
        tvNotification = findViewById(R.id.tvNotification);
        tvDaya = findViewById(R.id.tvDaya);
        tvArus = findViewById(R.id.tvArus);
        tvTegangan = findViewById(R.id.tvTegangan);
        tvEstimasiBiaya = findViewById(R.id.tvEstimasiBiaya);
        ivLampIcon = findViewById(R.id.ivLampIcon);
        toggleGroupLamp = findViewById(R.id.toggleGroupLamp);
        toggleGroupMode = findViewById(R.id.toggleGroupMode);
        btnDetail = findViewById(R.id.btnDetail);
        
        // Initial Selection
        toggleGroupLamp.check(R.id.btnOn);
        toggleGroupMode.check(R.id.btnManual);

        // Control Lamp Logic
        toggleGroupLamp.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (isOverload) {
                    showToast("Sistem terkunci karena Overload. Reset perangkat.");
                    group.post(() -> group.check(R.id.btnOff));
                } else if (isAutoMode) {
                    showToast(getString(R.string.msg_disable_auto));
                    group.post(() -> group.check(isLampOn ? R.id.btnOn : R.id.btnOff));
                } else {
                    updateLampStatus(checkedId == R.id.btnOn, "Manual");
                }
            }
        });

        // Mode System Logic
        toggleGroupMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                isAutoMode = (checkedId == R.id.btnAuto);
                String modeStr = isAutoMode ? getString(R.string.auto) : getString(R.string.manual);
                addNotification(getString(R.string.notif_mode_changed, modeStr));
                if (isAutoMode) simulateAutoLogic();
            }
        });

        // Detail Button Logic
        btnDetail.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            startActivity(intent);
        });

        // Start Realtime Simulation
        startRealtimeSimulation();
    }

    private void startRealtimeSimulation() {
        realtimeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateRealtimeData();
                realtimeHandler.postDelayed(this, 2000); // Update every 2 seconds
            }
        }, 1000);
    }

    private void updateRealtimeData() {
        double daya;
        if (isLampOn && !isOverload) {
            // Normal Simulation with possibility of random spike for overload testing
            daya = 50 + random.nextDouble() * 10;
            
            // Randomly trigger overload for demo purposes (1% chance)
            if (random.nextInt(100) == 1) {
                daya = 120.5; // Trigger Overload
            }
            
            double arus = daya / 220;
            double tegangan = 218 + random.nextDouble() * 4;
            
            tvDaya.setText(String.format(Locale.getDefault(), ": %s Watt", df.format(daya)));
            tvArus.setText(String.format(Locale.getDefault(), ": %s A", df.format(arus)));
            tvTegangan.setText(String.format(Locale.getDefault(), ": %s V", df.format(tegangan)));
            
            // Feature: Energy & Cost Analytics (Simulated hourly cost)
            double estimasiBiaya = (daya / 1000) * TARIF_PER_KWH;
            tvEstimasiBiaya.setText(String.format(Locale.getDefault(), ": Rp %s/jam", currencyDf.format(estimasiBiaya)));

            // Feature: Overload Alert
            if (daya > MAX_WATT_LIMIT) {
                triggerOverload(daya);
            }

        } else {
            tvDaya.setText(": 0 Watt");
            tvArus.setText(": 0 A");
            tvTegangan.setText(": 220 V");
            tvEstimasiBiaya.setText(": Rp 0");
        }
        
        // Random Light Condition
        int lightVal = random.nextInt(100);
        String conditionStr = lightVal > 50 ? "Terang" : "Gelap";
        tvKondisiCahaya.setText(String.format(Locale.getDefault(), ": %s (%d%%)", conditionStr, lightVal));
        
        if (isAutoMode && !isOverload) {
            simulateAutoLogic();
        }
    }

    private void triggerOverload(double currentDaya) {
        isOverload = true;
        updateLampStatus(false, "System (Overload)");
        
        // Show Alert Dialog
        new AlertDialog.Builder(this)
                .setTitle(R.string.alert_overload_title)
                .setMessage(getString(R.string.alert_overload_msg, df.format(currentDaya)))
                .setPositiveButton("Reset", (dialog, which) -> {
                    isOverload = false;
                    addNotification("System Reset after Overload");
                })
                .setCancelable(false)
                .show();
                
        addNotification(getString(R.string.notif_overload));
    }

    private void updateLampStatus(boolean on, String source) {
        isLampOn = on;
        if (on) {
            tvStatusLampu.setText(R.string.lampu_nyala);
            ivLampIcon.setImageResource(R.drawable.ic_lamp_on);
            toggleGroupLamp.check(R.id.btnOn);
        } else {
            tvStatusLampu.setText("Lampu Mati");
            ivLampIcon.setImageResource(R.drawable.ic_lamp_off);
            toggleGroupLamp.check(R.id.btnOff);
        }
        
        if (!source.equals("System") && !source.contains("Overload")) {
            String status = on ? "nyalakan" : "matikan";
            addNotification(getString(R.string.notif_lamp_status, status, source));
        }
    }

    private void simulateAutoLogic() {
        String kondisi = tvKondisiCahaya.getText().toString();
        boolean shouldBeOn = kondisi.contains("Gelap");
        if (isLampOn != shouldBeOn) {
            updateLampStatus(shouldBeOn, "Auto Mode");
        }
    }

    private void addNotification(String message) {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        tvNotification.setText(String.format("[%s] %s", currentTime, message));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // TEST COMMIT - tidak mengubah fungsi
    @Override
    protected void onDestroy() {
        super.onDestroy();
        realtimeHandler.removeCallbacksAndMessages(null);
    }
}
