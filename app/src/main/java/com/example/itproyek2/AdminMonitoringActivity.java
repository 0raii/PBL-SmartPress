package com.example.itproyek2;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class AdminMonitoringActivity extends AppCompatActivity {

    private TextView tvHardwareStatus, tvLampStatus, tvLuxValue;
    private TextView tvVoltage, tvCurrent, tvPowerWatt;
    private MaterialCardView btnBack;

    private DatabaseReference dbRef;
    private long lastTickTime = 0;
    private boolean isConnected = false;
    private final Handler offlineCheckHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_monitoring);

        tvHardwareStatus = findViewById(R.id.tvHardwareStatus);
        tvLampStatus = findViewById(R.id.tvLampStatus);
        tvLuxValue = findViewById(R.id.tvLuxValue);
        tvVoltage = findViewById(R.id.tvVoltage);
        tvCurrent = findViewById(R.id.tvCurrent);
        tvPowerWatt = findViewById(R.id.tvPowerWatt);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        dbRef = FirebaseDatabase.getInstance("https://smartpress-ea81d-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
        
        initFirebaseListeners();
        startOfflineCheckLoop();
    }

    private void initFirebaseListeners() {
        // Status Koneksi (Heartbeat)
        dbRef.child("is_connected_tick").addValueEventListener(new ValueEventListener() {
            private Object lastValue = null;
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Object newValue = snapshot.getValue();
                    if (lastValue != null && !newValue.equals(lastValue)) {
                        isConnected = true;
                        lastTickTime = System.currentTimeMillis();
                        updateConnectionUi();
                    }
                    lastValue = newValue;
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Status Lampu
        dbRef.child("lamp_status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    boolean isOn = snapshot.getValue(Boolean.class);
                    tvLampStatus.setText(isOn ? "ON" : "OFF");
                    tvLampStatus.setTextColor(isOn ? 
                            ContextCompat.getColor(AdminMonitoringActivity.this, R.color.accent_yellow) : 
                            ContextCompat.getColor(AdminMonitoringActivity.this, R.color.text_sub));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Sensor Lux
        dbRef.child("sensor_lux").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int lux = snapshot.getValue(Integer.class);
                    tvLuxValue.setText(lux + " Lux");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // PZEM - Voltage
        dbRef.child("sensor_voltage").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if(s.exists()) tvVoltage.setText(String.format(Locale.US, "%.1f V", s.getValue(Double.class)));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        // PZEM - Current
        dbRef.child("sensor_current").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if(s.exists()) tvCurrent.setText(String.format(Locale.US, "%.2f A", s.getValue(Double.class)));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        // PZEM - Power
        dbRef.child("sensor_power").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if(s.exists()) tvPowerWatt.setText(String.format(Locale.US, "%.1f W", s.getValue(Double.class)));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void startOfflineCheckLoop() {
        offlineCheckHandler.post(new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastTickTime > 20000) {
                    if (isConnected) {
                        isConnected = false;
                        updateConnectionUi();
                    }
                }
                offlineCheckHandler.postDelayed(this, 5000);
            }
        });
    }

    private void updateConnectionUi() {
        tvHardwareStatus.setText(isConnected ? "ONLINE" : "OFFLINE");
        tvHardwareStatus.setTextColor(Color.parseColor(isConnected ? "#4CAF50" : "#F44336"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        offlineCheckHandler.removeCallbacksAndMessages(null);
    }
}
