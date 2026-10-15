// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/detail/Validate.hpp"

namespace nbt::detail {

bool validateListTag(io::BytesDataInput& stream, size_t streamSize) {
    if (stream.getPosition() + sizeof(uint8_t) > streamSize) { return false; }
    auto type = static_cast<Tag::Type>(stream.getByte());
    if (stream.getPosition() + sizeof(int) > streamSize) { return false; }
    auto size = static_cast<size_t>(stream.getInt());
    switch (type) {
    case Tag::Type::End: {
        return true;
    }
    case Tag::Type::Byte: {
        if (stream.getPosition() + (sizeof(uint8_t) * size) > streamSize) { return false; }
        stream.ignoreBytes(sizeof(uint8_t) * size);
        break;
    }
    case Tag::Type::Short: {
        if (stream.getPosition() + (sizeof(short) * size) > streamSize) { return false; }
        stream.ignoreBytes(sizeof(short) * size);
        break;
    }
    case Tag::Type::Int: {
        if (stream.getPosition() + (sizeof(int) * size) > streamSize) { return false; }
        stream.ignoreBytes(sizeof(int) * size);
        break;
    }
    case Tag::Type::Long: {
        if (stream.getPosition() + (sizeof(int64_t) * size) > streamSize) { return false; }
        stream.ignoreBytes(sizeof(int64_t) * size);
        break;
    }
    case Tag::Type::Float: {
        if (stream.getPosition() + (sizeof(float) * size) > streamSize) { return false; }
        stream.ignoreBytes(sizeof(float) * size);
        break;
    }
    case Tag::Type::Double: {
        if (stream.getPosition() + (sizeof(double) * size) > streamSize) { return false; }
        stream.ignoreBytes(sizeof(double) * size);
        break;
    }
    case Tag::Type::ByteArray: {
        for (size_t i = 0; i < size; i++) {
            if (stream.getPosition() + sizeof(int) > streamSize) { return false; }
            auto len = static_cast<size_t>(stream.getInt());
            if (stream.getPosition() + (sizeof(uint8_t) * len) > streamSize) { return false; }
            stream.ignoreBytes((sizeof(uint8_t) * len));
        }
        break;
    }
    case Tag::Type::String: {
        for (size_t i = 0; i < size; i++) {
            if (stream.getPosition() + sizeof(short) > streamSize) { return false; }
            auto strSize = static_cast<size_t>(stream.getShort());
            if (stream.getPosition() + strSize > streamSize) { return false; }
            stream.ignoreBytes(strSize);
        }
        break;
    }
    case Tag::Type::List: {
        for (size_t i = 0; i < size; i++) {
            if (!validateListTag(stream, streamSize)) { return false; }
        }
        break;
    }
    case Tag::Type::Compound: {
        for (size_t i = 0; i < size; i++) {
            if (!validateCompoundTag(stream, streamSize)) { return false; }
        }
        break;
    }
    case Tag::Type::IntArray: {
        for (size_t i = 0; i < size; i++) {
            if (stream.getPosition() + sizeof(int) > streamSize) { return false; }
            auto len = static_cast<size_t>(stream.getInt());
            if (stream.getPosition() + (sizeof(int) * len) > streamSize) { return false; }
            stream.ignoreBytes((sizeof(int) * len));
        }
        break;
    }
    case Tag::Type::LongArray: {
        for (size_t i = 0; i < size; i++) {
            if (stream.getPosition() + sizeof(int) > streamSize) { return false; }
            auto len = static_cast<size_t>(stream.getInt());
            if (stream.getPosition() + (sizeof(int64_t) * len) > streamSize) { return false; }
            stream.ignoreBytes((sizeof(int64_t) * len));
        }
        break;
    }
    default:
        return false;
    }
    return true;
}

bool validateCompoundTag(io::BytesDataInput& stream, size_t streamSize) {
    while (true) {
        if (stream.getPosition() + sizeof(uint8_t) > streamSize) { return false; }
        auto type = static_cast<Tag::Type>(stream.getByte());
        if (type == Tag::Type::End) { return true; }
        if (stream.getPosition() + sizeof(short) > streamSize) { return false; }
        auto strLen = static_cast<size_t>(stream.getShort());
        if (stream.getPosition() + strLen > streamSize) { return false; }
        stream.ignoreBytes(strLen);
        switch (type) {
        case Tag::Type::Byte: {
            if (stream.getPosition() + sizeof(uint8_t) > streamSize) { return false; }
            stream.ignoreBytes(sizeof(uint8_t));
            break;
        }
        case Tag::Type::Short: {
            if (stream.getPosition() + sizeof(short) > streamSize) { return false; }
            stream.ignoreBytes(sizeof(short));
            break;
        }
        case Tag::Type::Int: {
            if (stream.getPosition() + sizeof(int) > streamSize) { return false; }
            stream.ignoreBytes(sizeof(int));
            break;
        }
        case Tag::Type::Long: {
            if (stream.getPosition() + sizeof(int64_t) > streamSize) { return false; }
            stream.ignoreBytes(sizeof(int64_t));
            break;
        }
        case Tag::Type::Float: {
            if (stream.getPosition() + sizeof(float) > streamSize) { return false; }
            stream.ignoreBytes(sizeof(float));
            break;
        }
        case Tag::Type::Double: {
            if (stream.getPosition() + sizeof(double) > streamSize) { return false; }
            stream.ignoreBytes(sizeof(double));
            break;
        }
        case Tag::Type::ByteArray: {
            if (stream.getPosition() + sizeof(int) > streamSize) { return false; }
            auto size = static_cast<size_t>(stream.getInt());
            if (stream.getPosition() + (sizeof(uint8_t) * size) > streamSize) { return false; }
            stream.ignoreBytes((sizeof(uint8_t) * size));
            break;
        }
        case Tag::Type::String: {
            if (stream.getPosition() + sizeof(short) > streamSize) { return false; }
            auto strSize = static_cast<size_t>(stream.getShort());
            if (stream.getPosition() + strSize > streamSize) { return false; }
            stream.ignoreBytes(strSize);
            break;
        }
        case Tag::Type::List: {
            if (!validateListTag(stream, streamSize)) { return false; }
            break;
        }
        case Tag::Type::Compound: {
            if (!validateCompoundTag(stream, streamSize)) { return false; }
            break;
        }
        case Tag::Type::IntArray: {
            if (stream.getPosition() + sizeof(int) > streamSize) { return false; }
            auto size = static_cast<size_t>(stream.getInt());
            if (stream.getPosition() + (sizeof(int) * size) > streamSize) { return false; }
            stream.ignoreBytes((sizeof(int) * size));
            break;
        }
        case Tag::Type::LongArray: {
            if (stream.getPosition() + sizeof(int) > streamSize) { return false; }
            auto size = static_cast<size_t>(stream.getInt());
            if (stream.getPosition() + (sizeof(int64_t) * size) > streamSize) { return false; }
            stream.ignoreBytes((sizeof(int64_t) * size));
            break;
        }
        default:
            return false;
        }
    }
    return true;
}

bool validateListTag(bstream::ReadOnlyBinaryStream& stream, size_t streamSize) {
    if (stream.getPosition() + sizeof(uint8_t) > streamSize) { return false; }
    auto type = static_cast<Tag::Type>(stream.getByte());
    auto size = static_cast<size_t>(stream.getVarInt());
    if (stream.isOverflowed()) { return false; }
    switch (type) {
    case Tag::Type::End: {
        return true;
    }
    case Tag::Type::Byte: {
        if (stream.getPosition() + (sizeof(uint8_t) * size) > streamSize) { return false; }
        stream.ignoreBytes(sizeof(uint8_t) * size);
        break;
    }
    case Tag::Type::Short: {
        if (stream.getPosition() + (sizeof(short) * size) > streamSize) { return false; }
        stream.ignoreBytes(sizeof(short) * size);
        break;
    }
    case Tag::Type::Int: {
        (void)stream.getVarInt();
        if (stream.isOverflowed()) { return false; }
        break;
    }
    case Tag::Type::Long: {
        (void)stream.getVarInt64();
        if (stream.isOverflowed()) { return false; }
        break;
    }
    case Tag::Type::Float: {
        if (stream.getPosition() + (sizeof(float) * size) > streamSize) { return false; }
        stream.ignoreBytes(sizeof(float) * size);
        break;
    }
    case Tag::Type::Double: {
        if (stream.getPosition() + (sizeof(double) * size) > streamSize) { return false; }
        stream.ignoreBytes(sizeof(double) * size);
        break;
    }
    case Tag::Type::ByteArray: {
        for (size_t i = 0; i < size; i++) {
            auto len = static_cast<size_t>(stream.getVarInt());
            if (stream.isOverflowed() || stream.getPosition() + (sizeof(uint8_t) * len) > streamSize) { return false; }
            stream.ignoreBytes((sizeof(uint8_t) * len));
        }
        break;
    }
    case Tag::Type::String: {
        for (size_t i = 0; i < size; i++) {
            auto strSize = static_cast<size_t>(stream.getUnsignedVarInt());
            if (stream.isOverflowed() || stream.getPosition() + strSize > streamSize) { return false; }
            stream.ignoreBytes(strSize);
        }
        break;
    }
    case Tag::Type::List: {
        for (size_t i = 0; i < size; i++) {
            if (!validateListTag(stream, streamSize)) { return false; }
        }
        break;
    }
    case Tag::Type::Compound: {
        for (size_t i = 0; i < size; i++) {
            if (!validateCompoundTag(stream, streamSize)) { return false; }
        }
        break;
    }
    case Tag::Type::IntArray: {
        for (size_t i = 0; i < size; i++) {
            auto len = static_cast<size_t>(stream.getVarInt());
            if (stream.isOverflowed() || stream.getPosition() + (sizeof(int) * len) > streamSize) { return false; }
            for (size_t j = 0; j < len; j++) {
                (void)stream.getVarInt();
                if (stream.isOverflowed()) { return false; }
            }
        }
        break;
    }
    case Tag::Type::LongArray: {
        for (size_t i = 0; i < size; i++) {
            auto len = static_cast<size_t>(stream.getVarInt());
            if (stream.isOverflowed() || stream.getPosition() + (sizeof(int64_t) * len) > streamSize) { return false; }
            for (size_t j = 0; j < len; j++) {
                (void)stream.getVarInt64();
                if (stream.isOverflowed()) { return false; }
            }
        }
        break;
    }
    default:
        return false;
    }
    return true;
}

bool validateCompoundTag(bstream::ReadOnlyBinaryStream& stream, size_t streamSize) {
    while (true) {
        if (stream.getPosition() + sizeof(uint8_t) > streamSize) { return false; }
        auto type = static_cast<Tag::Type>(stream.getByte());
        if (type == Tag::Type::End) { return true; }
        auto strLen = stream.getUnsignedVarInt();
        if (stream.isOverflowed() || stream.getPosition() + strLen > streamSize) { return false; }
        stream.ignoreBytes(strLen);
        switch (type) {
        case Tag::Type::Byte: {
            if (stream.getPosition() + sizeof(uint8_t) > streamSize) { return false; }
            stream.ignoreBytes(sizeof(uint8_t));
            break;
        }
        case Tag::Type::Short: {
            if (stream.getPosition() + sizeof(short) > streamSize) { return false; }
            stream.ignoreBytes(sizeof(short));
            break;
        }
        case Tag::Type::Int: {
            (void)stream.getUnsignedVarInt();
            if (stream.isOverflowed()) { return false; }
            break;
        }
        case Tag::Type::Long: {
            (void)stream.getUnsignedVarInt64();
            if (stream.isOverflowed()) { return false; }
            break;
        }
        case Tag::Type::Float: {
            if (stream.getPosition() + sizeof(float) > streamSize) { return false; }
            stream.ignoreBytes(sizeof(float));
            break;
        }
        case Tag::Type::Double: {
            if (stream.getPosition() + sizeof(double) > streamSize) { return false; }
            stream.ignoreBytes(sizeof(double));
            break;
        }
        case Tag::Type::ByteArray: {
            auto size = static_cast<size_t>(stream.getVarInt());
            if (stream.isOverflowed() || stream.getPosition() + (sizeof(uint8_t) * size) > streamSize) { return false; }
            stream.ignoreBytes((sizeof(uint8_t) * size));
            break;
        }
        case Tag::Type::String: {
            auto strSize = static_cast<size_t>(stream.getUnsignedVarInt());
            if (stream.isOverflowed() || stream.getPosition() + strSize > streamSize) { return false; }
            stream.ignoreBytes(strSize);
            break;
        }
        case Tag::Type::List: {
            if (!validateListTag(stream, streamSize)) { return false; }
            break;
        }
        case Tag::Type::Compound: {
            if (!validateCompoundTag(stream, streamSize)) { return false; }
            break;
        }
        case Tag::Type::IntArray: {
            auto size = static_cast<size_t>(stream.getVarInt());
            if (stream.isOverflowed() || stream.getPosition() + (sizeof(int) * size) > streamSize) { return false; }
            for (size_t i = 0; i < size; i++) {
                (void)stream.getVarInt();
                if (stream.isOverflowed()) { return false; }
            }
            break;
        }
        case Tag::Type::LongArray: {
            auto size = static_cast<size_t>(stream.getVarInt());
            if (stream.isOverflowed() || stream.getPosition() + (sizeof(int64_t) * size) > streamSize) { return false; }
            for (size_t i = 0; i < size; i++) {
                (void)stream.getVarInt64();
                if (stream.isOverflowed()) { return false; }
            }
            break;
        }
        default:
            return false;
        }
    }
    return true;
}

} // namespace nbt::detail
