// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/IntTag.hpp"

namespace nbt {

bool IntTag::equals(Tag const& other) const {
    return (other.getType() == Tag::Type::Int) && (mStorage == static_cast<const IntTag&>(other).mStorage);
}

IntTag IntTag::operator+() const noexcept { return IntTag{+mStorage}; }

Tag::Type IntTag::getType() const { return Type::Int; }

std::unique_ptr<Tag> IntTag::copy() const { return std::make_unique<IntTag>(mStorage); }

std::size_t IntTag::hash() const { return static_cast<size_t>(mStorage); }

void IntTag::write(io::BytesDataOutput& stream) const { stream.writeInt(mStorage); }

void IntTag::load(io::BytesDataInput& stream) { mStorage = stream.getInt(); }

void IntTag::write(bstream::BinaryStream& stream) const { stream.writeVarInt(mStorage); }

void IntTag::load(bstream::ReadOnlyBinaryStream& stream) { mStorage = stream.getVarInt(); }

} // namespace nbt