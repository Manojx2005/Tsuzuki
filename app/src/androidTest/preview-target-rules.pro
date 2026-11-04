# AndroidJUnitRunner shares this dependency with the tested app. R8 otherwise inlines/removes it
# from the app, while the test APK assumes that the original class is supplied by the target.
-keep class androidx.tracing.Trace { *; }

# Instrumentation calls these entry points directly; the daily preview may inline/remove them.
# These rules are included only with -PtsuzukiTestBuildType=preview, never in the daily APK.
-keepclassmembers class eu.kanade.tachiyomi.extension.util.ExtensionLoader {
    public android.content.pm.PackageInfo getArchivePackageInfo(android.content.Context, java.io.File);
    public java.util.List getSignatures(android.content.pm.PackageInfo);
}
-keep class eu.kanade.tachiyomi.extension.util.ExtensionInstaller {
    public <init>(...);
    public kotlinx.coroutines.flow.Flow downloadAndInstall(...);
    public static kotlinx.coroutines.flow.Flow downloadAndInstall$default(...);
}
-keep class eu.kanade.tachiyomi.extension.model.Extension$Available { public <init>(...); }
-keep class mihon.domain.extension.model.ExtensionStore { public <init>(...); }
-keep class mihon.domain.extension.model.ExtensionStore$Contact { public <init>(...); }
