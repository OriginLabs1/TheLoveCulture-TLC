#!/bin/sh
# Generate the Union release signing key (PKCS#12).
#
# The key stays on this machine under android/keys/, which is gitignored. Back
# it up somewhere safe: Android refuses to update an installed app with a
# different key, so losing it ends the update path for that install base.
#
# For CI, export the keystore as base64 and set:
#   UNION_KEYSTORE_BASE64, UNION_STORE_PASSWORD, UNION_KEY_PASSWORD
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
KEYS="$ROOT/android/keys"
STORE="$KEYS/union.p12"
PROPS="$KEYS/keystore.properties"
ALIAS=${UNION_KEY_ALIAS:-union}

mkdir -p "$KEYS"
chmod 700 "$KEYS"

if [ -f "$STORE" ]; then
  echo "Keystore already exists: $STORE"
  echo "Keep it backed up. Re-run with a new directory if you really want a new key."
  exit 0
fi

PASS=${UNION_STORE_PASSWORD:-}
if [ -z "$PASS" ]; then
  PASS=$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')
fi

keytool -genkeypair \
  -keystore "$STORE" \
  -storetype PKCS12 \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=Union, O=OriginLabs, OU=Mobile, C=NG" \
  -storepass "$PASS" \
  -keypass "$PASS" \
  1>/dev/null

umask 077
{
  printf 'UNION_KEY_ALIAS=%s\n' "$ALIAS"
  printf 'UNION_STORE_PASSWORD=%s\n' "$PASS"
  printf 'UNION_KEY_PASSWORD=%s\n' "$PASS"
} > "$PROPS"

keytool -list -v \
  -keystore "$STORE" \
  -storepass "$PASS" \
  -alias "$ALIAS" 2>/dev/null > "$KEYS/signing-certificate.txt"

echo "Generated $STORE"
echo "Fingerprint:"
keytool -list -v -keystore "$STORE" -storepass "$PASS" -alias "$ALIAS" 2>/dev/null |
  grep -E 'SHA1:|SHA256:|Valid from' | sed 's/^ *//'
echo
echo "Back up android/keys/ offline. It is gitignored on purpose."
