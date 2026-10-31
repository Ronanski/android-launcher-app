# Changelog

Lahat ng update ng Pad Launcher. Ang bawat `## X.Y.Z` na section dito ay nagiging release notes sa GitHub Releases.

## 0.5.0 — 2026-10-31

Navigation na hindi umaasa sa nav bar ng Xiaomi.

- **Navigation gestures sa lahat ng app** (Settings → Navigation). Gumagana ito sa pamamagitan ng accessibility service:
  - Swipe pataas mula sa ibaba → Home
  - Swipe pataas at i-hold → Recents
  - Swipe papasok mula sa kaliwa o kanang gilid → Back
- **Back / Home / Recents buttons** sa kaliwa ng taskbar.
- **Itago ang navigation bar ng Xiaomi** sa lahat ng app (experimental). Kailangan ng isang beses na `adb` command mula sa PC; nasa Settings → Navigation ang buong gabay at ang command.

## 0.4.1 — 2026-10-31

- **Task View:** may *Clear all* at ✕ na sa bawat app. Sa taskbar, long-press sa bukas na app → *Close*. Tinatanggal nito ang app sa taskbar hanggang gamitin mo ulit, at isinasara ang background process nito.
- Mas maikli na ang pagbilang sa app bilang "bukas": 4 na oras ang default (dati 12), at pwedeng baguhin sa Settings → Taskbar.
- **Quick Settings:** may *Edit* na para piliin at ayusin ang mga tile. Mga bagong tile: Hotspot, Display, Sound, Screenshot, Lock, Split screen.
- Nakatago na ang Android status bar sa desktop, dahil nasa taskbar na ang orasan, baterya, at notifications. I-swipe pababa mula sa itaas para ilabas ito. May setting din para itago ang navigation bar.
- Ayos: hindi na nagpapatong ang taskbar icons at ang system tray kapag portrait. Mag-i-scroll na ang icons kapag sobrang dami.
- Ayos: tamang brightness level sa Xiaomi/HyperOS.
- Mas malawak na automated test sa emulator: portrait, sukat ng phone, pag-clear ng recents, QS editor, at crash log.

## 0.4.0 — 2026-10-31

Mas Windows na.

- **Taskbar:** iisang hanay na ang pinned at bukas/recent na apps, gaya ng Windows. May guhit sa ilalim ng mga bukas na app; mas mahaba at accent color ang guhit ng huling ginamit.
- **Task View button** sa taskbar: listahan ng mga recent app, na may *Window*, *Split*, at *Android recents*.
- **Open in window:** pwedeng buksan ang app bilang resizable window (Android freeform). Kapag naka-on sa device, ang system ang naglalagay ng title bar na may minimize, maximize, at close. May bagong setting na *Open apps in windows* at gabay sa Settings → Windows & multitasking.
- **Pag-click sa orasan:** Notifications (kailangan ng notification access) at kalendaryo, gaya ng Windows 11.
- **Pag-click sa Wi-Fi/volume/baterya:** Quick Settings na may tiles (Wi-Fi, Bluetooth, Flashlight, Auto-rotate, Do not disturb, Battery saver, Airplane, Location, All settings), brightness at volume slider, at baterya.
- **Bagong Settings na parang Windows Settings:** bumubukas sa loob ng launcher, may menu sa kaliwa (Personalization, Desktop & icons, Taskbar, Start, Windows & multitasking, Gestures, Apps, Permissions & setup, Updates & about).
- **Mas mabilis na hold menu:** lumalabas na ang menu habang naka-hold pa, at mas maikli ang long-press (320 ms).

## 0.3.1 — 2026-10-31

- Mas solid ang default na kulay ng Start menu at taskbar (96% opacity), para hindi na kita ang desktop icons sa likod. Pwede pa ring baguhin sa Settings → Panel opacity.

## 0.3.0 — 2026-10-31

Windows-style na launcher.

- **Desktop:** malayang pagpwesto ng icons at widgets sa grid. Long-press at i-drag para ilipat; long-press at bitawan para sa menu.
- **Widgets:** long-press sa bakanteng desktop → *Add widget*. Pwedeng i-drag, i-resize (long-press → *Resize*, tapos hilahin ang bilog sa kanto), at tanggalin.
- **Taskbar:** Start button, Search, naka-pin na apps, recent apps (opsyonal), baterya, orasan at petsa, notifications.
- **Start menu:** search, Pinned apps, All apps (A–Z), Settings, Lock at Power options.
- Right-click-style na menu sa apps: Pin to Start, Pin to taskbar, Add to desktop, Rename, shortcuts, split screen, App info, Uninstall.
- Desktop menu: Add widget, Sort icons by name, Change wallpaper, Personalize.
- Bagong settings: desktop grid columns, taskbar sa gitna o kaliwa, recent apps, accent color, panel opacity, pinned apps per row.
- Awtomatikong nalilipat ang mga dating home apps papunta sa desktop, Start, at taskbar.

## 0.2.0 — 2026-10-31

- **Auto-update:** nagche-check ang launcher sa GitHub Releases (bawat 6 na oras, o manual sa Settings → Updates). Kapag may bagong version, may banner sa home screen. I-tap lang para i-download at i-install.
- Versioning: iisang version sa `version.properties`, at awtomatikong ginagawa ang GitHub Release kada bagong version.
- Release builds na naka-sign gamit ang parehong key, kaya pwedeng mag-update nang hindi nawawala ang settings.

## 0.1.0 — 2026-10-31

- Unang version: home screen na may orasan at mga paboritong app, app drawer na may search, long-press menu (shortcuts, split screen, rename, hide, uninstall), icon packs, icon shapes, custom fonts, gestures, at Settings.
