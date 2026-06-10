package com.example.eduinsight;import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.Locale;

public class ViewSolutionActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private ImageButton btnPlayPause;
    private SeekBar audioSeekBar;
    private TextView txtAudioDuration;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_solution);

        // 1. Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Doubt Solution");
        }

        // 2. Initialize Views
        TextView txtDesc = findViewById(R.id.txtDetailDescription);
        TextView txtSolutionText = findViewById(R.id.txtSolutionText);
        TextView txtWaitMessage = findViewById(R.id.txtWaitMessage);
        ImageView imgSolution = findViewById(R.id.imgSolution);
        LinearLayout waitNoticeContainer = findViewById(R.id.waitNoticeContainer);
        LinearLayout solutionContainer = findViewById(R.id.solutionContainer);

        LinearLayout layoutAudio = findViewById(R.id.layoutAudio);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        audioSeekBar = findViewById(R.id.audioSeekBar);
        txtAudioDuration = findViewById(R.id.txtAudioDuration);

        // 3. Get Data from Intent
        String description = getIntent().getStringExtra("description");
        String solution = getIntent().getStringExtra("solution");
        String solImageUrl = getIntent().getStringExtra("sol_image");
        String solAudioUrl = getIntent().getStringExtra("sol_audio");

        txtDesc.setText(description);

        // 4. Logic to show/hide content based on solution status
        if (solution == null || solution.trim().isEmpty() || solution.equalsIgnoreCase("null") || solution.equals("No solution yet")) {
            // No solution yet
            waitNoticeContainer.setVisibility(View.VISIBLE);
            solutionContainer.setVisibility(View.GONE);
            txtWaitMessage.setText("Your mentor is still reviewing this doubt.\nPlease check back later.");
        } else {
            // Solution exists
            waitNoticeContainer.setVisibility(View.GONE);
            solutionContainer.setVisibility(View.VISIBLE);
            txtSolutionText.setText(solution);

            // Show image solution if URL exists
            if (solImageUrl != null && !solImageUrl.isEmpty() && !solImageUrl.equalsIgnoreCase("null")) {
                imgSolution.setVisibility(View.VISIBLE);
                Glide.with(this).load(solImageUrl).into(imgSolution);
            } else {
                imgSolution.setVisibility(View.GONE);
            }

            // Show audio solution if URL exists
            if (solAudioUrl != null && !solAudioUrl.isEmpty() && !solAudioUrl.equalsIgnoreCase("null")) {
                layoutAudio.setVisibility(View.VISIBLE);
                setupAudioPlayer(solAudioUrl);
            } else {
                layoutAudio.setVisibility(View.GONE);
            }
        }
    }

    private void setupAudioPlayer(String audioUrl) {
        String fullUrl = audioUrl.startsWith("http") ? audioUrl : "http://10.0.2.2/eduinsight_api/" + audioUrl;
        
        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer == null) {
                startPlaying(fullUrl);
            } else {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    mediaPlayer.start();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    updateSeekBar();
                }
            }
        });

        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void startPlaying(String url) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                audioSeekBar.setMax(mp.getDuration());
                updateSeekBar();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                audioSeekBar.setProgress(0);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateSeekBar() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            audioSeekBar.setProgress(mediaPlayer.getCurrentPosition());
            int mins = mediaPlayer.getCurrentPosition() / 1000 / 60;
            int secs = (mediaPlayer.getCurrentPosition() / 1000) % 60;
            txtAudioDuration.setText(String.format(Locale.getDefault(), "%d:%02d", mins, secs));
            handler.postDelayed(this::updateSeekBar, 1000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}