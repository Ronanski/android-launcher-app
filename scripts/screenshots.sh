#!/usr/bin/env bash
# Runs inside an Android emulator (GitHub Actions) and captures screenshots
# of Pad Launcher into docs/screenshots/.
set -euo pipefail

PKG=com.ronanski.launcher
APK=app/build/outputs/apk/debug/app-debug.apk
OUT=docs/screenshots
mkdir -p "$OUT"

adb wait-for-device
adb install -r "$APK"

# Screen size, e.g. "Physical size: 2560x1800"
SIZE=$(adb shell wm size | tr -d '\r' | awk '/Physical size/ {print $3}')
W=${SIZE%x*}
H=${SIZE#*x}
CX=$((W / 2))
echo "Screen: ${W}x${H}"

# Pick some launchable apps to pin on the home screen. Keys use the same
# format as the app: "package/full.ClassName#userSerial".
KEYS=$(adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
  | tr -d '\r' | sed 's/^ *//' | grep -E '^[a-zA-Z0-9_.]+/[a-zA-Z0-9_.$]+$' \
  | grep -v "^$PKG/" \
  | awk -F/ '{cls=$2; if (substr(cls,1,1)==".") cls=$1 cls; print $1 "/" cls "#0"}' \
  | head -n 10)
echo "Pinned apps:"
echo "$KEYS"

write_prefs() {
  local extra="$1"
  adb shell am force-stop "$PKG"
  {
    echo "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>"
    echo "<map>"
    printf '    <string name="pinned">%s</string>\n' "$KEYS"
    echo '    <string name="gesture_down">LAUNCHER_SETTINGS</string>'
    echo '    <boolean name="auto_update_check" value="false" />'
    echo "$extra"
    echo "</map>"
  } > /tmp/launcher_settings.xml
  adb push /tmp/launcher_settings.xml /data/local/tmp/launcher_settings.xml
  adb shell "run-as $PKG mkdir -p shared_prefs"
  adb shell "run-as $PKG cp /data/local/tmp/launcher_settings.xml shared_prefs/launcher_settings.xml"
}

shot() {
  sleep "${2:-3}"
  adb exec-out screencap -p > "$OUT/$1.png"
  echo "Saved $OUT/$1.png"
}

go_home() {
  adb shell input keyevent KEYCODE_HOME
}

adb shell cmd package set-home-activity "$PKG/.MainActivity"

# 1) Default look
write_prefs ""
go_home
shot 01-home 6

# 2) App drawer (swipe up)
adb shell input swipe "$CX" $((H * 3 / 4)) "$CX" $((H / 4)) 250
shot 02-drawer

# 3) Long-press menu on the first drawer app
go_home
sleep 2
adb shell input swipe "$CX" $((H * 3 / 4)) "$CX" $((H / 4)) 250
sleep 2
adb shell input swipe $((W / 12)) $((H / 5)) $((W / 12)) $((H / 5)) 900
shot 03-app-menu
adb shell input keyevent KEYCODE_BACK

# 4) Settings (swipe down is mapped to Launcher settings for this capture)
go_home
sleep 2
adb shell input swipe "$CX" $((H / 3)) "$CX" $((H * 3 / 4)) 250
shot 04-settings
adb shell input swipe "$CX" $((H * 3 / 4)) "$CX" $((H / 6)) 400
shot 05-settings-more
adb shell input keyevent KEYCODE_BACK

# 5) Customized look: circle icons, serif font, gold labels, bigger icons
write_prefs '    <string name="icon_shape">circle</string>
    <string name="font">serif</string>
    <int name="label_color" value="-14249" />
    <int name="icon_size" value="72" />
    <int name="home_columns" value="5" />
    <boolean name="clock_24h" value="true" />'
go_home
shot 06-home-custom 6

ls -la "$OUT"
