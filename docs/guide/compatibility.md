# Compatibility and FAQ

## Supported Devices

LeviLauncher targets Android 9.0 or later on ARM64 devices. Devices with more
available RAM and storage will have a better experience, especially when
managing multiple versions or large worlds.

## Minecraft Requirement

LeviLauncher requires the official Minecraft Bedrock Edition app from Google
Play. It is designed for legitimate players and does not provide a Minecraft
license.

If Minecraft is not installed, installed from an unsupported source, or too old
for the current launcher behavior, the launcher may stop before starting the
game.

## Minecraft Versions

The app currently rejects Minecraft versions older than 1.21.80. Imported
versions should use version isolation when their data should remain separate
from the installed version.

## Storage and Migration

The launcher may need to migrate older data directories before continuing. Do
not delete the old directory until the migration succeeds and you have confirmed
that worlds, packs, and settings are available.

Backups are stored under the launcher-managed backup location. For important
worlds, keep another copy outside app storage.

## Native Modules

Native modules run native code in the game process. Install only modules from
sources you trust, and test them with an isolated version first.

## Common Questions

### Why does the launcher say Minecraft is missing?

Install or update the official Minecraft app from Google Play, then open
LeviLauncher again.

### Why can an imported version fail to launch?

Imported versions often need version isolation. Enable it in the instance
settings and try again.

### Where should developer API questions go?

Use the [Developer](/guide/developer) section. The Preloader API reference is
intentionally grouped there instead of the main user guide.
