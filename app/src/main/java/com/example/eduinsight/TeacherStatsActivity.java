package com.example.eduinsight;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class TeacherStatsActivity extends AppCompatActivity {

    private String userEmail, userMobile, userRole;
    private TextView txtMobile, txtEmail, txtName, txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_stats);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }

        txtName = findViewById(R.id.txtProfileName);
        txtEmail = findViewById(R.id.txtProfileEmail);
        txtMobile = findViewById(R.id.txtProfileMobile);
        txtStatus = findViewById(R.id.txtProfileStatus);
        
        View btnChangePass = findViewById(R.id.btnChangePassword);
        View btnLogout = findViewById(R.id.btnLogout);
        View btnChangeMobile = findViewById(R.id.btnChangeMobile);

        loadSessionData();
        refreshUserData();

        btnChangeMobile.setOnClickListener(v -> showUpdateMobileDialog());
        
        btnChangePass.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            if (userEmail != null && !userEmail.isEmpty() && !userEmail.equals("Not Provided")) {
                intent.putExtra("identity", userEmail);
            }
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.clear();
            editor.apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadSessionData() {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        userEmail = pref.getString("email", "Not Provided");
        userMobile = pref.getString("mobile", "Not Available");
        userRole = pref.getString("role", "User").toUpperCase();

        txtName.setText(pref.getString("name", "User"));
        txtEmail.setText(userEmail);
        txtMobile.setText(userMobile);
        
        if (txtStatus != null) {
            txtStatus.setText(userRole);
        }
    }

    private void refreshUserData() {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int userId = pref.getInt("user_id", -1);
        if (userId == -1) return;

        String url = "http://10.0.2.2/eduinsight_api/get_user_profile.php?user_id=" + userId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optString("status").equals("success")) {
                            userEmail = json.optString("email", userEmail);
                            userMobile = json.optString("mobile", userMobile);
                            String name = json.optString("name");
                            String role = json.optString("role");

                            txtName.setText(name);
                            txtEmail.setText(userEmail);
                            txtMobile.setText(userMobile);
                            if (txtStatus != null) txtStatus.setText(role.toUpperCase());
                            
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putString("email", userEmail);
                            editor.putString("mobile", userMobile);
                            editor.putString("name", name);
                            editor.putString("role", role);
                            editor.apply();
                        }
                    } catch (Exception e) { 
                        Log.e("PROFILE_ERROR", "Data refresh failed: " + e.getMessage()); 
                    }
                }, error -> Log.e("PROFILE_ERROR", "Network error"));

        Volley.newRequestQueue(this).add(request);
    }

    private void showUpdateMobileDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_otp_verify, null);
        EditText editNewMobile = view.findViewById(R.id.editOtp); 
        
        editNewMobile.setHint("Enter New 10-digit Mobile");
        editNewMobile.setInputType(InputType.TYPE_CLASS_PHONE);
        editNewMobile.setLetterSpacing(0);
        editNewMobile.setFilters(new InputFilter[] { new InputFilter.LengthFilter(10) });
        
        Button btnCancel = view.findViewById(R.id.btnCancelOtp);
        Button btnUpdate = view.findViewById(R.id.btnVerifyOtp);
        TextView txtResend = view.findViewById(R.id.txtResendOtp);
        TextView txtTitle = view.findViewById(R.id.dialogTitle);
        TextView txtMsg = view.findViewById(R.id.dialogMessage);

        if (txtTitle != null) txtTitle.setText("Update Contact");
        if (txtMsg != null) txtMsg.setText("Enter your new 10-digit mobile number below.");
        if (txtResend != null) txtResend.setVisibility(View.GONE);
        if (btnUpdate != null) btnUpdate.setText("UPDATE");

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnUpdate != null) btnUpdate.setOnClickListener(v -> {
            String newMobile = editNewMobile.getText().toString().trim();
            if (newMobile.length() == 10) {
                updateMobileInDatabase(newMobile);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Enter valid 10-digit number", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void updateMobileInDatabase(String newMobile) {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int userId = pref.getInt("user_id", -1);
        String url = "http://10.0.2.2/eduinsight_api/update_mobile.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.trim().equalsIgnoreCase("success")) {
                        userMobile = newMobile;
                        txtMobile.setText(newMobile);
                        SharedPreferences.Editor ed = pref.edit();
                        ed.putString("mobile", newMobile);
                        ed.apply();
                        Toast.makeText(this, "Mobile Updated Successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show();
                    }
                }, error -> Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("user_id", String.valueOf(userId));
                p.put("mobile", newMobile);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
