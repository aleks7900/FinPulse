# Fin Pulse - ProGuard & R8 Optimization Rules
# Optimized for Google Play Release Builds

# --- General Annotations & Attributes ---
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# --- Kotlin Coroutines & Flow ---
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- Kotlinx Serialization ---
-keepattributes *Annotation*,Signature
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    <fields>;
    <methods>;
}
-keep,allowobfuscation,allowshrinking class * {
    <init>(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepnames class kotlinx.serialization.PolymorphicSerializer
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-dontwarn kotlinx.serialization.**

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.Room
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# --- FinPulse Domain Models & Database Entities ---
-keep class md.alexlab.finpulse.domain.model.** { *; }
-keep class md.alexlab.finpulse.core.database.entity.** { *; }
-keep class md.alexlab.finpulse.domain.model.sync.** { *; }
-keep class md.alexlab.finpulse.domain.model.backup.** { *; }

# --- Firebase Auth & Cloud Firestore ---
-keepattributes *Annotation*
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# --- AndroidX Credential Manager & Google ID ---
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# --- WorkManager ---
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- AndroidX Biometric & Security Crypto ---
-keep class androidx.biometric.** { *; }
-keep class androidx.security.crypto.** { *; }

# --- Jetpack Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Suppress harmless warnings from third-party libraries
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn java.lang.ClassValue
