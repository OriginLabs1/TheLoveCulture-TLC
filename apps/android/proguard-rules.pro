# TLC v0.0.1 — keep the domain readable in stack traces, strip everything else.
-keepnames class culture.thelove.tlc.domain.** { *; }
-keepnames class culture.thelove.tlc.ledger.** { *; }
-dontwarn org.jetbrains.annotations.**
