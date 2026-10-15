// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <utility>

namespace nbt {

enum class SnbtFormat : uint16_t {
    Minimize                  = 0,
    CompoundLineFeed          = 1 << 0,
    ListArrayLineFeed         = 1 << 1,
    BinaryArrayLineFeed       = 1 << 2,
    ForceLineFeedIgnoreIndent = 1 << 3,
    ForceAscii                = 1 << 4,
    ForceKeyQuote             = 1 << 5,
    ForceValueQuote           = 1 << 6,
    ForceUppercase            = 1 << 7,
    MarkIntTag                = 1 << 8,
    MarkDoubleTag             = 1 << 9,
    CommentMarks              = 1 << 10,
    MarkSigned                = 1 << 11,
    PrettyFilePrint           = CompoundLineFeed | ListArrayLineFeed,
    ArrayLineFeed             = ListArrayLineFeed | BinaryArrayLineFeed,
    AlwaysLineFeed            = CompoundLineFeed | ArrayLineFeed,
    ForceQuote                = ForceKeyQuote | ForceValueQuote,
    Classic                   = PrettyFilePrint | ForceQuote,
    MarkAllTags               = MarkIntTag | MarkDoubleTag,
    Jsonify                   = AlwaysLineFeed | ForceQuote | CommentMarks,
    Default                   = PrettyFilePrint,
};

[[nodiscard]] constexpr SnbtFormat operator|(const SnbtFormat lhs, const SnbtFormat rhs) noexcept {
    return static_cast<SnbtFormat>(std::to_underlying(lhs) | std::to_underlying(rhs));
}

[[nodiscard]] constexpr SnbtFormat operator&(const SnbtFormat lhs, const SnbtFormat rhs) noexcept {
    return static_cast<SnbtFormat>(std::to_underlying(lhs) & std::to_underlying(rhs));
}

enum class SnbtNumberFormat : uint8_t {
    Decimal          = 0,
    LowerHexadecimal = 1,
    UpperHexadecimal = 2,
    Binary           = 3,
    Default          = Decimal,
};

} // namespace nbt