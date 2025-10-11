#pragma once

#pragma warning(error : 4834)

#define VA_EXPAND(...) __VA_ARGS__

#include <initializer_list>
#include <type_traits>


namespace memory {

using FuncPtr = void *;

template <typename T>
  requires(sizeof(T) == sizeof(FuncPtr))
constexpr FuncPtr toFuncPtr(T t) {
  union {
    FuncPtr fp;
    T t;
  } u{};
  u.t = t;
  return u.fp;
}

template <typename T>
  requires(std::is_member_function_pointer_v<T> &&
           sizeof(T) == sizeof(FuncPtr) + sizeof(ptrdiff_t))
constexpr FuncPtr toFuncPtr(T t) {
  union {
    struct {
      FuncPtr fp;
      ptrdiff_t offset;
    };
    T t;
  } u{};
  u.t = t;
  return u.fp;
}

template <typename T> struct IsConstMemberFun : std::false_type {};
template <typename T, typename Ret, typename... Args>
struct IsConstMemberFun<Ret (T::*)(Args...) const> : std::true_type {};
template <typename T>
inline constexpr bool IsConstMemberFunV = IsConstMemberFun<T>::value;

template <typename T> struct AddConstAtMemberFun {
  using type = T;
};
template <typename T, typename Ret, typename... Args>
struct AddConstAtMemberFun<Ret (T::*)(Args...)> {
  using type = Ret (T::*)(Args...) const;
};
template <typename T>
using AddConstAtMemberFunT = typename AddConstAtMemberFun<T>::type;

template <typename T, typename U>
using AddConstAtMemberFunIfOriginIs =
    std::conditional_t<IsConstMemberFunV<U>, AddConstAtMemberFunT<T>, T>;

enum class HookPriority : int {
  Lowest = 0,
  Low = 100,
  Normal = 200,
  High = 300,
  Highest = 400,
};

int hook(FuncPtr target, FuncPtr detour, FuncPtr *originalFunc,
         HookPriority priority, bool suspendThreads = true);
bool unhook(FuncPtr target, FuncPtr detour, bool suspendThreads = true);

/**
 * @brief Get the pointer of a function by identifier.
 *
 * @param identifier symbol or signature
 * @param moduleName module to search
 * @return FuncPtr
 */
FuncPtr resolveIdentifier(char const *identifier, char const *moduleName);
FuncPtr resolveIdentifier(std::initializer_list<const char *> identifiers,
                          char const *moduleName);

template <typename T>
concept FuncPtrType = std::is_function_v<std::remove_pointer_t<T>> ||
                      std::is_member_function_pointer_v<T>;

// 所有重载均加上 moduleName
template <typename T>
  requires(FuncPtrType<T> || std::is_same_v<T, uintptr_t>)
constexpr FuncPtr resolveIdentifier(T identifier, const char *moduleName) {
  return toFuncPtr(identifier);
}

// redirect to resolveIdentifier(char const*)
template <typename T>
constexpr FuncPtr resolveIdentifier(char const *identifier,
                                    const char *moduleName) {
  return resolveIdentifier(identifier, moduleName);
}

// redirect to resolveIdentifier(uintptr_t)
template <typename T>
constexpr FuncPtr resolveIdentifier(uintptr_t address, const char *moduleName) {
  return resolveIdentifier(address, moduleName);
}

// redirect to resolveIdentifier(FuncPtr)
template <typename T>
constexpr FuncPtr resolveIdentifier(FuncPtr address,
                                    const char * /*moduleName*/) {
  return address;
}

template <typename T>
constexpr FuncPtr
resolveIdentifier(std::initializer_list<const char *> identifiers,
                  const char *moduleName) {
  return resolveIdentifier(identifiers, moduleName);
}

template <typename T> struct HookAutoRegister {
  HookAutoRegister() { T::hook(); }
  ~HookAutoRegister() { T::unhook(); }
  static int hook() { return T::hook(); }
  static bool unhook() { return T::unhook(); }
};

} // namespace memory

#define HOOK_IMPL(REGISTER, FUNC_PTR, STATIC, CALL, DEF_TYPE, TYPE, PRIORITY,  \
                  IDENTIFIER, MODULE, RET_TYPE, ...)                           \
  struct DEF_TYPE TYPE {                                                       \
    using FuncPtr = ::memory::FuncPtr;                                         \
    using HookPriority = ::memory::HookPriority;                               \
    using OriginFuncType = ::memory::AddConstAtMemberFunIfOriginIs<            \
        RET_TYPE FUNC_PTR(__VA_ARGS__), decltype(IDENTIFIER)>;                 \
                                                                               \
    inline static FuncPtr target{};                                            \
    inline static OriginFuncType originFunc{};                                 \
                                                                               \
    template <typename... Args> STATIC RET_TYPE origin(Args &&...params) {     \
      return CALL(std::forward<Args>(params)...);                              \
    }                                                                          \
                                                                               \
    STATIC RET_TYPE detour(__VA_ARGS__);                                       \
                                                                               \
    static int hook() {                                                        \
      target = memory::resolveIdentifier<OriginFuncType>(IDENTIFIER, MODULE);  \
      if (target == nullptr) {                                                 \
        return -1;                                                             \
      }                                                                        \
      return memory::hook(target, memory::toFuncPtr(&DEF_TYPE::detour),        \
                          reinterpret_cast<FuncPtr *>(&originFunc), PRIORITY); \
    }                                                                          \
                                                                               \
    static bool unhook() {                                                     \
      return memory::unhook(target, memory::toFuncPtr(&DEF_TYPE::detour));     \
    }                                                                          \
  };                                                                           \
  REGISTER;                                                                    \
  RET_TYPE DEF_TYPE::detour(__VA_ARGS__)

