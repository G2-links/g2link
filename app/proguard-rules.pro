# ════════════════════════════════════════════════════════════
# DisasterMesh ProGuard Rules
# ════════════════════════════════════════════════════════════

# Keep app package
-keep class com.disastermesh.connect.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-keepclasseswithmembernames class * { @dagger.hilt.* <methods>; }

# Google Nearby
-keep class com.google.android.gms.nearby.** { *; }
-dontwarn com.google.android.gms.**

# ZXing QR
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Crypto
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-dontwarn java.security.**

# General Android
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
