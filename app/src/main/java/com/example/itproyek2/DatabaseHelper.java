package com.example.itproyek2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SmartPress.db";
    private static final int DATABASE_VERSION = 2; // Incremented version

    // Tabel User
    public static final String TABLE_USERS = "users";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_EMAIL = "email";
    public static final String COL_PASSWORD = "password";
    public static final String COL_PHONE = "phone";
    public static final String COL_ROLE = "role";
    public static final String COL_PHOTO = "photo";

    // Tabel Automatic Logs
    public static final String TABLE_AUTO_LOGS = "automatic_logs";
    public static final String COL_LOG_ID = "log_id";
    public static final String COL_LOG_USER_ID = "user_id"; // New column
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_ACTION = "action"; // "HIDUP" atau "MATI"
    public static final String COL_LUMEN = "lumen";
    public static final String COL_WATT = "watt";

    // Tabel Daily Stats
    public static final String TABLE_DAILY_STATS = "daily_stats";
    public static final String COL_STATS_USER_ID = "user_id"; // New column
    public static final String COL_DATE = "date"; // YYYY-MM-DD
    public static final String COL_KWH = "kwh";
    public static final String COL_AUTO_COUNT = "auto_count";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 3); // Upgraded to version 3
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT, " +
                COL_EMAIL + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT, " +
                COL_PHONE + " TEXT, " +
                COL_ROLE + " TEXT, " +
                COL_PHOTO + " TEXT)";
        db.execSQL(createTable);

        String createAutoLogsTable = "CREATE TABLE " + TABLE_AUTO_LOGS + " (" +
                COL_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_LOG_USER_ID + " INTEGER, " +
                COL_TIMESTAMP + " LONG, " +
                COL_ACTION + " TEXT, " +
                COL_LUMEN + " INTEGER, " +
                COL_WATT + " REAL)";
        db.execSQL(createAutoLogsTable);

        String createDailyStatsTable = "CREATE TABLE " + TABLE_DAILY_STATS + " (" +
                COL_STATS_USER_ID + " INTEGER, " +
                COL_DATE + " TEXT, " +
                COL_KWH + " REAL, " +
                COL_AUTO_COUNT + " INTEGER, " +
                "PRIMARY KEY (" + COL_STATS_USER_ID + ", " + COL_DATE + "))";
        db.execSQL(createDailyStatsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUTO_LOGS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DAILY_STATS);
            onCreate(db);
        }
    }

    // Fungsi Tambah Log Otomatis
    public void addAutoLog(int userId, String action, int lumen, double watt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_LOG_USER_ID, userId);
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        values.put(COL_ACTION, action);
        values.put(COL_LUMEN, lumen);
        values.put(COL_WATT, watt);
        db.insert(TABLE_AUTO_LOGS, null, values);
    }

    // Fungsi Update Statistik Harian
    public void updateDailyStats(int userId, String date, double kwh, int incrementAutoCount) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_AUTO_COUNT + " FROM " + TABLE_DAILY_STATS + 
                " WHERE " + COL_STATS_USER_ID + " = ? AND " + COL_DATE + " = ?", 
                new String[]{String.valueOf(userId), date});
        
        ContentValues values = new ContentValues();
        values.put(COL_KWH, kwh);
        
        if (cursor.moveToFirst()) {
            int currentCount = cursor.getInt(0);
            values.put(COL_AUTO_COUNT, currentCount + incrementAutoCount);
            db.update(TABLE_DAILY_STATS, values, COL_STATS_USER_ID + " = ? AND " + COL_DATE + " = ?", 
                    new String[]{String.valueOf(userId), date});
        } else {
            values.put(COL_STATS_USER_ID, userId);
            values.put(COL_DATE, date);
            values.put(COL_AUTO_COUNT, incrementAutoCount);
            db.insert(TABLE_DAILY_STATS, null, values);
        }
        cursor.close();
    }

    // Ambil Semua Log Otomatis (untuk PDF)
    public Cursor getAllAutoLogs(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_AUTO_LOGS + 
                " WHERE " + COL_LOG_USER_ID + " = ?" +
                " ORDER BY " + COL_TIMESTAMP + " DESC", new String[]{String.valueOf(userId)});
    }

    // Ambil Log Otomatis berdasarkan rentang waktu (untuk PDF Filtered)
    public Cursor getFilteredAutoLogs(int userId, long startTime, long endTime) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_AUTO_LOGS + 
                " WHERE " + COL_LOG_USER_ID + " = ? AND " + COL_TIMESTAMP + " BETWEEN ? AND ? ORDER BY " + COL_TIMESTAMP + " DESC", 
                new String[]{String.valueOf(userId), String.valueOf(startTime), String.valueOf(endTime)});
    }

    // Hitung jumlah log otomatis hari ini
    public int getAutoLogSummary(int userId, long startTime, long endTime) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_AUTO_LOGS + 
                " WHERE " + COL_LOG_USER_ID + " = ? AND " + COL_TIMESTAMP + " BETWEEN ? AND ?", 
                new String[]{String.valueOf(userId), String.valueOf(startTime), String.valueOf(endTime)});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // Ambil Log Otomatis dengan Pagination (Limit & Offset)
    public Cursor getPagedAutoLogs(int userId, long startTime, long endTime, int limit, int offset) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_AUTO_LOGS + 
                " WHERE " + COL_LOG_USER_ID + " = ? AND " + COL_TIMESTAMP + " BETWEEN ? AND ? " +
                " ORDER BY " + COL_TIMESTAMP + " DESC " +
                " LIMIT ? OFFSET ?", 
                new String[]{String.valueOf(userId), String.valueOf(startTime), String.valueOf(endTime), String.valueOf(limit), String.valueOf(offset)});
    }

    // Ambil Semua User
    public Cursor getAllUsers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS, null);
    }

    // Registrasi User (dari halaman Register)
    public boolean registerUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);
        values.put(COL_PHONE, "");
        values.put(COL_ROLE, "User");
        values.put(COL_PHOTO, "");
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    // Update data dari Google Login
    public boolean updateGoogleUser(String name, String email, String photo) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = getUserData(email);
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_PHOTO, photo);
        
        if (cursor.moveToFirst()) {
            // User sudah ada, update saja (jangan timpa password jika sudah ada)
            db.update(TABLE_USERS, values, COL_EMAIL + " = ?", new String[]{email});
            cursor.close();
            return true;
        } else {
            // User baru dari Google
            values.put(COL_EMAIL, email);
            values.put(COL_PASSWORD, ""); // Password kosong untuk nanti diset manual
            values.put(COL_PHONE, "");
            values.put(COL_ROLE, "User");
            long result = db.insert(TABLE_USERS, null, values);
            cursor.close();
            return result != -1;
        }
    }

    // Set password manual untuk user Google
    public boolean setManualPassword(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PASSWORD, password);
        return db.update(TABLE_USERS, values, COL_EMAIL + " = ?", new String[]{email}) > 0;
    }

    // Tambah User oleh Admin
    public boolean addUserByAdmin(String name, String email, String password, String phone, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);
        values.put(COL_PHONE, phone);
        values.put(COL_ROLE, role);
        values.put(COL_PHOTO, "");
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    // Update User
    public boolean updateUser(int id, String name, String email, String password, String phone, String role, String photo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);
        values.put(COL_PHONE, phone);
        values.put(COL_ROLE, role);
        values.put(COL_PHOTO, photo);
        return db.update(TABLE_USERS, values, COL_ID + " = ?", new String[]{String.valueOf(id)}) > 0;
    }

    // Hapus User
    public boolean deleteUser(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_USERS, COL_ID + " = ?", new String[]{String.valueOf(id)}) > 0;
    }

    // Reset Semua User (Kecuali Admin Hardcoded jika perlu, atau semua)
    public void resetAllUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_USERS);
        // Opsional: Reset auto increment ID
        db.execSQL("DELETE FROM sqlite_sequence WHERE name='" + TABLE_USERS + "'");
    }

    // Fungsi Cek Login
    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COL_ID};
        String selection = COL_EMAIL + " = ?" + " AND " + COL_PASSWORD + " = ?";
        String[] selectionArgs = {email, password};
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }
    
    // Ambil data user lengkap berdasarkan email
    public Cursor getUserData(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COL_EMAIL + " = ?", new String[]{email});
    }
}
