plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "eu.kanade.tachiyomi.source"

    defaultConfig {
        consumerProguardFiles("consumer-proguard.pro")
    }
}

dependencies {
    implementation(projects.core.common)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.injekt)
    implementation(libs.rxJava)
    implementation(libs.jsoup)
    // Exposed: the app starts an extension's HttpServer (a NanoHTTPD) for localhost videos (Aniyomi parity).
    api("org.nanohttpd:nanohttpd:2.3.1")

    implementation(libs.androidx.preference)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
}
