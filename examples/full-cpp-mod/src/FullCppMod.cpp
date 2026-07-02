#include "ExampleConfig.hpp"

#include <algorithm>
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
    const bool registered =
        pl::modmenu::ModuleBuilder(kModuleId, "Full C++ Config Demo")
            .description(
                "Pure C++ lifecycle module with persistent typed config.")
            .defaultEnabled(snapshot.moduleEnabled)
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

    if (const auto self = pl::mod::NativeMod::current()) {
      if (registered) {
        self->getLogger().info("Registered Mod Menu module {}", kModuleId);
      } else {
        self->getLogger().error("Failed to register Mod Menu module {}",
                                kModuleId);
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

  void handleModuleToggle(const char *moduleId, bool enabled) {
    if (!matchesModule(moduleId)) {
      return;
    }

    std::lock_guard lock(configMutex);
    if (!configFile) {
      return;
    }
    configFile->value().moduleEnabled = enabled;
    saveConfigLocked("module toggle");
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

  void unregisterModule() {
    const auto menu = GetPreloaderModMenu();
    if (menu && menu->UnregisterModule) {
      menu->UnregisterModule(kModuleId);
    }
  }
};

} // namespace

PL_REGISTER_MOD(FullCppMod, FullCppMod::instance())
