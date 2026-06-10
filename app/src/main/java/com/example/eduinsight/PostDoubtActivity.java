package com.example.eduinsight;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostDoubtActivity extends AppCompatActivity {

    private static final String TAG = "PostDoubtActivity";
    private AutoCompleteTextView spinnerDepartment, spinnerSubject, autoCompleteTeacher;
    private EditText editDoubtDescription;
    private Button btnSubmitDoubt;

    private final List<JSONObject> allDepartments = new ArrayList<>();
    private final List<JSONObject> allSubjects = new ArrayList<>();
    private final List<JSONObject> allTeachers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_doubt);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Post Doubt");
        }

        spinnerDepartment = findViewById(R.id.spinnerBranch);
        spinnerSubject = findViewById(R.id.spinnerSubject);
        autoCompleteTeacher = findViewById(R.id.autoCompleteTeacher);
        editDoubtDescription = findViewById(R.id.editDoubtDescription);
        btnSubmitDoubt = findViewById(R.id.btnSubmitDoubt);

        setupDropdownBehavior(spinnerDepartment);
        setupDropdownBehavior(spinnerSubject);
        setupDropdownBehavior(autoCompleteTeacher);

        fetchDropdownData();

        if (btnSubmitDoubt != null) {
            applyGlassTouch(btnSubmitDoubt);
            btnSubmitDoubt.setOnClickListener(v -> submitDoubtLogic());
        }
    }

    private void setupDropdownBehavior(AutoCompleteTextView view) {
        if (view == null) return;
        view.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) view.showDropDown(); });
        view.setOnClickListener(v -> view.showDropDown());
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) view.showDropDown();
            return false;
        });
    }

    private void fetchDropdownData() {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int institutionId = pref.getInt("institution_id", -1);

        if (institutionId == -1) {
            Toast.makeText(this, "Session error. Please logout and login again.", Toast.LENGTH_LONG).show();
            return;
        }

        String url = "http://10.0.2.2/eduinsight_api/get_dropdown_data.php?institution_id=" + institutionId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        allDepartments.clear();
                        allSubjects.clear();
                        allTeachers.clear();

                        if (jsonResponse.has("departments")) {
                            JSONArray deptArr = jsonResponse.getJSONArray("departments");
                            List<String> dNames = new ArrayList<>();
                            for (int i = 0; i < deptArr.length(); i++) {
                                JSONObject obj = deptArr.getJSONObject(i);
                                allDepartments.add(obj);
                                dNames.add(obj.optString("name", "Unknown"));
                            }
                            spinnerDepartment.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, dNames));
                        }

                        if (jsonResponse.has("subjects")) {
                            JSONArray subArr = jsonResponse.getJSONArray("subjects");
                            for (int i = 0; i < subArr.length(); i++) allSubjects.add(subArr.getJSONObject(i));
                        }

                        if (jsonResponse.has("teachers_full")) {
                            JSONArray teacherArr = jsonResponse.getJSONArray("teachers_full");
                            for (int i = 0; i < teacherArr.length(); i++) allTeachers.add(teacherArr.getJSONObject(i));
                        } else if (jsonResponse.has("teachers")) {
                            JSONArray teacherArr = jsonResponse.getJSONArray("teachers");
                            for (int i = 0; i < teacherArr.length(); i++) {
                                JSONObject t = new JSONObject();
                                t.put("name", teacherArr.getString(i));
                                allTeachers.add(t);
                            }
                        }

                        setupSelectionListeners();

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing Error: " + e.getMessage());
                        Log.e(TAG, "Response: " + response);
                        Toast.makeText(this, "Failed to load lists.", Toast.LENGTH_SHORT).show();
                    }
                }, error -> Toast.makeText(this, "Connection error.", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(request);
    }

    private void setupSelectionListeners() {
        spinnerDepartment.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDeptName = parent.getItemAtPosition(position).toString().trim();
            int deptId = -1;
            for (JSONObject d : allDepartments) {
                if (d.optString("name").trim().equalsIgnoreCase(selectedDeptName)) {
                    deptId = d.optInt("id");
                    break;
                }
            }
            updateSubjectAdapter(deptId);
            updateMentorAdapter(deptId, selectedDeptName);
        });
    }

    private void updateSubjectAdapter(int deptId) {
        List<String> filteredSubjects = new ArrayList<>();
        for (JSONObject s : allSubjects) {
            int sDeptId = s.optInt("department_id", -1);
            if (sDeptId == deptId) filteredSubjects.add(s.optString("subject_name"));
        }
        spinnerSubject.setText("");
        spinnerSubject.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filteredSubjects));
    }

    private void updateMentorAdapter(int deptId, String selectedDeptName) {
        List<JSONObject> filteredMentors = new ArrayList<>();
        for (JSONObject t : allTeachers) {
            int tDeptId = t.optInt("department_id", -1);
            String tBranch = t.optString("branch", "").trim();
            if ((deptId != -1 && tDeptId == deptId) || tBranch.equalsIgnoreCase(selectedDeptName)) {
                filteredMentors.add(t);
            }
        }
        autoCompleteTeacher.setText("");
        if (filteredMentors.isEmpty()) {
            autoCompleteTeacher.setAdapter(null);
            Toast.makeText(this, "No mentors found for " + selectedDeptName, Toast.LENGTH_SHORT).show();
        } else {
            TeacherStatusAdapter adapter = new TeacherStatusAdapter(this, filteredMentors);
            autoCompleteTeacher.setAdapter(adapter);
        }
    }

    private class TeacherStatusAdapter extends ArrayAdapter<JSONObject> {
        private List<JSONObject> teachers;

        public TeacherStatusAdapter(android.content.Context context, List<JSONObject> teachers) {
            super(context, R.layout.item_teacher_spinner, teachers);
            this.teachers = teachers;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_teacher_spinner, parent, false);
            }
            JSONObject teacher = getItem(position);
            TextView name = convertView.findViewById(R.id.txtTeacherName);
            View dot = convertView.findViewById(R.id.viewStatusDot);

            if (teacher != null) {
                name.setText(teacher.optString("name"));
                boolean isOnline = teacher.optInt("is_online", 0) == 1;
                dot.setBackgroundResource(isOnline ? R.drawable.status_dot_online : R.drawable.status_dot_offline);
            }
            return convertView;
        }

        @Override
        public int getCount() {
            return teachers.size();
        }

        @Nullable
        @Override
        public JSONObject getItem(int position) {
            return teachers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public android.widget.Filter getFilter() {
            return new android.widget.Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = teachers;
                    results.count = teachers.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    notifyDataSetChanged();
                }

                @Override
                public CharSequence convertResultToString(Object resultValue) {
                    return ((JSONObject) resultValue).optString("name");
                }
            };
        }
    }

    private void submitDoubtLogic() {
        String description = editDoubtDescription.getText().toString().trim();
        String department = spinnerDepartment.getText().toString().trim();
        String selectedSubject = spinnerSubject.getText().toString().trim();
        String assignedTeacher = autoCompleteTeacher.getText().toString().trim();

        if (description.isEmpty() || department.isEmpty() || selectedSubject.isEmpty() || assignedTeacher.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "http://10.0.2.2/eduinsight_api/post_doubt.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.trim().equalsIgnoreCase("success")) {
                        Toast.makeText(this, "Doubt Posted Successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else Toast.makeText(this, "Error: " + response, Toast.LENGTH_LONG).show();
                },
                error -> {
                    String msg = "Connect Error: Check server/Wi-Fi";
                    if (error.networkResponse != null) msg = "Server Error: " + error.networkResponse.statusCode;
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
                Map<String, String> params = new HashMap<>();
                params.put("student_id", String.valueOf(pref.getInt("user_id", -1)));
                params.put("student_name", pref.getString("name", "Unknown Student"));
                params.put("description", description);
                params.put("subject", selectedSubject); 
                params.put("department", department);
                params.put("teacher_name", assignedTeacher); 
                params.put("sender_id", String.valueOf(pref.getInt("user_id", -1)));
                params.put("sender_role", "student");
                return params;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(20000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(this).add(request);
    }

    private void applyGlassTouch(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.97f).scaleY(0.97f).alpha(0.8f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(150).start();
                    break;
            }
            return false;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
