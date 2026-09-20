# WorkLog R8 rules.
#
# The app is deliberately reflection-free apart from the libraries below, so
# this file stays short. Anything added here should come with a note saying
# what broke without it.

# Room generates implementations that are looked up by name at runtime.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# WorkManager instantiates Workers reflectively from their class name.
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# Glance instantiates the receiver, the widget and each ActionCallback by name
# from the manifest and from serialized widget state.
-keep class com.ajuworks.worklog.widget.** { *; }

# The domain model is plain data with no reflection, but keeping java.time
# shape-related warnings quiet avoids noise from the Ads SDK's transitive deps.
-dontwarn java.lang.invoke.StringConcatFactory

# The Google Mobile Ads SDK and UMP ship their own consumer rules; these only
# silence warnings about optional integrations WorkLog does not use.
-dontwarn com.google.android.gms.ads.**
