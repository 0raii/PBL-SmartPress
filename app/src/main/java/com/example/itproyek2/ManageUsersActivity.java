package com.example.itproyek2;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private UserAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<UserModel> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        dbHelper = new DatabaseHelper(this);
        rvUsers = findViewById(R.id.rvUsers);
        MaterialCardView btnBack = findViewById(R.id.btnBack);
        FloatingActionButton fabAddUser = findViewById(R.id.fabAddUser);

        btnBack.setOnClickListener(v -> finish());

        fabAddUser.setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditUserActivity.class));
        });

        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Coba sinkron dari Firebase setiap kali masuk halaman ini
        dbHelper.syncAllUsersFromFirebase(new DatabaseHelper.OnSyncCompleteListener() {
            @Override
            public void onSyncSuccess() {
                loadUsers(); // Refresh list setelah sinkron sukses
            }

            @Override
            public void onSyncFailure(String error) {
                Toast.makeText(ManageUsersActivity.this, "Gagal sinkron cloud: " + error, Toast.LENGTH_SHORT).show();
                loadUsers(); // Tetap load data lokal jika gagal
            }
        });
    }

    private void setupRecyclerView() {
        userList = new ArrayList<>();
        adapter = new UserAdapter(userList, new UserAdapter.OnUserClickListener() {
            @Override
            public void onEditClick(UserModel user) {
                Intent intent = new Intent(ManageUsersActivity.this, AddEditUserActivity.class);
                intent.putExtra("USER_ID", user.getId());
                intent.putExtra("USER_NAME", user.getName());
                intent.putExtra("USER_EMAIL", user.getEmail());
                intent.putExtra("USER_PASSWORD", user.getPassword());
                intent.putExtra("USER_PHONE", user.getPhone());
                intent.putExtra("USER_ROLE", user.getRole());
                intent.putExtra("USER_PHOTO", user.getPhoto());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(UserModel user) {
                new AlertDialog.Builder(ManageUsersActivity.this)
                        .setTitle("Hapus User")
                        .setMessage("Apakah Anda yakin ingin menghapus " + user.getName() + "?")
                        .setPositiveButton("Ya", (dialog, which) -> {
                            if (dbHelper.deleteUser(user.getId())) {
                                Toast.makeText(ManageUsersActivity.this, "User dihapus", Toast.LENGTH_SHORT).show();
                                loadUsers();
                            }
                        })
                        .setNegativeButton("Tidak", null)
                        .show();
            }

            @Override
            public void onMonitorClick(UserModel user) {
                Intent intent = new Intent(ManageUsersActivity.this, AdminMonitoringActivity.class);
                intent.putExtra("TARGET_NAME", user.getName());
                intent.putExtra("TARGET_EMAIL", user.getEmail());
                startActivity(intent);
            }
        });
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);
    }

    private void loadUsers() {
        userList.clear();
        Cursor cursor = dbHelper.getAllUsers();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EMAIL));
                String password = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PASSWORD));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PHONE));
                String role = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROLE));
                String photo = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PHOTO));
                userList.add(new UserModel(id, name, email, password, phone, role, photo));
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }
}
