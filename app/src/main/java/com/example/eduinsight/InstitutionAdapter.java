package com.example.eduinsight;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import java.util.List;

public class InstitutionAdapter extends RecyclerView.Adapter<InstitutionAdapter.ViewHolder> {

    private final List<JSONObject> institutions;
    private final OnInstitutionClickListener listener;
    private int lastPosition = -1;

    public interface OnInstitutionClickListener {
        void onInstitutionClick(JSONObject institution);
    }

    public InstitutionAdapter(List<JSONObject> institutions, OnInstitutionClickListener listener) {
        this.institutions = institutions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_institution, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject institution = institutions.get(position);
        holder.nameText.setText(institution.optString("name"));
        
        // Display address or fallback to default text
        String address = institution.optString("address", "");
        if (address.isEmpty() || address.equals("null")) {
            holder.addressText.setText("Official Education Node");
        } else {
            holder.addressText.setText(address);
        }
        
        // Trendy selection animation logic
        holder.itemView.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                listener.onInstitutionClick(institution);
            }).start();
        });

        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), android.R.anim.fade_in);
            animation.setDuration(400);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return institutions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView nameText;
        public final TextView addressText;

        public ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.txtInstitutionName);
            addressText = itemView.findViewById(R.id.txtInstitutionAddress);
        }
    }
}
