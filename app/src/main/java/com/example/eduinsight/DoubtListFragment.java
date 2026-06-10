package com.example.eduinsight;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class DoubtListFragment extends Fragment {

    private static final String ARG_STATUS = "status";
    private String statusFilter;
    private RecyclerView recyclerView;
    private DoubtAdapter adapter;
    private List<Doubt> doubtList = new ArrayList<>();
    private TextView emptyState;

    public static DoubtListFragment newInstance(String status) {
        DoubtListFragment fragment = new DoubtListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS, status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            statusFilter = getArguments().getString(ARG_STATUS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doubt_list, container, false);
        recyclerView = view.findViewById(R.id.rvDoubtList);
        emptyState = view.findViewById(R.id.emptyState);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DoubtAdapter(getContext(), doubtList, true);
        recyclerView.setAdapter(adapter);

        fetchDoubts();
        return view;
    }

    private void fetchDoubts() {
        if (getContext() == null) return;
        SharedPreferences pref = getContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        int studentId = pref.getInt("user_id", -1);

        if (studentId == -1) return;

        String url = "http://10.0.2.2/eduinsight_api/get_my_doubts.php?student_id=" + studentId;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        doubtList.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            String status = obj.optString("status", "pending");
                            
                            boolean matches = false;
                            if (statusFilter.equalsIgnoreCase("pending")) {
                                matches = !status.equalsIgnoreCase("solved");
                            } else {
                                matches = status.equalsIgnoreCase("solved");
                            }

                            if (matches) {
                                doubtList.add(new Doubt(
                                        obj.getInt("id"),
                                        obj.optString("student_name", "Student"),
                                        obj.optString("teacher_name", "Mentor"),
                                        obj.optString("subject", "General"),
                                        obj.getString("description"),
                                        status,
                                        obj.optString("solution", ""),
                                        obj.optString("sol_image", ""),
                                        obj.optString("sol_audio", ""),
                                        obj.optInt("rating", 0)
                                ));
                            }
                        }
                        adapter.notifyDataSetChanged();
                        
                        if (doubtList.isEmpty()) {
                            emptyState.setVisibility(View.VISIBLE);
                            emptyState.setText("No " + statusFilter + " doubts found.");
                        } else {
                            emptyState.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        Log.e("DOUBT_LIST", "Parse error: " + e.getMessage());
                    }
                }, error -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
        );
        Volley.newRequestQueue(getContext()).add(request);
    }
}
