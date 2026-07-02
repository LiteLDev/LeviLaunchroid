# Full C++ Native Mod Development

This page describes the recommended C++ path for LeviLaunchroid native mods.
Use `examples/full-cpp-mod` as the reference implementation: it is a complete
preloader `cpp_lifecycle` mod with registration, typed config, generated
schema, runtime persistence, Mod Menu integration, and `.levipack` packaging.

## Start From The Example

The example lives at:

```text
examples/full-cpp-mod/
```

It contains:

| File | Purpose |
| --- | --- |
| `manifest.json` | Native mod metadata consumed by LeviLaunchroid. |
| `src/ExampleConfig.hpp` | Shared C++ config type and schema metadata. |
| `src/FullCppMod.cpp` | Runtime lifecycle mod implementation. |
| `src/GenerateConfig.cpp` | Host-side default config/schema generator. |
| `CMakeLists.txt` | Builds both the host generator and Android shared library. |
| `build.ps1` | Builds, generates config files, and packages the mod. |

Build it from the repository root:

```powershell
.\examples\full-cpp-mod\build.ps1 -Clean
```

The output is:

```text
examples\full-cpp-mod\dist\arm64-v8a\full-cpp-mod\
examples\full-cpp-mod\dist\arm64-v8a\full-cpp-mod.levipack
```

Import the `.levipack`, or copy the `full-cpp-mod` directory as a native mod
directory.

## Package Layout

A native mod directory should look like this:

```text
full-cpp-mod/
├── manifest.json
├── libfull_cpp_mod.so
└── config/
    ├── config.json
    └── config.schema.json
```

The directory name is the runtime mod id. For lifecycle mods this id is also
used as the default Mod Menu owner group. Keep it stable once users have
installed the mod.

## manifest.json

```json
{
  "type": "preload-native",
  "name": "Full C++ Lifecycle Mod Example",
  "author": "LiteLDev",
  "version": "1.0.0",
  "entry": "libfull_cpp_mod.so",
  "minecraft_versions": []
}
```

| Field | Notes |
| --- | --- |
| `type` | Must be `preload-native`. |
| `entry` | Relative path to the Android `.so` inside the mod directory. |
| `name` | Display name shown by the launcher. |
| `author` | Author text shown by the launcher. |
| `version` | Mod version. |
| `icon` | Optional relative path to an icon. |
| `minecraft_versions` | Exact versions and `*` prefix wildcards are supported. Missing or empty means all versions. |

## C++ Lifecycle Shape

Use `PL_REGISTER_MOD` with a long-lived C++ object:

```cpp
#include <pl/cpp/mod/RegisterHelper.hpp>

class FullCppMod {
public:
  static FullCppMod &instance();

  bool load();
  bool enable();
  bool disable();
  bool unload();
};

PL_REGISTER_MOD(FullCppMod, FullCppMod::instance())
```

Lifecycle responsibilities:

| Method | Recommended work |
| --- | --- |
| `load()` | Load and normalize config. Create files needed before game startup. |
| `enable()` | Register Mod Menu modules and apply runtime state. |
| `disable()` | Stop runtime behavior and unregister menu modules if needed. |
| `unload()` | Release owned C++ state after `disable()`. |

Use the current mod object only while a lifecycle callback is active:

```cpp
const auto self = pl::mod::NativeMod::current();
if (!self) {
  return false;
}

self->getLogger().info("Loaded {}", self->getName());
```

Useful paths are available from `self`: `getModDir()`, `getDataDir()`,
`getConfigDir()`, `getResourceDir()`, `getManifestPath()`, and
`getLibraryPath()`.

## Typed Config

Define config as a simple aggregate with public fields and default member
initializers:

```cpp
enum class DisplayMode {
  Compact,
  Detailed,
  Debug,
};

struct ExampleConfig {
  int version = 1;
  bool moduleEnabled = true;
  bool showOverlay = true;
  int opacity = 80;
  double scale = 1.0;
  DisplayMode mode = DisplayMode::Compact;
  std::string accentColor = "#4AE0A0";
};
```

Then specialize `pl::config::Schema<T>` for launcher UI metadata:

```cpp
template <> struct pl::config::Schema<fullcppmod::ExampleConfig> {
  static constexpr std::string_view title = "Full C++ Lifecycle Mod Example";

  static constexpr FieldSchema field(std::string_view name) {
    if (name == "opacity") {
      return {"Opacity", "Overlay opacity percentage.", 0, 100, false};
    }
    return {};
  }
};
```

