#pragma once

#include "pl/internal/Macro.h"

#ifdef __cplusplus
namespace pl::hook {
#endif

typedef void *FuncPtr;

enum Priority : int {
  PriorityHighest = 0,
  PriorityHigh = 100,
  PriorityNormal = 200,
  PriorityLow = 300,
  PriorityLowest = 400,
};

PLCAPI int pl_hook(FuncPtr target, FuncPtr detour, FuncPtr *originalFunc,
                   Priority priority);

PLCAPI bool pl_unhook(FuncPtr target, FuncPtr detour);

#ifdef __cplusplus
} // namespace pl::hook
#endif