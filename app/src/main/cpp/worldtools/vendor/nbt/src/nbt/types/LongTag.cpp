// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/LongTag.hpp"

namespace nbt {

bool LongTag::equals(Tag const& other) const {
    return (other.getType() == Tag::Type::Long) && (mStorage == static_cast<const LongTag&>(other).mStorage);
}

LongTag LongTag::operator+() const noexcept { return LongTag{+mStorage}; }

Tag::Type LongTag::getType() const { return Type::Long; }

std::unique_ptr<Tag> LongTag::copy() const { return std::make_unique<LongTag>(mStorage); }

std::size_t LongTag::hash() const { return static_cast<size_t>(mStorage); }

void LongTag::write(io::BytesDataOutput& stream) const { stream.writeInt64(mStorage); }

void LongTag::load(io::BytesDataInput& stream) { mStorage = stream.getInt64(); }

void LongTag::write(bstream::BinaryStream& stream) const { stream.writeVarInt64(mStorage); }

void LongTag::load(bstream::ReadOnlyBinaryStream& stream) { mStorage = stream.getVarInt64(); }

} // namespace nbt