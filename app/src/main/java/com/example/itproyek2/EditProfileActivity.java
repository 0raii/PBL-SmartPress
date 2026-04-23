package com.example.itproyek2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Patterns;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.content.Intent;
import android.graphics.Bitmap;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone;
    private AutoCompleteTextView actvRole;
    private MaterialButton btnSave, btnUbahFoto;
    private ImageView btnBack;
    private ShapeableImageView ivProfile;
    private Uri imageUri;

    private final ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    startCrop(uri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> cropImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    final Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        imageUri = resultUri;
                        ivProfile.setImageURI(resultUri);
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                    final Throwable cropError = UCrop.getError(result.getData());
                    if (cropError != null) {
                        Toast.makeText(this, cropError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void startCrop(Uri uri) {
        String destinationFileName = "cropped_profile_pic.jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));
        uCrop.withAspectRatio(1, 1);
        uCrop.withMaxResultSize(1000, 1000);
        
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        options.setHideBottomControls(false);
        options.setFreeStyleCropEnabled(true);
        
        uCrop.withOptions(options);
        cropImage.launch(uCrop.getIntent(this));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ... (tema logic tetap sama)
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("is_dark_theme", true);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etName = findViewById(R.id.etEditName);
        etEmail = findViewById(R.id.etEditEmail);
        etPhone = findViewById(R.id.etEditPhone);
        actvRole = findViewById(R.id.actvEditRole);
        btnSave = findViewById(R.id.btnSimpanProfil);
        btnBack = findViewById(R.id.btnBackEdit);
        btnUbahFoto = findViewById(R.id.btnUbahFoto);
        ivProfile = findViewById(R.id.ivEditProfilePic);

        // Setup Dropdown Role
        String[] roles = {"Admin", "Pengurus Mushola"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roles);
        actvRole.setAdapter(adapter);

        // Load data saat ini
        etName.setText(prefs.getString("profile_name", "Sofiani"));
        etEmail.setText(prefs.getString("profile_email", "sofiani@gmail.com"));
        etPhone.setText(prefs.getString("profile_phone", "08XXXXXXXXXX"));
        actvRole.setText(prefs.getString("profile_role", "Admin / Pengurus Mushola"), false);
        
        String savedImageUri = prefs.getString("profile_image_uri", null);
        if (savedImageUri != null) {
            ivProfile.setImageURI(Uri.parse(savedImageUri));
        }

        btnBack.setOnClickListener(v -> finish());
        
        btnUbahFoto.setOnClickListener(v -> getContent.launch("image/*"));

        btnSave.setOnClickListener(v -> {
            saveProfileData();
        });
    }

    private void saveProfileData() {
        String newName = etName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();
        String newRole = actvRole.getText().toString().trim();

        // Validasi Nama
        if (newName.isEmpty()) {
            etName.setError("Nama tidak boleh kosong");
            return;
        }

        // Validasi Email (Standar @ dan .com)
        if (newEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            etEmail.setError("Masukkan email yang valid (contoh: user@gmail.com)");
            return;
        }

        // Validasi No HP (Minimal 10 angka)
        if (newPhone.isEmpty() || newPhone.length() < 10) {
            etPhone.setError("Masukkan nomor ponsel yang valid");
            return;
        }

        // Validasi Role
        if (newRole.isEmpty()) {
            Toast.makeText(this, "Pilih role terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("profile_name", newName);
        editor.putString("profile_email", newEmail);
        editor.putString("profile_phone", newPhone);
        editor.putString("profile_role", newRole);

        // Simpan Foto secara lokal jika ada yang baru
        if (imageUri != null) {
            String internalPath = saveImageToInternalStorage(imageUri);
            if (internalPath != null) {
                editor.putString("profile_image_uri", internalPath);
            }
        }

        editor.apply();

        Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private String saveImageToInternalStorage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            File file = new File(getFilesDir(), "profile_pic.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.close();
            is.close();
            return Uri.fromFile(file).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
