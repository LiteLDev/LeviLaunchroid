# Native Mod 快速开始

本页面描述 LeviLaunchroid native mod 的受支持开发路径。公开 SDK 位于
[LiteLDev/preloader-android](https://github.com/LiteLDev/preloader-android)。

独立第三方 mod 建议从
[LeviLauncher Android mod template](https://github.com/QYCottage/levilauncher-android-mod-template)
开始。

推荐以 `examples/full-cpp-mod` 作为参考实现。它包含生命周期注册、类型化
配置、Mod Menu 集成、Android 打包和 `.levipack` 输出。

## 构建示例

在仓库根目录运行：

```powershell
.\examples\full-cpp-mod\build.ps1 -Clean
```

这个内置示例使用仓库内部构建配置。独立 mod 工程请把 SDK 作为外部依赖引入。

输出：

```text
examples\full-cpp-mod\dist\arm64-v8a\full-cpp-mod\
examples\full-cpp-mod\dist\arm64-v8a\full-cpp-mod.levipack
```

可以导入 `.levipack`，也可以把解包后的 mod 目录复制到启动器 native mod 位置。

## 包结构

```text
full-cpp-mod/
├── manifest.json
├── libfull_cpp_mod.so
└── config/
    ├── config.json
    └── config.schema.json
```

目录名是运行期 mod id。发布后保持稳定，因为它会用于路径、Mod Menu 归属和用户持久化状态。

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

| 字段 | 说明 |
| --- | --- |
| `type` | 必须是 `preload-native`。 |
| `entry` | mod 目录内 Android `.so` 的相对路径。 |
| `name` | 启动器显示名。 |
| `author` | 作者信息。 |
| `version` | mod 版本。 |
| `icon` | 可选图标相对路径。 |
| `minecraft_versions` | 支持精确版本和 `*` 前缀通配；缺失或为空表示全部版本。 |

## 生命周期形态

```cpp
#include <pl/Mod.hpp>

class FullCppMod {
public:
  static FullCppMod &instance();

  FullCppMod();

  [[nodiscard]] ll::mod::NativeMod &getSelf() const { return mSelf; }

  bool load();
  bool enable();
  bool disable();
  bool unload();

private:
  ll::mod::NativeMod &mSelf;
};

PL_REGISTER_MOD(FullCppMod, FullCppMod::instance())
```

`load()` 是必需的。其它生命周期函数可选，缺失时默认成功。

在构造函数中保存 loader 提供的 native mod 对象：

```cpp
FullCppMod::FullCppMod() : mSelf(*ll::mod::NativeMod::current()) {}

bool FullCppMod::load() {
  auto &self = getSelf();
  std::filesystem::create_directories(self.getConfigDir());
  self.getLogger().info("Loaded {}", self.getName());
  return true;
}
```

## SDK 依赖

独立 mod 工程应像引入其它 CMake 第三方依赖一样引入 `preloader-android`。例如使用
`FetchContent`：

```cmake
include(FetchContent)

FetchContent_Declare(
    preloader_android
    GIT_REPOSITORY https://github.com/LiteLDev/preloader-android.git
    GIT_TAG main)
FetchContent_MakeAvailable(preloader_android)

target_link_libraries(my_mod PRIVATE preloader)
```

实际项目应把 `GIT_TAG` 固定到 release tag 或 commit，避免构建结果漂移。

如果 SDK 以 vendored 目录或 git submodule 放在 mod 工程里，就指向对应 checkout：

```cmake
add_subdirectory(third_party/preloader-android)
target_link_libraries(my_mod PRIVATE preloader)
```

常用 SDK 头文件：

```cpp
#include <pl/Mod.hpp>
#include <pl/ModMenu.hpp>
#include <pl/Input.hpp>
#include <pl/Config.hpp>
#include <pl/memory/Hook.hpp>
#include <pl/memory/Patch.hpp>
#include <pl/memory/Signature.hpp>
```

不要 include preloader 的 `src` 目录；那里是私有运行时代码。

## 类型化配置

配置应保存在 mod 自有状态中。生命周期调用期间，`ConfigFile` 默认会使用当前
native mod 的 `config/config.json` 和 `config/config.schema.json` 路径：

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

bool FullCppMod::load() {
  mConfig.emplace();
  return mConfig->load();
}
```

参考 `examples/full-cpp-mod/src/GenerateConfig.cpp` 的 host-side generator，在导入前
把 `config.json` 和 `config.schema.json` 放进包里。

## Mod Menu

```cpp
bool FullCppMod::enable() {
  const auto &config = mConfig->value();

  return pl::modmenu::ModuleBuilder("full_cpp_mod.hud",
                                    "Full C++ Config Demo")
      .modId(getSelf().getId())
      .description("Pure C++ lifecycle module with persistent typed config.")
      .defaultEnabled(config.showOverlay)
      .config("opacity", "Opacity", pl::modmenu::ConfigType::SliderInt,
              std::to_string(config.opacity), "0", "100")
      .registerModule();
}
```

浮动按钮使用 `ButtonBuilder`。如果模块或按钮是临时 UI，请在 `disable()` 中注销。

## 构建选项

```powershell
.\examples\full-cpp-mod\build.ps1
.\examples\full-cpp-mod\build.ps1 -Ndk <path-to-android-ndk>
.\examples\full-cpp-mod\build.ps1 -PreloaderRoot <path-to-preloader-android>
.\examples\full-cpp-mod\build.ps1 -NoLinkPreloader
```

`-NoLinkPreloader` 会让示例 `.so` 保留未解析的 SDK 符号，运行时由 preloader 解析。

## 检查清单

- native mod 目标 ABI 为 `arm64-v8a`。
- 只 include SDK `include` 目录。
- 使用 `PL_REGISTER_MOD` 注册一个长期存活对象。
- 从 `ll::mod::NativeMod::current()` 保存 `ll::mod::NativeMod &mSelf`。
- 注册运行期 UI 前先加载配置。
- hook 和 patch handle 放在 mod 自有状态中。
- `disable()` 中注销临时 Mod Menu 项。
- callback 在注销或 unload 前必须保持有效。

更底层的接口细节继续阅读 [Mod API 参考](/zh-CN/api/mod) 和
[Config API 参考](/zh-CN/api/config)。
