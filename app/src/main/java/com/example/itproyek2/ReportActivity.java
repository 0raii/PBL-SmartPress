package com.example.itproyek2;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    private BarChart barChart;
    private TextView tvChartTitle, tvTotalKwhReport, tvTotalCostReport, tvAvgDurationReport;
    private TextView tvAutoLogCount, tvCountHidup, tvCountMati, tvEmptyLog, tvPageInfo;
    private MaterialButton btnDownloadPdf, btnPrevPage, btnNextPage;
    private MaterialButtonToggleGroup toggleGroupFilter;
    private android.widget.LinearLayout layoutPagination;
    private RecyclerView rvAutoLogs;
    private AutoLogAdapter autoLogAdapter;
    private List<AutoLogModel> autoLogList = new ArrayList<>();

    private int currentPage = 0;
    private final int PAGE_SIZE = 5;
    private long currentStartTime = 0;
    private long currentEndTime = System.currentTimeMillis();

    private DatabaseReference dbRef;
    private DatabaseHelper dbHelper;
    private double currentKwh = 0;
    private long currentDurationSec = 0;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        barChart = findViewById(R.id.barChart);
        tvChartTitle = findViewById(R.id.tvChartTitle);
        tvTotalKwhReport = findViewById(R.id.tvTotalKwhReport);
        tvTotalCostReport = findViewById(R.id.tvTotalCostReport);
        tvAvgDurationReport = findViewById(R.id.tvAvgDurationReport);
        tvAutoLogCount = findViewById(R.id.tvAutoLogCount);
        tvCountHidup = findViewById(R.id.tvCountHidup);
        tvCountMati = findViewById(R.id.tvCountMati);
        tvEmptyLog = findViewById(R.id.tvEmptyLog);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        layoutPagination = findViewById(R.id.layoutPagination);
        toggleGroupFilter = findViewById(R.id.toggleGroupFilter);
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf);
        rvAutoLogs = findViewById(R.id.rvAutoLogs);

        dbHelper = new DatabaseHelper(this);
        currencyFormat.setMaximumFractionDigits(0);
        
        dbRef = FirebaseDatabase.getInstance("https://smartpress-ea81d-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
        
        autoLogAdapter = new AutoLogAdapter(autoLogList);
        rvAutoLogs.setLayoutManager(new LinearLayoutManager(this));
        rvAutoLogs.setAdapter(autoLogAdapter);

        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                loadAutoLogs(currentStartTime, currentEndTime);
            }
        });

        btnNextPage.setOnClickListener(v -> {
            currentPage++;
            loadAutoLogs(currentStartTime, currentEndTime);
        });

        initFirebaseListeners();
        setupBottomNav();
        
        toggleGroupFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateReportUi();
            }
        });

        btnDownloadPdf.setOnClickListener(v -> generatePdfReport());
    }

    private void generatePdfReport() {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("profile_id", -1);
        if (userId == -1) return;

        int checkedId = toggleGroupFilter.getCheckedButtonId();
        String period = "Harian";
        long startTime = 0;
        long endTime = System.currentTimeMillis();

        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (checkedId == R.id.btnDay) {
            period = "Harian";
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            startTime = cal.getTimeInMillis();
        } else if (checkedId == R.id.btnWeek) {
            period = "Mingguan";
            cal.add(java.util.Calendar.DAY_OF_YEAR, -7);
            startTime = cal.getTimeInMillis();
        } else if (checkedId == R.id.btnMonth) {
            period = "Bulanan";
            cal.add(java.util.Calendar.MONTH, -1);
            startTime = cal.getTimeInMillis();
        }

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(18);
        canvas.drawText("LAPORAN MONITORING SMARTPRESS (" + period.toUpperCase() + ")", 100, 50, titlePaint);

        paint.setTextSize(12);
        canvas.drawText("Dicetak pada: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()), 50, 80, paint);
        canvas.drawText("Ringkasan Penggunaan:", 50, 110, titlePaint);
        canvas.drawText("- Total Penggunaan Energi: " + String.format("%.2f", currentKwh) + " kWh", 70, 130, paint);
        canvas.drawText("- Estimasi Biaya: " + currencyFormat.format(currentKwh * 1444.70), 70, 150, paint);
        
        int totalLogs = dbHelper.getAutoLogSummary(userId, startTime, endTime);
        canvas.drawText("- Total Aksi Otomatis: " + totalLogs + " kali", 70, 170, paint);

        canvas.drawText("LOG AKTIVITAS OTOMATIS (SENSOR)", 50, 210, titlePaint);
        int y = 240;
        canvas.drawText("Waktu", 50, y, titlePaint);
        canvas.drawText("Aksi", 200, y, titlePaint);
        canvas.drawText("Cahaya (Lm)", 300, y, titlePaint);
        canvas.drawText("Daya (Watt)", 450, y, titlePaint);
        
        canvas.drawLine(50, y+5, 550, y+5, paint);
        y += 25;

        Cursor cursor = dbHelper.getFilteredAutoLogs(userId, startTime, endTime);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        
        if (cursor.moveToFirst()) {
            do {
                if (y > 780) {
                    pdfDocument.finishPage(page);
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50;
                }
                long ts = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TIMESTAMP));
                String action = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ACTION));
                int lumen = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LUMEN));
                double watt = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WATT));

                canvas.drawText(sdf.format(new Date(ts)), 50, y, paint);
                canvas.drawText(action, 200, y, paint);
                canvas.drawText(String.valueOf(lumen) + " Lm", 300, y, paint);
                canvas.drawText(String.format("%.2f", watt) + " W", 450, y, paint);
                
                y += 20;
            } while (cursor.moveToNext());
        }
        cursor.close();
        pdfDocument.finishPage(page);

        String fileName = "Laporan_SmartPress_" + System.currentTimeMillis() + ".pdf";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        
        Uri collection;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        } else {
            collection = MediaStore.Files.getContentUri("external");
        }

        Uri uri = getContentResolver().insert(collection, values);

        try {
            if (uri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    pdfDocument.writeTo(outputStream);
                    outputStream.close();
                    Toast.makeText(this, "Laporan PDF berhasil diunduh ke folder Download", Toast.LENGTH_LONG).show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal mengunduh laporan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        pdfDocument.close();
    }

    private void loadAutoLogs(long startTime, long endTime) {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("profile_id", -1);
        if (userId == -1) return;

        currentStartTime = startTime;
        currentEndTime = endTime;
        autoLogList.clear();

        // 1. Hitung total untuk summary (Tetap seluruh data)
        int countHidup = 0;
        int countMati = 0;
        Cursor totalCursor = dbHelper.getFilteredAutoLogs(userId, startTime, endTime);
        int totalData = totalCursor.getCount();
        if (totalCursor.moveToFirst()) {
            do {
                String action = totalCursor.getString(totalCursor.getColumnIndexOrThrow(DatabaseHelper.COL_ACTION));
                if ("HIDUP".equalsIgnoreCase(action)) countHidup++;
                else if ("MATI".equalsIgnoreCase(action)) countMati++;
            } while (totalCursor.moveToNext());
        }
        totalCursor.close();

        tvCountHidup.setText("Hidup: " + countHidup);
        tvCountMati.setText("Mati: " + countMati);

        // 2. Load data per halaman (Pagination)
        int offset = currentPage * PAGE_SIZE;
        Cursor pageCursor = dbHelper.getPagedAutoLogs(userId, startTime, endTime, PAGE_SIZE, offset);
        
        if (pageCursor.moveToFirst()) {
            do {
                long ts = pageCursor.getLong(pageCursor.getColumnIndexOrThrow(DatabaseHelper.COL_TIMESTAMP));
                String action = pageCursor.getString(pageCursor.getColumnIndexOrThrow(DatabaseHelper.COL_ACTION));
                int lumen = pageCursor.getInt(pageCursor.getColumnIndexOrThrow(DatabaseHelper.COL_LUMEN));
                double watt = pageCursor.getDouble(pageCursor.getColumnIndexOrThrow(DatabaseHelper.COL_WATT));
                autoLogList.add(new AutoLogModel(ts, action, lumen, watt));
            } while (pageCursor.moveToNext());
        }
        pageCursor.close();

        // 3. Update UI Navigasi
        if (totalData == 0) {
            tvEmptyLog.setVisibility(android.view.View.VISIBLE);
            rvAutoLogs.setVisibility(android.view.View.GONE);
            layoutPagination.setVisibility(android.view.View.GONE);
        } else {
            tvEmptyLog.setVisibility(android.view.View.GONE);
            rvAutoLogs.setVisibility(android.view.View.VISIBLE);
            layoutPagination.setVisibility(android.view.View.VISIBLE);
            
            int totalPages = (int) Math.ceil((double) totalData / PAGE_SIZE);
            tvPageInfo.setText("Hal " + (currentPage + 1) + " dari " + totalPages);
            
            btnPrevPage.setEnabled(currentPage > 0);
            btnNextPage.setEnabled(offset + PAGE_SIZE < totalData);
            
            btnPrevPage.setAlpha(btnPrevPage.isEnabled() ? 1.0f : 0.5f);
            btnNextPage.setAlpha(btnNextPage.isEnabled() ? 1.0f : 0.5f);
        }

        autoLogAdapter.notifyDataSetChanged();
    }

    private void initFirebaseListeners() {
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
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("profile_id", -1);
        if (userId == -1) return;

        int checkedId = toggleGroupFilter.getCheckedButtonId();
        currentPage = 0; // Reset ke halaman 1 setiap ganti filter
        tvTotalKwhReport.setText(String.format(Locale.US, "%.2f", currentKwh));
        tvTotalCostReport.setText(currencyFormat.format(currentKwh * 1444.70));
        
        long hours = currentDurationSec / 3600;
        long minutes = (currentDurationSec % 3600) / 60;
        tvAvgDurationReport.setText(hours + "J " + minutes + "M");

        long startTime = 0;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (checkedId == R.id.btnDay) {
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            startTime = cal.getTimeInMillis();
        } else if (checkedId == R.id.btnWeek) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -7);
            startTime = cal.getTimeInMillis();
        } else if (checkedId == R.id.btnMonth) {
            cal.add(java.util.Calendar.MONTH, -1);
            startTime = cal.getTimeInMillis();
        }
        
        int autoCount = dbHelper.getAutoLogSummary(userId, startTime, System.currentTimeMillis());
        tvAutoLogCount.setText(String.valueOf(autoCount));
        loadAutoLogs(startTime, System.currentTimeMillis());

        if (checkedId == R.id.btnDay) showDailyReport();
        else if (checkedId == R.id.btnWeek) showWeeklyReport();
        else if (checkedId == R.id.btnMonth) showMonthlyReport();
    }

    private void showDailyReport() {
        tvChartTitle.setText("Konsumsi Listrik Hari Ini (Jam)");
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) currentKwh));
        updateChart(entries, new String[]{"Hari Ini"});
    }

    private void showWeeklyReport() {
        tvChartTitle.setText("Konsumsi Listrik Minggu Ini (Hari)");
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) currentKwh));
        updateChart(entries, new String[]{"Mgg Ini"});
    }

    private void showMonthlyReport() {
        tvChartTitle.setText("Konsumsi Listrik Bulan Ini (Minggu)");
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) currentKwh));
        updateChart(entries, new String[]{"Bln Ini"});
    }

    private void updateChart(ArrayList<BarEntry> entries, String[] labels) {
        boolean isDarkMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int textColor = isDarkMode ? Color.WHITE : Color.BLACK;

        BarDataSet dataSet = new BarDataSet(entries, "kWh");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setValueTextColor(textColor);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(textColor);
        
        barChart.getAxisLeft().setTextColor(textColor);
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setTextColor(textColor);
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
                if ("Admin".equalsIgnoreCase(role)) startActivity(new Intent(this, AdminDashboardActivity.class));
                else startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            }
            return id == R.id.nav_report;
        });
    }
}