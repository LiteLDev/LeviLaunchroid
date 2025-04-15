#pragma once

#include <iostream>
#include <type_traits>

#define LOG(...)
#define VA_EXPAND(...) __VA_ARGS__

namespace memory {

void unhookAll();

using FuncPtr = void *;

template <typename T, std::enable_if_t<(sizeof(T) == sizeof(FuncPtr)), int> = 0>
constexpr FuncPtr toFuncPtr(T t) {
  union {
    FuncPtr fp;
    T t;
  } u{};
  u.t = t;
  return u.fp;
}

template <typename T,
          std::enable_if_t<(std::is_member_function_pointer<T>::value &&
                            sizeof(T) == sizeof(FuncPtr) + sizeof(ptrdiff_t)),
                           int> = 0>
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

/**
 * @brief Hook priority enum.
 * @details The lower priority, the hook will be executed earlier
 */
enum class HookPriority : int {
  Lowest = 0,
  Low = 100,
  Normal = 200,
  High = 300,
  Highest = 400,
};

int hook(FuncPtr target, FuncPtr detour, FuncPtr *originalFunc,
         HookPriority priority);

bool unhook(FuncPtr target, FuncPtr detour);

/**
 * @brief Get the pointer of a function by identifier.
 *
 * @param identifier symbol or signature
 * @return FuncPtr
 */
FuncPtr resolveIdentifier(char const *identifier);
FuncPtr resolveIdentifier(std::initializer_list<const char *> identifiers);

template <typename T>
using FuncPtrType =
    std::integral_constant<bool,
                           std::is_function<std::remove_pointer_t<T>>::value ||
                               std::is_member_function_pointer<T>::value>;

template <typename T,
          typename = std::enable_if_t<FuncPtrType<T>::value ||
                                      std::is_same<T, uintptr_t>::value>>
constexpr FuncPtr resolveIdentifier(T identifier) {
  return toFuncPtr(identifier);
}

// redirect to resolveIdentifier(char const*)
template <typename T>
constexpr FuncPtr resolveIdentifier(char const *identifier) {
  return resolveIdentifier(identifier);
}

// redirect to resolveIdentifier(uintptr_t)
template <typename T> constexpr FuncPtr resolveIdentifier(uintptr_t address) {
  return resolveIdentifier(address);
}

// redirect to resolveIdentifier(FuncPtr)
template <typename T> constexpr FuncPtr resolveIdentifier(FuncPtr address) {
  return address;
}

template <typename T>
constexpr FuncPtr
resolveIdentifier(std::initializer_list<const char *> identifiers) {
  return resolveIdentifier(identifiers);
}

template <typename T> struct HookAutoRegister {
  HookAutoRegister() { T::hook(); }
  ~HookAutoRegister() { T::unhook(); }
  static int hook() { return T::hook(); }
  static bool unhook() { return T::unhook(); }
};

} // namespace memory

#define HOOK_IMPL(REGISTER, FUNC_PTR, STATIC, CALL, DEF_TYPE, TYPE, PRIORITY,  \
                  IDENTIFIER, RET_TYPE, ...)                                   \
  struct DEF_TYPE TYPE {                                                       \
    using FuncPtr = ::memory::FuncPtr;                                         \
    using HookPriority = ::memory::HookPriority;                               \
    static constexpr auto IDENTIFIER_STORAGE = IDENTIFIER;                     \
    using OriginFuncType = ::memory::AddConstAtMemberFunIfOriginIs<            \
        RET_TYPE FUNC_PTR(__VA_ARGS__), decltype(IDENTIFIER_STORAGE)>;         \
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
      target = memory::resolveIdentifier<OriginFuncType>(IDENTIFIER);          \
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

/**
 * @brief Register a hook for a typed static function.
 * @param DefType The name of the hook definition.
 * @param type The type of the function.
 * @param priority The priority of the hook.
 * @param identifier The identifier of the hook. It can be a function symbol,
 * address or a signature.
 * @param Ret The return type of the hook.
 * @param ... The parameters of the hook.
 *
 * @note register or unregister by calling DefType::hook() and
 * DefType::unhook().
 */
#define SKY_TYPED_STATIC_HOOK(DefType, type, priority, identifier, Ret, ...)   \
  VA_EXPAND(SKY_STATIC_HOOK_IMPL(DefType,                                      \
                                 : public type, priority, identifier, Ret,     \
                                   __VA_ARGS__))

/**
 * @brief Register a hook for a static function.
 * @param DefType The name of the hook definition.
 * @param priority The priority of the hook.
 * @param identifier The identifier of the hook. It can be a function symbol,
 * address or a signature.
 * @param Ret The return type of the hook.
 * @param ... The parameters of the hook.
 *
 * @note register or unregister by calling DefType::hook() and
 * DefType::unhook().
 */
#define SKY_STATIC_HOOK(DefType, priority, identifier, Ret, ...)               \
  VA_EXPAND(                                                                   \
      SKY_STATIC_HOOK_IMPL(DefType, , priority, identifier, Ret, __VA_ARGS__))

/**
 * @brief Register a hook for a typed static function.
 * @details The hook will be automatically registered and unregistered.
 * @see TYPED_STATIC_HOOK for usage.
 */
#define SKY_AUTO_TYPED_STATIC_HOOK(DefType, type, priority, identifier, Ret,   \
                                   ...)                                        \
  VA_EXPAND(SKY_AUTO_STATIC_HOOK_IMPL(DefType,                                 \
                                      : public type, priority, identifier,     \
                                        Ret, __VA_ARGS__))

