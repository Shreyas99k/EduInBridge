package com.example.eduinsight;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DoubtAdapter extends RecyclerView.Adapter<DoubtAdapter.ViewHolder> {

    private final List<Doubt> doubtList;
    private final Context context;
    private final boolean isArchiveMode;

    public DoubtAdapter(Context context, List<Doubt> list, boolean isArchiveMode) {
        this.context = context;
        this.doubtList = list;
        this.isArchiveMode = isArchiveMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doubt, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Doubt d = doubtList.get(position);
        
        // Use Subject as the main header for students
        holder.txtStudentName.setText(d.getSubject() != null ? d.getSubject().toUpperCase() : "GENERAL QUERY");
        holder.txtDesc.setText(d.getDescription());
        holder.txtStatus.setText(d.getStatus().toUpperCase());

        // HIGH VISIBILITY COLORS (Dark text for White background)
        holder.txtStudentName.setTextColor(Color.parseColor("#6366F1")); // Indigo header
        holder.txtDesc.setTextColor(Color.parseColor("#0F172A")); // Very dark blue/black
        holder.txtTapHint.setTextColor(Color.parseColor("#64748B")); // Muted slate
        holder.dividerLine.setBackgroundColor(Color.parseColor("#F1F5F9")); // Subtle gray divider

        boolean isSolved = d.getStatus().equalsIgnoreCase("solved");

        if (isSolved) {
            holder.txtStatus.setBackgroundResource(R.drawable.status_pill_solved);
            holder.txtStatus.setTextColor(Color.parseColor("#16A34A")); // Green text
            holder.txtStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DCFCE7"))); // Light green background
            holder.txtStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.layoutRatingSection.setVisibility(View.VISIBLE);
            updateRatingUI(holder, d.getRating());
            setupRatingClickListeners(holder, d);
        } else {
            holder.txtStatus.setBackgroundResource(R.drawable.status_pill_pending);
            holder.txtStatus.setTextColor(Color.parseColor("#DC2626")); // Red text
            holder.txtStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2"))); // Light red background
            holder.txtStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.layoutRatingSection.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSolved && !isArchiveMode) {
                // If it's a solved doubt being viewed from the select list, activate mentorship
                startMentorship(d);
            } else {
                Intent intent = new Intent(context, MentorshipChatActivity.class);
                intent.putExtra("doubt_id", d.getId());
                intent.putExtra("student_name", d.getName());
                intent.putExtra("teacher_name", d.getTeacherName());
                intent.putExtra("description", d.getDescription());
                intent.putExtra("mode", isArchiveMode ? "read_only" : "mentorship");
                context.startActivity(intent);
            }
        });
    }

    private void startMentorship(Doubt d) {
        String url = "http://10.0.2.2/eduinsight_api/start_mentorship.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    // Navigate to chat regardless, as backend marks it
                    Intent intent = new Intent(context, MentorshipChatActivity.class);
                    intent.putExtra("doubt_id", d.getId());
                    intent.putExtra("student_name", d.getName());
                    intent.putExtra("teacher_name", d.getTeacherName());
                    intent.putExtra("description", d.getDescription());
                    intent.putExtra("mode", "mentorship");
                    context.startActivity(intent);
                    
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).finish(); // Close selection list
                    }
                },
                error -> Toast.makeText(context, "Connection Error", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("doubt_id", String.valueOf(d.getId()));
                return params;
            }
        };
        Volley.newRequestQueue(context).add(request);
    }

    private void setupRatingClickListeners(ViewHolder holder, Doubt d) {
        ImageView[] dots = {holder.dot1, holder.dot2, holder.dot3, holder.dot4, holder.dot5};
        for (int i = 0; i < dots.length; i++) {
            final int ratingValue = i + 1;
            dots[i].setOnClickListener(v -> submitRating(d, ratingValue, holder));
        }
    }

    private void submitRating(Doubt d, int rating, ViewHolder holder) {
        if (d.getRating() > 0) {
            Toast.makeText(context, "Rating already done", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = "http://10.0.2.2/eduinsight_api/submit_rating.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.trim().equalsIgnoreCase("success")) {
                        d.setRating(rating);
                        updateRatingUI(holder, rating);
                        Toast.makeText(context, "Rating submitted!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Failed to submit rating", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(context, "Connection Error", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("doubt_id", String.valueOf(d.getId()));
                params.put("rating", String.valueOf(rating));
                return params;
            }
        };
        Volley.newRequestQueue(context).add(request);
    }

    private void updateRatingUI(ViewHolder holder, int rating) {
        ImageView[] dots = {holder.dot1, holder.dot2, holder.dot3, holder.dot4, holder.dot5};
        for (int i = 0; i < 5; i++) {
            if (dots[i] != null) {
                if (i < rating) {
                    dots[i].setImageResource(android.R.drawable.btn_star_big_on);
                    dots[i].setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFD60A")));
                } else {
                    dots[i].setImageResource(android.R.drawable.btn_star_big_off);
                    dots[i].setImageTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
                }
            }
        }
    }

    @Override
    public int getItemCount() { return (doubtList != null) ? doubtList.size() : 0; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDesc, txtStatus, txtTapHint, txtStudentName, txtSubmitRating;
        MaterialButton btnView;
        View layoutRatingSection, dividerLine;
        ImageView dot1, dot2, dot3, dot4, dot5;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDesc = itemView.findViewById(R.id.tvDescription);
            txtStatus = itemView.findViewById(R.id.tvStatus);
            txtTapHint = itemView.findViewById(R.id.txtTapHint);
            txtStudentName = itemView.findViewById(R.id.tvStudentName);
            txtSubmitRating = itemView.findViewById(R.id.txtSubmitRating);
            btnView = itemView.findViewById(R.id.btnViewSolution);
            layoutRatingSection = itemView.findViewById(R.id.layoutRatingSection);
            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);
            dot4 = itemView.findViewById(R.id.dot4);
            dot5 = itemView.findViewById(R.id.dot5);
            dividerLine = itemView.findViewById(R.id.dividerLine);
        }
    }
}
