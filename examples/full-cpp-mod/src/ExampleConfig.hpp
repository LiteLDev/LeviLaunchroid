#pragma once

#include <optional>
#include <string>
#include <string_view>

#include <pl/cpp/Config.hpp>

namespace fullcppmod {

enum class DisplayMode {
  Compact,
  Detailed,
  Debug,
};

struct ExampleConfig {
  int version = 1;
  bool moduleEnabled = true;
  bool showOverlay = true;
  int opacity = 80;
  double scale = 1.0;
  DisplayMode mode = DisplayMode::Compact;
  std::string accentColor = "#4AE0A0";
};

inline constexpr int kMinOpacity = 0;
inline constexpr int kMaxOpacity = 100;
inline constexpr double kMinScale = 0.5;
inline constexpr double kMaxScale = 2.0;
inline constexpr std::string_view kModeMenuOptions = "Compact,Detailed,Debug";

} // namespace fullcppmod

namespace pl::config {

template <> struct Schema<fullcppmod::ExampleConfig> {
  static constexpr std::string_view title = "Full C++ Lifecycle Mod Example";
  static constexpr std::string_view description =
      "Typed config shared by the C++ lifecycle mod and host schema generator.";

  static constexpr FieldSchema field(std::string_view name) {
    if (name == "version") {
      return {"Version", "Config schema version managed by the mod.",
              std::nullopt, std::nullopt, true};
    }
    if (name == "moduleEnabled") {
      return {"Module Enabled",
              "Default enabled state for the Mod Menu module.", std::nullopt,
              std::nullopt, false};
    }
    if (name == "showOverlay") {
      return {"Show Overlay",
              "Controls whether the example overlay should be shown.",
              std::nullopt, std::nullopt, false};
    }
    if (name == "opacity") {
      return {"Opacity", "Overlay opacity percentage.",
              fullcppmod::kMinOpacity, fullcppmod::kMaxOpacity, false};
    }
    if (name == "scale") {
      return {"Scale", "Overlay scale multiplier.", fullcppmod::kMinScale,
              fullcppmod::kMaxScale, false};
    }
    if (name == "mode") {
      return {"Display Mode", "Overlay detail level.", std::nullopt,
              std::nullopt, false};
    }
    if (name == "accentColor") {
      return {"Accent Color",
              "Overlay accent color in #RRGGBB or #AARRGGBB form.",
              std::nullopt, std::nullopt, false};
    }
    return {};
  }
};

} // namespace pl::config
