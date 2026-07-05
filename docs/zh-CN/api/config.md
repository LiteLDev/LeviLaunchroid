# Config API

## 作用

Config API 为 native mod 提供类型化 JSON 配置。它可以创建默认配置、把旧用户
值合并进新默认结构、规范化 JSON，并生成启动器配置编辑器使用的
`config.schema.json`。

## 头文件

```cpp
#include <pl/Config.hpp>
```

配置类型应是简单 aggregate struct：字段公开、有默认成员初始化，并带一个整数
`version` 字段。

## 定义配置

```cpp
#include <pl/Config.hpp>

#include <string>
#include <vector>

enum class Profile {
  Quiet,
  Balanced,
  Verbose,
};

struct HudConfig {
  bool showMessage = true;
  std::string message = "Hello from config";
  double scale = 1.25;
};

struct ModConfig {
  int version = 1;
  bool enabled = true;
  Profile profile = Profile::Balanced;
  HudConfig hud;
  std::vector<std::string> tags = {"overlay", "example"};
};
```

生成 JSON 的字段顺序跟 aggregate 字段顺序一致。字段名以 `$` 开头时会被反射忽略。

## 加载与保存

请从 `pl::mod::ModContext` 传入显式路径。SDK 不保存全局 current mod，因此默认路径
helper 会返回空路径。

```cpp
class MyMod {
public:
  bool load(pl::mod::ModContext &context) {
    mConfig.emplace(ModConfig{}, context.configDir() / "config.json",
                    context.configDir() / "config.schema.json");
    if (!mConfig->load()) {
      context.logger().warn("Failed to load config");
      return false;
    }
    return true;
  }

  bool enable(pl::mod::ModContext &) {
    return mConfig && mConfig->value().enabled;
  }

private:
  std::optional<pl::config::ConfigFile<ModConfig>> mConfig;
};
```

运行期修改通过同一个 `ConfigFile` 保存：

```cpp
mConfig->value().enabled = false;
mConfig->save();
```

## 更新行为

`ConfigFile<T>::load()` 会先从 C++ 默认值开始，再把已有 JSON 合并进去。

- 缺失文件会自动创建。
- 新字段会使用 C++ 默认值补齐。
- 类型可反序列化时保留已有用户值。
- `version` 会强制回到 C++ 默认 `version`。
- 文件缺失、过旧、损坏或不完整时会规范化并写回。
- 规范化后不会保留未知 key。

## Schema 元数据

schema 生成器能推断 `type`、`default`、嵌套 `properties`、数组 `items` 和枚举选项。
使用 `pl::config::Schema<T>` 增加 UI 元数据。

```cpp
template <> struct pl::config::Schema<ModConfig> {
  static constexpr std::string_view title = "Example Config";
  static constexpr std::string_view description =
      "Settings for the example native mod.";

  static constexpr FieldSchema field(std::string_view name) {
    if (name == "version") {
      return {.title = "Version", .readOnly = true};
    }
    if (name == "enabled") {
      return {.title = "Enabled",
              .description = "Turns the mod behavior on or off."};
    }
    if (name == "profile") {
      return {.title = "Profile",
              .description = "Selects the runtime behavior preset."};
    }
    return {};
  }
};
```

支持的元数据：

| 字段 | Schema 输出 | 用途 |
| --- | --- | --- |
| `title` | `title` | 用户可读字段名。 |
| `description` | `description` | 编辑器帮助文本。 |
| `minimum` | `minimum` | 数值下限。 |
| `maximum` | `maximum` | 数值上限。 |
| `readOnly` | `readOnly` | 生成字段或信息字段。 |

## 支持类型

| C++ 类型 | JSON 表示 |
| --- | --- |
| aggregate struct | object |
| `std::string` | string |
| `bool` | boolean |
| integral types | integer |
| floating point types | number |
| enum | `magic_enum` 能命名时为 string enum |
| `std::vector<T>` | array |
| `std::optional<T>` | value 或 `null` |

配置对象保持简单。避免自定义构造、私有字段和带复杂逻辑的配置类型。
