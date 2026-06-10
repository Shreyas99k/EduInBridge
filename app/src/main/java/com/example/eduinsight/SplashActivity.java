package com.example.eduinsight;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(this::checkSessionAndRedirect, 2000);
    }

    private void checkSessionAndRedirect() {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);

        if (pref.contains("user_id")) {
            String role = pref.getString("role", "").trim().toLowerCase();

            Intent intent;
            if (role.equals("teacher")) {
                intent = new Intent(SplashActivity.this, TeacherDashboardActivity.class);
            } else if (role.equals("student")) {
                intent = new Intent(SplashActivity.this, MainStudentDashboardActivity.class);
            } else if (role.equals("institution")) {
                intent = new Intent(SplashActivity.this, InstitutionDashboardActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
