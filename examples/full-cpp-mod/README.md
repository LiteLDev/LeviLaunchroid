# Full C++ Lifecycle Mod Example

This directory is a complete C++ native mod example for LeviLaunchroid. It shows
the recommended `cpp_lifecycle` integration path without mixing C ABI entry
points, constructor-based registration, or ad hoc packaging scripts.

## What It Covers

- `PL_REGISTER_MOD` binding for a C++ lifecycle object
- `pl::config::ConfigFile<T>` for typed JSON config
- A host-side generator for default `config.json` and `config.schema.json`
- `pl::modmenu::ModuleBuilder` registration for Mod Menu modules
- Toggle/config callbacks that update and persist the same config file
- `build.ps1` packaging for the Android `.so` and `.levipack`

## Layout

```text
full-cpp-mod/
├── CMakeLists.txt
├── build.ps1
├── manifest.json
├── src/
│   ├── ExampleConfig.hpp
│   ├── FullCppMod.cpp
│   └── GenerateConfig.cpp
└── dist/
```

Key files:

| File | Purpose |
| --- | --- |
| `src/ExampleConfig.hpp` | Shared config definition used by both runtime and host generator. |
| `src/FullCppMod.cpp` | Lifecycle, Mod Menu registration, and runtime persistence logic. |
| `src/GenerateConfig.cpp` | Generates package config with `PL_CONFIG_NO_RUNTIME` enabled. |
| `manifest.json` | Uses `type: preload-native` and points `entry` to `libfull_cpp_mod.so`. |

## Build

Run from the repository root:

```powershell
.\examples\full-cpp-mod\build.ps1 -Clean
```

This project only supports `arm64-v8a`, so the script always builds that ABI.
You can override the NDK or preloader path:

```powershell
.\examples\full-cpp-mod\build.ps1 -Ndk <path-to-android-ndk>
.\examples\full-cpp-mod\build.ps1 -PreloaderRoot <path-to-preloader-android>
```

If `-Ndk` is omitted, the script resolves it from `ANDROID_NDK_HOME`,
`ANDROID_NDK_ROOT`, `ANDROID_HOME`, or `ANDROID_SDK_ROOT`.

Use `-NoLinkPreloader` if the example `.so` should leave preloader symbols for
runtime resolution instead of carrying a local `libpreloader.so` link
dependency:

```powershell
.\examples\full-cpp-mod\build.ps1 -NoLinkPreloader
```

## Output

Successful builds generate:

```text
examples\full-cpp-mod\dist\arm64-v8a\full-cpp-mod\
examples\full-cpp-mod\dist\arm64-v8a\full-cpp-mod.levipack
```

The package root should contain:

```text
manifest.json
libfull_cpp_mod.so
config\config.json
config\config.schema.json
```

The `full-cpp-mod` directory name is the runtime mod id and the default Mod
Menu owner group.

## Verify

```powershell
tar -tf .\examples\full-cpp-mod\dist\arm64-v8a\full-cpp-mod.levipack
```

Expected entries:

```text
config/config.json
config/config.schema.json
libfull_cpp_mod.so
manifest.json
```

After importing the mod into LeviLaunchroid, verify that:

- The launcher config page reads `config.json` and `config.schema.json`.
- The in-game Mod Menu shows `Full C++ Config Demo`.
- Changing a menu toggle or config entry updates `config/config.json`.
- Restarting uses the previously persisted config as the menu defaults.

See `docs/guide/developer.md` for the full development guide.