At runtime, keep a `pl::config::ConfigFile<ExampleConfig>` in mod-owned state
instead of constructing a throwaway object for every callback. This keeps the
config path, schema path, defaults, and current value together:

```cpp
std::optional<pl::config::ConfigFile<ExampleConfig>> configFile;

bool FullCppMod::load() {
  configFile.emplace(ExampleConfig{});
  if (!configFile->load()) {
    return false;
  }

  normalizeConfig(configFile->value());
  return configFile->save();
}
```

Use the host generator pattern from `GenerateConfig.cpp` with
`PL_CONFIG_NO_RUNTIME` to create `config.json` and `config.schema.json` before
the mod is imported. That lets the launcher show editable config even before
the native library is loaded for the first time.

## Mod Menu And Config Persistence

For lifecycle mods, prefer `pl::modmenu::ModuleBuilder`. It automatically uses
the current native mod id as the owner group when called from `enable()`:

```cpp
return pl::modmenu::ModuleBuilder("full_cpp_mod.hud", "Full C++ Config Demo")
    .description("Pure C++ lifecycle module with persistent typed config.")
    .defaultEnabled(config.moduleEnabled)
    .onToggle(onModuleToggle)
    .config("showOverlay", "Show Overlay", PL_CONFIG_TOGGLE, "true")
    .config("opacity", "Opacity", PL_CONFIG_SLIDER_INT, "80", "0", "100")
    .config("scale", "Scale", PL_CONFIG_SLIDER_FLOAT, "1.0", "0.5", "2.0")
    .config("mode", "Display Mode", PL_CONFIG_RADIO, "0",
            "Compact,Detailed,Debug")
    .config("accentColor", "Accent Color", PL_CONFIG_COLOR, "#4AE0A0")
    .onConfigChanged(onConfigChanged)
    .registerModule();
```

Menu callbacks receive strings. Parse them defensively, clamp numeric values to
the same ranges as the menu/schema, then call `save()`:

```cpp
void FullCppMod::handleConfigChanged(const char *moduleId,
                                     const char *key,
                                     const char *value) {
  if (!matchesModule(moduleId) || !key || !configFile) {
    return;
  }

  auto &config = configFile->value();
  if (std::string_view(key) == "opacity") {
    config.opacity = clampOpacity(value);
  }

  configFile->save();
}
```

Keep callback functions static or otherwise valid until the module is
unregistered or the mod is unloaded. `RegisterModule()` copies module strings
and config entries, but it does not own your callback code.

## Build And Package

`build.ps1` intentionally has two CMake passes. This project only supports
`arm64-v8a`, so the example always builds that ABI.

1. Host build: compile `full_cpp_mod_config_gen`.
2. Android build: compile `libfull_cpp_mod.so` for `arm64-v8a`.
3. Staging: copy `manifest.json`, `.so`, `config.json`, and
   `config.schema.json` into `dist/<Abi>/full-cpp-mod/`.
4. Packaging: zip the staged files as `full-cpp-mod.levipack`.

Useful options:

```powershell
.\examples\full-cpp-mod\build.ps1
.\examples\full-cpp-mod\build.ps1 -Ndk <path-to-android-ndk>
.\examples\full-cpp-mod\build.ps1 -PreloaderRoot <path-to-preloader-android>
.\examples\full-cpp-mod\build.ps1 -NoLinkPreloader
```

If `-Ndk` is omitted, the script resolves the NDK from `ANDROID_NDK_HOME`,
`ANDROID_NDK_ROOT`, `ANDROID_HOME`, or `ANDROID_SDK_ROOT`.

Use `-NoLinkPreloader` when you want the example `.so` to leave preloader
symbols for runtime resolution instead of linking a local `libpreloader.so`.

## Checklist

- Keep the mod directory name stable; it is the runtime mod id.
- Build only `arm64-v8a`; other Android ABIs are not supported by this project.
- Use `PL_REGISTER_MOD` for C++ lifecycle mods.
- Load config before registering menu modules.
- Use menu default values from the loaded config.
- Persist every menu change that should survive restart.
- Validate and clamp callback values; menu values are strings.
- Keep callbacks static or tied to a long-lived singleton.
- Generate `config.json` and `config.schema.json` during packaging.
- Verify the `.levipack` root contains `manifest.json`, `.so`, and `config/`.

Continue with the [Mod API Reference](/api/mod) and
[Config API Reference](/api/config) for lower-level details.
