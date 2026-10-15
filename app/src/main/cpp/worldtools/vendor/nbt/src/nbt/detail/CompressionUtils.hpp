// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <string>

namespace nbt::detail {

std::string compress(std::string_view input, int level, int windowBits);

std::string decompress(std::string_view input);

} // namespace nbt::detail
