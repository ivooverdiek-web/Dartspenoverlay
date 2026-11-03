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
        versionCode = 13
        versionName = "2.2-honor-v5-practice-capture-beta"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
