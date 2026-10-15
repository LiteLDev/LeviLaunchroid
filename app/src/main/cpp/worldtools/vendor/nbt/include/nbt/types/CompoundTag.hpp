// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <map>
#include <nbt/types/Tag.hpp>
#include <optional>
#include <vector>

namespace nbt {

class CompoundTagVariant;
class EndTag;
class ByteTag;
class ShortTag;
class IntTag;
class LongTag;
class FloatTag;
class DoubleTag;
class ByteArrayTag;
class StringTag;
class ListTag;
class IntArrayTag;
class LongArrayTag;

class CompoundTag : public Tag {
public:
    using TagMap = std::map<std::string, CompoundTagVariant, std::less<>>;

public:
    TagMap mTagMap{};

public:
    using iterator               = TagMap::iterator;
    using const_iterator         = TagMap::const_iterator;
    using reverse_iterator       = TagMap::reverse_iterator;
    using const_reverse_iterator = TagMap::const_reverse_iterator;

public:
    [[nodiscard]] NBT_API CompoundTag() = default;
    [[nodiscard]] NBT_API CompoundTag(std::initializer_list<TagMap::value_type> tagPairs) : mTagMap(tagPairs) {}

    [[nodiscard]] NBT_API Type getType() const override;

    [[nodiscard]] NBT_API bool equals(Tag const& other) const override;

    [[nodiscard]] NBT_API std::unique_ptr<Tag> copy() const override;

    [[nodiscard]] NBT_API std::size_t hash() const override;

    NBT_API void write(io::BytesDataOutput& stream) const override;

    NBT_API void load(io::BytesDataInput& stream) override;

    NBT_API void write(bstream::BinaryStream& stream) const override;

    NBT_API void load(bstream::ReadOnlyBinaryStream& stream) override;

    NBT_API void merge(CompoundTag const& other, bool mergeList = false);

    [[nodiscard]] NBT_API TagMap&       items() noexcept;
    [[nodiscard]] NBT_API TagMap const& items() const noexcept;

    [[nodiscard]] NBT_API TagMap&       storage() noexcept;
    [[nodiscard]] NBT_API TagMap const& storage() const noexcept;

public:
    [[nodiscard]] NBT_API CompoundTagVariant&       operator[](std::string_view index);
    [[nodiscard]] NBT_API CompoundTagVariant const& operator[](std::string_view index) const;

    template <size_t N>
    [[nodiscard]] CompoundTagVariant& operator[](char const (&index)[N]) {
        return operator[](std::string_view{index, N - 1});
    }

    template <size_t N>
    [[nodiscard]] CompoundTagVariant const& operator[](char const (&index)[N]) const {
        return operator[](std::string_view{index, N - 1});
    }

    [[nodiscard]] NBT_API CompoundTagVariant&       at(std::string_view index);
    [[nodiscard]] NBT_API CompoundTagVariant const& at(std::string_view index) const;

    [[nodiscard]] NBT_API iterator begin() noexcept;
    [[nodiscard]] NBT_API iterator end() noexcept;

    [[nodiscard]] NBT_API const_iterator begin() const noexcept;
    [[nodiscard]] NBT_API const_iterator end() const noexcept;

    [[nodiscard]] NBT_API reverse_iterator rbegin() noexcept;
    [[nodiscard]] NBT_API reverse_iterator rend() noexcept;

    [[nodiscard]] NBT_API const_iterator cbegin() const noexcept;
    [[nodiscard]] NBT_API const_iterator cend() const noexcept;

    [[nodiscard]] NBT_API const_reverse_iterator crbegin() const noexcept;
    [[nodiscard]] NBT_API const_reverse_iterator crend() const noexcept;

    NBT_API iterator erase(const_iterator where) noexcept;
    NBT_API iterator erase(const_iterator first, const_iterator last) noexcept;

public:
    NBT_API bool put(std::string_view key, Tag&& tag);
    NBT_API bool put(std::string_view key, std::unique_ptr<Tag>&& tag);

    NBT_API void set(std::string_view key, Tag&& tag);
    NBT_API void set(std::string_view key, std::unique_ptr<Tag>&& tag);

    [[nodiscard]] NBT_API Tag const* get(std::string_view key) const;
    [[nodiscard]] NBT_API Tag*       get(std::string_view key);

    [[nodiscard]] NBT_API bool contains(std::string_view key) const;
    [[nodiscard]] NBT_API bool contains(std::string_view key, Type type) const;

    [[nodiscard]] NBT_API bool empty() const noexcept;

    [[nodiscard]] NBT_API size_t size() const noexcept;

    NBT_API bool remove(std::string_view index);
    NBT_API bool rename(std::string_view index, std::string_view newName);
    NBT_API void clear() noexcept;

    [[nodiscard]] NBT_API std::unique_ptr<CompoundTag> clone() const;

public:
    NBT_API void serialize(bstream::BinaryStream& stream) const;
    NBT_API void serialize(io::BytesDataOutput& stream) const;

    NBT_API void deserialize(bstream::ReadOnlyBinaryStream& stream);
    NBT_API void deserialize(io::BytesDataInput& stream);

    [[nodiscard]] NBT_API std::string toNetworkNbt() const noexcept;
    [[nodiscard]] NBT_API std::string toBinaryNbt(bool isLittleEndian = true) const noexcept;
    [[nodiscard]] NBT_API             std::string
    toBinaryNbtWithHeader(bool isLittleEndian = true, std::optional<int> storageVersion = std::nullopt) const noexcept;

public:
    [[nodiscard]] NBT_API static CompoundTag fromNetworkNbt(std::string_view binaryData);
    [[nodiscard]] NBT_API static CompoundTag fromBinaryNbt(std::string_view binaryData, bool isLittleEndian = true);
    [[nodiscard]] NBT_API static CompoundTag
    fromBinaryNbtWithHeader(std::string_view binaryData, bool isLittleEndian = true);

    [[nodiscard]] NBT_API static bool validateNetworkNbt(std::string_view binaryData);
    [[nodiscard]] NBT_API static bool validateBinaryNbt(std::string_view binaryData, bool isLittleEndian = true);
    [[nodiscard]] NBT_API static bool
    validateBinaryNbtWithHeader(std::string_view binaryData, bool isLittleEndian = true);

    [[nodiscard]] NBT_API static std::optional<CompoundTag>
    fromSnbt(std::string_view snbt, std::optional<size_t> parsedLength = {}) noexcept;
    [[nodiscard]] NBT_API static std::optional<CompoundTag>
    fromJson(std::string_view snbt, std::optional<size_t> parsedLength = {}) noexcept;

    [[nodiscard]] NBT_API static int readHeaderVersion(std::string_view content, bool isLittleEndian = true) noexcept;
};

} // namespace nbt
