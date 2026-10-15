// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/detail/SnbtDeserializer.hpp"
#include "nbt/detail/Base64.hpp"
#include "nbt/detail/StringUtils.hpp"
#include "nbt/types/ByteArrayTag.hpp"
#include "nbt/types/ByteTag.hpp"
#include "nbt/types/CompoundTag.hpp"
#include "nbt/types/CompoundTagVariant.hpp"
#include "nbt/types/DoubleTag.hpp"
#include "nbt/types/EndTag.hpp"
#include "nbt/types/FloatTag.hpp"
#include "nbt/types/IntArrayTag.hpp"
#include "nbt/types/IntTag.hpp"
#include "nbt/types/ListTag.hpp"
#include "nbt/types/LongArrayTag.hpp"
#include "nbt/types/LongTag.hpp"
#include "nbt/types/ShortTag.hpp"
#include "nbt/types/StringTag.hpp"
#include <charconv>
#include <functional>
#include <limits>

namespace nbt {

namespace {

static constexpr auto CHAR_EOF = static_cast<char>(std::char_traits<char>::eof());

bool ignoreComment(std::string_view& s) noexcept {
    size_t i = 0;
    switch (s[i++]) {
    case '*': {
        while (i < s.size()) {
            switch (s[i++]) {
            case CHAR_EOF:
            case '\0':
                return false;
            case '*': {
                switch (s[i]) {
                case '/':
                    s.remove_prefix(std::min(i + 1, s.size()));
                    return true;
                default:
                    continue;
                }
            }
            default:
                continue;
            }
        }
        break;
    }
    case '/': {
        while (i < s.size()) {
            switch (s[i++]) {
            case CHAR_EOF:
            case '\0':
                return false;
            case '\r':
            case '\n': {
                s.remove_prefix(std::min(i + 1, s.size()));
                return true;
            }
            default:
                continue;
            }
        }
        break;
    }
    default: {
        while (i < s.size()) {
            switch (s[i++]) {
            case CHAR_EOF:
            case '\0':
                s.remove_prefix(std::min(i, s.size()));
                return false;

            default:
                break;
            }
        }
        break;
    }
    }
    return false;
}

void scanSpaces(std::string_view& s) noexcept {
    size_t i = 0;
    while (i <= s.size() && std::isspace(s[i++])) {}
    s.remove_prefix(std::min(i - 1, s.size()));
}

bool skipWhitespace(std::string_view& s) {
    scanSpaces(s);
    while (s.starts_with('/')) {
        s.remove_prefix(1);
        if (!ignoreComment(s)) { return false; }
        scanSpaces(s);
    }
    return true;
}

char get(std::string_view& s) {
    if (s.empty()) return '\0';
    char c = s.front();
    s.remove_prefix(1);
    return c;
}

inline std::optional<std::string> parseNumberStr(std::string_view str, size_t& n, bool& isInt) noexcept {
    n     = 0;
    isInt = true;
    if (str.empty()) return std::nullopt;

    auto it  = str.begin();
    auto end = str.end();

    while (it != end && std::isspace(static_cast<uint8_t>(*it))) ++it;
    auto start = it;
    if (it == end) return std::nullopt;

    if (*it == '+' || *it == '-') ++it;

    bool hex = false, bin = false;
    if (it + 1 < end && *it == '0') {
        char c = *(it + 1);
        if (c == 'x' || c == 'X') {
            hex  = true;
            it  += 2;
        } else if (c == 'b' || c == 'B') {
            bin  = true;
            it  += 2;
        }
    }

    auto digit_start = it;
    auto scan_digit  = [&] {
        if (hex) {
            while (it != end && (std::isxdigit(static_cast<uint8_t>(*it)) || *it == '_')) ++it;
        } else if (bin) {
            while (it != end && (*it == '0' || *it == '1' || *it == '_')) ++it;
        } else {
            while (it != end && (std::isdigit(static_cast<uint8_t>(*it)) || *it == '_')) ++it;
        }
    };
    scan_digit();

    if (!hex && !bin && it != end && *it == '.') {
        isInt = false;
        ++it;
        scan_digit();
    }

    bool ok = false;
    for (auto tmp = digit_start; tmp != it; ++tmp) {
        if (*tmp != '_') {
            ok = true;
            break;
        }
    }

    n           = static_cast<size_t>(it - str.begin());
    auto result = std::string(str.substr(static_cast<size_t>(start - str.begin()), static_cast<size_t>(it - start)));
    result.erase(std::remove_if(result.begin(), result.end(), [](uint8_t c) { return c == '_'; }), result.end());

    if (result == "0b" || result == "0B") {
        n     = 1;
        isInt = true;
        return "0";
    }

    if (!ok) { return std::nullopt; }

    return result;
}

inline std::string parseNumberMark(std::string_view& sv) noexcept {
    auto first = std::find_if_not(sv.begin(), sv.end(), [](uint8_t c) { return std::isspace(c); });
    if (first == sv.end()) { return {}; }
    auto last   = std::find_if(first, sv.end(), [](uint8_t c) { return c == ',' || c == '}' || c == ']'; });
    auto result = std::string(sv.substr(static_cast<size_t>(first - sv.begin()), static_cast<size_t>(last - first)));
    for (char& c : result) {
        if (static_cast<uint8_t>(c) >= 'A' && static_cast<uint8_t>(c) <= 'Z') { c |= 0x20; }
    }
    while (std::isspace(result.back())) { result.pop_back(); }
    sv.remove_prefix(static_cast<size_t>(first - sv.begin()));
    return result;
}

inline std::optional<std::variant<uint64_t, int64_t>> parseInt(std::string_view s) noexcept {
    bool negative = false;
    if (s.front() == '+') {
        s.remove_prefix(1);
        if (s.empty()) return std::nullopt;
    }
    if (s.front() == '-') {
        negative = true;
        s.remove_prefix(1);
        if (s.empty()) return std::nullopt;
    }
    int base = 10;
    if (s.size() >= 2 && s[0] == '0') {
        char c = s[1];
        if (c == 'x' || c == 'X') {
            base = 16;
            s.remove_prefix(2);
        } else if (c == 'b' || c == 'B') {
            base = 2;
            s.remove_prefix(2);
        }
    }
    uint64_t v     = 0;
    auto [ptr, ec] = std::from_chars(s.data(), s.data() + s.size(), v, base);
    if (ec != std::errc{} || ptr != s.data() + s.size()) { return std::nullopt; }
    return negative ? -static_cast<int64_t>(v) : v;
}

template <class R, class T>
std::optional<CompoundTagVariant> checkRange(std::string_view str) {
    if constexpr (std::is_floating_point_v<T>) {
        auto num = static_cast<T>(std::stod(std::string(str)));
        if (std::numeric_limits<T>::lowest() <= num && num <= std::numeric_limits<T>::max()) { return R(num); }
    } else {
        if (auto intValue = parseInt(str)) {
            return std::visit(
                [](auto val) -> std::optional<CompoundTagVariant> {
                    auto num = static_cast<T>(val);
                    if (std::numeric_limits<T>::lowest() <= num && num <= std::numeric_limits<T>::max()) {
                        return R(num);
                    }
                    return std::nullopt;
                },
                *intValue
            );
        }
    }
    return std::nullopt;
}

std::optional<CompoundTagVariant> parseNumber(std::string_view& str, bool parseJson) {
    size_t pos   = 0;
    bool   isInt = true;
    if (auto num = parseNumberStr(str, pos, isInt)) {
        str.remove_prefix(pos);

        if (parseJson) {
            if (!skipWhitespace(str)) { return std::nullopt; }
            if (isInt) {
                if (auto tag = checkRange<ByteTag, uint8_t>(*num)) { return tag; }
                if (auto tag = checkRange<ShortTag, short>(*num)) { return tag; }
                if (auto tag = checkRange<IntTag, int>(*num)) { return tag; }
                if (auto tag = checkRange<LongTag, int64_t>(*num)) { return tag; }
            }
            if (auto tag = checkRange<FloatTag, float>(*num)) { return tag; }
            return checkRange<DoubleTag, double>(*num);
        }

        auto mk = parseNumberMark(str);
        if (mk.empty()) {
            if (isInt) {
                if (auto tag = checkRange<IntTag, int>(*num)) { return tag; }
                return checkRange<LongTag, int>(*num);
            } else {
                return checkRange<DoubleTag, double>(*num);
            }
        } else if (mk.size() == 1) {
            switch (mk.front()) {
            case 'b':
                str.remove_prefix(1);
                return checkRange<ByteTag, uint8_t>(*num);
            case 's':
                str.remove_prefix(1);
                return checkRange<ShortTag, short>(*num);
            case 'i':
                str.remove_prefix(1);
                return checkRange<IntTag, int>(*num);
            case 'l':
                str.remove_prefix(1);
                return checkRange<LongTag, int64_t>(*num);
            case 'f':
                str.remove_prefix(1);
                return checkRange<FloatTag, float>(*num);
            case 'd':
                str.remove_prefix(1);
                return checkRange<DoubleTag, double>(*num);
            default:
                break;
            }
        } else if (mk.size() == 2) {
            switch (string_utils::doHash(mk)) {
            case string_utils::doHash("sb"):
            case string_utils::doHash("ub"): {
                str.remove_prefix(2);
                return checkRange<ByteTag, uint8_t>(*num);
            }
            case string_utils::doHash("ss"):
            case string_utils::doHash("us"): {
                str.remove_prefix(2);
                return checkRange<ShortTag, short>(*num);
            }
            case string_utils::doHash("si"):
            case string_utils::doHash("ui"): {
                str.remove_prefix(2);
                return checkRange<IntTag, int>(*num);
            }
            case string_utils::doHash("sl"):
            case string_utils::doHash("ul"): {
                str.remove_prefix(2);
                return checkRange<LongTag, int64_t>(*num);
            }
            default:
                break;
            }
        } else if (mk.size() == 5) {
            switch (string_utils::doHash(mk)) {
            case string_utils::doHash("/*b*/"): {
                str.remove_prefix(5);
                return checkRange<ByteTag, uint8_t>(*num);
            }
            case string_utils::doHash("/*s*/"): {
                str.remove_prefix(5);
                return checkRange<ShortTag, short>(*num);
            }
            case string_utils::doHash("/*i*/"): {
                str.remove_prefix(5);
                return checkRange<IntTag, int>(*num);
            }
            case string_utils::doHash("/*l*/"): {
                str.remove_prefix(5);
                return checkRange<LongTag, int64_t>(*num);
            }
            case string_utils::doHash("/*f*/"): {
                str.remove_prefix(5);
                return checkRange<FloatTag, float>(*num);
            }
            case string_utils::doHash("/*d*/"): {
                str.remove_prefix(5);
                return checkRange<DoubleTag, double>(*num);
            }
            default:
                break;
            }
        } else if (mk.size() == 6) {
            switch (string_utils::doHash(mk)) {
            case string_utils::doHash("/*sb*/"):
            case string_utils::doHash("/*ub*/"): {
                str.remove_prefix(6);
                return checkRange<ByteTag, uint8_t>(*num);
            }
            case string_utils::doHash("/*ss*/"):
            case string_utils::doHash("/*us*/"): {
                str.remove_prefix(6);
                return checkRange<ShortTag, short>(*num);
            }
            case string_utils::doHash("/*si*/"):
            case string_utils::doHash("/*ui*/"): {
                str.remove_prefix(6);
                return checkRange<IntTag, int>(*num);
            }
            case string_utils::doHash("/*sl*/"):
            case string_utils::doHash("/*ul*/"): {
                str.remove_prefix(6);
                return checkRange<LongTag, int64_t>(*num);
            }
            default:
                break;
            }
        }
    }
    return std::nullopt;
}

std::optional<int> get_codepoint(std::string_view& s) {
    int codepoint = 0;
    for (const auto factor : {12u, 8u, 4u, 0u}) {
        auto current = get(s);
        if (current >= '0' && current <= '9') {
            codepoint += static_cast<int>((static_cast<uint32_t>(current) - 0x30u) << factor);
        } else if (current >= 'A' && current <= 'F') {
            codepoint += static_cast<int>((static_cast<uint32_t>(current) - 0x37u) << factor);
        } else if (current >= 'a' && current <= 'f') {
            codepoint += static_cast<int>((static_cast<uint32_t>(current) - 0x57u) << factor);
        } else {
            return std::nullopt;
        }
    }
    return codepoint;
}

std::optional<std::string> parseString(std::string_view& s, bool parseJson) {
    char starts = s.front();
    if (starts != '\"' && starts != '\'') {
        if (parseJson) { return std::nullopt; }
        if (!(isalnum(starts) || starts == '-' || starts == '+' || starts == '_' || starts == '.')) {
            return std::nullopt;
        }
    }
    std::string res;
    if (starts == '\"' || starts == '\'') {
        s.remove_prefix(1);
    } else {
        while (!s.empty()) {
            auto fc = s.front();
            if (isalnum(fc) || fc == '-' || fc == '+' || fc == '_' || fc == '.') {
                s.remove_prefix(1);
                res.push_back(fc);
            } else {
                if (!skipWhitespace(s)) { return std::nullopt; }
                return res;
            }
        }
    }
    while (!s.empty()) {
        auto current = get(s);
        switch (current) {
        case '\"': {
            if (starts == '\"') {
                if (s.starts_with(" /*BASE64*/")) { return base64_utils::decode(res); }
                if (!skipWhitespace(s)) { return std::nullopt; }
                return res;
            }
            res.push_back('\"');
        } break;
        case '\'': {
            if (starts == '\'') {
                if (!skipWhitespace(s)) { return std::nullopt; }
                return res;
            }
            res.push_back('\'');
        } break;
        case '\\': {
            switch (get(s)) {
            case '\n':
            case '\r':
                if (!skipWhitespace(s)) { return std::nullopt; }
                break;
            case '\"': {
                if (starts == '\"') {
                    res.push_back('\"');
                } else {
                    return std::nullopt;
                }
            } break;
            case '\'': {
                if (starts == '\'') {
                    res.push_back('\'');
                } else {
                    return std::nullopt;
                }
            } break;
            case '\\':
                res.push_back('\\');
                break;
            case '/':
                res.push_back('/');
                break;
            case 'b':
                res.push_back('\b');
                break;
            case 'f':
                res.push_back('\f');
                break;
            case 'n':
                res.push_back('\n');
                break;
            case 'r':
                res.push_back('\r');
                break;
            case 't':
                res.push_back('\t');
                break;
            case 'v':
                res.push_back('\v');
                break;
            case 'u': {
                int codepoint1;
                if (auto getted = get_codepoint(s); getted) {
                    codepoint1 = *getted;
                } else {
                    return std::nullopt;
                }
                int codepoint = codepoint1;
                if (0xD800 <= codepoint1 && codepoint1 <= 0xDBFF) {
                    if (get(s) == '\\' && get(s) == 'u') {
                        int codepoint2;
                        if (auto getted = get_codepoint(s); getted) {
                            codepoint2 = *getted;
                        } else {
                            return std::nullopt;
                        }
                        if ((0xDC00 <= codepoint2 && codepoint2 <= 0xDFFF)) {
                            codepoint = static_cast<int>(
                                (static_cast<uint32_t>(codepoint1) << 10u) + static_cast<uint32_t>(codepoint2)
                                - 0x35FDC00u
                            );
                        } else {
                            return std::nullopt;
                        }
                    } else {
                        return std::nullopt;
                    }
                } else {
                    if (0xDC00 <= codepoint1 && codepoint1 <= 0xDFFF) { return std::nullopt; }
                }
                if (codepoint < 0x80) {
                    res.push_back(static_cast<char>(codepoint));
                } else if (codepoint <= 0x7FF) {
                    res.push_back(static_cast<char>(0xC0u | (static_cast<uint32_t>(codepoint) >> 6u)));
                    res.push_back(static_cast<char>(0x80u | (static_cast<uint32_t>(codepoint) & 0x3Fu)));
                } else if (codepoint <= 0xFFFF) {
                    res.push_back(static_cast<char>(0xE0u | (static_cast<uint32_t>(codepoint) >> 12u)));
                    res.push_back(static_cast<char>(0x80u | ((static_cast<uint32_t>(codepoint) >> 6u) & 0x3Fu)));
                    res.push_back(static_cast<char>(0x80u | (static_cast<uint32_t>(codepoint) & 0x3Fu)));
                } else {
                    res.push_back(static_cast<char>(0xF0u | (static_cast<uint32_t>(codepoint) >> 18u)));
                    res.push_back(static_cast<char>(0x80u | ((static_cast<uint32_t>(codepoint) >> 12u) & 0x3Fu)));
                    res.push_back(static_cast<char>(0x80u | ((static_cast<uint32_t>(codepoint) >> 6u) & 0x3Fu)));
                    res.push_back(static_cast<char>(0x80u | (static_cast<uint32_t>(codepoint) & 0x3Fu)));
                }
            } break;
            default:
                return std::nullopt;
            }
        } break;
        default:
            res.push_back(current);
            break;
        }
    }
    return std::nullopt;
}

template <class R, class T, class H, class F>
std::optional<R> parseNumArray(std::string_view& s, F&& f) {
    T res;
    while (!s.empty()) {
        if (!skipWhitespace(s)) { return std::nullopt; }
        if (s.starts_with(']')) {
            s.remove_prefix(1);
            if (!skipWhitespace(s)) { return std::nullopt; }
            return res;
        }
        if (auto value = parseNumber(s, false); value) {
            if (!value->hold<H>()) {
                return std::nullopt;
            } else {
                std::invoke(std::forward<F>(f), res, value->as<H>());
            }
        } else {
            return std::nullopt;
        }
        if (!skipWhitespace(s)) { return std::nullopt; }
        switch (s.front()) {
        case ']':
            s.remove_prefix(1);
            if (!skipWhitespace(s)) { return std::nullopt; }
            return res;
        case ',':
            s.remove_prefix(1);
        default:
            break;
        }
    }
    return std::nullopt;
}

std::optional<ByteArrayTag> parseByteArray(std::string_view& s) {
    return parseNumArray<ByteArrayTag, std::vector<uint8_t>, ByteTag>(s, [](auto&& vec, auto&& num) {
        vec.emplace_back(num);
    });
}

std::optional<IntArrayTag> parseIntArray(std::string_view& s) {
    return parseNumArray<IntArrayTag, std::vector<int>, IntTag>(s, [](auto&& vec, auto&& num) {
        vec.emplace_back(num);
    });
}

std::optional<LongArrayTag> parseLongArray(std::string_view& s) {
    return parseNumArray<LongArrayTag, std::vector<int64_t>, LongTag>(s, [](auto&& vec, auto&& num) {
        vec.emplace_back(num);
    });
}

std::optional<CompoundTagVariant> parseList(std::string_view& s) {
    s.remove_prefix(1);
    scanSpaces(s);
    if (s.starts_with("/*") && (s.size() > 5 && s[4] == '*' && s[5] == '/')) { s.remove_prefix(2); }
    if (s.starts_with("B;")) {
        s.remove_prefix(2);
        if (s.starts_with("*/")) { s.remove_prefix(2); }
        if (auto array = parseByteArray(s); array) {
            return *array;
        } else {
            return std::nullopt;
        }
    } else if (s.starts_with("I;")) {
        s.remove_prefix(2);
        if (s.starts_with("*/")) { s.remove_prefix(2); }
        if (auto array = parseIntArray(s); array) {
            return *array;
        } else {
            return std::nullopt;
        }
    } else if (s.starts_with("L;")) {
        s.remove_prefix(2);
        if (s.starts_with("*/")) { s.remove_prefix(2); }
        if (auto array = parseLongArray(s); array) {
            return *array;
        } else {
            return std::nullopt;
        }
    }
    ListTag res{};
    while (!s.empty()) {
        if (!skipWhitespace(s)) { return std::nullopt; }
        if (s.starts_with(']')) {
            s.remove_prefix(1);
            if (!res.empty()) res.mType = res.storage().front()->getType();
            if (!skipWhitespace(s)) { return std::nullopt; }
            return res;
        }
        auto value = detail::parseSnbtValueNonSkip(s, false);
        if (!value) { return std::nullopt; }
        res.push_back(std::move(*value).toUnique());

        if (!skipWhitespace(s)) { return std::nullopt; }
        switch (s.front()) {
        case ']':
            s.remove_prefix(1);
            res.mType = res.storage().front()->getType();
            res.checkAndFixElements();
            if (!skipWhitespace(s)) { return std::nullopt; }
            return res;
        case ',': {
            s.remove_prefix(1);
        }
        default:
            break;
        }
    }
    return std::nullopt;
}

std::optional<CompoundTagVariant> parseJsonList(std::string_view& s) {
    s.remove_prefix(1);
    ListTag res{};
    while (!s.empty()) {
        if (!skipWhitespace(s)) { return std::nullopt; }
        if (s.starts_with(']')) {
            s.remove_prefix(1);
            if (!res.empty()) res.mType = res.storage().front()->getType();
            if (!skipWhitespace(s)) { return std::nullopt; }
            return res;
        }
        auto value = detail::parseSnbtValueNonSkip(s, true);
        if (!value) { return std::nullopt; }
        res.push_back(std::move(*value).toUnique());

        if (!skipWhitespace(s)) { return std::nullopt; }
        switch (s.front()) {
        case ']':
            s.remove_prefix(1);
            res.mType = res.storage().front()->getType();
            res.checkAndFixElements();
            switch (res.getElementType()) {
            case Tag::Type::Byte: {
                ByteArrayTag newres{};
                for (auto& tag : res) { newres.push_back(tag.as<ByteTag>().storage()); }
                return newres;
            }
            case Tag::Type::Int: {
                IntArrayTag newres{};
                for (auto& tag : res) { newres.push_back(tag.as<IntTag>().storage()); }
                return newres;
            }
            case Tag::Type::Long: {
                LongArrayTag newres{};
                for (auto& tag : res) { newres.push_back(tag.as<LongTag>().storage()); }
                return newres;
            }
            default:
                break;
            }
            if (!skipWhitespace(s)) { return std::nullopt; }
            return res;
        case ',': {
            s.remove_prefix(1);
        }
        default:
            break;
        }
    }
    return std::nullopt;
}

std::optional<CompoundTagVariant> parseCompound(std::string_view& s, bool parseJson) {
    s.remove_prefix(1);
    if (!skipWhitespace(s)) { return std::nullopt; }
    if (s.starts_with('}')) {
        s.remove_prefix(1);
        return CompoundTag{};
    }
    CompoundTag res;
    while (!s.empty()) {
        if (!skipWhitespace(s)) { return std::nullopt; }
        if (s.starts_with('}')) {
            s.remove_prefix(1);
            if (!skipWhitespace(s)) { return std::nullopt; }
            return res;
        }
        auto key = parseString(s, parseJson);
        if (!key) { return std::nullopt; }
        if (!skipWhitespace(s)) { return std::nullopt; }
        auto p = get(s);
        if (p != ':' && p != '=') { return std::nullopt; }
        auto value = detail::parseSnbtValue(s, parseJson);
        if (!value) { return std::nullopt; }
        res[*key] = *value;

        switch (s.front()) {
        case '}':
            s.remove_prefix(1);
            if (!skipWhitespace(s)) { return std::nullopt; }
            return res;
        case ',':
            s.remove_prefix(1);
        default:
            break;
        }
    }
    return std::nullopt;
}

} // namespace

namespace detail {

std::optional<CompoundTagVariant> parseSnbtValueNonSkip(std::string_view& s, bool parseJson) {
    if (s.empty()) { return std::nullopt; }
    if (parseJson) {
        if (!skipWhitespace(s)) { return std::nullopt; }
    }
    switch (s.front()) {
    case 't':
        if (s.starts_with("true")) {
            s.remove_prefix(4);
            return ByteTag{true};
        }
        break;
    case 'f':
        if (s.starts_with("false")) {
            s.remove_prefix(5);
            return ByteTag{false};
        }
        break;
    case 'n':
        if (s.starts_with("null")) {
            s.remove_prefix(4);
            return EndTag{};
        }
        break;
    case ']':
    case '}':
        s.remove_prefix(1);
        return std::nullopt;
    case CHAR_EOF:
    case '\0':
        return std::nullopt;
    case '+':
    case '-':
    case '0':
    case '1':
    case '2':
    case '3':
    case '4':
    case '5':
    case '6':
    case '7':
    case '8':
    case '9':
    case '.':
        return parseNumber(s, parseJson);
    case '[':
        if (parseJson) { return parseJsonList(s); }
        return parseList(s);
    case '{':
        return parseCompound(s, parseJson);
    default:
        break;
    }
    return parseString(s, parseJson);
}

std::optional<CompoundTagVariant> parseSnbtValue(std::string_view& s, bool parseJson) {
    if (!skipWhitespace(s)) { return std::nullopt; }
    auto res = parseSnbtValueNonSkip(s, parseJson);
    if (!res) { return res; }
    if (!skipWhitespace(s)) { return std::nullopt; }
    return res;
}

} // namespace detail

} // namespace nbt
