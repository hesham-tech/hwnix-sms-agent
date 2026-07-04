# Add project specific ProGuard rules here.

# --- Kotlin & Coroutines ---
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, Exceptions
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }

# --- Retrofit ---
-keep class retrofit2.** { *; }
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okio.**
-dontwarn okhttp3.**

# --- OkHttp ---
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# --- Gson: Fix java.lang.Class cannot be cast to java.lang.reflect.ParameterizedType ---
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Fix generic type erasure with Retrofit + Gson converter
-keepattributes Signature
-keep class com.google.gson.JsonObject { *; }
-keep class com.google.gson.JsonArray { *; }
-keep class com.google.gson.JsonElement { *; }

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- App Models ---
-keep class com.hwnix.smsagent.** { *; }
