// TLC v0.0.1 — build root. See docs in CONTEXT.md §6 and SPECS.md §B-1.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
}

// The release gate (SPECS §C-8). `./gradlew verify` must be green before any change merges.
tasks.register("verify") {
    group = "verification"
    description = "Compile + unit tests for the sovereign core (no Android SDK required)."
    dependsOn(
        ":core:domain:test",
        ":core:ledger:test",
        ":tools:ledger-verify:test",
    )
}
