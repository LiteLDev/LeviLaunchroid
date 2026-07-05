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

每个 native mod library 在一个源文件中使用一次 `PL_REGISTER_MOD`。

## 生命周期

| 方法 | 建议职责 |
| --- | --- |
| `load()` | 读取配置、创建目录、准备 mod 自有状态。 |
| `enable()` | 注册 hook、input callback、Mod Menu 模块。 |
| `disable()` | 撤销面向游戏的行为并注销运行期 UI。 |
| `unload()` | 在 disable 后释放剩余 C++ 状态。 |

每个方法成功返回 `true`，失败返回 `false`。

## NativeMod

`ll::mod::NativeMod` 提供 manifest 元数据、包路径、Java VM、生命周期状态和 mod
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
| `getAuthor()` | manifest 中的作者。 |
| `getVersion()` | manifest 中的版本。 |
| `getEntryPath()` | 已解析的入口文件路径。 |
| `getEntryFileName()` | manifest 中的入口文件名。 |
| `getIconPath()` | 已解析的图标路径，未配置时为空路径。 |
| `getModDir()` | mod 包根目录。 |
| `getDataDir()` | `<mod root>/data`。 |
| `getConfigDir()` | `<mod root>/config`。 |
| `getResourceDir()` | `<mod root>/resources`。 |
| `getManifestPath()` | 已解析的 `manifest.json` 路径。 |
| `getLibraryPath()` | 已解析的 native library 路径。 |
| `getState()` | 当前 native mod 生命周期状态。 |
| `isLoaded()` / `isEnabled()` / `isDisabled()` | 常用状态判断。 |

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

## Mod Menu 覆盖层绘制

使用 `pl::modmenu::submitDrawCommands()` 替换某个模块当前的 HUD 覆盖层绘制
命令。文本命令可以使用 `registerFont()` 注册的字体；图片命令使用
`registerImage()` 注册的原始 RGBA 像素。

| API | 用途 |
| --- | --- |
| `registerFont(fontId, ttfData)` | 为文本命令注册 TrueType 字体。 |
| `registerImage(imageId, imageData, width, height)` | 为图片命令注册原始 RGBA 图片像素。 |
| `submitDrawCommands(moduleId, commands)` | 替换模块当前的覆盖层绘制命令列表。 |

`registerImage()` 要求 `imageData.size()` 必须等于 `width * height * 4`。
对 `DrawCommandType::Text`，请把 `DrawCommand::fontId` 设置为已注册的 font id。
对 `DrawCommandType::Image`，请把 `DrawCommand::imageId` 设置为已注册的 image id。

```cpp
std::vector<unsigned char> logoRgba = loadLogoPixels();
const int logoWidth = 64;
const int logoHeight = 64;

bool MyMod::enable() {
  if (!pl::modmenu::registerImage("example.logo", logoRgba,
                                  logoWidth, logoHeight)) {
    return false;
  }

  return pl::modmenu::ModuleBuilder(ModuleId, "Speed Meter")
      .modId(getSelf().getId())
      .defaultEnabled(true)
      .registerModule();
}

void submitOverlay() {
  const std::vector<pl::modmenu::DrawCommand> commands = {
      {
          .type = pl::modmenu::DrawCommandType::Image,
          .x = 12.0f,
          .y = 12.0f,
          .w = 32.0f,
          .h = 32.0f,
          .imageId = "example.logo",
      },
  };
  pl::modmenu::submitDrawCommands(ModuleId, commands);
}
```

## 注意

- 注册实例必须在进程生命周期内保持有效。
- 不要让异常跨越生命周期边界；捕获失败并返回 `false`。
- 用户可编辑配置应放在 `getSelf().getConfigDir()` 下。
