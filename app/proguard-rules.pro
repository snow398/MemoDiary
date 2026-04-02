# ProGuard rules for MemoDiary application

# Keep the application classes
-keep class com.memodiary.** { *; }

# Keep Room annotations
-keep class androidx.room.** { *; }
-keep class com.memodiary.data.local.entity.MemoEntity { *; }

# Keep ViewModel classes
-keep class com.memodiary.ui.**ViewModel { *; }

# Keep Jetpack Compose classes
-keep class androidx.compose.** { *; }

# Keep data classes
-keep class com.memodiary.domain.model.Memo { *; }

# Keep any other necessary classes or methods that should not be obfuscated
-keepclassmembers class * {
    @androidx.room.* <fields>;
}
-keepclassmembers class * {
    @androidx.lifecycle.* <fields>;
}