// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <nbt/types/Tag.hpp>

namespace nbt {

class DoubleTag : public Tag {
public:
    double mStorage{0};

public:
    [[nodiscard]] constexpr DoubleTag() = default;

    [[nodiscard]] constexpr explicit DoubleTag(double value) noexcept : mStorage(value) {}

    constexpr DoubleTag& operator=(double value) noexcept {
        mStorage = value;
        return *this;
    }

    [[nodiscard]] constexpr operator double() const noexcept { return mStorage; }

    [[nodiscard]] NBT_API Type getType() const override;

    [[nodiscard]] NBT_API bool equals(Tag const& other) const override;

    [[nodiscard]] NBT_API std::unique_ptr<Tag> copy() const override;

    [[nodiscard]] NBT_API std::size_t hash() const override;

    NBT_API void write(io::BytesDataOutput& stream) const override;

    NBT_API void load(io::BytesDataInput& stream) override;

    NBT_API void write(bstream::BinaryStream& stream) const override;

    NBT_API void load(bstream::ReadOnlyBinaryStream& stream) override;

    [[nodiscard]] constexpr double&       storage() noexcept { return mStorage; }
    [[nodiscard]] constexpr double const& storage() const noexcept { return mStorage; }
};

} // namespace nbt
