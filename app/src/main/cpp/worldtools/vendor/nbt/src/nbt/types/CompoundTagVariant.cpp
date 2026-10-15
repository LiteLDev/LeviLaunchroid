// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/CompoundTagVariant.hpp"
#include "nbt/detail/SnbtDeserializer.hpp"
#include "nbt/detail/SnbtSerializer.hpp"
#include "nbt/types/CompoundTag.hpp"

namespace nbt {

CompoundTagVariant::CompoundTagVariant(Tag&& tag) { emplace(std::move(tag)); }

CompoundTagVariant::CompoundTagVariant(std::unique_ptr<Tag>&& tag) { emplace(std::move(*tag)); }

CompoundTagVariant::CompoundTagVariant(std::nullptr_t) {}

CompoundTagVariant::CompoundTagVariant(TagVariant tag) : mStorage(std::move(tag)) {}

CompoundTagVariant::CompoundTagVariant(Tag const& tag) : CompoundTagVariant(tag.copy()) {}

CompoundTagVariant::CompoundTagVariant(std::initializer_list<CompoundTag::TagMap::value_type> tagPairs)
: mStorage(std::in_place_type<CompoundTag>, tagPairs) {}

CompoundTagVariant::CompoundTagVariant(std::string s) : mStorage(std::in_place_type<StringTag>, std::move(s)) {}

CompoundTagVariant::CompoundTagVariant(std::string_view s) : mStorage(std::in_place_type<StringTag>, std::string(s)) {}

size_t CompoundTagVariant::hash() const { return get()->hash(); }

void CompoundTagVariant::write(io::BytesDataOutput& stream) const { get()->write(stream); }

void CompoundTagVariant::load(io::BytesDataInput& stream) { get()->load(stream); }

void CompoundTagVariant::write(bstream::BinaryStream& stream) const { get()->write(stream); }

void CompoundTagVariant::load(bstream::ReadOnlyBinaryStream& stream) { get()->load(stream); }

CompoundTagVariant::iterator       CompoundTagVariant::begin() noexcept { return iterator::makeBegin(*this); }
CompoundTagVariant::const_iterator CompoundTagVariant::begin() const noexcept { return cbegin(); }
CompoundTagVariant::const_iterator CompoundTagVariant::cbegin() const noexcept {
    return const_iterator::makeBegin(*this);
}

CompoundTagVariant::iterator       CompoundTagVariant::end() noexcept { return iterator::makeEnd(*this); }
CompoundTagVariant::const_iterator CompoundTagVariant::end() const noexcept { return cend(); }
CompoundTagVariant::const_iterator CompoundTagVariant::cend() const noexcept { return const_iterator::makeEnd(*this); }

void CompoundTagVariant::clear() {
    std::visit(
        [](auto& tag) {
            if constexpr (requires { tag.clear(); }) {
                tag.clear();
            } else {
                throw std::runtime_error("tag not hold an object, array or binary");
            }
        },
        mStorage
    );
}

bool CompoundTagVariant::equals(CompoundTagVariant const& other) const { return get()->equals(*other.get()); }

CompoundTag::TagMap const& CompoundTagVariant::items() const { return as<CompoundTag>().items(); }
CompoundTag::TagMap&       CompoundTagVariant::items() { return as<CompoundTag>().items(); }

bool CompoundTagVariant::contains(std::string_view key) const noexcept {
    if (is_object()) { return as<CompoundTag>().contains(key); }
    return false;
}

bool CompoundTagVariant::contains(std::string_view key, Tag::Type type) const noexcept {
    if (is_object()) { return as<CompoundTag>().contains(key, type); }
    return false;
}

CompoundTagVariant& CompoundTagVariant::operator[](size_t index) {
    if (hold(Tag::Type::List)) {
        return as<ListTag>()[index];
    } else {
        throw std::runtime_error("tag not hold an array");
    }
}

CompoundTagVariant const& CompoundTagVariant::operator[](size_t index) const {
    if (hold(Tag::Type::List)) {
        return as<ListTag>()[index];
    } else {
        throw std::runtime_error("tag not hold an array");
    }
}

CompoundTagVariant& CompoundTagVariant::operator[](std::string_view index) {
    if (is_null()) { mStorage = CompoundTag(); }
    if (!hold(Tag::Type::Compound)) { throw std::runtime_error("tag not hold an object"); }
    return as<CompoundTag>()[index];
}

CompoundTagVariant const& CompoundTagVariant::operator[](std::string_view index) const {
    if (!hold(Tag::Type::Compound)) { throw std::runtime_error("tag not hold an object"); }
    return as<CompoundTag>()[index];
}

std::unique_ptr<Tag> CompoundTagVariant::toUniqueCopy() const& {
    return std::visit(
        [](auto const& val) -> std::unique_ptr<Tag> { return std::make_unique<std::decay_t<decltype(val)>>(val); },
        mStorage
    );
}

std::unique_ptr<Tag> CompoundTagVariant::toUnique() && {
    return std::visit(
        [](auto&& val) -> std::unique_ptr<Tag> {
            return std::make_unique<std::decay_t<decltype(val)>>(std::move(val));
        },
        mStorage
    );
}

bool CompoundTagVariant::remove(std::string_view index) {
    if (is_object()) { as<CompoundTag>().remove(index); }
    throw std::runtime_error("tag not hold an object");
}

bool CompoundTagVariant::remove(size_t index) {
    if (is_array()) { as<ListTag>().remove(index); }
    throw std::runtime_error("tag not hold an array");
}

bool CompoundTagVariant::rename(std::string_view index, std::string_view newName) {
    if (is_object()) { as<CompoundTag>().rename(index, newName); }
    throw std::runtime_error("tag not hold an object");
}

void CompoundTagVariant::push_back(CompoundTagVariant val) {
    if (is_null()) { mStorage = ListTag{}; }
    if (!hold(Tag::Type::List)) { throw std::runtime_error("tag not hold an array"); }
    as<ListTag>().push_back(std::move(val));
}

void CompoundTagVariant::push_back(Tag const& val) {
    if (is_null()) { mStorage = ListTag{}; }
    if (!hold(Tag::Type::List)) { throw std::runtime_error("tag not hold an array"); }
    as<ListTag>().push_back(val);
}

void CompoundTagVariant::push_back(std::unique_ptr<Tag>&& val) {
    if (is_null()) { mStorage = ListTag{}; }
    if (!hold(Tag::Type::List)) { throw std::runtime_error("tag not hold an array"); }
    as<ListTag>().push_back(std::move(val));
}

CompoundTagVariant::operator std::byte() const {
    if (hold(Tag::Type::Byte)) { return as<ByteTag>(); }
    throw std::runtime_error("tag can not convert to a byte");
}

CompoundTagVariant::operator std::string const&() const {
    if (!is_string()) { throw std::runtime_error("tag can not convert to a string"); }
    return as<StringTag>().storage();
}
CompoundTagVariant::operator std::string&() {
    if (!is_string()) { throw std::runtime_error("tag can not convert to a string"); }
    return as<StringTag>().storage();
}

CompoundTagVariant::operator std::vector<uint8_t> const&() const {
    if (!hold(Tag::Type::ByteArray)) { throw std::runtime_error("tag can not convert to a byte array"); }
    return as<ByteArrayTag>().storage();
}
CompoundTagVariant::operator std::vector<uint8_t>&() {
    if (!hold(Tag::Type::ByteArray)) { throw std::runtime_error("tag can not convert to a byte array"); }
    return as<ByteArrayTag>().storage();
}

CompoundTagVariant::operator std::vector<int> const&() const {
    if (!hold(Tag::Type::IntArray)) { throw std::runtime_error("tag can not convert to a int array"); }
    return as<IntArrayTag>().storage();
}
CompoundTagVariant::operator std::vector<int>&() {
    if (!hold(Tag::Type::IntArray)) { throw std::runtime_error("tag can not convert to a int array"); }
    return as<IntArrayTag>().storage();
}

CompoundTagVariant::operator std::vector<int64_t> const&() const {
    if (!hold(Tag::Type::LongArray)) { throw std::runtime_error("tag can not convert to a long array"); }
    return as<LongArrayTag>().storage();
}
CompoundTagVariant::operator std::vector<int64_t>&() {
    if (!hold(Tag::Type::LongArray)) { throw std::runtime_error("tag can not convert to a long array"); }
    return as<LongArrayTag>().storage();
}

Tag& CompoundTagVariant::emplace(Tag&& tag) {
    switch (tag.getType()) {
    case Tag::Type::Byte:
        return emplace<ByteTag>(static_cast<ByteTag&&>(tag));
    case Tag::Type::Short:
        return emplace<ShortTag>(static_cast<ShortTag&&>(tag));
    case Tag::Type::Int:
        return emplace<IntTag>(static_cast<IntTag&&>(tag));
    case Tag::Type::Long:
        return emplace<LongTag>(static_cast<LongTag&&>(tag));
    case Tag::Type::Float:
        return emplace<FloatTag>(static_cast<FloatTag&&>(tag));
    case Tag::Type::Double:
        return emplace<DoubleTag>(static_cast<DoubleTag&&>(tag));
    case Tag::Type::ByteArray:
        return emplace<ByteArrayTag>(static_cast<ByteArrayTag&&>(tag));
    case Tag::Type::String:
        return emplace<StringTag>(static_cast<StringTag&&>(tag));
    case Tag::Type::List:
        return emplace<ListTag>(static_cast<ListTag&&>(tag));
    case Tag::Type::Compound:
        return emplace<CompoundTag>(static_cast<CompoundTag&&>(tag));
    case Tag::Type::IntArray:
        return emplace<IntArrayTag>(static_cast<IntArrayTag&&>(tag));
    case Tag::Type::LongArray:
        return emplace<LongArrayTag>(static_cast<LongArrayTag&&>(tag));
    default:
        return emplace<EndTag>();
    }
}

const Tag* CompoundTagVariant::operator->() const { return get(); }

Tag* CompoundTagVariant::operator->() { return get(); }

const Tag* CompoundTagVariant::get() const {
    return std::visit([](auto&& arg) -> const Tag* { return &arg; }, mStorage);
}

Tag* CompoundTagVariant::get() {
    return std::visit([](auto&& arg) -> Tag* { return &arg; }, mStorage);
}

const Tag& CompoundTagVariant::operator*() const { return *get(); }

Tag& CompoundTagVariant::operator*() { return *get(); }

bool CompoundTagVariant::operator==(CompoundTagVariant const& other) const { return get()->equals(*other.get()); }

std::string
CompoundTagVariant::toSnbt(SnbtFormat snbtFormat, uint8_t indent, SnbtNumberFormat numberFormat) const noexcept {
    return std::visit(
        [&](auto& tag) { return detail::TypedToSnbt(tag, indent, snbtFormat, false, numberFormat); },
        mStorage
    );
}

std::string CompoundTagVariant::toJson(uint8_t indent) const noexcept {
    return std::visit(
        [&](auto& tag) {
            return detail::TypedToSnbt(
                tag,
                indent,
                SnbtFormat::AlwaysLineFeed | SnbtFormat::ForceQuote,
                true,
                SnbtNumberFormat::Default
            );
        },
        mStorage
    );
}

void CompoundTagVariant::merge(CompoundTagVariant const& other, bool mergeList) {
    if (is_object() && other.is_object()) {
        as<CompoundTag>().merge(other.as<CompoundTag>(), mergeList);
    } else if (is_array() && other.is_array() && mergeList) {
        as<ListTag>().merge(other.as<ListTag>());
    } else {
        operator=(other);
    }
}

std::optional<CompoundTagVariant>
CompoundTagVariant::parse(std::string_view snbt, std::optional<size_t> parsedLength) noexcept {
    auto begin{snbt.begin()};
    auto result = detail::parseSnbtValue(snbt, false);
    if (parsedLength) { *parsedLength = static_cast<size_t>(snbt.begin() - begin); }
    return result;
}

std::optional<CompoundTagVariant>
CompoundTagVariant::parseJson(std::string_view snbt, std::optional<size_t> parsedLength) noexcept {
    auto begin{snbt.begin()};
    auto result = detail::parseSnbtValue(snbt, true);
    if (parsedLength) { *parsedLength = static_cast<size_t>(snbt.begin() - begin); }
    return result;
}

} // namespace nbt
