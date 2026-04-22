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

import java.util.ArrayList;

public class ReportActivity extends AppCompatActivity {

    private BarChart barChart;
    private TextView tvChartTitle, tvSummaryText, tvTotalKwhReport, tvTotalCostReport, tvAvgDurationReport;
    private MaterialButtonToggleGroup toggleGroupFilter;

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

        setupBottomNav();
        
        // Default: Harian
        showDailyReport();

        toggleGroupFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnDay) showDailyReport();
                else if (checkedId == R.id.btnWeek) showWeeklyReport();
                else if (checkedId == R.id.btnMonth) showMonthlyReport();
            }
        });
    }

    private void showDailyReport() {
        tvChartTitle.setText("Konsumsi Listrik Hari Ini (Jam)");
        tvSummaryText.setText("💡 Penggunaan stabil di jam sibuk (19:00 - 21:00). Tidak ada lonjakan daya.");
        
        tvTotalKwhReport.setText("4.0");
        tvTotalCostReport.setText("Rp 5k");
        tvAvgDurationReport.setText("8 Jam");

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 0.5f));
        entries.add(new BarEntry(1, 0.8f));
        entries.add(new BarEntry(2, 0.6f));
        entries.add(new BarEntry(3, 1.2f));
        entries.add(new BarEntry(4, 0.9f));
        
        String[] labels = {"17:00", "18:00", "19:00", "20:00", "21:00"};
        updateChart(entries, labels);
    }

    private void showWeeklyReport() {
        tvChartTitle.setText("Konsumsi Listrik Minggu Ini (Hari)");
        tvSummaryText.setText("✅ Bagus! Penggunaan listrik minggu ini turun 12% dibandingkan minggu lalu.");

        tvTotalKwhReport.setText("30.5");
        tvTotalCostReport.setText("Rp 45k");
        tvAvgDurationReport.setText("7.5 Jam");

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 4.5f));
        entries.add(new BarEntry(1, 5.2f));
        entries.add(new BarEntry(2, 3.8f));
        entries.add(new BarEntry(3, 4.1f));
        entries.add(new BarEntry(4, 5.5f));
        entries.add(new BarEntry(5, 6.2f));
        entries.add(new BarEntry(6, 4.9f));

        String[] labels = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
        updateChart(entries, labels);
    }

    private void showMonthlyReport() {
        tvChartTitle.setText("Konsumsi Listrik Bulan Ini (Minggu)");
        tvSummaryText.setText("⭐ Estimasi tagihan bulan ini adalah Rp 180.000. Kamu menghemat Rp 15k bulan ini.");

        tvTotalKwhReport.setText("120.2");
        tvTotalCostReport.setText("Rp 180k");
        tvAvgDurationReport.setText("6.8 Jam");

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 25f));
        entries.add(new BarEntry(1, 28f));
        entries.add(new BarEntry(2, 22f));
        entries.add(new BarEntry(3, 26f));

        String[] labels = {"Mgg 1", "Mgg 2", "Mgg 3", "Mgg 4"};
        updateChart(entries, labels);
    }

    private void updateChart(ArrayList<BarEntry> entries, String[] labels) {
        BarDataSet dataSet = new BarDataSet(entries, "kWh");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setValueTextColor(Color.GRAY);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_report);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
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