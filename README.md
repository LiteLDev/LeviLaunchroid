<div align="center">

![LeviLauncher Logo](https://avatars.githubusercontent.com/u/78095377?s=200&v=4)

# LeviLauncher

![Banner](https://camo.githubusercontent.com/bd7bd77cb422a267057d9863095b239b096d4f46dc831a37b45867a9acfad697/68747470733a2f2f63617073756c652d72656e6465722e76657263656c2e6170702f6170693f747970653d576176696e6726636f6c6f723d74696d654772616469656e74266865696768743d33303026616e696d6174696f6e3d66616465496e2673656374696f6e3d68656164657226746578743d4c6576694d4326666f6e7453697a653d313230)

**A lightweight Android launcher for Minecraft: Bedrock Edition (MCBE / MCPE)**

[![GitHub Release](https://img.shields.io/github/v/release/LiteLDev/LeviLaunchroid?style=flat-square&label=Release)](https://github.com/LiteLDev/LeviLaunchroid/releases)
[![License](https://img.shields.io/github/license/LiteLDev/LeviLaunchroid?style=flat-square&label=License)](LICENSE)
[![Downloads](https://img.shields.io/github/downloads/LiteLDev/LeviLaunchroid/total?style=flat-square&label=Downloads)](https://github.com/LiteLDev/LeviLaunchroid/releases)
[![Stars](https://img.shields.io/github/stars/LiteLDev/LeviLaunchroid?style=flat-square&label=Stars)](https://github.com/LiteLDev/LeviLaunchroid/stargazers)
[![Forks](https://img.shields.io/github/forks/LiteLDev/LeviLaunchroid?style=flat-square&label=Forks)](https://github.com/LiteLDev/LeviLaunchroid/network/members)
[![Contributors](https://img.shields.io/github/contributors/LiteLDev/LeviLaunchroid?style=flat-square&label=Contributors)](https://github.com/LiteLDev/LeviLaunchroid/graphs/contributors)
[![Issues](https://img.shields.io/github/issues/LiteLDev/LeviLaunchroid?style=flat-square&label=Issues)](https://github.com/LiteLDev/LeviLaunchroid/issues)
[![Pull Requests](https://img.shields.io/github/issues-pr/LiteLDev/LeviLaunchroid?style=flat-square&label=Pull%20Requests)](https://github.com/LiteLDev/LeviLaunchroid/pulls)
[![Last Commit](https://img.shields.io/github/last-commit/LiteLDev/LeviLaunchroid?style=flat-square&label=Last%20Commit)](https://github.com/LiteLDev/LeviLaunchroid/commits/main)

</div>

---

## Introduction

LeviLauncher is a lightweight, open-source Android launcher for legitimate players of Minecraft: Bedrock Edition (MCBE / MCPE). It provides an alternative to the standard Google Play installation and helps you manage multiple game versions while extending functionality with external modules.

LeviLauncher lets you import your official Minecraft APK and run it directly without requiring system installation. The launcher supports external native modules, isolated multi-version management, and built-in tools for resource packs and worlds.

For contribution guidance, see [`CONTRIBUTING.md`](CONTRIBUTING.md).  
For security reporting, see [`SECURITY.md`](SECURITY.md).  
For community rules, see [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md).  
For license details, see [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE).

---

## Key Features

- **APK Import & Installation-Free Launching** — Import your official Minecraft APK and run it directly without system installation.
- **SO Module Loading** — Load external native SO modules to extend or enhance Minecraft features and performance.
- **Multi-Version Management & Isolation** — Manage multiple Minecraft versions independently with separated configurations and data.
- **Multiple Xbox Account Management** — Switch between multiple Xbox accounts inside the launcher.
- **Resource Pack & World Management** — Import, export, and back up resource packs and worlds with the built-in manager.

---

## System Requirements

| Requirement | Details |
|---|---|
| Operating system | Android 9.0 (API 28) or higher |
| Device architecture | ARM64 (v8a) |
| RAM | 1 GB minimum; 3 GB or more recommended |
| Storage | 2 GB minimum; 5 GB or more recommended |
| Minecraft license | Official licensed copy required |

> **Note:** For better performance and stability, Android 9.0 or higher with more available RAM and free storage is recommended.

---

## Installation

### Prerequisites

Make sure the official Minecraft Bedrock Edition app is already installed on your device from Google Play. LeviLauncher depends on the official game installation.

### Installation Steps

1. Download the latest APK from the [Releases page](https://github.com/LiteLDev/LeviLaunchroid/releases).
2. Allow installation from unknown sources if Android prompts for it.
3. Open the downloaded APK from your file manager and install it.
4. Grant any requested permissions.
5. Launch LeviLauncher from your app drawer.

> **Important:** LeviLauncher requires a legitimate, licensed copy of Minecraft Bedrock Edition. Do not use unauthorized or pirated versions of the game. Make sure your Minecraft license is valid and linked to your Microsoft account.

---

## Development Setup

If you want to build LeviLauncher from source or contribute to development, use the steps below.

### Prerequisites

- Git
- Android Studio
- Java Development Kit (JDK) 21 or higher
- Android SDK with API level 28 or higher

### Setup Instructions

1. Clone the repository:

   ```sh
   git clone https://github.com/LiteLDev/LeviLaunchroid.git
   ```

2. Open the cloned project in Android Studio.

3. If prompted, allow Android Studio to download the required Gradle, Android SDK components, and other project dependencies.

4. Wait for the initial Gradle Sync to complete successfully before making any changes.

5. Connect an Android device running Android 9.0 (API 28) or later, or start an emulator with a supported Android version.

6. Select your target device, then click Run to build and deploy the application.

7. Once the build completes successfully, Android Studio will install and launch LeviLauncher on the selected device or emulator.

>Build Tip: During development, use Build Make Project for incremental builds instead of performing a full rebuild whenever possible. This can significantly reduce build times.

---

## Contribution Guidelines

We welcome contributions from the community to improve LeviLauncher. A full guide is available in [`CONTRIBUTING.md`](CONTRIBUTING.md).

---

## Usage Guidelines

LeviLauncher is intended for legitimate Minecraft Bedrock Edition users. A detailed policy summary is available in [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE).

> **Disclaimer:** The authors and contributors of LeviLauncher are not responsible for bans, damages, or issues arising from the use of this software. Use it at your own risk and in accordance with Minecraft’s terms of service.

---

## Credits & Acknowledgements

LeviLauncher would not be possible without the contributions and support of many talented individuals and organizations:

### Special Thanks To

- **LeviMC Organization** – For maintaining the LeviLauncher project and providing infrastructure support
- **Android Community** – For excellent documentation, libraries, and tools that made this launcher possible
- **Open Source Community** – For all the libraries, frameworks, and tools that power this project
- **Contributors** – A heartfelt thank you to all [contributors](https://github.com/LiteLDev/LeviLaunchroid/graphs/contributors) who have continuously improved and maintained LeviLauncher through their time and expertise

---

## Contact & Support

**Author / Team:** LeviMC Team

**Project Repository:** [https://github.com/LiteLDev/LeviLaunchroid](https://github.com/LiteLDev/LeviLaunchroid)

**Report Issues:** [GitHub Issues Page](https://github.com/LiteLDev/LeviLaunchroid/issues)

**For support and questions:** Please create an issue on the GitHub repository or contact the LeviMC team directly

---

<div align="center">

[![GitHub Release](https://img.shields.io/github/v/release/LiteLDev/LeviLaunchroid?style=flat-square&color=blue)](https://github.com/LiteLDev/LeviLaunchroid/releases)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0)
[![Release Date](https://img.shields.io/github/release-date/LiteLDev/LeviLaunchroid?style=flat-square)](https://github.com/LiteLDev/LeviLaunchroid/releases)
[![Commit Activity](https://img.shields.io/github/commit-activity/m/LiteLDev/LeviLaunchroid?style=flat-square)](https://github.com/LiteLDev/LeviLaunchroid/graphs/commit-activity)
[![Repo Size](https://img.shields.io/github/repo-size/LiteLDev/LeviLaunchroid?style=flat-square)](https://github.com/LiteLDev/LeviLaunchroid)
[![Android](https://img.shields.io/badge/Android-9.0%2B-green?style=flat-square&logo=android)](https://www.android.com/)

**Made with ❤️ by the LeviMC Community**

</div>