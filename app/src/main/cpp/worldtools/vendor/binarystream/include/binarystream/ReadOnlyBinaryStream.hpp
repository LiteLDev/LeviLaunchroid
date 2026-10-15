// Copyright © 2025 GlacieTeam.All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <algorithm>
#include <array>
#include <binarystream-c/Macros.h>
#include <bit>
#include <cstdint>
#include <string>
#include <type_traits>
#include <vector>

namespace bstream {

class BinaryStream;

namespace detail {
template <typename T>
    requires std::is_trivially_copyable_v<T>
[[nodiscard]] constexpr T swapEndian(T u) noexcept {
    if constexpr (sizeof(T) == 1) {
        return u;
    } else {
        auto bytes = std::bit_cast<std::array<std::byte, sizeof(T)>>(u);
        std::reverse(bytes.begin(), bytes.end());
        return std::bit_cast<T>(bytes);
    }
}
} // namespace detail

class ReadOnlyBinaryStream {
    friend class BinaryStream;

protected:
    std::string      mOwnedBuffer;
    std::string_view mBufferView;
    size_t           mReadPointer;
    bool             mHasOverflowed;
    const bool       mBigEndian;

private:
    template <typename T>
    bool read(T* target, bool bigEndian = false) noexcept;

public:
    [[nodiscard]] BSAPI explicit ReadOnlyBinaryStream(
        std::string_view buffer,
        bool             copyBuffer = false,
        bool             bigEndian  = false
    );
    [[nodiscard]] BSAPI explicit ReadOnlyBinaryStream(
        std::vector<uint8_t> const& buffer,
        bool                        copyBuffer = false,
        bool                        bigEndian  = false
    );
    [[nodiscard]] BSAPI explicit ReadOnlyBinaryStream(
        const char* data,
        size_t      size,
        bool        copyBuffer = false,
        bool        bigEndian  = false
    );
    [[nodiscard]] BSAPI explicit ReadOnlyBinaryStream(
        const uint8_t* data,
        size_t         size,
        bool           copyBuffer = false,
        bool           bigEndian  = false
    );

    [[nodiscard]] BSAPI size_t size() const noexcept;
    [[nodiscard]] BSAPI size_t getPosition() const noexcept;

    BSAPI void setPosition(size_t value) noexcept;
    BSAPI void resetPosition() noexcept;
    BSAPI void ignoreBytes(size_t length) noexcept;

    [[nodiscard]] BSAPI std::string getLeftBuffer() const;
    [[nodiscard]] BSAPI bool        isOverflowed() const noexcept;
    [[nodiscard]] BSAPI bool        hasDataLeft() const noexcept;
    [[nodiscard]] BSAPI std::string_view view() const noexcept;
    [[nodiscard]] BSAPI std::string copyData() const;
    [[nodiscard]] BSAPI bool        operator==(ReadOnlyBinaryStream const&) const noexcept;

    BSAPI bool          getBytes(void* target, size_t num) noexcept;
    [[nodiscard]] BSAPI std::byte getByte() noexcept;
    [[nodiscard]] BSAPI uint8_t   getUnsignedChar() noexcept;
    [[nodiscard]] BSAPI uint16_t  getUnsignedShort() noexcept;
    [[nodiscard]] BSAPI uint32_t  getUnsignedInt() noexcept;
    [[nodiscard]] BSAPI uint64_t  getUnsignedInt64() noexcept;
    [[nodiscard]] BSAPI bool      getBool() noexcept;
    [[nodiscard]] BSAPI double    getDouble() noexcept;
    [[nodiscard]] BSAPI float     getFloat() noexcept;
    [[nodiscard]] BSAPI int32_t   getSignedInt() noexcept;
    [[nodiscard]] BSAPI int64_t   getSignedInt64() noexcept;
    [[nodiscard]] BSAPI int16_t   getSignedShort() noexcept;
    [[nodiscard]] BSAPI uint32_t  getUnsignedVarInt() noexcept;
    [[nodiscard]] BSAPI uint64_t  getUnsignedVarInt64() noexcept;
    [[nodiscard]] BSAPI int32_t   getVarInt() noexcept;
    [[nodiscard]] BSAPI int64_t   getVarInt64() noexcept;
    [[nodiscard]] BSAPI float     getNormalizedFloat() noexcept;
    [[nodiscard]] BSAPI int32_t   getSignedBigEndianInt() noexcept;
    [[nodiscard]] BSAPI uint32_t  getUnsignedInt24() noexcept;

    BSAPI void getString(std::string& outString);
    BSAPI void getShortString(std::string& outString);
    BSAPI void getLongString(std::string& outString);

    [[nodiscard]] BSAPI std::string getString();
    [[nodiscard]] BSAPI std::string getShortString();
    [[nodiscard]] BSAPI std::string getLongString();

    [[nodiscard]] BSAPI std::string_view getStringView();
    [[nodiscard]] BSAPI std::string_view getShortStringView();
    [[nodiscard]] BSAPI std::string_view getLongStringView();

    BSAPI void          getRawBytes(std::string& rawBuffer, size_t length);
    [[nodiscard]] BSAPI std::string getRawBytes(size_t length);
};

} // namespace bstream