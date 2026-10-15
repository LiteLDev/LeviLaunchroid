// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <nbt/types/Tag.hpp>
#include <vector>

namespace nbt {

class LongArrayTag : public Tag {
public:
    std::vector<int64_t> mStorage;

public:
    [[nodiscard]] NBT_API LongArrayTag() = default;

    [[nodiscard]] NBT_API LongArrayTag(std::vector<int64_t> const& arr);
    [[nodiscard]] NBT_API LongArrayTag(std::initializer_list<int64_t> val);

    [[nodiscard]] NBT_API operator std::vector<int64_t> const&() const;
    [[nodiscard]] NBT_API operator std::vector<int64_t>&();

    [[nodiscard]] NBT_API Type getType() const override;

    [[nodiscard]] NBT_API bool equals(Tag const& other) const override;

    [[nodiscard]] NBT_API std::unique_ptr<Tag> copy() const override;

    [[nodiscard]] NBT_API std::size_t hash() const override;

    NBT_API void write(io::BytesDataOutput& stream) const override;

    NBT_API void load(io::BytesDataInput& stream) override;

    NBT_API void write(bstream::BinaryStream& stream) const override;

    NBT_API void load(bstream::ReadOnlyBinaryStream& stream) override;

    [[nodiscard]] NBT_API std::vector<int64_t>& storage() noexcept;
    [[nodiscard]] NBT_API std::vector<int64_t> const& storage() const noexcept;

    [[nodiscard]] NBT_API size_t size() const noexcept;

    NBT_API void reserve(size_t size);

    NBT_API bool remove(size_t index);
    NBT_API bool remove(size_t startIndex, size_t endIndex);
    NBT_API void clear() noexcept;

    [[nodiscard]] NBT_API int64_t&       operator[](size_t index) noexcept;
    [[nodiscard]] NBT_API int64_t const& operator[](size_t index) const noexcept;

    [[nodiscard]] NBT_API int64_t&       at(size_t index);
    [[nodiscard]] NBT_API int64_t const& at(size_t index) const;

    NBT_API void push_back(int64_t val);

    NBT_API LongArrayTag& operator=(std::vector<int64_t> const& value);
};

} // namespace nbt
