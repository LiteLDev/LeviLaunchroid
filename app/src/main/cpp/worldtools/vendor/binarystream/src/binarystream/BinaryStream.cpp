// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "bstream.hpp"
#include <span>

namespace bstream {

BinaryStream::BinaryStream(bool bigEndian)
: ReadOnlyBinaryStream(std::string(), true, bigEndian),
  mBuffer(mOwnedBuffer) {}

BinaryStream::BinaryStream(std::string& buffer, bool copyBuffer, bool bigEndian)
: ReadOnlyBinaryStream(buffer, copyBuffer, bigEndian),
  mBuffer(copyBuffer ? mOwnedBuffer : buffer) {
    mBufferView = mBuffer;
}

template <typename T>
void BinaryStream::write(T value, bool bigEndian) {
    if (bigEndian) { value = detail::swapEndian(value); }
    mBuffer.append(reinterpret_cast<const char*>(&value), sizeof(T));
    mBufferView = mBuffer;
}

void BinaryStream::reserve(size_t size) { mBuffer.reserve(size); }

void BinaryStream::reset() noexcept {
    mBuffer.clear();
    mReadPointer   = 0;
    mHasOverflowed = false;
    mBufferView    = mBuffer;
}

std::string& BinaryStream::data() noexcept { return mBuffer; }

const std::string& BinaryStream::data() const noexcept { return mBuffer; }

std::string BinaryStream::copyBuffer() const { return mBuffer; }

std::string BinaryStream::getAndReleaseData() {
    std::string result = std::move(mBuffer);
    reset();
    return result;
}

void BinaryStream::writeBytes(const void* origin, size_t num) {
    if (num > 0) {
        mBuffer.append(reinterpret_cast<const char*>(origin), num);
        mBufferView = mBuffer;
    }
}

void BinaryStream::writeByte(std::byte value) { write(value, mBigEndian); }

void BinaryStream::writeUnsignedChar(uint8_t value) { write(value, mBigEndian); }

void BinaryStream::writeUnsignedShort(uint16_t value) { write(value, mBigEndian); }

void BinaryStream::writeUnsignedInt(uint32_t value) { write(value, mBigEndian); }

void BinaryStream::writeUnsignedInt64(uint64_t value) { write(value, mBigEndian); }

void BinaryStream::writeBool(bool value) { write(value, mBigEndian); }

void BinaryStream::writeDouble(double value) { write(value, mBigEndian); }

void BinaryStream::writeFloat(float value) { write(value, mBigEndian); }

void BinaryStream::writeSignedInt(int32_t value) { write(value, mBigEndian); }

void BinaryStream::writeSignedInt64(int64_t value) { write(value, mBigEndian); }

void BinaryStream::writeSignedShort(int16_t value) { write(value, mBigEndian); }

void BinaryStream::writeUnsignedVarInt(uint32_t uvalue) {
    if (mBigEndian) { uvalue = detail::swapEndian(uvalue); }
    do {
        uint8_t next_byte   = uvalue & 0x7F;
        uvalue            >>= 7;
        if (uvalue != 0) { next_byte |= 0x80; }
        writeUnsignedChar(next_byte);
    } while (uvalue != 0);
}

void BinaryStream::writeUnsignedVarInt64(uint64_t uvalue) {
    if (mBigEndian) { uvalue = detail::swapEndian(uvalue); }
    do {
        uint8_t next_byte   = uvalue & 0x7F;
        uvalue            >>= 7;
        if (uvalue != 0) { next_byte |= 0x80; }
        writeUnsignedChar(next_byte);
    } while (uvalue != 0);
}

void BinaryStream::writeVarInt(int32_t value) {
    if (value >= 0) {
        writeUnsignedVarInt(static_cast<uint32_t>(value) << 1);
    } else {
        writeUnsignedVarInt((static_cast<uint32_t>(~value) << 1) | 1);
    }
}

void BinaryStream::writeVarInt64(int64_t value) {
    if (value >= 0) {
        writeUnsignedVarInt64(static_cast<uint64_t>(value) << 1);
    } else {
        writeUnsignedVarInt64((static_cast<uint64_t>(~value) << 1) | 1);
    }
}

void BinaryStream::writeNormalizedFloat(float value) {
    auto scaled = static_cast<int64_t>(value * 2147483647.0f);
    writeVarInt64(scaled);
}

void BinaryStream::writeSignedBigEndianInt(int32_t value) { write(value, true); }

void BinaryStream::writeString(std::string_view value) {
    auto size = static_cast<uint32_t>(value.size());
    writeUnsignedVarInt(size);
    writeRawBytes(value, static_cast<size_t>(size));
}

void BinaryStream::writeShortString(std::string_view value) {
    auto size = static_cast<int16_t>(value.size());
    writeSignedShort(size);
    writeRawBytes(value, static_cast<size_t>(size));
}

void BinaryStream::writeLongString(std::string_view value) {
    auto size = static_cast<int>(value.size());
    writeSignedInt(size);
    writeRawBytes(value, static_cast<size_t>(size));
}

void BinaryStream::writeUnsignedInt24(uint32_t value) {
    if (mBigEndian) {
        writeUnsignedChar(static_cast<uint8_t>((value >> 16) & 0xFF));
        writeUnsignedChar(static_cast<uint8_t>((value >> 8) & 0xFF));
        writeUnsignedChar(static_cast<uint8_t>(value & 0xFF));
    } else {
        writeUnsignedChar(static_cast<uint8_t>(value & 0xFF));
        writeUnsignedChar(static_cast<uint8_t>((value >> 8) & 0xFF));
        writeUnsignedChar(static_cast<uint8_t>((value >> 16) & 0xFF));
    }
}

void BinaryStream::writeRawBytes(std::string_view rawBuffer) {
    if (!rawBuffer.empty()) {
        mBuffer.append(rawBuffer);
        mBufferView = mBuffer;
    }
}

void BinaryStream::writeRawBytes(std::string_view rawBuffer, size_t size) {
    if (!rawBuffer.empty()) {
        mBuffer.append(rawBuffer.data(), size);
        mBufferView = mBuffer;
    }
}

void BinaryStream::writeStream(ReadOnlyBinaryStream const& stream) { writeRawBytes(stream.view()); }

} // namespace bstream