#include <android/log.h>

#include <cstdlib>
#include <string>
#include <string_view>

#include <pl/Mod.hpp>
#include <pl/ModMenu.hpp>

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

void onModuleToggle(std::string_view moduleId, bool enabled) {
  if (moduleId == kQuickToggleModule) {
    g_quickToggleEnabled = enabled;
  } else if (moduleId == kConfiguredModule) {
    g_configuredEnabled = enabled;
  }

  __android_log_print(ANDROID_LOG_INFO, kLogTag, "toggle %.*s = %s",
                      static_cast<int>(moduleId.size()), moduleId.data(),
                      enabled ? "true" : "false");
}

void onConfigChanged(std::string_view moduleId, std::string_view key,
                     std::string_view value) {
  if (moduleId != kConfiguredModule)
    return;

  const std::string safeValue(value);
  if (key == "strength") {
    g_strength = std::atoi(safeValue.c_str());
  } else if (key == "scale") {
    g_scale = std::strtof(safeValue.c_str(), nullptr);
  } else if (key == "mode") {
    g_mode = std::atoi(safeValue.c_str());
  }

  __android_log_print(ANDROID_LOG_INFO, kLogTag, "config %.*s.%.*s = %s",
                      static_cast<int>(moduleId.size()), moduleId.data(),
                      static_cast<int>(key.size()), key.data(),
                      safeValue.c_str());
}

void onButtonEvent(std::string_view buttonId, pl::modmenu::ButtonEvent event,
                   float value) {
  if (buttonId == kHoldButton) {
    g_holdButtonDown = event == pl::modmenu::ButtonEvent::Down;
  } else if (buttonId == kToggleButton &&
             event == pl::modmenu::ButtonEvent::StateChanged) {
    g_toggleButtonActive = value > 0.5f;
  }

  __android_log_print(ANDROID_LOG_INFO, kLogTag,
                      "button %.*s event %d value %.2f",
                      static_cast<int>(buttonId.size()), buttonId.data(),
                      static_cast<int>(event), value);
}

class CppLifecycleMod {
public:
  bool load(pl::mod::ModContext &context) {
    (void)context;
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
            .config("strength", "Strength",
                    pl::modmenu::ConfigType::SliderInt, "40", "0", "100")
            .config("scale", "Scale",
                    pl::modmenu::ConfigType::SliderFloat, "1.0", "0.5",
                    "2.0")
            .config("mode", "Mode", pl::modmenu::ConfigType::Radio, "1",
                    "Off,Normal,Aggressive")
            .config("accent", "Accent", pl::modmenu::ConfigType::Color,
                    "#4AE0A0")
            .onConfigChanged(onConfigChanged)
            .registerModule();

    const bool quickDropButtonRegistered =
        pl::modmenu::ButtonBuilder(kQuickDropButton, "CPP Quick Drop")
            .moduleId(kQuickToggleModule)
            .label("Q")
            .androidKeyCode(45)
            .behavior(pl::modmenu::ButtonBehavior::Click)
            .registerButton();

    const bool holdButtonRegistered =
        pl::modmenu::ButtonBuilder(kHoldButton, "CPP Hold Button")
            .moduleId(kQuickToggleModule)
            .label("H")
            .behavior(pl::modmenu::ButtonBehavior::Hold)
            .onEvent(onButtonEvent)
            .registerButton();

    const bool toggleButtonRegistered =
        pl::modmenu::ButtonBuilder(kToggleButton, "CPP Toggle Button")
            .moduleId(kQuickToggleModule)
            .label("T")
            .behavior(pl::modmenu::ButtonBehavior::Toggle)
            .stylePreset(pl::modmenu::ButtonStylePreset::Accent)
            .styleColors(0xCC24282CU, 0xFF4AE0A0U, 0x994AE0A0U)
            .textColor(0xFFFFFFFFU)
            .activeTextColor(0xFF000000U)
            .onEvent(onButtonEvent)
            .registerButton();

    const bool takeButtonRegistered =
        pl::modmenu::ButtonBuilder(kTakeButton, "CPP Take Button")
            .moduleId(kQuickToggleModule)
            .label("Take")
            .behavior(pl::modmenu::ButtonBehavior::Click)
            .sizeScale(2.0f, 1.0f)
            .onEvent(onButtonEvent)
            .registerButton();

    return quickRegistered && configuredRegistered &&
           quickDropButtonRegistered && holdButtonRegistered &&
           toggleButtonRegistered && takeButtonRegistered;
  }

  bool enable(pl::mod::ModContext &context) {
    (void)context;
    logInfo("enable");
    return true;
  }

  bool disable(pl::mod::ModContext &context) {
    (void)context;
    logInfo("disable");
    pl::modmenu::unregisterButton(kQuickDropButton);
    pl::modmenu::unregisterButton(kHoldButton);
    pl::modmenu::unregisterButton(kToggleButton);
    pl::modmenu::unregisterButton(kTakeButton);
    pl::modmenu::unregisterModule(kQuickToggleModule);
    pl::modmenu::unregisterModule(kConfiguredModule);
    return true;
  }

  bool unload(pl::mod::ModContext &context) {
    (void)context;
    logInfo("unload");
    return true;
  }
};

CppLifecycleMod g_mod;

} // namespace

PL_REGISTER_MOD(CppLifecycleMod, g_mod)
