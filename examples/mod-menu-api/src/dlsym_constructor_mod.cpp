#include <android/log.h>
#include <dlfcn.h>

#include <cstdlib>
#include <cstring>

#include <pl/c/Macro.h>
#include <pl/c/Mod.h>
#include <pl/c/PreloaderModMenu.h>

namespace {

constexpr const char *kLogTag = "LLMenuDlsymExample";
constexpr const char *kOwnerModId = "dlsym-constructor";
constexpr const char *kModuleId = "example.dlsym_constructor.runtime";

using GetPreloaderModMenuFn = PLModMenu_Interface *(*)();

bool g_enabled = true;
float g_opacity = 0.85f;

void logInfo(const char *message) {
  __android_log_print(ANDROID_LOG_INFO, kLogTag, "%s", message);
}

PLModMenu_Interface *resolveMenu() {
  auto *fn = reinterpret_cast<GetPreloaderModMenuFn>(
      dlsym(RTLD_DEFAULT, "GetPreloaderModMenu"));
  if (fn) {
    return fn();
  }

  void *handle = nullptr;
#ifdef RTLD_NOLOAD
  handle = dlopen("libpreloader.so", RTLD_NOW | RTLD_NOLOAD);
#endif
  if (!handle) {
    handle = dlopen("libpreloader.so", RTLD_NOW);
  }
  if (!handle) {
    __android_log_print(ANDROID_LOG_WARN, kLogTag,
                        "failed to open libpreloader.so: %s", dlerror());
    return nullptr;
  }

  fn = reinterpret_cast<GetPreloaderModMenuFn>(
      dlsym(handle, "GetPreloaderModMenu"));
  return fn ? fn() : nullptr;
}

void onModuleToggle(const char *module_id, bool enabled) {
  if (!module_id || std::strcmp(module_id, kModuleId) != 0) {
    return;
  }

  g_enabled = enabled;
  __android_log_print(ANDROID_LOG_INFO, kLogTag, "toggle %s = %s", module_id,
                      enabled ? "true" : "false");
}

void onConfigChanged(const char *module_id, const char *key,
                     const char *value) {
  if (!module_id || std::strcmp(module_id, kModuleId) != 0 || !key) {
    return;
  }

  const char *safeValue = value ? value : "";
  if (std::strcmp(key, "opacity") == 0) {
    g_opacity = std::strtof(safeValue, nullptr);
  }

  __android_log_print(ANDROID_LOG_INFO, kLogTag, "config %s.%s = %s",
                      module_id, key, safeValue);
}

void registerDlsymModule() {
  PLModMenu_Interface *menu = resolveMenu();
  if (!menu || !menu->RegisterModule) {
    logInfo("mod menu api is unavailable");
    return;
  }

  const PLModMenu_ConfigEntry configs[] = {
      {
          .key = "opacity",
          .display_name = "Opacity",
          .type = PL_CONFIG_SLIDER_FLOAT,
          .default_value = "0.85",
          .min_value = "0.20",
          .max_value = "1.00",
          .depends_on = nullptr,
      },
  };

  PLModMenu_ModuleInfo info{
      .module_id = kModuleId,
      .display_name = "Dlsym Constructor Module",
      .description = "Registered from a constructor using dlsym.",
      .mod_id = kOwnerModId,
      .default_enabled = g_enabled,
      .on_toggle = onModuleToggle,
      .config_count = static_cast<int>(sizeof(configs) / sizeof(configs[0])),
      .configs = configs,
      .on_config_changed = onConfigChanged,
      .hide_in_hud_editor = false,
  };

  if (!menu->RegisterModule(&info)) {
    logInfo("RegisterModule failed");
  }
}

void unregisterDlsymModule() {
  PLModMenu_Interface *menu = resolveMenu();
  if (menu && menu->UnregisterModule) {
    menu->UnregisterModule(kModuleId);
  }
}

__attribute__((constructor)) void onLibraryLoaded() {
  logInfo("constructor");
  registerDlsymModule();
}

__attribute__((destructor)) void onLibraryUnloaded() {
  logInfo("destructor");
  unregisterDlsymModule();
}

} // namespace

PL_SHARED_EXPORT bool PLMod_Load(JavaVM *vm, const PLModInfo *mod_info) {
  (void)vm;
  (void)mod_info;
  logInfo("PLMod_Load");
  return true;
}

PL_SHARED_EXPORT bool PLMod_Enable(void) {
  logInfo("PLMod_Enable");
  return true;
}

PL_SHARED_EXPORT bool PLMod_Disable(void) {
  logInfo("PLMod_Disable");
  return true;
}

PL_SHARED_EXPORT bool PLMod_Unload(void) {
  logInfo("PLMod_Unload");
  unregisterDlsymModule();
  return true;
}
