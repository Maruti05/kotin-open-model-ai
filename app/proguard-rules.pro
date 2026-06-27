# ============================================================
# Llamatik native library
# ============================================================
-keep class com.llamatik.library.** { *; }
-keep class com.llamatik.library.platform.** { *; }
-keep class com.llamatik.library.platform.GenStream { *; }
-keep class com.llamatik.library.platform.LlamaBridge { *; }
-keepnames class com.llamatik.library.** { *; }

# Keep JNI native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================================
# Room
# ============================================================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# ============================================================
# Hilt / Dagger
# ============================================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclassmembers class * {
    @dagger.hilt.android.internal.lifecycle.HiltViewModelMap <fields>;
}
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ============================================================
# Kotlin Serialization
# ============================================================
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.vedica.labs.ind.app.chat.**$$serializer { *; }
-keepclassmembers class com.vedica.labs.ind.app.chat.** {
    *** Companion;
}
-keepclasseswithmembers class com.vedica.labs.ind.app.chat.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================
# Kotlin Coroutines
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============================================================
# Compose / Navigation
# ============================================================
-keep class androidx.navigation.** { *; }
-keep class * implements androidx.compose.runtime.saveable.Saver { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Stable <fields>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.Immutable <fields>;
}

# ============================================================
# OkHttp
# ============================================================
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ============================================================
# Gson / Reflection-based serializers (if any)
# ============================================================
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# ============================================================
# LiteRT / Google AI Edge
# ============================================================
-keep class com.google.ai.edge.litert.** { *; }
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.ai.edge.litert.support.** { *; }
-dontwarn com.google.ai.edge.litert.**
-dontwarn org.tensorflow.lite.**

# ============================================================
# General Android / Resources
# ============================================================
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }

# Keep R8 from stripping R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}
