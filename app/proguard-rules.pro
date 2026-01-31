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
-keepattributes Exceptions

# Keep ALL app packages - no obfuscation at all
-keep,allowoptimization class com.kevann.nosteq.** { *; }
-keep,allowoptimization class com.example.nosteq.** { *; }
-keep,allowoptimization class com.nosteq.provider.** { *; }
-keep,allowoptimization class com.kevannTechnologies.nosteqCustomers.** { *; }

-keepclassmembers class com.kevann.nosteq.** { *; }
-keepclassmembers class com.example.nosteq.** { *; }
-keepclassmembers class com.nosteq.provider.** { *; }
-keepclassmembers class com.kevannTechnologies.nosteqCustomers.** { *; }

# Keep class names - no renaming
-keepnames class com.kevann.nosteq.**
-keepnames class com.example.nosteq.**
-keepnames class com.nosteq.provider.**
-keepnames class com.kevannTechnologies.nosteqCustomers.**

# Keep all fields with SerializedName annotation
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all interfaces completely intact
-keep interface com.kevann.nosteq.** { *; }
-keep interface com.example.nosteq.** { *; }
-keep interface com.nosteq.provider.** { *; }
-keep interface com.kevannTechnologies.nosteqCustomers.** { *; }

# ========== RETROFIT RULES ==========
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Retrofit service interfaces with full signatures
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Retrofit Call and Response - DO NOT obfuscate
-keep class retrofit2.Call { *; }
-keep class retrofit2.Response { *; }
-keep class retrofit2.Callback { *; }

# Keep generic signatures for Retrofit
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Kotlin Coroutines with Retrofit
-keep class kotlin.coroutines.Continuation { *; }
-keep class kotlin.coroutines.** { *; }
-keepclassmembers class kotlin.coroutines.** { *; }

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

# ========== SPECIFIC MODEL CLASSES ==========
# SmartOLT API Models - explicitly keep all fields
-keep class com.nosteq.provider.network.models.** { *; }
-keepclassmembers class com.nosteq.provider.network.models.** { *; }
-keepnames class com.nosteq.provider.network.models.**

# Keep SmartOLT service and config
-keep class com.nosteq.provider.network.SmartOltApiService { *; }
-keep class com.nosteq.provider.network.SmartOltClient { *; }
-keep class com.nosteq.provider.network.SmartOltConfig { *; }
-keep interface com.nosteq.provider.network.SmartOltApiService { *; }

# Keep OnuDetails, OnuStatus, etc with all fields
-keepclassmembers class com.nosteq.provider.network.models.OnuDetails {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.nosteq.provider.network.models.OnuDetailsItem {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.nosteq.provider.network.models.OnuStatus {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.nosteq.provider.network.models.AllOnusDetailsResponse {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.nosteq.provider.network.models.OnuDetailsResponse {
    <fields>;
    <init>(...);
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** e(...);
    public static *** w(...);
}
