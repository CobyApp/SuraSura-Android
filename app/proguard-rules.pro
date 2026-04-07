# Add project specific ProGuard rules here.
-keep class com.coby.surasura.data.** { *; }
# gRPC + Google Cloud APIs (Speech v1; Translation + TTS use REST + API key)
-dontwarn io.grpc.**
-dontwarn com.google.protobuf.**
-keep class com.google.cloud.speech.v1.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageV3 { *; }
# Hilt
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
