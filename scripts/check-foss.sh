#!/usr/bin/env bash
# Prove the `foss` channel is shippable to F-Droid, Orion and GitHub Releases.
#
# Two independent checks, because each catches a different mistake:
#   1. Gradle guard  — the resolved release runtime classpath contains no Play Services,
#                      Firebase, Crashlytics or tracking SDK (SK-02).
#   2. Static scan   — no source or resource file references those SDKs either, so a
#                      commented-out dependency cannot sneak in through a later review.
#
# Usage: ANDROID_HOME=... ./scripts/check-foss.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

: "${ANDROID_HOME:?ANDROID_HOME must point at an Android SDK}"

FORBIDDEN='com\.google\.android\.gms|com\.google\.firebase|com\.google\.android\.play|com\.crashlytics|com\.facebook\.android|com\.appsflyer|com\.adjust\.sdk|io\.branch|io\.sentry'

echo "— foss channel —"

# 1. Resolved dependency graph (authoritative: this is what actually gets packaged).
./gradlew :apps:android:verifyFossOnly --console=plain -q

# 2. Source/resource scan. Excludes build output; the Gradle guard covers the build script.
if grep -rInE "$FORBIDDEN" \
    --exclude-dir=build \
    --exclude-dir=.gradle \
    --exclude-dir=.git \
    apps/android/src core 2>/dev/null; then
    echo "✗ source references a proprietary/tracking SDK"
    exit 1
fi

# 3. Every store channel builds from the same tag: the version must be set.
VERSION="$(sed -nE 's/^versionName=//p' version.properties)"
[ -n "$VERSION" ] || { echo "✗ version.properties has no versionName"; exit 1; }

echo "✓ foss channel clean · version $VERSION · no Play Services, Firebase or tracking"
