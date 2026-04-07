# Add project specific ProGuard rules here.
-keep class com.coby.surasura.data.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
# Hilt
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
