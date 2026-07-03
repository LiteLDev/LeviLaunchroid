# Mod API

## Purpose

The Mod API provides the `MyMod` lifecycle style used by LeviLauncher native
mods. New mods should use the C++ template and `PL_REGISTER_MOD`.

## Headers

```cpp
#include <pl/cpp/Mod.hpp>
#include <pl/cpp/mod/RegisterHelper.hpp>
```

Use the typed config helpers from:

```cpp
#include <pl/cpp/Config.hpp>
```

## Register a Mod

```cpp
#include "mod/MyMod.h"
#include <pl/cpp/mod/RegisterHelper.hpp>

PL_REGISTER_MOD(my_mod::MyMod, my_mod::MyMod::getInstance());
```

`MyMod` should provide these methods:

```cpp
class MyMod {
public:
  static MyMod &getInstance();

  bool load();
  bool enable();
  bool disable();
  bool unload();
};
```

`unload()` is optional. Add it when the mod owns resources that should be
released during shutdown.

## Lifecycle

| Method | When it runs |
| --- | --- |
| `load()` | The mod is loaded. |
| `enable()` | The game is about to start. |
| `disable()` | The game is closing. |
| `unload()` | The mod is doing final cleanup. |

Each method should return `true` when it succeeds and `false` when it fails.

## NativeMod

Use `getSelf()` in your mod class to access the current mod object:

```cpp
pl::mod::NativeMod &MyMod::getSelf() const {
  return *pl::mod::NativeMod::current();
}
```

Common methods:

| Method | Purpose |
| --- | --- |
| `getLogger()` | Logger dedicated to this mod. |
| `getId()` | Mod id. |
| `getName()` | Display name. |
| `getAuthor()` | Author from manifest. |
| `getVersion()` | Version from manifest. |
| `getModDir()` | Mod package directory. |
| `getDataDir()` | Directory for mod data files. |
| `getConfigDir()` | Directory for mod configuration files. |
| `getResourceDir()` | Directory for bundled resource files. |
| `getManifestPath()` | Manifest file path. |
| `getLibraryPath()` | Mod library path. |
| `getJavaVM()` | Current Java VM pointer. |

## Example

```cpp
bool MyMod::load() {
  auto &self = getSelf();
  self.getLogger().info("Loading {}", self.getName());

  std::filesystem::create_directories(self.getDataDir());
  std::filesystem::create_directories(self.getConfigDir());
  return true;
}

bool MyMod::enable() {
  getSelf().getLogger().info("Enabled");
  return true;
}

bool MyMod::disable() {
  getSelf().getLogger().info("Disabled");
  return true;
}

bool MyMod::unload() {
  getSelf().getLogger().info("Unloaded");
  return true;
}
```

## Config

Use `pl::config::ConfigFile<T>` for typed JSON config files, automatic default
layout updates, and launcher-editable schema generation. See the
[Config API Reference](/api/config).

## Mod Menu Grouping

Native mods can expose multiple in-game Mod Menu modules. LeviLauncher groups
external modules by `mod_id`, so every module from the same manifest mod should
use the same owner id. Modules with an empty `mod_id` still load outside the
managed lifecycle path, but they are shown in the ungrouped external-mod section.

Prefer the C++ helper when your mod uses `PL_REGISTER_MOD`:

```cpp
#include <pl/cpp/ModMenu.hpp>
#include <string>

namespace {
constexpr const char *kModuleId = "example_mod.speed_meter";

bool g_speedMeterEnabled = false;
int g_refreshRate = 20;

void onModuleToggle(const char *module_id, bool enabled) {
  if (std::string(module_id) != kModuleId)
    return;

  g_speedMeterEnabled = enabled;
}

void onModuleConfigChanged(const char *module_id,
                           const char *key,
                           const char *value) {
  if (std::string(module_id) != kModuleId || std::string(key) != "refreshRate")
    return;

  g_refreshRate = value ? std::stoi(value) : 20;
}
} // namespace

bool MyMod::enable() {
  return pl::modmenu::ModuleBuilder(kModuleId, "Speed Meter")
      .description("Shows a small movement speed overlay.")
      .defaultEnabled(g_speedMeterEnabled)
      .onToggle(onModuleToggle)
      .config("refreshRate", "Refresh Rate", PL_CONFIG_SLIDER_INT, "20", "1",
              "60")
      .onConfigChanged(onModuleConfigChanged)
      .registerModule();
}
```

