package com.example.eduinsight;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import com.google.android.material.card.MaterialCardView;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherDoubtAdapter extends RecyclerView.Adapter<TeacherDoubtAdapter.ViewHolder> {

    private List<JSONObject> doubtList;
    private Context context;
    private String mode; 
    private List<Integer> selectedItems = new ArrayList<>();
    private boolean isSelectionMode = false;
    private OnSelectionChangedListener selectionListener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public TeacherDoubtAdapter(Context context, List<JSONObject> doubtList, String mode) {
        this(context, doubtList, mode, null);
    }

    public TeacherDoubtAdapter(Context context, List<JSONObject> doubtList, String mode, OnSelectionChangedListener listener) {
        this.context = context;
        this.doubtList = doubtList;
        this.mode = mode;
        this.selectionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_teacher_doubt, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject doubt = doubtList.get(position);
        try {
            int doubtId = doubt.optInt("id", -1);
            String studentName = doubt.optString("student_name", "Student");
            String teacherName = doubt.optString("teacher_name", "Mentor");
            String description = doubt.optString("description", "No description");
            String status = doubt.optString("status", "pending");
            int teacherRepliedCount = doubt.optInt("teacher_replied_count", 0);
            boolean hasNewMessage = doubt.optInt("has_new_message", 0) == 1;

            holder.txtName.setText(studentName);
            
            // Show last message if available, else description
            String lastMsg = doubt.optString("last_message", "");
            if (lastMsg.isEmpty() || lastMsg.equals("null")) {
                holder.txtDesc.setText(description);
            } else {
                holder.txtDesc.setText(lastMsg);
            }

            // Set Student Initial
            if (studentName.length() > 0 && holder.txtInitial != null) {
                holder.txtInitial.setText(studentName.substring(0, 1).toUpperCase());
            }
            
            if (holder.unreadIndicator != null) {
                holder.unreadIndicator.setVisibility(hasNewMessage ? View.VISIBLE : View.GONE);
            }

            if (selectedItems.contains(doubtId)) {
                holder.itemView.setBackgroundResource(R.drawable.glass_item_selector);
            } else {
                holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            }

            View.OnClickListener openChat = v -> {
                if (isSelectionMode) {
                    toggleSelection(doubtId);
                } else {
                    Intent intent = new Intent(context, MentorshipChatActivity.class);
                    intent.putExtra("doubt_id", doubtId);
                    intent.putExtra("student_name", studentName);
                    intent.putExtra("teacher_name", teacherName);
                    intent.putExtra("description", description);
                    intent.putExtra("mode", mode.equals("archive") ? "read_only" : "mentorship");
                    context.startActivity(intent);
                }
            };

            holder.itemView.setOnClickListener(openChat);
            
            holder.itemView.setOnLongClickListener(v -> {
                if (mode.equals("mentorship") || mode.equals("archive")) {
                    isSelectionMode = true;
                    toggleSelection(doubtId);
                    return true;
                }
                return false;
            });

            if (mode.equals("queue")) {
                if (teacherRepliedCount > 0) {
                    holder.txtAction.setText("SOLVE");
                    holder.btnActionContainer.setCardBackgroundColor(Color.parseColor("#DCFCE7")); // Green background
                    holder.txtAction.setTextColor(Color.parseColor("#15803D")); // Dark Green text
                    holder.btnActionContainer.setOnClickListener(v -> markAsSolvedApi(doubtId, "Resolved via Discussion", position));
                } else {
                    holder.txtAction.setText("OPEN");
                    holder.btnActionContainer.setCardBackgroundColor(Color.parseColor("#EEF2FF")); // Default indigo
                    holder.txtAction.setTextColor(Color.parseColor("#4F46E5"));
                    holder.btnActionContainer.setOnClickListener(openChat);
                }
                holder.btnActionContainer.setVisibility(View.VISIBLE);
            } else {
                holder.txtAction.setText(mode.equals("mentorship") ? "CHAT" : "VIEW");
                holder.btnActionContainer.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);
                holder.btnActionContainer.setOnClickListener(openChat);
                
                if (mode.equals("archive")) {
                    holder.btnActionContainer.setCardBackgroundColor(Color.parseColor("#E0E7FF")); 
                    holder.txtAction.setTextColor(Color.parseColor("#4338CA"));
                } else {
                    holder.btnActionContainer.setCardBackgroundColor(Color.parseColor("#DCFCE7")); 
                    holder.txtAction.setTextColor(Color.parseColor("#15803D"));
                }
            }

            holder.txtStatusBadge.setText(status.toUpperCase());
            if (status.equalsIgnoreCase("solved")) {
                holder.txtStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DCFCE7"))); // Light green
                holder.txtStatusBadge.setTextColor(Color.parseColor("#16A34A")); // Dark green
            } else {
                holder.txtStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2"))); // Light red
                holder.txtStatusBadge.setTextColor(Color.parseColor("#DC2626")); // Dark red
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void toggleSelection(int id) {
        if (selectedItems.contains(id)) {
            selectedItems.remove(Integer.valueOf(id));
        } else {
            selectedItems.add(id);
        }
        if (selectedItems.isEmpty()) isSelectionMode = false;
        notifyDataSetChanged();
        if (selectionListener != null) selectionListener.onSelectionChanged(selectedItems.size());
    }

    public List<Integer> getSelectedIds() { return selectedItems; }

    public void clearSelection() {
        selectedItems.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
    }

    private void markAsSolvedApi(int doubtId, String solution, int position) {
        String url = "http://10.0.2.2/eduinsight_api/mark_solved.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.trim().equalsIgnoreCase("success")) {
                        doubtList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, doubtList.size());
                        Toast.makeText(context, "Resolved!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Error: " + response, Toast.LENGTH_SHORT).show();
                    }
                }, error -> Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("doubt_id", String.valueOf(doubtId));
                p.put("solution", solution);
                return p;
            }
        };
        Volley.newRequestQueue(context).add(request);
    }

    @Override
    public int getItemCount() { return doubtList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtDesc, txtStatusBadge, txtAction, txtInitial;
        View unreadIndicator;
        MaterialCardView btnActionContainer;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtStudentName);
            txtDesc = itemView.findViewById(R.id.txtDescription);
            txtStatusBadge = itemView.findViewById(R.id.txtStatusBadge);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            txtAction = itemView.findViewById(R.id.btnSolve);
            btnActionContainer = itemView.findViewById(R.id.btnSolveContainer);
            txtInitial = itemView.findViewById(R.id.txtStudentInitial);
        }
    }
}
