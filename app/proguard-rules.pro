# ========== BASIC OPTIMIZATION RULES ==========
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-printmapping mapping.txt

# ========== DEBUGGING SUPPORT (optional) ==========
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# ========== KEEP APPLICATION & MAIN CLASSES ==========
-keep class com.gestureai.gameautomation.** { *; }
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.accessibilityservice.AccessibilityService { *; }

# ========== KEEP LIBRARIES ==========
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

-keep class opennlp.tools.** { *; }
-dontwarn opennlp.tools.**
-keepclassmembers class opennlp.tools.** { *; }

-keep class opennlp.model.** { *; }
-dontwarn opennlp.model.**

-keep class timber.log.** { *; }
-dontwarn timber.log.**

-keep class com.google.code.gson.** { *; }
-dontwarn com.google.code.gson.**

# ========== KEEP ROOM / DATABASE STUFF ==========
-keep class com.gestureai.gameautomation.database.** { *; }
-keep class com.gestureai.gameautomation.models.** { *; }

-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# ========== NATIVE METHODS ==========
-keepclasseswithmembernames class * {
    native <methods>;
}

# ========== PARCELABLES ==========
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ========== REFLECTION PROTECTION ==========
-keepnames class * {
    public <methods>;
}

# ========== FRAGMENTS / VIEWMODELS ==========
-keep public class * extends androidx.fragment.app.Fragment
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ========== ANDROIDX SUPPORT ==========
-keep class androidx.** { *; }
-dontwarn androidx.**

# ========== GESTURE AI PACKAGE (SAFEGUARD) ==========
-keep class com.gestureai.** { *; }
