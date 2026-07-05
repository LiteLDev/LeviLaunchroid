# Memory Hook Example

## Purpose

This example resolves a target with `pl::memory::resolveSignature()`, installs
a hook with `pl::memory::HookHandle`, and releases it in `disable()` or by
destroying the handle.

## Headers

```cpp
#include <pl/memory/Hook.hpp>
#include <pl/memory/Signature.hpp>
```

## Example

```cpp
#include <pl/Mod.hpp>
#include <pl/memory/Hook.hpp>
#include <pl/memory/Signature.hpp>

class MyMod {
public:
  bool enable(pl::mod::ModContext &context) {
    const auto target = pl::memory::resolveSignature(
        "Game_update", "libminecraftpe.so");
    if (target == 0) {
      context.logger().warn("Game_update was not found");
      return true;
    }

    mUpdateHook = pl::memory::HookHandle(
        reinterpret_cast<void *>(target),
        reinterpret_cast<void *>(&updateHook),
        reinterpret_cast<void **>(&mOriginalUpdate));
    return mUpdateHook.installed();
  }

  bool disable(pl::mod::ModContext &) {
    mUpdateHook.reset();
    return true;
  }

private:
  using UpdateFn = void (*)(void *);

  static void updateHook(void *self) {
    instance().mOriginalUpdate(self);
  }

  static MyMod &instance();

  UpdateFn mOriginalUpdate{};
  pl::memory::HookHandle mUpdateHook;
};
```

## Notes

- Keep the detour signature identical to the target function.
- Store hook handles in mod-owned state, not local variables.
- Prefer explicit lifecycle cleanup over constructor-time auto registration.
