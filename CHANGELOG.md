# Changelog

Lahat ng update ng Pad Launcher. Ang bawat `## X.Y.Z` na section dito ay nagiging release notes sa GitHub Releases.

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
