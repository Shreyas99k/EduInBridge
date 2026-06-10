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
import java.util.List;

public class ManageMentorsActivity extends AppCompatActivity {

    private static final String TAG = "ManageMentorsActivity";
    private RecyclerView rvMentors;
    private MentorAdapter adapter;
    private List<TeacherMentor> mentorList = new ArrayList<>();
    private String institutionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_mentors);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Mentors");
        }

        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        institutionId = String.valueOf(pref.getInt("user_id", -1));
        
        rvMentors = findViewById(R.id.rvMentors);
        rvMentors.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new MentorAdapter(mentorList);
        rvMentors.setAdapter(adapter);

        fetchRegisteredMentors();
    }

    private void fetchRegisteredMentors() {
        if (institutionId.equals("-1")) return;

        String url = "http://10.0.2.2/eduinsight_api/get_registered_mentors.php?institution_id=" + institutionId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray arr = new JSONArray(response);
                        mentorList.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            
                            // Try multiple possible keys for the department/branch/expertise
                            String dept = obj.optString("branch", "");
                            if (dept.isEmpty() || dept.equals("null")) {
                                dept = obj.optString("department", "");
                            }
                            if (dept.isEmpty() || dept.equals("null")) {
                                dept = obj.optString("expertise", "Not Provided");
                            }

                            mentorList.add(new TeacherMentor(
                                    obj.optString("id", ""),
                                    obj.optString("name", "Unknown"),
                                    obj.optString("email", ""),
                                    obj.optString("mobile", "N/A"),
                                    dept
                            ));
                        }
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        Log.e(TAG, "Error: " + e.getMessage());
                    }
                }, error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show());
        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
