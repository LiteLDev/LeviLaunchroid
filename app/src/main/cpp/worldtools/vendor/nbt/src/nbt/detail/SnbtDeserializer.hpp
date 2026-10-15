// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <optional>
#include <string_view>

namespace nbt {

class CompoundTagVariant;

namespace detail {

std::optional<CompoundTagVariant> parseSnbtValue(std::string_view& s, bool parseJson);
std::optional<CompoundTagVariant> parseSnbtValueNonSkip(std::string_view& s, bool parseJson);

} // namespace detail

} // namespace nbt
