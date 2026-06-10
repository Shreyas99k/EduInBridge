package com.example.eduinsight;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.List;

public class MyDoubtAdapter extends RecyclerView.Adapter<MyDoubtAdapter.ViewHolder> implements Filterable {

    private List<MyDoubt> originalList;
    private List<MyDoubt> filteredList;

    public MyDoubtAdapter(List<MyDoubt> list) {
        this.originalList = list;
        this.filteredList = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Keeping your existing square layout
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_doubt_square, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyDoubt d = filteredList.get(position);
        holder.txtSub.setText(d.getSubject());
        holder.txtDesc.setText(d.getDescription());
        holder.txtStatus.setText(d.getStatus().toUpperCase());

        // Logic for colors based on status
        if (d.getStatus().equalsIgnoreCase("solved")) {
            holder.txtStatus.setTextColor(Color.parseColor("#2E7D32"));
            if (holder.statusIndicator != null) holder.statusIndicator.setBackgroundColor(Color.parseColor("#2E7D32"));
        } else {
            holder.txtStatus.setTextColor(Color.parseColor("#EF6C00"));
            if (holder.statusIndicator != null) holder.statusIndicator.setBackgroundColor(Color.parseColor("#EF6C00"));
        }

        holder.itemView.setOnClickListener(v -> showDetailDialog(v.getContext(), d));
    }

    private void showDetailDialog(Context context, MyDoubt doubt) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_doubt_detail, null);

        TextView sub = view.findViewById(R.id.detailSubject);
        TextView q = view.findViewById(R.id.detailQuestion);
        TextView a = view.findViewById(R.id.detailAnswer);
        Button btn = view.findViewById(R.id.btnCloseDialog);

        sub.setText(doubt.getSubject());
        q.setText(doubt.getDescription());

        // Check if solved to show answer or waiting message
        if (doubt.getStatus().equalsIgnoreCase("solved")) {
            a.setText(doubt.getAnswer());
            a.setTextColor(Color.BLACK);
        } else {
            a.setText("Teacher is working on your answer. Please check back soon!");
            a.setTextColor(Color.GRAY);
        }

        btn.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(view);
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return (filteredList != null) ? filteredList.size() : 0;
    }

    // --- SEARCH FILTER LOGIC ---
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String query = constraint.toString().toLowerCase().trim();
                List<MyDoubt> resultsList = new ArrayList<>();

                if (query.isEmpty()) {
                    resultsList = originalList;
                } else {
                    for (MyDoubt row : originalList) {
                        if (row.getSubject().toLowerCase().contains(query) ||
                                row.getDescription().toLowerCase().contains(query)) {
                            resultsList.add(row);
                        }
                    }
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = resultsList;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                // Fixed the potential cast error here
                filteredList = (List<MyDoubt>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtSub, txtDesc, txtStatus;
        View statusIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtSub = itemView.findViewById(R.id.txtSubject);
            txtDesc = itemView.findViewById(R.id.txtDoubtSnippet);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
        }
    }
}