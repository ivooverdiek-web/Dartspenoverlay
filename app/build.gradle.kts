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
        versionCode = 28
        versionName = "3.7-quick-save-no-snap"
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
        // Old debug-signing keys cannot always be reproduced in CI. A new
        // distinct ID preserves *both* of the user's previous installations.
        create("velocitycopy") {
            dimension = "installation"
            applicationId = "nl.dartspen.overlay.gamesnap.velocitycopy"
        }
        // Verified: this CI key differs from the installed 3.4 APK.
        // A dedicated new package prevents destructive uninstall/data loss.
        create("verifycopy") {
            dimension = "installation"
            applicationId = "nl.dartspen.overlay.gamesnap.verifycopy"
        }
        // Independent installation protects all previously saved throws if
        // the experimental debug-signing identity changed between builds.
        create("quickcopy") {
            dimension = "installation"
            applicationId = "nl.dartspen.overlay.gamesnap.quickcopy"
        }
        // Fallback if the former quickcopy's debug signing key cannot be
        // recovered: a parallel install never deletes saved recordings.
        create("compactcopy") {
            dimension = "installation"
            applicationId = "nl.dartspen.overlay.gamesnap.compactcopy"
        }
    }
    // Explicit CI signing: AGP previously used an ephemeral debug key while
    // the workflow cached an empty ~/.android/debug.keystore path.
    if (System.getenv("CI") == "true") {
        signingConfigs.getByName("debug") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
