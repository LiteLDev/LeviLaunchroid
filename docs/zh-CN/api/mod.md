# Mod API

## 作用

Mod API 是 native mod 的生命周期入口。它使用 `<pl/Mod.hpp>`、接收
`pl::mod::ModContext &` 的生命周期函数，以及通过 `PL_REGISTER_MOD` 注册的长期存活
C++ 对象。

## 头文件

```cpp
#include <pl/Mod.hpp>
```

## 注册

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

`load()` 是必需的。`enable()`、`disable()`、`unload()` 是可选的；缺失时注册
helper 会按成功处理。

`PL_REGISTER_MOD` 导出未改名的 C linkage 符号 `PLGetModRegistration`，返回
C++ 生命周期 registration 表。

## 生命周期

| 方法 | 建议职责 |
| --- | --- |
| `load(context)` | 读取配置、创建目录、准备 mod 自有状态。 |
| `enable(context)` | 注册 hook、input callback、Mod Menu 模块。 |
| `disable(context)` | 撤销面向游戏的行为并注销运行期 UI。 |
| `unload(context)` | 在 disable 后释放剩余 C++ 状态。 |

每个方法成功返回 `true`，失败返回 `false`。

## ModContext

`pl::mod::ModContext` 包含 manifest 元数据、路径、Java VM 和 mod 专属 logger。

```cpp
bool MyMod::load(pl::mod::ModContext &context) {
  std::filesystem::create_directories(context.configDir());
  context.logger().info("Loading {}", context.name());
  return true;
}
```

常用成员：

| 成员 | 用途 |
| --- | --- |
| `javaVm()` | 当前 `JavaVM *`。 |
| `info()` | 完整 `pl::mod::ModInfo`。 |
| `logger()` | 当前 mod 的 `pl::log::Logger`。 |
| `id()` | 稳定运行期 mod id。 |
| `name()` | manifest 中的显示名。 |
| `modRootPath()` | mod 包根目录。 |
| `dataDir()` | `<mod root>/data`。 |
| `configDir()` | `<mod root>/config`。 |
| `resourceDir()` | `<mod root>/resources`。 |

## Mod Menu 示例

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

浮动按钮使用 `pl::modmenu::ButtonBuilder`。如果 UI 是 mod 临时注册的，请在
`disable()` 中调用 `unregisterModule()` / `unregisterButton()`。

## 注意

- 注册实例必须在进程生命周期内保持有效。
- 不要让异常跨越生命周期边界；捕获失败并返回 `false`。
- 用户可编辑配置应放在 `context.configDir()` 下。
