// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/io/BytesDataOutput.hpp"

namespace nbt::io {

BytesDataOutput::BytesDataOutput(bool isLittleEndian)
: BytesDataInput({}, true, isLittleEndian),
  mBuffer(mOwnedBuffer) {}

BytesDataOutput::BytesDataOutput(std::string& buffer, bool copyBuffer, bool isLittleEndian)
: BytesDataInput(buffer, copyBuffer, isLittleEndian),
  mBuffer(buffer) {}

std::string BytesDataOutput::getAndReleaseData() { return std::move(mBuffer); }

void BytesDataOutput::writeBytes(const void* origin, size_t num) {
    mBuffer.append(reinterpret_cast<const char*>(origin), num);
    mBufferView = mBuffer;
}

void BytesDataOutput::writeString(std::string_view value) {
    writeShort(static_cast<int16_t>(value.size()));
    writeBytes(value.data(), value.size());
}

void BytesDataOutput::writeLongString(std::string_view value) {
    writeInt(static_cast<int>(value.size()));
    writeBytes(value.data(), value.size());
}

void BytesDataOutput::writeFloat(float value) {
    if (!mIsLittleEndian) { value = bstream::detail::swapEndian(value); }
    writeBytes(&value, sizeof(float));
}

void BytesDataOutput::writeDouble(double value) {
    if (!mIsLittleEndian) { value = bstream::detail::swapEndian(value); }
    writeBytes(&value, sizeof(double));
}

void BytesDataOutput::writeByte(uint8_t value) { writeBytes(&value, sizeof(uint8_t)); }

void BytesDataOutput::writeShort(int16_t value) {
    if (!mIsLittleEndian) { value = bstream::detail::swapEndian(value); }
    writeBytes(&value, sizeof(int16_t));
}

void BytesDataOutput::writeInt(int value) {
    if (!mIsLittleEndian) { value = bstream::detail::swapEndian(value); }
    writeBytes(&value, sizeof(int));
}

void BytesDataOutput::writeInt64(int64_t value) {
    if (!mIsLittleEndian) { value = bstream::detail::swapEndian(value); }
    writeBytes(&value, sizeof(int64_t));
}

} // namespace nbt::io
