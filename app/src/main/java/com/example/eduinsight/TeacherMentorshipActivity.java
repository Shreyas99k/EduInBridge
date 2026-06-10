package com.example.eduinsight;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherMentorshipActivity extends AppCompatActivity implements TeacherDoubtAdapter.OnSelectionChangedListener {

    private RecyclerView rvMentorship;
    private TeacherDoubtAdapter adapter;
    private List<JSONObject> chatList = new ArrayList<>();
    private Menu optionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_mentorship);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Student Chats");
        }

        rvMentorship = findViewById(R.id.rvMentorshipList);
        rvMentorship.setLayoutManager(new LinearLayoutManager(this));
        
        // mode "mentorship" allows active chat interaction
        adapter = new TeacherDoubtAdapter(this, chatList, "mentorship", this);
        rvMentorship.setAdapter(adapter);

        fetchRecentStudentChats();
    }

    private void fetchRecentStudentChats() {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String teacherName = pref.getString("name", "");
        String url = "http://10.0.2.2/eduinsight_api/get_recent_chats.php?teacher_name=" + android.net.Uri.encode(teacherName);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getString("status").equals("success")) {
                            chatList.clear();
                            JSONArray arr = response.getJSONArray("chats");
                            for (int i = 0; i < arr.length(); i++) {
                                chatList.add(arr.getJSONObject(i));
                            }
                            adapter.notifyDataSetChanged();
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }, error -> Log.e("MENTORSHIP", "Error fetching chats"));

        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        // Create a new menu resource named selection_menu.xml with a delete action
        getMenuInflater().inflate(R.menu.selection_menu, menu);
        menu.findItem(R.id.action_delete).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            deleteSelectedChats();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteSelectedChats() {
        List<Integer> ids = adapter.getSelectedIds();
        if (ids.isEmpty()) return;

        String url = "http://10.0.2.2/eduinsight_api/delete_doubts.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(this, "Chats deleted successfully", Toast.LENGTH_SHORT).show();
                    adapter.clearSelection();
                    onSelectionChanged(0);
                    fetchRecentStudentChats();
                }, error -> Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                // Sending comma separated IDs for the SQL 'IN' clause
                params.put("ids", ids.toString().replace("[", "").replace("]", ""));
                return params;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public void onSelectionChanged(int count) {
        if (optionsMenu != null) {
            optionsMenu.findItem(R.id.action_delete).setVisible(count > 0);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(count > 0 ? count + " Selected" : "Student Chats");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchRecentStudentChats();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (adapter.getSelectedIds().size() > 0) {
            adapter.clearSelection();
            onSelectionChanged(0);
            return true;
        }
        onBackPressed();
        return true;
    }
}
