#include <android/log.h>

#include <cstdlib>
#include <string>

#include <pl/cpp/ModMenu.hpp>
#include <pl/cpp/mod/RegisterHelper.hpp>

namespace {

constexpr const char *kLogTag = "LLMenuCppExample";
constexpr const char *kQuickToggleModule = "example.cpp_lifecycle.quick_toggle";
constexpr const char *kConfiguredModule = "example.cpp_lifecycle.configured";
constexpr const char *kQuickDropButton = "example.cpp_lifecycle.quick_drop_button";
constexpr const char *kHoldButton = "example.cpp_lifecycle.hold_button";
constexpr const char *kToggleButton = "example.cpp_lifecycle.toggle_button";
constexpr const char *kTakeButton = "example.cpp_lifecycle.take_button";

bool g_quickToggleEnabled = false;
bool g_configuredEnabled = true;
int g_strength = 40;
float g_scale = 1.0f;
int g_mode = 1;
bool g_holdButtonDown = false;
bool g_toggleButtonActive = false;

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

void onButtonEvent(const char *button_id, PLModMenu_ButtonEvent event,
                   float value) {
  if (!button_id)
    return;

  const std::string id(button_id);
  if (id == kHoldButton) {
    g_holdButtonDown = event == PL_BUTTON_EVENT_DOWN;
  } else if (id == kToggleButton &&
             event == PL_BUTTON_EVENT_STATE_CHANGED) {
    g_toggleButtonActive = value > 0.5f;
  }

  __android_log_print(ANDROID_LOG_INFO, kLogTag, "button %s event %d value %.2f",
                      button_id, static_cast<int>(event), value);
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

    const bool quickDropButtonRegistered =
        pl::modmenu::ButtonBuilder(kQuickDropButton, "CPP Quick Drop")
            .moduleId(kQuickToggleModule)
            .label("Q")
            .androidKeyCode(45)
            .behavior(PL_BUTTON_CLICK)
            .registerButton();

    const bool holdButtonRegistered =
        pl::modmenu::ButtonBuilder(kHoldButton, "CPP Hold Button")
            .moduleId(kQuickToggleModule)
            .label("H")
            .behavior(PL_BUTTON_HOLD)
            .onEvent(onButtonEvent)
            .registerButton();

    const bool toggleButtonRegistered =
        pl::modmenu::ButtonBuilder(kToggleButton, "CPP Toggle Button")
            .moduleId(kQuickToggleModule)
            .label("T")
            .behavior(PL_BUTTON_TOGGLE)
            .stylePreset(PL_BUTTON_STYLE_ACCENT)
            .styleColors(0xCC24282CU, 0xFF4AE0A0U, 0x994AE0A0U)
            .textColor(0xFFFFFFFFU)
            .activeTextColor(0xFF000000U)
            .onEvent(onButtonEvent)
            .registerButton();

    const bool takeButtonRegistered =
        pl::modmenu::ButtonBuilder(kTakeButton, "CPP Take Button")
            .moduleId(kQuickToggleModule)
            .label("Take")
            .behavior(PL_BUTTON_CLICK)
            .sizeScale(2.0f, 1.0f)
            .onEvent(onButtonEvent)
            .registerButton();

    return quickRegistered && configuredRegistered &&
           quickDropButtonRegistered && holdButtonRegistered &&
           toggleButtonRegistered && takeButtonRegistered;
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
