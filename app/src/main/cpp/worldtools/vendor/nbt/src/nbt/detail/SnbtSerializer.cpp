// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

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
#include "nbt/types/LongTag.hpp"
#include "nbt/types/ShortTag.hpp"
#include "nbt/types/StringTag.hpp"
#include <cmath>
#include <format>
#include <limits>

namespace nbt {

namespace {

template <std::integral T>
std::string toString(T value, SnbtNumberFormat nfmt) {
    switch (nfmt) {
    case SnbtNumberFormat::Binary: {
        return std::format("0b{:b}", value);
    }
    case SnbtNumberFormat::LowerHexadecimal: {
        return std::format("{:#x}", value);
    }
    case SnbtNumberFormat::UpperHexadecimal: {
        return std::format("{:#X}", value);
    }
    default: {
        return std::format("{:d}", value);
    }
    }
}

template <std::floating_point T>
std::string toString(T value, SnbtNumberFormat) {
    if (std::round(value) == value) { return std::format("{:.1f}", value); }
    return std::format("{}", value);
}

template <std::integral T>
constexpr std::string makeSnbtTagValue(T value, SnbtFormat format, char mark, SnbtNumberFormat nfmt) {
    bool upper = static_cast<bool>(format & SnbtFormat::ForceUppercase);
    if (static_cast<bool>(format & SnbtFormat::MarkSigned)) {
        char smk = std::is_signed_v<T> ? 's' : 'u';
        if (static_cast<bool>(format & SnbtFormat::CommentMarks)) {
            return std::format(
                "{0} /*{1:c}{2:c}*/",
                toString(value, nfmt),
                upper ? std::toupper(smk) : smk,
                upper ? std::toupper(mark) : mark
            );
        }
        return std::format(
            "{0}{1:c}{2:c}",
            toString(value, nfmt),
            upper ? std::toupper(smk) : smk,
            upper ? std::toupper(mark) : mark
        );
    }
    if (static_cast<bool>(format & SnbtFormat::CommentMarks)) {
        return std::format("{0} /*{1:c}*/", toString(value, nfmt), upper ? std::toupper(mark) : mark);
    }
    return std::format("{0}{1:c}", toString(value, nfmt), upper ? std::toupper(mark) : mark);
}

template <std::floating_point T>
constexpr std::string makeSnbtTagValue(T value, SnbtFormat format, char mark, SnbtNumberFormat) {
    bool upper = static_cast<bool>(format & SnbtFormat::ForceUppercase);
    if (static_cast<bool>(format & SnbtFormat::CommentMarks)) {
        if (std::round(value) == value) {
            return std::format("{0:.1f} /*{1:c}*/", value, upper ? std::toupper(mark) : mark);
        }
        return std::format("{0} /*{1:c}*/", value, upper ? std::toupper(mark) : mark);
    }
    if (std::round(value) == value) { return std::format("{0:.1f}{1:c}", value, upper ? std::toupper(mark) : mark); }
    return std::format("{0}{1:c}", value, upper ? std::toupper(mark) : mark);
}

bool isMinimize(SnbtFormat format) {
    return !(
        static_cast<bool>(format & SnbtFormat::CompoundLineFeed)
        || static_cast<bool>(format & SnbtFormat::ListArrayLineFeed)
        || static_cast<bool>(format & SnbtFormat::BinaryArrayLineFeed)
    );
}

std::string toDumpString(std::string const& str, SnbtFormat format, bool key) {
    std::string res;
    if (str.empty()) {
        res = "\"\"";
    } else {
        bool isTrivial = true;
        if (!(static_cast<bool>(format & SnbtFormat::ForceKeyQuote) && key)
            && !(static_cast<bool>(format & SnbtFormat::ForceValueQuote) && !key)) {
            if (!key && (str[0] == '-' || str[0] == '+' || str[0] == '.' || isdigit(str[0]))) {
                isTrivial = false;
            } else {
                for (auto c : str) {
                    if (!(isalnum(c) || c == '-' || c == '+' || c == '_' || c == '.')) {
                        isTrivial = false;
                        break;
                    }
                }
            }
        } else {
            isTrivial = false;
        }
        if (isTrivial) {
            res = str;
        } else {
            if (string_utils::isValidUTF8(str)) {
                res = res = std::format(
                    "\"{}\"",
                    string_utils::dumpString(str, static_cast<bool>(format & SnbtFormat::ForceAscii))
                );
            } else {
                res = std::format("\"{}\" /*BASE64*/", base64_utils::encode(str));
            }
        }
    }
    return res;
}

} // namespace

namespace detail {

std::string TypedToSnbt(EndTag const&, uint8_t, SnbtFormat, bool, SnbtNumberFormat) { return "null"; }

std::string TypedToSnbt(ByteTag const& self, uint8_t, SnbtFormat format, bool dumpJson, SnbtNumberFormat nfmt) {
    if (dumpJson) return toString(self.storage(), nfmt);
    return makeSnbtTagValue(self.storage(), format, 'b', nfmt);
}

std::string TypedToSnbt(ShortTag const& self, uint8_t, SnbtFormat format, bool dumpJson, SnbtNumberFormat nfmt) {
    if (dumpJson) return toString(self.storage(), nfmt);
    return makeSnbtTagValue(self.storage(), format, 's', nfmt);
}

std::string TypedToSnbt(IntTag const& self, uint8_t, SnbtFormat format, bool dumpJson, SnbtNumberFormat nfmt) {
    if (dumpJson) return toString(self.storage(), nfmt);
    if (static_cast<bool>(format & SnbtFormat::MarkIntTag)) {
        return makeSnbtTagValue(self.storage(), format, 'i', nfmt);
    }
    return toString(self.storage(), nfmt);
}

std::string TypedToSnbt(LongTag const& self, uint8_t, SnbtFormat format, bool dumpJson, SnbtNumberFormat nfmt) {
    if (dumpJson) return toString(self.storage(), nfmt);
    return makeSnbtTagValue(self.storage(), format, 'l', nfmt);
}

std::string TypedToSnbt(FloatTag const& self, uint8_t, SnbtFormat format, bool dumpJson, SnbtNumberFormat nfmt) {
    if (dumpJson) return toString(self.storage(), nfmt);
    return makeSnbtTagValue(self.storage(), format, 'f', nfmt);
}

std::string TypedToSnbt(DoubleTag const& self, uint8_t, SnbtFormat format, bool dumpJson, SnbtNumberFormat nfmt) {
    if (dumpJson) return toString(self.storage(), nfmt);
    if (static_cast<bool>(format & SnbtFormat::MarkDoubleTag)) {
        return makeSnbtTagValue(self.storage(), format, 'd', nfmt);
    }
    return toString(self.storage(), nfmt);
}

std::string TypedToSnbt(StringTag const& self, uint8_t, SnbtFormat format, bool, SnbtNumberFormat) {
    return toDumpString(self.storage(), format, false);
}

std::string TypedToSnbt(ListTag const& self, uint8_t indent, SnbtFormat format, bool dumpJson, SnbtNumberFormat) {
    std::string res;

    static constexpr std::string_view lbracket{"["}, rbracket{"]"};

    res += lbracket;

    size_t      i = self.size();
    std::string indentSpace(indent, ' ');

    bool isMinimized = isMinimize(format);
    bool isNewLine   = static_cast<bool>(format & SnbtFormat::ListArrayLineFeed)
                  && (static_cast<bool>(format & SnbtFormat::ForceLineFeedIgnoreIndent) || (indent > 0));

    if (isNewLine && self.size() > 0) { res += '\n'; }
    for (auto& tag : self) {
        i--;
        if (isNewLine) { res += indentSpace; }
        auto key = tag->toSnbt(format, indent);
        if (dumpJson) { key = tag->toJson(); }

        if (isNewLine) { string_utils::replaceAll(key, "\n", "\n" + indentSpace); }
        res += key;

        if (i > 0) {
            res += ',';
            if (!isMinimized && !isNewLine) { res += ' '; }
        }
        if (isNewLine) { res += '\n'; }
    }

    res += rbracket;

    return res;
}

std::string
TypedToSnbt(CompoundTag const& self, uint8_t indent, SnbtFormat format, bool dumpJson, SnbtNumberFormat nfmt) {
    std::string res;

    static constexpr std::string_view lbracket{"{"}, rbracket{"}"};

    res += lbracket;

    size_t      i = self.size();
    std::string indentSpace(indent, ' ');

    bool isMinimized = isMinimize(format);
    bool isNewLine   = static_cast<bool>(format & SnbtFormat::CompoundLineFeed)
                  && (static_cast<bool>(format & SnbtFormat::ForceLineFeedIgnoreIndent) || (indent > 0));

    if (isNewLine && self.size() > 0) { res += '\n'; }

    for (auto& [k, v] : self.items()) {
        if (v.hold(Tag::Type::End)) { continue; }

        i--;
        if (isNewLine) { res += indentSpace; }
        auto keyStr = toDumpString(k, format, true);

        res += keyStr;

        res += ':';

        if (!isMinimized) { res += ' '; }
        auto key = v.toSnbt(format, indent, nfmt);
        if (dumpJson) { key = v.toJson(); }

        if (isNewLine) { string_utils::replaceAll(key, "\n", "\n" + indentSpace); }
        res += key;

        if (i > 0) {
            res += ',';
            if (!isMinimized && !isNewLine) { res += ' '; }
        }
        if (isNewLine) { res += '\n'; }
    }

    res += rbracket;

    return res;
}

std::string
TypedToSnbt(ByteArrayTag const& self, uint8_t indent, SnbtFormat format, bool dumpJson, SnbtNumberFormat nfmt) {

    std::string res;

    std::string lbracket{"[B;"}, rbracket{"]"};
    if (static_cast<bool>(format & SnbtFormat::CommentMarks)) { lbracket = "[ /*B;*/"; }
    if (dumpJson) { lbracket = "["; }

    res += lbracket;

    size_t      i = self.size();
    std::string indentSpace(indent, ' ');

    bool isMinimized = isMinimize(format);
    bool isNewLine   = static_cast<bool>(format & SnbtFormat::BinaryArrayLineFeed)
                  && (static_cast<bool>(format & SnbtFormat::ForceLineFeedIgnoreIndent) || (indent > 0));

    if (isNewLine && self.size() > 0) { res += '\n'; }

    for (auto& tag : self.storage()) {
        i--;
        if (isNewLine) { res += indentSpace; }

        if (dumpJson) {
            res += toString(tag, nfmt);
        } else {
            res += makeSnbtTagValue(tag, format, 'b', nfmt);
        }

        if (i > 0) {
            res += ',';
            if (!isMinimized && !isNewLine) { res += ' '; }
        }
        if (isNewLine) { res += '\n'; }
    }

    res += rbracket;

    return res;
}

std::string
TypedToSnbt(IntArrayTag const& self, uint8_t indent, SnbtFormat format, bool dumpJson, SnbtNumberFormat nfmt) {

    std::string res;

    std::string lbracket{"[I;"}, rbracket{"]"};
    if (static_cast<bool>(format & SnbtFormat::CommentMarks)) { lbracket = "[ /*I;*/"; }
    if (dumpJson) { lbracket = "["; }

    res += lbracket;

    size_t      i = self.size();
    std::string indentSpace(indent, ' ');

    bool isMinimized = isMinimize(format);
    bool isNewLine   = static_cast<bool>(format & SnbtFormat::BinaryArrayLineFeed)
                  && (static_cast<bool>(format & SnbtFormat::ForceLineFeedIgnoreIndent) || (indent > 0));

    if (isNewLine && self.size() > 0) { res += '\n'; }

    for (auto& tag : self.storage()) {
        i--;
        if (isNewLine) { res += indentSpace; }

        if (dumpJson) {
            res += toString(tag, nfmt);
        } else if (static_cast<bool>(format & SnbtFormat::MarkIntTag)) {
            res += makeSnbtTagValue(tag, format, 'i', nfmt);
        } else {
            res += toString(tag, nfmt);
        }

        if (i > 0) {
            res += ',';
            if (!isMinimized && !isNewLine) { res += ' '; }
        }
        if (isNewLine) { res += '\n'; }
    }

    res += rbracket;

    return res;
}

std::string
TypedToSnbt(LongArrayTag const& self, uint8_t indent, SnbtFormat format, bool dumpJson, SnbtNumberFormat nfmt) {

    std::string res;

    std::string lbracket{"[L;"}, rbracket{"]"};
    if (static_cast<bool>(format & SnbtFormat::CommentMarks)) { lbracket = "[ /*L;*/"; }
    if (dumpJson) { lbracket = "["; }

    res += lbracket;

    size_t      i = self.size();
    std::string indentSpace(indent, ' ');

    bool isMinimized = isMinimize(format);
    bool isNewLine   = static_cast<bool>(format & SnbtFormat::BinaryArrayLineFeed)
                  && (static_cast<bool>(format & SnbtFormat::ForceLineFeedIgnoreIndent) || (indent > 0));

    if (isNewLine && self.size() > 0) { res += '\n'; }

    for (auto& tag : self.storage()) {
        i--;
        if (isNewLine) { res += indentSpace; }

        if (dumpJson) {
            res += toString(tag, nfmt);
        } else {
            res += makeSnbtTagValue(tag, format, 'l', nfmt);
        }

        if (i > 0) {
            res += ',';
            if (!isMinimized && !isNewLine) { res += ' '; }
        }
        if (isNewLine) { res += '\n'; }
    }

    res += rbracket;

    return res;
}

} // namespace detail

} // namespace nbt