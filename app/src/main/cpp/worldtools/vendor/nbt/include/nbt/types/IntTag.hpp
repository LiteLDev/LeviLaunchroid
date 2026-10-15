// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <nbt/types/Tag.hpp>

namespace nbt {

class IntTag : public Tag {
public:
    int mStorage{0};

public:
    [[nodiscard]] constexpr IntTag() = default;

    template <std::integral T>
    [[nodiscard]] constexpr explicit IntTag(T value) noexcept : mStorage(static_cast<int>(value)) {}

    template <std::integral T>
    constexpr IntTag& operator=(T value) noexcept {
        mStorage = static_cast<int>(value);
        return *this;
    }

    template <std::integral T>
    [[nodiscard]] constexpr operator T() const noexcept {
        return static_cast<T>(mStorage);
    }

    [[nodiscard]] NBT_API IntTag operator+() const noexcept;

    [[nodiscard]] NBT_API Type getType() const override;

    [[nodiscard]] NBT_API bool equals(Tag const& other) const override;

    [[nodiscard]] NBT_API std::unique_ptr<Tag> copy() const override;

    [[nodiscard]] NBT_API std::size_t hash() const override;

    NBT_API void write(io::BytesDataOutput& stream) const override;

    NBT_API void load(io::BytesDataInput& stream) override;

    NBT_API void write(bstream::BinaryStream& stream) const override;

    NBT_API void load(bstream::ReadOnlyBinaryStream& stream) override;

    [[nodiscard]] constexpr int&       storage() noexcept { return mStorage; }
    [[nodiscard]] constexpr int const& storage() const noexcept { return mStorage; }
};

} // namespace nbt
