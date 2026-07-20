# Add project-specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\dhana\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Room-specific rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>(...);
}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# AdMob / Play Services
-keep class com.google.android.gms.ads.** { *; }
-keep class com.android.billingclient.api.** { *; }
