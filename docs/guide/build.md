# Build from Source

This page is for contributors who want to build LeviLauncher for Android itself. Native mod build notes live in [Native Mod Quick Start](/guide/developer).

## Prerequisites

- Git.
- Android Studio.
- JDK 21 or later.
- Android SDK for API 28 or later.
- Android NDK and CMake components installed through Android Studio when native code is built.

## Open the Project

```bash
git clone https://github.com/LiteLDev/LeviLaunchroid.git
cd LeviLaunchroid
```

Open the project directory in Android Studio and allow Gradle sync to finish. The project root is named `levilauncher` in Gradle settings, while the repository remains `LeviLaunchroid`.

## Build and Run

1. Connect an Android device or start an emulator that matches the supported Android baseline.
2. Select the `app` run configuration.
3. Build and run from Android Studio.
4. Install the official Minecraft app on the test device before testing launcher flows.

## Command Line Build

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

## Contributor Checks

Before submitting launcher changes, build the app and test the touched flow on a device. Documentation-only changes should at least pass the docs build: run `npm ci` and then `npm run build` in `docs/` (the site is built with levimc-kit).
