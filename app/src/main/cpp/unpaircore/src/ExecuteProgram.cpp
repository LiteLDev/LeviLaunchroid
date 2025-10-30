#include "HookManager.h"

extern "C" void ExecuteProgram(void) {
    static int isHooked = 0;
    if (isHooked == 1) {
        core::setupHooks();
        core::patchMinecraftLogo();
        return;
    }
    ++isHooked;
}