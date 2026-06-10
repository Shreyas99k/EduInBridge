package com.example.eduinsight;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

public class HistoryFragment extends Fragment {

    private DoubtAdapter adapter;
    private List<Doubt> doubtList = new ArrayList<>();
    private List<Doubt> filteredList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doubt_list, container, false);

        RecyclerView rv = view.findViewById(R.id.rvDoubtList);
        TextView emptyState = view.findViewById(R.id.emptyState);
        
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new DoubtAdapter(getContext(), filteredList, true);
            rv.setAdapter(adapter);
        }

        fetchDoubts(emptyState);

        return view;
    }

    private void fetchDoubts(TextView emptyState) {
        if (getContext() == null) return;
        SharedPreferences pref = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        int studentId = pref.getInt("user_id", -1);

        String url = "http://10.0.2.2/eduinsight_api/get_my_doubts.php?student_id=" + studentId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        doubtList.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);

                            // ONLY SHOW DOUBTS MARKED AS 'SOLVED' AND NOT YET IN RECENT CHATS
                            if (!obj.optString("status", "").equalsIgnoreCase("solved") || obj.optInt("is_mentorship", 0) == 1) {
                                continue;
                            }

                            doubtList.add(new Doubt(
                                    obj.getInt("id"),
                                    obj.optString("student_name", "Student"),
                                    obj.optString("teacher_name", "Mentor"),
                                    obj.optString("subject", "General"),
                                    obj.getString("description"),
                                    obj.getString("status"),
                                    obj.optString("solution", ""),
                                    obj.optString("sol_image", ""),
                                    obj.optString("sol_audio", ""),
                                    obj.optInt("rating", 0)
                            ));
                        }
                        filteredList.clear();
                        filteredList.addAll(doubtList);
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                        
                        if (filteredList.isEmpty()) {
                            if (emptyState != null) {
                                emptyState.setVisibility(View.VISIBLE);
                                emptyState.setText("No history found.");
                            }
                        } else {
                            if (emptyState != null) emptyState.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        Log.e("HISTORY_ERROR", "Parsing Error: " + e.getMessage());
                    }
                },
                error -> Log.e("VOLLEY_ERROR", error.toString())
        );
        Volley.newRequestQueue(requireContext()).add(request);
    }
}
