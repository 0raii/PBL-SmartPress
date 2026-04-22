package com.example.itproyek2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class UserGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Ambil preferensi tema
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("is_dark_theme", true);
        
        // Terapkan tema
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        // Paksa sistem menerapkan tema sebelum layout dipasang
        getDelegate().applyDayNight();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_guide);

        ImageView btnBack = findViewById(R.id.btnBackGuide);
        btnBack.setOnClickListener(v -> finish());
    }
}
