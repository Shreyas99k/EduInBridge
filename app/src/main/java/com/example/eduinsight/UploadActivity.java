package com.example.eduinsight;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadActivity extends AppCompatActivity {

    private static final String TAG = "UploadActivity";
    private static final int PICK_FILE_REQUEST = 1;
    private static final int SCAN_DOC_REQUEST = 2;

    // IMPORTANT: 10.0.2.2 is correct for Emulator. Change to your local IP if using a physical phone.
    private static final String BASE_URL = "http://10.0.2.2/eduinsight_api/";

    private View step0Choice, step1Type, stepSelection;
    private TextView txtSelectedType, txtFileName, txtTypeHeader;
    private AutoCompleteTextView spinnerDepartment, spinnerSubject;
    private MaterialButton btnUpload, btnSelectFile;
    
    private String selectedType = "";
    private Uri selectedFileUri;
    private String institutionId, uploaderId;
    private int selectedDeptId = -1;
    private boolean isDownloadMode = false;
    private boolean isDownloadOnly = false;
    
    private final List<JSONObject> allDepartments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        
        // FIX: Correctly determine the institution ID
        int instIdFromPref = pref.getInt("institution_id", -1);
        int userIdFromPref = pref.getInt("user_id", -1);
        
        // If institution_id is -1, it means the logged-in user IS the institution
        if (instIdFromPref == -1) {
            institutionId = String.valueOf(userIdFromPref);
        } else {
            institutionId = String.valueOf(instIdFromPref);
        }
        uploaderId = String.valueOf(userIdFromPref);

        Log.d(TAG, "Using Institution ID: " + institutionId);

        initViews();
        setupChoiceButtons();
        setupTypeButtons();
        fetchCurriculumData();

        String mode = getIntent().getStringExtra("mode");
        if ("download_only".equals(mode)) {
            isDownloadOnly = true;
            isDownloadMode = true;
            showTypeSelection();
        }
    }

    private void initViews() {
        step0Choice = findViewById(R.id.step0Choice);
        step1Type = findViewById(R.id.step1Type);
        stepSelection = findViewById(R.id.stepSelection);
        txtTypeHeader = findViewById(R.id.txtTypeHeader);
        txtSelectedType = findViewById(R.id.txtSelectedType);
        txtFileName = findViewById(R.id.txtFileName);
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        spinnerSubject = findViewById(R.id.spinnerSubject);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnUpload = findViewById(R.id.btnUpload);

        // Ensure dropdowns show up immediately on click
        setupDropdownBehavior(spinnerDepartment);
        setupDropdownBehavior(spinnerSubject);

        btnSelectFile.setOnClickListener(v -> pickFile());
        btnUpload.setOnClickListener(v -> {
            if (isDownloadMode) navigateToMaterialList();
            else uploadMaterial();
        });
        
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            stepSelection.setVisibility(View.GONE);
            step1Type.setVisibility(View.VISIBLE);
        });
    }

    private void setupDropdownBehavior(AutoCompleteTextView view) {
        view.setThreshold(0);
        view.setOnClickListener(v -> view.showDropDown());
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) view.showDropDown();
        });
    }

    private void setupChoiceButtons() {
        findViewById(R.id.btnChoiceUpload).setOnClickListener(v -> {
            isDownloadMode = false;
            showTypeSelection();
        });
        findViewById(R.id.btnChoiceDownload).setOnClickListener(v -> {
            isDownloadMode = true;
            showTypeSelection();
        });
    }

    private void showTypeSelection() {
        step0Choice.setVisibility(View.GONE);
        step1Type.setVisibility(View.VISIBLE);
        stepSelection.setVisibility(View.GONE);
    }

    private void setupTypeButtons() {
        findViewById(R.id.btnTypeTextbook).setOnClickListener(v -> handleCategorySelection("textbook"));
        findViewById(R.id.btnTypeQuestionPaper).setOnClickListener(v -> handleCategorySelection("question_paper"));
        findViewById(R.id.btnTypeNotes).setOnClickListener(v -> handleCategorySelection("notes"));
        
        View btnScan = findViewById(R.id.btnScanDoc);
        if (btnScan != null) {
            btnScan.setOnClickListener(v -> {
                // Placeholder for Document Scanning logic
                // In a production app, we would use ML Kit Document Scanner here
                Toast.makeText(this, "Opening Smart Document Scanner...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, SCAN_DOC_REQUEST);
            });
        }
    }

    private void handleCategorySelection(String type) {
        selectedType = type;
        String label = type.replace("_", " ").toUpperCase();
        step1Type.setVisibility(View.GONE);
        stepSelection.setVisibility(View.VISIBLE);
        
        if (isDownloadMode) {
            txtSelectedType.setText("SEARCHING " + label);
            btnSelectFile.setVisibility(View.GONE);
            txtFileName.setVisibility(View.GONE);
            btnUpload.setText("VIEW MATERIALS");
            btnUpload.setEnabled(true);
        } else {
            txtSelectedType.setText("UPLOADING " + label);
            btnSelectFile.setVisibility(View.VISIBLE);
            txtFileName.setVisibility(View.VISIBLE);
            btnUpload.setText("START UPLOAD");
            btnUpload.setEnabled(selectedFileUri != null);
        }
    }

    private void navigateToMaterialList() {
        String subject = spinnerSubject.getText().toString().trim();
        if (selectedDeptId == -1 || subject.isEmpty()) {
            Toast.makeText(this, "Please select Department and Subject", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, MaterialListActivity.class);
        intent.putExtra("type", selectedType);
        intent.putExtra("institution_id", institutionId);
        intent.putExtra("department_id", String.valueOf(selectedDeptId));
        intent.putExtra("subject_name", subject);
        startActivity(intent);
    }

    private void fetchCurriculumData() {
        String url = BASE_URL + "get_curriculum.php?institution_id=" + institutionId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        allDepartments.clear();
                        JSONArray deptArr = json.optJSONArray("departments");
                        if (deptArr != null && deptArr.length() > 0) {
                            List<String> dNames = new ArrayList<>();
                            for (int i = 0; i < deptArr.length(); i++) {
                                JSONObject obj = deptArr.getJSONObject(i);
                                allDepartments.add(obj);
                                dNames.add(obj.getString("name"));
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, dNames);
                            spinnerDepartment.setAdapter(adapter);
                            spinnerDepartment.setOnItemClickListener((parent, view, position, id) -> {
                                String selectedName = parent.getItemAtPosition(position).toString();
                                for (JSONObject d : allDepartments) {
                                    if (d.optString("name").equalsIgnoreCase(selectedName)) {
                                        selectedDeptId = d.optInt("id");
                                        fetchSubjectsForDepartment(selectedDeptId);
                                        break;
                                    }
                                }
                            });
                        } else {
                            Toast.makeText(this, "No departments found for this institution.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) { 
                        Log.e(TAG, "Curriculum Parse error: " + e.getMessage());
                        Toast.makeText(this, "Error parsing curriculum data", Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
                    Log.e(TAG, "Network error fetching curriculum: " + error.toString());
                    Toast.makeText(this, "Network error: Couldn't reach server.", Toast.LENGTH_SHORT).show();
                });
        Volley.newRequestQueue(this).add(request);
    }

    private void fetchSubjectsForDepartment(int deptId) {
        String url = BASE_URL + "get_curriculum.php?institution_id=" + institutionId + "&department_id=" + deptId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        JSONArray subArr = json.optJSONArray("subjects");
                        if (subArr != null && subArr.length() > 0) {
                            List<String> sNames = new ArrayList<>();
                            for (int i = 0; i < subArr.length(); i++) sNames.add(subArr.getString(i));
                            ArrayAdapter<String> subAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sNames);
                            spinnerSubject.setText(""); 
                            spinnerSubject.setAdapter(subAdapter);
                        } else {
                            spinnerSubject.setText("");
                            spinnerSubject.setAdapter(null);
                            Toast.makeText(this, "No subjects in this department.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }, error -> {});
        Volley.newRequestQueue(this).add(request);
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_FILE_REQUEST) {
                selectedFileUri = data.getData();
                if (selectedFileUri != null) {
                    txtFileName.setText(getFileName(selectedFileUri));
                    btnUpload.setEnabled(true);
                }
            } else if (requestCode == SCAN_DOC_REQUEST) {
                // For demonstration, handle camera result as a "Scanned Doc"
                Toast.makeText(this, "Document Scanned Successfully!", Toast.LENGTH_SHORT).show();
                txtFileName.setText("scanned_document_" + System.currentTimeMillis() + ".jpg");
                btnUpload.setEnabled(true);
                // In actual logic, we would convert data.getExtras().get("data") to a URI
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private void uploadMaterial() {
        String subject = spinnerSubject.getText().toString().trim();
        if (subject.isEmpty() || selectedFileUri == null || selectedDeptId == -1) {
            Toast.makeText(this, "Complete all selections", Toast.LENGTH_SHORT).show();
            return;
        }
        btnUpload.setEnabled(false);
        btnUpload.setText("UPLOADING...");
        try {
            InputStream iStream = getContentResolver().openInputStream(selectedFileUri);
            byte[] inputData = getBytes(iStream);
            String encodedFile = Base64.encodeToString(inputData, Base64.NO_WRAP);
            String fileName = getFileName(selectedFileUri);

            String url = BASE_URL + "upload_material.php";
            StringRequest request = new StringRequest(Request.Method.POST, url,
                    response -> {
                        btnUpload.setEnabled(true);
                        btnUpload.setText("START UPLOAD");
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.optString("status").equals("success")) {
                                Toast.makeText(this, "Upload Successful!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Server: " + json.optString("message"), Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Server error. Check database table.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Raw Response: " + response);
                        }
                    },
                    error -> {
                        btnUpload.setEnabled(true);
                        btnUpload.setText("START UPLOAD");
                        String errMsg = "Check Connection/Server IP";
                        if (error.networkResponse == null) errMsg = "Timeout! Check PHP post_max_size.";
                        Toast.makeText(this, "Upload Failed: " + errMsg, Toast.LENGTH_LONG).show();
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("institution_id", institutionId);
                    params.put("uploader_id", uploaderId);
                    params.put("type", selectedType);
                    params.put("department_id", String.valueOf(selectedDeptId));
                    params.put("subject_name", subject);
                    params.put("file_name", fileName);
                    params.put("file_data", encodedFile);
                    return params;
                }
            };
            request.setRetryPolicy(new DefaultRetryPolicy(60000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            btnUpload.setEnabled(true);
            btnUpload.setText("START UPLOAD");
            Toast.makeText(this, "Error reading file.", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] getBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024]; int len;
        while ((len = inputStream.read(buffer)) != -1) byteBuffer.write(buffer, 0, len);
        return byteBuffer.toByteArray();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (stepSelection.getVisibility() == View.VISIBLE) {
            stepSelection.setVisibility(View.GONE);
            step1Type.setVisibility(View.VISIBLE);
            return true;
        } else if (step1Type.getVisibility() == View.VISIBLE) {
            if (isDownloadOnly) finish();
            else {
                step1Type.setVisibility(View.GONE);
                step0Choice.setVisibility(View.VISIBLE);
            }
            return true;
        }
        finish();
        return true;
    }
}
