package com.example.eduinsight;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MentorAdapter extends RecyclerView.Adapter<MentorAdapter.ViewHolder> {

    private List<TeacherMentor> mentorList;

    public MentorAdapter(List<TeacherMentor> mentorList) {
        this.mentorList = mentorList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mentor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TeacherMentor mentor = mentorList.get(position);
        holder.txtName.setText(mentor.getName());
        holder.txtEmail.setText(mentor.getEmail());
        holder.txtMobile.setText(mentor.getMobile() == null || mentor.getMobile().equals("null") ? "No mobile linked" : mentor.getMobile());
        holder.txtDepartment.setText(mentor.getDepartment());
    }

    @Override
    public int getItemCount() {
        return mentorList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtEmail, txtMobile, txtDepartment;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtMentorName);
            txtEmail = itemView.findViewById(R.id.txtMentorEmail);
            txtMobile = itemView.findViewById(R.id.txtMentorMobile);
            txtDepartment = itemView.findViewById(R.id.txtMentorDepartment);
        }
    }
}
