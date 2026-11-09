plugins {
    id("com.android.application")
}
android {
    namespace = "nl.dartspen.overlay"
    compileSdk = 35
    defaultConfig {
        applicationId = "nl.dartspen.overlay.gamesnap.percentremote"
        minSdk = 26
        targetSdk = 35
        versionCode = 23
        versionName = "3.2-per-target-swipe-editor"
    }
    // Two APKs: try the existing app ID as an update without deleting data,
    // or install a safe standalone test app if the old debug signatures differ.
    flavorDimensions += "installation"
    productFlavors {
        create("update") {
            dimension = "installation"
            applicationId = "nl.dartspen.overlay.gamesnap.percentremote"
        }
        create("safecopy") {
            dimension = "installation"
            applicationId = "nl.dartspen.overlay.gamesnap.editorcopy"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
