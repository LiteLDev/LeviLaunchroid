// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/CompoundTag.hpp"
#include "nbt/io/NBTIO.hpp"
#include "nbt/types/ByteArrayTag.hpp"
#include "nbt/types/ByteTag.hpp"
#include "nbt/types/CompoundTagVariant.hpp"
#include "nbt/types/DoubleTag.hpp"
#include "nbt/types/EndTag.hpp"
#include "nbt/types/FloatTag.hpp"
#include "nbt/types/IntArrayTag.hpp"
#include "nbt/types/IntTag.hpp"
#include "nbt/types/ListTag.hpp"
#include "nbt/types/LongArrayTag.hpp"
#include "nbt/types/LongTag.hpp"
#include "nbt/types/ShortTag.hpp"
#include "nbt/types/StringTag.hpp"
#include <algorithm>
#include <format>

namespace nbt {

bool CompoundTag::equals(Tag const& other) const {
    if (other.getType() != Type::Compound) { return false; }
    const auto& otherTag = static_cast<const CompoundTag&>(other);
    if (size() != otherTag.size()) { return false; }
    return std::all_of(mTagMap.begin(), mTagMap.end(), [&](const auto& kv) {
        const auto* tag = otherTag.get(kv.first);
        return tag && kv.second.get()->equals(*tag);
    });
}

std::unique_ptr<Tag> CompoundTag::copy() const { return clone(); }

std::size_t CompoundTag::hash() const {
    size_t           hash   = 0x811c9dc5;
    constexpr size_t prime  = 0x01000193;
    auto             binary = toBinaryNbt();
    for (char byte : binary) {
        hash ^= static_cast<size_t>(byte);
        hash  = hash * prime;
    }
    return hash;
}

Tag::Type CompoundTag::getType() const { return Type::Compound; }

std::unique_ptr<CompoundTag> CompoundTag::clone() const {
    auto tag = std::make_unique<CompoundTag>();
    for (const auto& [key, value] : mTagMap) { tag->mTagMap.emplace(key, value.toUniqueCopy()); }
    return tag;
}

void CompoundTag::write(io::BytesDataOutput& stream) const {
    for (const auto& [key, tag] : mTagMap) {
        auto type = tag->getType();
        if (type != Type::End) {
            stream.writeByte(static_cast<uint8_t>(type));
            stream.writeString(key);
            tag->write(stream);
        }
    }
    stream.writeByte(static_cast<uint8_t>(Type::End));
}

void CompoundTag::load(io::BytesDataInput& stream) {
    while (true) {
        const auto type = static_cast<Type>(stream.getByte());
        if (type == Type::End) { return; }
        auto key = stream.getStringView();
        if (auto tagPtr = Tag::newTag(type)) {
            tagPtr->load(stream);
            mTagMap.emplace(std::move(key), std::move(*tagPtr));
        }
    }
}

void CompoundTag::write(bstream::BinaryStream& stream) const {
    for (const auto& [key, tag] : mTagMap) {
        auto type = tag->getType();
        if (type != Type::End) {
            stream.writeUnsignedChar(static_cast<uint8_t>(type));
            stream.writeString(key);
            tag->write(stream);
        }
    }
    stream.writeUnsignedChar(static_cast<uint8_t>(Type::End));
}

void CompoundTag::load(bstream::ReadOnlyBinaryStream& stream) {
    while (true) {
        const auto type = static_cast<Type>(stream.getByte());
        if (type == Type::End) { return; }
        auto key = stream.getStringView();
        if (auto tagPtr = Tag::newTag(type)) {
            tagPtr->load(stream);
            mTagMap.emplace(std::move(key), std::move(*tagPtr));
        }
    }
}

void CompoundTag::merge(CompoundTag const& other, bool mergeList) {
    for (auto const& [key, val] : other) { mTagMap[key].merge(val, mergeList); }
}

bool CompoundTag::put(std::string_view key, Tag&& tag) {
    auto [_, result] = mTagMap.emplace(key, std::forward<Tag>(tag));
    return result;
}

bool CompoundTag::put(std::string_view key, std::unique_ptr<Tag>&& tag) {
    if (tag) {
        auto [_, result] = mTagMap.emplace(key, std::move(*tag));
        return result;
    }
    return false;
}

void CompoundTag::set(std::string_view key, Tag&& tag) { operator[](key) = tag; }

void CompoundTag::set(std::string_view key, std::unique_ptr<Tag>&& tag) { operator[](key) = std::move(tag); }

Tag const* CompoundTag::get(std::string_view key) const {
    if (mTagMap.contains(key)) { return mTagMap.at(std::string(key)).get(); }
    return nullptr;
}

Tag* CompoundTag::get(std::string_view key) {
    if (mTagMap.contains(key)) { return mTagMap.at(std::string(key)).get(); }
    return nullptr;
}

bool CompoundTag::contains(std::string_view key) const {
    return mTagMap.contains(key) && !mTagMap.at(std::string(key)).hold(Type::End);
}

bool CompoundTag::contains(std::string_view key, Tag::Type type) const {
    if (contains(key)) { return mTagMap.at(std::string(key)).hold(type); }
    return false;
}

bool CompoundTag::empty() const noexcept { return (size() == 0); }

bool CompoundTag::remove(std::string_view index) {
    if (mTagMap.contains(index)) {
        mTagMap.erase(std::string(index));
        return true;
    }
    return false;
}

bool CompoundTag::rename(std::string_view index, std::string_view newName) {
    if (contains(index)) {
        mTagMap[std::string(newName)] = mTagMap.at(std::string(index));
        remove(index);
        return true;
    }
    return false;
}

void CompoundTag::clear() noexcept { mTagMap.clear(); }

CompoundTag::iterator CompoundTag::begin() noexcept { return mTagMap.begin(); }
CompoundTag::iterator CompoundTag::end() noexcept { return mTagMap.end(); }

CompoundTag::const_iterator CompoundTag::begin() const noexcept { return cbegin(); }
CompoundTag::const_iterator CompoundTag::end() const noexcept { return cend(); }

CompoundTag::reverse_iterator CompoundTag::rbegin() noexcept { return mTagMap.rbegin(); }
CompoundTag::reverse_iterator CompoundTag::rend() noexcept { return mTagMap.rend(); }

CompoundTag::const_iterator CompoundTag::cbegin() const noexcept { return mTagMap.cbegin(); }
CompoundTag::const_iterator CompoundTag::cend() const noexcept { return mTagMap.cend(); }

CompoundTag::const_reverse_iterator CompoundTag::crbegin() const noexcept { return mTagMap.crbegin(); }
CompoundTag::const_reverse_iterator CompoundTag::crend() const noexcept { return mTagMap.crend(); }

CompoundTag::iterator CompoundTag::erase(const_iterator where) noexcept { return mTagMap.erase(where); }
CompoundTag::iterator CompoundTag::erase(const_iterator first, const_iterator last) noexcept {
    return mTagMap.erase(first, last);
}

CompoundTag::TagMap&       CompoundTag::items() noexcept { return mTagMap; }
CompoundTag::TagMap const& CompoundTag::items() const noexcept { return mTagMap; }

CompoundTag::TagMap&       CompoundTag::storage() noexcept { return mTagMap; }
CompoundTag::TagMap const& CompoundTag::storage() const noexcept { return mTagMap; }

void CompoundTag::serialize(bstream::BinaryStream& stream) const {
    stream.writeByte(static_cast<std::byte>(Type::Compound));
    stream.writeString("");
    write(stream);
}

void CompoundTag::serialize(io::BytesDataOutput& stream) const {
    stream.writeByte(static_cast<uint8_t>(Type::Compound));
    stream.writeString("");
    write(stream);
}

void CompoundTag::deserialize(bstream::ReadOnlyBinaryStream& stream) {
    auto tagType = static_cast<Type>(stream.getByte());
    (void)stream.getStringView();
    if (tagType == Type::Compound) { load(stream); }
}

void CompoundTag::deserialize(io::BytesDataInput& stream) {
    auto tagType = static_cast<Type>(stream.getByte());
    (void)stream.getStringView();
    if (tagType == Type::Compound) { load(stream); }
}

CompoundTag CompoundTag::fromBinaryNbt(std::string_view binaryData, bool isLittleEndian) {
    io::BytesDataInput stream(binaryData, false, isLittleEndian);
    CompoundTag        result;
    result.deserialize(stream);
    return result;
}

CompoundTag CompoundTag::fromBinaryNbtWithHeader(std::string_view binaryData, bool isLittleEndian) {
    io::BytesDataInput stream(binaryData, false, isLittleEndian);
    stream.ignoreBytes(sizeof(int));
    return fromBinaryNbt(stream.getLongStringView(), isLittleEndian);
}

std::string CompoundTag::toBinaryNbt(bool isLittleEndian) const noexcept {
    io::BytesDataOutput stream(isLittleEndian);
    serialize(stream);
    return stream.getAndReleaseData();
}

std::string CompoundTag::toBinaryNbtWithHeader(bool isLittleEndian, std::optional<int> storageVersion) const noexcept {
    io::BytesDataOutput stream(isLittleEndian);
    int                 storage_version = 0;
    if (storageVersion.has_value()) {
        storage_version = *storageVersion;
    } else if (contains("StorageVersion")) {
        auto& version = at("StorageVersion");
        if (version.getType() == Type::Int) { storage_version = version; }
    }
    stream.writeInt(storage_version);
    stream.writeLongString(toBinaryNbt(isLittleEndian));
    return stream.getAndReleaseData();
}

CompoundTag CompoundTag::fromNetworkNbt(std::string_view binaryData) {
    bstream::ReadOnlyBinaryStream stream(binaryData, false);
    CompoundTag                   result;
    result.deserialize(stream);
    return result;
}

std::string CompoundTag::toNetworkNbt() const noexcept {
    bstream::BinaryStream stream;
    serialize(stream);
    return stream.getAndReleaseData();
}

CompoundTagVariant& CompoundTag::at(std::string_view index) {
    if (!contains(index)) { throw std::out_of_range(std::format("Tag not contains key: {}", index)); }
    return mTagMap.at(std::string(index));
}
CompoundTagVariant const& CompoundTag::at(std::string_view index) const {
    if (!contains(index)) { throw std::out_of_range(std::format("Tag not contains key: {}", index)); }
    return mTagMap.at(std::string(index));
}

CompoundTagVariant&       CompoundTag::operator[](std::string_view index) { return mTagMap[std::string(index)]; }
CompoundTagVariant const& CompoundTag::operator[](std::string_view index) const {
    if (!contains(index)) { throw std::out_of_range(std::format("Tag not contains key: {}", index)); }
    return mTagMap.at(std::string(index));
}

size_t CompoundTag::size() const noexcept {
    size_t result = 0;
    for (auto& [_, v] : mTagMap) {
        if (!v.hold(Type::End)) { result++; }
    }
    return result;
}

std::optional<CompoundTag> CompoundTag::fromSnbt(std::string_view snbt, std::optional<size_t> parsedLength) noexcept {
    return CompoundTagVariant::parse(snbt, parsedLength)
        .and_then([](CompoundTagVariant&& val) -> std::optional<CompoundTag> {
            if (val.hold<CompoundTag>()) {
                return std::move(val.as<CompoundTag>());
            } else {
                return std::nullopt;
            }
        });
}

std::optional<CompoundTag> CompoundTag::fromJson(std::string_view snbt, std::optional<size_t> parsedLength) noexcept {
    return CompoundTagVariant::parseJson(snbt, parsedLength)
        .and_then([](CompoundTagVariant&& val) -> std::optional<CompoundTag> {
            if (val.hold<CompoundTag>()) {
                return std::move(val.as<CompoundTag>());
            } else {
                return std::nullopt;
            }
        });
}

int CompoundTag::readHeaderVersion(std::string_view content, bool isLittleEndian) noexcept {
    io::BytesDataInput stream(content, isLittleEndian);
    return stream.getInt();
}

bool CompoundTag::validateNetworkNbt(std::string_view binaryData) {
    return io::validateContent(binaryData, NbtFileFormat::BedrockNetwork);
}

bool CompoundTag::validateBinaryNbt(std::string_view binaryData, bool isLittleEndian) {
    return io::validateContent(binaryData, isLittleEndian ? NbtFileFormat::LittleEndian : NbtFileFormat::BigEndian);
}

bool CompoundTag::validateBinaryNbtWithHeader(std::string_view binaryData, bool isLittleEndian) {
    return io::validateContent(
        binaryData,
        isLittleEndian ? NbtFileFormat::LittleEndianWithHeader : NbtFileFormat::BigEndianWithHeader
    );
}

} // namespace nbt