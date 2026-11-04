# AndroidJUnitRunner shares this dependency with the tested app. R8 otherwise inlines/removes it
# from the app, while the test APK assumes that the original class is supplied by the target.
-keep class androidx.tracing.Trace { *; }
