package com.example.eduinsight;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class TeacherDashboardActivity extends AppCompatActivity {

    private TextView txtSolved, txtPending, txtRating, txtStatusLabel, txtTeacherName, txtInstitutionDept, txtAlertTitle, txtAlertSubtitle;
    private MaterialCardView cardQueue, cardChat, cardHistory, cardProfile, cardAi, cardDoubtAlert;
    private MaterialSwitch switchStatus;
    private MaterialButton btnUpload;
    private ImageButton btnNotif;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        // 1. Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(""); 
        }

        // 2. Initialize Views
        txtTeacherName = findViewById(R.id.txtTeacherName);
        txtInstitutionDept = findViewById(R.id.txtInstitutionDept);
        txtSolved = findViewById(R.id.txtSolvedCount);
        txtPending = findViewById(R.id.txtPendingCount);
        txtRating = findViewById(R.id.txtRatingScore);
        
        cardDoubtAlert = findViewById(R.id.cardDoubtAlert);
        txtAlertTitle = findViewById(R.id.txtAlertTitle);
        txtAlertSubtitle = findViewById(R.id.txtAlertSubtitle);
        
        cardQueue = findViewById(R.id.cardTeacherPending);
        cardChat = findViewById(R.id.cardTeacherChat);
        cardHistory = findViewById(R.id.cardTeacherHistory);
        cardProfile = findViewById(R.id.cardTeacherProgress);
        cardAi = findViewById(R.id.cardAiAssistant);
        btnNotif = findViewById(R.id.btnTeacherNotif);
        
        txtStatusLabel = findViewById(R.id.txtStatusLabel);
        switchStatus = findViewById(R.id.switchStatus);
        
        btnUpload = findViewById(R.id.btnTeacherUpload);

        // 3. Personalize Greeting & Status
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String name = pref.getString("name", "User");
        String institution = pref.getString("institution_name", "Institution Name");
        String department = pref.getString("department_name", "Department");

        if (txtTeacherName != null) txtTeacherName.setText(name);
        if (txtInstitutionDept != null) {
            txtInstitutionDept.setText(institution.toUpperCase());
        }
        
        boolean isOnline = pref.getBoolean("is_online", false);
        if (switchStatus != null) {
            switchStatus.setChecked(isOnline);
            updateStatusUI(isOnline);
            switchStatus.setOnCheckedChangeListener((buttonView, isChecked) -> toggleStatus(isChecked));
        }

        // 4. Set Click Listeners
        if (cardQueue != null) cardQueue.setOnClickListener(v -> startActivity(new Intent(this, TeacherDoubtListActivity.class)));
        if (cardChat != null) cardChat.setOnClickListener(v -> startActivity(new Intent(this, TeacherMentorshipActivity.class)));
        if (cardHistory != null) cardHistory.setOnClickListener(v -> startActivity(new Intent(this, TeacherArchiveActivity.class)));
        if (cardProfile != null) cardProfile.setOnClickListener(v -> startActivity(new Intent(this, TeacherStatsActivity.class)));
        
        if (cardAi != null) {
            cardAi.setOnClickListener(v -> {
                Intent intent = new Intent(this, AiChatActivity.class);
                intent.putExtra("mode", "teacher");
                startActivity(intent);
            });
        }

        if (findViewById(R.id.btnStartAi) != null) {
            findViewById(R.id.btnStartAi).setOnClickListener(v -> {
                Intent intent = new Intent(this, AiChatActivity.class);
                intent.putExtra("mode", "teacher");
                startActivity(intent);
            });
        }

        if (btnUpload != null) {
            btnUpload.setOnClickListener(v -> startActivity(new Intent(this, UploadActivity.class)));
        }
        

        if (btnNotif != null) {
            btnNotif.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        }

        fetchData();
    }

    private void fetchData() {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String teacherName = pref.getString("name", "Teacher");
        String url = "http://10.0.2.2/eduinsight_api/get_teacher_doubts.php?teacher_name=" + android.net.Uri.encode(teacherName);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray array;
                        JSONObject root = null;
                        if (response.trim().startsWith("{")) {
                            root = new JSONObject(response);
                            array = root.getJSONArray("doubts");
                        } else {
                            array = new JSONArray(response);
                        }

                        // Update Stats from wrapper object if available
                        if (root != null && root.has("status") && root.getString("status").equals("success")) {
                            int solved = root.optInt("solved_count", 0);
                            int pending = root.optInt("pending_count", 0);
                            double rating = root.optDouble("rating", 0.0);
                            String inst = root.optString("institution_name", "");
                            String dept = root.optString("department_name", "");

                            if (txtSolved != null) txtSolved.setText(String.format("%02d", solved));
                            if (txtPending != null) txtPending.setText(String.format("%02d", pending));
                            if (txtRating != null) txtRating.setText(String.format("%.1f", rating));
                            
                            if (txtInstitutionDept != null && !inst.isEmpty()) {
                                String display = inst.toUpperCase();
                                if (!dept.isEmpty()) display += " • " + dept;
                                txtInstitutionDept.setText(display);
                            }

                            // Update Alert Area
                            int unread = root.optInt("unread_count", 0);
                            if (cardDoubtAlert != null) {
                                if (unread > 0) {
                                    cardDoubtAlert.setVisibility(android.view.View.VISIBLE);
                                    if (txtAlertTitle != null) txtAlertTitle.setText("New Messages");
                                    if (txtAlertSubtitle != null) txtAlertSubtitle.setText("You have " + unread + " unread mentorship chats");
                                    cardDoubtAlert.setOnClickListener(v -> startActivity(new Intent(this, TeacherMentorshipActivity.class)));
                                } else if (array.length() > 0 && array.getJSONObject(0).optString("status").equalsIgnoreCase("pending")) {
                                    JSONObject latest = array.getJSONObject(0);
                                    cardDoubtAlert.setVisibility(android.view.View.VISIBLE);
                                    if (txtAlertTitle != null) txtAlertTitle.setText("Pending: " + latest.optString("subject", "Doubt"));
                                    if (txtAlertSubtitle != null) txtAlertSubtitle.setText(latest.optString("student_name") + ": " + latest.optString("description"));
                                    cardDoubtAlert.setOnClickListener(v -> startActivity(new Intent(this, TeacherDoubtListActivity.class)));
                                } else {
                                    cardDoubtAlert.setVisibility(android.view.View.GONE);
                                }
                            }
                        } else {
                            // Fallback: calculate manually if direct array
                            int solved = 0, pending = 0;
                            for (int i = 0; i < array.length(); i++) {
                                if (array.getJSONObject(i).optString("status").equalsIgnoreCase("solved")) solved++;
                                else pending++;
                            }
                            if (txtSolved != null) txtSolved.setText(String.format("%02d", solved));
                            if (txtPending != null) txtPending.setText(String.format("%02d", pending));
                        }

                    } catch (Exception e) { 
                        Log.e("DASHBOARD_ERROR", "Data parsing failed: " + e.getMessage()); 
                    }
                }, error -> Log.e("DASHBOARD_ERROR", "Network error"));

        Volley.newRequestQueue(this).add(request);
    }

    private void toggleStatus(boolean isOnline) {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int userId = pref.getInt("user_id", -1);
        String url = "http://10.0.2.2/eduinsight_api/update_status.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.trim().equals("success")) {
                        pref.edit().putBoolean("is_online", isOnline).apply();
                        updateStatusUI(isOnline);
                    } else {
                        switchStatus.setChecked(!isOnline);
                        Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    switchStatus.setChecked(!isOnline);
                    Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected java.util.Map<String, String> getParams() {
                java.util.Map<String, String> params = new java.util.HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("status", isOnline ? "1" : "0");
                return params;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    private void updateStatusUI(boolean isOnline) {
        if (txtStatusLabel != null) {
            txtStatusLabel.setText(isOnline ? "Online" : "Offline");
            txtStatusLabel.setTextColor(isOnline ? getResources().getColor(R.color.success) : getResources().getColor(R.color.text_secondary));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchData();
    }
}
