# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.suvojeet.notenext.data.** {
    *** Companion;
    *** serializer(...);
}
-keep,allowobfuscation class com.suvojeet.notenext.data.**
-keepclassmembers class com.suvojeet.notenext.data.** {
    @kotlinx.serialization.Serializable *;
}
