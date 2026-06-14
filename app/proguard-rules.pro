# ProGuard rules for Poem300
-keep class com.poem300.data.model.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**
-keep class com.android.billingclient.api.** { *; }
