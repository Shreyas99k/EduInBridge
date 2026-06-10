package com.example.eduinsight;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MentorshipHistoryAdapter extends RecyclerView.Adapter<MentorshipHistoryAdapter.ViewHolder> {

    private final List<JSONObject> chatList;
    private final Context context;
    private final List<Integer> selectedIds = new ArrayList<>();
    private boolean isSelectionMode = false;
    private final OnSelectionChangedListener selectionListener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public MentorshipHistoryAdapter(Context context, List<JSONObject> chatList, OnSelectionChangedListener listener) {
        this.context = context;
        this.chatList = chatList;
        this.selectionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_mentorship_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject chat = chatList.get(position);
        try {
            int doubtId = chat.getInt("id");
            String subject = chat.optString("subject", "Doubt #" + doubtId);
            String description = chat.getString("description");
            String studentName = chat.optString("student_name", "Student");
            String teacherName = chat.optString("teacher_name", "Mentor");
            boolean hasNew = chat.optInt("has_new_message", 0) == 1;
            int rating = chat.optInt("rating", 0);

            holder.txtSubject.setText(subject);
            
            // Show last message if available, else description
            String lastMsg = chat.optString("last_message", "");
            if (lastMsg.isEmpty() || lastMsg.equals("null")) {
                holder.txtLastMsg.setText(description);
            } else {
                holder.txtLastMsg.setText(lastMsg);
            }
            
            // Time formatting
            String rawTime = chat.optString("last_message_time", "");
            if (rawTime.isEmpty() || rawTime.equals("null")) rawTime = chat.optString("updated_at", "");
            if (rawTime.isEmpty() || rawTime.equals("null")) rawTime = chat.optString("created_at", "");
            
            if (!rawTime.isEmpty() && !rawTime.equals("null")) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm A", Locale.getDefault());
                    Date date = inputFormat.parse(rawTime);
                    holder.txtTime.setText(date != null ? outputFormat.format(date) : "Active");
                } catch (Exception e) { holder.txtTime.setText("Active"); }
            } else { holder.txtTime.setText("Active"); }

            if (hasNew) {
                holder.unreadDot.setVisibility(View.VISIBLE);
                holder.txtLastMsg.setTypeface(null, Typeface.BOLD);
            } else {
                holder.unreadDot.setVisibility(View.GONE);
                holder.txtLastMsg.setTypeface(null, Typeface.NORMAL);
            }

            updateRatingUI(holder, rating);

            final int dId = doubtId;
            View.OnClickListener dotClick = v -> {
                int newRating;
                int id = v.getId();
                if (id == R.id.dot1) newRating = 1;
                else if (id == R.id.dot2) newRating = 2;
                else if (id == R.id.dot3) newRating = 3;
                else if (id == R.id.dot4) newRating = 4;
                else newRating = 5;
                submitRating(dId, newRating, holder);
            };

            holder.dot1.setOnClickListener(dotClick);
            holder.dot2.setOnClickListener(dotClick);
            holder.dot3.setOnClickListener(dotClick);
            holder.dot4.setOnClickListener(dotClick);
            holder.dot5.setOnClickListener(dotClick);

            holder.itemView.setOnClickListener(v -> {
                if (isSelectionMode) toggleSelection(doubtId);
                else {
                    Intent intent = new Intent(context, MentorshipChatActivity.class);
                    intent.putExtra("doubt_id", doubtId);
                    intent.putExtra("student_name", studentName);
                    intent.putExtra("teacher_name", teacherName);
                    intent.putExtra("description", description);
                    intent.putExtra("mode", "mentorship");
                    context.startActivity(intent);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                isSelectionMode = true;
                toggleSelection(doubtId);
                return true;
            });

            if (selectedIds.contains(doubtId)) holder.itemView.setAlpha(0.5f);
            else holder.itemView.setAlpha(1.0f);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateRatingUI(ViewHolder holder, int rating) {
        int activeDotColor = Color.parseColor("#FFD60A");
        
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        int inactiveDotColor = typedValue.data;

        View[] dots = {holder.dot1, holder.dot2, holder.dot3, holder.dot4, holder.dot5};
        for (int i = 0; i < 5; i++) {
            if (rating > 0 && i < rating) {
                dots[i].getBackground().setTint(activeDotColor);
                dots[i].setAlpha(1.0f);
            } else {
                dots[i].getBackground().setTint(inactiveDotColor);
                dots[i].setAlpha(0.3f);
            }
        }
    }

    private void submitRating(int doubtId, int rating, ViewHolder holder) {
        String url = "http://10.0.2.2/eduinsight_api/submit_rating.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.trim().equals("success")) {
                        updateRatingUI(holder, rating);
                        Toast.makeText(context, "Feedback shared!", Toast.LENGTH_SHORT).show();
                    }
                }, null) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("doubt_id", String.valueOf(doubtId));
                p.put("rating", String.valueOf(rating));
                return p;
            }
        };
        Volley.newRequestQueue(context).add(request);
    }

    private void toggleSelection(int id) {
        if (selectedIds.contains(id)) selectedIds.remove(Integer.valueOf(id));
        else selectedIds.add(id);
        if (selectedIds.isEmpty()) isSelectionMode = false;
        notifyDataSetChanged();
        if (selectionListener != null) selectionListener.onSelectionChanged(selectedIds.size());
    }

    public List<Integer> getSelectedIds() {
        return selectedIds;
    }

    public void clearSelection() {
        selectedIds.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return chatList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtSubject, txtLastMsg, txtTime;
        View unreadDot, dot1, dot2, dot3, dot4, dot5;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtSubject = itemView.findViewById(R.id.txtSubject);
            txtLastMsg = itemView.findViewById(R.id.txtLastMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
            unreadDot = itemView.findViewById(R.id.unreadDot);
            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);
            dot4 = itemView.findViewById(R.id.dot4);
            dot5 = itemView.findViewById(R.id.dot5);
        }
    }
}
