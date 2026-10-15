// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <binarystream/ReadOnlyBinaryStream.hpp>
#include <nbt-c/Macros.h>

namespace nbt::io {

class BytesDataInput {
protected:
    size_t           mReadPointer;
    bool             mHasOverflowed;
    std::string      mOwnedBuffer;
    std::string_view mBufferView;
    const bool       mIsLittleEndian;

public:
    [[nodiscard]] NBT_API explicit BytesDataInput(bool isLittleEndian = true);
    [[nodiscard]] NBT_API explicit BytesDataInput(
        std::string_view buffer,
        bool             copyBuffer     = false,
        bool             isLittleEndian = true
    );

    [[nodiscard]] NBT_API bool hasDataLeft() const noexcept;

    NBT_API void ignoreBytes(size_t length) noexcept;

    NBT_API size_t getPosition() const noexcept;

    NBT_API size_t size() const noexcept;

    NBT_API void getBytes(void* target, size_t num) noexcept;

    NBT_API void getString(std::string& result);

    [[nodiscard]] NBT_API std::string getString();

    NBT_API std::string_view getStringView() noexcept;

    NBT_API void getLongString(std::string& result);

    [[nodiscard]] NBT_API std::string getLongString();

    NBT_API std::string_view getLongStringView() noexcept;

    [[nodiscard]] NBT_API float getFloat() noexcept;

    [[nodiscard]] NBT_API double getDouble() noexcept;

    [[nodiscard]] NBT_API uint8_t getByte() noexcept;

    [[nodiscard]] NBT_API int16_t getShort() noexcept;

    [[nodiscard]] NBT_API int getInt() noexcept;

    [[nodiscard]] NBT_API int64_t getInt64() noexcept;
};

} // namespace nbt