When the helper is called from a lifecycle mod, it automatically uses the
current manifest mod id. If you register from a constructor or another
`dlsym`-based path, set the owner explicitly with `.modId("your_manifest_id")`.

Use the launcher header directly instead of redefining the Mod Menu structs in
your project. The raw C API is still available:

```cpp
#include <pl/c/PreloaderModMenu.h>
```

Register modules during `enable()` or after the preloader interface is
available:

```cpp
#include <pl/c/PreloaderModMenu.h>
#include <array>
#include <string>

namespace {
constexpr const char *kModuleId = "example_mod.speed_meter";

bool g_speedMeterEnabled = false;
int g_refreshRate = 20;

const std::array<PLModMenu_ConfigEntry, 1> kSpeedMeterConfigs{{
    {
        .key = "refreshRate",
        .display_name = "Refresh Rate",
        .type = PL_CONFIG_SLIDER_INT,
        .default_value = "20",
        .min_value = "1",
        .max_value = "60",
        .depends_on = nullptr,
    },
}};

void onModuleToggle(const char *module_id, bool enabled) {
  if (std::string(module_id) != kModuleId)
    return;

  g_speedMeterEnabled = enabled;
  // Apply your module state here, then persist your config if needed.
}

void onModuleConfigChanged(const char *module_id,
                           const char *key,
                           const char *value) {
  if (std::string(module_id) != kModuleId || std::string(key) != "refreshRate")
    return;

  g_refreshRate = value ? std::stoi(value) : 20;
  // Apply the new config and save it to your mod config file if needed.
}
} // namespace

bool MyMod::enable() {
  auto *menu = GetPreloaderModMenu();
  if (!menu)
    return true;

  auto &self = getSelf();
  PLModMenu_ModuleInfo info{
      .module_id = kModuleId,
      .display_name = "Speed Meter",
      .description = "Shows a small movement speed overlay.",
      .mod_id = self.getId().c_str(),
      .default_enabled = g_speedMeterEnabled,
      .on_toggle = onModuleToggle,
      .config_count = static_cast<int>(kSpeedMeterConfigs.size()),
      .configs = kSpeedMeterConfigs.data(),
      .on_config_changed = onModuleConfigChanged,
      .hide_in_hud_editor = false,
  };
  menu->RegisterModule(&info);
  return true;
}
```

## Custom Binding Buttons

After registering a module, a mod can register one or more floating buttons.
Buttons follow the enabled state of their owning module and reuse HUD editor
position, size, opacity, and lock controls.

```cpp
constexpr const char *kQuickDropButton = "example_mod.speed_meter.quick_drop";

void onButtonEvent(const char *button_id, PLModMenu_ButtonEvent event,
                   float value) {
  if (event == PL_BUTTON_EVENT_STATE_CHANGED) {
    const bool active = value > 0.5f;
    // Apply toggle state.
  }
}

bool MyMod::enable() {
  pl::modmenu::ModuleBuilder(kModuleId, "Speed Meter")
      .defaultEnabled(true)
      .registerModule();

  return pl::modmenu::ButtonBuilder(kQuickDropButton, "Quick Drop")
      .moduleId(kModuleId)
      .label("Q")
      .androidKeyCode(45) // Android KEYCODE_Q
      .behavior(PL_BUTTON_CLICK)
      .onEvent(onButtonEvent)
      .registerButton();
}
```

Buttons use the default `PL_BUTTON_STYLE_KEYCAP` preset, so labels are rendered
inside the same keycap surface as the built-in overlay buttons. Short labels
like `Q` or `H` stay square; multi-letter labels like `Take` or `Drop` are
automatically widened. The HUD editor size slider controls the base button
size, and a mod can tune the button shape with `.sizeScale(width, height)`:

