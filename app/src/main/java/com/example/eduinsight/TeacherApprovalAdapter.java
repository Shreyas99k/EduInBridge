package com.example.eduinsight;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TeacherApprovalAdapter extends RecyclerView.Adapter<TeacherApprovalAdapter.ViewHolder> {

    private List<TeacherApproval> approvalList;
    private OnApprovalActionListener listener;

    public interface OnApprovalActionListener {
        void onApprove(TeacherApproval teacher);
        void onReject(TeacherApproval teacher);
    }

    public TeacherApprovalAdapter(List<TeacherApproval> approvalList, OnApprovalActionListener listener) {
        this.approvalList = approvalList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher_approval, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TeacherApproval teacher = approvalList.get(position);
        holder.txtName.setText(teacher.getName());
        holder.txtEmail.setText(teacher.getEmail());
        holder.txtMobile.setText(teacher.getMobile().isEmpty() || teacher.getMobile().equals("null") ? "Not Provided" : teacher.getMobile());
        holder.txtDepartment.setText(teacher.getDepartment());

        holder.btnApprove.setOnClickListener(v -> listener.onApprove(teacher));
        holder.btnReject.setOnClickListener(v -> listener.onReject(teacher));
    }

    @Override
    public int getItemCount() {
        return approvalList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtEmail, txtMobile, txtDepartment;
        Button btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtTeacherName);
            txtEmail = itemView.findViewById(R.id.txtTeacherEmail);
            txtMobile = itemView.findViewById(R.id.txtTeacherMobile);
            txtDepartment = itemView.findViewById(R.id.txtTeacherDepartment);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
