// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/ByteArrayTag.hpp"

namespace nbt {

ByteArrayTag::ByteArrayTag(std::vector<uint8_t> const& arr) : mStorage(arr) {}

ByteArrayTag::ByteArrayTag(std::vector<std::byte> const& arr)
: mStorage(reinterpret_cast<uint8_t const*>(arr.data()), reinterpret_cast<uint8_t const*>(arr.data() + arr.size())) {}

ByteArrayTag::ByteArrayTag(std::initializer_list<uint8_t> val) : mStorage(val) {}

ByteArrayTag::ByteArrayTag(std::initializer_list<std::byte> val)
: mStorage(reinterpret_cast<uint8_t const*>(val.begin()), reinterpret_cast<uint8_t const*>(val.end())) {}

ByteArrayTag::ByteArrayTag(uint8_t const* data, size_t size) : mStorage(data, data + size) {}

ByteArrayTag::ByteArrayTag(std::byte const* data, size_t size)
: mStorage(reinterpret_cast<uint8_t const*>(data), reinterpret_cast<uint8_t const*>(data) + size) {}

ByteArrayTag::ByteArrayTag(std::string_view binaryData)
: mStorage(
      reinterpret_cast<uint8_t const*>(binaryData.data()),
      reinterpret_cast<uint8_t const*>(binaryData.data() + binaryData.size())
  ) {}

ByteArrayTag::operator std::vector<uint8_t> const&() const { return mStorage; }
ByteArrayTag::operator std::vector<uint8_t>&() { return mStorage; }
ByteArrayTag::operator std::vector<std::byte>() const {
    return {
        reinterpret_cast<std::byte const*>(mStorage.data()),
        reinterpret_cast<std::byte const*>(mStorage.data() + mStorage.size())
    };
}
ByteArrayTag::operator std::string_view() const {
    return std::string_view(reinterpret_cast<char const*>(mStorage.data()), mStorage.size());
}

bool ByteArrayTag::equals(Tag const& other) const {
    return (other.getType() == Tag::Type::ByteArray) && (mStorage == static_cast<const ByteArrayTag&>(other).mStorage);
}

Tag::Type ByteArrayTag::getType() const { return Type::ByteArray; }

std::unique_ptr<Tag> ByteArrayTag::copy() const { return std::make_unique<ByteArrayTag>(mStorage); }

std::size_t ByteArrayTag::hash() const {
    std::size_t hash = 0;
    for (uint8_t value : mStorage) {
        std::size_t element_hash  = std::hash<uint8_t>{}(value);
        hash                     ^= element_hash + 0x9e3779b9 + (hash << 6) + (hash >> 2);
    }
    return hash;
}

void ByteArrayTag::write(io::BytesDataOutput& stream) const {
    stream.writeInt((int)mStorage.size());
    stream.writeBytes(mStorage.data(), mStorage.size());
}

void ByteArrayTag::load(io::BytesDataInput& stream) {
    auto size = static_cast<size_t>(stream.getInt());
    mStorage.resize(size);
    stream.getBytes(mStorage.data(), size);
}

void ByteArrayTag::write(bstream::BinaryStream& stream) const {
    stream.writeVarInt((int)mStorage.size());
    for (auto& data : mStorage) { stream.writeUnsignedChar(data); }
}

void ByteArrayTag::load(bstream::ReadOnlyBinaryStream& stream) {
    auto size = stream.getVarInt();
    for (auto i = 0; i < size; i++) { mStorage.emplace_back(stream.getUnsignedChar()); }
}

std::vector<uint8_t>&       ByteArrayTag::storage() noexcept { return mStorage; }
std::vector<uint8_t> const& ByteArrayTag::storage() const noexcept { return mStorage; }

uint8_t const* ByteArrayTag::data() const noexcept { return mStorage.data(); }
uint8_t*       ByteArrayTag::data() noexcept { return mStorage.data(); }

size_t ByteArrayTag::size() const noexcept { return mStorage.size(); }

void ByteArrayTag::reserve(size_t size) { mStorage.reserve(size); }

bool ByteArrayTag::remove(size_t index) {
    if (index < mStorage.size()) {
        mStorage.erase(mStorage.begin() + static_cast<decltype(mStorage)::difference_type>(index));
        return true;
    }
    return false;
}

bool ByteArrayTag::remove(size_t startIndex, size_t endIndex) {
    if (startIndex < endIndex && endIndex < mStorage.size()) {
        mStorage.erase(
            mStorage.begin() + static_cast<decltype(mStorage)::difference_type>(startIndex),
            mStorage.begin() + static_cast<decltype(mStorage)::difference_type>(endIndex)
        );
        return true;
    }
    return false;
}

void ByteArrayTag::clear() noexcept { mStorage.clear(); }

uint8_t&       ByteArrayTag::operator[](size_t index) noexcept { return mStorage[index]; }
uint8_t const& ByteArrayTag::operator[](size_t index) const noexcept { return mStorage[index]; }

uint8_t&       ByteArrayTag::at(size_t index) { return mStorage.at(index); }
uint8_t const& ByteArrayTag::at(size_t index) const { return mStorage.at(index); }

void ByteArrayTag::push_back(uint8_t val) { mStorage.push_back(val); }
void ByteArrayTag::push_back(std::byte val) { mStorage.push_back(static_cast<uint8_t>(val)); }

ByteArrayTag& ByteArrayTag::operator=(std::vector<uint8_t> const& value) {
    mStorage = value;
    return *this;
}
ByteArrayTag& ByteArrayTag::operator=(std::vector<std::byte> const& value) {
    mStorage.assign(
        reinterpret_cast<uint8_t const*>(value.data()),
        reinterpret_cast<uint8_t const*>(value.data() + value.size())
    );
    return *this;
}
ByteArrayTag& ByteArrayTag::operator=(std::string_view value) {
    mStorage.assign(
        reinterpret_cast<uint8_t const*>(value.data()),
        reinterpret_cast<uint8_t const*>(value.data() + value.size())
    );
    return *this;
}

void ByteArrayTag::reinit(uint8_t const* data, size_t size) { mStorage.assign(data, data + size); }

void ByteArrayTag::reinit(std::byte const* data, size_t size) {
    mStorage.assign(reinterpret_cast<uint8_t const*>(data), reinterpret_cast<uint8_t const*>(data) + size);
}

} // namespace nbt