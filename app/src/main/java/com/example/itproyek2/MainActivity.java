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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
    private TextView tvDaya, tvArus, tvTegangan;
    private TextView tvEspStatus, tvWifiStatus, tvCloudStatus;
    private ImageView ivLampIcon;
    private MaterialButtonToggleGroup toggleGroupLamp, toggleGroupMode;
    private Button btnDetail;

    private boolean isLampOn = true;
    private boolean isAutoMode = false;
    
    private final Handler realtimeHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final DecimalFormat df = new DecimalFormat("#.##");

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
        ivLampIcon = findViewById(R.id.ivLampIcon);
        toggleGroupLamp = findViewById(R.id.toggleGroupLamp);
        toggleGroupMode = findViewById(R.id.toggleGroupMode);
        btnDetail = findViewById(R.id.btnDetail);
        
        // IoT Status Views (Using generic way to find since they don't have IDs in previous XML edit, I should update XML first or find them by position, better update XML)
        // Wait, I will update activity_main.xml to add IDs for the IoT status and Light Condition
        
        // Initial Selection
        toggleGroupLamp.check(R.id.btnOn);
        toggleGroupMode.check(R.id.btnManual);

        // Control Lamp Logic
        toggleGroupLamp.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (isAutoMode) {
                    showToast("Matikan Mode Auto untuk kontrol manual");
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
                addNotification("Mode diubah ke " + (isAutoMode ? "Otomatis" : "Manual"));
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
        if (isLampOn) {
            double daya = 50 + random.nextDouble() * 10;
            double arus = daya / 220;
            double tegangan = 218 + random.nextDouble() * 4;
            
            tvDaya.setText(": " + df.format(daya) + " Watt");
            tvArus.setText(": " + df.format(arus) + " A");
            tvTegangan.setText(": " + df.format(tegangan) + " V");
        } else {
            tvDaya.setText(": 0 Watt");
            tvArus.setText(": 0 A");
            tvTegangan.setText(": 220 V");
        }
        
        // Random Light Condition
        int lightVal = random.nextInt(100);
        if (lightVal > 50) {
            tvKondisiCahaya.setText(": Terang (" + lightVal + "%)");
        } else {
            tvKondisiCahaya.setText(": Gelap (" + lightVal + "%)");
        }
        
        if (isAutoMode) {
            simulateAutoLogic();
        }
    }

    private void updateLampStatus(boolean on, String source) {
        isLampOn = on;
        if (on) {
            tvStatusLampu.setText("Lampu Nyala");
            ivLampIcon.setImageResource(R.drawable.ic_lamp_on);
            toggleGroupLamp.check(R.id.btnOn);
        } else {
            tvStatusLampu.setText("Lampu Mati");
            ivLampIcon.setImageResource(R.drawable.ic_lamp_off);
            toggleGroupLamp.check(R.id.btnOff);
        }
        
        if (!source.equals("System")) {
            addNotification("Lampu di" + (on ? "nyalakan" : "matikan") + " via " + source);
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
        tvNotification.setText("[" + currentTime + "] " + message);
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
