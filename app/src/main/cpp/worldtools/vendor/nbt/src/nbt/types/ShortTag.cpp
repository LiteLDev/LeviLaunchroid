// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/ShortTag.hpp"

namespace nbt {

bool ShortTag::equals(Tag const& other) const {
    return (other.getType() == Tag::Type::Short) && (mStorage == static_cast<const ShortTag&>(other).mStorage);
}

ShortTag ShortTag::operator+() const noexcept { return ShortTag{+mStorage}; }

Tag::Type ShortTag::getType() const { return Type::Short; }

std::unique_ptr<Tag> ShortTag::copy() const { return std::make_unique<ShortTag>(mStorage); }

std::size_t ShortTag::hash() const { return static_cast<size_t>(mStorage); }

void ShortTag::write(io::BytesDataOutput& stream) const { stream.writeShort(mStorage); }

void ShortTag::load(io::BytesDataInput& stream) { mStorage = stream.getShort(); }

void ShortTag::write(bstream::BinaryStream& stream) const { stream.writeSignedShort(mStorage); }

void ShortTag::load(bstream::ReadOnlyBinaryStream& stream) { mStorage = stream.getSignedShort(); }

} // namespace nbt