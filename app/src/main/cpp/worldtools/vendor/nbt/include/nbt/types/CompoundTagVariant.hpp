// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <nbt/types/ByteArrayTag.hpp>
#include <nbt/types/ByteTag.hpp>
#include <nbt/types/CompoundTag.hpp>
#include <nbt/types/DoubleTag.hpp>
#include <nbt/types/EndTag.hpp>
#include <nbt/types/FloatTag.hpp>
#include <nbt/types/IntArrayTag.hpp>
#include <nbt/types/IntTag.hpp>
#include <nbt/types/ListTag.hpp>
#include <nbt/types/LongArrayTag.hpp>
#include <nbt/types/LongTag.hpp>
#include <nbt/types/ShortTag.hpp>
#include <nbt/types/StringTag.hpp>
#include <nbt/types/Tag.hpp>
#include <optional>
#include <stdexcept>
#include <utility>
#include <variant>

namespace nbt {

class CompoundTagVariant {
public:
    using TagVariant = std::variant<
        EndTag,
        ByteTag,
        ShortTag,
        IntTag,
        LongTag,
        FloatTag,
        DoubleTag,
        ByteArrayTag,
        StringTag,
        ListTag,
        CompoundTag,
        IntArrayTag,
        LongArrayTag>;

public:
    TagVariant mStorage{};

public:
    template <bool Const>
    class Iterator {
    public:
        using reference = std::conditional_t<Const, Tag const, Tag>&;
        using pointer   = std::add_pointer_t<reference>;

        std::conditional_t<
            Const,
            std::variant<CompoundTagVariant const*, CompoundTag::const_iterator, ListTag::const_iterator>,
            std::variant<CompoundTagVariant*, CompoundTag::iterator, ListTag::iterator>>
            iter;

        [[nodiscard]] static Iterator makeBegin(auto& var) noexcept {
            Iterator res;
            switch (var.index()) {
            case Tag::Type::List:
                res.iter.template emplace<2>(var.template as<ListTag>().begin());
                break;
            case Tag::Type::Compound:
                res.iter.template emplace<1>(var.template as<CompoundTag>().begin());
                break;
            case Tag::Type::End:
                res.iter.template emplace<0>(std::addressof(var) + 1);
                break;
            default:
                res.iter.template emplace<0>(std::addressof(var));
            }
            return res;
        }

        [[nodiscard]] static Iterator makeEnd(auto& var) noexcept {
            Iterator res;
            switch (var.index()) {
            case Tag::Type::List:
                res.iter.template emplace<2>(var.template as<ListTag>().end());
                break;
            case Tag::Type::Compound:
                res.iter.template emplace<1>(var.template as<CompoundTag>().end());
                break;
            default:
                res.iter.template emplace<0>(std::addressof(var) + 1);
            }
            return res;
        }

        [[nodiscard]] reference operator*() const noexcept {
            switch (iter.index()) {
            case 1:
                return *std::get<1>(iter)->second.get();
            case 2:
                return *std::get<2>(iter)->get();
            default:
                return *std::get<0>(iter)->get();
            }
        }

        [[nodiscard]] pointer operator->() const noexcept { return std::addressof(**this); }

        Iterator& operator++() noexcept {
            std::visit([](auto& val) { ++val; }, iter);
            return *this;
        }

        Iterator operator++(int) noexcept {
            Iterator tmp = *this;
            ++*this;
            return tmp;
        }

        Iterator& operator--() noexcept {
            std::visit([](auto& val) { --val; }, iter);
            return *this;
        }

        Iterator operator--(int) noexcept {
            Iterator tmp = *this;
            --*this;
            return tmp;
        }

        [[nodiscard]] bool operator==(Iterator const& r) const noexcept { return this->iter == r.iter; }
    };

public:
    [[nodiscard]] NBT_API CompoundTagVariant(Tag&& tag);
    [[nodiscard]] NBT_API CompoundTagVariant(std::unique_ptr<Tag>&& tag);

    [[nodiscard]] NBT_API CompoundTagVariant()                          = default;
    [[nodiscard]] NBT_API CompoundTagVariant(CompoundTagVariant&&)      = default;
    [[nodiscard]] NBT_API CompoundTagVariant(CompoundTagVariant const&) = default;

    CompoundTagVariant& operator=(CompoundTagVariant&&)      = default;
    CompoundTagVariant& operator=(CompoundTagVariant const&) = default;

    template <class T>
        requires(requires(T o) { mStorage = std::move(o); })
    constexpr CompoundTagVariant& operator=(T other) {
        mStorage = std::move(other);
        return *this;
    }

public:
    template <typename T, typename... Args>
    T& emplace(Args&&... args) {
        mStorage = T(std::forward<Args>(args)...);
        return std::get<T>(mStorage);
    }

    NBT_API Tag& emplace(Tag&& tag);

