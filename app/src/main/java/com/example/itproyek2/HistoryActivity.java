package com.example.itproyek2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private BarChart barChart;
    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList;
    private TextView tvHistoryKwhToday, tvHistoryKwhWeek;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        barChart = findViewById(R.id.barChart);
        rvHistory = findViewById(R.id.rvHistory);
        tvHistoryKwhToday = findViewById(R.id.tvHistoryKwhToday);
        tvHistoryKwhWeek = findViewById(R.id.tvHistoryKwhWeek);

        setupBarChart();
        loadHistoryData();
        setupBottomNav();
    }

    private void loadHistoryData() {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        String rawData = prefs.getString("history_data", "");
        String currentKwh = prefs.getString("total_kwh", "0.45");

        // Update Summary Energi
        tvHistoryKwhToday.setText("Hari Ini : " + currentKwh + " kWh");
        tvHistoryKwhWeek.setText("Minggu : 3,2 kWh"); // Statis sebagai benchmark

        historyList = new ArrayList<>();
        
        if (!rawData.isEmpty()) {
            String[] entries = rawData.split(";");
            // Ambil 10 aktivitas terakhir
            int count = 0;
            for (int i = entries.length - 1; i >= 0 && count < 10; i--) {
                String[] parts = entries[i].split("\\|");
                if (parts.length >= 3) {
                    String title = parts[0];
                    String time = parts[1];
                    int iconType = Integer.parseInt(parts[2]);
                    int iconRes = (iconType == 1) ? R.drawable.bulb_glow : R.drawable.ic_history;
                    
                    historyList.add(new HistoryItem(title, time, iconRes));
                    count++;
                }
            }
        } else {
            // Data Dummy jika kosong
            historyList.add(new HistoryItem("Belum ada aktivitas", "-", R.drawable.ic_history));
        }

        adapter = new HistoryAdapter(historyList);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);
    }

    private void setupBarChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 0.8f));
        entries.add(new BarEntry(1, 1.8f));
        entries.add(new BarEntry(2, 2.1f));
        entries.add(new BarEntry(3, 2.6f));
        entries.add(new BarEntry(4, 3.5f));
        entries.add(new BarEntry(5, 4.2f));

        BarDataSet dataSet = new BarDataSet(entries, "Penggunaan");
        dataSet.setColor(Color.parseColor("#FFC107"));
        dataSet.setValueTextColor(Color.WHITE);

        barChart.setData(new BarData(dataSet));
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setTextColor(Color.WHITE);
        
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"6", "12", "15", "18", "21", "24"}));
        
        barChart.invalidate();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavHistory);
        bottomNav.setSelectedItemId(R.id.nav_history);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return id == R.id.nav_history;
        });
    }
}
