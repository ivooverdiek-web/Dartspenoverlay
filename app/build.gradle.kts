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
        versionCode = 19
        versionName = "2.8-honor-v5-finger-thick-pro-lines"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
