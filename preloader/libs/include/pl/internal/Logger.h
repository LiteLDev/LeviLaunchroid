#pragma once

#include <android/log.h>
#include <fmt/core.h>
#include <string>
#include <utility>

namespace pl {
namespace log {

inline constexpr const char *LOG_TAG = "PreLoader";

template <typename... Args>
void Info(fmt::format_string<Args...> fmt, Args &&...args) {
  __android_log_print(ANDROID_LOG_INFO, "LeviLogger", "[%s] %s", LOG_TAG,
                      fmt::format(fmt, std::forward<Args>(args)...).c_str());
}

template <typename... Args>
void Warn(fmt::format_string<Args...> fmt, Args &&...args) {
  __android_log_print(ANDROID_LOG_WARN, "LeviLogger", "[%s] %s", LOG_TAG,
                      fmt::format(fmt, std::forward<Args>(args)...).c_str());
}

template <typename... Args>
void Error(fmt::format_string<Args...> fmt, Args &&...args) {
  __android_log_print(ANDROID_LOG_ERROR, "LeviLogger", "[%s] %s", LOG_TAG,
                      fmt::format(fmt, std::forward<Args>(args)...).c_str());
}

} // namespace log
} // namespace pl