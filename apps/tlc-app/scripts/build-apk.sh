#!/bin/sh
# Build and sign the Union Android APK without Gradle.
#
# Requirements:
#   * JDK 17+ (javac, keytool)
#   * Android build-tools (aapt2, zipalign, apksigner)
#   * android/toolchain/android-34.jar and android/toolchain/r8.jar
#   * a keystore from android/scripts/gen-keys.sh, unless UNION_UNSIGNED=1
#
# The APK embeds the built web surface in assets/web and serves it from a
# loopback-only asset server, so IndexedDB, WebCrypto and the service worker
# behave exactly as they do on the web. INTERNET is the only permission.
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

VERSION_NAME=${UNION_VERSION_NAME:-$(node -p "require('./package.json').version")}
VERSION_CODE=${UNION_VERSION_CODE:-1}
ANDROID_JAR=${ANDROID_JAR:-$ROOT/android/toolchain/android-34.jar}
R8_JAR=${R8_JAR:-$ROOT/android/toolchain/r8.jar}
KEYS=${UNION_KEYS:-$ROOT/android/keys}
STORE=${UNION_KEYSTORE:-$KEYS/union.p12}
PROPS="$KEYS/keystore.properties"
ASSETS="$ROOT/android/app/src/main/assets"
WEB="$ASSETS/web"
WORK="$ROOT/build/apk"
OUT_DIR="$ROOT/release"
OUT="$OUT_DIR/Union-$VERSION_NAME.apk"

# A machine without the platform jars can borrow the Coordinator toolchain.
if [ ! -f "$ANDROID_JAR" ] && [ -f /root/projects/Coordinator/mobile/toolchain/android-34.jar ]; then
  ANDROID_JAR=/root/projects/Coordinator/mobile/toolchain/android-34.jar
fi
if [ ! -f "$R8_JAR" ] && [ -f /root/projects/Coordinator/mobile/toolchain/r8.jar ]; then
  R8_JAR=/root/projects/Coordinator/mobile/toolchain/r8.jar
fi

need() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing $1. Install the Android build tools and a JDK." >&2
    exit 1
  }
}

need javac
need keytool
need aapt2
need zipalign
need apksigner
need zip

[ -f "$ANDROID_JAR" ] || {
  echo "Missing $ANDROID_JAR (platform android-34)." >&2
  exit 1
}
[ -f "$R8_JAR" ] || {
  echo "Missing $R8_JAR (D8/R8)." >&2
  exit 1
}
if [ "${UNION_UNSIGNED:-0}" != "1" ] && [ ! -f "$STORE" ]; then
  echo "Missing keystore $STORE. Run android/scripts/gen-keys.sh first," >&2
  echo "or set UNION_UNSIGNED=1 to produce an unsigned APK." >&2
  exit 1
fi

if [ "${UNION_UNSIGNED:-0}" != "1" ] && [ -f "$PROPS" ]; then
  # shellcheck disable=SC1090
  . "$PROPS"
fi
ALIAS=${UNION_KEY_ALIAS:-union}
STORE_PASS=${UNION_STORE_PASSWORD:-}
KEY_PASS=${UNION_KEY_PASSWORD:-$STORE_PASS}
if [ "${UNION_UNSIGNED:-0}" != "1" ] && [ -z "$STORE_PASS" ]; then
  echo "No UNION_STORE_PASSWORD. Set it or keep android/keys/keystore.properties." >&2
  exit 1
fi

if [ "${UNION_SKIP_WEB:-0}" != "1" ]; then
  echo "==> web surface"
  npm run build
fi

echo "==> embed web surface"
[ -f "$ROOT/dist/index.html" ] || {
  echo "dist/index.html is missing. Build the web surface first." >&2
  exit 1
}
rm -rf "$WEB"
mkdir -p "$WEB"
cp -R "$ROOT/dist/." "$WEB/"
# The app never needs the source map or the build manifest inside the APK.
rm -f "$WEB/SHA256SUMS"

