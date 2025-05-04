#pragma once

#include <string>
#include <string_view>
#include <utility>
#include <vector>

namespace pl::utils {
[[nodiscard]] inline std::u8string str2u8str(std::string str) {
  std::u8string &tmp = *reinterpret_cast<std::u8string *>(&str);
  return {std::move(tmp)};
}

[[nodiscard]] inline std::string u8str2str(std::u8string str) {
  std::string &tmp = *reinterpret_cast<std::string *>(&str);
  return {std::move(tmp)};
}
} // namespace pl::utils