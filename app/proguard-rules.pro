# Add project specific ProGuard rules here.

# Keep all attributes needed for Retrofit and Gson
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keep class com.kevann.nosteq.** { *; }
-keep class com.example.nosteq.** { *; }
-keep class com.nosteq.provider.** { *; }

-keepclassmembers class com.kevann.nosteq.** { *; }
-keepclassmembers class com.example.nosteq.** { *; }
-keepclassmembers class com.nosteq.provider.** { *; }

# Keep all fields with SerializedName annotation
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all interfaces completely intact
-keep interface com.kevann.nosteq.** { *; }
-keep interface com.example.nosteq.** { *; }
-keep interface com.nosteq.provider.** { *; }

-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Prevent obfuscation of Retrofit service methods
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepclassmembers class okhttp3.** { *; }

# OkHttp platform used only on JVM and when Conscrypt dependency is available
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep Okio (required by OkHttp)
-keep class okio.** { *; }
-keepclassmembers class okio.** { *; }
-dontwarn okio.**

# Keep Gson and prevent field name obfuscation
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-dontwarn sun.misc.Unsafe

-keep class * implements com.google.gson.JsonDeserializer { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }

# Keep data classes and their constructors
-keepclassmembers class * {
    public <init>(...);
}

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-dontwarn com.google.firebase.crashlytics.**

# Keep custom keys and logs for Crashlytics
-keepclassmembers class com.google.firebase.crashlytics.FirebaseCrashlytics {
    public *;
}

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep companion objects
-keepclassmembers class * {
    *** Companion;
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** e(...);
    public static *** w(...);
}
