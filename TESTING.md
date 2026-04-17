# Testing without Android Studio

You have three realistic paths. Pick one.

---

## Option A — GitHub Actions builds the APK for you (zero local setup)

Easiest if you don't want to install any SDKs.

1. Push this folder to a GitHub repo.
2. The workflow at [.github/workflows/build.yml](.github/workflows/build.yml) runs automatically.
3. Open the **Actions** tab → latest run → download the `app-debug` artifact → unzip → you have `app-debug.apk`.
4. Transfer it to your phone (USB, email, Drive). On the phone, enable **Install unknown apps** for your file manager and tap the APK to install.
5. Open **Usage Reminder**, flip the switch, grant notification permission.

---

## Option B — Build locally from the command line (Windows)

One-time setup (~600 MB of downloads):

1. **JDK 17** — install Temurin: `winget install EclipseAdoptium.Temurin.17.JDK` (or download from adoptium.net). Verify: `java -version`.
2. **Android command-line tools** — download "Command line tools only" from https://developer.android.com/studio (bottom of page).
   - Unzip to `C:\Android\cmdline-tools\latest\` (the `latest` folder must contain `bin/`, `lib/`, etc.).
   - Set env vars (PowerShell, one-time):
     ```powershell
     [Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Android", "User")
     [Environment]::SetEnvironmentVariable("Path", "$env:Path;C:\Android\cmdline-tools\latest\bin;C:\Android\platform-tools", "User")
     ```
   - Restart your terminal.
3. **Install SDK pieces + accept licenses:**
   ```bash
   sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   sdkmanager --licenses     # press y repeatedly
   ```
4. **Gradle** — install via scoop or download:
   ```bash
   scoop install gradle
   ```
   Or grab the 8.7 binary zip from https://gradle.org/releases/ and add its `bin` to PATH.

Generate the wrapper once, then build:
```bash
cd C:\Users\msinghseegriwal\Documents\MobApp
gradle wrapper --gradle-version 8.7
gradlew.bat assembleDebug
```

Output: `app\build\outputs\apk\debug\app-debug.apk`.

Install on a connected phone (USB debugging enabled in Developer Options):
```bash
adb devices                      # confirm phone is listed
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Or just copy the APK to the phone and tap it.

---

## Option C — Test on an emulator without a physical phone

After Option B's SDK install:
```bash
sdkmanager --install "system-images;android-34;google_apis;x86_64" "emulator"
avdmanager create avd -n test34 -k "system-images;android-34;google_apis;x86_64"
emulator -avd test34
```
Then `adb install -r ...` the APK.

Note: emulator screen-on/off broadcasts fire when you press the power button (F7 in the emulator window) or run `adb shell input keyevent 26`.

---

## Shortcut: test the alert without waiting 18 minutes

Editing one constant lets you verify the alert fires end-to-end:

1. Open [app/src/main/java/com/example/mobapp/ScreenTimeService.kt](app/src/main/java/com/example/mobapp/ScreenTimeService.kt).
2. Change:
   ```kotlin
   const val THRESHOLD_MS: Long = 18L * 60L * 1000L
   ```
   to:
   ```kotlin
   const val THRESHOLD_MS: Long = 30L * 1000L   // 30 seconds for testing
   ```
3. Rebuild + reinstall. Flip the switch, lock the screen, unlock, wait 30 s — notification + vibration + alarm sound should fire.
4. Change it back to `18L * 60L * 1000L` for real use.

## Manually driving screen events via adb

Useful on an emulator or a dev-connected phone:
```bash
adb shell input keyevent 26      # toggles power (screen on/off)
adb logcat | findstr "ScreenTimeService"    # watch service logs
```

## Troubleshooting

- **Switch doesn't persist "on" after reboot** — confirm the app has battery-saver whitelist on OEM skins (Xiaomi/Oppo/Samsung). Settings → Apps → Usage Reminder → Battery → Unrestricted.
- **No sound** — make sure Do Not Disturb is off, and the alarm channel volume is up. The channel uses the alarm stream, not the ringer.
- **Notification doesn't appear on Android 13+** — you declined the `POST_NOTIFICATIONS` prompt. Settings → Apps → Usage Reminder → Notifications → allow.
