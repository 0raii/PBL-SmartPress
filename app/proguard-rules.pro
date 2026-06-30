# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve Firebase Model Classes (Crucial for IoT data display)
-keepclassmembers class com.example.itproyek2.UserModel { *; }
-keepclassmembers class com.example.itproyek2.AutoLogModel { *; }
-keepclassmembers class com.example.itproyek2.HistoryItem { *; }

# EncryptedSharedPreferences Support
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Firebase Realtime Database
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }

# Support libraries
-dontwarn androidx.**
-keep class androidx.** { *; }

# MPAndroidChart (Agar grafik tidak error)
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Strip all Logging (High Security Boost)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Strip Stack Traces (Prevent code leakage)
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile