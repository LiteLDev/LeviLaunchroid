// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once

namespace nbt {

enum class NbtCompressionLevel : int {
    Default         = -1,
    NoCompression   = 0,
    BestSpeed       = 1,
    Low             = 2,
    MediumLow       = 3,
    Medium          = 4,
    MediumHigh      = 5,
    High            = 6,
    VeryHigh        = 7,
    Ultra           = 8,
    BestCompression = 9,
};

} // namespace nbt
