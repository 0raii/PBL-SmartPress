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
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
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

        tvHistoryKwhToday.setText("Hari Ini : " + currentKwh + " kWh");
        tvHistoryKwhWeek.setText("Minggu : 3,2 kWh");

        historyList = new ArrayList<>();
        if (!rawData.isEmpty()) {
            String[] entries = rawData.split(";");
            int count = 0;
            // Ambil 10 aktivitas terbaru
            for (int i = entries.length - 1; i >= 0 && count < 10; i--) {
                String[] parts = entries[i].split("\\|");
                if (parts.length >= 2) {
                    String title = parts[0];
                    String time = parts[1];
                    
                    int iconRes = R.drawable.ic_history; // Default
                    
                    // Logika penentuan ikon berdasarkan kata kunci di pesan log
                    String upperTitle = title.toUpperCase();
                    if (upperTitle.contains("HIDUP")) {
                        iconRes = R.drawable.ic_lamp_on;
                    } else if (upperTitle.contains("MATI")) {
                        iconRes = R.drawable.ic_lamp_off;
                    } else if (upperTitle.contains("OTOMATIS")) {
                        iconRes = R.drawable.ic_star_on;
                    } else if (upperTitle.contains("MANUAL")) {
                        iconRes = R.drawable.ic_settings;
                    }
                    
                    historyList.add(new HistoryItem(title, time, iconRes));
                    count++;
                }
            }
        } else {
            historyList.add(new HistoryItem("Belum ada aktivitas", "-", R.drawable.ic_history));
        }

        adapter = new HistoryAdapter(historyList);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);
    }

    private void setupBarChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        // Menggunakan Menit agar lebih mudah dipahami
        entries.add(new BarEntry(0, 45f));   // 06:00
        entries.add(new BarEntry(1, 120f));  // 12:00 -> 2 Jam
        entries.add(new BarEntry(2, 90f));   // 15:00
        entries.add(new BarEntry(3, 180f));  // 18:00 -> 3 Jam
        entries.add(new BarEntry(4, 240f));  // 21:00 -> 4 Jam
        entries.add(new BarEntry(5, 300f));  // 24:00 -> 5 Jam

        BarDataSet dataSet = new BarDataSet(entries, "Durasi Menyala (Menit)");
        dataSet.setColor(Color.parseColor("#FFC107"));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(9f);
        
        // Formatter: Jika >= 60 menit, tampilkan (X Jam) disampingnya
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int minutes = (int) value;
                if (minutes >= 60) {
                    int hours = minutes / 60;
                    return minutes + "m (" + hours + "j)";
                }
                return minutes + "m";
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);

        // Pengaturan Tampilan Grafik
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setTextColor(Color.WHITE);
        barChart.getAxisRight().setEnabled(false);
        
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + " m";
            }
        });

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"06:00", "12:00", "15:00", "18:00", "21:00", "24:00"}));
        
        barChart.animateY(1000);
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
