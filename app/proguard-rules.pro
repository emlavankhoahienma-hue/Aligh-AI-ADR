# Proguard rules for AlignAI Camera
-keep class com.google.mlkit.** { *; }
-keep class org.tensorflow.lite.** { *; }
-keep class androidx.camera.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn org.tensorflow.lite.**
