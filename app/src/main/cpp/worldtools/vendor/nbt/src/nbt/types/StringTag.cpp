// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/StringTag.hpp"

namespace nbt {

StringTag::StringTag(std::string_view str) : mStorage(str) {}

StringTag::StringTag(std::string str) : mStorage(std::move(str)) {}

StringTag::StringTag(const char* data, size_t size) : mStorage(data, size) {}

bool StringTag::equals(Tag const& other) const {
    return (other.getType() == Tag::Type::String) && (mStorage == static_cast<const StringTag&>(other).mStorage);
}

Tag::Type StringTag::getType() const { return Type::String; }

std::unique_ptr<Tag> StringTag::copy() const { return std::make_unique<StringTag>(mStorage); }

std::size_t StringTag::hash() const { return std::hash<std::string>{}(mStorage); }

void StringTag::write(io::BytesDataOutput& stream) const { stream.writeString(mStorage); }

void StringTag::load(io::BytesDataInput& stream) { stream.getString(mStorage); }

void StringTag::write(bstream::BinaryStream& stream) const { stream.writeString(mStorage); }

void StringTag::load(bstream::ReadOnlyBinaryStream& stream) { stream.getString(mStorage); }

std::string&       StringTag::storage() noexcept { return mStorage; }
std::string const& StringTag::storage() const noexcept { return mStorage; }
std::string_view   StringTag::view() const noexcept { return mStorage; }

StringTag::operator std::string const&() const { return mStorage; }
StringTag::operator std::string&() { return mStorage; }

} // namespace nbt