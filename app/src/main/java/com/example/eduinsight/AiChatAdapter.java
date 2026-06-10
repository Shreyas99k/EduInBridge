package com.example.eduinsight;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AiChatAdapter extends RecyclerView.Adapter<AiChatAdapter.ViewHolder> {
    private List<AiChatMessage> messages;

    public AiChatAdapter(List<AiChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                viewType == 1 ? R.layout.item_ai_user_msg : R.layout.item_ai_bot_msg, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? 1 : 0;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtMsg.setText(messages.get(position).getMessage());
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtMsg;
        ViewHolder(View itemView) {
            super(itemView);
            txtMsg = itemView.findViewById(R.id.txtAiMsg);
        }
    }
}
