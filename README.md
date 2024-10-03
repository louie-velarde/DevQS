# DevQS

Quick Settings tiles for developers

[<img src="https://f-droid.org/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/me.velc.devqs)


Tired of banking and other financial apps whining that Developer Options is on? With *DevQS*,
Developer Options can be toggled off then back on from the Quick Settings panel without resetting
other settings. Long-clicking on any of the tiles will also open the Developer Options settings
screen if the DEV toggle is on.

**IMPORTANT:**

There is no launcher icon nor activity. The tiles must be manually added to the Quick Settings panel.
The app requires the permission WRITE_SECURE_SETTINGS which can be granted using ADB.

`adb shell pm grant me.velc.devqs android.permission.WRITE_SECURE_SETTINGS`

Also, the tiles are disabled while on the lock screen and can only be toggled after unlocking the device.
If that is not the case, try allowing auto-start or turning off battery optimizations for the app.
See [dontkillmyapp.com](https://dontkillmyapp.com) for instructions.

## Screenshots

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg" width=49% /> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" width=49% />
