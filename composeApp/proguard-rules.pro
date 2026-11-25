# Kotlin serialization — keep @Serializable classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.dubois.aura.**$$serializer { *; }
-keepclassmembers class com.dubois.aura.** {
    *** Companion;
}
-keepclasseswithmembers class com.dubois.aura.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Firebase / Firestore
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# KMP / GitLive Firebase
-keep class dev.gitlive.** { *; }
-dontwarn dev.gitlive.**