```cpp
pl::modmenu::ButtonBuilder("example_mod.speed_meter.take", "Take")
    .moduleId(kModuleId)
    .label("Take")
    .behavior(PL_BUTTON_CLICK)
    .sizeScale(2.0f, 1.0f)
    .onEvent(onButtonEvent)
    .registerButton();
```

Use the builder style helpers only when the button needs a different accent;
style and size can be combined:

```cpp
pl::modmenu::ButtonBuilder("example_mod.speed_meter.toggle", "Toggle")
    .moduleId(kModuleId)
    .label("T")
    .behavior(PL_BUTTON_TOGGLE)
    .stylePreset(PL_BUTTON_STYLE_ACCENT)
    .styleColors(0xCC24282CU, 0xFF4AE0A0U, 0x994AE0A0U)
    .textColor(0xFFFFFFFFU)
    .activeTextColor(0xFF000000U)
    .sizeScale(1.4f, 1.0f)
    .onEvent(onButtonEvent)
    .registerButton();
```

Button behaviors:

| Behavior | Notes |
| --- | --- |
| `PL_BUTTON_CLICK` | Taps trigger the optional `android_key_code` and `PL_BUTTON_EVENT_CLICK`. |
| `PL_BUTTON_HOLD` | Press triggers `PL_BUTTON_EVENT_DOWN`; release or hide triggers `PL_BUTTON_EVENT_UP`. |
| `PL_BUTTON_TOGGLE` | Each tap toggles state and sends `PL_BUTTON_EVENT_STATE_CHANGED` with `1.0` or `0.0`. |

The raw C API uses `PLModMenu_ButtonInfo` and `RegisterButton()`. To provide
only colors, pass a `PLModMenu_ButtonStyle` to `RegisterButtonWithStyle()`. To
also specify shape ratios, pass a `PLModMenu_ButtonStyleV2` to
`RegisterButtonWithStyleV2()`. Color fields use `0xAARRGGBB`; leave a color as
`0` to use the preset default. Leave `width_scale` / `height_scale` as `0` to
use defaults; width then auto-expands from the label length. `button_id` must
be globally unique, and `module_id` must point to the owning menu module.
`label` is the text shown inside the keycap and supports multiple letters.

Field notes:

| Field | Notes |
| --- | --- |
| `module_id` | Must be globally unique. Use a stable prefix based on your mod id, such as `example_mod.speed_meter`. |
| `display_name` / `description` | Shown in the in-game Mod Menu. The preloader copies these strings during registration. |
| `mod_id` | Set this to `getSelf().getId().c_str()` or the exact manifest mod id when using the raw API. Lifecycle mods with an empty `mod_id` are assigned the current owner automatically; mismatched owner ids are rejected. |
| `default_enabled` | Initial state shown by the menu. Load your saved state before registering if you persist it yourself. |
| `on_toggle` | Called on the thread that changes the module state. Keep it fast and apply runtime state here. |
| `configs` / `config_count` | Optional config entries. Pass `nullptr` and `0` when the module has no config. |
| `on_config_changed` | Called on the thread that changes a config value. Parse the string value, apply it, and persist it if needed. |
| `hide_in_hud_editor` | Set `true` for modules that should not appear in the HUD editor. |

`RegisterModule()` copies module strings and config entries before it returns,
so temporary `std::string` values and local config arrays are safe for the
registration call itself. Callback function pointers are not copied into owned
code; they must remain valid until the module is unregistered or the owning mod
is unloaded.

If you register many modules, build one `PLModMenu_ModuleInfo` per module and
reuse the same `mod_id`. LeviLauncher will group them together automatically.
Lifecycle mods are cleaned up on successful unload; constructor-style mods
should call `UnregisterModule()` if they need to remove modules manually.

## Notes

- Store mod data in `getDataDir()`.
- Store user-editable configuration in `getConfigDir()`, or use
  `pl::config::ConfigFile<T>` for typed JSON config.
- Keep `load()` lightweight and move game-facing work to `enable()` when possible.
- Clean up resources in the reverse order: `disable()` first, then `unload()`.
