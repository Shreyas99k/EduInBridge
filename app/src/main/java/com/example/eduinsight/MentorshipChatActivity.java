package com.example.eduinsight;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MentorshipChatActivity extends AppCompatActivity implements MentorshipAdapter.OnMessageInteractionListener {

    private RecyclerView rvMessages;
    private MentorshipAdapter adapter;
    private final List<MentorshipMessage> messageList = new ArrayList<>();
    
    private EditText editMessage;
    private TextView txtDescription, txtReplyUserHeader, txtReplyMsgPreview;
    private View layoutReply;
    private ImageButton btnMic;
    private LottieAnimationView lottieSendAnimation;
    private FloatingActionButton btnSend;
    
    private int doubtId, currentUserId;
    private String currentUserRole, studentName, teacherName, currentUserName, sessionDescription;
    private MentorshipMessage replyingTo = null;
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private boolean isFirstLoad = true;
    private int lastMessageCount = 0;
    private String lastResponseHash = "";

    private static final int PICK_IMAGE_REQUEST = 1;
    
    private MediaRecorder recorder;
    private String audioFileName;
    private boolean isRecording = false;
    private long recordStartTime = 0;
    
    private int recordingSeconds = 0;
    private final Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    private ActivityResultLauncher<String> audioPermissionLauncher;
    private ActivityResultLauncher<String> storagePermissionLauncher;

    private com.facebook.shimmer.ShimmerFrameLayout shimmerView;
    private View dimOverlay;

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Parallax disabled to prevent blank space rendering issues
        }
        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mentorship_chat);

        setupLaunchers();

        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUserId = pref.getInt("user_id", -1);
        currentUserRole = pref.getString("role", "student").toLowerCase().trim();
        currentUserName = pref.getString("name", "User");

        doubtId = getIntent().getIntExtra("doubt_id", -1);
        studentName = getIntent().getStringExtra("student_name");
        teacherName = getIntent().getStringExtra("teacher_name");
        sessionDescription = getIntent().getStringExtra("description");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(currentUserRole.equals("student") ? teacherName : studentName);
        }

        txtDescription = findViewById(R.id.txtDescription);
        if (sessionDescription != null) txtDescription.setText(sessionDescription);

        rvMessages = findViewById(R.id.rvMessages);
        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);
        ImageButton btnAttach = findViewById(R.id.btnAttach);
        btnMic = findViewById(R.id.btnMic);
        lottieSendAnimation = findViewById(R.id.lottieSendAnimation);
        shimmerView = findViewById(R.id.shimmerView);
        layoutReply = findViewById(R.id.layoutReply);
        dimOverlay = findViewById(R.id.dimOverlay);
        txtReplyUserHeader = findViewById(R.id.txtReplyUserHeader);
        txtReplyMsgPreview = findViewById(R.id.txtReplyMsgPreview);
        ImageButton btnCancelReply = findViewById(R.id.btnCancelReply);

        adapter = new MentorshipAdapter(this, messageList, currentUserRole, this);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> {
            String msg = editMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendMessage(msg, null, null);
                cancelReply();
            }
        });

        btnAttach.setOnClickListener(v -> checkStoragePermissionAndOpenGallery());

        btnMic.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                checkAudioPermissionAndStartRecording();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                stopRecordingAndSend();
            }
            return true;
        });

        btnCancelReply.setOnClickListener(v -> cancelReply());

        setupSwipeToReply();
        setupSensors();
        startAutoRefresh();
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }
    }

    private void setupLaunchers() {
        audioPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) startRecording();
            else Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show();
        });

        storagePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) openGallery();
            else Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkAudioPermissionAndStartRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void checkStoragePermissionAndOpenGallery() {
        String p = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            storagePermissionLauncher.launch(p);
        }
    }

    private void startRecording() {
        try {
            File cacheDir = getExternalCacheDir();
            if (cacheDir == null) {
                Toast.makeText(this, "Cache directory not found", Toast.LENGTH_SHORT).show();
                return;
            }
            audioFileName = cacheDir.getAbsolutePath() + "/audiorecord.m4a";
            
            if (recorder != null) {
                recorder.release();
                recorder = null;
            }

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(audioFileName);
            
            recorder.prepare();
            recorder.start();
            isRecording = true;
            recordStartTime = System.currentTimeMillis();
            btnMic.setImageResource(android.R.drawable.presence_audio_online);
            startRecordingTimer();
            Log.d("MENTORSHIP", "Recording started: " + audioFileName);
        } catch (Exception e) {
            Log.e("MENTORSHIP", "Start recording failed: " + e.getMessage());
            Toast.makeText(this, "Recording failed to start", Toast.LENGTH_SHORT).show();
            if (recorder != null) {
                recorder.release();
                recorder = null;
            }
            isRecording = false;
        }
    }

    private void startRecordingTimer() {
        recordingSeconds = 0;
        timerRunnable = new Runnable() {
            @Override public void run() {
                if (isRecording) {
                    recordingSeconds++;
                    int mins = recordingSeconds / 60; int secs = recordingSeconds % 60;
                    editMessage.setHint(String.format(Locale.getDefault(), "Rec: %02d:%02d", mins, secs));
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopRecordingAndSend() {
        if (isRecording && recorder != null) {
            try {
                isRecording = false;
                timerHandler.removeCallbacks(timerRunnable);
                editMessage.setHint("Message...");
                long duration = System.currentTimeMillis() - recordStartTime;
                
                try {
                    recorder.stop();
                } catch (RuntimeException stopException) {
                    Log.e("MENTORSHIP", "Recorder stop failed (possibly too short): " + stopException.getMessage());
                }
                
                recorder.release();
                recorder = null;
                btnMic.setImageResource(android.R.drawable.ic_btn_speak_now);
                
                if (duration > 1000) {
                    new Handler(Looper.getMainLooper()).postDelayed(this::sendAudioMessage, 200);
                } else {
                    Log.d("MENTORSHIP", "Recording too short: " + duration + "ms");
                    File f = new File(audioFileName);
                    if (f.exists()) f.delete();
                }
            } catch (Exception e) {
                Log.e("MENTORSHIP", "Stop recording error: " + e.getMessage());
                if (recorder != null) {
                    recorder.release();
                    recorder = null;
                }
                isRecording = false;
            }
        }
    }

    private void sendAudioMessage() {
        File f = new File(audioFileName);
        if (f.exists() && f.length() > 0) {
            byte[] b = fileToByteArray(f);
            if (b != null) {
                Log.d("MENTORSHIP", "Sending audio, size: " + b.length + " bytes");
                sendMessage("", null, Base64.encodeToString(b, Base64.NO_WRAP));
            }
        } else {
            Log.e("MENTORSHIP", "Audio file is missing or empty");
        }
    }

    private byte[] fileToByteArray(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024]; int n;
            while ((n = fis.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toByteArray();
        } catch (IOException e) { return null; }
    }

    private void startAutoRefresh() {
        refreshRunnable = new Runnable() {
            @Override public void run() {
                if (doubtId != -1) fetchMessages(false);
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(refreshRunnable);
    }

    private void fetchMessages(boolean forceScroll) {
        String url = "http://10.0.2.2/eduinsight_api/get_message.php?doubt_id=" + doubtId + "&viewer_role=" + currentUserRole + "&user_id=" + currentUserId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (shimmerView != null && shimmerView.getVisibility() == View.VISIBLE) {
                        shimmerView.stopShimmer();
                        shimmerView.setVisibility(View.GONE);
                        rvMessages.setVisibility(View.VISIBLE);
                        // Force a redraw to fix blank pages issue
                        rvMessages.invalidate();
                        rvMessages.requestLayout();
                    }
                    try {
                        JSONArray array = new JSONArray(response);
                        
                        String currentResponseHash = response.length() + "_" + response.hashCode();
                        if (currentResponseHash.equals(lastResponseHash) && !isFirstLoad && !forceScroll) return;
                        lastResponseHash = currentResponseHash;

                        List<MentorshipMessage> newMessages = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            String sender = obj.optString("sender_type", "student").toLowerCase().trim();
                            
                            newMessages.add(new MentorshipMessage(
                                    obj.optInt("id", -1),
                                    obj.optString("message", ""),
                                    sender,
                                    obj.optString("created_at", ""),
                                    obj.optString("image_url", ""),
                                    obj.optString("audio_url", ""), 
                                    obj.optString("reply_to_message", ""),
                                    obj.optString("reply_to_user", ""),
                                    obj.optInt("is_saved", 0) == 1,
                                    obj.optInt("is_deleted", 0) == 1,
                                    obj.optInt("is_edited", 0) == 1,
                                    obj.optInt("deleted_by", -1)
                            ));
                        }

                        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MentorshipDiffCallback(messageList, newMessages));
                        messageList.clear();
                        messageList.addAll(newMessages);
                        diffResult.dispatchUpdatesTo(adapter);
                        
                        if (isFirstLoad || forceScroll || array.length() > lastMessageCount) {
                            if (!messageList.isEmpty()) {
                                rvMessages.post(() -> {
                                    rvMessages.scrollToPosition(messageList.size() - 1);
                                    // Secondary scroll check to ensure bottom is reached after layout
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> 
                                        rvMessages.scrollToPosition(messageList.size() - 1), 50);
                                });
                            }
                            isFirstLoad = false;
                        }
                        lastMessageCount = array.length();
                    } catch (Exception e) { Log.e("MENTORSHIP", "JSON err: " + e.getMessage()); }
                }, error -> Log.e("MENTORSHIP", "Volley fail"));
        Volley.newRequestQueue(this).add(request);
    }

    private void sendMessage(String msg, String imageBase64, String audioBase64) {
        editMessage.setHint("Sending...");
        String url = "http://10.0.2.2/eduinsight_api/send_message.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    editMessage.setHint("Message...");
                    if (response.trim().contains("success")) {
                        showSendSuccessAnimation();
                        editMessage.setText("");
                        new Handler(Looper.getMainLooper()).postDelayed(() -> fetchMessages(true), 300);
                    } else {
                        Toast.makeText(this, "Server error: " + response, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    editMessage.setHint("Message...");
                    Toast.makeText(this, "Check connection", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("doubt_id", String.valueOf(doubtId));
                p.put("sender_id", String.valueOf(currentUserId)); 
                p.put("sender_type", currentUserRole);
                p.put("sender_name", currentUserName);
                p.put("message", msg);
                if (imageBase64 != null) p.put("image", imageBase64);
                if (audioBase64 != null) p.put("audio", audioBase64);
                if (replyingTo != null) {
                    p.put("reply_to_message", replyingTo.getMessage());
                    p.put("reply_to_user", replyingTo.getSenderType());
                }
                return p;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                ClipData cd = data.getClipData();
                for (int i = 0; i < cd.getItemCount(); i++) uploadImage(cd.getItemAt(i).getUri());
            } else if (data.getData() != null) {
                uploadImage(data.getData());
            }
        }
    }

    private void uploadImage(android.net.Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos);
            sendMessage("", Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT), null);
        } catch (IOException e) { Log.e("MENTORSHIP", "Img fail"); }
    }

    @Override
    public void onReply(MentorshipMessage message) {
        hideDimOverlay();
        replyingTo = message;
        layoutReply.setVisibility(View.VISIBLE);
        txtReplyUserHeader.setText(message.getSenderType().toUpperCase());
        txtReplyMsgPreview.setText(message.getMessage());
        editMessage.requestFocus();
    }

    @Override
    public void onDelete(MentorshipMessage message) {
        hideDimOverlay();
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Message");
        
        String[] options;
        if (message.getSenderType().equalsIgnoreCase(currentUserRole)) {
            options = new String[]{"Delete for Me", "Delete for Everyone"};
        } else {
            options = new String[]{"Delete for Me"};
        }

        builder.setItems(options, (dialog, which) -> {
            String deleteType = options[which].equals("Delete for Everyone") ? "for_everyone" : "for_me";
            performDelete(message.getId(), deleteType);
        });
        builder.show();
    }

    private void performDelete(int messageId, String type) {
        String url = "http://10.0.2.2/eduinsight_api/delete_message.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.trim().contains("success")) {
                        lastResponseHash = "";
                        fetchMessages(true);
                    } else {
                        Toast.makeText(this, "Failed to delete: " + response, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("message_id", String.valueOf(messageId));
                p.put("user_id", String.valueOf(currentUserId));
                p.put("type", type);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public void onEdit(MentorshipMessage message) {
        hideDimOverlay();
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Edit Message");

        final EditText input = new EditText(this);
        input.setText(message.getMessage());
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        params.topMargin = 20;
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newText = input.getText().toString().trim();
            if (!newText.isEmpty() && !newText.equals(message.getMessage())) {
                performEdit(message.getId(), newText);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void performEdit(int messageId, String newText) {
        String url = "http://10.0.2.2/eduinsight_api/edit_message.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.trim().contains("success")) {
                        lastResponseHash = "";
                        fetchMessages(true);
                    } else {
                        Toast.makeText(this, "Failed to edit: " + response, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("message_id", String.valueOf(messageId));
                p.put("user_id", String.valueOf(currentUserId));
                p.put("new_text", newText);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    private void showSendSuccessAnimation() {
        if (lottieSendAnimation == null) return;
        
        btnSend.setVisibility(View.INVISIBLE);
        lottieSendAnimation.setVisibility(View.VISIBLE);
        lottieSendAnimation.playAnimation();
        
        lottieSendAnimation.addAnimatorListener(new android.animation.Animator.AnimatorListener() {
            @Override public void onAnimationStart(android.animation.Animator animation) {}
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                lottieSendAnimation.setVisibility(View.GONE);
                btnSend.setVisibility(View.VISIBLE);
                lottieSendAnimation.removeAllAnimatorListeners();
            }
            @Override public void onAnimationCancel(android.animation.Animator animation) {}
            @Override public void onAnimationRepeat(android.animation.Animator animation) {}
        });
    }

    private void setupSwipeToReply() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    onReply(messageList.get(position));
                }
                adapter.notifyItemChanged(position);
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    float limit = 200f;
                    float translationX = Math.min(dX, limit);
                    
                    // Apply elastic resistance
                    if (dX > limit) {
                        translationX = limit + (dX - limit) * 0.3f;
                    }
                    
                    super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive);
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(rvMessages);
    }

    private void cancelReply() {
        replyingTo = null;
        layoutReply.setVisibility(View.GONE);
    }

    public void showDimOverlay() {
        if (dimOverlay == null) return;
        dimOverlay.setVisibility(View.VISIBLE);
        dimOverlay.animate().alpha(1f).setDuration(300).start();
    }

    public void hideDimOverlay() {
        if (dimOverlay == null) return;
        dimOverlay.animate().alpha(0f).setDuration(300).withEndAction(() -> dimOverlay.setVisibility(View.GONE)).start();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(sensorListener, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
    }
}
