package com.example.itproyek2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private MaterialCardView btnGoogle;
    private DatabaseHelper dbHelper;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("is_dark_theme", true);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnGoogle = findViewById(R.id.btnGoogleLogin);

        // Configure Google Sign In
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("637607881737-pibjogl2ime1c5prl8gr801rpdkh8sd2.apps.googleusercontent.com")
                    .requestEmail()
                    .requestProfile()
                    .build();

            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

            if (btnGoogle != null) {
                btnGoogle.setOnClickListener(v -> {
                    mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                        startActivityForResult(signInIntent, RC_SIGN_IN);
                    });
                });
            }
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "GoogleSignIn init error: " + e.getMessage());
        }

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "isi dulu email ama pass nya", Toast.LENGTH_SHORT).show();
            } else if (dbHelper.checkUser(email, password)) {
                proceedLogin(email, prefs);
            } else {
                checkFirebaseLogin(email, password, prefs);
            }
        });

        tvForgotPassword.setOnClickListener(v -> 
            Toast.makeText(this, "fitur reset pass blm ada nih", Toast.LENGTH_SHORT).show()
        );

        tvRegister.setOnClickListener(v -> 
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
    }

    private void proceedLogin(String email, SharedPreferences prefs) {
        Cursor cursor = dbHelper.getUserData(email);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                int idIndex = cursor.getColumnIndex(DatabaseHelper.COL_ID);
                int nameIndex = cursor.getColumnIndex(DatabaseHelper.COL_NAME);
                int phoneIndex = cursor.getColumnIndex(DatabaseHelper.COL_PHONE);
                int roleIndex = cursor.getColumnIndex(DatabaseHelper.COL_ROLE);

                int id = (idIndex != -1) ? cursor.getInt(idIndex) : -1;
                String name = (nameIndex != -1) ? cursor.getString(nameIndex) : "User";
                String phone = (phoneIndex != -1) ? cursor.getString(phoneIndex) : "";
                String role = (roleIndex != -1) ? cursor.getString(roleIndex) : "User";
                
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_logged_in", true);
                editor.putInt("profile_id", id);
                editor.putString("profile_name", name);
                editor.putString("profile_email", email);
                editor.putString("profile_phone", phone);
                editor.putString("profile_role", role);
                editor.remove("history_data");
                editor.apply();
                
                Toast.makeText(this, "Sip, login berhasil!", Toast.LENGTH_SHORT).show();
                
                if ("Admin".equalsIgnoreCase(role)) {
                    startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                } else {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                }
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Error saat ambil data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            } finally {
                cursor.close();
            }
        }
    }

    private void checkFirebaseLogin(String email, String password, SharedPreferences prefs) {
        String safeEmail = email.replace(".", ",");
        DatabaseReference ref = FirebaseDatabase.getInstance("https://smartpress-ea81d-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users").child(safeEmail);
        
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String dbPass = snapshot.child("password").getValue(String.class);
                    if (password.equals(dbPass)) {
                        String name = snapshot.child("name").getValue(String.class);
                        String phone = snapshot.child("phone").getValue(String.class);
                        String role = snapshot.child("role").getValue(String.class);
                        
                        dbHelper.addUserByAdmin(name, email, password, phone, role); 
                        proceedLogin(email, prefs);
                    } else {
                        Toast.makeText(LoginActivity.this, "Password salah!", Toast.LENGTH_SHORT).show();
                    }
                } else if (email.equals("admin@gmail.com") && password.equals("123456")) {
                    Toast.makeText(LoginActivity.this, "Login Admin Berhasil!", Toast.LENGTH_SHORT).show();
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("is_logged_in", true);
                    editor.putString("profile_role", "Admin");
                    editor.apply();
                    startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Email tidak terdaftar!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Error Cloud: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String name = account.getDisplayName();
            String email = account.getEmail();
            String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";

            if (dbHelper.updateGoogleUser(name, email, photoUrl)) {
                Cursor cursor = dbHelper.getUserData(email);
                if (cursor != null && cursor.moveToFirst()) {
                    try {
                        int idIndex = cursor.getColumnIndex(DatabaseHelper.COL_ID);
                        int nameIndex = cursor.getColumnIndex(DatabaseHelper.COL_NAME);
                        int phoneIndex = cursor.getColumnIndex(DatabaseHelper.COL_PHONE);
                        int roleIndex = cursor.getColumnIndex(DatabaseHelper.COL_ROLE);
                        int photoIndex = cursor.getColumnIndex(DatabaseHelper.COL_PHOTO);
                        int passIndex = cursor.getColumnIndex(DatabaseHelper.COL_PASSWORD);

                        int id = (idIndex != -1) ? cursor.getInt(idIndex) : -1;
                        String dbName = (nameIndex != -1) ? cursor.getString(nameIndex) : name;
                        String dbPhone = (phoneIndex != -1) ? cursor.getString(phoneIndex) : "";
                        String role = (roleIndex != -1) ? cursor.getString(roleIndex) : "User";
                        String dbPhoto = (photoIndex != -1) ? cursor.getString(photoIndex) : photoUrl;
                        String pass = (passIndex != -1) ? cursor.getString(passIndex) : "";
                        
                        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("is_logged_in", true);
                        editor.putInt("profile_id", id);
                        editor.putString("profile_name", dbName);
                        editor.putString("profile_email", email);
                        editor.putString("profile_phone", dbPhone);
                        editor.putString("profile_role", role);
                        editor.putString("profile_image_uri", dbPhoto);
                        editor.putBoolean("has_password", (pass != null && !pass.isEmpty()));
                        editor.remove("history_data");
                        editor.apply();

                        Toast.makeText(this, "Login Google Berhasil!", Toast.LENGTH_SHORT).show();
                        
                        if ("Admin".equalsIgnoreCase(role)) {
                            startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                        } else {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        }
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error saat ambil data Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        cursor.close();
                    }
                }
            }
        } catch (ApiException e) {
            Toast.makeText(this, "Google sign in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
        }
    }
}
