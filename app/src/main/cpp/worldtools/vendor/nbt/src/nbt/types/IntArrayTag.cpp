// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/IntArrayTag.hpp"

namespace nbt {

IntArrayTag::IntArrayTag(std::vector<int> const& arr) : mStorage(std::move(arr)) {}

IntArrayTag::IntArrayTag(std::initializer_list<int> val) : mStorage(val) {}

IntArrayTag::operator std::vector<int> const&() const { return mStorage; }
IntArrayTag::operator std::vector<int>&() { return mStorage; }

bool IntArrayTag::equals(Tag const& other) const {
    return (other.getType() == Tag::Type::IntArray) && (mStorage == static_cast<const IntArrayTag&>(other).mStorage);
}

Tag::Type IntArrayTag::getType() const { return Type::IntArray; }

std::unique_ptr<Tag> IntArrayTag::copy() const { return std::make_unique<IntArrayTag>(mStorage); }

std::size_t IntArrayTag::hash() const {
    std::size_t hash = 0;
    for (int value : mStorage) {
        std::size_t element_hash  = std::hash<int>{}(value);
        hash                     ^= element_hash + 0x9e3779b9 + (hash << 6) + (hash >> 2);
    }
    return hash;
}

void IntArrayTag::write(io::BytesDataOutput& stream) const {
    stream.writeInt((int)mStorage.size());
    for (const int i : mStorage) { stream.writeInt(i); }
}

void IntArrayTag::load(io::BytesDataInput& stream) {
    auto size = stream.getInt();
    mStorage.clear();
    for (int i = 0; i < size; ++i) { mStorage.push_back(stream.getInt()); }
}

void IntArrayTag::write(bstream::BinaryStream& stream) const {
    stream.writeVarInt((int)mStorage.size());
    for (auto& data : mStorage) { stream.writeVarInt(data); }
}

void IntArrayTag::load(bstream::ReadOnlyBinaryStream& stream) {
    auto size = stream.getVarInt();
    for (auto i = 0; i < size; i++) { mStorage.emplace_back(stream.getVarInt()); }
}

std::vector<int>&       IntArrayTag::storage() noexcept { return mStorage; }
std::vector<int> const& IntArrayTag::storage() const noexcept { return mStorage; }

size_t IntArrayTag::size() const noexcept { return mStorage.size(); }

void IntArrayTag::reserve(size_t size) { mStorage.reserve(size); }

bool IntArrayTag::remove(size_t index) {
    if (index < mStorage.size()) {
        mStorage.erase(mStorage.begin() + static_cast<decltype(mStorage)::difference_type>(index));
        return true;
    }
    return false;
}

bool IntArrayTag::remove(size_t startIndex, size_t endIndex) {
    if (startIndex < endIndex && endIndex < mStorage.size()) {
        mStorage.erase(
            mStorage.begin() + static_cast<decltype(mStorage)::difference_type>(startIndex),
            mStorage.begin() + static_cast<decltype(mStorage)::difference_type>(endIndex)
        );
        return true;
    }
    return false;
}

void IntArrayTag::clear() noexcept { mStorage.clear(); }

int&       IntArrayTag::operator[](size_t index) noexcept { return mStorage[index]; }
int const& IntArrayTag::operator[](size_t index) const noexcept { return mStorage[index]; }

int&       IntArrayTag::at(size_t index) { return mStorage.at(index); }
int const& IntArrayTag::at(size_t index) const { return mStorage.at(index); }

void IntArrayTag::push_back(int val) { mStorage.push_back(val); }

IntArrayTag& IntArrayTag::operator=(std::vector<int> const& value) {
    mStorage = value;
    return *this;
}

} // namespace nbt