# MediaPipe GenAI task JNI classes are accessed via reflection.
-keep class com.google.mediapipe.tasks.genai.** { *; }
-dontwarn com.google.mediapipe.**

# Guava (transitive dependency of MediaPipe)
-dontwarn com.google.errorprone.**
