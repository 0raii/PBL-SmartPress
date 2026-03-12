package com.example.itproyek2;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Random;

public class DetailActivity extends AppCompatActivity {

    private LineChart lineChart;
    private final ArrayList<Entry> entries = new ArrayList<>();
    private int xValue = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbarDetail);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

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

        // Customize X Axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        // Customize Left Y Axis
        lineChart.getAxisLeft().setTextColor(Color.WHITE);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#333333"));

        // Disable Right Y Axis
        lineChart.getAxisRight().setEnabled(false);

        // Legend
        lineChart.getLegend().setTextColor(Color.WHITE);
    }

    private void startDataSimulation() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                addEntry();
                handler.postDelayed(this, 2000); // Update every 2 seconds
            }
        }, 1000);
    }

    private void addEntry() {
        float val = 50 + random.nextFloat() * 10; // Random watt between 50-60
        entries.add(new Entry(xValue++, val));

        // Keep only last 20 points
        if (entries.size() > 20) {
            entries.remove(0);
        }

        LineDataSet dataSet = new LineDataSet(entries, "Pemakaian (Watt)");
        dataSet.setColor(Color.parseColor("#D0BCFF")); // Purple Light
        dataSet.setCircleColor(Color.WHITE);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
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
