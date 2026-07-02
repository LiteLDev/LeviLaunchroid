#include <android/log.h>

#include <cstdlib>
#include <string>

#include <pl/cpp/ModMenu.hpp>
#include <pl/cpp/mod/RegisterHelper.hpp>

namespace {

constexpr const char *kLogTag = "LLMenuCppExample";
constexpr const char *kQuickToggleModule = "example.cpp_lifecycle.quick_toggle";
constexpr const char *kConfiguredModule = "example.cpp_lifecycle.configured";

bool g_quickToggleEnabled = false;
bool g_configuredEnabled = true;
int g_strength = 40;
float g_scale = 1.0f;
int g_mode = 1;

void logInfo(const char *message) {
  __android_log_print(ANDROID_LOG_INFO, kLogTag, "%s", message);
}

void onModuleToggle(const char *module_id, bool enabled) {
  if (!module_id)
    return;

  const std::string id(module_id);
  if (id == kQuickToggleModule) {
    g_quickToggleEnabled = enabled;
  } else if (id == kConfiguredModule) {
    g_configuredEnabled = enabled;
  }

  __android_log_print(ANDROID_LOG_INFO, kLogTag, "toggle %s = %s", module_id,
                      enabled ? "true" : "false");
}

void onConfigChanged(const char *module_id, const char *key,
                     const char *value) {
  if (!module_id || !key)
    return;

  const std::string moduleId(module_id);
  if (moduleId != kConfiguredModule)
    return;

  const std::string configKey(key);
  const char *safeValue = value ? value : "";
  if (configKey == "strength") {
    g_strength = std::atoi(safeValue);
  } else if (configKey == "scale") {
    g_scale = std::strtof(safeValue, nullptr);
  } else if (configKey == "mode") {
    g_mode = std::atoi(safeValue);
  }

  __android_log_print(ANDROID_LOG_INFO, kLogTag, "config %s.%s = %s",
                      module_id, key, safeValue);
}

class CppLifecycleMod {
public:
  bool load() {
    logInfo("load");

    const bool quickRegistered =
        pl::modmenu::ModuleBuilder(kQuickToggleModule, "CPP Quick Toggle")
            .description("Registered with pl::modmenu::ModuleBuilder.")
            .defaultEnabled(g_quickToggleEnabled)
            .onToggle(onModuleToggle)
            .registerModule();

    const bool configuredRegistered =
        pl::modmenu::ModuleBuilder(kConfiguredModule, "CPP Configured Module")
            .description("Exercises slider, radio and color config entries.")
            .defaultEnabled(g_configuredEnabled)
            .onToggle(onModuleToggle)
            .config("strength", "Strength", PL_CONFIG_SLIDER_INT, "40", "0",
                    "100")
            .config("scale", "Scale", PL_CONFIG_SLIDER_FLOAT, "1.0", "0.5",
                    "2.0")
            .config("mode", "Mode", PL_CONFIG_RADIO, "1",
                    "Off,Normal,Aggressive")
            .config("accent", "Accent", PL_CONFIG_COLOR, "#4AE0A0")
            .onConfigChanged(onConfigChanged)
            .registerModule();

    return quickRegistered && configuredRegistered;
  }

  bool enable() {
    logInfo("enable");
    return true;
  }

  bool disable() {
    logInfo("disable");
    return true;
  }

  bool unload() {
    logInfo("unload");
    return true;
  }
};

CppLifecycleMod g_mod;

} // namespace

PL_REGISTER_MOD(CppLifecycleMod, g_mod)