#define SKY_AUTO_REG_HOOK_IMPL(FUNC_PTR, STATIC, CALL, DEF_TYPE, ...)          \
  VA_EXPAND(HOOK_IMPL(                                                         \
      inline memory::HookAutoRegister<DEF_TYPE> DEF_TYPE##AutoRegister,        \
      FUNC_PTR, STATIC, CALL, DEF_TYPE, __VA_ARGS__))

#define SKY_MANUAL_REG_HOOK_IMPL(...) VA_EXPAND(HOOK_IMPL(, __VA_ARGS__))

#define SKY_STATIC_HOOK_IMPL(...)                                              \
  VA_EXPAND(SKY_MANUAL_REG_HOOK_IMPL((*), static, originFunc, __VA_ARGS__))

#define SKY_AUTO_STATIC_HOOK_IMPL(...)                                         \
  VA_EXPAND(SKY_AUTO_REG_HOOK_IMPL((*), static, originFunc, __VA_ARGS__))

#define SKY_INSTANCE_HOOK_IMPL(DEF_TYPE, ...)                                  \
  VA_EXPAND(SKY_MANUAL_REG_HOOK_IMPL((DEF_TYPE::*), , (this->*originFunc),     \
                                     DEF_TYPE, __VA_ARGS__))

#define SKY_AUTO_INSTANCE_HOOK_IMPL(DEF_TYPE, ...)                             \
  VA_EXPAND(SKY_AUTO_REG_HOOK_IMPL((DEF_TYPE::*), , (this->*originFunc),       \
                                   DEF_TYPE, __VA_ARGS__))

#define SKY_TYPED_STATIC_HOOK(DefType, type, priority, identifier, module,     \
                              Ret, ...)                                        \
  VA_EXPAND(SKY_STATIC_HOOK_IMPL(DefType, : public type, priority, identifier, \
                                 module, Ret, __VA_ARGS__))

#define SKY_STATIC_HOOK(DefType, priority, identifier, module, Ret, ...)       \
  VA_EXPAND(SKY_STATIC_HOOK_IMPL(DefType, , priority, identifier, module, Ret, \
                                 __VA_ARGS__))

#define SKY_AUTO_TYPED_STATIC_HOOK(DefType, type, priority, identifier,        \
                                   module, Ret, ...)                           \
  VA_EXPAND(SKY_AUTO_STATIC_HOOK_IMPL(DefType, : public type, priority,        \
                                      identifier, module, Ret, __VA_ARGS__))

#define SKY_AUTO_STATIC_HOOK(DefType, priority, identifier, module, Ret, ...)  \
  VA_EXPAND(SKY_AUTO_STATIC_HOOK_IMPL(DefType, , priority, identifier, module, \
                                      Ret, __VA_ARGS__))

#define SKY_TYPED_HOOK(DEF_TYPE, PRIORITY, TYPE, IDENTIFIER, MODULE, RET_TYPE, \
                       ...)                                                    \
  VA_EXPAND(SKY_INSTANCE_HOOK_IMPL(DEF_TYPE, : public TYPE, PRIORITY,          \
                                   IDENTIFIER, MODULE, RET_TYPE, __VA_ARGS__))

#define SKY_INSTANCE_HOOK(DEF_TYPE, PRIORITY, IDENTIFIER, MODULE, RET_TYPE,    \
                          ...)                                                 \
  VA_EXPAND(SKY_INSTANCE_HOOK_IMPL(DEF_TYPE, , PRIORITY, IDENTIFIER, MODULE,   \
                                   RET_TYPE, __VA_ARGS__))

#define SKY_AUTO_TYPED_INSTANCE_HOOK(DEF_TYPE, PRIORITY, TYPE, IDENTIFIER,     \
                                     MODULE, RET_TYPE, ...)                    \
  VA_EXPAND(SKY_AUTO_INSTANCE_HOOK_IMPL(DEF_TYPE, : public TYPE, PRIORITY,     \
                                        IDENTIFIER, MODULE, RET_TYPE,          \
                                        __VA_ARGS__))

#define SKY_AUTO_INSTANCE_HOOK(DEF_TYPE, PRIORITY, IDENTIFIER, MODULE,         \
                               RET_TYPE, ...)                                  \
  VA_EXPAND(SKY_AUTO_INSTANCE_HOOK_IMPL(DEF_TYPE, , PRIORITY, IDENTIFIER,      \
                                        MODULE, RET_TYPE, __VA_ARGS__))