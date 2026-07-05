# SDK 头文件与宏

## 公开头文件

SDK 只安装以下头文件：

```cpp
#include <pl/Config.hpp>
#include <pl/Export.hpp>
#include <pl/Input.hpp>
#include <pl/Logger.hpp>
#include <pl/Mod.hpp>
#include <pl/ModMenu.hpp>
#include <pl/memory/Hook.hpp>
#include <pl/memory/Patch.hpp>
#include <pl/memory/Signature.hpp>
```

## PL_EXPORT

`PL_EXPORT` 在支持的编译器上为符号设置默认可见性。大多数 mod 不需要直接使用；
`PL_REGISTER_MOD` 已经会导出 `PLGetModRegistration`。

```cpp
#include <pl/Export.hpp>

PL_EXPORT void customVisibleFunction();
```

## PL_REGISTER_MOD

`PL_REGISTER_MOD(Type, instanceExpr)` 注册生命周期对象。

```cpp
#include <pl/Mod.hpp>

class MyMod {
public:
  bool load(pl::mod::ModContext &context);
};

PL_REGISTER_MOD(MyMod, MyMod{})
```

该宏以 C linkage 导出 `PLGetModRegistration`，loader 无需依赖 C++ mangled name
即可找到它。导出的函数返回 C++ 生命周期 registration 表。

## 命名

公开 SDK 遵循 Lamina C++ 风格：

| 项目 | 风格 |
| --- | --- |
| 文件和类型 | `UpperCamelCase` |
| 函数和变量 | `lowerCamelCase` |
| 私有成员 | `mUpperCamelCase` |
| 常量 | `UpperCamelCase` |
| 宏 | `UPPER_SNAKE_CASE` |
