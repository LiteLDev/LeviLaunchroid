# Mod API

## Purpose

The Mod API is the lifecycle entry point for native mods. It uses
`<pl/Mod.hpp>`, lifecycle methods that receive `pl::mod::ModContext &`, and a
long-lived C++ instance registered with `PL_REGISTER_MOD`.

## Header

```cpp
#include <pl/Mod.hpp>
```

## Registration

```cpp
#include <pl/Mod.hpp>

class MyMod {
public:
  static MyMod &instance();

  bool load(pl::mod::ModContext &context);
  bool enable(pl::mod::ModContext &context);
  bool disable(pl::mod::ModContext &context);
  bool unload(pl::mod::ModContext &context);
};

PL_REGISTER_MOD(MyMod, MyMod::instance())
```

`load()` is required. `enable()`, `disable()`, and `unload()` are optional; the
registration helper treats missing optional phases as success.

`PL_REGISTER_MOD` exports the unmangled C-linkage symbol
`PLGetModRegistration`, which returns the C++ lifecycle registration table.

## Lifecycle

| Method | Recommended work |
| --- | --- |
| `load(context)` | Read config, create directories, prepare mod-owned state. |
| `enable(context)` | Register hooks, input callbacks, and Mod Menu modules. |
| `disable(context)` | Undo game-facing work and unregister runtime UI. |
| `unload(context)` | Release remaining C++ state after disable. |

Each method returns `true` on success and `false` on failure.

## ModContext

`pl::mod::ModContext` contains resolved manifest metadata, paths, the Java VM,
and a mod-scoped logger.

```cpp
bool MyMod::load(pl::mod::ModContext &context) {
  std::filesystem::create_directories(context.configDir());
  context.logger().info("Loading {}", context.name());
  return true;
}
```

Common members:

| Member | Purpose |
| --- | --- |
| `javaVm()` | Current `JavaVM *`. |
| `info()` | Full `pl::mod::ModInfo`. |
| `logger()` | `pl::log::Logger` for this mod. |
| `id()` | Stable runtime mod id. |
| `name()` | Display name from the manifest. |
| `modRootPath()` | Root directory of the mod package. |
| `dataDir()` | `<mod root>/data`. |
| `configDir()` | `<mod root>/config`. |
| `resourceDir()` | `<mod root>/resources`. |

## Mod Menu Example

```cpp
#include <pl/Mod.hpp>
#include <pl/ModMenu.hpp>

namespace {
constexpr const char *ModuleId = "example.speed_meter";

void onToggle(std::string_view moduleId, bool enabled) {
  (void)moduleId;
  (void)enabled;
}
} // namespace

bool MyMod::enable(pl::mod::ModContext &context) {
  return pl::modmenu::ModuleBuilder(ModuleId, "Speed Meter")
      .modId(context.id())
      .description("Shows a small movement speed overlay.")
      .defaultEnabled(true)
      .onToggle(onToggle)
      .config("refreshRate", "Refresh Rate",
              pl::modmenu::ConfigType::SliderInt, "20", "1", "60")
      .registerModule();
}
```

Use `pl::modmenu::ButtonBuilder` for floating buttons and
`pl::modmenu::unregisterModule()` / `unregisterButton()` during `disable()` when
the mod owns temporary UI.

## Notes

- Keep the registered instance alive for the process lifetime.
- Do not throw across lifecycle boundaries; catch failures and return `false`.
- Store user-editable config under `context.configDir()`.
