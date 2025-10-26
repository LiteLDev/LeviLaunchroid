#pragma once

namespace elf_util {
    void* resolveElfSymbol(const char *libraryName, const char *symbolName);
    const char *getAndroidLinkerPath();
    int getAndroidSystemVersion();
}