package com.example.eduinsight;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MentorshipHistoryFragment extends Fragment implements MentorshipHistoryAdapter.OnSelectionChangedListener {

    private RecyclerView rvHistory;
    private MentorshipHistoryAdapter adapter;
    private final List<JSONObject> chatList = new ArrayList<>();
    private LinearLayout emptyState;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private Menu optionsMenu;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // FIXED: Using the new unique and polished layout
        View view = inflater.inflate(R.layout.fragment_mentorship_history, container, false);

        rvHistory = view.findViewById(R.id.rvGeneralList);
        emptyState = view.findViewById(R.id.chatPlaceholder);
        
        TextView txtEmptyTitle = view.findViewById(R.id.txtEmptyTitle);
        TextView txtEmptyDesc = view.findViewById(R.id.txtEmptyDesc);
        
        if (txtEmptyTitle != null) txtEmptyTitle.setText("Silence is Golden");
        if (txtEmptyDesc != null) txtEmptyDesc.setText("Your resolved mentorship sessions will appear here as a gallery of knowledge.");

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MentorshipHistoryAdapter(getContext(), chatList, this);
        rvHistory.setAdapter(adapter);

        startAutoRefresh();
        return view;
    }

    private void startAutoRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded() && (adapter == null || adapter.getSelectedIds().isEmpty())) {
                    fetchRecentChats();
                }
                handler.postDelayed(this, 5000); 
            }
        };
        handler.post(refreshRunnable);
    }

    private void fetchRecentChats() {
        if (!isAdded() || getContext() == null) return;
        
        SharedPreferences pref = getContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        int studentId = pref.getInt("user_id", -1);
        String url = "http://10.0.2.2/eduinsight_api/get_my_doubts.php?student_id=" + studentId;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        List<JSONObject> tempList = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            // Ensure we only show actual mentorship chats
                            if (obj.optInt("is_mentorship", 0) == 1) {
                                tempList.add(obj);
                            }
                        }

                        // FIXED: Removed client-side sorting by ID to respect the backend's "Latest Message" order (WhatsApp style)

                        chatList.clear();
                        chatList.addAll(tempList);
                        if (adapter != null) adapter.notifyDataSetChanged();

                        updateVisibility();

                    } catch (Exception e) { Log.e("HISTORY", "Parse Error"); }
                }, error -> Log.e("HISTORY", "Network Error"));

        if (getContext() != null) Volley.newRequestQueue(getContext()).add(request);
    }

    private void updateVisibility() {
        if (chatList.isEmpty()) {
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            if (rvHistory != null) rvHistory.setVisibility(View.GONE);
        } else {
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (rvHistory != null) rvHistory.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchRecentChats();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.selection_menu, menu);
        this.optionsMenu = menu;
        MenuItem deleteItem = menu.findItem(R.id.action_delete);
        if (deleteItem != null) deleteItem.setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            deleteSelectedChats();
            return true;
        }
        return false;
    }

    private void deleteSelectedChats() {
        if (adapter == null) return;
        List<Integer> ids = adapter.getSelectedIds();
        if (ids.isEmpty()) return;

        String url = "http://10.0.2.2/eduinsight_api/delete_doubts.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(getContext(), "Selected sessions cleared", Toast.LENGTH_SHORT).show();
                    adapter.clearSelection();
                    onSelectionChanged(0);
                    fetchRecentChats();
                }, error -> Toast.makeText(getContext(), "Request Failed", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("ids", ids.toString().replace("[", "").replace("]", ""));
                return p;
            }
        };
        if (getContext() != null) Volley.newRequestQueue(getContext()).add(request);
    }

    @Override
    public void onSelectionChanged(int count) {
        if (optionsMenu != null) {
            MenuItem deleteItem = optionsMenu.findItem(R.id.action_delete);
            if (deleteItem != null) deleteItem.setVisible(count > 0);
            
            if (getActivity() != null && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(count > 0 ? count + " Selected" : "Mentorship Hub");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
    }
}
