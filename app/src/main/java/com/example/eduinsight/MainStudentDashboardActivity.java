package com.example.eduinsight;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import java.util.Calendar;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import android.widget.TextView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class MainStudentDashboardActivity extends AppCompatActivity {

    private SharedPreferences pref;
    private TextView txtGreeting, txtInstitution, txtSolvedRate, txtPendingRate, txtTotalAskedCount, txtWelcome, txtStudentAlertTitle, txtStudentAlertSubtitle;
    private View badge1, badge2, badge3, badge4, cardStudentAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_student_dashboard);

        pref = getSharedPreferences("UserSession", MODE_PRIVATE);

        txtGreeting = findViewById(R.id.txtGreeting);
        txtWelcome = findViewById(R.id.txtWelcome);
        txtInstitution = findViewById(R.id.txtInstitution);
        txtSolvedRate = findViewById(R.id.rateCleared);
        txtPendingRate = findViewById(R.id.ratePending);
        txtTotalAskedCount = findViewById(R.id.txtTotalAskedCount);

        badge1 = findViewById(R.id.badge1);
        badge2 = findViewById(R.id.badge2);
        badge3 = findViewById(R.id.badge3);
        badge4 = findViewById(R.id.badge4);
        
        cardStudentAlert = findViewById(R.id.cardStudentAlert);
        txtStudentAlertTitle = findViewById(R.id.txtStudentAlertTitle);
        txtStudentAlertSubtitle = findViewById(R.id.txtStudentAlertSubtitle);

        applyAdaptiveGreeting();

        txtGreeting.setText(pref.getString("name", "Student"));
        txtInstitution.setText("Loading profile...");

        findViewById(R.id.cardPostDoubt).setOnClickListener(v -> startActivity(new Intent(this, PostDoubtActivity.class)));
        findViewById(R.id.cardChatHistory).setOnClickListener(v -> startActivity(new Intent(this, MyDoubtsActivity.class)));
        findViewById(R.id.cardMentorship).setOnClickListener(v -> startActivity(new Intent(this, StudentDashboardActivity.class)));
        findViewById(R.id.avatarContainer).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        
        findViewById(R.id.cardRepository).setOnClickListener(v -> {
            Intent intent = new Intent(this, UploadActivity.class);
            intent.putExtra("mode", "download_only");
            startActivity(intent);
        });

        // Fixed Notification Button click listener
        View btnNotifications = findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        }

        refreshDashboardData();
    }

    private void refreshDashboardData() {
        int userId = pref.getInt("user_id", -1);
        if (userId == -1) return;

        // Fetches real dashboard data and institution name from server
        String url = "http://10.0.2.2/eduinsight_api/get_student_dashboard_data.php?user_id=" + userId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.optString("status").equals("success")) {
                            txtInstitution.setText(response.optString("institution_name", "Independent Learner"));
                            
                            int solved = response.optInt("solved", 0);
                            int pending = response.optInt("pending", 0);
                            int total = solved + pending;
                            
                            txtTotalAskedCount.setText(String.valueOf(total));
                            
                            if (total > 0) {
                                txtSolvedRate.setText(((solved * 100) / total) + "%");
                                txtPendingRate.setText(((pending * 100) / total) + "%");
                            } else {
                                txtSolvedRate.setText("0%");
                                txtPendingRate.setText("0%");
                            }

                            // Update Badges based on engagement
                            updateBadges(solved, total);

                            // Update Student Alert Area (Simple Logic: If any pending doubt has new message)
                            if (cardStudentAlert != null) {
                                int unreadCount = response.optInt("unread_messages", 0);
                                if (unreadCount > 0) {
                                    cardStudentAlert.setVisibility(View.VISIBLE);
                                    if (txtStudentAlertTitle != null) txtStudentAlertTitle.setText("New Mentor Reply");
                                    if (txtStudentAlertSubtitle != null) txtStudentAlertSubtitle.setText("You have " + unreadCount + " unread messages from mentors");
                                    cardStudentAlert.setOnClickListener(v -> startActivity(new Intent(this, MyDoubtsActivity.class)));
                                } else {
                                    cardStudentAlert.setVisibility(View.GONE);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("DASHBOARD", "Data parse error");
                    }
                }, error -> {
                    Log.e("DASHBOARD", "Network error");
                });

        Volley.newRequestQueue(this).add(request);
    }

    private void applyAdaptiveGreeting() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        if (timeOfDay >= 5 && timeOfDay < 12) {
            txtWelcome.setText("Good Morning,");
        } else if (timeOfDay >= 12 && timeOfDay < 17) {
            txtWelcome.setText("Good Afternoon,");
        } else if (timeOfDay >= 17 && timeOfDay < 21) {
            txtWelcome.setText("Good Evening,");
        } else {
            txtWelcome.setText("Good Night,");
        }
    }

    private void updateBadges(int solvedCount, int totalAsked) {
        // Simple logic for gamification badges
        if (badge1 != null && solvedCount >= 1) {
            badge1.setAlpha(1.0f);
        }
        if (badge2 != null && solvedCount >= 10) {
            badge2.setAlpha(1.0f);
        }
        if (badge3 != null && totalAsked >= 5) {
            badge3.setAlpha(1.0f);
        }
        if (badge4 != null && solvedCount >= 50) {
            badge4.setAlpha(1.0f);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboardData();
    }
}
