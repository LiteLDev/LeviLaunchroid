// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <string>

namespace nbt::string_utils {

std::string& replaceAll(std::string& str, std::string_view oldValue, std::string_view newValue);

[[nodiscard]] std::string replaceAll(std::string const& str, std::string_view oldValue, std::string_view newValue);

[[nodiscard]] constexpr size_t doHash(std::string_view x) {
    size_t           hash  = 0xcbf29ce484222325;
    constexpr size_t prime = 0x100000001b3;
    for (char c : x) {
        hash ^= static_cast<size_t>(c);
        hash *= prime;
    }
    return hash;
}

[[nodiscard]] bool isValidUTF8(std::string_view s);

[[nodiscard]] std::string dumpString(std::string_view content, bool ensureAscii);

} // namespace nbt::string_utils
