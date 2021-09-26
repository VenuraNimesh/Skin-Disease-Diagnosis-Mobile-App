package com.example.skindiseasediagnosis;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class TreatmentsActivity extends AppCompatActivity {

    private TextView homeButton, helpButton,textETreatment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_treatments);

        homeButton = findViewById(R.id.homeButton);
        helpButton = findViewById(R.id.helpButton);

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TreatmentsActivity.this, MainActivity.class));
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TreatmentsActivity.this, HelpActivity.class));
            }
        });
    }
}