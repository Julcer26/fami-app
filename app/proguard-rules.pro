# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve source file and line numbers for readable crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Kotlin metadata & annotations used by Firebase RTDB and Moshi
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class kotlin.Metadata { *; }

# Firebase Realtime Database models & @PropertyName serialization
-keep class com.example.database.** { *; }
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <methods>;
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.Exclude <methods>;
    @com.google.firebase.database.Exclude <fields>;
}

# WorkManager & Firebase Messaging runtime services
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
    public *;
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
    public *;
}
-keep class com.example.notifications.ChoreSyncWorker { *; }
-keep class com.example.notifications.FamiMessagingService { *; }

# DataStore Preferences Protobuf reflection schemas
-keep class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }

# Biometric, Credentials & Security
-keep class androidx.biometric.** { *; }
-keep class androidx.credentials.** { *; }
-keep class com.example.security.LocalCryptoBox { *; }

# Firebase Auth / Play Services / Coroutines
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Retrofit / OkHttp / Moshi
-keepattributes Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
