# ── Crashlytics ───────────────────────────────────────────────────────────────
# Preserve source file names and line numbers for readable Firebase stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep public class * extends java.lang.Exception

# ── kotlinx.serialization ─────────────────────────────────────────────────────
# The Kotlin serialization compiler plugin generates a $$serializer inner object
# and registers a KSerializer companion for every @Serializable class. R8 strips
# these as dead code because they are only reached through the plugin's
# generated SerializerFactory. The rules below tell R8 to keep them.
-keepattributes *Annotation*, InnerClasses

# Keep the generated $$serializer classes for all our own @Serializable types.
-keep,includedescriptorclasses class com.endiar.purrodoro.**$$serializer { *; }

# Keep Companion objects that expose a serializer() method (used by Supabase/Ktor
# to encode/decode DTOs).
-keepclassmembers class com.endiar.purrodoro.** {
    *** Companion;
}
-keepclasseswithmembers class com.endiar.purrodoro.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep the kotlinx.serialization runtime itself — Supabase/Ktor access it via
# reflection for polymorphic and sealed-class serialization.
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# ── Ktor ──────────────────────────────────────────────────────────────────────
# Ktor 3.x and the OkHttp engine use internal service-loader lookups and
# reflective engine initialisation. Keep the whole io.ktor namespace so a first
# R8 enable does not silently break networking; tighten in a follow-up once
# end-to-end tests confirm stability.
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ── OkHttp / Okio ─────────────────────────────────────────────────────────────
# OkHttp ships its own consumer ProGuard rules inside the AAR; these suppress
# warnings for APIs that target Java 9+ modules not present on Android.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okio.**

# ── Supabase ──────────────────────────────────────────────────────────────────
# Supabase-kt 3.x is built on Ktor + kotlinx.serialization. Keep the whole
# namespace for the same reason as Ktor above.
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# ── Room ──────────────────────────────────────────────────────────────────────
# Room's KSP-generated _Impl classes reference the abstract Database/DAO classes
# by name. Keep the abstract declarations so R8 does not rename them out from
# under the generated code.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ── WorkManager ───────────────────────────────────────────────────────────────
# Workers are instantiated via reflection by WorkManager using a
# (Context, WorkerParameters) constructor. The classes are NOT in the manifest,
# so AGP does not auto-generate keeps for them.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── Glance app-widget receivers ───────────────────────────────────────────────
# Receiver class names are declared in AndroidManifest.xml — AGP auto-generates
# keeps via aapt_rules.txt, but explicit rules make intent clear and guard
# against tooling changes.
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# ── Kotlin coroutines ─────────────────────────────────────────────────────────
# Suppress warnings for optional debug-only APIs that are absent on Android.
-dontwarn kotlinx.coroutines.debug.**
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }

# ── Kotlin metadata ───────────────────────────────────────────────────────────
# Retain Kotlin metadata on all surviving classes so the Kotlin reflection
# and serialization APIs (used by Supabase, Ktor) can read parameter names and
# nullability at runtime.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# ── Lottie ────────────────────────────────────────────────────────────────────
# Lottie 6.x ships its own consumer rules inside the AAR — no additions needed.
# Rule included here as documentation only.

# ── Firebase / Google Services ────────────────────────────────────────────────
# Firebase discovers ComponentRegistrar implementations via manifest metadata and
# reflection (class name strings). R8 must not rename or strip them — otherwise
# FirebaseCrashlytics.getInstance() throws "component is not present" at launch.
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keepnames class com.google.firebase.components.ComponentDiscoveryService
-dontwarn com.google.android.gms.**
