# SDK Headers and Macros

## Public Headers

The SDK installs only these headers:

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

`PL_EXPORT` marks a symbol with default visibility on supported compilers. Most
mods do not need it directly; `PL_REGISTER_MOD` already exports
`PLGetModRegistration`.

```cpp
#include <pl/Export.hpp>

PL_EXPORT void customVisibleFunction();
```

## PL_REGISTER_MOD

`PL_REGISTER_MOD(Type, instanceExpr)` registers a lifecycle object.

```cpp
#include <pl/Mod.hpp>

class MyMod {
public:
  bool load(pl::mod::ModContext &context);
};

PL_REGISTER_MOD(MyMod, MyMod{})
```

The macro exports `PLGetModRegistration` with C linkage so the loader can find
it without a C++ mangled name. The exported function returns a C++ lifecycle
registration table.

## Naming

Public SDK names follow the Lamina C++ style:

| Item | Style |
| --- | --- |
| Files and types | `UpperCamelCase` |
| Functions and variables | `lowerCamelCase` |
| Private members | `mUpperCamelCase` |
| Constants | `UpperCamelCase` |
| Macros | `UPPER_SNAKE_CASE` |
