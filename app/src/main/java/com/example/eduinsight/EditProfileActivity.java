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

public class EditProfileActivity extends AppCompatActivity {

    private EditText editName, editEmail;
    private Button btnSave;
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Link UI components
        editName = findViewById(R.id.editTeacherName);
        editEmail = findViewById(R.id.editTeacherEmail);
        btnSave = findViewById(R.id.btnSaveChanges);

        // Access the session to get existing user info
        pref = getSharedPreferences("UserSession", MODE_PRIVATE);

        // Pre-fill the fields with current data
        editName.setText(pref.getString("user_name", ""));
        editEmail.setText(pref.getString("user_email", ""));

        btnSave.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            String newEmail = editEmail.getText().toString().trim();
            int userId = pref.getInt("user_id", -1);

            if (!newName.isEmpty() && !newEmail.isEmpty()) {
                updateProfileOnServer(newName, newEmail, userId);
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfileOnServer(String name, String email, int id) {
        // Change to your server IP (10.0.2.2 for Emulator)
        String url = "http://10.0.2.2/eduinsight_api/update_profile.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.trim().equalsIgnoreCase("success")) {
                        // Success! Update local session
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("user_name", name);
                        editor.putString("user_email", email);
                        editor.apply();

                        Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                        finish(); // Close activity and return to Dashboard
                    } else {
                        Toast.makeText(this, "Update Failed: " + response, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network Error: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(id));
                params.put("name", name);
                params.put("email", email);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}