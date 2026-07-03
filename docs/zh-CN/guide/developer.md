# 纯 C++ Native Mod 开发指南

本页说明 LeviLaunchroid native mod 推荐的 C++ 开发方式。请把
`examples/full-cpp-mod` 当作基准实现：它是完整的 preloader `cpp_lifecycle`
示例，覆盖模组注册、强类型配置、schema 生成、运行时持久化、Mod Menu 接入和
`.levipack` 打包。

## 从示例开始

示例目录：

```text
examples/full-cpp-mod/
```

目录内容：

| 文件 | 作用 |
| --- | --- |
| `manifest.json` | LeviLaunchroid 读取的 native mod 元数据。 |
| `src/ExampleConfig.hpp` | C++ 配置类型和 schema 元数据。 |
| `src/FullCppMod.cpp` | 运行时 lifecycle mod 实现。 |
| `src/GenerateConfig.cpp` | 主机端默认配置和 schema 生成器。 |
| `CMakeLists.txt` | 同时构建 host generator 和 Android shared library。 |
| `build.ps1` | 构建、生成配置文件并打包 mod。 |

在仓库根目录运行：

```powershell
.\examples\full-cpp-mod\build.ps1 -Clean
```

输出位置：

```text
examples\full-cpp-mod\dist\arm64-v8a\full-cpp-mod\
examples\full-cpp-mod\dist\arm64-v8a\full-cpp-mod.levipack
```

可以导入 `.levipack`，也可以把 `full-cpp-mod` 目录作为 native mod 目录复制到
目标位置。

## 包结构

native mod 目录应保持这种结构：

```text
full-cpp-mod/
├── manifest.json
├── libfull_cpp_mod.so
└── config/
    ├── config.json
    └── config.schema.json
```

目录名就是运行时 mod id。对于 lifecycle mod，它也会作为 Mod Menu 默认 owner
分组。用户安装后不要随意改目录名，否则配置、菜单分组和持久化状态都容易错位。

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
| `name` | 启动器展示的名称。 |
| `author` | 启动器展示的作者。 |
| `version` | mod 版本。 |
| `icon` | 可选图标相对路径。 |
| `minecraft_versions` | 支持精确版本和 `*` 前缀通配；缺失或为空表示兼容全部版本。 |

## C++ 生命周期形态

使用 `PL_REGISTER_MOD` 绑定一个长生命周期 C++ 对象：

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

生命周期建议分工：

| 方法 | 建议职责 |
| --- | --- |
| `load()` | 加载并规范化配置；创建游戏启动前就需要的文件。 |
| `enable()` | 注册 Mod Menu 模块，并应用运行时状态。 |
| `disable()` | 停止运行时行为，按需注销菜单模块。 |
| `unload()` | 在 `disable()` 之后释放 C++ 持有的状态。 |

只在 lifecycle callback 有效期间访问当前模组对象：

```cpp
const auto self = pl::mod::NativeMod::current();
if (!self) {
  return false;
}

self->getLogger().info("Loaded {}", self->getName());
```

常用路径都从 `self` 获取：`getModDir()`、`getDataDir()`、`getConfigDir()`、
`getResourceDir()`、`getManifestPath()`、`getLibraryPath()`。

## 强类型配置

配置类型应是简单 aggregate：公开字段、默认成员初始化，不要把业务逻辑塞进配置对象。

```cpp
enum class DisplayMode {
  Compact,
  Detailed,
  Debug,
};

struct ExampleConfig {
  int version = 1;
  bool showOverlay = true;
  int opacity = 80;
  double scale = 1.0;
  DisplayMode mode = DisplayMode::Compact;
  std::string accentColor = "#4AE0A0";
};
```

再特化 `pl::config::Schema<T>`，给启动器配置编辑器提供 UI 元数据：

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

运行时不要每次 callback 都临时构造 `ConfigFile`。把
`pl::config::ConfigFile<ExampleConfig>` 保存在 mod 自己的状态里，这样配置路径、
schema 路径、默认值和当前值不会分散：

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

构建期使用 `GenerateConfig.cpp` 的模式，并定义 `PL_CONFIG_NO_RUNTIME`，在 host
端生成 `config.json` 和 `config.schema.json`。这样 mod 刚导入启动器、native
库还没第一次加载时，配置页也能直接显示可编辑配置。

## Mod Menu 与配置持久化

lifecycle mod 优先使用 `pl::modmenu::ModuleBuilder`。它在 `enable()` 内调用时会
自动使用当前 native mod id 作为 owner 分组：

```cpp
return pl::modmenu::ModuleBuilder("full_cpp_mod.hud", "Full C++ Config Demo")
    .description("Pure C++ lifecycle module with persistent typed config.")
    .defaultEnabled(true)
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

LeviLauncher 会按 `module_id` 持久化用户在 Mod Menu 中选择的启用状态。
`defaultEnabled()` 只作为首次发现模块时的默认值；mod 自己的配置只需要保存
overlay 显示、透明度、缩放、模式、颜色等参数。

菜单 callback 收到的是字符串。需要防御式解析、把数值 clamp 到菜单和 schema 的同一
范围，然后调用 `save()`：

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

callback 函数必须是 static，或来自一个在模块注销/模组卸载前一直有效的长生命周期
对象。`RegisterModule()` 会复制模块字符串和配置项，但不会“复制”你的 callback
代码。

## 构建与打包

`build.ps1` 有意拆成两次 CMake。本项目只支持 `arm64-v8a`，所以示例固定构建
这个 ABI。

1. Host build：编译 `full_cpp_mod_config_gen`。
2. Android build：为 `arm64-v8a` 编译 `libfull_cpp_mod.so`。
3. Staging：把 `manifest.json`、`.so`、`config.json` 和
   `config.schema.json` 复制到 `dist/<Abi>/full-cpp-mod/`。
4. Packaging：把 staged 文件压成 `full-cpp-mod.levipack`。

常用参数：

```powershell
.\examples\full-cpp-mod\build.ps1
.\examples\full-cpp-mod\build.ps1 -Ndk <path-to-android-ndk>
.\examples\full-cpp-mod\build.ps1 -PreloaderRoot <path-to-preloader-android>
.\examples\full-cpp-mod\build.ps1 -NoLinkPreloader
```

如果不传 `-Ndk`，脚本会从 `ANDROID_NDK_HOME`、`ANDROID_NDK_ROOT`、
`ANDROID_HOME` 或 `ANDROID_SDK_ROOT` 推导 NDK 路径。

当你希望示例 `.so` 不携带本地 `libpreloader.so` link dependency，而是把 preloader
符号交给运行时解析时，使用 `-NoLinkPreloader`。

## 检查清单

- 保持 mod 目录名稳定；它就是运行时 mod id。
- 只构建 `arm64-v8a`；本项目不支持其他 Android ABI。
- C++ lifecycle mod 使用 `PL_REGISTER_MOD`。
- 注册菜单前先加载 config。
- 菜单默认值来自已加载 config。
- 需要跨重启保留的菜单改动必须立即持久化。
- callback 收到的是字符串，必须解析、校验并 clamp。
- callback 保持 static，或绑定到长生命周期 singleton。
- 打包时生成 `config.json` 和 `config.schema.json`。
- 验证 `.levipack` 根级包含 `manifest.json`、`.so` 和 `config/`。

更底层的接口细节继续阅读 [Mod API 参考](/zh-CN/api/mod) 和
[Config API 参考](/zh-CN/api/config)。
