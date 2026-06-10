package com.example.eduinsight;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class ViewFullImageActivity extends AppCompatActivity {

    private String imageUrl;
    private int messageId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_full_image);

        imageUrl = getIntent().getStringExtra("image_url");
        messageId = getIntent().getIntExtra("message_id", -1);

        Toolbar toolbar = findViewById(R.id.toolbarImage);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }

        ImageView imgView = findViewById(R.id.imgFullView);
        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).into(imgView);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_view_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            saveImageToGallery();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveImageToGallery() {
        if (imageUrl == null) return;
        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();
        Glide.with(this).asBitmap().load(imageUrl).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                saveBitmap(resource);
            }
            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {}
        });
    }

    private void saveBitmap(Bitmap bitmap) {
        String filename = "EduInsight_" + System.currentTimeMillis() + ".jpg";
        OutputStream fos = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/EduInSight");
                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    fos = getContentResolver().openOutputStream(imageUri);
                }
            } else {
                String imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM).toString();
                java.io.File imageDirFile = new java.io.File(imagesDir);
                if (!imageDirFile.exists()) imageDirFile.mkdirs();
                java.io.File image = new java.io.File(imagesDir, filename);
                fos = new java.io.FileOutputStream(image);
            }
            
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
                Toast.makeText(this, "Saved to Gallery!", Toast.LENGTH_SHORT).show();
                notifySenderSaved();
            }
        } catch (Exception e) {
            Log.e("SAVE_IMG", "Error: " + e.getMessage());
            Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void notifySenderSaved() {
        if (messageId == -1) return;
        String url = "http://10.0.2.2/eduinsight_api/notify_saved.php";
        StringRequest request = new StringRequest(Request.Method.POST, url, response -> {
            // Notification tracked successfully
        }, null) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("message_id", String.valueOf(messageId));
                return p;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
