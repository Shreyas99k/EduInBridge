package com.example.eduinsight;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.airbnb.lottie.LottieAnimationView;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class ForgotPasswordActivity extends AppCompatActivity {

    private LinearLayout layoutStep1, layoutStep2, layoutStep3;
    private EditText editIdentity, editForgotOtp, editNewPassword, editConfirmPassword;
    private MaterialButton btnSendOtp, btnVerifyForgotOtp, btnUpdatePassword;
    private TextView step1Dot, step2Dot, step3Dot;
    private TextView txtHeaderSubtitle;
    private LottieAnimationView lottieLock;
    
    private String generatedOTP;
    private String userIdentity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize Views
        layoutStep1 = findViewById(R.id.layoutStep1);
        layoutStep2 = findViewById(R.id.layoutStep2);
        layoutStep3 = findViewById(R.id.layoutStep3);
        
        editIdentity = findViewById(R.id.editIdentity);
        editForgotOtp = findViewById(R.id.editForgotOtp);
        editNewPassword = findViewById(R.id.editNewPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyForgotOtp = findViewById(R.id.btnVerifyForgotOtp);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);

        step1Dot = findViewById(R.id.step1_dot);
        step2Dot = findViewById(R.id.step2_dot);
        step3Dot = findViewById(R.id.step3_dot);
        txtHeaderSubtitle = findViewById(R.id.txtHeaderSubtitle);
        lottieLock = findViewById(R.id.lottieLock);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        btnSendOtp.setOnClickListener(v -> {
            userIdentity = editIdentity.getText().toString().trim();
            if (userIdentity.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(userIdentity).matches()) {
                Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
                return;
            }
            sendOtpToUser(userIdentity);
        });

        btnVerifyForgotOtp.setOnClickListener(v -> {
            String enteredOtp = editForgotOtp.getText().toString().trim();
            if (enteredOtp.equals(generatedOTP)) {
                moveToStep(3);
            } else {
                Toast.makeText(this, getString(R.string.otp_error), Toast.LENGTH_SHORT).show();
            }
        });

        btnUpdatePassword.setOnClickListener(v -> {
            String newPass = editNewPassword.getText().toString().trim();
            String confirmPass = editConfirmPassword.getText().toString().trim();
            
            if (newPass.length() < 6) {
                Toast.makeText(this, getString(R.string.password_length_error), Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, getString(R.string.password_mismatch), Toast.LENGTH_SHORT).show();
                return;
            }
            updateUserPassword(userIdentity, newPass);
        });
    }

    private void moveToStep(int step) {
        layoutStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        layoutStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        layoutStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        updateIndicators(step);

        if (step == 2) {
            txtHeaderSubtitle.setText(getString(R.string.step_verify_subtitle, userIdentity));
            lottieLock.setAnimation(R.raw.bot_typing); 
            lottieLock.playAnimation();
        } else if (step == 3) {
            txtHeaderSubtitle.setText(getString(R.string.step_secure_subtitle));
            lottieLock.setAnimation(R.raw.success_tick);
            lottieLock.setRepeatCount(0);
            lottieLock.playAnimation();
        }
    }

    private void updateIndicators(int step) {
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.border);
        int activeTextColor = ContextCompat.getColor(this, R.color.white);
        int inactiveTextColor = ContextCompat.getColor(this, R.color.text_muted);

        step1Dot.setBackgroundTintList(ColorStateList.valueOf(step >= 1 ? activeColor : inactiveColor));
        step1Dot.setTextColor(step >= 1 ? activeTextColor : inactiveTextColor);
        
        step2Dot.setBackgroundTintList(ColorStateList.valueOf(step >= 2 ? activeColor : inactiveColor));
        step2Dot.setTextColor(step >= 2 ? activeTextColor : inactiveTextColor);
        
        step3Dot.setBackgroundTintList(ColorStateList.valueOf(step >= 3 ? activeColor : inactiveColor));
        step3Dot.setTextColor(step >= 3 ? activeTextColor : inactiveTextColor);
    }

    private void sendOtpToUser(String identity) {
        generatedOTP = String.format(Locale.US, "%04d", new Random().nextInt(10000));
        String url = "http://10.0.2.2/eduinsight_api/send_otp.php";
        
        btnSendOtp.setEnabled(false);
        btnSendOtp.setText(getString(R.string.sending));

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    btnSendOtp.setEnabled(true);
                    btnSendOtp.setText(getString(R.string.request_code));
                    
                    if (response.trim().contains("success")) {
                        Toast.makeText(this, getString(R.string.otp_sent), Toast.LENGTH_SHORT).show();
                        moveToStep(2);
                    } else {
                        Toast.makeText(this, "Error: " + response, Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    btnSendOtp.setEnabled(true);
                    btnSendOtp.setText(getString(R.string.request_code));
                    Toast.makeText(this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("identity", identity);
                params.put("otp", generatedOTP);
                params.put("skip_check", "true"); 
                return params;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(15000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(this).add(request);
    }

    private void updateUserPassword(String identity, String password) {
        String url = "http://10.0.2.2/eduinsight_api/update_password.php";
        btnUpdatePassword.setEnabled(false);
        btnUpdatePassword.setText(getString(R.string.updating));

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    btnUpdatePassword.setEnabled(true);
                    btnUpdatePassword.setText(getString(R.string.finalize_reset));
                    if (response.trim().contains("success")) {
                        Toast.makeText(this, getString(R.string.password_updated), Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Error: " + response, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    btnUpdatePassword.setEnabled(true);
                    btnUpdatePassword.setText(getString(R.string.finalize_reset));
                    Toast.makeText(this, "Request failed", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("identity", identity);
                params.put("password", password);
                return params;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }
}
