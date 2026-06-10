package com.example.eduinsight;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DoubtSelectionAdapter extends RecyclerView.Adapter<DoubtSelectionAdapter.ViewHolder> {

    private List<String> items;
    private List<String> stats;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public DoubtSelectionAdapter(List<String> items, List<String> stats, OnItemClickListener listener) {
        this.items = items;
        this.stats = stats;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selection_doubt, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtDesc.setText(items.get(position));
        holder.txtStatus.setText(stats.get(position).toUpperCase());
        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDesc, txtStatus;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDesc = itemView.findViewById(R.id.txtSelectionDesc);
            txtStatus = itemView.findViewById(R.id.txtSelectionStatus);
        }
    }
}
