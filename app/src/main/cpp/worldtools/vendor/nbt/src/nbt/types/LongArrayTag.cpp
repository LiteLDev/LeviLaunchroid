// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/LongArrayTag.hpp"

namespace nbt {

LongArrayTag::LongArrayTag(std::vector<int64_t> const& arr) : mStorage(std::move(arr)) {}

LongArrayTag::LongArrayTag(std::initializer_list<int64_t> val) : mStorage(val) {}

LongArrayTag::operator std::vector<int64_t> const&() const { return mStorage; }
LongArrayTag::operator std::vector<int64_t>&() { return mStorage; }

bool LongArrayTag::equals(Tag const& other) const {
    return (other.getType() == Tag::Type::LongArray) && (mStorage == static_cast<const LongArrayTag&>(other).mStorage);
}

Tag::Type LongArrayTag::getType() const { return Type::LongArray; }

std::unique_ptr<Tag> LongArrayTag::copy() const { return std::make_unique<LongArrayTag>(mStorage); }

std::size_t LongArrayTag::hash() const {
    std::size_t hash = 0;
    for (int64_t value : mStorage) {
        std::size_t element_hash  = std::hash<int64_t>{}(value);
        hash                     ^= element_hash + 0x9e3779b9 + (hash << 6) + (hash >> 2);
    }
    return hash;
}

void LongArrayTag::write(io::BytesDataOutput& stream) const {
    stream.writeInt((int)mStorage.size());
    for (const int64_t i : mStorage) { stream.writeInt64(i); }
}

void LongArrayTag::load(io::BytesDataInput& stream) {
    auto size = stream.getInt();
    mStorage.clear();
    for (int64_t i = 0; i < size; ++i) { mStorage.push_back(stream.getInt64()); }
}

void LongArrayTag::write(bstream::BinaryStream& stream) const {
    stream.writeVarInt((int)mStorage.size());
    for (auto& data : mStorage) { stream.writeVarInt64(data); }
}

void LongArrayTag::load(bstream::ReadOnlyBinaryStream& stream) {
    auto size = stream.getVarInt();
    for (auto i = 0; i < size; i++) { mStorage.emplace_back(stream.getVarInt64()); }
}

std::vector<int64_t>&       LongArrayTag::storage() noexcept { return mStorage; }
std::vector<int64_t> const& LongArrayTag::storage() const noexcept { return mStorage; }

size_t LongArrayTag::size() const noexcept { return mStorage.size(); }

void LongArrayTag::reserve(size_t size) { mStorage.reserve(size); }

bool LongArrayTag::remove(size_t index) {
    if (index < mStorage.size()) {
        mStorage.erase(mStorage.begin() + static_cast<decltype(mStorage)::difference_type>(index));
        return true;
    }
    return false;
}

bool LongArrayTag::remove(size_t startIndex, size_t endIndex) {
    if (startIndex < endIndex && endIndex < mStorage.size()) {
        mStorage.erase(
            mStorage.begin() + static_cast<decltype(mStorage)::difference_type>(startIndex),
            mStorage.begin() + static_cast<decltype(mStorage)::difference_type>(endIndex)
        );
        return true;
    }
    return false;
}

void LongArrayTag::clear() noexcept { mStorage.clear(); }

int64_t&       LongArrayTag::operator[](size_t index) noexcept { return mStorage[index]; }
int64_t const& LongArrayTag::operator[](size_t index) const noexcept { return mStorage[index]; }

int64_t&       LongArrayTag::at(size_t index) { return mStorage.at(index); }
int64_t const& LongArrayTag::at(size_t index) const { return mStorage.at(index); }

void LongArrayTag::push_back(int64_t val) { mStorage.push_back(val); }

LongArrayTag& LongArrayTag::operator=(std::vector<int64_t> const& value) {
    mStorage = value;
    return *this;
}

} // namespace nbt