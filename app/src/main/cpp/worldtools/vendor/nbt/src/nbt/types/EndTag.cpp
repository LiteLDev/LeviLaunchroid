// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/EndTag.hpp"

namespace nbt {

bool EndTag::equals(Tag const& other) const { return other.getType() == Tag::Type::End; }

Tag::Type EndTag::getType() const { return Type::End; }

std::unique_ptr<Tag> EndTag::copy() const { return std::make_unique<EndTag>(); }

std::size_t EndTag::hash() const { return 0; }

void EndTag::write(io::BytesDataOutput&) const {}

void EndTag::load(io::BytesDataInput&) {}

void EndTag::write(bstream::BinaryStream&) const {}

void EndTag::load(bstream::ReadOnlyBinaryStream&) {}

} // namespace nbt
