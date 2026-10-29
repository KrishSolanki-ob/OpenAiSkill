package com.example.invitationmetadata;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    private ImageView imagePreview;
    private TextView txtStatus;
    private TextView txtMetadata;
    private Button btnAnalyze;

    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedImageUri = uri;
                            imagePreview.setImageURI(uri);
                            txtStatus.setText(
                                    "Image selected. Ready to analyze."
                            );
                            txtMetadata.setText("");
                            btnAnalyze.setEnabled(true);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imagePreview = findViewById(R.id.imagePreview);
        txtStatus = findViewById(R.id.txtStatus);
        txtMetadata = findViewById(R.id.txtMetadata);

        Button btnSelectImage = findViewById(R.id.btnSelectImage);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        btnAnalyze.setEnabled(false);
        btnSelectImage.setOnClickListener(v -> imagePicker.launch("image/*"));

        btnAnalyze.setOnClickListener(v -> {
            if (selectedImageUri == null) {
                txtStatus.setText("Please select an image first.");
                return;
            }

            txtStatus.setText(
                    "Image ready. Backend integration is next."
            );
        });
    }
}
