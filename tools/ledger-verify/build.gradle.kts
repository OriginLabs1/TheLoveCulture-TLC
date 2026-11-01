plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("culture.thelove.tlc.tools.LedgerVerifyKt")
}

dependencies {
    implementation(project(":core:ledger"))
    implementation(project(":core:domain"))
    testImplementation(libs.kotlin.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
