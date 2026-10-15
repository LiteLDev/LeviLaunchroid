// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <nbt/types/Tag.hpp>

namespace nbt {

class ByteTag : public Tag {
public:
    uint8_t mStorage{0};

public:
    [[nodiscard]] constexpr ByteTag() = default;

    template <std::integral T>
    [[nodiscard]] constexpr explicit ByteTag(T value) : mStorage(static_cast<uint8_t>(value)) {}

    [[nodiscard]] constexpr explicit ByteTag(std::byte byte) : mStorage(std::to_integer<uint8_t>(byte)) {}

    template <std::integral T>
    constexpr ByteTag& operator=(T value) {
        mStorage = static_cast<uint8_t>(value);
        return *this;
    }

    template <std::integral T>
    [[nodiscard]] constexpr operator T() const {
        return static_cast<T>(mStorage);
    }

    [[nodiscard]] constexpr operator std::byte() const { return static_cast<std::byte>(mStorage); }

    [[nodiscard]] NBT_API ByteTag operator+() const;

    [[nodiscard]] NBT_API Type getType() const override;

    [[nodiscard]] NBT_API bool equals(Tag const& other) const override;

    [[nodiscard]] NBT_API std::unique_ptr<Tag> copy() const override;

    [[nodiscard]] NBT_API std::size_t hash() const override;

    NBT_API void write(io::BytesDataOutput& stream) const override;

    NBT_API void load(io::BytesDataInput& stream) override;

    NBT_API void write(bstream::BinaryStream& stream) const override;

    NBT_API void load(bstream::ReadOnlyBinaryStream& stream) override;

    [[nodiscard]] constexpr uint8_t&       storage() noexcept { return mStorage; }
    [[nodiscard]] constexpr uint8_t const& storage() const noexcept { return mStorage; }
};

} // namespace nbt
