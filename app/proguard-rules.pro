# TUMBA AI — release rules.
# Keep provider DTOs and Room entities intact for serialization/reflection-free code.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# Kotlinx Serialization
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes RuntimeVisibleAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Hilt
-keep class dagger.hilt.** { *; }