    template <typename T>
        requires std::derived_from<std::remove_cvref_t<T>, Tag>
    [[nodiscard]] constexpr T& as() noexcept {
        return static_cast<std::remove_cvref_t<T>&>(*get());
    }

    template <typename T>
        requires std::derived_from<std::remove_cvref_t<T>, Tag>
    [[nodiscard]] constexpr T const& as() const noexcept {
        return static_cast<std::remove_cvref_t<T> const&>(*get());
    }

    [[nodiscard]] NBT_API const Tag* operator->() const;
    [[nodiscard]] NBT_API Tag*       operator->();

    [[nodiscard]] NBT_API const Tag* get() const;
    [[nodiscard]] NBT_API Tag*       get();

    [[nodiscard]] NBT_API const Tag& operator*() const;
    [[nodiscard]] NBT_API Tag&       operator*();

    [[nodiscard]] NBT_API bool operator==(CompoundTagVariant const& other) const;

public:
    using iterator       = Iterator<false>;
    using const_iterator = Iterator<true>;

    [[nodiscard]] NBT_API CompoundTagVariant(std::nullptr_t);
    [[nodiscard]] NBT_API CompoundTagVariant(TagVariant tag);
    [[nodiscard]] NBT_API CompoundTagVariant(Tag const& tag);
    [[nodiscard]] NBT_API CompoundTagVariant(std::initializer_list<CompoundTag::TagMap::value_type> tagPairs);

    template <class T, class... Args>
    [[nodiscard]] constexpr CompoundTagVariant(std::in_place_type_t<T>, Args&&... args)
    : mStorage(std::in_place_type<T>, std::forward<Args>(args)...) {}

    template <typename T>
        requires std::derived_from<std::remove_cvref_t<T>, Tag>
    [[nodiscard]] constexpr CompoundTagVariant(T tag) : mStorage(std::move(tag)) {}

    template <std::integral T>
    [[nodiscard]] constexpr CompoundTagVariant(T integer) {
        constexpr size_t size = sizeof(T);
        if constexpr (size == 1) {
            mStorage = ByteTag{integer};
        } else if constexpr (size == 2) {
            mStorage = ShortTag{integer};
        } else if constexpr (size == 4) {
            mStorage = IntTag{integer};
        } else {
            mStorage = LongTag{integer};
        }
    }

    [[nodiscard]] constexpr CompoundTagVariant(std::byte b) : mStorage(ByteTag{b}) {}

    [[nodiscard]] constexpr CompoundTagVariant(float f) : mStorage(FloatTag{f}) {}

    [[nodiscard]] constexpr CompoundTagVariant(double d) : mStorage(DoubleTag{d}) {}

    [[nodiscard]] NBT_API CompoundTagVariant(std::string s);

    [[nodiscard]] NBT_API CompoundTagVariant(std::string_view s);

    template <size_t N>
    [[nodiscard]] constexpr CompoundTagVariant(char const (&str)[N])
    : CompoundTagVariant(std::string_view{str, N - 1}) {}

    NBT_API size_t hash() const;

    NBT_API void write(io::BytesDataOutput& stream) const;

    NBT_API void load(io::BytesDataInput& stream);

    NBT_API void write(bstream::BinaryStream& stream) const;

    NBT_API void load(bstream::ReadOnlyBinaryStream& stream);

    [[nodiscard]] NBT_API iterator       begin() noexcept;
    [[nodiscard]] NBT_API const_iterator begin() const noexcept;
    [[nodiscard]] NBT_API const_iterator cbegin() const noexcept;

    [[nodiscard]] NBT_API iterator       end() noexcept;
    [[nodiscard]] NBT_API const_iterator end() const noexcept;
    [[nodiscard]] NBT_API const_iterator cend() const noexcept;

    [[nodiscard]] constexpr Tag::Type index() const noexcept { return static_cast<Tag::Type>(mStorage.index()); }
    [[nodiscard]] constexpr Tag::Type getType() const noexcept { return index(); }

    NBT_API void clear();

    NBT_API bool equals(CompoundTagVariant const& other) const;

    template <std::derived_from<Tag> T>
    [[nodiscard]] constexpr bool hold() const noexcept {
        return std::holds_alternative<T>(mStorage);
    }
    [[nodiscard]] constexpr bool hold(Tag::Type type) const noexcept { return getType() == type; }

