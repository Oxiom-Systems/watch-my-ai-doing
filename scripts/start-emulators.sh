#!/usr/bin/env bash
set -euo pipefail

SDK_ROOT="${ANDROID_EMULATOR_ROOT:-/opt/homebrew/share/android-commandlinetools}"
EMULATOR="${SDK_ROOT}/emulator/emulator"

nohup "${EMULATOR}" -avd Pixel_6_API_34 -port 5554 -no-snapshot-load \
  >"${TMPDIR:-/tmp}/wmaid-phone-emulator.log" 2>&1 &
nohup "${EMULATOR}" -avd WMAID_Wear_OS_6 -port 5556 -no-snapshot-load \
  >"${TMPDIR:-/tmp}/wmaid-wear-emulator.log" 2>&1 &

echo "Started phone on emulator-5554 and watch on emulator-5556."
