package com.example.eduinsight;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.view.HapticFeedbackConstants;
import android.view.animation.OvershootInterpolator;
import android.widget.PopupWindow;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MentorshipAdapter extends RecyclerView.Adapter<MentorshipAdapter.ViewHolder> {

    private List<MentorshipMessage> list;
    private String userRole; 
    private Context context;
    private MediaPlayer mediaPlayer;
    private int currentlyPlayingPos = -1;
    private Handler handler = new Handler();
    private ViewHolder currentlyPlayingHolder;
    private OnMessageInteractionListener interactionListener;
    private int selectedPosition = -1;

    public interface OnMessageInteractionListener {
        void onReply(MentorshipMessage message);
        void onDelete(MentorshipMessage message);
        void onEdit(MentorshipMessage message);
    }

    public MentorshipAdapter(Context context, List<MentorshipMessage> list, String userRole, OnMessageInteractionListener listener) {
        this.context = context;
        this.list = list;
        this.userRole = (userRole != null) ? userRole.toLowerCase().trim() : "";
        this.interactionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mentorship_bubble, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MentorshipMessage msg = list.get(position);
        
        boolean isDeleted = msg.isDeleted();
        boolean isEdited = msg.isEdited();

        if (isDeleted) {
            holder.txtContent.setText("This message was deleted");
            holder.txtContent.setTypeface(null, android.graphics.Typeface.ITALIC);
            holder.imgContent.setVisibility(View.GONE);
            holder.layoutAudio.setVisibility(View.GONE);
            holder.layoutReply.setVisibility(View.GONE);
        } else {
            String editedSuffix = isEdited ? " (edited)" : "";
            holder.txtContent.setText(msg.getMessage() + editedSuffix);
            holder.txtContent.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.txtContent.setVisibility((msg.getMessage() == null || msg.getMessage().isEmpty()) ? View.GONE : View.VISIBLE);
            
            // Image handling
            if (msg.getImageUrl() != null && !msg.getImageUrl().isEmpty() && !msg.getImageUrl().equals("null")) {
                holder.imgContent.setVisibility(View.VISIBLE);
                String url = msg.getImageUrl().startsWith("http") ? msg.getImageUrl() : "http://10.0.2.2/eduinsight_api/" + msg.getImageUrl();
                Glide.with(context).load(url).into(holder.imgContent);
                
                holder.imgContent.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ViewFullImageActivity.class);
                    intent.putExtra("image_url", url);
                    intent.putExtra("message_id", msg.getId());
                    context.startActivity(intent);
                });
            } else {
                holder.imgContent.setVisibility(View.GONE);
            }

            // Audio handling
            if (msg.getAudioUrl() != null && !msg.getAudioUrl().isEmpty() && !msg.getAudioUrl().equals("null")) {
                holder.layoutAudio.setVisibility(View.VISIBLE);
                setupAudioPlayer(holder, msg, position);
            } else {
                holder.layoutAudio.setVisibility(View.GONE);
            }

            // Reply logic
            if (msg.getReplyToMsg() != null && !msg.getReplyToMsg().isEmpty() && !msg.getReplyToMsg().equals("null")) {
                holder.layoutReply.setVisibility(View.VISIBLE);
                holder.txtReplyUser.setText(msg.getReplyToUser());
                holder.txtReplyMsg.setText(msg.getReplyToMsg());
            } else {
                holder.layoutReply.setVisibility(View.GONE);
            }
        }

        holder.txtTime.setText(msg.getCreatedAt());
        holder.txtSavedStatus.setVisibility(msg.isSaved() ? View.VISIBLE : View.GONE);
        if (msg.isSaved()) holder.txtSavedStatus.setText("Pinned to session");

        String sender = msg.getSenderType().toLowerCase().trim();
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.card.getLayoutParams();

        holder.txtContent.setTextColor(Color.WHITE);
        holder.txtTime.setTextColor(Color.parseColor("#99FFFFFF"));

        if (sender.equals(userRole)) {
            params.gravity = Gravity.END;
            holder.card.setBackgroundResource(R.drawable.glass_bubble_sent);
        } else {
            params.gravity = Gravity.START;
            holder.card.setBackgroundResource(R.drawable.glass_bubble_received);
        }
        holder.card.setLayoutParams(params);
        holder.card.setAlpha(1.0f);
        
        // Reset any potential translations from previous versions
        holder.card.setTranslationX(0f);
        holder.card.setTranslationY(0f);
        holder.card.setRotationY(0f);

        holder.itemView.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            
            // Scale-Down Feedback
            holder.card.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        showContextualPill(v, msg, position, holder);
                    })
                    .start();
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            // Optional: short click also shows pill or just resets scale if it was scaled
            if (holder.card.getScaleX() < 1.0f) {
                holder.card.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
            }
        });
    }

    private void showContextualPill(View anchor, MentorshipMessage msg, int position, ViewHolder holder) {
        View menuView = LayoutInflater.from(context).inflate(R.layout.layout_contextual_menu, null);
        PopupWindow popupWindow = new PopupWindow(menuView, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                true);

        // Show Dim Overlay
        if (context instanceof MentorshipChatActivity) {
            ((MentorshipChatActivity) context).showDimOverlay();
        }

        // Haptic "Tick"
        anchor.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);

        // Configure Menu Options
        boolean isMyMsg = msg.getSenderType().equalsIgnoreCase(userRole);
        View btnReply = menuView.findViewById(R.id.menuReply);
        View btnEdit = menuView.findViewById(R.id.menuEdit);
        View btnCopy = menuView.findViewById(R.id.menuCopy);
        View btnDelete = menuView.findViewById(R.id.menuDelete);

        if (!isMyMsg) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }

        btnReply.setOnClickListener(v -> {
            interactionListener.onReply(msg);
            popupWindow.dismiss();
        });

        btnEdit.setOnClickListener(v -> {
            interactionListener.onEdit(msg);
            popupWindow.dismiss();
        });

        btnCopy.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("label", msg.getMessage());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            interactionListener.onDelete(msg);
            popupWindow.dismiss();
        });

        popupWindow.setOnDismissListener(() -> {
            holder.card.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
        });

        // Spring-Pop Animation logic
        menuView.setScaleX(0.5f);
        menuView.setScaleY(0.5f);
        menuView.setAlpha(0f);

        // Position and Show
        int[] location = new int[2];
        anchor.getLocationInWindow(location);
        
        // Show above or below the bubble depending on space
        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, 
                location[0] + (anchor.getWidth() / 2) - 150, // rough center adjustment
                location[1] - 120);

        menuView.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();
    }

    private void showOptionsDialog(MentorshipMessage msg, int position) {
        // This method is now replaced by showContextualPill
    }

    private float currentSpeed = 1.0f;

    private void setupAudioPlayer(ViewHolder holder, MentorshipMessage msg, int position) {
        String rawUrl = msg.getAudioUrl();
        String fullAudioUrl = rawUrl.startsWith("http") ? rawUrl : "http://10.0.2.2/eduinsight_api/" + rawUrl;
        
        holder.txtAudioSpeed.setText(String.format(Locale.getDefault(), "%.1fx", currentSpeed));
        holder.txtAudioSpeed.setOnClickListener(v -> {
            if (currentSpeed == 1.0f) currentSpeed = 1.5f;
            else if (currentSpeed == 1.5f) currentSpeed = 2.0f;
            else currentSpeed = 1.0f;
            
            holder.txtAudioSpeed.setText(String.format(Locale.getDefault(), "%.1fx", currentSpeed));
            if (currentlyPlayingPos == position && mediaPlayer != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(currentSpeed));
            }
        });

        if (currentlyPlayingPos == position) {
            currentlyPlayingHolder = holder;
            holder.btnPlayPause.setImageResource(mediaPlayer != null && mediaPlayer.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            if (mediaPlayer != null) {
                holder.audioSeekBar.setMax(mediaPlayer.getDuration());
                holder.audioSeekBar.setProgress(mediaPlayer.getCurrentPosition());
                updateDurationText(holder, mediaPlayer.getCurrentPosition());
            }
        } else {
            holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            holder.audioSeekBar.setProgress(0);
            holder.txtAudioDuration.setText("0:00");
        }

        holder.btnPlayPause.setOnClickListener(v -> {
            if (currentlyPlayingPos == position) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                } else if (mediaPlayer != null) {
                    mediaPlayer.start();
                    holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    updateSeekBar();
                }
            } else {
                playAudio(fullAudioUrl, position, holder);
            }
        });

        holder.audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && currentlyPlayingPos == position && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    updateDurationText(holder, progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void playAudio(String url, int position, ViewHolder holder) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        currentlyPlayingPos = position;
        currentlyPlayingHolder = holder;
        
        holder.btnPlayPause.setVisibility(View.GONE);
        holder.audioLoading.setVisibility(View.VISIBLE);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());
        try {
            mediaPlayer.setOnPreparedListener(mp -> {
                holder.audioLoading.setVisibility(View.GONE);
                holder.btnPlayPause.setVisibility(View.VISIBLE);
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(currentSpeed));
                }
                
                mediaPlayer.start();
                holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                holder.audioSeekBar.setMax(mediaPlayer.getDuration());
                updateSeekBar();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                holder.audioSeekBar.setProgress(0);
                holder.txtAudioDuration.setText("0:00");
                currentlyPlayingPos = -1;
                currentlyPlayingHolder = null;
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                holder.audioLoading.setVisibility(View.GONE);
                holder.btnPlayPause.setVisibility(View.VISIBLE);
                String errorMsg = "Playback Error (" + what + ")";
                if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) errorMsg = "Server Died";
                else if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
                    if (extra == -1004) errorMsg = "File Not Found (404)";
                    else if (extra == -1007) errorMsg = "Unsupported Format";
                    else errorMsg = "IO/Timed Out (" + extra + ")";
                }
                Log.e("AUDIO_PLAYER", "Error: " + what + ", Extra: " + extra + " for URL: " + url);
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show();
                currentlyPlayingPos = -1;
                holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                return true;
            });
            Log.d("AUDIO_PLAYER", "Preparing audio: " + url);
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e("AUDIO_PLAYER", "Init Failed: " + e.getMessage());
        }
    }

    private void updateSeekBar() {
        if (mediaPlayer != null && mediaPlayer.isPlaying() && currentlyPlayingHolder != null) {
            int current = mediaPlayer.getCurrentPosition();
            currentlyPlayingHolder.audioSeekBar.setProgress(current);
            updateDurationText(currentlyPlayingHolder, current);
            handler.postDelayed(this::updateSeekBar, 100);
        }
    }

    private void updateDurationText(ViewHolder holder, int current) {
        holder.txtAudioDuration.setText(formatTime(current));
    }

    private String formatTime(int ms) {
        int seconds = (ms / 1000) % 60;
        int minutes = (ms / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtContent, txtTime, txtReplyUser, txtReplyMsg, txtAudioDuration, txtSavedStatus, txtAudioSpeed;
        ImageView imgContent;
        View card, layoutReply, layoutAudio, audioLoading;
        ImageButton btnPlayPause;
        SeekBar audioSeekBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtContent = itemView.findViewById(R.id.txtMessageContent);
            txtTime = itemView.findViewById(R.id.txtTimestamp);
            imgContent = itemView.findViewById(R.id.imgMessage);
            card = itemView.findViewById(R.id.chatCard);
            layoutReply = itemView.findViewById(R.id.layoutReplyPreview);
            txtReplyUser = itemView.findViewById(R.id.txtReplyUser);
            txtReplyMsg = itemView.findViewById(R.id.txtReplyMsg);
            layoutAudio = itemView.findViewById(R.id.layoutAudio);
            btnPlayPause = itemView.findViewById(R.id.btnPlayPause);
            audioLoading = itemView.findViewById(R.id.audioLoading);
            audioSeekBar = itemView.findViewById(R.id.audioSeekBar);
            txtAudioDuration = itemView.findViewById(R.id.txtAudioDuration);
            txtAudioSpeed = itemView.findViewById(R.id.txtAudioSpeed);
            txtSavedStatus = itemView.findViewById(R.id.txtSavedStatus);
        }
    }
}
