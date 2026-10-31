# Private DNS Quick Toggle

Quick Settings tile + home-screen widget + small Compose app for Android's Private DNS.
Tested design target: Realme / ColorOS (also stock Android, Samsung, Xiaomi, OnePlus).
**Not compiled or run by the author on any device — build it and report back.**

## Build the APK
**GitHub Actions (no local setup):** push this folder to a GitHub repo, open Actions → "Build APK" →
download the `PrivateDnsToggle-debug-apk` artifact → install `app-debug.apk`.

**Android Studio:** File → Open this folder → Build → Build APK(s). (Or `gradle assembleDebug` with Gradle 8.9 + JDK 17.)

## Enable real switching (one-time, no root)
Android only lets apps *read* Private DNS. Changing it needs `WRITE_SECURE_SETTINGS`, which only ADB can grant:

    adb shell pm grant com.privatedns.toggle android.permission.WRITE_SECURE_SETTINGS

Phone: Settings → About → tap Build number 7 times → Developer options → USB debugging. Connect to a PC and run the command.
If the grant is refused on ColorOS/Realme, look in Developer options for a security/permission-monitoring toggle
(wording varies by version) and retry.
Without the grant, the tile and widget open the Private DNS settings screen instead of switching. Nothing is faked.

## Android limitations (honest list)
- **Expanded tile panel (like Wi-Fi/Bluetooth):** not possible. Those use private system UI. The public API only allows
  label, subtitle, icon, state, tap, and a long-press preferences activity. Long-press opens this app's main screen.
- **Changing Private DNS:** needs the ADB-granted permission above. No root required after that.
- **Provider hostname:** readable only when mode = "hostname". In Automatic mode no hostname exists.
- **Settings shortcut:** no public deep link to the Private DNS page. The app tries `Settings$PrivateDnsSettingsActivity`, then
  Network settings, then main Settings. ColorOS may land on Connection & sharing; Private DNS is inside.
- **Widget sync:** updated on tap, on tile/app activity, every ~30 min, and via a JobScheduler trigger on setting changes
  (best-effort; ColorOS battery management can delay it). Allow background activity for the app.
- **Add-tile prompt:** Android 13+ only, and some ROMs ignore it. Fallback: Quick Settings → Edit.
