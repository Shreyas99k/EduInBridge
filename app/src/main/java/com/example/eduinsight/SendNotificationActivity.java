package com.example.eduinsight;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SendNotificationActivity extends AppCompatActivity {

    private EditText etTitle, etMessage;
    private CheckBox cbStudents, cbTeachers;
    private Button btnSend;
    private int currentUserId;
    
    private RecyclerView rvSentHistory;
    private HistoryAdapter historyAdapter;
    private List<HistoryItem> historyList = new ArrayList<>();
    private TextView txtHistoryHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_notification);

        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUserId = pref.getInt("user_id", -1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etTitle = findViewById(R.id.etNotifTitle);
        etMessage = findViewById(R.id.etNotifMessage);
        cbStudents = findViewById(R.id.cbStudents);
        cbTeachers = findViewById(R.id.cbTeachers);
        btnSend = findViewById(R.id.btnSendNotif);
        
        txtHistoryHeader = findViewById(R.id.txtHistoryHeader);
        rvSentHistory = findViewById(R.id.rvSentHistory);
        rvSentHistory.setLayoutManager(new LinearLayoutManager(this));
        
        loadHistoryFromPrefs();
        historyAdapter = new HistoryAdapter(historyList);
        rvSentHistory.setAdapter(historyAdapter);

        btnSend.setOnClickListener(v -> sendNotification());
        updateHistoryVisibility();
    }

    private void sendNotification() {
        String title = etTitle.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Title and message are required", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean toStudents = cbStudents.isChecked();
        boolean toTeachers = cbTeachers.isChecked();

        if (!toStudents && !toTeachers) {
            Toast.makeText(this, "Please select at least one target group", Toast.LENGTH_SHORT).show();
            return;
        }

        String target = "all";
        if (toStudents && !toTeachers) target = "student";
        else if (!toStudents && toTeachers) target = "teacher";

        btnSend.setEnabled(false);
        btnSend.setText("Sending...");

        String url = "http://10.0.2.2/eduinsight_api/send_push.php"; 
        String finalTarget = target;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    btnSend.setEnabled(true);
                    btnSend.setText("Send Announcement");
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optString("status").equals("success")) {
                            Toast.makeText(this, "Notification Sent Successfully", Toast.LENGTH_LONG).show();
                            saveToHistory(title, message, finalTarget);
                            etTitle.setText("");
                            etMessage.setText("");
                        } else {
                            Toast.makeText(this, json.optString("message", "Failed to send"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("NOTIF", "Error: " + response);
                        Toast.makeText(this, "Server Response Error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    btnSend.setEnabled(true);
                    btnSend.setText("Send Announcement");
                    Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("title", title);
                params.put("message", message);
                params.put("target", finalTarget);
                params.put("type", "broadcast");
                params.put("sender_id", String.valueOf(currentUserId));
                params.put("sender_role", "institution");
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void saveToHistory(String title, String message, String target) {
        String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
        HistoryItem newItem = new HistoryItem(title, message, time, target);
        
        historyList.add(0, newItem);
        if (historyList.size() > 10) {
            historyList.remove(10);
        }
        
        historyAdapter.notifyDataSetChanged();
        saveHistoryToPrefs();
        updateHistoryVisibility();
    }

    private void loadHistoryFromPrefs() {
        SharedPreferences pref = getSharedPreferences("SentNotificationsHistory", MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray(pref.getString("list", "[]"));
            historyList.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                historyList.add(new HistoryItem(
                        obj.getString("title"),
                        obj.getString("message"),
                        obj.getString("time"),
                        obj.getString("target")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveHistoryToPrefs() {
        SharedPreferences pref = getSharedPreferences("SentNotificationsHistory", MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray();
            for (HistoryItem item : historyList) {
                JSONObject obj = new JSONObject();
                obj.put("title", item.title);
                obj.put("message", item.message);
                obj.put("time", item.time);
                obj.put("target", item.target);
                arr.put(obj);
            }
            pref.edit().putString("list", arr.toString()).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateHistoryVisibility() {
        if (historyList.isEmpty()) {
            txtHistoryHeader.setVisibility(View.GONE);
        } else {
            txtHistoryHeader.setVisibility(View.VISIBLE);
        }
    }

    private static class HistoryItem {
        String title, message, time, target;
        HistoryItem(String t, String m, String tm, String tr) {
            title = t; message = m; time = tm; target = tr;
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<HistoryItem> items;
        HistoryAdapter(List<HistoryItem> items) { this.items = items; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_notification, p, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            HistoryItem it = items.get(p);
            h.title.setText(it.title);
            h.body.setText(it.message);
            h.time.setText(it.time + " • Sent to " + it.target);
            
            h.itemView.setOnClickListener(v -> {
                new AlertDialog.Builder(SendNotificationActivity.this)
                        .setTitle(it.title)
                        .setMessage(it.message)
                        .setPositiveButton("Close", null)
                        .show();
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, body, time;
            ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.txtNotifTitle);
                body = v.findViewById(R.id.txtNotifBody);
                time = v.findViewById(R.id.txtNotifTime);
            }
        }
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
