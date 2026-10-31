# Pad Launcher

Simple at customizable na Android launcher. Ginawa para sa Xiaomi Pad 5, pero gagana rin sa kahit anong Android 8.0+ na phone o tablet.

## Features

Windows-style na launcher para sa Android tablets (at phones).

**Desktop**
- Pwedeng ilagay kahit saan sa grid ang mga app icon at widget
- **Long-press + drag** para ilipat; **long-press + bitaw** para sa menu
- Long-press sa bakanteng desktop: *Add widget*, *Sort icons by name*, *Change wallpaper*, *Personalize*

**Widgets**
- Magdagdag ng kahit anong Android widget
- I-drag para ilipat; long-press → *Resize*, tapos hilahin ang bilog sa kanto; long-press → *Remove widget*

**Taskbar**
- Start button, Search, naka-pin na apps, at recent apps (opsyonal, kailangan ng "Usage access")
- System tray: baterya (tap para sa quick settings), orasan at petsa (tap para sa notifications)
- Pwedeng nasa gitna (Windows 11) o nasa kaliwa (Windows 10)

**Start menu**
- Search, Pinned apps, All apps (A–Z)
- Settings, Lock, Power options

**Menu ng app** (long-press)
- Pin to Start, Pin to taskbar, Add to desktop, Rename, Hide
- App shortcuts, Open in split screen, App info, Uninstall

**Customization**
- Accent color, panel opacity, laki ng desktop grid
- Icon packs (Nova/ADW), icon shapes, icon size
- Fonts (built-in o sariling .ttf/.otf), kulay at laki ng labels

**Gestures sa bakanteng desktop** (pwedeng palitan)
- Swipe up: Start menu
- Swipe down: notifications
- Double tap: lock screen

## Screenshots

Makikita sa [`docs/screenshots/`](docs/screenshots). Awtomatiko itong ginagawa ng *Screenshots* workflow sa isang Android tablet emulator.

## Paano i-install

1. Pumunta sa **[Releases](https://github.com/Ronanski/android-launcher-app/releases/latest)** at i-download ang `PadLauncher-vX.Y.Z.apk`.
2. I-install sa tablet (payagan ang "Install unknown apps").
3. Buksan ang Pad Launcher, long-press sa bakanteng space, tapos **Launcher settings → Set as default home app**.
4. Para gumana ang double-tap lock, recents, at split-screen gestures: **Settings → Accessibility service**, tapos i-on ang Pad Launcher.
5. **Importante sa Xiaomi:** Settings → Apps → Pad Launcher → i-on ang **Autostart**, at gawing **No restrictions** ang Battery saver. Kung hindi, papatayin ng HyperOS ang accessibility service.

## Updates

Kusang nagche-check ang app ng bagong version sa GitHub Releases, isang beses kada 6 na oras. Pwede ring manual sa **Settings → Updates**. Kapag may update, may banner sa home screen. I-tap ito para i-download at i-install. Sa unang beses, hihingi ang Android ng permiso na "Install unknown apps" para sa Pad Launcher.

### Paano mag-release ng bagong version (para sa developer)

1. Taasan ang `VERSION_NAME` sa `version.properties` (hal. `0.2.0` → `0.2.1`). Awtomatikong kinukuwenta ang `versionCode` bilang `MAJOR*10000 + MINOR*100 + PATCH`.
2. Magdagdag ng `## 0.2.1 — petsa` na section sa `CHANGELOG.md`. Ito ang magiging release notes.
3. I-push sa default branch. Ang GitHub Actions ang magbi-build ng APK at gagawa ng release na `v0.2.1`.

Kung hindi binago ang version, magbi-build pa rin pero hindi gagawa ng bagong release.

> Paalala: naka-commit ang signing key (`app/debug.keystore`) para iisa ang signature ng bawat build at gumana ang updates. Ayos ito para sa personal na gamit. Kung ipapamahagi mo ito sa publiko, ilipat ito sa GitHub Secrets.

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
