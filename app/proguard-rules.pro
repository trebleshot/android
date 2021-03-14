-keep class org.spongycastle.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class org.monora.uprotocol.client.android.model.** { *; }
-keep class org.monora.uprotocol.client.android.database.model.** { *; }

-keep class * extends androidx.fragment.app.Fragment{}