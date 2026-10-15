// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <cstdint>

namespace nbt {

enum class NbtFileFormat : uint8_t {
    LittleEndian           = 0,
    LittleEndianWithHeader = 1,
    BigEndian              = 2,
    BigEndianWithHeader    = 3,
    BedrockNetwork         = 4,
};

} // namespace nbt
