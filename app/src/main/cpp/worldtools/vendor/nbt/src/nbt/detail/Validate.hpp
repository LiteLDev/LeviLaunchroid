// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include "nbt/types/CompoundTagVariant.hpp"

namespace nbt::detail {

bool validateCompoundTag(io::BytesDataInput& stream, size_t streamSize);

bool validateCompoundTag(bstream::ReadOnlyBinaryStream& stream, size_t streamSize);

} // namespace nbt::detail