    [[nodiscard]] constexpr bool is_array() const noexcept { return hold(Tag::Type::List); }
    [[nodiscard]] constexpr bool is_binary() const noexcept {
        return hold(Tag::Type::ByteArray) || hold(Tag::Type::IntArray) || hold(Tag::Type::LongArray);
    }
    [[nodiscard]] constexpr bool is_boolean() const noexcept { return hold(Tag::Type::Byte); }
    [[nodiscard]] constexpr bool is_null() const noexcept { return hold(Tag::Type::End); }
    [[nodiscard]] constexpr bool is_number_float() const noexcept {
        return hold(Tag::Type::Float) || hold(Tag::Type::Double);
    }
    [[nodiscard]] constexpr bool is_number_integer() const noexcept {
        return hold(Tag::Type::Byte) || hold(Tag::Type::Short) || hold(Tag::Type::Int) || hold(Tag::Type::Long);
    }
    [[nodiscard]] constexpr bool is_object() const noexcept { return hold(Tag::Type::Compound); }
    [[nodiscard]] constexpr bool is_string() const noexcept { return hold(Tag::Type::String); }
    [[nodiscard]] constexpr bool is_number() const noexcept { return is_number_float() || is_number_integer(); }
    [[nodiscard]] constexpr bool is_primitive() const noexcept {
        return is_null() || is_string() || is_number() || is_binary();
    }
    [[nodiscard]] constexpr bool is_structured() const noexcept { return is_array() || is_object(); }

    [[nodiscard]] NBT_API CompoundTag::TagMap const& items() const;
    [[nodiscard]] NBT_API CompoundTag::TagMap& items();

    [[nodiscard]] NBT_API bool contains(std::string_view key) const noexcept;

    [[nodiscard]] NBT_API bool contains(std::string_view key, Tag::Type type) const noexcept;

    [[nodiscard]] constexpr size_t size() const noexcept {
        switch (index()) {
        case Tag::Type::Byte:
        case Tag::Type::Short:
        case Tag::Type::Int:
        case Tag::Type::Long:
        case Tag::Type::Float:
        case Tag::Type::Double:
        case Tag::Type::String:
            return 1;
        case Tag::Type::List:
            return as<ListTag>().size();
        case Tag::Type::Compound:
            return as<CompoundTag>().size();
        case Tag::Type::IntArray:
            return as<IntArrayTag>().size();
        case Tag::Type::ByteArray:
            return as<ByteArrayTag>().size();
        default:
            return 0;
        }
    }

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

    [[nodiscard]] NBT_API std::unique_ptr<Tag> toUniqueCopy() const&;
    [[nodiscard]] NBT_API std::unique_ptr<Tag> toUnique() &&;

    NBT_API bool remove(std::string_view index);
    NBT_API bool remove(size_t index);

    NBT_API bool rename(std::string_view index, std::string_view newName);

    NBT_API void push_back(CompoundTagVariant val);
    NBT_API void push_back(Tag const& val);
    NBT_API void push_back(std::unique_ptr<Tag>&& val);

    template <typename T>
        requires std::is_arithmetic_v<T>
    [[nodiscard]] constexpr operator T() const {
        return std::visit(
            [](auto& val) -> T {
                if constexpr (std::is_convertible_v<std::decay_t<decltype(val)>, T>) {
                    return static_cast<T>(val);
                } else {
                    throw std::runtime_error("tag can not convert to a number");
                }
            },
            mStorage
        );
    }

    [[nodiscard]] NBT_API operator std::byte() const;

    [[nodiscard]] NBT_API operator std::string const&() const;
    [[nodiscard]] NBT_API operator std::string&();

    [[nodiscard]] NBT_API operator std::vector<uint8_t> const&() const;
    [[nodiscard]] NBT_API operator std::vector<uint8_t>&();

    [[nodiscard]] NBT_API operator std::vector<int> const&() const;
    [[nodiscard]] NBT_API operator std::vector<int>&();

    [[nodiscard]] NBT_API operator std::vector<int64_t> const&() const;
    [[nodiscard]] NBT_API operator std::vector<int64_t>&();

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

    [[nodiscard]] NBT_API std::string toSnbt(
        SnbtFormat       snbtFormat   = SnbtFormat::Default,
        uint8_t          indent       = 4,
        SnbtNumberFormat numberFormat = SnbtNumberFormat::Default
    ) const noexcept;

    [[nodiscard]] NBT_API std::string toJson(uint8_t indent = 4) const noexcept;

    NBT_API void merge(CompoundTagVariant const& other, bool mergeList = false);

public:
    [[nodiscard]] NBT_API static CompoundTagVariant
    object(std::initializer_list<CompoundTag::TagMap::value_type> init = {}) {
        return CompoundTagVariant{std::in_place_type<CompoundTag>, init};
    }

    [[nodiscard]] NBT_API static CompoundTagVariant array(std::initializer_list<CompoundTagVariant> init = {}) {
        return CompoundTagVariant{std::in_place_type<ListTag>, init};
    }

    [[nodiscard]] NBT_API static std::optional<CompoundTagVariant>
    parse(std::string_view snbt, std::optional<size_t> parsedLength = {}) noexcept;

    [[nodiscard]] NBT_API static std::optional<CompoundTagVariant>
    parseJson(std::string_view snbt, std::optional<size_t> parsedLength = {}) noexcept;
};

} // namespace nbt
