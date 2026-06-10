package com.example.eduinsight;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.util.HashMap;
import java.util.Map;

public class AddCurriculumActivity extends AppCompatActivity {

    private EditText editDeptName, editSubName;
    private View layoutSubName, subjectsLabel;
    private Button btnSubmitAdd;
    private String institutionId;
    private int departmentId = -1;
    private String type;
    private boolean isFormatting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_curriculum);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        institutionId = String.valueOf(pref.getInt("user_id", -1));

        editDeptName = findViewById(R.id.editDeptName);
        editSubName = findViewById(R.id.editSubName);
        layoutSubName = findViewById(R.id.layoutSubName);
        subjectsLabel = findViewById(R.id.subjectsLabel);
        btnSubmitAdd = findViewById(R.id.btnSubmitAdd);

        type = getIntent().getStringExtra("type");
        departmentId = getIntent().getIntExtra("department_id", -1);
        String deptName = getIntent().getStringExtra("department_name");

        if ("subject".equals(type)) {
            setTitle("Add Subjects");
            editDeptName.setText(deptName);
            editDeptName.setEnabled(false);
            btnSubmitAdd.setText("ADD SUBJECTS");
        } else {
            setTitle("Add Department");
            editDeptName.setEnabled(true);
            btnSubmitAdd.setText("ADD DEPARTMENT");
        }

        // Always show subjects field
        if (subjectsLabel != null) subjectsLabel.setVisibility(View.VISIBLE);
        if (layoutSubName != null) layoutSubName.setVisibility(View.VISIBLE);
        if (editSubName != null) editSubName.setVisibility(View.VISIBLE);

        setupSerialNumbering();

        // Add smooth animations on load
        editDeptName.setAlpha(0.7f);
        editDeptName.animate().alpha(1f).setDuration(400).start();

        editSubName.setAlpha(0.7f);
        editSubName.animate().alpha(1f).setDuration(500).start();

        btnSubmitAdd.setScaleX(0.9f);
        btnSubmitAdd.setScaleY(0.9f);
        btnSubmitAdd.animate()
            .scaleX(1f).scaleY(1f)
            .setDuration(400)
            .start();

        btnSubmitAdd.setOnClickListener(v -> {
            // Add button press animation
            v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start();
            v.postDelayed(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                processCurriculum();
            }, 100);
        });
    }

    private void processCurriculum() {
        btnSubmitAdd.setEnabled(false);
        if ("subject".equals(type)) {
            submitSubjects();
        } else {
            addDepartment();
        }
    }

    private String getParsedSubjects() {
        if (editSubName == null) return "";
        String subjectsRaw = editSubName.getText().toString().trim();
        if (subjectsRaw.isEmpty()) return "";

        String[] lines = subjectsRaw.split("\n");
        StringBuilder bulkName = new StringBuilder();
        for (String line : lines) {
            String clean = line.replaceAll("^\\d+\\.\\s*", "").trim();
            if (!clean.isEmpty()) {
                if (bulkName.length() > 0) bulkName.append("|");
                bulkName.append(clean);
            }
        }
        return bulkName.toString();
    }

    private void addDepartment() {
        String deptName = editDeptName.getText().toString().trim();
        if (deptName.isEmpty()) {
            Toast.makeText(this, "Department name cannot be empty.", Toast.LENGTH_SHORT).show();
            btnSubmitAdd.setEnabled(true);
            return;
        }

        final String subjects = getParsedSubjects();

        String url = "http://10.0.2.2/eduinsight_api/update_curriculum.php";
        StringRequest request = new StringRequest(Request.Method.POST, url, response -> {
            try {
                Log.d("AddDept Response", response);

                // Try to extract success and department_id from JSON response
                boolean success = response.contains("\"success\":true") ||
                                response.contains("\"success\": true") ||
                                response.contains("success") && !response.contains("false");

                if (success) {
                    // Extract department_id from response
                    // Response format: {"success": true, "department_id": 123, ...}
                    int newDeptId = -1;

                    if (response.contains("\"department_id\"")) {
                        String[] parts = response.split("\"department_id\"");
                        if (parts.length > 1) {
                            String idPart = parts[1].split("[,}]")[0].replaceAll("[^0-9]", "");
                            if (!idPart.isEmpty()) {
                                newDeptId = Integer.parseInt(idPart);
                                departmentId = newDeptId;
                                Log.d("New Dept ID", String.valueOf(departmentId));
                            }
                        }
                    }

                    complete();
                } else {
                    Log.e("AddDept Error", "Response does not indicate success");
                    error();
                }
            } catch (Exception e) {
                Log.e("AddDept Exception", "Error parsing response: " + e.getMessage(), e);
                error();
            }
        }, volleyError -> {
            Log.e("AddDept VolleyError", "Network error: " + volleyError.getMessage(), volleyError);
            String errorMsg = "Sync Failed";
            if (volleyError.networkResponse != null) {
                Log.e("AddDept StatusCode", String.valueOf(volleyError.networkResponse.statusCode));
                errorMsg += " (Status: " + volleyError.networkResponse.statusCode + ")";
            }
            Toast.makeText(AddCurriculumActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            btnSubmitAdd.setEnabled(true);
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("institution_id", institutionId);
                p.put("action", "add");
                p.put("type", "department");
                p.put("name", deptName);
                if (!subjects.isEmpty()) {
                    p.put("subjects", subjects);
                }
                Log.d("AddDept Params", p.toString());
                return p;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    private void submitSubjects() {
        final String subjects = getParsedSubjects();
        if (subjects.isEmpty()) {
            Toast.makeText(this, "Subject names cannot be empty.", Toast.LENGTH_SHORT).show();
            btnSubmitAdd.setEnabled(true);
            return;
        }

        String url = "http://10.0.2.2/eduinsight_api/update_curriculum.php";
        StringRequest request = new StringRequest(Request.Method.POST, url, response -> {
            Log.d("SubmitSubjects Response", response);
            complete();
        }, volleyError -> {
            Log.e("SubmitSubjects VolleyError", "Network error: " + volleyError.getMessage(), volleyError);
            String errorMsg = "Sync Failed";
            if (volleyError.networkResponse != null) {
                Log.e("SubmitSubjects StatusCode", String.valueOf(volleyError.networkResponse.statusCode));
                errorMsg += " (Status: " + volleyError.networkResponse.statusCode + ")";
            }
            Toast.makeText(AddCurriculumActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            btnSubmitAdd.setEnabled(true);
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("institution_id", institutionId);
                p.put("action", "add");
                p.put("type", "subject");
                p.put("name", subjects);
                p.put("department_id", String.valueOf(departmentId));
                Log.d("SubmitSubjects Params", p.toString());
                return p;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    private void complete() {
        Toast.makeText(this, "Update Successful", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void error() {
        Toast.makeText(this, "Sync Failed", Toast.LENGTH_SHORT).show();
        btnSubmitAdd.setEnabled(true);
    }

    private void setupSerialNumbering() {
        if (editSubName == null) return;
        editSubName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isFormatting) return;
                if (count > before && s.toString().endsWith("\n")) {
                    isFormatting = true;
                    String[] lines = s.toString().split("\n", -1);
                    int lineCount = lines.length;
                    editSubName.append(lineCount + ". ");
                    isFormatting = false;
                }
            }
            @Override public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                if (s.length() > 0 && !s.toString().startsWith("1. ")) {
                    isFormatting = true;
                    s.insert(0, "1. ");
                    isFormatting = false;
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
