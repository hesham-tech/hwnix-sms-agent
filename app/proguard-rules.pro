# Add project specific ProGuard rules here.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Retrofit & OkHttp rules
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okio.**
-dontwarn retrofit2.**

# Gson rules — Fix: java.lang.Class cannot be cast to ParameterizedType
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Room Database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep generic signatures for Retrofit converters
-keepattributes Signature
-keepattributes Exceptions
