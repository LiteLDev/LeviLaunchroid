<div align="center">

![LeviLauncher Logo](https://media.discordapp.net/attachments/1425451887699689505/1426231628228726915/78095377.png?ex=68ea7936&is=68e927b6&hm=c396178117c324b35712605ea3f921d431b8097c71fe9b70200b77494c4edb88&=&format=webp&quality=lossless&width=275&height=275)

# LeviLauncher

![Banner](https://camo.githubusercontent.com/c8d82f38bfba3eb0d5fd232271a2e923820c4b3692b6f626b1e84d14e25a590a/68747470733a2f2f63617073756c652d72656e6465722e76657263656c2e6170702f6170693f747970653d576176696e6726636f6c6f723d74696d654772616469656e74266865696768743d33303026616e696d6174696f6e3d66616465496e2673656374696f6e3d68656164657226746578743d4c6576694d4326666f6e7453697a653d313230)

**A lightweight Android launcher for Minecraft: Bedrock Edition**

[![GitHub Release](https://img.shields.io/github/v/release/LiteLDev/LeviLaunchroid?style=flat-square&color=blue)](https://github.com/LiteLDev/LeviLaunchroid/releases)
[![License: LGPL v3](https://img.shields.io/badge/License-LGPLv3-blue.svg?style=flat-square)](https://www.gnu.org/licenses/lgpl-3.0)
[![Issues](https://img.shields.io/github/issues/LiteLDev/LeviLaunchroid?style=flat-square&color=red)](https://github.com/LiteLDev/LeviLaunchroid/issues)
[![Stars](https://img.shields.io/github/stars/LiteLDev/LeviLaunchroid?style=flat-square&color=yellow)](https://github.com/LiteLDev/LeviLaunchroid)
[![Android](https://img.shields.io/badge/Android-7.0%2B-green?style=flat-square&logo=android)](https://www.android.com/)

</div>

---

## Introduction

LeviLauncher is a lightweight, open-source Android launcher specifically designed for legitimate players of Minecraft: Bedrock Edition (MCBE). It provides a flexible and user-friendly alternative to the standard Google Play installation, allowing you to manage multiple game versions and extend functionality with external modules.

LeviLauncher enables you to import your official Minecraft APK and run it directly without requiring system installation. The launcher supports loading external native modules to enhance gameplay, provides robust multi-version management with complete isolation between installations, and includes built-in tools for managing resource packs and worlds. Whether you're looking to organize different game versions, test modifications, or optimize your gaming experience, LeviLauncher offers the flexibility you need.

### Key Features

- **APK Import & Installation-Free Launching** – Import your official Minecraft APK and run it directly without system installation
- **SO Module Loading** – Load external native SO modules to extend or enhance Minecraft features and performance
- **Multi-Version Management & Version Isolation** – Manage multiple Minecraft versions independently, with separated configurations and data
- **Resource Pack & World Management** – Import, export, and back up your resource packs and worlds with the built-in manager
- **Multi-Architecture Support** – Compatible with ARM64 (v8a) and ARM32 (v7a) device architectures

---

## System Requirements

Before installing LeviLauncher, ensure your device meets the following minimum specifications:

- **Operating System:** Android 7.0 (API 24) or higher
- **Device Architecture:** ARM64 (v8a) or ARM32 (v7a)
- **RAM:** Minimum 1 GB available RAM (2 GB or more recommended)
- **Storage:** At least 2 GB of available storage for Minecraft and game data
- **License Requirement:** You must own a licensed copy of Minecraft Bedrock Edition purchased from Google Play

> **Note:** For optimal performance and stability, we recommend Android 8.0 or higher with at least 3 GB of available RAM and 5 GB of free storage.

---

## Installation

### Prerequisites

Before proceeding with LeviLauncher installation, ensure that you have the official Minecraft Bedrock Edition app installed on your device from Google Play. This is required for LeviLauncher to function properly.

### Installation Steps

1. Visit the [Releases Page](https://github.com/LiteLDev/LeviLaunchroid/releases) and download the latest APK build for your device architecture
2. Open your device Settings and navigate to Security or Applications
3. Enable "Unknown Sources" or "Allow installation from unknown sources" to permit APK installation
4. Locate the downloaded APK file using your file manager and tap to install
5. Grant the necessary permissions when prompted during installation
6. Once installed, open LeviLauncher from your application drawer
7. Follow the in-app setup wizard to configure your game versions and settings

> **Important:** LeviLauncher requires a legitimate, licensed copy of Minecraft Bedrock Edition. Do not use this launcher with pirated or unauthorized versions of the game. Ensure your Minecraft license is valid and properly linked to your Microsoft account.

---

## Development Setup

If you want to build LeviLauncher from source or contribute to development, follow these steps to set up your development environment:

### Prerequisites

- Git installed on your system
- Android Studio (latest version recommended)
- Java Development Kit (JDK) 11 or higher
- Android SDK with API level 24 or higher

### Setup Instructions

1. Clone the LeviLauncher repository:
   ```bash
   git clone https://github.com/LiteLDev/LeviLaunchroid.git
   ```

2. Open the project directory in Android Studio

3. Allow Android Studio to download and install required dependencies and build tools

4. Wait for Gradle to complete the initial sync process

5. Connect your Android device or start an emulator (API 24+)

6. Click the "Run" button in Android Studio to build and deploy to your device

7. The app will launch automatically on successful build completion

> **Build Tip:** For faster builds during development, use `Build > Make Project` to compile incrementally instead of full rebuilds.

---

## Contribution Guidelines

We welcome contributions from the community to improve LeviLauncher. To ensure a high-quality codebase and smooth collaboration, please adhere to the following guidelines:

### Code Quality

Write clean, modular code with descriptive variable names and consistent formatting. Follow Kotlin and Java style guidelines established in the project. Ensure your code is readable and well-structured for future maintainers.

### Commit Structure

Use small, focused commits with clear and descriptive messages. Each commit should address a single feature or bug fix. Example: "Fixed memory leak in version manager" or "Added support for ARM32 architecture".

### Documentation

Add comments for complex logic and update relevant documentation in the repository. If you add new features, update the README and any related documentation files.

### Performance

Optimize all additions to maintain low latency and smooth performance. Test your changes thoroughly to ensure they don't introduce lag or performance regressions.

### Testing

Test all changes on multiple devices and Android versions to ensure compatibility and stability. Verify that your modifications work correctly on both ARM64 and ARM32 architectures when applicable.

### Pull Requests

Submit PRs with a detailed description of changes, including the problem solved or feature added. Reference any related issues and provide screenshots or videos if your changes affect the UI.

### Community Standards

Follow our Code of Conduct to maintain a respectful and inclusive environment. Be constructive in feedback, respect others' work, and communicate professionally with all contributors.

**Before Submitting:** Run a full build cycle and test on at least one device to minimize errors. We review all contributions promptly and appreciate your efforts to enhance LeviLauncher.

---

## Usage Guidelines

LeviLauncher is designed for legitimate players of Minecraft Bedrock Edition. Please respect the following guidelines and terms of use:

### Permitted Uses

- Modify LeviLauncher for personal gameplay and to test new features
- Create educational content (videos, tutorials, blog posts) showcasing LeviLauncher's capabilities
- Fork the repository for learning purposes or to create derivative projects, provided you comply with the GNU LGPL v3.0
- Share your modified versions with others as long as you comply with the LGPL license terms

### Prohibited Uses

- Do not distribute modified versions without sharing the source code, as required by the LGPL
- Do not claim LeviLauncher as your own without crediting the LeviMC team and its contributors
- Selling LeviLauncher or derivatives without adhering to the LGPL is strictly prohibited
- Do not distribute LeviLauncher as closed-source or under a non-LGPL license
- Do not use LeviLauncher to violate Mojang or Microsoft's user agreements

> **Disclaimer:** The authors and contributors of LeviLauncher are not responsible for bans, damages, or issues arising from the use of this software. Use it at your own risk and in accordance with Minecraft's terms of service.

For full legal details, see the LICENSE and NOTICE files in the repository.

---

## Credits & Acknowledgements

LeviLauncher would not be possible without the contributions and support of many talented individuals and organizations:

### Special Thanks To

- **LeviMC Organization** – For maintaining the LeviLauncher project and providing infrastructure support
- **Android Community** – For excellent documentation, libraries, and tools that made this launcher possible
- **UnpairedCore Library** – For providing essential utilities and components used in LeviLauncher. See [Kitsuri Studios - UnpairCore](https://github.com/Kitsuri-Studios/unpaircore)
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
[![License: LGPL v3](https://img.shields.io/badge/License-LGPLv3-blue.svg?style=flat-square)](https://www.gnu.org/licenses/lgpl-3.0)
[![Issues](https://img.shields.io/github/issues/LiteLDev/LeviLaunchroid?style=flat-square&color=red)](https://github.com/LiteLDev/LeviLaunchroid/issues)
[![Stars](https://img.shields.io/github/stars/LiteLDev/LeviLaunchroid?style=flat-square&color=yellow)](https://github.com/LiteLDev/LeviLaunchroid)
[![Android](https://img.shields.io/badge/Android-7.0%2B-green?style=flat-square&logo=android)](https://www.android.com/)

**Made with ❤️ by the LeviMC Community**

</div>