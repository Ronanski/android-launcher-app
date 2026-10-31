# Changelog

Lahat ng update ng Pad Launcher. Ang bawat `## X.Y.Z` na section dito ay nagiging release notes sa GitHub Releases.

## 0.8.1 — 2026-10-31

- Ayos: tinatakpan pa rin ng overlay taskbar ang navigation bar sa v0.7.2. Kinukuwenta na ngayon ang taas ng navigation bar, at inilalagay ang taskbar sa ibabaw nito.

## 0.8.0 — 2026-10-31

- **Totoong pagsasara ng app**, gaya ng "End task" sa Windows:
  - Long-press ang bukas na app sa taskbar → *Close app*
  - Sa Task View: ✕ sa bawat app, o *Close all*

  Binubuksan ng accessibility service ang App info ng app, pinipindot ang *Force stop* at *OK*, tapos bumabalik sa dati mong screen. Mabilis na makikita sandali ang App info page habang isinasara. Babasahin lang ng service ang screen habang may isinasarang app. Tandaan: mawawala ang hindi pa naka-save sa app, gaya ng End task.
- Pagkatapos mag-update, i-off tapos i-on ulit ang Pad Launcher sa Accessibility para gumana ang bagong pagsasara.

## 0.7.2 — 2026-10-31

- Nasa ibabaw na ng navigation bar (ang 3 button ng Xiaomi) ang overlay taskbar, at hindi na nito tinatakpan ang mga iyon.
- Mas malinaw na pangalan: "Remove from taskbar" (dati "Close") at "Clear list" (dati "Clear all"). Hindi kayang isara ng launcher ang window ng ibang app, kaya tinatanggal lang ng mga ito ang app sa listahan.
- Mas matatag ang accessibility service: hindi na ito babagsak kapag nagka-error ang overlay.

## 0.7.1 — 2026-10-31

- **Ayos sa crash:** nagka-crash ang launcher kapag naka-on ang "Show the taskbar over all apps" sa v0.7.0. Nakita ito sa emulator test.
- Search: ginagamit na ang buong taas at nakatago ang Settings/Power habang nagta-type, para kita ang lahat ng resulta kahit bukas ang keyboard.

## 0.7.0 — 2026-10-31

- **Taskbar sa ibabaw ng lahat ng app** (Settings → Taskbar → *Show the taskbar over all apps*). Gumagana ito sa pamamagitan ng accessibility service. Nasa ibabaw na ng anumang app at floating window ang Start, Search, Task View, Notifications, at Quick Settings, kaya hindi na sila natatakpan. Tandaan: hindi lumiliit ang mga app para dito, kaya natatakpan ng taskbar ang ibabang bahagi ng app na gamit mo.
- Bumubukas na ang Settings ng launcher mula kahit saan (Start, Search, gestures). Dinadala ka nito pabalik sa launcher.

## 0.6.0 — 2026-10-31

- **Built-in wallpapers:** 6 orihinal na disenyo (Bloom, Glow, Waves, Ribbon, Aurora, Solid) na sumusunod sa accent color mo at sa tema. Nasa Settings → Personalization. Hindi kasama ang wallpapers ng Microsoft dahil may copyright ang mga iyon.
- **Light mode:** Dark, Light, o Same as system, para sa taskbar, Start, mga flyout, at Settings.
- **Notifications:** nakatago na by default ang mga ongoing na notification ("… is running", media, at iba pa). Long-press ang isang notification para itago ang notifications ng app na iyon o buksan ang notification settings nito. May listahan ng mga itinagong app sa Settings → Notifications.
- **Search:** bukod sa apps, may calculator na (hal. `12*7+1`), Android settings, settings ng launcher, contacts (kailangan ng permiso), at web search.
- **Backup & restore:** i-save sa file ang desktop, taskbar, Start, at lahat ng settings, at i-load ulit kahit kailan. Kailangang idagdag ulit ang widgets pagkatapos mag-restore.

## 0.5.2 — 2026-10-31

- Ayos: hindi na napuputol ang orasan at petsa sa taskbar kapag malaki ang system font size.
- Settings → Permissions & setup: gabay sa "Allow restricted settings" kapag naka-grey ang accessibility o notification access.
- Sinusubukan na rin sa emulator ang malaking font size.

## 0.5.1 — 2026-10-31

- **Brightness slider:** tama na ang level sa Xiaomi (dati, mga 13% ang pakita kahit max) at sumusunod na sa curve ng slider ng Android.
- **Open in window:** humihiling na ng window mode diretso sa Android. Experimental pa rin sa HyperOS.
- **Phone at makikitid na screen:** laging kita ang Start, Search, at Task View. Mag-i-scroll ang mga app sa taskbar, at mas siksik ang tray. Nag-a-adjust na rin ang bilang ng column ng Start menu para hindi maputol ang mga pangalan.
- Mas madilim ang background ng Task View para mabasa nang maayos.
- Kasama na rin ang lahat ng laman ng 0.4.1 (Clear all sa recents, Edit sa Quick Settings, nakatagong status bar, ayos sa portrait).

## 0.5.0 — 2026-10-31

Navigation na hindi umaasa sa nav bar ng Xiaomi.

- **Navigation gestures sa lahat ng app** (Settings → Navigation). Gumagana ito sa pamamagitan ng accessibility service:
  - Swipe pataas mula sa ibaba → Home
  - Swipe pataas at i-hold → Recents
  - Swipe papasok mula sa kaliwa o kanang gilid → Back
- **Back / Home / Recents buttons** sa kaliwa ng taskbar.
- **Open in window:** humihiling na ang launcher ng window mode diretso sa Android, gamit ang HiddenApiBypass. Ito rin ang mode na gamit ng floating windows ng Xiaomi. Experimental ito sa HyperOS.
- **Brightness slider:** tama na ang level sa Xiaomi (dati, mga 13% ang pakita kahit max), at sumusunod na sa curve ng slider ng Android.
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
