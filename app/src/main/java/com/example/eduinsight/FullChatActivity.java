package com.example.eduinsight;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FullChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private ChatAdapter chatAdapter;
    private final List<Message> messageList = new ArrayList<>();
    private EditText editMessage;
    private TextView txtTopQuestion;
    private int doubtId;
    private final Handler handler = new Handler();
    private Runnable refreshRunnable;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int VOICE_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_chat);

        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String currentUserRole = pref.getString("role", "student").toLowerCase();

        doubtId = getIntent().getIntExtra("doubt_id", -1);
        String studentName = getIntent().getStringExtra("student_name");
        String mode = getIntent().getStringExtra("mode");

        Toolbar toolbar = findViewById(R.id.toolbarChat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(studentName);
        }

        rvChat = findViewById(R.id.rvFullChat);
        editMessage = findViewById(R.id.editChatMessage);
        txtTopQuestion = findViewById(R.id.txtChatQuestion);
        LinearLayout inputArea = findViewById(R.id.inputArea);
        ImageButton btnSend = findViewById(R.id.btnChatSend);
        ImageButton btnAttach = findViewById(R.id.btnChatAttach);
        ImageButton btnMic = findViewById(R.id.btnChatMic);

        chatAdapter = new ChatAdapter(this, messageList, currentUserRole);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);

        fetchDoubtDetails();

        if ("archive".equalsIgnoreCase(mode)) {
            if (inputArea != null) inputArea.setVisibility(View.GONE);
        }

        btnSend.setOnClickListener(v -> {
            String msg = editMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendMessage(msg, null);
                editMessage.setText("");
            }
        });

        btnAttach.setOnClickListener(v -> openGallery());
        if (btnMic != null) btnMic.setOnClickListener(v -> startVoiceRecognition());

        startChatRefresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchDoubtDetails() {
        String desc = getIntent().getStringExtra("description");
        if (desc != null) txtTopQuestion.setText(desc);
        else txtTopQuestion.setText("Doubt #" + doubtId);
    }

    private void startChatRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchMessages();
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(refreshRunnable);
    }

    private void fetchMessages() {
        String url = "http://10.0.2.2/eduinsight_api/get_message.php?doubt_id=" + doubtId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        messageList.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            messageList.add(new Message(
                                    obj.optString("message", ""),
                                    obj.optString("image_url", ""),
                                    obj.optString("sender_type", "student").toLowerCase().trim(),
                                    obj.optString("created_at", "")
                            ));
                        }
                        chatAdapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) rvChat.scrollToPosition(messageList.size() - 1);
                    } catch (Exception e) { }
                }, null);
        Volley.newRequestQueue(this).add(request);
    }

    private void sendMessage(String text, @Nullable Bitmap bitmap) {
        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int userId = pref.getInt("user_id", -1);
        String currentUserRole = pref.getString("role", "student").toLowerCase().trim();
        String url = "http://10.0.2.2/eduinsight_api/send_message.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> fetchMessages(), null) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("doubt_id", String.valueOf(doubtId));
                params.put("sender_id", String.valueOf(userId));
                params.put("sender_type", currentUserRole);
                params.put("message", text);
                if (bitmap != null) params.put("image", imageToString(bitmap));
                return params;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    private String imageToString(Bitmap bitmap) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, os);
        return Base64.encodeToString(os.toByteArray(), Base64.DEFAULT);
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak...");
        try { startActivityForResult(intent, VOICE_REQUEST_CODE); } catch (Exception e) { }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                    sendMessage("Sent an image", bitmap);
                } catch (IOException e) { }
            } else if (requestCode == VOICE_REQUEST_CODE) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (res != null && !res.isEmpty()) editMessage.setText(res.get(0));
            }
        }
    }

    @Override
    protected void onDestroy() { super.onDestroy(); handler.removeCallbacks(refreshRunnable); }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
