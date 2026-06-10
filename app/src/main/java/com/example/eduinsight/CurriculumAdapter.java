package com.example.eduinsight;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CurriculumAdapter extends RecyclerView.Adapter<CurriculumAdapter.ViewHolder> {

    private final List<String> items;
    private final OnDeleteClickListener deleteListener;
    private OnItemClickListener itemClickListener;
    private boolean showSerialNumbers = false;

    public interface OnDeleteClickListener {
        void onDelete(String item);
    }

    public interface OnItemClickListener {
        void onItemClick(String item);
    }

    public CurriculumAdapter(List<String> items, OnDeleteClickListener deleteListener) {
        this.items = items;
        this.deleteListener = deleteListener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setShowSerialNumbers(boolean show) {
        this.showSerialNumbers = show;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_curriculum, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = items.get(position);
        String displayText = showSerialNumbers ? (position + 1) + ". " + item : item;
        holder.txtItemName.setText(displayText);
        
        // Smooth animation for items
        holder.itemView.setAlpha(0.7f);
        holder.itemView.setScaleX(0.95f);
        holder.itemView.setScaleY(0.95f);
        holder.itemView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setStartDelay(position * 50)
            .start();

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                // Animate out before deletion
                v.animate().rotation(180f).alpha(0f).scaleX(0.8f).scaleY(0.8f)
                    .setDuration(200)
                    .withEndAction(() -> deleteListener.onDelete(item))
                    .start();
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                // Scale animation on click
                v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start();
                v.postDelayed(() -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    itemClickListener.onItemClick(item);
                }, 100);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtItemName;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtItemName = itemView.findViewById(R.id.txtItemName);
            btnDelete = itemView.findViewById(R.id.btnDeleteItem);
        }
    }
}
