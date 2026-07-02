#include <android/log.h>

#include <stdbool.h>
#include <stdlib.h>
#include <string.h>

#include <pl/c/Macro.h>
#include <pl/c/Mod.h>
#include <pl/c/PreloaderModMenu.h>

static const char *kLogTag = "LLMenuCExample";
static const char *kModuleId = "example.c_lifecycle.raw_api";

static bool g_enabled = false;
static bool g_visible = true;
static int g_size = 16;
static int g_layout = 0;

static void log_message(const char *message) {
  __android_log_print(ANDROID_LOG_INFO, kLogTag, "%s", message);
}

static void on_module_toggle(const char *module_id, bool enabled) {
  if (!module_id || strcmp(module_id, kModuleId) != 0) {
    return;
  }

  g_enabled = enabled;
  __android_log_print(ANDROID_LOG_INFO, kLogTag, "toggle %s = %s", module_id,
                      enabled ? "true" : "false");
}

static void on_config_changed(const char *module_id, const char *key,
                              const char *value) {
  if (!module_id || strcmp(module_id, kModuleId) != 0 || !key) {
    return;
  }

  const char *safe_value = value ? value : "";
  if (strcmp(key, "visible") == 0) {
    g_visible = strcmp(safe_value, "true") == 0 || strcmp(safe_value, "1") == 0;
  } else if (strcmp(key, "size") == 0) {
    g_size = atoi(safe_value);
  } else if (strcmp(key, "layout") == 0) {
    g_layout = atoi(safe_value);
  }

  __android_log_print(ANDROID_LOG_INFO, kLogTag, "config %s.%s = %s",
                      module_id, key, safe_value);
}

PL_SHARED_EXPORT bool PLMod_Load(JavaVM *vm, const PLModInfo *mod_info) {
  (void)vm;
  (void)mod_info;
  log_message("load");

  PLModMenu_Interface *menu = GetPreloaderModMenu();
  if (!menu || !menu->RegisterModule) {
    log_message("mod menu api is unavailable");
    return false;
  }

  const PLModMenu_ConfigEntry configs[] = {
      {
          .key = "visible",
          .display_name = "Visible",
          .type = PL_CONFIG_TOGGLE,
          .default_value = "true",
          .min_value = NULL,
          .max_value = NULL,
          .depends_on = NULL,
      },
      {
          .key = "size",
          .display_name = "Size",
          .type = PL_CONFIG_SLIDER_INT,
          .default_value = "16",
          .min_value = "8",
          .max_value = "32",
          .depends_on = NULL,
      },
      {
          .key = "layout",
          .display_name = "Layout",
          .type = PL_CONFIG_RADIO,
          .default_value = "0",
          .min_value = "Compact,Wide,Debug",
          .max_value = NULL,
          .depends_on = NULL,
      },
  };

  PLModMenu_ModuleInfo info = {
      .module_id = kModuleId,
      .display_name = "C Raw API Module",
      .description = "Registered with PLModMenu_Interface from C.",
      .mod_id = NULL,
      .default_enabled = g_enabled,
      .on_toggle = on_module_toggle,
      .config_count = (int)(sizeof(configs) / sizeof(configs[0])),
      .configs = configs,
      .on_config_changed = on_config_changed,
      .hide_in_hud_editor = false,
  };

  return menu->RegisterModule(&info);
}

PL_SHARED_EXPORT bool PLMod_Enable(void) {
  log_message("enable");
  return true;
}

PL_SHARED_EXPORT bool PLMod_Disable(void) {
  log_message("disable");
  return true;
}

PL_SHARED_EXPORT bool PLMod_Unload(void) {
  log_message("unload");
  return true;
}
