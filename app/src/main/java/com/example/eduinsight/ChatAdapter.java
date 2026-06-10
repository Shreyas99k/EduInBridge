package com.example.eduinsight;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Message> messageList;
    private String currentUserType; // "student" or "teacher"
    private Context context;

    public ChatAdapter(Context context, List<Message> messageList, String currentUserType) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserType = (currentUserType != null) ? currentUserType.toLowerCase().trim() : "";
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_bubble, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Message msg = messageList.get(position);
        String text = msg.getMessage();

        holder.txtMessage.setText(text);
        holder.txtTime.setText(msg.getCreatedAt());

        // Handling Image Visibility
        if (msg.getImageUrl() != null && !msg.getImageUrl().isEmpty() && !msg.getImageUrl().equals("null")) {
            holder.imgChat.setVisibility(View.VISIBLE);
            String fullImageUrl = "http://10.0.2.2/eduinsight_api/" + msg.getImageUrl();
            Glide.with(context).load(fullImageUrl).into(holder.imgChat);
        } else {
            holder.imgChat.setVisibility(View.GONE);
        }

        // --- GLASS BUBBLE ALIGNMENT & DRAWABLES ---
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.chatCard.getLayoutParams();

        String msgSender = (msg.getSenderType() != null) ? msg.getSenderType().toLowerCase().trim() : "";

        if (msgSender.equals(currentUserType)) {
            // My message: Right side + Neon Green Glass
            params.gravity = Gravity.END;
            holder.chatCard.setBackgroundResource(R.drawable.glass_bubble_sent);
        } else {
            // Peer message: Left side + Dark White Glass
            params.gravity = Gravity.START;
            holder.chatCard.setBackgroundResource(R.drawable.glass_bubble_received);
        }
        holder.chatCard.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return (messageList != null) ? messageList.size() : 0;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage, txtTime;
        ImageView imgChat;
        View chatCard; // Changed to View to support generic backgrounds

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txtChatMessage);
            txtTime = itemView.findViewById(R.id.txtChatTime);
            imgChat = itemView.findViewById(R.id.imgChat);
            chatCard = itemView.findViewById(R.id.chatCard);
        }
    }
}
