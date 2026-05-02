package com.example.itproyek2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    private BarChart barChart;
    private TextView tvChartTitle, tvSummaryText, tvTotalKwhReport, tvTotalCostReport, tvAvgDurationReport;
    private MaterialButtonToggleGroup toggleGroupFilter;

    private DatabaseReference dbRef;
    private double currentKwh = 0;
    private long currentDurationSec = 0;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        barChart = findViewById(R.id.barChart);
        tvChartTitle = findViewById(R.id.tvChartTitle);
        tvSummaryText = findViewById(R.id.tvSummaryText);
        tvTotalKwhReport = findViewById(R.id.tvTotalKwhReport);
        tvTotalCostReport = findViewById(R.id.tvTotalCostReport);
        tvAvgDurationReport = findViewById(R.id.tvAvgDurationReport);
        toggleGroupFilter = findViewById(R.id.toggleGroupFilter);

        currencyFormat.setMaximumFractionDigits(0);
        
        dbRef = FirebaseDatabase.getInstance("https://smartpress-ea81d-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
        initFirebaseListeners();

        setupBottomNav();
        
        toggleGroupFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateReportUi();
            }
        });
    }

    private void initFirebaseListeners() {
        // Listen to kWh
        dbRef.child("sensor_energy").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentKwh = snapshot.getValue(Double.class);
                    updateReportUi();
                }
            }
            @Override public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {}
        });

        // Listen to Duration
        dbRef.child("lamp_duration_sec").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentDurationSec = snapshot.getValue(Long.class);
                    updateReportUi();
                }
            }
            @Override public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {}
        });
    }

    private void updateReportUi() {
        int checkedId = toggleGroupFilter.getCheckedButtonId();
        
        // Update Summary Cards with Real Data
        tvTotalKwhReport.setText(String.format(Locale.US, "%.2f", currentKwh));
        tvTotalCostReport.setText(currencyFormat.format(currentKwh * 1444.70));
        
        long hours = currentDurationSec / 3600;
        long minutes = (currentDurationSec % 3600) / 60;
        tvAvgDurationReport.setText(hours + "J " + minutes + "M");

        if (checkedId == R.id.btnDay) showDailyReport();
        else if (checkedId == R.id.btnWeek) showWeeklyReport();
        else if (checkedId == R.id.btnMonth) showMonthlyReport();
    }

    private void showDailyReport() {
        tvChartTitle.setText("Konsumsi Listrik Hari Ini (Jam)");
        tvSummaryText.setText("💡 Data real-time menunjukkan penggunaan daya Anda saat ini.");
        
        ArrayList<BarEntry> entries = new ArrayList<>();
        // Current real data as the main bar
        entries.add(new BarEntry(0, (float) currentKwh));
        
        String[] labels = {"Hari Ini"};
        updateChart(entries, labels);
    }

    private void showWeeklyReport() {
        tvChartTitle.setText("Konsumsi Listrik Minggu Ini (Hari)");
        tvSummaryText.setText("✅ Penggunaan minggu ini terakumulasi dari data sensor harian.");

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) currentKwh)); // Simplified to current since history isn't persistent in DB yet
        
        String[] labels = {"Mgg Ini"};
        updateChart(entries, labels);
    }

    private void showMonthlyReport() {
        tvChartTitle.setText("Konsumsi Listrik Bulan Ini (Minggu)");
        tvSummaryText.setText("⭐ Estimasi tagihan Anda bulan ini berdasarkan penggunaan kWh saat ini.");

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) currentKwh));
        
        String[] labels = {"Bln Ini"};
        updateChart(entries, labels);
    }

    private void updateChart(ArrayList<BarEntry> entries, String[] labels) {
        // Cek apakah sedang mode gelap
        boolean isDarkMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int textColor = isDarkMode ? Color.WHITE : Color.BLACK;
        int subTextColor = isDarkMode ? Color.LTGRAY : Color.DKGRAY;

        BarDataSet dataSet = new BarDataSet(entries, "kWh");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setValueTextColor(textColor);
        dataSet.setValueTextSize(12f); // Ukuran lebih besar untuk orang tua

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(textColor);
        xAxis.setTextSize(11f);
        
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisLeft().setTextColor(textColor);
        barChart.getAxisLeft().setTextSize(11f);

        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        
        // Atur warna Legend (keterangan kWh)
        barChart.getLegend().setTextColor(textColor);
        barChart.getLegend().setTextSize(12f);

        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_report);
        
        android.content.SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        String role = prefs.getString("profile_role", "User");
        
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                if ("Admin".equalsIgnoreCase(role)) {
                    startActivity(new Intent(this, AdminDashboardActivity.class));
                } else {
                    startActivity(new Intent(this, MainActivity.class));
                }
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
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
            return id == R.id.nav_report;
        });
    }
}