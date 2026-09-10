#!/usr/bin/env bash
set -euo pipefail

manifest="app/src/main/AndroidManifest.xml"
if grep -Eq 'android.permission.INTERNET|ACCESS_FINE_LOCATION|ACCESS_COARSE_LOCATION|READ_MEDIA|WRITE_EXTERNAL_STORAGE' "$manifest"; then
  echo "Unexpected data-sensitive permission declared in $manifest" >&2
  exit 1
fi
grep -q 'android.permission.CAMERA' "$manifest"
echo "Privacy source-manifest check passed. Inspect the packaged APK separately to confirm merger removals."
