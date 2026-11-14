# H59 Heart Guard

تطبيق أندرويد بالعربية (RTL) يتصل بساعة **LIGE H59_F603** عبر البلوتوث منخفض الطاقة، ويراقب معدل نبض القلب، ويرسل تنبيهات صوتية ونصية عند تجاوز الحد المحدد.

Arabic RTL Android app that connects to the LIGE H59_F603 smartwatch over Bluetooth Low Energy, monitors heart rate, and sends voice and written alerts when a configured threshold is crossed.

Developer: Zain Abuzaid. Package: `com.zain.h59heartguard`. Based on the original project at https://github.com/zainzaina/H59Heartmonitor.

## Features

- Dashboard with live BPM, connection state, watch name, threshold, and last alert.
- Threshold settings for high and low BPM, with confirmation readings, minimum duration above the threshold, cooldown between alerts, repeat count, and repeat spacing.
- One alert per episode: a sustained high or low reading triggers one alert, and no further alerts fire until the reading returns to normal or the cooldown ends.
- Arabic voice alert "تنبيه! ارتفع معدل نبض القلب، خذ أدويتك." repeated 2 to 3 times (configurable). A custom MP3 or WAV file can replace the built-in voice (up to 5 MB).
- Notifications that show the actual BPM and include an acknowledge action.
- Foreground service so monitoring continues in the background with a persistent notification.
- Do Not Disturb policy-access screen and battery-optimization shortcut.
- Alert history stored on the device.
- Simulation mode that generates test readings without a watch.

## Watch protocol

The Bluetooth code is taken unchanged from the original project:

- Service `6e40fff0-b5a3-f393-e0a9-e50e24dcca9e`
- Write `6e400002-b5a3-f393-e0a9-e50e24dcca9e`, notify `6e400003-b5a3-f393-e0a9-e50e24dcca9e`, client config `00002902-0000-1000-8000-00805f9b34fb`
- Commands 0x69 (on-demand heart rate) and 0x1E (real-time), 16-byte packets, checksum is the sum of bytes 0 to 14 modulo 256 in byte 15.
- The watch is found by name prefix `H59` or address `31:30:44:36:F6:03`.

No BLE characteristics were added.

## Simulation mode

Turn on "وضع المحاكاة" in Settings, then stop and start monitoring. The app generates readings and runs the full alert logic, so you can test thresholds and sounds without the watch. The dashboard shows a simulation badge while this mode is active.

## Do Not Disturb limits

Granting the notification policy permission lets alerts bypass Do Not Disturb where Android allows it. Android does not guarantee this on every device. Some manufacturers add extra restrictions, so test an alert on your phone before relying on it.

## Building the APK (free, GitHub Actions)

1. Push this repository to GitHub.
2. Open the **Actions** tab and run **Build H59 Heart Guard APK**, or push to `main`/`master`.
3. Download the `H59HeartGuard-debug` artifact. It contains `app-debug.apk`.

Gradle 8.9 and JDK 17 are pinned in the workflow. The Android Gradle Plugin is 8.7.3, with compileSdk and targetSdk 35 and minSdk 26.

To build locally, open the project in Android Studio (Ladybug or newer) and run **Build > Build APK(s)**.

## Medical disclaimer

H59 Heart Guard is a monitoring and reminder tool. It is not a medical device and does not diagnose any condition. The readings come from a consumer smartwatch and can be inaccurate. If you have chest pain, shortness of breath, fainting, or other severe symptoms, seek medical help or call emergency services immediately. Do not rely on this app as your only warning system.
