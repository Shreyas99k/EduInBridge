package com.example.eduinsight;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_DEBUG";
    private static final String CHANNEL_ID = "edu_pro_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "!!! FCM MESSAGE RECEIVED !!!");

        SharedPreferences userPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String currentUserRole = userPref.getString("role", "").toLowerCase().trim();

        Map<String, String> data = remoteMessage.getData();
        String title = data.getOrDefault("title", "New Notification");
        String body = data.getOrDefault("message", "");
        String type = data.getOrDefault("type", "general");
        String senderId = data.getOrDefault("sender_id", "-1");
        String senderRole = data.getOrDefault("sender_role", "");
        String targetRole = data.getOrDefault("target_role", "all");
        String targetId = data.getOrDefault("target_id", "0");

        // 1. BROADCAST LOGIC (From Institution)
        if (type.equalsIgnoreCase("broadcast")) {
            String displayTitle = "From Institution";
            String displayBody = title; // The user-defined title from the form
            String content = body;      // The user-defined message from the form

            if (targetRole.equals("student") || targetRole.equals("all")) {
                saveNotificationLocally(displayTitle, displayBody, content, "student");
            }
            if (targetRole.equals("teacher") || targetRole.equals("mentor") || targetRole.equals("all")) {
                saveNotificationLocally(displayTitle, displayBody, content, "teacher");
            }

            boolean isMeTargeted = targetRole.equals("all") || currentUserRole.equals(targetRole);
            if (isMeTargeted) {
                showNotification(displayTitle, displayBody); // Show popup with clean title
            }
            return;
        }

        // 2. OTHER NOTIFICATIONS (Chat, Solutions, etc.)
        String intendedRecipientRole = "";
        if (type.equalsIgnoreCase("solution")) intendedRecipientRole = "student";
        else if (type.equalsIgnoreCase("doubt_posted")) intendedRecipientRole = "teacher";
        else if (type.equalsIgnoreCase("chat")) intendedRecipientRole = senderRole.equals("teacher") ? "student" : "teacher";

        if (!intendedRecipientRole.isEmpty()) {
            saveNotificationLocally(title, body, body, intendedRecipientRole);

            boolean isMeTheSender = senderId.equals(String.valueOf(userPref.getInt("user_id", -1)));
            boolean isMeTheRecipient = currentUserRole.equals(intendedRecipientRole);

            if (isMeTheRecipient && !isMeTheSender) {
                showNotification(title, body);
            }
        }
    }

    private void showNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "EduInsight Alerts", NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, NotificationsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pi);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void saveNotificationLocally(String title, String body, String content, String role) {
        if (role == null || role.isEmpty()) return;
        String prefName = role.equals("teacher") ? "TeacherNotifications" : "StudentNotifications";
        SharedPreferences pref = getSharedPreferences(prefName, MODE_PRIVATE);
        try {
            String existing = pref.getString("list", "[]");
            if (existing == null) existing = "[]";
            JSONArray arr = new JSONArray(existing);
            
            JSONObject obj = new JSONObject();
            obj.put("title", title);
            obj.put("body", body);
            obj.put("content", content);
            obj.put("time", new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date()));

            JSONArray newList = new JSONArray();
            newList.put(obj);
            for (int i = 0; i < Math.min(arr.length(), 49); i++) {
                newList.put(arr.get(i));
            }
            pref.edit().putString("list", newList.toString()).apply();
            Log.d(TAG, "Notification saved to " + prefName);
        } catch (Exception e) {
            Log.e(TAG, "Error saving notification: " + e.getMessage());
        }
    }
}
