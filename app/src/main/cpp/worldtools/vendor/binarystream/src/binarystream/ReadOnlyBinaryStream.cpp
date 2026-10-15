// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "binarystream/ReadOnlyBinaryStream.hpp"
#include <span>

namespace bstream {

ReadOnlyBinaryStream::ReadOnlyBinaryStream(std::string_view buffer, bool copyBuffer, bool bigEndian)
: mReadPointer(0),
  mHasOverflowed(false),
  mBigEndian(bigEndian) {
    if (copyBuffer) {
        mOwnedBuffer = buffer;
        mBufferView  = mOwnedBuffer;
    } else {
        mBufferView = buffer;
    }
}

ReadOnlyBinaryStream::ReadOnlyBinaryStream(std::vector<uint8_t> const& buffer, bool copyBuffer, bool bigEndian)
: ReadOnlyBinaryStream(buffer.data(), buffer.size(), copyBuffer, bigEndian) {}

ReadOnlyBinaryStream::ReadOnlyBinaryStream(const char* data, size_t size, bool copyBuffer, bool bigEndian)
: ReadOnlyBinaryStream(std::string(data, size), copyBuffer, bigEndian) {}

ReadOnlyBinaryStream::ReadOnlyBinaryStream(const uint8_t* data, size_t size, bool copyBuffer, bool bigEndian)
: ReadOnlyBinaryStream(reinterpret_cast<const char*>(data), size, copyBuffer, bigEndian) {}

template <typename T>
bool ReadOnlyBinaryStream::read(T* target, bool bigEndian) noexcept {
    if (mHasOverflowed) { return false; }
    size_t newReadPointer = mReadPointer + sizeof(T);

    if (newReadPointer < mReadPointer || newReadPointer > mBufferView.length()) {
        mHasOverflowed = true;
        return false;
    }

    std::copy_n(mBufferView.begin() + mReadPointer, sizeof(T), reinterpret_cast<char*>(target));
    mReadPointer = newReadPointer;
    if (bigEndian) { *target = detail::swapEndian(*target); }
    return true;
}

size_t ReadOnlyBinaryStream::getPosition() const noexcept { return mReadPointer; }

void ReadOnlyBinaryStream::setPosition(size_t value) noexcept { mReadPointer = value; }

void ReadOnlyBinaryStream::resetPosition() noexcept { setPosition(0); }

void ReadOnlyBinaryStream::ignoreBytes(size_t length) noexcept { mReadPointer += length; }

std::string ReadOnlyBinaryStream::getLeftBuffer() const { return std::string(mBufferView.substr(mReadPointer)); }

bool ReadOnlyBinaryStream::isOverflowed() const noexcept { return mHasOverflowed; }

bool ReadOnlyBinaryStream::hasDataLeft() const noexcept { return mReadPointer < mBufferView.size(); }

size_t ReadOnlyBinaryStream::size() const noexcept { return mBufferView.size(); }

std::string_view ReadOnlyBinaryStream::view() const noexcept { return mBufferView; }

std::string ReadOnlyBinaryStream::copyData() const { return std::string(mBufferView); }

bool ReadOnlyBinaryStream::operator==(ReadOnlyBinaryStream const& other) const noexcept {
    return mBufferView == other.mBufferView;
}

bool ReadOnlyBinaryStream::getBytes(void* target, size_t num) noexcept {
    if (mHasOverflowed) { return false; }
    if (num == 0) { return true; }

    size_t newPointer = mReadPointer + num;

    if (newPointer < mReadPointer || newPointer > mBufferView.size()) {
        mHasOverflowed = true;
        return false;
    }

    std::copy_n(mBufferView.begin() + mReadPointer, num, static_cast<char*>(target));
    mReadPointer = newPointer;

    return true;
}

uint8_t ReadOnlyBinaryStream::getUnsignedChar() noexcept {
    uint8_t value = 0;
    read(&value, mBigEndian);
    return value;
}

std::byte ReadOnlyBinaryStream::getByte() noexcept { return std::byte(getUnsignedChar()); }

uint16_t ReadOnlyBinaryStream::getUnsignedShort() noexcept {
    uint16_t value = 0;
    read(&value, mBigEndian);
    return value;
}

uint32_t ReadOnlyBinaryStream::getUnsignedInt() noexcept {
    uint32_t value = 0;
    read(&value, mBigEndian);
    return value;
}

uint64_t ReadOnlyBinaryStream::getUnsignedInt64() noexcept {
    uint64_t value = 0;
    read(&value, mBigEndian);
    return value;
}

bool ReadOnlyBinaryStream::getBool() noexcept { return getUnsignedChar() != 0; }

double ReadOnlyBinaryStream::getDouble() noexcept {
    double value = 0;
    read(&value, mBigEndian);
    return value;
}

float ReadOnlyBinaryStream::getFloat() noexcept {
    float value = 0;
    read(&value, mBigEndian);
    return value;
}

int32_t ReadOnlyBinaryStream::getSignedInt() noexcept {
    int32_t value = 0;
    read(&value, mBigEndian);
    return value;
}

int64_t ReadOnlyBinaryStream::getSignedInt64() noexcept {
    int64_t value = 0;
    read(&value, mBigEndian);
    return value;
}

int16_t ReadOnlyBinaryStream::getSignedShort() noexcept {
    int16_t value = 0;
    read(&value, mBigEndian);
    return value;
}

uint32_t ReadOnlyBinaryStream::getUnsignedVarInt() noexcept {
    uint32_t value = 0;
    unsigned shift = 0;
    uint8_t  byte;

    do {
        if (shift >= 35) {
            mHasOverflowed = true;
            return value;
        }

        if (mReadPointer >= mBufferView.size()) {
            mHasOverflowed = true;
            return value;
        }

        byte   = static_cast<uint8_t>(mBufferView[mReadPointer++]);
        value |= static_cast<uint32_t>(byte & 0x7F) << shift;
        shift += 7;

    } while (byte & 0x80);

    if (mBigEndian) { value = detail::swapEndian(value); }
    return value;
}

uint64_t ReadOnlyBinaryStream::getUnsignedVarInt64() noexcept {
    uint64_t value = 0;
    unsigned shift = 0;
    uint8_t  byte;

    do {
        if (shift >= 70) {
            mHasOverflowed = true;
            return value;
        }

        if (mReadPointer >= mBufferView.size()) {
            mHasOverflowed = true;
            return value;
        }

        byte   = static_cast<uint8_t>(mBufferView[mReadPointer++]);
        value |= static_cast<uint64_t>(byte & 0x7F) << shift;
        shift += 7;

    } while (byte & 0x80);

    if (mBigEndian) { value = detail::swapEndian(value); }
    return value;
}

int32_t ReadOnlyBinaryStream::getVarInt() noexcept {
    uint32_t value = getUnsignedVarInt();
    return (value & 1) ? ~(value >> 1) : (value >> 1);
}

int64_t ReadOnlyBinaryStream::getVarInt64() noexcept {
    uint64_t value = getUnsignedVarInt64();
    return (value & 1) ? ~(value >> 1) : (value >> 1);
}

float ReadOnlyBinaryStream::getNormalizedFloat() noexcept { return static_cast<float>(getVarInt64()) / 2147483647.0f; }

int32_t ReadOnlyBinaryStream::getSignedBigEndianInt() noexcept {
    int32_t value = 0;
    if (read(&value, true)) { return value; }
    return 0;
}

void ReadOnlyBinaryStream::getString(std::string& outString) {
    uint32_t length = getUnsignedVarInt();
    getRawBytes(outString, static_cast<size_t>(length));
}

void ReadOnlyBinaryStream::getShortString(std::string& outString) {
    short length = getSignedShort();
    getRawBytes(outString, static_cast<size_t>(length));
}

void ReadOnlyBinaryStream::getLongString(std::string& outString) {
    int length = getSignedInt();
    getRawBytes(outString, static_cast<size_t>(length));
}

std::string ReadOnlyBinaryStream::getString() {
    std::string result;
    getString(result);
    return result;
}

std::string ReadOnlyBinaryStream::getShortString() {
    std::string result;
    getShortString(result);
    return result;
}

std::string ReadOnlyBinaryStream::getLongString() {
    std::string result;
    getLongString(result);
    return result;
}

std::string_view ReadOnlyBinaryStream::getStringView() {
    auto length   = static_cast<size_t>(getUnsignedVarInt());
    auto result   = mBufferView.substr(mReadPointer, length);
    mReadPointer += length;
    return result;
}

std::string_view ReadOnlyBinaryStream::getShortStringView() {
    auto length   = static_cast<size_t>(getSignedShort());
    auto result   = mBufferView.substr(mReadPointer, length);
    mReadPointer += length;
    return result;
}

std::string_view ReadOnlyBinaryStream::getLongStringView() {
    auto length   = static_cast<size_t>(getSignedInt());
    auto result   = mBufferView.substr(mReadPointer, length);
    mReadPointer += length;
    return result;
}

uint32_t ReadOnlyBinaryStream::getUnsignedInt24() noexcept {
    if (mReadPointer + 3 > mBufferView.size()) {
        mHasOverflowed = true;
        return 0;
    }
    uint32_t value = 0;
    if (mBigEndian) {
        value  = static_cast<uint32_t>(static_cast<uint8_t>(mBufferView[mReadPointer++]) << 16);
        value |= static_cast<uint32_t>(static_cast<uint8_t>(mBufferView[mReadPointer++])) << 8;
        value |= static_cast<uint32_t>(static_cast<uint8_t>(mBufferView[mReadPointer++]));
    } else {
        value  = static_cast<uint8_t>(mBufferView[mReadPointer++]);
        value |= static_cast<uint32_t>(static_cast<uint8_t>(mBufferView[mReadPointer++])) << 8;
        value |= static_cast<uint32_t>(static_cast<uint8_t>(mBufferView[mReadPointer++])) << 16;
    }
    return value;
}

void ReadOnlyBinaryStream::getRawBytes(std::string& rawBuffer, size_t length) {
    if (length == 0) {
        rawBuffer.clear();
        return;
    }

    if (mReadPointer + length > mBufferView.size()) {
        mHasOverflowed = true;
        rawBuffer.clear();
        return;
    }

    rawBuffer.assign(mBufferView.substr(mReadPointer, length));
    mReadPointer += length;
}

std::string ReadOnlyBinaryStream::getRawBytes(size_t length) {
    std::string result;
    getRawBytes(result, length);
    return result;
}

} // namespace bstream