# Usage Reminder (MobApp)

Android app for screen time rationing — a native Android (Kotlin) app that alerts you with a notification + sound/vibration after **18 minutes of continuous screen-on time**. The timer resets whenever the screen turns off.

## How it works
- A foreground `ScreenTimeService` registers a runtime `BroadcastReceiver` for `ACTION_SCREEN_ON` / `ACTION_SCREEN_OFF` (these broadcasts cannot be declared in the manifest — they only work while a process is alive, which is why we use a foreground service).
- On screen-on, a `Handler.postDelayed` is scheduled for 18 minutes. On screen-off, the callback is cancelled, effectively resetting the timer.
- When the 18-minute callback fires, a high-importance notification is posted on the alarm channel and the device vibrates.
- `BootReceiver` restarts the service after reboot if monitoring was enabled.
- A persistent on/off switch in the app, plus a "Turn off" action on every notification, lets you disable the feature at any time.

## Build & run
1. Open the `MobApp/` folder in **Android Studio** (Giraffe or newer).
2. Let Gradle sync (it will download the Android Gradle Plugin 8.5.2 + Kotlin 1.9.24).
3. Connect a device or start an emulator (API 24+).
4. Run the **app** configuration.
5. On first launch, flip the switch and grant the notification permission prompt.

If you don't have Android Studio installed, see [TESTING.md](TESTING.md) for a CLI-only build path and a GitHub Actions workflow that builds the APK for you.

## Adjusting the threshold
Change `THRESHOLD_MS` in [`ScreenTimeService.kt`](app/src/main/java/com/example/mobapp/ScreenTimeService.kt).

## Permissions
- `POST_NOTIFICATIONS` — required on Android 13+ to show the alert.
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` — keeps the receiver alive.
- `VIBRATE` — used by the alert.
- `RECEIVE_BOOT_COMPLETED` — re-starts monitoring after reboot.

## Notes / limitations
- "Continuous screen-on" is measured from the last `SCREEN_ON` broadcast. Brief screen-off/on cycles will reset the timer.
- Some OEM skins (Xiaomi, Oppo, etc.) aggressively kill background services. The app may need to be whitelisted in the vendor's battery-saver settings.
- No gradle wrapper is checked in. Android Studio will generate `gradlew` on first sync, or you can run `gradle wrapper` once.
