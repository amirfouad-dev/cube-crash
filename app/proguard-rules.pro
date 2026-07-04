# kotlinx.serialization — keep generated serializers for persisted models
-keepclassmembers @kotlinx.serialization.Serializable class com.neongrid.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.neongrid.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
