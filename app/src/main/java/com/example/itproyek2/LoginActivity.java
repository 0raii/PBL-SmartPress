package com.example.itproyek2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
        // ... (tema logic tetap sama)
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

        if (btnGoogle == null) {
            android.util.Log.e("LoginActivity", "btnGoogleLogin not found in layout!");
        }

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
                    // Paksa Google Sign Out dulu agar selalu muncul pilihan akun (tidak otomatis login)
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
                // Ambil data user dari SQLite untuk ditaruh di Profile
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
                        // Clear history when logging in as a new user
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
            } else if (email.equals("admin@gmail.com") && password.equals("123456")) {
                // Fallback dummy admin
                Toast.makeText(this, "Login Admin Berhasil!", Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_logged_in", true);
                editor.putString("profile_role", "Admin");
                editor.apply();
                startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Email atau password salah!", Toast.LENGTH_SHORT).show();
            }
        });

        tvForgotPassword.setOnClickListener(v -> 
            Toast.makeText(this, "fitur reset pass blm ada nih", Toast.LENGTH_SHORT).show()
        );

        tvRegister.setOnClickListener(v -> 
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
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

            // Signed in successfully, show authenticated UI.
            String name = account.getDisplayName();
            String email = account.getEmail();
            String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";

            if (dbHelper.updateGoogleUser(name, email, photoUrl)) {
                Cursor cursor = dbHelper.getUserData(email);
                if (cursor != null && cursor.moveToFirst()) {
                    try {
                        int idIndex = cursor.getColumnIndex(DatabaseHelper.COL_ID);
                        int roleIndex = cursor.getColumnIndex(DatabaseHelper.COL_ROLE);
                        int passIndex = cursor.getColumnIndex(DatabaseHelper.COL_PASSWORD);

                        int id = (idIndex != -1) ? cursor.getInt(idIndex) : -1;
                        String role = (roleIndex != -1) ? cursor.getString(roleIndex) : "User";
                        String pass = (passIndex != -1) ? cursor.getString(passIndex) : "";
                        
                        SharedPreferences prefs = getSharedPreferences("SmartLampPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("is_logged_in", true);
                        editor.putInt("profile_id", id);
                        editor.putString("profile_name", name);
                        editor.putString("profile_email", email);
                        editor.putString("profile_role", role);
                        editor.putString("profile_image_uri", photoUrl);
                        editor.putBoolean("has_password", (pass != null && !pass.isEmpty()));
                        // Clear history when logging in as a new user
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
