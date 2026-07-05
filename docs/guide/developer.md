# Native Mod Quick Start

This page describes the supported developer path for LeviLaunchroid native
mods. The public SDK is published in
[LiteLDev/preloader-android](https://github.com/LiteLDev/preloader-android).

For standalone third-party mods, start from the
[LeviLauncher Android mod template](https://github.com/QYCottage/levilauncher-android-mod-template).

Use `examples/full-cpp-mod` as the reference implementation. It includes
lifecycle registration, typed config, Mod Menu integration, Android packaging,
and `.levipack` output.

## Build the Example

From the repository root:

```powershell
.\examples\full-cpp-mod\build.ps1 -Clean
```

This built-in example uses the repository's internal build wiring. Standalone
mod projects should import the SDK as an external dependency.

Output:

```text
examples\full-cpp-mod\dist\arm64-v8a\full-cpp-mod\
examples\full-cpp-mod\dist\arm64-v8a\full-cpp-mod.levipack
```

Import the `.levipack`, or copy the unpacked mod directory into the launcher
native mod location.

## Package Layout

```text
full-cpp-mod/
├── manifest.json
├── libfull_cpp_mod.so
└── config/
    ├── config.json
    └── config.schema.json
```

The directory name is the runtime mod id. Keep it stable after release because
it is used for paths, Mod Menu ownership, and persisted user state.

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

## Lifecycle Shape

```cpp
#include <pl/Mod.hpp>

class FullCppMod {
public:
  static FullCppMod &instance();

  bool load(pl::mod::ModContext &context);
  bool enable(pl::mod::ModContext &context);
  bool disable(pl::mod::ModContext &context);
  bool unload(pl::mod::ModContext &context);
};

PL_REGISTER_MOD(FullCppMod, FullCppMod::instance())
```

`load()` is required. The other lifecycle methods are optional and default to
success when absent.

Use `pl::mod::ModContext` instead of global current-mod state:

```cpp
bool FullCppMod::load(pl::mod::ModContext &context) {
  std::filesystem::create_directories(context.configDir());
  context.logger().info("Loaded {}", context.name());
  return true;
}
```

## SDK Dependency

In a standalone mod project, import `preloader-android` the same way you import
other third-party CMake dependencies. For example, with `FetchContent`:

```cmake
include(FetchContent)

FetchContent_Declare(
    preloader_android
    GIT_REPOSITORY https://github.com/LiteLDev/preloader-android.git
    GIT_TAG 0.2.0)
FetchContent_MakeAvailable(preloader_android)

target_link_libraries(my_mod PRIVATE preloader)
```

Pin `GIT_TAG` to a released tag or commit for reproducible builds. The current
SDK release is `0.2.0`.

If the SDK is vendored or added as a git submodule in your mod project, point
to that checkout instead:

```cmake
add_subdirectory(third_party/preloader-android)
target_link_libraries(my_mod PRIVATE preloader)
```

Common SDK headers:

```cpp
#include <pl/Mod.hpp>
#include <pl/ModMenu.hpp>
#include <pl/Input.hpp>
#include <pl/Config.hpp>
#include <pl/memory/Hook.hpp>
#include <pl/memory/Patch.hpp>
#include <pl/memory/Signature.hpp>
```

Do not include preloader `src` directories. They contain private runtime
implementation details.

## Typed Config

Keep config in mod-owned state and pass explicit paths from `ModContext`:

```cpp
struct ExampleConfig {
  int version = 1;
  bool showOverlay = true;
  int opacity = 80;
};

class FullCppMod {
private:
  std::optional<pl::config::ConfigFile<ExampleConfig>> mConfig;
};

bool FullCppMod::load(pl::mod::ModContext &context) {
  mConfig.emplace(ExampleConfig{}, context.configDir() / "config.json",
                  context.configDir() / "config.schema.json");
  return mConfig->load();
}
```

Use a host-side generator like `examples/full-cpp-mod/src/GenerateConfig.cpp`
to place `config.json` and `config.schema.json` into the package before import.

## Mod Menu

```cpp
bool FullCppMod::enable(pl::mod::ModContext &context) {
  const auto &config = mConfig->value();

  return pl::modmenu::ModuleBuilder("full_cpp_mod.hud",
                                    "Full C++ Config Demo")
      .modId(context.id())
      .description("Pure C++ lifecycle module with persistent typed config.")
      .defaultEnabled(config.showOverlay)
      .config("opacity", "Opacity", pl::modmenu::ConfigType::SliderInt,
              std::to_string(config.opacity), "0", "100")
      .registerModule();
}
```

Use `ButtonBuilder` for floating buttons, and unregister modules/buttons in
`disable()` when they are temporary.

## Build Options

```powershell
.\examples\full-cpp-mod\build.ps1
.\examples\full-cpp-mod\build.ps1 -Ndk <path-to-android-ndk>
.\examples\full-cpp-mod\build.ps1 -PreloaderRoot <path-to-preloader-android>
.\examples\full-cpp-mod\build.ps1 -NoLinkPreloader
```

`-NoLinkPreloader` leaves SDK symbols unresolved in the example `.so` so the
runtime preloader can resolve them when the mod is loaded.

## Checklist

- Build native mods for `arm64-v8a`.
- Include only the SDK `include` directory.
- Register one long-lived object with `PL_REGISTER_MOD`.
- Pass `pl::mod::ModContext &` through lifecycle methods.
- Load config before registering runtime UI.
- Store hook and patch handles in mod-owned state.
- Unregister temporary Mod Menu entries during `disable()`.
- Keep callbacks valid until they are unregistered or the mod unloads.

Continue with the [Mod API Reference](/api/mod) and
[Config API Reference](/api/config) for lower-level details.
