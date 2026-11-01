import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Version identity comes from one file so Gradle and CI can never disagree (CONTEXT §3).
val tlcVersion = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val tlcVersionName = requireNotNull(tlcVersion.getProperty("versionName")) { "versionName missing" }
val tlcVersionCode = requireNotNull(tlcVersion.getProperty("versionCode")?.toIntOrNull()) {
    "versionCode missing or not an integer"
}

android {
    namespace = "culture.thelove.tlc.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // Frozen forever: the package id IS the identity. Changing it breaks every continuity link.
        applicationId = "culture.thelove.tlc"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = tlcVersionCode
        versionName = tlcVersionName
        resourceConfigurations += listOf("en", "en-rNG")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Two channels, one package id, one signature (SPECS §C-8).
    //   foss -> no Play Services, no Firebase, no analytics, no proprietary SDK: F-Droid / Orion / GitHub
    //   full -> optional first-party integrations, never required for a core flow
    flavorDimensions += "distribution"
    productFlavors {
        create("foss") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION", "\"foss\"")
            buildConfigField("boolean", "FEATURE_PUSH", "false")
            buildConfigField("boolean", "FEATURE_TELEMETRY", "false")
        }
        create("full") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION", "\"full\"")
            buildConfigField("boolean", "FEATURE_PUSH", "false")
            buildConfigField("boolean", "FEATURE_TELEMETRY", "false")
        }
    }

    // Signed release is produced only when the upload key is present (CI). Otherwise the build
    // stays unsigned and the pipeline fails its guard instead of shipping something unverifiable.
    val keystorePath = System.getenv("TLC_KEYSTORE_PATH")
    signingConfigs {
        if (!keystorePath.isNullOrBlank()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("TLC_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("TLC_KEY_ALIAS")
                keyPassword = System.getenv("TLC_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // One core, many shells: the shell owns pixels, never rules (CONTEXT §6).
    implementation(project(":core:domain"))
    implementation(project(":core:ledger"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
}

// ---------------------------------------------------------------------------------------------
// FOSS guard: the foss channel is a legal commitment to F-Droid users, so it is checked by the
// build, not by a wiki page. Adding a proprietary dependency to `foss` fails here (SK-02).
// ---------------------------------------------------------------------------------------------
val forbiddenFossGroups = listOf(
    "com.google.android.gms",
    "com.google.firebase",
    "com.google.android.play",
    "com.google.android.gms.ads",
    "com.crashlytics",
    "com.facebook.android",
    "com.appsflyer",
    "com.adjust.sdk",
    "io.branch",
)

tasks.register("verifyFossOnly") {
    group = "verification"
    description = "Fail if the foss channel resolves any proprietary / tracking dependency."
    doLast {
        val config = configurations.getByName("fossReleaseRuntimeClasspath")
        val offending = config.incoming.resolutionResult.allComponents
            .mapNotNull { it.moduleVersion?.let { mv -> "${mv.group}:${mv.name}" } }
            .filter { id -> forbiddenFossGroups.any { id.startsWith(it) } }
            .distinct()
            .sorted()
        if (offending.isNotEmpty()) {
            throw GradleException(
                "FOSS violation — the foss channel must stay free of Play Services/Firebase/tracking:\n" +
                    offending.joinToString("\n") { "  - $it" },
            )
        }
        println("✓ foss channel is clean (no proprietary Google/tracking dependencies)")
    }
}

// Diagnostic: prints the exact resolved compile classpath. Used by scripts/check-shell.sh to
// type-check the shell on machines where AAPT2 cannot run (e.g. Linux arm64).
tasks.register("printDebugClasspath") {
    doLast {
        configurations.getByName("fossDebugCompileClasspath").files
            .sortedBy { it.absolutePath }
            .forEach { println(it.absolutePath) }
    }
}
