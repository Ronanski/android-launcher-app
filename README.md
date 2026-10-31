# Pad Launcher

Simple at customizable na Android launcher. Ginawa para sa Xiaomi Pad 5, pero gagana rin sa kahit anong Android 8.0+ na phone o tablet.

## Features

**Home screen**
- Orasan at petsa (pwedeng 12h o 24h, i-tap para buksan ang alarms)
- Grid ng mga paborito mong app; ikaw ang pipili ng bilang ng columns
- Long-press sa bakanteng space para buksan ang Settings, palitan ang wallpaper, o makita lahat ng apps

**App drawer** (swipe up)
- Search bar. Pindutin ang Enter para buksan agad ang unang result
- Pwedeng i-adjust ang columns at background opacity
- I-swipe pababa o pindutin ang Back para isara

**Long-press sa app**
- App shortcuts (hal. "New tab" sa Chrome). Kailangan default launcher ka para lumabas ito
- Add/Remove sa home, Move left/right
- **Open in split screen**
- Rename, Hide, App info, Uninstall

**Customization**
- Icon packs: kahit anong Nova/ADW-compatible na icon pack mula sa Play Store
- Icon shape: original, circle, squircle, rounded, square, teardrop
- Icon size, label size, label color, label shadow
- Fonts: built-in na Light / Thin / Condensed / Serif / Mono / Casual / Cursive, **o sarili mong .ttf/.otf file**

**Gestures** (pwedeng palitan sa Settings)
- Swipe up: app drawer
- Swipe down: notifications
- Double tap: lock screen
- Iba pang pagpipilian: quick settings, recent apps, toggle split screen, launcher settings

## Paano i-install

1. Pumunta sa **Actions** tab ng repo, buksan ang pinakabagong successful na "Build APK" run, at i-download ang `PadLauncher-debug-apk` artifact.
2. I-unzip at i-install ang `app-debug.apk` sa tablet (payagan ang "Install unknown apps").
3. Buksan ang Pad Launcher, long-press sa bakanteng space, tapos **Launcher settings → Set as default home app**.
4. Para gumana ang double-tap lock, recents, at split-screen gestures: **Settings → Accessibility service**, tapos i-on ang Pad Launcher.
5. **Importante sa Xiaomi:** Settings → Apps → Pad Launcher → i-on ang **Autostart**, at gawing **No restrictions** ang Battery saver. Kung hindi, papatayin ng HyperOS ang accessibility service.

## Limitasyon sa Xiaomi (MIUI / HyperOS)

Sa Xiaomi, ang **Recents screen, full-screen gestures, at split-screen button sa Recents** ay bahagi ng system launcher (MIUI/HyperOS Home), hindi ng third-party launcher. Ito rin ang dahilan kung bakit nawawala ang mga ito kapag gumamit ka ng Nova. **Walang third-party launcher na makakapag-ayos nito**, dahil nakakandado ito sa system.

Mga paraan para malusutan:
- **Split screen:** gamitin ang gesture na "Toggle split screen" (accessibility), o ang "Open in split screen" sa long-press menu. Depende sa HyperOS version kung gagana.
- **Recent apps:** gamitin ang gesture na "Recent apps".
- **Lock screen:** double tap, gamit ang accessibility service.

## Build locally

Kailangan: Android Studio (o JDK 17 + Android SDK 35).

```bash
./gradlew assembleDebug
```

Lalabas ang APK sa `app/build/outputs/apk/debug/`.

Tech: Kotlin + Jetpack Compose, minSdk 26, targetSdk 34.
