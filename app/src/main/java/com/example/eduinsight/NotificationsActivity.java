package com.example.eduinsight;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private final List<NotificationItem> notificationList = new ArrayList<>();
    private ExtendedFloatingActionButton btnClearAll;
    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        SharedPreferences userPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUserRole = userPref.getString("role", "").toLowerCase().trim();

        Toolbar toolbar = findViewById(R.id.toolbarNotifications);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(currentUserRole.equals("teacher") ? "Mentor Alerts" : "Student Alerts");
        }

        rvNotifications = findViewById(R.id.rvNotifications);
        btnClearAll = findViewById(R.id.btnClearAll);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        loadRealNotifications();
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                notificationList.remove(pos);
                adapter.notifyItemRemoved(pos);
                saveListToStorage();
                checkEmptyState();
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvNotifications);

        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> {
                if (notificationList.isEmpty()) return;
                notificationList.clear();
                adapter.notifyDataSetChanged();
                saveListToStorage();
                Toast.makeText(this, "Cleared all notifications", Toast.LENGTH_SHORT).show();
                checkEmptyState();
            });
        }
        checkEmptyState();
    }

    private void loadRealNotifications() {
        notificationList.clear();
        String prefName = currentUserRole.equals("teacher") ? "TeacherNotifications" : "StudentNotifications";
        SharedPreferences pref = getSharedPreferences(prefName, MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray(pref.getString("list", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                notificationList.add(new NotificationItem(
                        obj.optString("title", "System Alert"),
                        obj.optString("body", ""),
                        obj.optString("time", "Recent"),
                        obj.optString("content", obj.optString("body", ""))
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveListToStorage() {
        String prefName = currentUserRole.equals("teacher") ? "TeacherNotifications" : "StudentNotifications";
        SharedPreferences pref = getSharedPreferences(prefName, MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray();
            for (NotificationItem item : notificationList) {
                JSONObject obj = new JSONObject();
                obj.put("title", item.title);
                obj.put("body", item.body);
                obj.put("time", item.time);
                obj.put("content", item.content);
                arr.put(obj);
            }
            pref.edit().putString("list", arr.toString()).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void checkEmptyState() {
        if (btnClearAll != null) {
            if (notificationList.isEmpty()) btnClearAll.hide(); else btnClearAll.show();
        }
    }

    @Override public boolean onSupportNavigateUp() { 
        getOnBackPressedDispatcher().onBackPressed();
        return true; 
    }

    private static class NotificationItem {
        String title, body, time, content;
        NotificationItem(String t, String b, String tm, String c) { 
            this.title = t; 
            this.body = b; 
            this.time = tm; 
            this.content = c; 
        }
    }

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private final List<NotificationItem> items;
        
        NotificationAdapter(List<NotificationItem> items) { 
            this.items = items; 
        }
        
        @NonNull 
        @Override 
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_notification, p, false));
        }
        
        @Override 
        public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            NotificationItem it = items.get(p);
            h.title.setText(it.title);
            h.body.setText(it.body);
            h.time.setText(it.time);

            h.itemView.setOnClickListener(v -> {
                new AlertDialog.Builder(NotificationsActivity.this)
                        .setTitle(it.title)
                        .setMessage(it.content)
                        .setPositiveButton("Close", null)
                        .show();
            });
        }
        
        @Override 
        public int getItemCount() { 
            return items.size(); 
        }
        
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
}
