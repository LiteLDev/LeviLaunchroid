#include <android/log.h>

#include <stdbool.h>
#include <stdlib.h>
#include <string.h>

#include <pl/c/Macro.h>
#include <pl/c/Mod.h>
#include <pl/c/PreloaderModMenu.h>

static const char *kLogTag = "LLMenuCExample";
static const char *kModuleId = "example.c_lifecycle.raw_api";
static const char *kQuickDropButtonId = "example.c_lifecycle.quick_drop_button";

static const unsigned char kRawApiPngIcon[] = {
    0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
    0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x20,
    0x08, 0x06, 0x00, 0x00, 0x00, 0x73, 0x7A, 0x7A, 0xF4, 0x00, 0x00, 0x00,
    0x82, 0x49, 0x44, 0x41, 0x54, 0x78, 0xDA, 0x63, 0x60, 0x18, 0x05, 0x03,
    0x0C, 0x18, 0x09, 0x29, 0xF0, 0x7A, 0xB0, 0xE0, 0x3F, 0xA5, 0x96, 0x6C,
    0x53, 0x48, 0x60, 0x24, 0xD9, 0x01, 0xD4, 0xB0, 0x98, 0x18, 0x87, 0x30,
    0xD1, 0xCB, 0x72, 0x5C, 0xE6, 0x32, 0xD1, 0xCB, 0x72, 0x5C, 0xE6, 0x33,
    0x0D, 0xAA, 0x44, 0x88, 0xCD, 0xF7, 0xC7, 0x0D, 0x0B, 0x71, 0x6A, 0xB6,
    0x3C, 0xDF, 0x4F, 0xB2, 0x3A, 0xF4, 0xF4, 0x30, 0xE0, 0x21, 0x30, 0xEA,
    0x80, 0x51, 0x07, 0x8C, 0x3A, 0x60, 0xD4, 0x01, 0xA3, 0x0E, 0x18, 0x75,
    0x00, 0x0B, 0x25, 0x9A, 0xF1, 0xD5, 0x80, 0xA3, 0x51, 0x40, 0x76, 0xAB,
    0x18, 0x57, 0x93, 0x8C, 0x9C, 0xE0, 0xC6, 0xD6, 0x10, 0x41, 0x6F, 0x9C,
    0x0E, 0xBE, 0x10, 0xA0, 0x75, 0xC3, 0x14, 0xBD, 0x69, 0xCE, 0x44, 0x6A,
    0x47, 0x82, 0xDA, 0xFD, 0x82, 0x01, 0xEF, 0x19, 0x8D, 0x02, 0x00, 0xFF,
    0x75, 0x30, 0x2E, 0x2F, 0xBC, 0x9B, 0x9A, 0x00, 0x00, 0x00, 0x00, 0x49,
    0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82,
};

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

static void on_button_event(const char *button_id,
                            PLModMenu_ButtonEvent event, float value) {
  if (!button_id || strcmp(button_id, kQuickDropButtonId) != 0) {
    return;
  }

  __android_log_print(ANDROID_LOG_INFO, kLogTag,
                      "button %s event %d value %.2f", button_id, event,
                      value);
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

  if (!menu->RegisterModule(&info)) {
    return false;
  }

  if (menu->RegisterButton) {
    PLModMenu_ButtonInfo button = {
        .button_id = kQuickDropButtonId,
        .module_id = kModuleId,
        .display_name = "C Quick Drop Button",
        .mod_id = NULL,
        .label = "Drop",
        .android_key_code = 45,
        .behavior = PL_BUTTON_CLICK,
        .default_visible = true,
        .on_event = on_button_event,
        .preset = PL_BUTTON_STYLE_ACCENT,
        .normal_bg_color = 0xCC24282CU,
        .active_bg_color = 0xFF4AE0A0U,
        .border_color = 0x994AE0A0U,
        .text_color = 0xFFFFFFFFU,
        .active_text_color = 0xFF000000U,
        .width_scale = 1.9f,
        .height_scale = 1.0f,
        .icon_data = kRawApiPngIcon,
        .icon_data_size = (int)sizeof(kRawApiPngIcon),
        .icon_format = PL_BUTTON_ICON_PNG,
        .hide_label_when_icon_present = true,
    };
    return menu->RegisterButton(&button);
  }

  return true;
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
