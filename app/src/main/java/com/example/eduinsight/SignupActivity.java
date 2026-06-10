package com.example.eduinsight;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class SignupActivity extends AppCompatActivity {
    private EditText nameField, emailField, mobileField, passField, expertiseField;
    private AutoCompleteTextView institutionSpinner;
    private TextInputLayout layoutExpertise, layoutInstitution;
    private RadioGroup roleGroup;
    private View txtIdentityLabel;
    private Button btnSignup;
    private String generatedOTP, fcmToken = "";
    private CountDownTimer resendTimer;
    private String preSelectedRole = "";
    private List<String> institutionList = new ArrayList<>();
    private List<String> institutionIds = new ArrayList<>();
    private String selectedInstitutionId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        preSelectedRole = getIntent().getStringExtra("selected_role");

        nameField = findViewById(R.id.signupName);
        emailField = findViewById(R.id.signupEmail);
        mobileField = findViewById(R.id.signupMobile);
        passField = findViewById(R.id.signupPassword);
        expertiseField = findViewById(R.id.editExpertise);
        layoutExpertise = findViewById(R.id.layoutExpertise);
        layoutInstitution = findViewById(R.id.layoutInstitution);
        institutionSpinner = findViewById(R.id.autoCompleteInstitution);
        roleGroup = findViewById(R.id.roleGroup);
        txtIdentityLabel = findViewById(R.id.txtIdentityLabel);
        btnSignup = findViewById(R.id.btnSignup);

        fetchFcmToken();
        configureFormByRole();
        fetchInstitutions();

        btnSignup.setOnClickListener(v -> initiateEmailVerification());
        
        institutionSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedInstitutionId = institutionIds.get(position);
        });
    }

    private void fetchInstitutions() {
        String url = "http://10.0.2.2/eduinsight_api/get_institutions.php";
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray arr = new JSONArray(response);
                        institutionList.clear();
                        institutionIds.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            institutionList.add(obj.getString("name"));
                            institutionIds.add(obj.getString("id"));
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_dropdown_item_1line, institutionList);
                        institutionSpinner.setAdapter(adapter);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, error -> Log.e("Signup", "Failed to fetch institutions"));
        Volley.newRequestQueue(this).add(request);
    }

    private void configureFormByRole() {
        if (preSelectedRole != null && !preSelectedRole.isEmpty()) {
            if (roleGroup != null) roleGroup.setVisibility(View.GONE);
            if (txtIdentityLabel != null) txtIdentityLabel.setVisibility(View.GONE);

            if ("teacher".equalsIgnoreCase(preSelectedRole)) {
                if (layoutExpertise != null) layoutExpertise.setVisibility(View.VISIBLE);
                if (layoutInstitution != null) layoutInstitution.setVisibility(View.VISIBLE);
                nameField.setHint("Mentor Full Name");
            } else if ("institution".equalsIgnoreCase(preSelectedRole)) {
                if (layoutExpertise != null) layoutExpertise.setVisibility(View.GONE);
                if (layoutInstitution != null) layoutInstitution.setVisibility(View.GONE);
                nameField.setHint("Institution Name");
            } else {
                // For Students
                if (layoutExpertise != null) layoutExpertise.setVisibility(View.GONE);
                if (layoutInstitution != null) layoutInstitution.setVisibility(View.VISIBLE);
                nameField.setHint("Student Full Name");
            }
        } else {
            if (roleGroup != null) roleGroup.setVisibility(View.VISIBLE);
            if (txtIdentityLabel != null) txtIdentityLabel.setVisibility(View.VISIBLE);

            roleGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.radioTeacher || checkedId == R.id.radioStudent) {
                    if (layoutInstitution != null) layoutInstitution.setVisibility(View.VISIBLE);
                    if (checkedId == R.id.radioTeacher) {
                        if (layoutExpertise != null) layoutExpertise.setVisibility(View.VISIBLE);
                    } else {
                        if (layoutExpertise != null) layoutExpertise.setVisibility(View.GONE);
                    }
                } else {
                    if (layoutExpertise != null) layoutExpertise.setVisibility(View.GONE);
                    if (layoutInstitution != null) layoutInstitution.setVisibility(View.GONE);
                }
            });
        }
    }

    private void fetchFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                fcmToken = task.getResult();
            }
        });
    }

    private void initiateEmailVerification() {
        String name = nameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String pass = passField.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Fill all mandatory fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate institution selection for students/teachers
        String currentRole = preSelectedRole;
        if (currentRole == null || currentRole.isEmpty()) {
            int id = roleGroup.getCheckedRadioButtonId();
            if (id == R.id.radioTeacher) currentRole = "teacher";
            else if (id == R.id.radioStudent) currentRole = "student";
        }

        if (("student".equalsIgnoreCase(currentRole) || "teacher".equalsIgnoreCase(currentRole)) && selectedInstitutionId.isEmpty()) {
            Toast.makeText(this, "Please select your Institution", Toast.LENGTH_SHORT).show();
            return;
        }

        generatedOTP = String.format(Locale.getDefault(), "%04d", new Random().nextInt(10000));
        sendOtpToEmail(email, generatedOTP);
        showOtpDialog(email);
    }

    private void showOtpDialog(String email) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_otp_verify, null);
        EditText editOtp = v.findViewById(R.id.editOtp);
        TextView txtResend = v.findViewById(R.id.txtResendOtp);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Secure Verification").setView(v).setCancelable(false)
                .setPositiveButton("Register", null).setNegativeButton("Cancel", (d, w) -> {
                    if (resendTimer != null) resendTimer.cancel();
                }).create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                if (editOtp.getText().toString().trim().equals(generatedOTP)) {
                    performSignup();
                    dialog.dismiss();
                } else Toast.makeText(this, "Invalid Node Key", Toast.LENGTH_SHORT).show();
            });
            startResendTimer(txtResend, email);
        });
        dialog.show();
    }

    private void startResendTimer(TextView txtResend, String email) {
        if (resendTimer != null) resendTimer.cancel();
        txtResend.setEnabled(false);
        resendTimer = new CountDownTimer(30000, 1000) {
            public void onTick(long ms) { txtResend.setText("Retry in " + (ms / 1000) + "s"); }
            public void onFinish() {
                txtResend.setText("Resend Key");
                txtResend.setEnabled(true);
                txtResend.setOnClickListener(v -> {
                    generatedOTP = String.format(Locale.getDefault(), "%04d", new Random().nextInt(10000));
                    sendOtpToEmail(email, generatedOTP);
                    startResendTimer(txtResend, email);
                });
            }
        }.start();
    }

    private void sendOtpToEmail(String email, String otp) {
        String url = "http://10.0.2.2/eduinsight_api/send_otp.php";
        StringRequest request = new StringRequest(Request.Method.POST, url, null, null) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("identity", email); p.put("otp", otp); p.put("skip_check", "true");
                return p;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(20000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(this).add(request);
    }

    private void performSignup() {
        String url = "http://10.0.2.2/eduinsight_api/signup.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.contains("success")) {
                        Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else Toast.makeText(this, "Error: " + response, Toast.LENGTH_LONG).show();
                }, error -> Toast.makeText(this, "Network Link Offline", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("name", nameField.getText().toString().trim());
                p.put("email", emailField.getText().toString().trim());
                p.put("mobile", mobileField.getText().toString().trim());
                p.put("password", passField.getText().toString().trim());
                p.put("fcm_token", fcmToken);

                String finalRole = preSelectedRole;
                if (finalRole == null || finalRole.isEmpty()) {
                    int checkedId = roleGroup.getCheckedRadioButtonId();
                    if (checkedId == R.id.radioInstitution) finalRole = "institution";
                    else if (checkedId == R.id.radioTeacher) finalRole = "teacher";
                    else finalRole = "student";
                }
                p.put("role", finalRole);
                p.put("branch", expertiseField.getText().toString().trim());
                
                // Include institution_id for both students and teachers
                if ("teacher".equalsIgnoreCase(finalRole) || "student".equalsIgnoreCase(finalRole)) {
                    p.put("institution_id", selectedInstitutionId);
                }

                if ("teacher".equalsIgnoreCase(finalRole)) {
                    p.put("status", "pending");
                } else {
                    p.put("status", "active");
                }

                return p;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }
}
