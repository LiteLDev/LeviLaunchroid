# Mod API

## 作用

Mod API 提供 LeviLauncher native mod 使用的 `MyMod` 生命周期写法。新 mod
建议使用 C++ 模板和 `PL_REGISTER_MOD`。

## 头文件

```cpp
#include <pl/cpp/Mod.hpp>
#include <pl/cpp/mod/RegisterHelper.hpp>
```

强类型配置 helper 使用：

```cpp
#include <pl/cpp/Config.hpp>
```

## 注册模组

```cpp
#include "mod/MyMod.h"
#include <pl/cpp/mod/RegisterHelper.hpp>

PL_REGISTER_MOD(my_mod::MyMod, my_mod::MyMod::getInstance());
```

`MyMod` 应提供这些方法：

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

`unload()` 是可选的。模组持有需要在退出时释放的资源时再添加即可。

## 生命周期

| 方法 | 调用时机 |
| --- | --- |
| `load()` | 模组被加载时。 |
| `enable()` | 游戏即将启动时。 |
| `disable()` | 游戏正在结束时。 |
| `unload()` | 模组进行最终清理时。 |

每个方法成功时返回 `true`，失败时返回 `false`。

## NativeMod

在模组类里通过 `getSelf()` 访问当前模组对象：

```cpp
pl::mod::NativeMod &MyMod::getSelf() const {
  return *pl::mod::NativeMod::current();
}
```

常用方法：

| 方法 | 作用 |
| --- | --- |
| `getLogger()` | 当前模组专属 logger。 |
| `getId()` | 模组 id。 |
| `getName()` | 显示名称。 |
| `getAuthor()` | manifest 中的作者。 |
| `getVersion()` | manifest 中的版本。 |
| `getModDir()` | 模组包目录。 |
| `getDataDir()` | 模组数据文件目录。 |
| `getConfigDir()` | 模组配置文件目录。 |
| `getResourceDir()` | 模组资源文件目录。 |
| `getManifestPath()` | manifest 文件路径。 |
| `getLibraryPath()` | 模组库文件路径。 |
| `getJavaVM()` | 当前 Java VM 指针。 |

## 示例

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

## 配置

使用 `pl::config::ConfigFile<T>` 可以管理强类型 JSON 配置、自动更新默认布局，
并生成启动器可编辑的 schema。详情见 [Config API 参考](/zh-CN/api/config)。

## 模组菜单分组

native mod 可以向游戏内模组菜单注册多个模块。LeviLauncher 使用 `mod_id`
给外部模块分组，因此同一个 manifest 模组注册的模块应该复用同一个所属
模组 id。非 lifecycle 接入里 `mod_id` 为空的模块仍然可以加载，但会显示在
外部模组的未分组区域。

如果你的模组使用 `PL_REGISTER_MOD` 生命周期，优先使用 C++ helper：

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

helper 在 lifecycle mod 中会自动使用当前 manifest 模组 id。如果你在
constructor 或其他 `dlsym` 风格路径里注册模块，应显式调用
`.modId("your_manifest_id")` 指定所属模组。

项目里直接 include 启动器提供的头文件即可，不要在自己的项目里重新定义这些
Mod Menu 结构体。原始 C API 仍然可用：

```cpp
#include <pl/c/PreloaderModMenu.h>
```

通常在 `enable()` 中，或在 preloader 接口已经可用后注册模块：

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
  // 在这里应用模块启用状态；如果需要，也可以保存自己的配置。
}

void onModuleConfigChanged(const char *module_id,
                           const char *key,
                           const char *value) {
  if (std::string(module_id) != kModuleId || std::string(key) != "refreshRate")
    return;

  g_refreshRate = value ? std::stoi(value) : 20;
  // 在这里应用新的配置值，并按需写入自己的配置文件。
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

字段说明：

| 字段 | 说明 |
| --- | --- |
| `module_id` | 必须全局唯一。建议使用基于模组 id 的稳定前缀，例如 `example_mod.speed_meter`。 |
| `display_name` / `description` | 显示在游戏内模组菜单中。preloader 会在注册时复制这些字符串。 |
| `mod_id` | 使用原始 C API 时设置为 `getSelf().getId().c_str()` 或 manifest 中的准确模组 id。lifecycle mod 传空值会自动使用当前所属模组；传入不匹配的 owner 会被拒绝。 |
| `default_enabled` | 菜单显示的初始启用状态。如果你自己保存启用状态，应在注册前先加载。 |
| `on_toggle` | 在模块状态变化的调用线程上执行，应快速返回，并在这里应用运行时状态。 |
| `configs` / `config_count` | 可选配置项。没有配置时传 `nullptr` 和 `0`。 |
| `on_config_changed` | 在配置变化的调用线程上执行。需要自行解析字符串值、应用并按需持久化。 |
| `hide_in_hud_editor` | 不希望出现在 HUD 编辑器里的模块设为 `true`。 |

`RegisterModule()` 返回前会复制模块字符串和配置项，因此注册调用期间有效的
临时 `std::string`、局部 config 数组都是安全的。回调函数指针不会被复制成
owned code，必须在模块注销或所属模组卸载前一直有效。

如果一个模组要注册多个菜单模块，为每个模块构造一个 `PLModMenu_ModuleInfo`，
并使用相同的 `mod_id`。LeviLauncher 会自动把它们分到同一组。lifecycle mod
成功 unload 后会自动清理已注册模块；constructor 风格接入如果需要主动移除模块，
应调用 `UnregisterModule()`。

## 注意事项

- 模组数据放到 `getDataDir()`。
- 用户可编辑配置放到 `getConfigDir()`，也可以使用
  `pl::config::ConfigFile<T>` 管理强类型 JSON 配置。
- `load()` 尽量保持轻量，和游戏运行相关的工作优先放到 `enable()`。
- 清理资源时按相反顺序处理：先 `disable()`，再 `unload()`。
