// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/ListTag.hpp"
#include "nbt/types/CompoundTagVariant.hpp"
#include <algorithm>

namespace nbt {

struct ListTag::TagListImpl {
    TagList mStorage;

    TagListImpl() = default;
    TagListImpl(TagList const& tags) : mStorage(tags) {}
    TagListImpl(TagList&& tags) : mStorage(std::move(tags)) {}
};

ListTag::ListTag() : mStorageImpl(std::make_unique<TagListImpl>()) {}

ListTag::ListTag(std::initializer_list<CompoundTagVariant> tags) : mStorageImpl(std::make_unique<TagListImpl>(tags)) {
    if (!mStorageImpl->mStorage.empty()) { mType = mStorageImpl->mStorage.front()->getType(); }
}

ListTag::ListTag(std::vector<nbt::CompoundTagVariant> const& data) : mStorageImpl(std::make_unique<TagListImpl>(data)) {
    if (!mStorageImpl->mStorage.empty()) { mType = mStorageImpl->mStorage.front()->getType(); }
}

ListTag::ListTag(std::vector<CompoundTagVariant>&& tags)
: mStorageImpl(std::make_unique<TagListImpl>(std::move(tags))) {
    if (!mStorageImpl->mStorage.empty()) { mType = mStorageImpl->mStorage.front()->getType(); }
}

ListTag::ListTag(std::vector<std::unique_ptr<Tag>> const& tags) {
    for (auto& tag : tags) { mStorageImpl->mStorage.emplace_back(*tag); }
    if (!mStorageImpl->mStorage.empty()) { mType = mStorageImpl->mStorage.front()->getType(); }
}

ListTag::ListTag(std::vector<std::unique_ptr<Tag>>&& tags) {
    for (auto&& tag : tags) { mStorageImpl->mStorage.push_back(std::move(tag)); }
    if (!mStorageImpl->mStorage.empty()) { mType = mStorageImpl->mStorage.front()->getType(); }
}

ListTag::~ListTag() = default;

ListTag::ListTag(ListTag const& other) {
    mType        = other.mType;
    mStorageImpl = std::make_unique<TagListImpl>(other.mStorageImpl->mStorage);
}

ListTag::ListTag(ListTag&& other) = default;

ListTag& ListTag::operator=(ListTag const& other) {
    mType = other.mType;
    mStorageImpl->mStorage.clear();
    mStorageImpl->mStorage = other.mStorageImpl->mStorage;
    return *this;
}

ListTag& ListTag::operator=(ListTag&& other) = default;

bool ListTag::equals(Tag const& other) const {
    return (other.getType() == Type::List)
        && (mStorageImpl->mStorage == static_cast<const ListTag&>(other).mStorageImpl->mStorage);
}

Tag::Type ListTag::getType() const { return Type::List; }

std::unique_ptr<Tag> ListTag::copy() const { return clone(); }

std::size_t ListTag::hash() const {
    std::size_t hash = 0;
    for (auto& value : mStorageImpl->mStorage) {
        std::size_t element_hash  = value.hash();
        hash                     ^= element_hash + 0x9e3779b9 + (hash << 6) + (hash >> 2);
    }
    return hash;
}

std::unique_ptr<ListTag> ListTag::clone() const { return std::make_unique<ListTag>(mStorageImpl->mStorage); }

void ListTag::write(io::BytesDataOutput& stream) const {
    stream.writeByte(static_cast<uint8_t>(mType));
    stream.writeInt(static_cast<int>(mStorageImpl->mStorage.size()));
    for (const auto& data : mStorageImpl->mStorage) { data->write(stream); }
}

void ListTag::load(io::BytesDataInput& stream) {
    mType     = static_cast<Type>(stream.getByte());
    auto size = stream.getInt();
    for (int i = 0; i < size; i++) {
        if (auto tag = Tag::newTag(mType)) {
            tag->load(stream);
            mStorageImpl->mStorage.emplace_back(std::move(tag));
        }
    }
}

void ListTag::write(bstream::BinaryStream& stream) const {
    stream.writeByte(static_cast<std::byte>(mType));
    stream.writeVarInt(static_cast<int>(mStorageImpl->mStorage.size()));
    for (const auto& data : mStorageImpl->mStorage) { data->write(stream); }
}

void ListTag::load(bstream::ReadOnlyBinaryStream& stream) {
    mType     = static_cast<Type>(stream.getByte());
    auto size = stream.getVarInt();
    for (int i = 0; i < size; i++) {
        if (auto tag = Tag::newTag(mType)) {
            tag->load(stream);
            mStorageImpl->mStorage.emplace_back(std::move(tag));
        }
    }
}

void ListTag::merge(ListTag const& other) {
    if (other.empty()) { return; }
    if (mType == other.mType) {
        for (auto const& val : other.mStorageImpl->mStorage) {
            bool isEqual = false;
            for (const auto& tag : mStorageImpl->mStorage) {
                if (tag->equals(*val)) {
                    isEqual = true;
                    break;
                }
            }
            if (!isEqual) { mStorageImpl->mStorage.emplace_back(val); }
        }
    } else {
        mType = other.mType;
        mStorageImpl->mStorage.clear();
        for (const auto& data : other.mStorageImpl->mStorage) { mStorageImpl->mStorage.emplace_back(data); }
    }
}

void ListTag::push_back(std::unique_ptr<Tag>&& tag) {
    if (mStorageImpl->mStorage.empty()) { mType = tag->getType(); }
    mStorageImpl->mStorage.emplace_back(std::move(tag));
}

void ListTag::push_back(Tag const& tag) {
    if (mStorageImpl->mStorage.empty()) { mType = tag.getType(); }
    mStorageImpl->mStorage.emplace_back(tag.copy());
}

void ListTag::push_back(CompoundTagVariant val) {
    if (mStorageImpl->mStorage.empty()) { mType = val.getType(); }
    mStorageImpl->mStorage.push_back(std::move(val));
}

bool ListTag::add(std::unique_ptr<Tag>&& tag) {
    if (mStorageImpl->mStorage.empty()) {
        mType = tag->getType();
    } else if (mType != tag->getType()) {
        return false;
    }
    mStorageImpl->mStorage.emplace_back(std::move(tag));
    return true;
}

bool ListTag::add(Tag const& tag) {
    if (mStorageImpl->mStorage.empty()) {
        mType = tag.getType();
    } else if (mType != tag.getType()) {
        return false;
    }
    mStorageImpl->mStorage.emplace_back(tag.copy());
    return true;
}

bool ListTag::add(CompoundTagVariant val) {
    if (mStorageImpl->mStorage.empty()) {
        mType = val.getType();
    } else if (mType != val.getType()) {
        return false;
    }
    mStorageImpl->mStorage.push_back(std::move(val));
    return true;
}

bool ListTag::checkElements() {
    for (auto& tag : mStorageImpl->mStorage) {
        if (!tag.hold(mType)) { return false; }
    }
    return true;
}

bool ListTag::checkAndFixElements() {
    if (!checkElements()) {
        mType = Type::Compound;
        for (auto& tag : mStorageImpl->mStorage) {
            if (!tag.hold(Type::Compound)) {
                tag = CompoundTag({
                    {"", tag}
                });
            }
        }
        return true;
    }
    return false;
}

void ListTag::reserve(size_t size) { mStorageImpl->mStorage.reserve(size); }

bool ListTag::remove(size_t index) {
    if (index < mStorageImpl->mStorage.size()) {
        mStorageImpl->mStorage.erase(
            mStorageImpl->mStorage.begin() + static_cast<decltype(mStorageImpl->mStorage)::difference_type>(index)
        );
        return true;
    }
    return false;
}

bool ListTag::remove(size_t startIndex, size_t endIndex) {
    if (startIndex < endIndex && endIndex < mStorageImpl->mStorage.size()) {
        mStorageImpl->mStorage.erase(
            mStorageImpl->mStorage.begin() + static_cast<decltype(mStorageImpl->mStorage)::difference_type>(startIndex),
            mStorageImpl->mStorage.begin() + static_cast<decltype(mStorageImpl->mStorage)::difference_type>(endIndex)
        );
        return true;
    }
    return false;
}

void ListTag::clear() noexcept { mStorageImpl->mStorage.clear(); }

ListTag::TagList&       ListTag::storage() noexcept { return mStorageImpl->mStorage; }
ListTag::TagList const& ListTag::storage() const noexcept { return mStorageImpl->mStorage; }

size_t ListTag::size() const noexcept { return mStorageImpl->mStorage.size(); }
bool   ListTag::empty() const { return mStorageImpl->mStorage.empty(); }

Tag::Type ListTag::getElementType() const { return mType; }

CompoundTagVariant&       ListTag::operator[](size_t index) noexcept { return mStorageImpl->mStorage[index]; }
CompoundTagVariant const& ListTag::operator[](size_t index) const noexcept { return mStorageImpl->mStorage[index]; }

CompoundTagVariant&       ListTag::at(size_t index) { return mStorageImpl->mStorage.at(index); }
CompoundTagVariant const& ListTag::at(size_t index) const { return mStorageImpl->mStorage.at(index); }

ListTag::iterator ListTag::begin() noexcept { return mStorageImpl->mStorage.begin(); }
ListTag::iterator ListTag::end() noexcept { return mStorageImpl->mStorage.end(); }

ListTag::reverse_iterator ListTag::rbegin() noexcept { return mStorageImpl->mStorage.rbegin(); }
ListTag::reverse_iterator ListTag::rend() noexcept { return mStorageImpl->mStorage.rend(); }

ListTag::const_iterator ListTag::begin() const noexcept { return cbegin(); }
ListTag::const_iterator ListTag::end() const noexcept { return cend(); }

ListTag::const_iterator ListTag::cbegin() const noexcept { return mStorageImpl->mStorage.cbegin(); }
ListTag::const_iterator ListTag::cend() const noexcept { return mStorageImpl->mStorage.cend(); }

ListTag::const_reverse_iterator ListTag::crbegin() const noexcept { return mStorageImpl->mStorage.crbegin(); }
ListTag::const_reverse_iterator ListTag::crend() const noexcept { return mStorageImpl->mStorage.crend(); }

ListTag::iterator ListTag::erase(const_iterator where) noexcept { return mStorageImpl->mStorage.erase(where); }
ListTag::iterator ListTag::erase(const_iterator first, const_iterator last) noexcept {
    return mStorageImpl->mStorage.erase(first, last);
}

bool ListTag::set(size_t index, Tag const& tag) {
    if (index < mStorageImpl->mStorage.size()) {
        mStorageImpl->mStorage[index] = tag;
        return true;
    }
    return false;
}

bool ListTag::set(size_t index, std::unique_ptr<Tag>&& tag) {
    if (index < mStorageImpl->mStorage.size()) {
        mStorageImpl->mStorage[index] = std::move(tag);
        return true;
    }
    return false;
}

bool ListTag::set(size_t index, CompoundTagVariant tag) {
    if (index < mStorageImpl->mStorage.size()) {
        mStorageImpl->mStorage[index] = std::move(tag);
        return true;
    }
    return false;
}

} // namespace nbt