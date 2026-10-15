// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/FloatTag.hpp"

namespace nbt {

bool FloatTag::equals(Tag const& other) const {
    return (other.getType() == Tag::Type::Float) && (mStorage == static_cast<const FloatTag&>(other).mStorage);
}

Tag::Type FloatTag::getType() const { return Type::Float; }

std::unique_ptr<Tag> FloatTag::copy() const { return std::make_unique<FloatTag>(mStorage); }

std::size_t FloatTag::hash() const { return std::hash<float>{}(mStorage); }

void FloatTag::write(io::BytesDataOutput& stream) const { stream.writeFloat(mStorage); }

void FloatTag::load(io::BytesDataInput& stream) { mStorage = stream.getFloat(); }

void FloatTag::write(bstream::BinaryStream& stream) const { stream.writeFloat(mStorage); }

void FloatTag::load(bstream::ReadOnlyBinaryStream& stream) { mStorage = stream.getFloat(); }

} // namespace nbt