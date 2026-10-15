// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <nbt/io/BytesDataInput.hpp>

namespace nbt::io {

class BytesDataOutput : public BytesDataInput {
protected:
    std::string& mBuffer;

public:
    [[nodiscard]] NBT_API explicit BytesDataOutput(bool isLittleEndian = true);
    [[nodiscard]] NBT_API explicit BytesDataOutput(
        std::string& buffer,
        bool         copyBuffer     = false,
        bool         isLittleEndian = true
    );

    [[nodiscard]] NBT_API std::string getAndReleaseData();

    NBT_API void writeBytes(const void* origin, size_t num);

    NBT_API void writeString(std::string_view value);

    NBT_API void writeLongString(std::string_view value);

    NBT_API void writeFloat(float value);

    NBT_API void writeDouble(double value);

    NBT_API void writeByte(uint8_t value);

    NBT_API void writeShort(int16_t value);

    NBT_API void writeInt(int value);

    NBT_API void writeInt64(int64_t value);
};

} // namespace nbt