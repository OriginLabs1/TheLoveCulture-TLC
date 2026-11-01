#!/usr/bin/env bash
# Bump the release identity and print the tag to push. Never hand-edit version.properties.
#   ./scripts/bump-version.sh patch|minor|major
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FILE="$ROOT/version.properties"
[ -f "$FILE" ] || { echo "✗ $FILE missing"; exit 2; }

KIND="${1:-patch}"
CURRENT="$(sed -nE 's/^versionName=//p' "$FILE")"
CODE="$(sed -nE 's/^versionCode=//p' "$FILE")"
IFS=. read -r MAJOR MINOR PATCH <<<"$CURRENT"

case "$KIND" in
  major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0;;
  minor) MINOR=$((MINOR + 1)); PATCH=0;;
  patch) PATCH=$((PATCH + 1));;
  *) echo "✗ usage: $0 patch|minor|major"; exit 2;;
esac

NEXT="$MAJOR.$MINOR.$PATCH"
NEXT_CODE=$((CODE + 1))

sed -i "s/^versionName=.*/versionName=$NEXT/; s/^versionCode=.*/versionCode=$NEXT_CODE/" "$FILE"

echo "✓ $CURRENT (code $CODE) -> $NEXT (code $NEXT_CODE)"
echo "  next: git add version.properties && git commit -m \"release: v$NEXT\" && git tag v$NEXT && git push origin main v$NEXT"
