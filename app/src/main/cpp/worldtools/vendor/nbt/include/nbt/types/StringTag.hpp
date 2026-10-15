// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <nbt/types/Tag.hpp>

namespace nbt {

class StringTag : public Tag {
public:
    std::string mStorage{};

public:
    [[nodiscard]] NBT_API StringTag() = default;
    [[nodiscard]] NBT_API StringTag(std::string_view str);
    [[nodiscard]] NBT_API StringTag(std::string str);
    [[nodiscard]] NBT_API StringTag(const char* data, size_t size);

    template <size_t N>
    [[nodiscard]] constexpr StringTag(char const (&str)[N]) : mStorage(str) {}

    [[nodiscard]] NBT_API operator std::string const&() const;
    [[nodiscard]] NBT_API operator std::string&();

    [[nodiscard]] NBT_API Type getType() const override;

    [[nodiscard]] NBT_API bool equals(Tag const& other) const override;

    [[nodiscard]] NBT_API std::unique_ptr<Tag> copy() const override;

    [[nodiscard]] NBT_API std::size_t hash() const override;

    NBT_API void write(io::BytesDataOutput& stream) const override;

    NBT_API void load(io::BytesDataInput& stream) override;

    NBT_API void write(bstream::BinaryStream& stream) const override;

    NBT_API void load(bstream::ReadOnlyBinaryStream& stream) override;

public:
    [[nodiscard]] NBT_API std::string& storage() noexcept;
    [[nodiscard]] NBT_API std::string const& storage() const noexcept;
    [[nodiscard]] NBT_API std::string_view view() const noexcept;
};

} // namespace nbt
