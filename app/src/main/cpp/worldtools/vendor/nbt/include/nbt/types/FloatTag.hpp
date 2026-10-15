// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <nbt/types/Tag.hpp>

namespace nbt {

class FloatTag : public Tag {
public:
    float mStorage{0};

public:
    [[nodiscard]] constexpr FloatTag() = default;

    [[nodiscard]] constexpr explicit FloatTag(float value) noexcept : mStorage(value) {}

    constexpr FloatTag& operator=(float value) noexcept {
        mStorage = value;
        return *this;
    }

    constexpr operator float() const noexcept { return mStorage; }

    [[nodiscard]] NBT_API Type getType() const override;

    [[nodiscard]] NBT_API bool equals(Tag const& other) const override;

    [[nodiscard]] NBT_API std::unique_ptr<Tag> copy() const override;

    [[nodiscard]] NBT_API std::size_t hash() const override;

    NBT_API void write(io::BytesDataOutput& stream) const override;

    NBT_API void load(io::BytesDataInput& stream) override;

    NBT_API void write(bstream::BinaryStream& stream) const override;

    NBT_API void load(bstream::ReadOnlyBinaryStream& stream) override;

    [[nodiscard]] constexpr float&       storage() noexcept { return mStorage; }
    [[nodiscard]] constexpr float const& storage() const noexcept { return mStorage; }
};

} // namespace nbt