echo "==> resources"
rm -rf "$WORK"
mkdir -p "$WORK/res" "$WORK/gen" "$WORK/classes" "$WORK/dex" "$OUT_DIR"
aapt2 compile \
  --dir android/app/src/main/res \
  -o "$WORK/res.zip"

aapt2 link \
  -o "$WORK/app-unsigned.apk" \
  -I "$ANDROID_JAR" \
  --manifest android/app/src/main/AndroidManifest.xml \
  --java "$WORK/gen" \
  -A "$ASSETS" \
  --auto-add-overlay \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
  "$WORK/res.zip"

echo "==> java"
find android/app/src/main/java -name '*.java' > "$WORK/sources.txt"
find "$WORK/gen" -name '*.java' >> "$WORK/sources.txt"
javac \
  -source 8 \
  -target 8 \
  -Xlint:-options \
  -classpath "$ANDROID_JAR" \
  -d "$WORK/classes" \
  @"$WORK/sources.txt"

echo "==> dex"
find "$WORK/classes" -name '*.class' > "$WORK/classes.txt"
java -cp "$R8_JAR" com.android.tools.r8.D8 \
  --release \
  --min-api 28 \
  --lib "$ANDROID_JAR" \
  --output "$WORK/dex" \
  @"$WORK/classes.txt"

echo "==> package"
( cd "$WORK/dex" && zip -q -X "$WORK/app-unsigned.apk" classes.dex )
zipalign -f -p 4 "$WORK/app-unsigned.apk" "$WORK/app-aligned.apk"

if [ "${UNION_UNSIGNED:-0}" = "1" ]; then
  echo "==> unsigned (caller signs)"
  cp "$WORK/app-aligned.apk" "$OUT"
else
  echo "==> sign"
  apksigner sign \
    --ks "$STORE" \
    --ks-type PKCS12 \
    --ks-key-alias "$ALIAS" \
    --ks-pass "pass:$STORE_PASS" \
    --key-pass "pass:$KEY_PASS" \
    --min-sdk-version 28 \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    --v3-signing-enabled true \
    --out "$OUT" \
    "$WORK/app-aligned.apk"

  apksigner verify --verbose --print-certs "$OUT" > "$WORK/verify.txt"
  grep -E 'Verified using|Number of signers|Signer #1 certificate SHA-256' "$WORK/verify.txt" | sed 's/^ */  /'
fi

echo "==> inspect"
BADGING=$(aapt2 dump badging "$OUT")
printf '%s\n' "$BADGING" | grep -E 'package:|application-label|sdkVersion|targetSdkVersion|uses-permission' || true

PERMISSIONS=$(printf '%s\n' "$BADGING" | grep -o "uses-permission: name='[^']*'" | sed "s/.*name='//;s/'//" | sort -u || true)
UNEXPECTED=$(printf '%s\n' "$PERMISSIONS" | grep -v '^android.permission.INTERNET$' || true)
if [ -n "$UNEXPECTED" ]; then
  echo "Refusing to ship: unexpected permission(s):" >&2
  printf '  %s\n' "$UNEXPECTED" >&2
  exit 1
fi

if ! printf '%s\n' "$BADGING" | grep -q "sdkVersion:'28'"; then
  echo "Refusing to ship: minSdkVersion is not 28." >&2
  printf '%s\n' "$BADGING" | grep -i sdk >&2
  exit 1
fi

SIZE=$(wc -c < "$OUT")
if [ "${UNION_UNSIGNED:-0}" = "1" ]; then
  STATE="unsigned"
else
  STATE="signed"
fi

( cd "$OUT_DIR" && sha256sum "$(basename "$OUT")" > SHA256SUMS )

echo
echo "APK: $OUT (${SIZE} bytes)"
echo "    version $VERSION_NAME ($VERSION_CODE), permission: INTERNET, $STATE."
echo "    sha256 $(cut -d' ' -f1 "$OUT_DIR/SHA256SUMS")"
echo "    checksums $OUT_DIR/SHA256SUMS"
