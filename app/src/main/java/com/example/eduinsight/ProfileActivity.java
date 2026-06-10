package com.example.eduinsight;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {

    private TextView txtProfileName, txtProfileEmail, txtProfileMobile, txtProfileStatus;
    private View btnChangePassword, layoutCommunication;
    private MaterialButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }

        txtProfileName = findViewById(R.id.txtProfileName);
        txtProfileEmail = findViewById(R.id.txtProfileEmail);
        txtProfileMobile = findViewById(R.id.txtProfileMobile);
        txtProfileStatus = findViewById(R.id.txtProfileStatus);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        layoutCommunication = findViewById(R.id.layoutCommunication);
        btnLogout = findViewById(R.id.btnLogout);

        // Load initial data from session
        loadLocalSession();
        
        // Fetch live data from server to fix the "Not Provided" issue
        fetchProfileData();

        if (layoutCommunication != null) {
            layoutCommunication.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        }

        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void loadLocalSession() {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        txtProfileName.setText(pref.getString("name", "User"));
        txtProfileEmail.setText(pref.getString("email", "N/A"));
        txtProfileMobile.setText(pref.getString("mobile", "Loading..."));
        txtProfileStatus.setText(pref.getString("role", "Student").toUpperCase());
    }

    private void fetchProfileData() {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int userId = pref.getInt("user_id", -1);
        if (userId == -1) return;

        String url = "http://10.0.2.2/eduinsight_api/get_user_profile.php?user_id=" + userId;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optString("status").equals("success")) {
                            String mobile = json.optString("mobile");
                            String name = json.optString("name");
                            String email = json.optString("email");
                            String role = json.optString("role");

                            txtProfileName.setText(name);
                            txtProfileEmail.setText(email);
                            txtProfileMobile.setText((mobile == null || mobile.isEmpty() || mobile.equalsIgnoreCase("null")) ? "Not Provided" : mobile);
                            txtProfileStatus.setText(role.toUpperCase());

                            // Update session with correct values
                            pref.edit()
                                .putString("mobile", (mobile == null || mobile.isEmpty() || mobile.equalsIgnoreCase("null")) ? "Not Provided" : mobile)
                                .putString("name", name)
                                .putString("email", email)
                                .apply();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, error -> Toast.makeText(this, "Server Connection Error", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
