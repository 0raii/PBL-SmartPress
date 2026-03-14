package com.example.itproyek2;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class UserGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_guide);

        ImageView btnBack = findViewById(R.id.btnBackGuide);
        btnBack.setOnClickListener(v -> finish());
    }
}
