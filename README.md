# KLauncher

KLauncher is an Android launcher project for Minecraft: Bedrock Edition, based on the upstream LeviLauncher project. This fork rebrands the launcher and focuses on improving multi-version APK import and runtime compatibility.

## Current capabilities

- Import and manage separate Minecraft APK instances where supported by the current runtime.
- Keep per-version instance data isolated.
- Manage worlds and resource packs.
- Load supported native modules.
- Build for Android ARM64 devices.

## APK/version compatibility

KLauncher is intended to handle user-provided Minecraft APKs, including builds obtained outside Google Play, **when the APK is compatible with the launcher's runtime and the user has the right to use it**. This does not mean every Minecraft version will run automatically: Bedrock releases can change native libraries, assets, ABI requirements, package metadata, and startup behavior. Compatibility must be tested per version. Do not use APKs you are not authorized to use, and do not assume that a sideloaded build bypasses Microsoft/Mojang licensing or account requirements.

## Requirements

- Android 9.0 (API 28) or newer
- ARM64 device (arm64-v8a)
- Android Studio, JDK 21+, Android SDK, and NDK 28.2.13676358 for building

## Build

1. Clone this repository.
2. Open it in Android Studio and allow Gradle sync to finish.
3. Configure any required local secrets in `local.properties` (for example, the CurseForge API key if you use that integration).
4. Build the `app` module using the debug or release variant.

## Project identity

- App display name: **KLauncher**
- Android namespace/application ID remains `org.levimc.launcher` to avoid breaking existing package references and integrations in this rebrand pass.
- Upstream project: [LeviLaunchroid](https://github.com/LiteLDev/LeviLaunchroid)

## Attribution and licensing

This fork contains code and third-party components from the upstream project and other dependencies. Retain the applicable copyright notices, licenses, and notices when redistributing. See [LICENSE](LICENSE) and bundled NOTICE/license files.

## Disclaimer

KLauncher is an independent community fork and is not affiliated with or endorsed by Mojang or Microsoft. Use only game files you are authorized to use.
