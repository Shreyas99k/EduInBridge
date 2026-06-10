package com.example.eduinsight;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        MaterialCardView cardInstitution = findViewById(R.id.cardInstitution);
        MaterialCardView cardMentor = findViewById(R.id.cardMentor);
        MaterialCardView cardStudent = findViewById(R.id.cardStudent);

        cardInstitution.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignupActivity.class);
            intent.putExtra("selected_role", "institution");
            startActivity(intent);
        });

        cardMentor.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignupActivity.class);
            intent.putExtra("selected_role", "teacher");
            startActivity(intent);
        });

        cardStudent.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignupActivity.class);
            intent.putExtra("selected_role", "student");
            startActivity(intent);
        });
    }
}
