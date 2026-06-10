package com.example.eduinsight;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String BASE_URL = "http://10.0.2.2/eduinsight_api/";

    private EditText emailField, passField;
    private EditText regUserField, regEmailField, regMobileField, regPassField, regInstitutionField, regAddressField, regDepartmentField;
    private TextInputLayout layoutRegInstitution, layoutRegAddress, layoutRegDepartment;
    private Button btnLogin, btnRoleNext, btnFinalRegister;
    private TextView txtShowSignup, txtShowLoginFromRole;
    private LinearLayout loginForm, registerForm, roleSelectionForm;
    private TextView appNameTitle;
    
    private MaterialCardView cardStudent, cardTeacher, cardInstitution;
    private RadioButton radioStudent, radioTeacher, radioInstitution;
    private String selectedRole = "student";

    private String fcmToken = "";
    private boolean isLoginMode = true;
    private final List<JSONObject> fullInstitutionList = new ArrayList<>();
    private final List<JSONObject> filteredInstitutionList = new ArrayList<>();
    private final List<JSONObject> departmentList = new ArrayList<>();
    
    private String selectedInstitutionId = "";
    private String selectedDepartmentId = "";
    private CountDownTimer appNameTimer;
    private String generatedOTP;
    private CountDownTimer resendTimer;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> 
                Log.d("FCM_DEBUG", "Permission: " + (isGranted ? "Granted" : "Denied")));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        checkExistingSession();
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.inner_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Branding & Animation
        appNameTitle = findViewById(R.id.appNameTitle);
        // FORCE NEW LOGO AND REMOVE DYNAMIC OVERRIDE
        ImageView logo = findViewById(R.id.headerIllustration);
        if (logo != null) logo.setImageResource(R.drawable.ic_edu_insight_logo);
        animateText("EduInBridge", appNameTitle);

        // 2. Containers
        loginForm = findViewById(R.id.loginForm);
        registerForm = findViewById(R.id.registerForm);
        roleSelectionForm = findViewById(R.id.roleSelectionForm);

        // 3. Login
        emailField = findViewById(R.id.loginEmail);
        passField = findViewById(R.id.loginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtShowSignup = findViewById(R.id.txtShowSignup);

        // 4. Register
        regUserField = findViewById(R.id.regUsername);
        regEmailField = findViewById(R.id.regEmail);
        regMobileField = findViewById(R.id.regMobile);
        regPassField = findViewById(R.id.regPassword);
        regAddressField = findViewById(R.id.regAddress);
        regInstitutionField = findViewById(R.id.autoCompleteRegInstitution);
        regDepartmentField = findViewById(R.id.autoCompleteRegDepartment);
        layoutRegInstitution = findViewById(R.id.layoutRegInstitution);
        layoutRegAddress = findViewById(R.id.layoutRegAddress);
        layoutRegDepartment = findViewById(R.id.layoutRegDepartment);
        btnFinalRegister = findViewById(R.id.btnFinalRegister);

        // 5. Role
        cardStudent = findViewById(R.id.cardStudent);
        cardTeacher = findViewById(R.id.cardTeacher);
        cardInstitution = findViewById(R.id.cardInstitution);
        radioStudent = findViewById(R.id.radioStudent);
        radioTeacher = findViewById(R.id.radioTeacher);
        radioInstitution = findViewById(R.id.radioInstitution);
        btnRoleNext = findViewById(R.id.btnRoleNext);
        txtShowLoginFromRole = findViewById(R.id.txtShowLoginFromRole);

        setupRoleSelection();
        setupInteractiveAnimations();
        animateEntrance();

        askNotificationPermission();
        fetchFcmToken();
        fetchInstitutions();

        btnLogin.setOnClickListener(v -> performLogin());
        
        btnRoleNext.setOnClickListener(v -> {
            if (selectedRole.equals("institution")) {
                layoutRegInstitution.setVisibility(View.GONE);
                layoutRegDepartment.setVisibility(View.GONE);
                layoutRegAddress.setVisibility(View.VISIBLE);
            } else if (selectedRole.equals("teacher")) {
                layoutRegInstitution.setVisibility(View.VISIBLE);
                layoutRegDepartment.setVisibility(View.VISIBLE);
                layoutRegAddress.setVisibility(View.GONE);
            } else {
                layoutRegInstitution.setVisibility(View.VISIBLE);
                layoutRegDepartment.setVisibility(View.GONE);
                layoutRegAddress.setVisibility(View.GONE);
            }
            showRegistrationForm(true);
        });

        if (regInstitutionField != null) {
            regInstitutionField.setOnClickListener(v -> showInstitutionSearchDialog());
        }

        if (regDepartmentField != null) {
            regDepartmentField.setOnClickListener(v -> {
                if (selectedInstitutionId.isEmpty()) {
                    Toast.makeText(this, "Please select an institution first", Toast.LENGTH_SHORT).show();
                } else {
                    showDepartmentSearchDialog();
                }
            });
        }
        
        btnFinalRegister.setOnClickListener(v -> initiateEmailVerification());
        txtShowSignup.setOnClickListener(v -> toggleMode(false));
        txtShowLoginFromRole.setOnClickListener(v -> toggleMode(true));
        findViewById(R.id.btnBackToRole).setOnClickListener(v -> showRegistrationForm(false));
    }

    private void setupRoleSelection() {
        View.OnClickListener listener = v -> {
            int id = v.getId();
            radioStudent.setChecked(id == R.id.cardStudent);
            radioTeacher.setChecked(id == R.id.cardTeacher);
            radioInstitution.setChecked(id == R.id.cardInstitution);

            cardStudent.setStrokeColor(id == R.id.cardStudent ? Color.parseColor("#6366F1") : Color.parseColor("#E2E8F0"));
            cardStudent.setStrokeWidth(id == R.id.cardStudent ? dpToPx(2) : dpToPx(1));
            cardTeacher.setStrokeColor(id == R.id.cardTeacher ? Color.parseColor("#6366F1") : Color.parseColor("#E2E8F0"));
            cardTeacher.setStrokeWidth(id == R.id.cardTeacher ? dpToPx(2) : dpToPx(1));
            cardInstitution.setStrokeColor(id == R.id.cardInstitution ? Color.parseColor("#6366F1") : Color.parseColor("#E2E8F0"));
            cardInstitution.setStrokeWidth(id == R.id.cardInstitution ? dpToPx(2) : dpToPx(1));

            if (id == R.id.cardStudent) selectedRole = "student";
            else if (id == R.id.cardTeacher) selectedRole = "teacher";
            else selectedRole = "institution";
        };
        cardStudent.setOnClickListener(listener);
        cardTeacher.setOnClickListener(listener);
        cardInstitution.setOnClickListener(listener);
    }

    private int dpToPx(int dp) { return (int) (dp * getResources().getDisplayMetrics().density); }

    private void animateText(String text, TextView textView) {
        if (textView == null) return;
        if (appNameTimer != null) appNameTimer.cancel();
        textView.setText("");
        appNameTimer = new CountDownTimer(text.length() * 100 + 200, 100) {
            int index = 0;
            @Override
            public void onTick(long millisUntilFinished) {
                if (index < text.length()) {
                    index++;
                    String t = text.substring(0, index);
                    SpannableString spannable = new SpannableString(t);
                    if (t.length() > 5) {
                        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#0F172A")), 5, t.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    textView.setText(spannable);
                }
            }
            @Override public void onFinish() {
                SpannableString spannable = new SpannableString(text);
                spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#0F172A")), 5, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textView.setText(spannable);
            }
        }.start();
    }

    private void setupInteractiveAnimations() {
        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            float scale = hasFocus ? 1.03f : 1.0f;
            v.animate().scaleX(scale).scaleY(scale).setDuration(250).start();
        };
        emailField.setOnFocusChangeListener(focusListener);
        passField.setOnFocusChangeListener(focusListener);
        
        View.OnTouchListener listener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start();
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).setInterpolator(new OvershootInterpolator()).start();
            return false;
        };
        btnLogin.setOnTouchListener(listener);
        btnRoleNext.setOnTouchListener(listener);
    }

    private void animateEntrance() {
        loginForm.setAlpha(0f);
        loginForm.setTranslationY(80f);
        loginForm.animate().alpha(1f).translationY(0).setDuration(600).setStartDelay(200).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void toggleMode(boolean login) {
        isLoginMode = login;
        if (login) {
            registerForm.setVisibility(View.GONE);
            roleSelectionForm.setVisibility(View.GONE);
            loginForm.setVisibility(View.VISIBLE);
            loginForm.setAlpha(1f);
            loginForm.setTranslationY(0);
        } else {
            loginForm.setVisibility(View.GONE);
            roleSelectionForm.setVisibility(View.VISIBLE);
            roleSelectionForm.setAlpha(1f);
            roleSelectionForm.setTranslationY(0);
        }
        View current = getCurrentFocus();
        if (current != null) current.clearFocus();
    }

    private void showRegistrationForm(boolean show) {
        if (show) {
            roleSelectionForm.setVisibility(View.GONE);
            registerForm.setVisibility(View.VISIBLE);
            registerForm.setAlpha(1f);
            registerForm.setTranslationY(0);
            regUserField.requestFocus();
        } else {
            registerForm.setVisibility(View.GONE);
            roleSelectionForm.setVisibility(View.VISIBLE);
        }
    }

    private void performLogin() {
        String email = emailField.getText().toString().trim();
        String pass = passField.getText().toString().trim();
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Credentials required", Toast.LENGTH_SHORT).show();
            return;
        }
        btnLogin.setText("Connecting...");
        String url = BASE_URL + "login.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    btnLogin.setText("Sign In");
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optString("status").equals("success")) {
                            saveSession(json);
                            navigateToDashboard(json.optString("role", ""));
                        } else Toast.makeText(this, json.optString("message", "Login failed"), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) { 
                        Log.e(TAG, "Login Error: " + e.getMessage() + "\nResponse: " + response);
                        Toast.makeText(this, "Server error", Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
                    btnLogin.setText("Sign In");
                    Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show();
                }) {
            @Override protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("email", email); p.put("password", pass); p.put("fcm_token", fcmToken);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    private void initiateEmailVerification() {
        String name = regUserField.getText().toString().trim();
        String email = regEmailField.getText().toString().trim();
        String mobile = regMobileField.getText().toString().trim();
        String password = regPassField.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || mobile.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedRole.equals("institution") && regAddressField.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Institution address required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!selectedRole.equals("institution") && selectedInstitutionId.isEmpty()) {
            Toast.makeText(this, "Select your institution", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Sending code...", Toast.LENGTH_SHORT).show();
        generatedOTP = String.format(Locale.US, "%04d", new Random().nextInt(10000));
        sendOtpToEmail(email, generatedOTP);
        showOtpDialog(email);
    }

    private void showOtpDialog(final String email) {
        runOnUiThread(() -> {
            try {
                View v = LayoutInflater.from(this).inflate(R.layout.dialog_otp_verify, null);
                final EditText editOtp = v.findViewById(R.id.editOtp);
                final MaterialButton btnVerify = v.findViewById(R.id.btnVerifyOtp);
                final TextView txtResend = v.findViewById(R.id.txtResendOtp);

                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setView(v).setCancelable(false).create();

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }

                btnVerify.setOnClickListener(v1 -> {
                    if (editOtp.getText().toString().trim().equals(generatedOTP)) {
                        performRegistration();
                        dialog.dismiss();
                    } else Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                });

                v.findViewById(R.id.btnCancelOtp).setOnClickListener(v1 -> dialog.dismiss());

                startResendTimer(txtResend, email);
                dialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Dialog error: " + e.getMessage());
            }
        });
    }

    private void startResendTimer(TextView tv, String email) {
        if (resendTimer != null) resendTimer.cancel();
        tv.setEnabled(false);
        resendTimer = new CountDownTimer(30000, 1000) {
            public void onTick(long ms) { tv.setText("Resend in " + (ms / 1000) + "s"); }
            public void onFinish() {
                tv.setText("Resend OTP");
                tv.setTextColor(Color.parseColor("#6366F1"));
                tv.setEnabled(true);
                tv.setOnClickListener(v -> initiateEmailVerification());
            }
        }.start();
    }

    private void sendOtpToEmail(String email, String otp) {
        String url = BASE_URL + "send_otp.php";
        StringRequest request = new StringRequest(Request.Method.POST, url, 
            response -> Log.d(TAG, "OTP Sent"), 
            error -> Log.e(TAG, "OTP Error: " + error.toString())) {
            @Override protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("identity", email); p.put("otp", otp); p.put("skip_check", "true");
                return p;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(20000, 0, 1f));
        Volley.newRequestQueue(this).add(request);
    }

    private void performRegistration() {
        String name = regUserField.getText().toString().trim();
        String email = regEmailField.getText().toString().trim();
        String mobile = regMobileField.getText().toString().trim();
        String password = regPassField.getText().toString().trim();
        String address = regAddressField.getText().toString().trim();

        btnFinalRegister.setText("Joining...");
        String url = BASE_URL + "signup.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    btnFinalRegister.setText("Join Now");
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optString("status").equals("success")) {
                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show();
                            toggleMode(true);
                        } else Toast.makeText(this, json.optString("message"), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) { e.printStackTrace(); }
                }, error -> {
                    btnFinalRegister.setText("Join Now");
                    Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show();
                }) {
            @Override protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("name", name); p.put("email", email); p.put("mobile", mobile);
                p.put("password", password); p.put("role", selectedRole);
                p.put("address", address); p.put("institution_id", selectedInstitutionId);
                p.put("department_id", selectedDepartmentId);
                p.put("fcm_token", fcmToken);
                p.put("status", selectedRole.equals("teacher") ? "pending" : "active");
                return p;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    private void saveSession(JSONObject json) throws Exception {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        
        int userId = json.optInt("user_id", -1);
        int instId = json.optInt("institution_id", -1);
        
        String mobile = json.optString("mobile", "");
        if (mobile.isEmpty() || mobile.equalsIgnoreCase("null")) {
            mobile = "Not Provided";
        }
        
        editor.putInt("user_id", userId);
        editor.putString("name", json.optString("name", ""));
        editor.putString("email", json.optString("email", ""));
        editor.putString("mobile", mobile);
        editor.putString("role", json.optString("role", ""));
        editor.putInt("institution_id", instId);
        editor.putString("institution_name", json.optString("institution_name", ""));
        editor.putString("department_name", json.optString("department_name", ""));

        editor.apply();
    }

    private void navigateToDashboard(String role) {
        if ("teacher".equalsIgnoreCase(role)) startActivity(new Intent(this, TeacherDashboardActivity.class));
        else if ("institution".equalsIgnoreCase(role)) startActivity(new Intent(this, InstitutionDashboardActivity.class));
        else startActivity(new Intent(this, MainStudentDashboardActivity.class));
        finish();
    }

    private void checkExistingSession() {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        if (pref.contains("user_id")) {
            navigateToDashboard(pref.getString("role", ""));
        }
    }

    private void fetchInstitutions() {
        String url = BASE_URL + "get_institutions.php";
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray arr = new JSONArray(response);
                        fullInstitutionList.clear();
                        for (int i = 0; i < arr.length(); i++) fullInstitutionList.add(arr.getJSONObject(i));
                    } catch (Exception e) {}
                }, null);
        Volley.newRequestQueue(this).add(request);
    }

    private void fetchDepartments(String instId) {
        String url = BASE_URL + "get_departments.php?institution_id=" + instId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray arr = new JSONArray(response);
                        departmentList.clear();
                        for (int i = 0; i < arr.length(); i++) departmentList.add(arr.getJSONObject(i));
                    } catch (Exception e) {}
                }, null);
        Volley.newRequestQueue(this).add(request);
    }

    private void showInstitutionSearchDialog() {
        if (fullInstitutionList.isEmpty()) { fetchInstitutions(); return; }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_searchable_selection, null);
        EditText searchBox = view.findViewById(R.id.editSearchQuery);
        RecyclerView recyclerView = view.findViewById(R.id.rvSearchList);
        ((TextView) view.findViewById(R.id.txtSearchTitle)).setText("Select Institution");

        filteredInstitutionList.clear(); filteredInstitutionList.addAll(fullInstitutionList);
        AlertDialog dialog = builder.setView(view).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        InstitutionAdapter adapter = new InstitutionAdapter(filteredInstitutionList, item -> {
            try {
                selectedInstitutionId = item.getString("id");
                regInstitutionField.setText(item.getString("name"));
                selectedDepartmentId = ""; regDepartmentField.setText("");
                fetchDepartments(selectedInstitutionId);
                dialog.dismiss();
            } catch (Exception e) {}
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filteredInstitutionList.clear();
                for (JSONObject o : fullInstitutionList) {
                    if (o.optString("name").toLowerCase().contains(s.toString().toLowerCase())) filteredInstitutionList.add(o);
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        view.findViewById(R.id.btnCancelSearch).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDepartmentSearchDialog() {
        if (departmentList.isEmpty()) { Toast.makeText(this, "No departments found", Toast.LENGTH_SHORT).show(); return; }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_searchable_selection, null);
        EditText searchBox = view.findViewById(R.id.editSearchQuery);
        RecyclerView recyclerView = view.findViewById(R.id.rvSearchList);
        ((TextView) view.findViewById(R.id.txtSearchTitle)).setText("Select Department");

        AlertDialog dialog = builder.setView(view).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        InstitutionAdapter adapter = new InstitutionAdapter(departmentList, item -> {
            try {
                selectedDepartmentId = item.getString("id");
                regDepartmentField.setText(item.getString("name"));
                dialog.dismiss();
            } catch (Exception e) {}
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        view.findViewById(R.id.btnCancelSearch).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void fetchFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) fcmToken = task.getResult();
        });
    }

    @Override
    protected void onDestroy() {
        if (appNameTimer != null) appNameTimer.cancel();
        if (resendTimer != null) resendTimer.cancel();
        super.onDestroy();
    }
}
