package com.example.eduinsight;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MaterialListActivity extends AppCompatActivity {

    private RecyclerView rvMaterials;
    private ProgressBar progressBar;
    private TextView txtNoData;
    private MaterialAdapter adapter;
    private List<JSONObject> materialList = new ArrayList<>();
    private String type, institutionId, departmentId, subjectName;
    
    private String pendingDownloadUrl;
    private final ActivityResultLauncher<String> createFileLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("*/*"),
                    uri -> {
                        if (uri != null) {
                            downloadFileToUri(pendingDownloadUrl, uri);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material_list);

        type = getIntent().getStringExtra("type");
        institutionId = getIntent().getStringExtra("institution_id");
        departmentId = getIntent().getStringExtra("department_id");
        subjectName = getIntent().getStringExtra("subject_name");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            String title = type != null ? type.substring(0, 1).toUpperCase() + type.substring(1).replace("_", " ") : "Materials";
            getSupportActionBar().setTitle(title);
        }

        // Fix: Explicitly handle navigation click for the toolbar back arrow
        toolbar.setNavigationOnClickListener(v -> finish());

        rvMaterials = findViewById(R.id.rvMaterials);
        progressBar = findViewById(R.id.progressBar);
        txtNoData = findViewById(R.id.txtNoData);

        rvMaterials.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MaterialAdapter(materialList);
        rvMaterials.setAdapter(adapter);

        fetchMaterials();
    }

    private void fetchMaterials() {
        progressBar.setVisibility(View.VISIBLE);
        String url = "http://10.0.2.2/eduinsight_api/get_materials.php?" +
                "type=" + Uri.encode(type) + 
                "&institution_id=" + Uri.encode(institutionId) +
                "&department_id=" + Uri.encode(departmentId) +
                "&subject_name=" + Uri.encode(subjectName);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONArray arr = new JSONArray(response);
                        materialList.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            materialList.add(arr.getJSONObject(i));
                        }
                        if (materialList.isEmpty()) findViewById(R.id.emptyState).setVisibility(View.VISIBLE);
                        else findViewById(R.id.emptyState).setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        Log.e("MATERIAL_LIST", "Error", e);
                        findViewById(R.id.emptyState).setVisibility(View.VISIBLE);
                    }
                }, error -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
        });
        Volley.newRequestQueue(this).add(request);
    }

    private void downloadFileToUri(String fileUrl, Uri targetUri) {
        Toast.makeText(this, "Starting download...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Server returned HTTP " + connection.getResponseCode());
                }

                try (InputStream input = connection.getInputStream();
                     OutputStream output = getContentResolver().openOutputStream(targetUri)) {
                    byte[] data = new byte[4096];
                    int count;
                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                    }
                }
                
                runOnUiThread(() -> Toast.makeText(this, "Download saved successfully", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e("DOWNLOAD_ERROR", "Failed to download", e);
                runOnUiThread(() -> Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private String getTimeAgo(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(dateStr);
            if (date == null) return dateStr;

            long time = date.getTime();
            long now = System.currentTimeMillis();
            long diff = now - time;

            if (diff < 60000) return "Just now";
            if (diff < 3600000) return (diff / 60000) + " mins ago";
            if (diff < 86400000) return (diff / 3600000) + " hours ago";
            return (diff / 86400000) + " days ago";
        } catch (Exception e) {
            return dateStr;
        }
    }

    private class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.ViewHolder> {
        List<JSONObject> list;
        MaterialAdapter(List<JSONObject> list) { this.list = list; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_material, parent, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                JSONObject obj = list.get(position);
                String fileName = obj.getString("file_name");
                String uploader = obj.optString("uploader_name", "Unknown");
                String timeStr = obj.optString("uploaded_at", "");
                
                holder.title.setText(fileName);
                holder.subject.setText(obj.optString("subject_name", "General"));
                holder.uploaderInfo.setText("Uploaded by " + uploader + " • " + getTimeAgo(timeStr));
                
                String fileUrl = "http://10.0.2.2/eduinsight_api/uploads/" + obj.getString("file_path");

                if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg") || fileName.toLowerCase().endsWith(".png")) {
                    Glide.with(MaterialListActivity.this).load(fileUrl).into(holder.icon);
                } else {
                    holder.icon.setImageResource(android.R.drawable.ic_menu_save);
                }
                
                holder.btnDownload.setOnClickListener(v -> {
                    pendingDownloadUrl = fileUrl;
                    createFileLauncher.launch(fileName);
                });

                holder.itemView.setOnClickListener(v -> {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(fileUrl));
                    startActivity(i);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, subject, uploaderInfo;
            ImageView icon;
            ImageButton btnDownload;
            ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.txtMaterialTitle);
                subject = v.findViewById(R.id.txtMaterialSubject);
                uploaderInfo = v.findViewById(R.id.txtUploaderInfo);
                icon = v.findViewById(R.id.imgMaterialIcon);
                btnDownload = v.findViewById(R.id.btnDownload);
            }
        }
    }
}
