// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "binarystream-c/bstream.h"
#include "binarystream/BinaryStream.hpp"

namespace {

inline bstream::ReadOnlyBinaryStream* to_robs(void* handle) {
    return reinterpret_cast<bstream::ReadOnlyBinaryStream*>(handle);
}

inline bstream::BinaryStream* to_bs(void* handle) { return reinterpret_cast<bstream::BinaryStream*>(handle); }

inline stream_buffer* make_stream_buffer(std::string_view buffer) {
    auto result  = new stream_buffer();
    result->data = new uint8_t[buffer.size()];
    std::copy_n(buffer.data(), buffer.size(), result->data);
    result->size = buffer.size();
    return result;
}

inline stream_buffer* make_stream_buffer(std::vector<uint8_t> const& buffer) {
    auto result  = new stream_buffer();
    result->data = new uint8_t[buffer.size()];
    std::copy_n(buffer.data(), buffer.size(), result->data);
    result->size = buffer.size();
    return result;
}

} // namespace

extern "C" {

void stream_buffer_destroy(stream_buffer* buffer) {
    if (buffer && buffer->data) {
        delete[] buffer->data;
        buffer->data = nullptr;
        buffer->size = 0;
    }
}

void* read_only_binary_stream_create(const uint8_t* data, size_t size, bool copy_data, bool big_endian) {
    try {
        if (data && size > 0) {
            return new bstream::ReadOnlyBinaryStream(reinterpret_cast<const char*>(data), size, copy_data, big_endian);
        } else {
            return nullptr;
        }
    } catch (...) { return nullptr; }
}

void read_only_binary_stream_destroy(void* stream) {
    if (stream) { delete to_robs(stream); }
}

size_t read_only_binary_stream_size(void* stream) {
    if (!stream) return 0;
    return to_robs(stream)->size();
}

size_t read_only_binary_stream_get_position(void* stream) {
    if (!stream) return 0;
    return to_robs(stream)->getPosition();
}

void read_only_binary_stream_set_position(void* stream, size_t position) {
    if (stream) to_robs(stream)->setPosition(position);
}

void read_only_binary_stream_reset_position(void* stream) {
    if (stream) to_robs(stream)->resetPosition();
}

void read_only_binary_stream_ignore_bytes(void* stream, size_t length) {
    if (stream) to_robs(stream)->ignoreBytes(length);
}

bool read_only_binary_stream_overflowed(void* stream) {
    if (!stream) return true;
    return to_robs(stream)->isOverflowed();
}

bool read_only_binary_stream_has_data_left(void* stream) {
    if (!stream) return false;
    return to_robs(stream)->hasDataLeft();
}

stream_buffer* read_only_binary_stream_get_left_buffer(void* stream) {
    if (stream) { return make_stream_buffer(to_robs(stream)->getLeftBuffer()); }
    return nullptr;
}

size_t read_only_binary_stream_get_bytes(void* stream, uint8_t* buffer, size_t buffer_size) {
    if (!stream || !buffer || buffer_size == 0) return 0;
    return to_robs(stream)->getBytes(buffer, buffer_size) ? buffer_size : 0;
}

bool read_only_binary_stream_get_bool(void* stream) {
    if (!stream) return false;
    return to_robs(stream)->getBool();
}

uint8_t read_only_binary_stream_get_unsigned_char(void* stream) {
    if (!stream) return 0;
    return to_robs(stream)->getUnsignedChar();
}

uint16_t read_only_binary_stream_get_unsigned_short(void* stream) {
    if (!stream) return 0;
    return to_robs(stream)->getUnsignedShort();
}

int16_t read_only_binary_stream_get_signed_short(void* stream) {
    if (!stream) return 0;
    return to_robs(stream)->getSignedShort();
}

uint32_t read_only_binary_stream_get_unsigned_int24(void* stream) {
    if (!stream) return 0;
    return to_robs(stream)->getUnsignedInt24();
}

uint32_t read_only_binary_stream_get_unsigned_int(void* stream) {
    if (!stream) return 0;
    return to_robs(stream)->getUnsignedInt();
}

int32_t read_only_binary_stream_get_signed_int(void* stream) {
    {
        if (!stream) return 0;
        return to_robs(stream)->getSignedInt();
    }
}

uint64_t read_only_binary_stream_get_unsigned_int64(void* stream) {
    if (!stream) return 0;
    return to_robs(stream)->getUnsignedInt64();
}

int64_t read_only_binary_stream_get_signed_int64(void* stream) {
    if (!stream) return 0;
    return to_robs(stream)->getSignedInt64();
}

float read_only_binary_stream_get_float(void* stream) {
    if (!stream) return 0.0f;
    return to_robs(stream)->getFloat();
}

double read_only_binary_stream_get_double(void* stream) {
    if (!stream) return 0.0;
    return to_robs(stream)->getDouble();
}

uint32_t read_only_binary_stream_get_unsigned_varint(void* stream) {
    if (!stream) return 0;
    return to_robs(stream)->getUnsignedVarInt();
}

int32_t read_only_binary_stream_get_varint(void* stream) {
    if (!stream) return 0;
    return to_robs(stream)->getVarInt();
}

uint64_t read_only_binary_stream_get_unsigned_varint64(void* stream) {
    if (!stream) return 0;
    return to_robs(stream)->getUnsignedVarInt();
}

int64_t read_only_binary_stream_get_varint64(void* stream) {
    if (!stream) return 0;
    return to_robs(stream)->getVarInt();
}

int32_t read_only_binary_stream_get_signed_big_endian_int(void* stream) {
    if (!stream) return 0;
    return to_robs(stream)->getSignedBigEndianInt();
}

stream_buffer* read_only_binary_stream_get_string(void* stream) {
    if (stream) { return make_stream_buffer((to_robs(stream)->getString())); }
    return nullptr;
}

size_t read_only_binary_stream_get_raw_bytes(void* stream, uint8_t* buffer, size_t length) {
    if (!stream || !buffer || length == 0) return 0;
    return to_robs(stream)->getBytes(buffer, length) ? length : 0;
}


void* binary_stream_create(bool big_endian) {
    try {
        return new bstream::BinaryStream(big_endian);
    } catch (...) { return nullptr; }
}

void* binary_stream_create_with_buffer(const uint8_t* data, size_t size, bool copy_data, bool big_endian) {
    try {
        if (data) {
            std::string buffer(reinterpret_cast<const char*>(data), size);
            return new bstream::BinaryStream(buffer, copy_data, big_endian);
        } else {
            return binary_stream_create(big_endian);
        }
    } catch (...) { return nullptr; }
}

void binary_stream_destroy(void* stream) {
    if (stream) { delete to_bs(stream); }
}

void binary_stream_write_bytes(void* stream, const uint8_t* data, size_t size) {
    if (stream && data && size > 0) { to_bs(stream)->writeBytes(data, size); }
}

void binary_stream_write_bool(void* stream, bool value) {
    if (stream) to_bs(stream)->writeBool(value);
}

void binary_stream_write_unsigned_char(void* stream, uint8_t value) {
    if (stream) to_bs(stream)->writeUnsignedChar(value);
}

void binary_stream_write_unsigned_short(void* stream, uint16_t value) {
    if (stream) to_bs(stream)->writeUnsignedShort(value);
}

void binary_stream_write_signed_short(void* stream, int16_t value) {
    if (stream) to_bs(stream)->writeSignedShort(value);
}

void binary_stream_write_unsigned_int24(void* stream, uint32_t value) {
    if (stream) to_bs(stream)->writeUnsignedInt24(value);
}

void binary_stream_write_unsigned_int(void* stream, uint32_t value) {
    if (stream) to_bs(stream)->writeUnsignedInt(value);
}

void binary_stream_write_unsigned_int64(void* stream, uint64_t value) {
    if (stream) to_bs(stream)->writeUnsignedInt64(value);
}

void binary_stream_write_signed_int(void* stream, int32_t value) {
    if (stream) to_bs(stream)->writeSignedInt(value);
}

void binary_stream_write_signed_int64(void* stream, int64_t value) {
    if (stream) to_bs(stream)->writeSignedInt64(value);
}

void binary_stream_write_float(void* stream, float value) {
    if (stream) to_bs(stream)->writeFloat(value);
}

void binary_stream_write_double(void* stream, double value) {
    if (stream) to_bs(stream)->writeDouble(value);
}

void binary_stream_write_unsigned_varint(void* stream, uint32_t value) {
    if (stream) to_bs(stream)->writeUnsignedVarInt(value);
}

void binary_stream_write_varint(void* stream, int32_t value) {
    if (stream) to_bs(stream)->writeVarInt(value);
}

void binary_stream_write_unsigned_varint64(void* stream, uint64_t value) {
    if (stream) to_bs(stream)->writeUnsignedVarInt64(value);
}

void binary_stream_write_varint64(void* stream, int64_t value) {
    if (stream) to_bs(stream)->writeVarInt64(value);
}

void binary_stream_write_string(void* stream, const char* str, size_t length) {
    if (stream && str && length > 0) { to_bs(stream)->writeString(std::string_view(str, length)); }
}

void binary_stream_write_signed_big_endian_int(void* stream, int32_t value) {
    if (stream) to_bs(stream)->writeSignedBigEndianInt(value);
}

void binary_stream_reset(void* stream) {
    if (stream) to_bs(stream)->reset();
}

stream_buffer* binary_stream_get_buffer(void* stream) {
    if (stream) { return make_stream_buffer(to_bs(stream)->data()); }
    return nullptr;
}

} // extern "C"
