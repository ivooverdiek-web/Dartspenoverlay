plugins {
    id("com.android.application")
}
android {
    namespace = "nl.dartspen.overlay"
    compileSdk = 35
    defaultConfig {
        applicationId = "nl.dartspen.overlay"
        minSdk = 26
        targetSdk = 35
        versionCode = 20
        versionName = "2.9-honor-v5-video-swipe-vs-dart"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
