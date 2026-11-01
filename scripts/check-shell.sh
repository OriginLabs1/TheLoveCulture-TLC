#!/usr/bin/env bash
# Type-check the Android shell without AAPT2.
#
# Why this exists: AAPT2 ships as an x86-64 binary, so `assembleDebug` cannot run on an arm64
# Linux host (or in this agent sandbox). This script compiles every Kotlin source of the shell
# against the *resolved* Compose/Android classpath plus a generated R stub, so a typo in a
# composable still fails locally instead of only in CI. Packaging/signing stays in CI (x86_64).
#
# Usage: ANDROID_HOME=... ./scripts/check-shell.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

: "${ANDROID_HOME:?ANDROID_HOME must point at an Android SDK (platforms;android-35)}"
WORK="${TMPDIR:-/tmp}/tlc-shellcheck"
rm -rf "$WORK"; mkdir -p "$WORK/aar" "$WORK/out" "$WORK/src"

# 1. Resolved compile classpath (the android module joins the build only when ANDROID_HOME exists).
./gradlew :apps:android:printDebugClasspath -q --console=plain | grep -E '^/' > "$WORK/cp.txt"

# 2. Extract classes.jar from every AAR; keep plain jars as they are.
i=0
CP=""
while read -r p; do
  case "$p" in
    *.aar) mkdir -p "$WORK/aar/$i"; unzip -q -o "$p" classes.jar -d "$WORK/aar/$i" && CP="$CP:$WORK/aar/$i/classes.jar"; i=$((i + 1));;
    *.jar) CP="$CP:$p";;
  esac
done < "$WORK/cp.txt"

# 3. Generate an R stub from the real string resources, so resource drift is caught too.
{
  echo 'package culture.thelove.tlc.android'
  echo 'object R { object string {'
  n=0
  grep -oE 'name="[a-z0-9_]+"' apps/android/src/main/res/values/strings.xml |
    sed -E 's/name="(.*)"/\1/' |
    while read -r name; do n=$((n + 1)); echo "  const val $name = $n"; done
  echo '} }'
} > "$WORK/src/RStub.kt"

# 4. Toolchain jars. The Compose compiler plugin is required for correct codegen.
fetch() {
  local artifact="$1" version="$2" dest="$WORK/$(basename "$1").jar"
  [ -f "$dest" ] || curl -sSL -o "$dest" \
    "https://repo1.maven.org/maven2/org/jetbrains/kotlin/$artifact/$version/$artifact-$version.jar"
  echo "$dest"
}
KOTLIN_VERSION="$(grep -E '^kotlin *= *"' gradle/libs.versions.toml | sed -E 's/.*"(.*)".*/\1/')"
COMPOSE_PLUGIN="$(fetch kotlin-compose-compiler-plugin-embeddable "$KOTLIN_VERSION")"
REFLECT="$(fetch kotlin-reflect "$KOTLIN_VERSION")"

CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
STDLIB="$(find "$CACHE/org.jetbrains.kotlin/kotlin-stdlib" -name "kotlin-stdlib-$KOTLIN_VERSION.jar" | head -1)"
COMPILER="$(find "$CACHE/org.jetbrains.kotlin/kotlin-compiler-embeddable" -name "kotlin-compiler-embeddable-$KOTLIN_VERSION.jar" | head -1)"
TROVE="$(find "$CACHE" -name 'trove4j*.jar' | head -1)"
COROUTINES="$(find "$CACHE" -name 'kotlinx-coroutines-core-jvm-*.jar' | head -1)"
ANNOTATIONS="$(find "$CACHE/org.jetbrains/annotations" -name 'annotations-*.jar' | sort -V | tail -1)"
ANDROID_JAR="$ANDROID_HOME/platforms/android-35/android.jar"

[ -f "$ANDROID_JAR" ] || { echo "✗ missing $ANDROID_JAR (sdkmanager 'platforms;android-35')"; exit 2; }

java -cp "$COMPILER:$STDLIB:$REFLECT:$TROVE:$COROUTINES:$ANNOTATIONS" \
  org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -no-reflect -nowarn -jvm-target 17 \
  -Xplugin="$COMPOSE_PLUGIN" \
  -classpath "$ANDROID_JAR$CP:$STDLIB" \
  -d "$WORK/out" \
  $(find apps/android/src/main/kotlin -name '*.kt') "$WORK/src/RStub.kt"

COUNT="$(find "$WORK/out" -name '*.class' | wc -l | tr -d ' ')"
echo "✓ shell type-checks · $COUNT classes · resources: $(grep -c 'const val' "$WORK/src/RStub.kt")"
