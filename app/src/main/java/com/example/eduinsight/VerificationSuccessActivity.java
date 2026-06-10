package com.example.eduinsight;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class VerificationSuccessActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_success);

        findViewById(R.id.btnGoToDashboard).setOnClickListener(v -> {
            // Logic to redirect based on saved role
            String role = getSharedPreferences("UserSession", MODE_PRIVATE).getString("role", "");
            Intent intent;
            if ("teacher".equalsIgnoreCase(role)) {
                intent = new Intent(this, TeacherDashboardActivity.class);
            } else if ("institution".equalsIgnoreCase(role)) {
                intent = new Intent(this, InstitutionDashboardActivity.class);
            } else {
                intent = new Intent(this, MainStudentDashboardActivity.class);
            }
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }
}
