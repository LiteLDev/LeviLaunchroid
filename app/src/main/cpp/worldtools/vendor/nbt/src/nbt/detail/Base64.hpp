// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <cstdint>
#include <string>

namespace nbt::base64_utils {

namespace detail {

constexpr int8_t encodeLookup(int8_t c) {
    return "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"[c];
}

constexpr int8_t decodeLookup(int8_t c) {
    if (c >= 'A' && c <= 'Z') return c - 'A';
    if (c >= 'a' && c <= 'z') return c - 71;
    if (c >= '0' && c <= '9') return c + 4;
    if (c == '+') return 62;
    if (c == '/') return 63;
    return 64;
}

} // namespace detail

constexpr size_t getEncodeLength(size_t len) { return (len + 2 - ((len + 2) % 3)) / 3 * 4; }

constexpr size_t getEncodeLength(std::string_view str) { return getEncodeLength(str.length()); }

constexpr size_t getDecodeLength(std::string_view in) {
    uint8_t count      = 0;
    size_t  input_size = in.size();
    for (auto it = in.rbegin(); it != in.rend() && *it == '='; ++it) { ++count; }
    input_size -= count;     // remove padding size
    count       = 0;         // reset padding counter
    while (input_size % 4) { // redo padding
        input_size++;
        count++;
    }
    return ((6 * input_size) / 8) - count;
}

inline std::string encode(std::string_view str) {
    std::string result;
    result.reserve(getEncodeLength(str));
    int32_t i = 0;
    int32_t j = -6;
    for (auto& c : str) {
        i  = (i << 8) + static_cast<uint8_t>(c);
        j += 8;
        while (j >= 0) {
            result += detail::encodeLookup((i >> j) & 0x3F);
            j      -= 6;
        }
    }
    if (j > -6) { result += detail::encodeLookup(((i << 8) >> (j + 8)) & 0x3F); }
    while (result.size() % 4) { result.push_back('='); }
    return result;
}

inline std::string decode(std::string_view str) {
    const size_t input_size = str.size();
    std::string  out;

    if (input_size % 4 != 0) { return out; }

    const size_t output_size = getDecodeLength(str);

    out.resize(output_size);
    auto next_char = [&](size_t& pos) -> uint32_t {
        if (pos >= input_size) return 0;

        const char ch = str[pos++];
        if (ch == '=') { return 0; }
        return static_cast<uint32_t>(detail::decodeLookup(ch));
    };

    size_t pos = 0;
    size_t j   = 0;

    while (pos < input_size) {
        uint32_t c1 = next_char(pos);
        uint32_t c2 = next_char(pos);
        uint32_t c3 = next_char(pos);
        uint32_t c4 = next_char(pos);

        const uint32_t data = (c1 << 18) | (c2 << 12) | (c3 << 6) | c4;

        if (j < output_size) out[j++] = static_cast<char>((data >> 16) & 0xFF);
        if (j < output_size) out[j++] = static_cast<char>((data >> 8) & 0xFF);
        if (j < output_size) out[j++] = static_cast<char>(data & 0xFF);
    }

    if (j < output_size) { out.resize(j); }

    return out;
}

} // namespace nbt::base64_utils