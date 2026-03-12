package com.example.itproyek2;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
    private TextView tvVoltDetail, tvAmpereDetail, tvSuhuESP, tvDurasiNyala;
    private TextView tvKwhToday, tvEstimasiBiaya;
    
    private final ArrayList<Entry> entries = new ArrayList<>();
    private int xValue = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final DecimalFormat df = new DecimalFormat("#.##");
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    
    private long startTime = System.currentTimeMillis();
    private double totalKwh = 0.45;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbarDetail);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize NumberFormat for no decimals in currency
        currencyFormat.setMaximumFractionDigits(0);

        // Initialize Views
        tvPrediction = findViewById(R.id.tvPrediction);
        tvEfficiency = findViewById(R.id.tvEfficiency);
        tvVoltDetail = findViewById(R.id.tvVoltDetail);
        tvAmpereDetail = findViewById(R.id.tvAmpereDetail);
        tvSuhuESP = findViewById(R.id.tvSuhuESP);
        tvDurasiNyala = findViewById(R.id.tvDurasiNyala);
        tvKwhToday = findViewById(R.id.tvKwhToday);
        tvEstimasiBiaya = findViewById(R.id.tvEstimasiBiaya);
        lineChart = findViewById(R.id.lineChart);

        setupChart();
        startDataSimulation();
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setBackgroundColor(Color.TRANSPARENT);
        lineChart.setNoDataText("Menunggu data...");
        lineChart.setNoDataTextColor(Color.WHITE);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        lineChart.getAxisLeft().setTextColor(Color.WHITE);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#333333"));
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setTextColor(Color.WHITE);
    }

    private void startDataSimulation() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateDeepMonitoring();
                handler.postDelayed(this, 2000); 
            }
        }, 1000);
    }

    private void updateDeepMonitoring() {
        // 1. Electric Simulation
        double volt = 218 + random.nextDouble() * 5;
        double watt = 45 + random.nextDouble() * 10;
        double ampere = watt / volt;
        
        addEntry((float) watt);

        // 2. Main Stats
        tvVoltDetail.setText(String.format(Locale.getDefault(), "%.1f V", volt));
        tvAmpereDetail.setText(String.format(Locale.getDefault(), "%.2f A", ampere));
        
        // 3. Duration
        long elapsedMillis = System.currentTimeMillis() - startTime;
        int seconds = (int) (elapsedMillis / 1000) % 60;
        int minutes = (int) ((elapsedMillis / (1000 * 60)) % 60);
        int hours = (int) ((elapsedMillis / (1000 * 60 * 60)) % 24);
        tvDurasiNyala.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));

        // 4. Energy & Cost
        totalKwh += (watt / 1000.0) * (2.0 / 3600.0);
        tvKwhToday.setText(df.format(totalKwh) + " kWh");
        tvEstimasiBiaya.setText(currencyFormat.format(totalKwh * 1444.70));

        // 5. Prediction (30 days estimate)
        double monthlyPrediction = totalKwh * 30 * 1444.70; 
        tvPrediction.setText(currencyFormat.format(monthlyPrediction));

        // 6. Device health
        float suhu = 32 + random.nextFloat() * 6;
        tvSuhuESP.setText(String.format(Locale.getDefault(), "%.1f°C", suhu));
        
        // 7. Efficiency Analysis
        if (watt < 50) {
            tvEfficiency.setText("Status: Sangat Efisien (Eco Mode)");
        } else {
            tvEfficiency.setText("Status: Pemakaian Normal");
        }
    }

    private void addEntry(float val) {
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
