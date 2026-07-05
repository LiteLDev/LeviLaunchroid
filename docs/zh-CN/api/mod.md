# Mod API

## 作用

Mod API 是 native mod 的生命周期入口。它使用 `<pl/Mod.hpp>`、
`ll::mod::NativeMod::current()`，以及通过 `PL_REGISTER_MOD` 注册的长期存活 C++
对象。

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

  MyMod();

  [[nodiscard]] ll::mod::NativeMod &getSelf() const { return mSelf; }

  bool load();
  bool enable();
  bool disable();
  bool unload();

private:
  ll::mod::NativeMod &mSelf;
};

PL_REGISTER_MOD(MyMod, MyMod::instance())
```

```cpp
MyMod::MyMod() : mSelf(*ll::mod::NativeMod::current()) {}
```

`load()` 是必需的。`enable()`、`disable()`、`unload()` 是可选的；缺失时注册
helper 会按成功处理。

`PL_REGISTER_MOD` 导出未改名的 C linkage 符号 `PLGetModRegistration`，返回
C++ 生命周期 registration 表。

## 生命周期

| 方法 | 建议职责 |
| --- | --- |
| `load()` | 读取配置、创建目录、准备 mod 自有状态。 |
| `enable()` | 注册 hook、input callback、Mod Menu 模块。 |
| `disable()` | 撤销面向游戏的行为并注销运行期 UI。 |
| `unload()` | 在 disable 后释放剩余 C++ 状态。 |

每个方法成功返回 `true`，失败返回 `false`。

## NativeMod

`ll::mod::NativeMod` 提供 manifest 元数据、路径、Java VM、生命周期状态和 mod
专属 logger。

```cpp
bool MyMod::load() {
  auto &self = getSelf();
  std::filesystem::create_directories(self.getConfigDir());
  self.getLogger().info("Loading {}", self.getName());
  return true;
}
```

常用成员：

| 成员 | 用途 |
| --- | --- |
| `getJavaVM()` | 当前 `JavaVM *`。 |
| `getLogger()` | 当前 mod 的 `pl::log::Logger`。 |
| `getId()` | 稳定运行期 mod id。 |
| `getName()` | manifest 中的显示名。 |
| `getModDir()` | mod 包根目录。 |
| `getDataDir()` | `<mod root>/data`。 |
| `getConfigDir()` | `<mod root>/config`。 |
| `getResourceDir()` | `<mod root>/resources`。 |
| `getState()` | 当前 native mod 生命周期状态。 |

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

bool MyMod::enable() {
  return pl::modmenu::ModuleBuilder(ModuleId, "Speed Meter")
      .modId(getSelf().getId())
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
- 用户可编辑配置应放在 `getSelf().getConfigDir()` 下。
