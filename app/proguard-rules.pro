# PedroSG94 RootEncoder rules
-keep class com.pedro.encoder.** { *; }
-keep class com.pedro.library.** { *; }
-keep class com.pedro.srt.** { *; }
-keep class com.pedro.common.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
