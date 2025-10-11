#include "Hook.h"
#include "pl/Hook.h"
#include "pl/Logger.h"
#include "pl/Signature.h"

namespace memory {

FuncPtr resolveIdentifier(char const *identifier, char const *moduleName) {
  return reinterpret_cast<FuncPtr>(
      pl::signature::resolveSignature(identifier, moduleName));
}

FuncPtr resolveIdentifier(std::initializer_list<const char *> identifiers,
                          char const *moduleName) {
  for (const auto &identifier : identifiers) {
    FuncPtr result = resolveIdentifier(identifier, moduleName);
    if (result != nullptr) {
      return result;
    }
  }
  return nullptr;
}

int hook(FuncPtr target, FuncPtr detour, FuncPtr *originalFunc,
         HookPriority priority, bool) {
  return pl::hook::pl_hook(target, detour, originalFunc,
                           pl::hook::Priority(priority));
}

bool unhook(FuncPtr target, FuncPtr detour, bool) {
  return pl::hook::pl_unhook(target, detour);
}

} // namespace memory
