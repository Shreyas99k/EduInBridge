package com.example.eduinsight;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

public class InstitutionDashboardActivity extends AppCompatActivity {

    private static final String TAG = "InstitutionDashboard";
    private TextView txtTotalStudents, txtTotalTeachers, txtInstitutionName;
    private MaterialCardView btnManageTeachers, btnPendingApprovals, btnManageDepartments, btnSendNotification, btnInstitutionProfile;
    private MaterialButton btnUploadResources;
    private String institutionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_institution_dashboard);

        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int id = pref.getInt("user_id", -1);
        String name = pref.getString("name", "Institution");
        
        if (id == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        institutionId = String.valueOf(id);

        txtInstitutionName = findViewById(R.id.txtInstitutionName);
        if (txtInstitutionName != null) {
            txtInstitutionName.setText(name);
        }

        txtTotalStudents = findViewById(R.id.txtTotalStudents);
        txtTotalTeachers = findViewById(R.id.txtTotalTeachers);
        btnManageTeachers = findViewById(R.id.btnManageTeachers);
        btnPendingApprovals = findViewById(R.id.btnPendingApprovalsGrid);
        btnManageDepartments = findViewById(R.id.btnManageDepartments);
        btnSendNotification = findViewById(R.id.btnSendNotification);
        
        // New Profile triggers
        btnInstitutionProfile = findViewById(R.id.btnInstitutionProfile);
        
        btnUploadResources = findViewById(R.id.btnUploadResources);

        setupClickListeners();
        fetchInstitutionStats();
    }

    private void setupClickListeners() {
        if (btnManageTeachers != null) {
            btnManageTeachers.setOnClickListener(v -> startActivity(new Intent(this, ManageMentorsActivity.class)));
        }
        
        if (btnPendingApprovals != null) {
            btnPendingApprovals.setOnClickListener(v -> startActivity(new Intent(this, TeacherApprovalsActivity.class)));
        }

        if (btnManageDepartments != null) {
            btnManageDepartments.setOnClickListener(v -> startActivity(new Intent(this, ManageCurriculumActivity.class)));
        }

        if (btnSendNotification != null) {
            btnSendNotification.setOnClickListener(v -> startActivity(new Intent(this, SendNotificationActivity.class)));
        }

        // Fixed Profile Click Listeners
        if (btnInstitutionProfile != null) {
            btnInstitutionProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }
        
        if (btnUploadResources != null) {
            btnUploadResources.setOnClickListener(v -> {
                Intent intent = new Intent(this, UploadActivity.class);
                startActivity(intent);
            });
        }
    }

    private void fetchInstitutionStats() {
        String url = "http://10.0.2.2/eduinsight_api/get_institution_stats.php?institution_id=" + institutionId;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.optString("status").equals("success")) {
                            txtTotalStudents.setText(String.valueOf(response.optInt("total_students", 0)));
                            txtTotalTeachers.setText(String.valueOf(response.optInt("total_teachers", 0)));
                            
                            if (response.has("institution_name") && txtInstitutionName != null) {
                                txtInstitutionName.setText(response.getString("institution_name"));
                            }
                        }
                    } catch (Exception e) { Log.e(TAG, "Stats parse error"); }
                }, error -> Log.e(TAG, "Network error"));
        Volley.newRequestQueue(this).add(request);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        fetchInstitutionStats();
    }
}
