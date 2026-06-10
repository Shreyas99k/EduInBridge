package com.example.eduinsight;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherApprovalsActivity extends AppCompatActivity {

    private RecyclerView rvApprovals;
    private TeacherApprovalAdapter adapter;
    private List<TeacherApproval> approvalList = new ArrayList<>();
    private int institutionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_approvals);

        Toolbar toolbar = findViewById(R.id.toolbarApprovals);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        institutionId = pref.getInt("user_id", -1);

        rvApprovals = findViewById(R.id.rvApprovals);
        rvApprovals.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new TeacherApprovalAdapter(approvalList, new TeacherApprovalAdapter.OnApprovalActionListener() {
            @Override
            public void onApprove(TeacherApproval teacher) {
                updateTeacherStatus(teacher.getId(), "active");
            }

            @Override
            public void onReject(TeacherApproval teacher) {
                updateTeacherStatus(teacher.getId(), "rejected");
            }
        });
        rvApprovals.setAdapter(adapter);

        fetchPendingApprovals();
    }

    private void fetchPendingApprovals() {
        if (institutionId == -1) return;
        
        String url = "http://10.0.2.2/eduinsight_api/get_pending_teachers.php?institution_id=" + institutionId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray arr = new JSONArray(response);
                        approvalList.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            approvalList.add(new TeacherApproval(
                                    obj.getString("id"),
                                    obj.getString("name"),
                                    obj.getString("email"),
                                    obj.optString("mobile", "Not Provided"),
                                    obj.optString("branch", "N/A")
                            ));
                        }
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        Log.e("Approvals", "Error parsing response: " + response);
                    }
                }, error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show());
        Volley.newRequestQueue(this).add(request);
    }

    private void updateTeacherStatus(String teacherId, String status) {
        String url = "http://10.0.2.2/eduinsight_api/update_teacher_status.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.contains("success")) {
                        Toast.makeText(this, "Status updated to " + status, Toast.LENGTH_SHORT).show();
                        fetchPendingApprovals();
                    } else {
                        Toast.makeText(this, "Error: " + response, Toast.LENGTH_LONG).show();
                    }
                }, error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("teacher_id", teacherId);
                p.put("status", status);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
