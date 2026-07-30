#!/usr/bin/env bash
set -euo pipefail

mkdir -p evidence
adb -s emulator-5554 exec-out screencap -p > evidence/phone.png
adb -s emulator-5556 exec-out screencap -p > evidence/watch.png

echo "Captured evidence/phone.png and evidence/watch.png."
