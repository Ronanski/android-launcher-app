#!/usr/bin/env bash
# Runs inside an Android emulator (GitHub Actions) and captures screenshots
# of Pad Launcher into docs/screenshots/.
set -euo pipefail

PKG=com.ronanski.launcher
APK=app/build/outputs/apk/debug/app-debug.apk
OUT=docs/screenshots
mkdir -p "$OUT"
rm -f "$OUT"/*.png "$OUT"/*.txt

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
adb shell appwidget grantbind --package "$PKG" --user 0 || true
adb shell appops set "$PKG" GET_USAGE_STATS allow || true
adb shell cmd notification allow_listener "$PKG/$PKG.LauncherNotificationService" || true
adb shell cmd notification post -S bigtext -t "Meeting at 3 PM" demo1 "Project sync with the team in Conference Room B" || true
adb shell cmd notification post -t "Battery optimized" demo2 "Pad Launcher is ready to use" || true

# Status bar height in px (24dp at the emulator density) — desktop coordinates start below it.
DENSITY=$(adb shell wm density | tr -d '\r' | awk '/Physical density/ {print $3}')
DP=$((DENSITY / 160))
SB=0   # the Android status bar is hidden on the desktop by default

# 1) Windows-style desktop + taskbar
write_prefs ""
go_home
shot 01-desktop 6

# 2) Start menu (swipe up on empty desktop)
adb shell input swipe $((W * 6 / 10)) $((H / 2)) $((W * 6 / 10)) $((H / 5)) 250
shot 02-start
adb shell input keyevent KEYCODE_BACK

# 3) Long-press a desktop icon -> context menu
go_home
sleep 2
CELL=$((W / ((W / DP) / 104)))
adb shell input swipe $((CELL / 2)) $((SB + CELL / 2)) $((CELL / 2)) $((SB + CELL / 2)) 1000
shot 03-icon-menu
adb shell input keyevent KEYCODE_BACK

# 4) Long-press empty desktop -> "Add widget" -> widget picker
go_home
sleep 2
MX=$((W * 6 / 10))
MY=$((H * 4 / 10))
adb shell input swipe "$MX" "$MY" "$MX" "$MY" 1000
sleep 2
adb shell input tap $((MX + 50 * DP)) $((MY + 32 * DP))
shot 04-widget-picker 4

# 5) Pick the first widget
adb shell input tap $((115 * DP)) $((SB + 230 * DP))
shot 05-desktop-widget 6
go_home

TBY=$((H - 86 * DP))

# 6) Notifications & calendar (tap the clock)
go_home
sleep 2
adb shell input tap $((W - 53 * DP)) "$TBY"
shot 06-calendar-flyout

# 7) Quick settings (tap Wi-Fi / volume / battery)
adb shell input keyevent KEYCODE_BACK
sleep 1
adb shell input tap $((W - 160 * DP)) "$TBY"
shot 07-quick-settings
adb shell input keyevent KEYCODE_BACK

# 8) Settings window (swipe down on empty desktop is mapped to Launcher settings for this capture)
sleep 1
adb shell input swipe $((W * 6 / 10)) $((H / 3)) $((W * 6 / 10)) $((H * 2 / 3)) 250
shot 08-settings
adb shell input keyevent KEYCODE_BACK

# 9) Personalized: left taskbar (Windows 10 style), purple accent, circle icons, serif font
write_prefs '    <string name="icon_shape">circle</string>
    <string name="font">serif</string>
    <int name="accent_color" value="-4745994" />
    <boolean name="taskbar_centered" value="false" />
    <boolean name="clock_24h" value="true" />'
go_home
sleep 4
adb shell input swipe $((W * 6 / 10)) $((H / 2)) $((W * 6 / 10)) $((H / 5)) 250
shot 09-personalized-start
adb shell input keyevent KEYCODE_BACK

# 10) Task View — open a few apps first so there is something in it
for pkg in com.android.settings com.google.android.deskclock com.google.android.calendar; do
  adb shell monkey -p "$pkg" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
  sleep 3
done
go_home
sleep 3
# (after Back/Home/Recents, Task View is the third taskbar button when the taskbar is left-aligned)
adb shell input tap $((258 * DP)) "$TBY"
shot 10-task-view

# 11) Task View -> Clear all (apps should disappear from Task View and the taskbar)
adb shell input tap $((W - 210 * DP)) $((50 * DP))
shot 11-task-view-cleared
adb shell input keyevent KEYCODE_BACK

# 12) Quick settings -> Edit
sleep 1
adb shell input tap $((W - 175 * DP)) "$TBY"
sleep 2
adb shell input tap $((W - 88 * DP)) $((TBY - 66 * DP))
shot 12-quick-settings-edit
adb shell input keyevent KEYCODE_BACK

# 17) Settings -> Navigation page
go_home
sleep 1
adb shell input swipe $((W * 6 / 10)) $((H / 3)) $((W * 6 / 10)) $((H * 2 / 3)) 250
sleep 2
adb shell input tap $((142 * DP)) $((284 * DP))
shot 17-settings-navigation
adb shell input keyevent KEYCODE_BACK

# 18-20) Light theme: Start, search with calculator, Personalization (wallpapers)
write_prefs '    <string name="theme_mode">light</string>
    <boolean name="taskbar_centered" value="false" />'
go_home
sleep 4
adb shell input swipe $((W * 6 / 10)) $((H / 2)) $((W * 6 / 10)) $((H / 5)) 250
shot 18-light-start
adb shell input keyevent KEYCODE_BACK
sleep 1
adb shell input tap $((210 * DP)) "$TBY"
sleep 2
adb shell "input text '12*7+1'"
shot 19-search-calculator
adb shell input keyevent KEYCODE_BACK
adb shell input keyevent KEYCODE_BACK
sleep 1
adb shell input swipe $((W * 6 / 10)) $((H / 3)) $((W * 6 / 10)) $((H * 2 / 3)) 250
shot 20-settings-personalization 4
adb shell input keyevent KEYCODE_BACK

# 21-22) Taskbar over apps: open another app, then press Start on the overlay taskbar
write_prefs '    <boolean name="overlay_taskbar" value="true" />
    <boolean name="taskbar_centered" value="false" />'
adb shell settings put secure enabled_accessibility_services "$PKG/$PKG.LauncherAccessibilityService"
adb shell settings put secure accessibility_enabled 1
go_home
sleep 4
adb shell monkey -p com.android.settings -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
sleep 4
shot 21-overlay-taskbar-over-app 1
adb shell input tap $((166 * DP)) "$TBY"
shot 22-overlay-start-over-app
adb shell input keyevent KEYCODE_BACK
adb shell settings delete secure enabled_accessibility_services || true
adb shell settings put secure accessibility_enabled 0
go_home

# 13-14) Portrait tablet
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
sleep 3
go_home
shot 13-portrait-desktop 4
PW=$H
PH=$W
adb shell input tap $((PW - 175 * DP)) $((PH - 86 * DP))
shot 14-portrait-quick-settings
adb shell input keyevent KEYCODE_BACK
adb shell settings put system user_rotation 0
sleep 3

# 15-16) Phone-sized screen with a large system font (catches clipped text)
adb shell wm size 1080x2340
adb shell wm density 440
adb shell settings put system font_scale 1.3
sleep 3
go_home
shot 15-phone-desktop 4
adb shell input swipe 540 1400 540 500 250
shot 16-phone-start
adb shell input keyevent KEYCODE_BACK
adb shell wm size reset
adb shell wm density reset
adb shell settings put system font_scale 1.0

# Any crashes during the run
adb logcat -d -b crash > "$OUT/crash-log.txt" || true
echo "Crash log lines: $(wc -l < "$OUT/crash-log.txt")"

ls -la "$OUT"
