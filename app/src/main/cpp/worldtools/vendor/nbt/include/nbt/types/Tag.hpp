// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <bstream.hpp>
#include <memory>
#include <nbt/io/BytesDataOutput.hpp>
#include <nbt/types/SnbtFormat.hpp>
#include <stdexcept>

namespace nbt {

class CompoundTagVariant;

class Tag {
public:
    enum class Type : uint8_t {
        End         = 0,
        Byte        = 1,
        Short       = 2,
        Int         = 3,
        Long        = 4,
        Float       = 5,
        Double      = 6,
        ByteArray   = 7,
        String      = 8,
        List        = 9,
        Compound    = 10,
        IntArray    = 11,
        LongArray   = 12,
        NumTagTypes = 13,
    };

    virtual ~Tag() = default;

    [[nodiscard]] virtual Type getType() const = 0;

    [[nodiscard]] virtual bool equals(Tag const& other) const = 0;

    [[nodiscard]] virtual std::unique_ptr<Tag> copy() const = 0;

    [[nodiscard]] virtual std::size_t hash() const = 0;

    virtual void write(io::BytesDataOutput& stream) const = 0;

    virtual void load(io::BytesDataInput& stream) = 0;

    virtual void write(bstream::BinaryStream& stream) const = 0;

    virtual void load(bstream::ReadOnlyBinaryStream& stream) = 0;

public:
    [[nodiscard]] NBT_API bool operator==(Tag const& other) const;

    [[nodiscard]] NBT_API CompoundTagVariant&       operator[](size_t index);
    [[nodiscard]] NBT_API CompoundTagVariant const& operator[](size_t index) const;

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

    [[nodiscard]] NBT_API std::string toSnbt(
        SnbtFormat       snbtFormat   = SnbtFormat::Default,
        uint8_t          indent       = 4,
        SnbtNumberFormat numberFormat = SnbtNumberFormat::Default
    ) const noexcept;

    [[nodiscard]] NBT_API std::string toJson(uint8_t indent = 4) const noexcept;

    template <typename T>
        requires std::derived_from<std::remove_cvref_t<T>, Tag>
    [[nodiscard]] constexpr T& as() noexcept {
        return static_cast<std::remove_cvref_t<T>&>(*this);
    }

    template <typename T>
        requires std::derived_from<std::remove_cvref_t<T>, Tag>
    [[nodiscard]] constexpr T const& as() const noexcept {
        return static_cast<std::remove_cvref_t<T> const&>(*this);
    }

    template <typename T>
        requires std::derived_from<std::remove_cvref_t<T>, Tag>
    [[nodiscard]] constexpr operator T&() noexcept {
        return as<std::remove_cvref_t<T>&>();
    }

    template <typename T>
        requires std::derived_from<std::remove_cvref_t<T>, Tag>
    [[nodiscard]] constexpr operator T const&() const noexcept {
        return as<std::remove_cvref_t<T> const&>();
    }

    template <typename T>
        requires std::derived_from<std::remove_cvref_t<T>, Tag>
    constexpr T& operator=(T const& other) {
        return as<std::remove_cvref_t<T>&>().T::operator=(other);
    }

    [[nodiscard]] NBT_API operator std::string&();
    [[nodiscard]] NBT_API operator std::string const&() const;

    [[nodiscard]] NBT_API operator std::byte() const;

public:
    [[nodiscard]] NBT_API static std::unique_ptr<Tag> newTag(Type type);
};

} // namespace nbt
