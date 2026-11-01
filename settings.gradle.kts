rootProject.name = "TLC App"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":core:domain")
include(":core:ledger")
include(":tools:ledger-verify")

// The Android shell needs the Android SDK. It joins the build only where one exists
// (local dev machines and the CI runner), so `./gradlew verify` stays runnable anywhere.
if (System.getenv("ANDROID_HOME") != null || file("local.properties").exists()) {
    include(":apps:android")
}
