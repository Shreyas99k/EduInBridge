package com.example.eduinsight;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AiChatActivity extends AppCompatActivity {

    private RecyclerView rvAiMessages;
    private EditText editAiQuery;
    private AiChatAdapter adapter;
    private List<AiChatMessage> messageList = new ArrayList<>();
    private GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI Teaching Assistant");
        }

        // Using gemini-3-flash-preview as per your verification that it works best for your project
        try {
            GenerativeModel gm = new GenerativeModel("gemini-3-flash-preview", Config.GEMINI_API_KEY);
            model = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            Log.e("AI_CHAT", "Init failed", e);
        }

        rvAiMessages = findViewById(R.id.rvAiMessages);
        editAiQuery = findViewById(R.id.editAiQuery);
        MaterialButton btnSendAi = findViewById(R.id.btnSendAi);

        adapter = new AiChatAdapter(messageList);
        rvAiMessages.setLayoutManager(new LinearLayoutManager(this));
        rvAiMessages.setAdapter(adapter);

        // Welcome message
        messageList.add(new AiChatMessage("Hello! I'm your EduInsight AI Assistant. How can I help you with your lessons today?", false));
        adapter.notifyItemInserted(0);

        btnSendAi.setOnClickListener(v -> {
            String query = editAiQuery.getText().toString().trim();
            if (!query.isEmpty()) {
                sendMessageToAi(query);
                editAiQuery.setText("");
            }
        });
    }

    private void sendMessageToAi(String query) {
        messageList.add(new AiChatMessage(query, true));
        adapter.notifyItemInserted(messageList.size() - 1);
        rvAiMessages.scrollToPosition(messageList.size() - 1);

        // Add a "Thinking..." placeholder
        AiChatMessage thinkingMsg = new AiChatMessage("Thinking...", false);
        messageList.add(thinkingMsg);
        int thinkingPos = messageList.size() - 1;
        adapter.notifyItemInserted(thinkingPos);

        Content content = new Content.Builder()
                .addText(query)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    messageList.remove(thinkingPos);
                    adapter.notifyItemRemoved(thinkingPos);
                    String aiText = result.getText();
                    messageList.add(new AiChatMessage(aiText, false));
                    adapter.notifyItemInserted(messageList.size() - 1);
                    rvAiMessages.scrollToPosition(messageList.size() - 1);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    messageList.remove(thinkingPos);
                    adapter.notifyItemRemoved(thinkingPos);
                    Toast.makeText(AiChatActivity.this, "AI error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }, executor);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
