package com.example.itproyek2;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

public class DetailActivity extends AppCompatActivity {

    private LineChart lineChart;
    private TextView tvPrediction, tvEfficiency;
    private TextView tvVoltDetail, tvAmpereDetail, tvDurasiNyala;
    private TextView tvKwhToday, tvEstimasiBiaya;
    private com.google.android.material.card.MaterialCardView cardProtectionAlert;
    private TextView tvProtectionTitle, tvProtectionDesc;
    
    private DatabaseReference dbRef;
    private boolean isLampOn, isConnected;
    private long lastTickTime = 0;
    private int xValue = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Handler offlineCheckHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final DecimalFormat df = new DecimalFormat("#.##");
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    
    private long startTime = System.currentTimeMillis();
    private double totalKwh;
    private final ArrayList<Entry> entries = new ArrayList<>();

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
        setContentView(R.layout.activity_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbarDetail);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        currencyFormat.setMaximumFractionDigits(0);

        tvPrediction = findViewById(R.id.tvPrediction);
        tvEfficiency = findViewById(R.id.tvEfficiency);
        tvVoltDetail = findViewById(R.id.tvVoltDetail);
        tvAmpereDetail = findViewById(R.id.tvAmpereDetail);
        tvDurasiNyala = findViewById(R.id.tvDurasiNyala);
        tvKwhToday = findViewById(R.id.tvKwhToday);
        tvEstimasiBiaya = findViewById(R.id.tvEstimasiBiaya);
        cardProtectionAlert = findViewById(R.id.cardProtectionAlert);
        tvProtectionTitle = findViewById(R.id.tvProtectionTitle);
        tvProtectionDesc = findViewById(R.id.tvProtectionDesc);
        lineChart = findViewById(R.id.lineChart); 

        dbRef = FirebaseDatabase.getInstance("https://smartpress-ea81d-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
        initFirebaseListeners();

        // Loop pengecekan offline
        offlineCheckHandler.post(new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastTickTime > 7000) {
                    isConnected = false;
                }
                offlineCheckHandler.postDelayed(this, 3000);
            }
        });

        loadCurrentKwh();
        setupChart(); 
        startDataSimulation();
    }

    // ambil data kwh yg ada
    private void loadCurrentKwh() {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        String kwhStr = prefs.getString("total_kwh", "0.0").replace(",", ".");
        try {
            totalKwh = Double.parseDouble(kwhStr);
        } catch (Exception e) {
            totalKwh = 0.0;
        }
    }

    // simpen kwh terbaru
    private void saveCurrentKwh() {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        prefs.edit().putString("total_kwh", String.format(Locale.US, "%.4f", totalKwh)).apply();
    }

    // atur grafik garis nya
    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setBackgroundColor(Color.TRANSPARENT);
        lineChart.setNoDataText("tunggu bentar, datanya lg dijalan...");
        
        int textColor = Color.parseColor("#999999"); // Abu-abu lembut agar enak dibaca
        lineChart.setNoDataTextColor(textColor);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextColor(textColor);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(5);

        lineChart.getAxisLeft().setTextColor(textColor);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#1AFFFFFF")); // Grid tipis
        lineChart.getAxisLeft().setAxisMinimum(0f); // Mulai dari 0 biar gampang dibaca
        
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false); // Matikan legend biar bersih
    }

    // mulai simulasi data real-time
    private void startDataSimulation() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateDeepMonitoring();
                handler.postDelayed(this, 2000); 
            }
        }, 1000);
    }

    private void initFirebaseListeners() {
        dbRef.child("lamp_status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) isLampOn = snapshot.getValue(Boolean.class);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        dbRef.child("is_connected_tick").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    isConnected = true;
                    lastTickTime = System.currentTimeMillis();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        dbRef.child("lamp_duration_sec").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    long secondsTotal = snapshot.getValue(Long.class);
                    long hours = secondsTotal / 3600;
                    long minutes = (secondsTotal % 3600) / 60;
                    long seconds = secondsTotal % 60;
                    tvDurasiNyala.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // update monitoring lebih dalem
    private void updateDeepMonitoring() {
        // Karena sensor fisik belum ada, kita simulasi pakai data lampu 5W milik user
        double volt = 0.0;
        double ampere = 0.0;
        double watt = 0.0;

        if (isConnected) {
            // Simulasi tegangan PLN yang naik turun sedikit (218 - 222V)
            volt = 220.0 + (random.nextDouble() * 4 - 2);
            if (isLampOn) {
                // Simulasi daya lampu 5 Watt
                watt = 5.0 + (random.nextDouble() * 0.4 - 0.2);
                ampere = watt / volt;

                // Tambah total kWh secara otomatis
                totalKwh += (watt * (2.0 / 3600.0)) / 1000.0;
                saveCurrentKwh();
            }
            tvEfficiency.setText(isLampOn ? "Status: Simulasi Lampu 5W Aktif" : "Status: Lampu Standby");
            tvEfficiency.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            // Jika OFFLINE, paksa semua nilai ke nol
            volt = 0.0;
            ampere = 0.0;
            watt = 0.0;
            tvEfficiency.setText("Status: PERANGKAT OFFLINE");
            tvEfficiency.setTextColor(Color.parseColor("#F44336"));
        }

        // UPDATE UI TEKS (PENTING)
        tvVoltDetail.setText(String.format(Locale.getDefault(), "%.1f V", volt));
        tvAmpereDetail.setText(String.format(Locale.getDefault(), "%.3f A", ampere));
        tvKwhToday.setText(String.format(Locale.getDefault(), "%.4f kWh", totalKwh));
        tvEstimasiBiaya.setText(currencyFormat.format(totalKwh * 1444.70));
        
        // Prediksi bulanan
        double monthlyPrediction = totalKwh * 30 * 1444.70; 
        tvPrediction.setText(currencyFormat.format(monthlyPrediction));
        
        addEntry((float) watt);
    }

    // nambahin titik baru ke grafik
    private void addEntry(float val) {
        if (lineChart == null) return;

        entries.add(new Entry(xValue++, val));
        if (entries.size() > 30) entries.remove(0); // Simpan 30 titik data

        LineDataSet dataSet = new LineDataSet(entries, "Daya (Watt)");
        
        // Styling grafik agar "Mudah Dibaca" & "Modern"
        dataSet.setColor(Color.parseColor("#FFD54F")); // Warna Kuning (identik dengan lampu)
        dataSet.setLineWidth(3f); // Garis lebih tebal
        dataSet.setDrawCircles(false); // Hilangkan bulatan biar smooth
        dataSet.setDrawValues(false); // Hilangkan angka di atas garis biar gak rame
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Garis melengkung halus
        
        // Efek isi di bawah garis (Gradient-like)
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#FFD54F"));
        dataSet.setFillAlpha(30); // Transparan tipis

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        
        // Update grafik tanpa kedip
        lineChart.notifyDataSetChanged();
        lineChart.setVisibleXRangeMaximum(20); // Tampilkan 20 data sekaligus
        lineChart.moveViewToX(xValue);
        lineChart.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}