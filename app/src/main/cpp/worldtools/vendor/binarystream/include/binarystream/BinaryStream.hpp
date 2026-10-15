// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <binarystream/ReadOnlyBinaryStream.hpp>

namespace bstream {

class BinaryStream : public ReadOnlyBinaryStream {
protected:
    std::string& mBuffer;

private:
    template <typename T>
    void write(T value, bool bigEndian = false);

public:
    [[nodiscard]] BSAPI explicit BinaryStream(bool bigEndian = false);
    [[nodiscard]] BSAPI explicit BinaryStream(std::string& buffer, bool copyBuffer = false, bool bigEndian = false);

    BSAPI void reserve(size_t size);
    BSAPI void reset() noexcept;

    [[nodiscard]] BSAPI std::string& data() noexcept;
    [[nodiscard]] BSAPI const std::string& data() const noexcept;

    [[nodiscard]] BSAPI std::string copyBuffer() const;
    [[nodiscard]] BSAPI std::string getAndReleaseData();

    BSAPI void writeBytes(const void* origin, size_t num);
    BSAPI void writeByte(std::byte value);
    BSAPI void writeUnsignedChar(uint8_t value);
    BSAPI void writeUnsignedShort(uint16_t value);
    BSAPI void writeUnsignedInt(uint32_t value);
    BSAPI void writeUnsignedInt64(uint64_t value);
    BSAPI void writeBool(bool value);
    BSAPI void writeDouble(double value);
    BSAPI void writeFloat(float value);
    BSAPI void writeSignedInt(int32_t value);
    BSAPI void writeSignedInt64(int64_t value);
    BSAPI void writeSignedShort(int16_t value);
    BSAPI void writeUnsignedVarInt(uint32_t uvalue);
    BSAPI void writeUnsignedVarInt64(uint64_t uvalue);
    BSAPI void writeVarInt(int32_t value);
    BSAPI void writeVarInt64(int64_t value);
    BSAPI void writeNormalizedFloat(float value);
    BSAPI void writeSignedBigEndianInt(int32_t value);
    BSAPI void writeString(std::string_view value);
    BSAPI void writeShortString(std::string_view value);
    BSAPI void writeLongString(std::string_view value);
    BSAPI void writeUnsignedInt24(uint32_t value);
    BSAPI void writeRawBytes(std::string_view rawBuffer);
    BSAPI void writeRawBytes(std::string_view rawBuffer, size_t size);
    BSAPI void writeStream(ReadOnlyBinaryStream const& stream);
};

} // namespace bstream