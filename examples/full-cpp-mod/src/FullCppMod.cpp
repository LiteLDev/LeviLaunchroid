#include "ExampleConfig.hpp"

#include <algorithm>
#include <atomic>
#include <cerrno>
#include <cstdlib>
#include <cstring>
#include <limits>
#include <mutex>
#include <optional>
#include <sstream>
#include <string>
#include <string_view>

#include <pl/c/PreloaderModMenu.h>
#include <pl/cpp/ModMenu.hpp>
#include <pl/cpp/mod/NativeMod.hpp>
#include <pl/cpp/mod/RegisterHelper.hpp>

namespace {

using fullcppmod::DisplayMode;
using fullcppmod::ExampleConfig;

constexpr const char *kModuleId = "full_cpp_mod.hud";
constexpr const char *kShowOverlayKey = "showOverlay";
constexpr const char *kOpacityKey = "opacity";
constexpr const char *kScaleKey = "scale";
constexpr const char *kModeKey = "mode";
constexpr const char *kAccentColorKey = "accentColor";
constexpr const char *kQuickDropButtonId = "full_cpp_mod.quick_drop_button";
constexpr const char *kHoldButtonId = "full_cpp_mod.hold_button";
constexpr const char *kToggleButtonId = "full_cpp_mod.toggle_button";
constexpr const char *kTakeButtonId = "full_cpp_mod.take_button";

static constexpr unsigned char kPngButtonIcon[] = {
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

static constexpr unsigned char kWebpButtonIcon[] = {
    0x52, 0x49, 0x46, 0x46, 0xAA, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50,
    0x56, 0x50, 0x38, 0x4C, 0x9D, 0x00, 0x00, 0x00, 0x2F, 0x1F, 0xC0, 0x07,
    0x10, 0x1F, 0x20, 0x24, 0x20, 0xDC, 0xF1, 0xBF, 0x78, 0xE7, 0x86, 0x40,
    0x80, 0x30, 0xC3, 0xFF, 0x21, 0x81, 0x00, 0x61, 0xC8, 0x9D, 0x7F, 0x02,
    0x01, 0xC2, 0x86, 0xFF, 0x38, 0x19, 0x02, 0xE0, 0x5F, 0xAD, 0x12, 0x8E,
    0x6A, 0xDB, 0x76, 0x9A, 0xBB, 0xD6, 0x17, 0x80, 0x24, 0x54, 0x74, 0x5F,
    0x7D, 0x5E, 0x2F, 0x90, 0x48, 0x00, 0x3B, 0x61, 0xC4, 0x38, 0x8D, 0x7A,
    0x59, 0x89, 0x83, 0x88, 0xFE, 0x2B, 0x6C, 0xDB, 0xB6, 0x51, 0xBA, 0x77,
    0x75, 0x06, 0xFE, 0x89, 0xF7, 0x72, 0x62, 0x5E, 0x16, 0xFB, 0x7C, 0xDC,
    0x72, 0xAA, 0x96, 0x96, 0x8B, 0x01, 0xC6, 0x81, 0x7C, 0x5A, 0xC0, 0x7C,
    0x27, 0x27, 0x1B, 0x78, 0xB7, 0x39, 0xB9, 0x00, 0x0B, 0x43, 0xF2, 0x6B,
    0x30, 0x30, 0x2C, 0xF8, 0x3C, 0x11, 0xF1, 0x69, 0xC6, 0x3B, 0x43, 0xA7,
    0x99, 0xAC, 0x13, 0x85, 0xE0, 0x28, 0x0F, 0x29, 0xAE, 0x86, 0x75, 0x9B,
    0xBC, 0xFB, 0xE2, 0x1F, 0xFA, 0x2F, 0xF9, 0xA7, 0xFE, 0x5B, 0x3D, 0xA8,
    0x17, 0xF1, 0xA4, 0xDE, 0xC4, 0xE3, 0x2F, 0x72, 0x01, 0x00,
};

std::string_view viewOrEmpty(const char *value) {
  return value ? std::string_view(value) : std::string_view();
}

bool matchesModule(const char *moduleId) {
  return moduleId && std::string_view(moduleId) == kModuleId;
}

bool parseBool(std::string_view value, bool fallback) {
  if (value == "true" || value == "1" || value == "on" ||
      value == "enabled") {
    return true;
  }
  if (value == "false" || value == "0" || value == "off" ||
      value == "disabled") {
    return false;
  }
  return fallback;
}

int parseInt(std::string_view value, int fallback) {
  std::string text(value);
  char *end = nullptr;
  errno = 0;
  const long parsed = std::strtol(text.c_str(), &end, 10);
  if (end == text.c_str() || *end != '\0' || errno == ERANGE) {
    return fallback;
  }
  if (parsed < std::numeric_limits<int>::min() ||
      parsed > std::numeric_limits<int>::max()) {
    return fallback;
  }
  return static_cast<int>(parsed);
}

double parseDouble(std::string_view value, double fallback) {
  std::string text(value);
  char *end = nullptr;
  errno = 0;
  const double parsed = std::strtod(text.c_str(), &end);
  if (end == text.c_str() || *end != '\0' || errno == ERANGE) {
    return fallback;
  }
  return parsed;
}

bool isHexDigit(char value) {
  return (value >= '0' && value <= '9') || (value >= 'a' && value <= 'f') ||
         (value >= 'A' && value <= 'F');
}

bool isValidColor(std::string_view value) {
  if ((value.size() != 7 && value.size() != 9) || value.front() != '#') {
    return false;
  }
  return std::all_of(value.begin() + 1, value.end(), isHexDigit);
}

int modeToIndex(DisplayMode mode) {
  switch (mode) {
  case DisplayMode::Compact:
    return 0;
  case DisplayMode::Detailed:
    return 1;
  case DisplayMode::Debug:
    return 2;
  }
  return 0;
}

DisplayMode modeFromIndex(int index, DisplayMode fallback) {
  switch (index) {
  case 0:
    return DisplayMode::Compact;
  case 1:
    return DisplayMode::Detailed;
  case 2:
    return DisplayMode::Debug;
  default:
    return fallback;
  }
}

DisplayMode parseMode(std::string_view value, DisplayMode fallback) {
  if (value == "Compact") {
    return DisplayMode::Compact;
  }
  if (value == "Detailed") {
    return DisplayMode::Detailed;
  }
  if (value == "Debug") {
    return DisplayMode::Debug;
  }
  return modeFromIndex(parseInt(value, modeToIndex(fallback)), fallback);
}

std::string boolToMenuValue(bool value) { return value ? "true" : "false"; }

std::string doubleToMenuValue(double value) {
  std::ostringstream stream;
  stream << value;
  return stream.str();
}

void normalizeConfig(ExampleConfig &config) {
  const ExampleConfig defaults;
  config.version = defaults.version;
  config.opacity =
      std::clamp(config.opacity, fullcppmod::kMinOpacity,
                 fullcppmod::kMaxOpacity);
  config.scale =
      std::clamp(config.scale, fullcppmod::kMinScale, fullcppmod::kMaxScale);
  config.mode = modeFromIndex(modeToIndex(config.mode), defaults.mode);
  if (!isValidColor(config.accentColor)) {
    config.accentColor = defaults.accentColor;
  }
}

class FullCppMod {
public:
  static FullCppMod &instance() {
    static FullCppMod mod;
    return mod;
  }

  bool load() {
    const auto self = pl::mod::NativeMod::current();
    if (!self) {
      return false;
    }

    std::lock_guard lock(configMutex);
    configFile.emplace(ExampleConfig{});
    if (!configFile->load()) {
      self->getLogger().error("Failed to load config");
      configFile.reset();
      return false;
    }

    normalizeConfig(configFile->value());
    if (!configFile->save()) {
      self->getLogger().error("Failed to persist normalized config");
      configFile.reset();
      return false;
    }

    self->getLogger().info("Loaded config from {}",
                           configFile->configPath().string());
    return true;
  }

  bool enable() {
    const auto snapshot = snapshotConfig();
    const bool moduleRegistered =
        pl::modmenu::ModuleBuilder(kModuleId, "Full C++ Config Demo")
            .description(
                "Pure C++ lifecycle module with persistent typed config.")
            .defaultEnabled(true)
            .onToggle(onModuleToggle)
            .config(kShowOverlayKey, "Show Overlay", PL_CONFIG_TOGGLE,
                    boolToMenuValue(snapshot.showOverlay))
            .config(kOpacityKey, "Opacity", PL_CONFIG_SLIDER_INT,
                    std::to_string(snapshot.opacity),
                    std::to_string(fullcppmod::kMinOpacity),
                    std::to_string(fullcppmod::kMaxOpacity))
            .config(kScaleKey, "Scale", PL_CONFIG_SLIDER_FLOAT,
                    doubleToMenuValue(snapshot.scale),
                    doubleToMenuValue(fullcppmod::kMinScale),
                    doubleToMenuValue(fullcppmod::kMaxScale))
            .config(kModeKey, "Display Mode", PL_CONFIG_RADIO,
                    std::to_string(modeToIndex(snapshot.mode)),
                    std::string(fullcppmod::kModeMenuOptions))
            .config(kAccentColorKey, "Accent Color", PL_CONFIG_COLOR,
                    snapshot.accentColor)
            .onConfigChanged(onConfigChanged)
            .registerModule();

    const bool quickDropButtonRegistered =
        moduleRegistered &&
        pl::modmenu::ButtonBuilder(kQuickDropButtonId, "Full C++ Quick Drop")
            .moduleId(kModuleId)
            .label("Q")
            .androidKeyCode(45)
            .behavior(PL_BUTTON_CLICK)
            .pngIcon(kPngButtonIcon, static_cast<int>(sizeof(kPngButtonIcon)))
            .registerButton();

    const bool holdButtonRegistered =
        moduleRegistered &&
        pl::modmenu::ButtonBuilder(kHoldButtonId, "Full C++ Hold Demo")
            .moduleId(kModuleId)
            .label("H")
            .behavior(PL_BUTTON_HOLD)
            .webpIcon(kWebpButtonIcon,
                      static_cast<int>(sizeof(kWebpButtonIcon)))
            .onEvent(onButtonEvent)
            .registerButton();

    const bool toggleButtonRegistered =
        moduleRegistered &&
        pl::modmenu::ButtonBuilder(kToggleButtonId, "Full C++ Toggle Demo")
            .moduleId(kModuleId)
            .label("T")
            .behavior(PL_BUTTON_TOGGLE)
            .stylePreset(PL_BUTTON_STYLE_ACCENT)
            .styleColors(0xCC24282CU, 0xFF4AE0A0U, 0x994AE0A0U)
            .textColor(0xFFFFFFFFU)
            .activeTextColor(0xFF000000U)
            .svgIcon(R"svg(<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
              <rect x="10" y="10" width="44" height="44" rx="10" fill="#4AE0A0"/>
              <path d="M24 21v22l18-11z" fill="#111111"/>
            </svg>)svg")
            .onEvent(onButtonEvent)
            .registerButton();

    const bool takeButtonRegistered =
        moduleRegistered &&
        pl::modmenu::ButtonBuilder(kTakeButtonId, "Full C++ Take Demo")
            .moduleId(kModuleId)
            .label("Take")
            .behavior(PL_BUTTON_CLICK)
            .sizeScale(2.0f, 1.0f)
            .onEvent(onButtonEvent)
            .registerButton();

    const bool registered = moduleRegistered && quickDropButtonRegistered &&
                            holdButtonRegistered && toggleButtonRegistered &&
                            takeButtonRegistered;
    if (const auto self = pl::mod::NativeMod::current()) {
      if (registered) {
        self->getLogger().info("Registered Mod Menu module {} and demo buttons",
                               kModuleId);
      } else {
        self->getLogger().error(
            "Failed to register Mod Menu module/buttons: module={} quickDrop={} "
            "hold={} toggle={} take={}",
            moduleRegistered, quickDropButtonRegistered, holdButtonRegistered,
            toggleButtonRegistered, takeButtonRegistered);
      }
    }
    return registered;
  }

  bool disable() {
    unregisterModule();
    if (const auto self = pl::mod::NativeMod::current()) {
      self->getLogger().info("Disabled");
    }
    return true;
  }

  bool unload() {
    unregisterModule();
    {
      std::lock_guard lock(configMutex);
      configFile.reset();
    }
    if (const auto self = pl::mod::NativeMod::current()) {
      self->getLogger().info("Unloaded");
    }
    return true;
  }

private:
  std::mutex configMutex;
  std::optional<pl::config::ConfigFile<ExampleConfig>> configFile;
  std::atomic_bool holdButtonDown{false};
  std::atomic_bool toggleButtonActive{false};

  ExampleConfig snapshotConfig() {
    std::lock_guard lock(configMutex);
    if (!configFile) {
      return ExampleConfig{};
    }
    auto snapshot = configFile->value();
    normalizeConfig(snapshot);
    return snapshot;
  }

  static void onModuleToggle(const char *moduleId, bool enabled) {
    instance().handleModuleToggle(moduleId, enabled);
  }

  static void onConfigChanged(const char *moduleId, const char *key,
                              const char *value) {
    instance().handleConfigChanged(moduleId, key, value);
  }

  static void onButtonEvent(const char *buttonId, PLModMenu_ButtonEvent event,
                            float value) {
    instance().handleButtonEvent(buttonId, event, value);
  }

  void handleModuleToggle(const char *moduleId, bool enabled) {
    if (!matchesModule(moduleId)) {
      return;
    }

    if (const auto self = pl::mod::NativeMod::current()) {
      self->getLogger().info("Module {} {}", moduleId,
                             enabled ? "enabled" : "disabled");
    }
  }

  void handleConfigChanged(const char *moduleId, const char *key,
                           const char *value) {
    if (!matchesModule(moduleId) || !key) {
      return;
    }

    const std::string_view configKey(key);
    const std::string_view configValue = viewOrEmpty(value);
    std::lock_guard lock(configMutex);
    if (!configFile) {
      return;
    }

    auto &config = configFile->value();
    if (configKey == kShowOverlayKey) {
      config.showOverlay = parseBool(configValue, config.showOverlay);
    } else if (configKey == kOpacityKey) {
      config.opacity =
          std::clamp(parseInt(configValue, config.opacity),
                     fullcppmod::kMinOpacity, fullcppmod::kMaxOpacity);
    } else if (configKey == kScaleKey) {
      config.scale =
          std::clamp(parseDouble(configValue, config.scale),
                     fullcppmod::kMinScale, fullcppmod::kMaxScale);
    } else if (configKey == kModeKey) {
      config.mode = parseMode(configValue, config.mode);
    } else if (configKey == kAccentColorKey) {
      if (isValidColor(configValue)) {
        config.accentColor = std::string(configValue);
      }
    } else {
      return;
    }

    saveConfigLocked(configKey);
  }

  bool saveConfigLocked(std::string_view reason) {
    if (!configFile) {
      return false;
    }

    normalizeConfig(configFile->value());
    const bool saved = configFile->save();
    if (const auto self = pl::mod::NativeMod::current()) {
      if (saved) {
        self->getLogger().info("Persisted config after {}", reason);
      } else {
        self->getLogger().warn("Failed to persist config after {}", reason);
      }
    }
    return saved;
  }

  void handleButtonEvent(const char *buttonId, PLModMenu_ButtonEvent event,
                         float value) {
    if (!buttonId) {
      return;
    }

    const std::string_view id(buttonId);
    if (id == kHoldButtonId) {
      if (event == PL_BUTTON_EVENT_DOWN) {
        holdButtonDown = true;
      } else if (event == PL_BUTTON_EVENT_UP) {
        holdButtonDown = false;
      }
    } else if (id == kToggleButtonId &&
               event == PL_BUTTON_EVENT_STATE_CHANGED) {
      toggleButtonActive = value > 0.5f;
    } else if (id == kTakeButtonId) {
      // Logged below.
    } else {
      return;
    }

    if (const auto self = pl::mod::NativeMod::current()) {
      self->getLogger().info(
          "External button {} event={} value={} holdDown={} toggleActive={}",
          buttonId, static_cast<int>(event), value, holdButtonDown.load(),
          toggleButtonActive.load());
    }
  }

  void unregisterModule() {
    const auto menu = GetPreloaderModMenu();
    if (menu && menu->UnregisterButton) {
      menu->UnregisterButton(kQuickDropButtonId);
      menu->UnregisterButton(kHoldButtonId);
      menu->UnregisterButton(kToggleButtonId);
      menu->UnregisterButton(kTakeButtonId);
    }
    if (menu && menu->UnregisterModule) {
      menu->UnregisterModule(kModuleId);
    }
  }
};

} // namespace

PL_REGISTER_MOD(FullCppMod, FullCppMod::instance())
