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

public class TeacherDoubtListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TeacherDoubtAdapter adapter;
    private final List<JSONObject> doubtList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_doubt_list);

        Toolbar toolbar = findViewById(R.id.toolbarDoubts);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Activity Queue");
        }

        recyclerView = findViewById(R.id.recyclerTeacherDoubts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TeacherDoubtAdapter(this, doubtList, "queue");
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchDoubts();
    }

    private void fetchDoubts() {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String teacherName = pref.getString("name", "").trim();

        if (teacherName.isEmpty()) {
            Toast.makeText(this, "Session error: Teacher name not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Encode the URL to handle spaces in names
        String url = "http://10.0.2.2/eduinsight_api/get_teacher_doubts.php?teacher_name=" + android.net.Uri.encode(teacherName);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.optString("status").equals("success")) {
                            JSONArray array = obj.optJSONArray("doubts");
                            doubtList.clear();
                            if (array != null) {
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject doubt = array.getJSONObject(i);
                                    if (!doubt.optString("status").equalsIgnoreCase("solved")) {
                                        doubtList.add(doubt);
                                    }
                                }
                            }
                            adapter.notifyDataSetChanged();
                            if (doubtList.isEmpty()) {
                                Toast.makeText(this, "No doubts found for: " + teacherName, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Server Error: " + obj.optString("message"), Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e("QUEUE", "Parse Error: " + e.getMessage());
                        Toast.makeText(this, "Invalid Data Received", Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
                    String msg = "Network Error";
                    if (error.networkResponse != null) msg += " (Code: " + error.networkResponse.statusCode + ")";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });

        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
