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

public class TeacherArchiveActivity extends AppCompatActivity implements TeacherDoubtAdapter.OnSelectionChangedListener {

    private RecyclerView rvArchive;
    private TeacherDoubtAdapter adapter;
    private List<JSONObject> solvedList = new ArrayList<>();
    private Menu optionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_archive);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Doubt Archives");
        }

        rvArchive = findViewById(R.id.rvSolvedArchive);
        rvArchive.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new TeacherDoubtAdapter(this, solvedList, "archive", this);
        rvArchive.setAdapter(adapter);

        // Fetch initially
        fetchSolvedDoubts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchSolvedDoubts();
    }

    private void fetchSolvedDoubts() {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String teacherName = pref.getString("name", "");
        String url = "http://10.0.2.2/eduinsight_api/get_teacher_doubts.php?teacher_name=" + android.net.Uri.encode(teacherName) + "&status=solved";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getString("status").equals("success")) {
                            solvedList.clear();
                            JSONArray arr = response.getJSONArray("doubts");
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                if (obj.optString("status").equalsIgnoreCase("solved")) {
                                    solvedList.add(obj);
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }
                    } catch (JSONException e) {
                        Log.e("ARCHIVE_ERR", "Parse error: " + e.getMessage());
                    }
                }, error -> Log.e("ARCHIVE_ERR", "Volley network error"));

        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        getMenuInflater().inflate(R.menu.selection_menu, menu);
        menu.findItem(R.id.action_delete).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            deleteSelectedArchives();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteSelectedArchives() {
        List<Integer> ids = adapter.getSelectedIds();
        if (ids.isEmpty()) return;

        String url = "http://10.0.2.2/eduinsight_api/delete_doubts.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(this, "Archives deleted successfully", Toast.LENGTH_SHORT).show();
                    adapter.clearSelection();
                    onSelectionChanged(0);
                    fetchSolvedDoubts();
                }, error -> Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
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
                getSupportActionBar().setTitle(count > 0 ? count + " Selected" : "Doubt Archives");
            }
        }
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
