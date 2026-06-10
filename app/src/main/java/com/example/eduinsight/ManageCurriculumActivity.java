package com.example.eduinsight;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageCurriculumActivity extends AppCompatActivity {

    private static final String TAG = "ManageCurriculum_v4";
    private RecyclerView rvCurriculum;
    private CurriculumAdapter adapter;
    private final List<String> currentList = new ArrayList<>();
    private final List<JSONObject> fullDeptList = new ArrayList<>();
    private final List<String> subList = new ArrayList<>();
    private String institutionId;
    private int selectedDepartmentId = -1;
    private String selectedDepartmentName = "";
    private TextView txtSectionTitle;
    private MaterialButton btnFabAdd;
    private boolean isShowingDepartments = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_curriculum);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Robust institutionId retrieval logic
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String role = pref.getString("role", "");
        int idValue = pref.getInt("institution_id", -1);
        if (idValue == -1 || "institution".equalsIgnoreCase(role)) {
            idValue = pref.getInt("user_id", -1);
        }
        institutionId = String.valueOf(idValue);

        rvCurriculum = findViewById(R.id.rvCurriculum);
        txtSectionTitle = findViewById(R.id.txtSectionTitle);
        btnFabAdd = findViewById(R.id.btnFabAdd);

        rvCurriculum.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new CurriculumAdapter(currentList, this::onRemoveClick);
        adapter.setOnItemClickListener(item -> {
            if (isShowingDepartments) {
                for(JSONObject dept : fullDeptList) {
                    if(dept.optString("name").equals(item)){
                        // Check for multiple possible ID keys
                        selectedDepartmentId = dept.optInt("id", dept.optInt("dept_id", -1));
                        selectedDepartmentName = item;
                        break;
                    }
                }
                if (selectedDepartmentId != -1) {
                    isShowingDepartments = false;
                    fetchCurriculum();
                } else {
                    Toast.makeText(this, "Could not identify department ID", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        rvCurriculum.setAdapter(adapter);

        btnFabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCurriculumActivity.class);
            if (isShowingDepartments) {
                intent.putExtra("type", "department");
            } else {
                intent.putExtra("type", "subject");
                intent.putExtra("department_id", selectedDepartmentId);
                intent.putExtra("department_name", selectedDepartmentName);
            }
            startActivity(intent);
        });

        fetchCurriculum();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchCurriculum(); 
    }

    private void updateUI() {
        currentList.clear();

        // Fade out existing content
        rvCurriculum.animate().alpha(0.3f).setDuration(150).start();

        if (isShowingDepartments) {
            txtSectionTitle.setText("DEPARTMENTS");
            btnFabAdd.setText("ADD DEPARTMENT");
            for(JSONObject dept : fullDeptList) currentList.add(dept.optString("name"));
            adapter.setShowSerialNumbers(false);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Manage Curriculum");
        } else {
            txtSectionTitle.setText("SUBJECTS IN " + selectedDepartmentName);
            btnFabAdd.setText("ADD SUBJECT");
            currentList.addAll(subList);
            adapter.setShowSerialNumbers(true);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(selectedDepartmentName);
        }

        adapter.notifyDataSetChanged();

        // Fade in with animation
        rvCurriculum.setAlpha(0.3f);
        rvCurriculum.animate()
            .alpha(1f)
            .setDuration(400)
            .start();

        // Animate title
        txtSectionTitle.setAlpha(0.5f);
        txtSectionTitle.animate()
            .alpha(1f)
            .setDuration(300)
            .start();
    }

    private void onRemoveClick(String item) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to remove '" + item + "'? This cannot be undone.")
                .setPositiveButton("DELETE", (dialog, which) -> deleteItem(item))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (!isShowingDepartments) {
            isShowingDepartments = true;
            fetchCurriculum();
        } else {
            super.onBackPressed();
        }
    }

    private void fetchCurriculum() {
        String url = "http://10.0.2.2/eduinsight_api/get_curriculum.php?institution_id=" + institutionId;
        if (!isShowingDepartments && selectedDepartmentId != -1) {
            url += "&department_id=" + selectedDepartmentId;
        }

        Log.d(TAG, "Fetching: " + url);

        StringRequest request = new StringRequest(Request.Method.GET, url, response -> {
            try {
                Log.d(TAG, "Response: " + response);
                JSONObject json = new JSONObject(response);
                if (!json.optString("status", "").equals("success")) {
                    Log.e(TAG, "Server error: " + json.optString("message"));
                    return;
                }

                if (isShowingDepartments) {
                    fullDeptList.clear();
                    JSONArray depts = json.optJSONArray("departments");
                    if (depts != null) {
                        for (int i = 0; i < depts.length(); i++) fullDeptList.add(depts.getJSONObject(i));
                    }
                } else {
                    subList.clear();
                    JSONArray subs = json.optJSONArray("subjects");
                    if (subs != null) {
                        for (int i = 0; i < subs.length(); i++) {
                            // Support both String array and Object array (where name is expected)
                            Object obj = subs.get(i);
                            if (obj instanceof String) {
                                subList.add((String) obj);
                            } else if (obj instanceof JSONObject) {
                                subList.add(((JSONObject) obj).optString("name", "Unnamed Subject"));
                            }
                        }
                    } else {
                        Log.w(TAG, "No 'subjects' array found in response");
                    }
                }
                updateUI();
            } catch (Exception e) { 
                Log.e(TAG, "Parse Error", e);
                Toast.makeText(this, "Data Error", Toast.LENGTH_SHORT).show();
            }
        }, error -> {
            Log.e(TAG, "Network error: " + error.getMessage());
            Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
        });
        request.setShouldCache(false);
        Volley.newRequestQueue(this).add(request);
    }

    private void deleteItem(final String name) {
        final boolean deletingDept = isShowingDepartments;
        
        String url = "http://10.0.2.2/eduinsight_api/update_curriculum.php";
        StringRequest request = new StringRequest(Request.Method.POST, url, response -> {
            try {
                JSONObject json = new JSONObject(response);
                if (json.optString("status").equals("success")) {
                    Toast.makeText(this, "Entry Removed", Toast.LENGTH_SHORT).show();
                    fetchCurriculum();
                } else {
                    Toast.makeText(this, json.optString("message"), Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) { fetchCurriculum(); }
        }, error -> Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show()) {
            @Override protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("institution_id", institutionId);
                p.put("action", "delete");
                p.put("type", deletingDept ? "department" : "subject");
                p.put("name", name);
                if (!deletingDept) p.put("department_id", String.valueOf(selectedDepartmentId));
                return p;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
