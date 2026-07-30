#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-/Users/hanneszietsman/Library/Android/sdk}"

./gradlew :phone:assembleDebug :wear:assembleDebug
adb -s emulator-5554 install -r phone/build/outputs/apk/debug/phone-debug.apk
adb -s emulator-5556 install -r wear/build/outputs/apk/debug/wear-debug.apk

adb -s emulator-5554 shell pm grant com.oxiomsystems.wmaid android.permission.POST_NOTIFICATIONS || true
adb -s emulator-5556 shell pm grant com.oxiomsystems.wmaid android.permission.POST_NOTIFICATIONS || true

adb -s emulator-5554 shell am start -n com.oxiomsystems.wmaid/.phone.MainActivity
adb -s emulator-5556 shell am start -n com.oxiomsystems.wmaid/.wear.WearActivity
