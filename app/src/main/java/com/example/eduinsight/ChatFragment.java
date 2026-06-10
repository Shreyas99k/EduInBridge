package com.example.eduinsight;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Collections;
import java.util.List;

public class ChatFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // This fragment is now a hub to start new chats
        view.findViewById(R.id.btnSelectDoubt).setOnClickListener(v -> showDoubtDialog());

        return view;
    }

    private void showDoubtDialog() {
        if (getContext() == null) return;
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_select_doubt);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        
        RecyclerView rv = dialog.findViewById(R.id.rvDoubtList);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Fix: Cancel button click listener
        View btnCancel = dialog.findViewById(R.id.btnBack);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        SharedPreferences pref = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String url = "http://10.0.2.2/eduinsight_api/get_my_doubts.php?student_id=" + pref.getInt("user_id", -1);

        StringRequest req = new StringRequest(Request.Method.GET, url, response -> {
            try {
                JSONArray arr = new JSONArray(response);
                List<JSONObject> solvedDoubts = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    if (obj.optString("status").equalsIgnoreCase("solved")) {
                        solvedDoubts.add(obj);
                    }
                }

                // Sort/Filter: Take only the last 15 solved doubts
                // Assuming newest are at the end of the list, we reverse to show newest first and then limit
                Collections.reverse(solvedDoubts);
                
                int limit = Math.min(solvedDoubts.size(), 15);
                List<String> items = new ArrayList<>();
                List<String> stats = new ArrayList<>();
                List<Integer> ids = new ArrayList<>();
                List<String> tNames = new ArrayList<>();
                List<String> sNames = new ArrayList<>();

                for (int i = 0; i < limit; i++) {
                    JSONObject obj = solvedDoubts.get(i);
                    items.add(obj.optString("subject", "Doubt") + ": " + obj.optString("description", ""));
                    stats.add(obj.optString("status", "SOLVED"));
                    ids.add(obj.getInt("id"));
                    tNames.add(obj.optString("teacher_name", "Mentor"));
                    sNames.add(obj.optString("student_name", "Student"));
                }
                
                if (items.isEmpty()) {
                    Toast.makeText(getContext(), "No solved doubts available", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    return;
                }

                rv.setAdapter(new DoubtSelectionAdapter(items, stats, pos -> {
                    Intent intent = new Intent(getContext(), MentorshipChatActivity.class);
                    intent.putExtra("doubt_id", ids.get(pos));
                    intent.putExtra("student_name", sNames.get(pos));
                    intent.putExtra("teacher_name", tNames.get(pos));
                    intent.putExtra("description", items.get(pos));
                    intent.putExtra("mode", "mentorship");
                    startActivity(intent);
                    dialog.dismiss();
                }));
            } catch (Exception e) { 
                Log.e("CHAT", "Dialog Error", e); 
                Toast.makeText(getContext(), "Error loading doubts", Toast.LENGTH_SHORT).show();
            }
        }, error -> {
            Log.e("CHAT", "Volley Error: " + error.getMessage());
            Toast.makeText(getContext(), "Connection error", Toast.LENGTH_SHORT).show();
        });
        Volley.newRequestQueue(requireContext()).add(req);
        dialog.show();
    }
}
