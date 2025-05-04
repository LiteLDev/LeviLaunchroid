#pragma once
#include <filesystem>
#include <jni.h>
#include <string>

namespace ModManager {
bool IsModEnabled(const std::filesystem::path &configPath,
                  const std::string &modFileName);
void LoadAndInitializeEnabledMods(const std::string &modsDir,
                                  const std::string &dataDir, JavaVM *vm);
} // namespace ModManager