/**
 * @brief Register a hook for a static function.
 * @details The hook will be automatically registered and unregistered.
 * @see STATIC_HOOK for usage.
 */
#define SKY_AUTO_STATIC_HOOK(DefType, priority, identifier, Ret, ...)          \
  VA_EXPAND(SKY_AUTO_STATIC_HOOK_IMPL(DefType, , priority, identifier, Ret,    \
                                      __VA_ARGS__))

/**
 * @brief Register a hook for a typed instance function.
 * @param DEF_TYPE The name of the hook definition.
 * @param PRIORITY memory::HookPriority The priority of the hook.
 * @param TYPE The type which the function belongs to.
 * @param IDENTIFIER The identifier of the hook. It can be a function pointer,
 * symbol, address or a signature.
 * @param RET_TYPE The return type of the hook.
 * @param ... The parameters of the hook.
 *
 * @note register or unregister by calling DEF_TYPE::hook() and
 * DEF_TYPE::unhook().
 */
#define SKY_TYPED_HOOK(DEF_TYPE, PRIORITY, TYPE, IDENTIFIER, RET_TYPE, ...)    \
  VA_EXPAND(SKY_INSTANCE_HOOK_IMPL(DEF_TYPE,                                   \
                                   : public TYPE, PRIORITY, IDENTIFIER,        \
                                     RET_TYPE, __VA_ARGS__))

/**
 * @brief Register a hook for a instance function.
 * @param DEF_TYPE The name of the hook definition.
 * @param PRIORITY memory::HookPriority The priority of the hook.
 * @param IDENTIFIER The identifier of the hook. It can be a function pointer,
 * symbol, address or a signature.
 * @param RET_TYPE The return type of the hook.
 * @param ... The parameters of the hook.
 *
 * @note register or unregister by calling DEF_TYPE::hook() and
 * DEF_TYPE::unhook().
 */
#define SKY_INSTANCE_HOOK(DEF_TYPE, PRIORITY, IDENTIFIER, RET_TYPE, ...)       \
  VA_EXPAND(SKY_INSTANCE_HOOK_IMPL(DEF_TYPE, , PRIORITY, IDENTIFIER, RET_TYPE, \
                                   __VA_ARGS__))

/**
 * @brief Register a hook for a typed instance function.
 * @details The hook will be automatically registered and unregistered.
 * @see TYPED_HOOK for usage.
 */
#define SKY_AUTO_TYPED_INSTANCE_HOOK(DEF_TYPE, PRIORITY, TYPE, IDENTIFIER,     \
                                     RET_TYPE, ...)                            \
  VA_EXPAND(SKY_AUTO_INSTANCE_HOOK_IMPL(DEF_TYPE,                              \
                                        : public TYPE, PRIORITY, IDENTIFIER,   \
                                          RET_TYPE, __VA_ARGS__))

/**
 * @brief Register a hook for a instance function.
 * @details The hook will be automatically registered and unregistered.
 * @see HOOK for usage.
 */
#define SKY_AUTO_INSTANCE_HOOK(DEF_TYPE, PRIORITY, IDENTIFIER, RET_TYPE, ...)  \
  VA_EXPAND(SKY_AUTO_INSTANCE_HOOK_IMPL(DEF_TYPE, , PRIORITY, IDENTIFIER,      \
                                        RET_TYPE, __VA_ARGS__))