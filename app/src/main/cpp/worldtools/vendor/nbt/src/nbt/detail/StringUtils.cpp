// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/detail/StringUtils.hpp"
#include <cstdint>
#include <format>
#include <zlib.h>

namespace nbt::string_utils {

std::string& replaceAll(std::string& str, std::string_view oldValue, std::string_view newValue) {
    for (std::string::size_type pos(0); pos != std::string::npos; pos += newValue.length()) {
        if ((pos = str.find(oldValue, pos)) != std::string::npos) str.replace(pos, oldValue.length(), newValue);
        else break;
    }
    return str;
}

std::string replaceAll(std::string const& str, std::string_view oldValue, std::string_view newValue) {
    std::string ret = str;
    replaceAll(ret, oldValue, newValue);
    return ret;
}

bool isValidUTF8(std::string_view s) {
    auto       it  = s.begin();
    const auto end = s.end();

    while (it != end) {
        const uint8_t c         = static_cast<uint8_t>(*it++);
        size_t        remaining = 0;

        if (c <= 0x7F) {
            continue;
        } else if ((c & 0xE0) == 0xC0) {
            remaining = 1;
            if (c < 0xC2) return false;
        } else if ((c & 0xF0) == 0xE0) {
            remaining = 2;
        } else if ((c & 0xF8) == 0xF0) {
            remaining = 3;
            if (c > 0xF4) return false;
        } else {
            return false;
        }

        if (static_cast<size_t>(std::distance(it, end)) < remaining) return false;
        for (size_t i = 0; i < remaining; ++i, ++it) {
            if ((static_cast<uint8_t>(*it) & 0xC0) != 0x80) return false;
        }

        if (remaining == 1) {
            return true;
        } else if (remaining == 2) {
            const uint32_t cp = static_cast<uint32_t>((c & 0x0F) << 12)
                              | static_cast<uint32_t>((static_cast<uint8_t>(*(it - 2)) & 0x3F) << 6)
                              | static_cast<uint32_t>((static_cast<uint8_t>(*(it - 1)) & 0x3F));
            if (cp < 0x0800 || (cp >= 0xD800 && cp <= 0xDFFF)) return false;
        } else if (remaining == 3) {
            const uint32_t cp = static_cast<uint32_t>((c & 0x07) << 18)
                              | static_cast<uint32_t>((static_cast<uint8_t>(*(it - 3)) & 0x3F) << 12)
                              | static_cast<uint32_t>(((static_cast<uint8_t>(*(it - 2)) & 0x3F) << 6))
                              | static_cast<uint32_t>((static_cast<uint8_t>(*(it - 1)) & 0x3F));
            if (cp < 0x10000 || cp > 0x10FFFF) return false;
        }
    }
    return true;
}

std::string dumpString(std::string_view content, bool ensureAscii) {
    std::string result;
    result.reserve(static_cast<size_t>(static_cast<double>(content.size()) * 1.2));

    if (ensureAscii) {
        auto it = content.begin();
        while (it != content.end()) {
            const uint8_t c         = static_cast<uint8_t>(*it++);
            uint32_t      codepoint = c;
            size_t        char_len  = 1;

            if (c >= 0xC0) {
                char_len = (c >= 0xF0) ? 4 : (c >= 0xE0) ? 3 : 2;

                codepoint = c & (0xFFu >> (char_len + 1));
                for (size_t i = 1; i < char_len; ++i) {
                    codepoint = (codepoint << 6) | (static_cast<uint8_t>(*it++) & 0x3F);
                }
            }

            switch (codepoint) {
            case '"':
                result += "\\\"";
                break;
            case '\\':
                result += "\\\\";
                break;
            case '\b':
                result += "\\b";
                break;
            case '\f':
                result += "\\f";
                break;
            case '\n':
                result += "\\n";
                break;
            case '\r':
                result += "\\r";
                break;
            case '\t':
                result += "\\t";
                break;
            default:
                if (codepoint <= 0x1F) {
                    result += std::format("\\u{:04x}", codepoint);
                } else if (codepoint <= 0x7F) {
                    result += static_cast<char>(codepoint);
                } else {
                    if (codepoint <= 0xFFFF) {
                        result += std::format("\\u{:04x}", codepoint);
                    } else {
                        codepoint -= 0x10000;
                        result +=
                            std::format("\\u{:04x}\\u{:04x}", 0xD800 + (codepoint >> 10), 0xDC00 + (codepoint & 0x3FF));
                    }
                }
            }
        }
    } else {
        for (char c : content) {
            switch (c) {
            case '"':
                result += "\\\"";
                break;
            case '\\':
                result += "\\\\";
                break;
            case '\b':
                result += "\\b";
                break;
            case '\f':
                result += "\\f";
                break;
            case '\n':
                result += "\\n";
                break;
            case '\r':
                result += "\\r";
                break;
            case '\t':
                result += "\\t";
                break;
            default:
                if (static_cast<uint8_t>(c) <= 0x1F) {
                    result.append(std::format("\\u{:04x}", c));
                } else {
                    result.push_back(c);
                }
            }
        }
    }

    return result;
}

} // namespace nbt::string_utils
