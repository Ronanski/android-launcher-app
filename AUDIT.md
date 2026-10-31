# Pad Launcher — audit (v0.9.0)

Mga bug na nakita sa pagbasa ng code at sa emulator test, at ang status ng bawat isa.

| # | Bug | Saan | Status |
|---|-----|------|--------|
| 1 | Nagka-crash ang launcher kapag naka-on ang overlay taskbar (walang katapusang pagtawag sa sarili sa `ComposeView.Content`) | OverlayTaskbar | Naayos sa 0.7.1 |
| 2 | Tinatakpan ng overlay taskbar ang navigation bar (3 button) | OverlayTaskbar | Naayos sa 0.8.1 |
| 3 | Hindi gumagana ang Close app / Close all (hindi mahanap ang Force stop button) | AppCloser | Inaayos (0.8.3: sariling paghahanap sa buong screen) |
| 4 | Pinapasara lang ng Back button sa overlay taskbar ang panel, hindi nagba-Back sa app | Taskbar | Naayos sa 0.9.0 |
| 5 | Lumalabas ang overlay taskbar at Start sa lock screen | Accessibility service | Naayos sa 0.9.0 (nakatago habang naka-lock) |
| 6 | Tinatakpan ng bottom gesture strip ang mga button ng nav bar, at nakaharang sa taskbar | Navigation | Naayos sa 0.9.0 |
| 7 | Gumagawa ng duplicate na kopya ng app ang "Open in split screen" (`FLAG_ACTIVITY_MULTIPLE_TASK`) | AppLaunch | Naayos sa 0.9.0 |
| 8 | Walang nangyayari sa "Rename" kapag galing sa overlay taskbar | StartMenu/Taskbar | Naayos sa 0.9.0 (nakatago doon) |
| 9 | Lumalabas ang "Open in window" kahit hindi ito kontrolado sa HyperOS | Mga menu | Naayos sa 0.9.0 (nakatago by default) |
| 10 | Hindi nasusulat ang screenshots kapag sabay ang dalawang test run | CI | Naayos |
| 11 | Nagiging "Not working" ang accessibility pagkatapos ng crash | Android | Epekto ng #1; i-off tapos i-on ulit ang service |

## Mga limitasyon ng Android/HyperOS (hindi bug ng launcher)

- Hindi kayang palitan ng launcher ang Recents screen ng system, o lagyan ng minimize/maximize ang window ng ibang app.
- Ang floating windows at split screen ay kontrolado ng HyperOS. Ang launcher ay humihiling lang, at maaaring tanggihan ito ng system.
- Kailangan ng accessibility service para sa Lock, Power, Back/Home/Recents, split screen, overlay taskbar, at pagsasara ng app. Sa Android 13+, kailangan munang i-"Allow restricted settings".
- Hindi lumiliit ang mga app para sa overlay taskbar, kaya natatakpan nito ang ibabang bahagi ng app.
