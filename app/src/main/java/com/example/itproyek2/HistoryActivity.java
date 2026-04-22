package com.example.itproyek2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList;
    private TextView tvHistoryKwhToday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        rvHistory = findViewById(R.id.rvHistory);
        tvHistoryKwhToday = findViewById(R.id.tvHistoryKwhToday);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        
        loadHistoryData();
        setupBottomNav();
    }

    private void loadHistoryData() {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        String rawData = prefs.getString("history_data", "");
        String currentKwh = prefs.getString("total_kwh", "0.45");

        tvHistoryKwhToday.setText(currentKwh + " kWh");

        historyList = new ArrayList<>();
        if (!rawData.isEmpty()) {
            String[] entries = rawData.split(";");
            // Ambil 20 log sistem terakhir
            for (int i = entries.length - 1; i >= 0 && (entries.length - i) <= 20; i--) {
                String[] parts = entries[i].split("\\|");
                if (parts.length >= 3) {
                    historyList.add(new HistoryItem(parts[0], parts[1], Integer.parseInt(parts[2])));
                }
            }
        }
        
        adapter = new HistoryAdapter(historyList);
        rvHistory.setAdapter(adapter);
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
            if (id == R.id.nav_report) {
                startActivity(new Intent(this, ReportActivity.class));
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
            return id == R.id.nav_history;
        });
    }
}