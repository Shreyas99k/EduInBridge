package com.example.eduinsight;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SolveDoubtActivity extends AppCompatActivity {

    private int doubtId, currentUserId;
    private EditText editSolution;
    private ImageView imgAttachmentPreview;
    private CardView cardImagePreview;
    private Bitmap bitmap = null;
    private static final int PICK_IMAGE_REQUEST = 1;
    private String studentQuestion = "", teacherName;

    private MediaRecorder recorder;
    private String audioFileName;
    private boolean isRecording = false;
    private long recordStartTime = 0;
    private int recordingSeconds = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private ImageButton btnMic;
    private String audioBase64 = null;

    private final ActivityResultLauncher<String> audioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startRecording();
                else Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            });

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#1E3C40"));
        }

        setContentView(R.layout.activity_solve_doubt);

        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUserId = pref.getInt("user_id", -1);
        teacherName = pref.getString("name", "Mentor");

        Toolbar toolbar = findViewById(R.id.toolbarSolve);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        editSolution = findViewById(R.id.editSolution);
        imgAttachmentPreview = findViewById(R.id.imgAttachmentPreview);
        cardImagePreview = findViewById(R.id.cardImagePreview);
        
        TextView txtQuestionDesc = findViewById(R.id.txtQuestionDesc);
        ImageButton btnAttach = findViewById(R.id.btnAttach);
        ImageButton btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);

        doubtId = getIntent().getIntExtra("doubt_id", -1);
        studentQuestion = getIntent().getStringExtra("description");
        txtQuestionDesc.setText(studentQuestion);

        btnAttach.setOnClickListener(v -> openGallery());
        
        btnSend.setOnClickListener(v -> {
            String solText = editSolution.getText().toString().trim();
            if (!solText.isEmpty() || bitmap != null || audioBase64 != null) {
                submitSolution(solText);
            } else {
                Toast.makeText(this, "Provide text, image or audio", Toast.LENGTH_SHORT).show();
            }
        });

        btnMic.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                checkAudioPermissionAndStartRecording();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                stopRecording();
            }
            return true;
        });
    }

    private void checkAudioPermissionAndStartRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startRecording() {
        audioFileName = getExternalCacheDir().getAbsolutePath() + "/solution_voice.m4a";
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setAudioSamplingRate(44100);
        recorder.setOutputFile(audioFileName);
        try {
            recorder.prepare();
            recorder.start();
            isRecording = true;
            recordStartTime = System.currentTimeMillis();
            btnMic.setImageResource(android.R.drawable.presence_audio_online);
            startRecordingTimer();
        } catch (IOException e) {
            Log.e("SOLVE_DOUBT", "Recording failed", e);
        }
    }

    private void startRecordingTimer() {
        recordingSeconds = 0;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    recordingSeconds++;
                    int mins = recordingSeconds / 60;
                    int secs = recordingSeconds % 60;
                    editSolution.setHint(String.format(Locale.getDefault(), "Recording: %02d:%02d", mins, secs));
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopRecording() {
        if (isRecording && recorder != null) {
            try {
                isRecording = false;
                timerHandler.removeCallbacks(timerRunnable);
                editSolution.setHint("Type your solution...");
                long duration = System.currentTimeMillis() - recordStartTime;
                recorder.stop();
                recorder.release();
                recorder = null;
                btnMic.setImageResource(android.R.drawable.ic_btn_speak_now);
                if (duration > 1000) {
                    processAudioFile();
                } else {
                    Toast.makeText(this, "Recording too short", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("SOLVE_DOUBT", "Stop recording failed", e);
                isRecording = false;
            }
        }
    }

    private void processAudioFile() {
        File f = new File(audioFileName);
        if (f.exists()) {
            byte[] b = fileToByteArray(f);
            if (b != null) {
                audioBase64 = Base64.encodeToString(b, Base64.DEFAULT);
                Toast.makeText(this, "Voice added to solution", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private byte[] fileToByteArray(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = fis.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri path = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), path);
                cardImagePreview.setVisibility(View.VISIBLE);
                imgAttachmentPreview.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void submitSolution(String solution) {
        String url = "http://10.0.2.2/eduinsight_api/solve_doubt.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getString("status").equals("success")) {
                            Toast.makeText(this, "Doubt Solved Successfully!", Toast.LENGTH_SHORT).show();
                            finish(); 
                        } else {
                            Toast.makeText(this, "Failed: " + jsonResponse.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Response: " + response);
                    }
                },
                error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("doubt_id", String.valueOf(doubtId));
                params.put("solution", solution);
                params.put("teacher_name", teacherName);
                params.put("sender_id", String.valueOf(currentUserId));
                params.put("sender_role", "teacher");
                if (bitmap != null) {
                    params.put("image", imageToString(bitmap));
                }
                if (audioBase64 != null) {
                    params.put("audio", audioBase64);
                }
                return params;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    private String imageToString(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, baos);
        byte[] imgBytes = baos.toByteArray();
        return Base64.encodeToString(imgBytes, Base64.DEFAULT);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
