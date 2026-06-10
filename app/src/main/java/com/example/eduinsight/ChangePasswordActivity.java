package com.example.eduinsight;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.util.HashMap;
import java.util.Map;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText editOld, editNew;
    private Button btnUpdate;
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        editOld = findViewById(R.id.editOldPassword);
        editNew = findViewById(R.id.editNewPassword);
        btnUpdate = findViewById(R.id.btnUpdatePassword);
        pref = getSharedPreferences("UserSession", MODE_PRIVATE);

        btnUpdate.setOnClickListener(v -> {
            String oldP = editOld.getText().toString().trim();
            String newP = editNew.getText().toString().trim();
            int userId = pref.getInt("user_id", -1);

            if (oldP.isEmpty() || newP.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            } else if (newP.length() < 6) {
                Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            } else {
                performPasswordChange(oldP, newP, userId);
            }
        });
    }

    private void performPasswordChange(String oldP, String newP, int id) {
        String url = "http://10.0.2.2/eduinsight_api/change_password.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.trim().equalsIgnoreCase("success")) {
                        Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Error: " + response, Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(id));
                params.put("old_pass", oldP);
                params.put("new_pass", newP);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}