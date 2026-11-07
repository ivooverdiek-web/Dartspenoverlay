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
        versionCode = 21
        versionName = "3.0-honor-v5-all-20-doubles-trebles"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
