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

public class DetailActivity extends AppCompatActivity {

    private LineChart lineChart;
    private TextView tvPrediction, tvEfficiency;
    private TextView tvVoltDetail, tvAmpereDetail, tvDurasiNyala;
    private TextView tvKwhToday, tvEstimasiBiaya;
    private com.google.android.material.card.MaterialCardView cardProtectionAlert;
    private TextView tvProtectionTitle, tvProtectionDesc;
    
    private final ArrayList<Entry> entries = new ArrayList<>();
    private int xValue = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final DecimalFormat df = new DecimalFormat("#.##");
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    
    private long startTime = System.currentTimeMillis();
    private double totalKwh;

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
        // lineChart = findViewById(R.id.lineChart); // sembunyiin dulu cuy

        loadCurrentKwh();
        // setupChart(); // grafik diumpetin dulu sementara
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
        
        int textColor = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES ? Color.WHITE : Color.BLACK;
        lineChart.setNoDataTextColor(textColor);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextColor(textColor);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        lineChart.getAxisLeft().setTextColor(textColor);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#333333"));
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setTextColor(textColor);
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

    // update monitoring lebih dalem
    private void updateDeepMonitoring() {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        boolean isLampOn = prefs.getBoolean("lamp_status", false);
        boolean isConnected = prefs.getBoolean("is_connected", true);

        double volt = isConnected ? (215 + random.nextDouble() * 15) : 0;
        double ampere = (isConnected && isLampOn) ? (0.2 + random.nextDouble() * 0.2) : 0;
        double watt = volt * ampere;
        
        addEntry((float) watt);

        tvVoltDetail.setText(String.format(Locale.getDefault(), "%.1f V", volt));
        tvAmpereDetail.setText(String.format(Locale.getDefault(), "%.2f A", ampere));

        // Logic Proteksi di Detail
        if (isLampOn) {
            if (volt > 240.0) {
                cardProtectionAlert.setVisibility(View.VISIBLE);
                tvProtectionTitle.setText("TEGANGAN TINGGI!");
                tvProtectionDesc.setText("Voltase terdeteksi " + df.format(volt) + "V. Sistem mematikan lampu.");
                prefs.edit().putBoolean("lamp_status", false).apply();
            } else if (ampere > 0.5) {
                cardProtectionAlert.setVisibility(View.VISIBLE);
                tvProtectionTitle.setText("ARUS BERLEBIH!");
                tvProtectionDesc.setText("Arus terdeteksi " + df.format(ampere) + "A. Sistem mematikan lampu.");
                prefs.edit().putBoolean("lamp_status", false).apply();
            } else {
                cardProtectionAlert.setVisibility(View.GONE);
            }
        } else {
            // Cek apakah baru saja dimatikan oleh proteksi (misal via MainActivity)
            // Di sini kita cuma sembunyiin kalo emang normal off
            if (volt <= 240.0 && ampere <= 0.5) {
                cardProtectionAlert.setVisibility(View.GONE);
            }
        }
        
        if (isLampOn && isConnected) {
            long elapsedMillis = System.currentTimeMillis() - startTime;
            int seconds = (int) (elapsedMillis / 1000) % 60;
            int minutes = (int) ((elapsedMillis / (1000 * 60)) % 60);
            int hours = (int) ((elapsedMillis / (1000 * 60 * 60)) % 24);
            tvDurasiNyala.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));

            totalKwh += (watt / 1000.0) * (2.0 / 3600.0);
            saveCurrentKwh();
        } else if (!isLampOn) {
            startTime = System.currentTimeMillis();
            tvDurasiNyala.setText("00:00:00");
        }

        tvKwhToday.setText(df.format(totalKwh) + " kWh");
        tvEstimasiBiaya.setText(currencyFormat.format(totalKwh * 1444.70));

        double monthlyPrediction = totalKwh * 30 * 1444.70; 
        tvPrediction.setText(currencyFormat.format(monthlyPrediction));

        if (watt > 0 && watt < 50) {
            tvEfficiency.setText("status: hemat bgt (eco mode)");
        } else if (watt >= 50) {
            tvEfficiency.setText("status: pemakaian biasa");
        } else {
            tvEfficiency.setText("status: standby/off");
        }
    }

    // nambahin titik baru ke grafik
    private void addEntry(float val) {
        if (lineChart == null) return; // cegah crash kalo grafik lg diumpetin

        entries.add(new Entry(xValue++, val));
        if (entries.size() > 20) entries.remove(0);

        LineDataSet dataSet = new LineDataSet(entries, "Daya (Watt)");
        dataSet.setColor(Color.parseColor("#D0BCFF"));
        dataSet.setCircleColor(Color.WHITE);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#D0BCFF"));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.notifyDataSetChanged();
        lineChart.setVisibleXRangeMaximum(10);
        lineChart.moveViewToX(xValue);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}