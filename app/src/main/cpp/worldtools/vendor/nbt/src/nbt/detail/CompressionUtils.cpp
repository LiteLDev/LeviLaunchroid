// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/detail/CompressionUtils.hpp"
#include <cstdint>
#include <zlib.h>

namespace nbt::detail {

static constexpr size_t ZLIB_STREAM_CHUNK = 65536;

std::string compress(std::string_view input, int level, int windowBits) {
    if (input.empty()) { return std::string(); }

    z_stream strm{};
    strm.zalloc = Z_NULL;
    strm.zfree  = Z_NULL;
    strm.opaque = Z_NULL;

    if (deflateInit2(&strm, level, Z_DEFLATED, windowBits, 8, Z_DEFAULT_STRATEGY) != Z_OK) {
        return std::string(input);
    }

    strm.next_in  = reinterpret_cast<Bytef*>(const_cast<char*>(input.data()));
    strm.avail_in = static_cast<uInt>(input.size());

    std::string output;
    Bytef       out_buffer[ZLIB_STREAM_CHUNK];
    int         ret;

    do {
        strm.next_out  = out_buffer;
        strm.avail_out = ZLIB_STREAM_CHUNK;

        ret = deflate(&strm, Z_FINISH);
        if (ret == Z_STREAM_ERROR) { break; }

        size_t have = ZLIB_STREAM_CHUNK - strm.avail_out;
        output.append(reinterpret_cast<char*>(out_buffer), have);
    } while (strm.avail_out == 0);

    deflateEnd(&strm);
    return (ret == Z_STREAM_END) ? output : std::string(input);
}

std::string decompress(std::string_view input) {
    if (input.size() < 2) { return std::string(input); }

    const uint8_t* data = reinterpret_cast<const uint8_t*>(input.data());
    uint8_t        b0   = data[0];
    uint8_t        b1   = data[1];

    int windowBits = 0;

    if (b0 == 0x1F && b1 == 0x8B) {
        windowBits = 15 + 16; // GZIP
    } else if (b0 == 0x78 && (b1 == 0x01 || b1 == 0x9C || b1 == 0xDA)) {
        windowBits = 15; // ZLIB
    } else {
        return std::string(input);
    }

    z_stream zstr{};
    zstr.zalloc = Z_NULL;
    zstr.zfree  = Z_NULL;
    zstr.opaque = Z_NULL;

    if (inflateInit2(&zstr, windowBits) != Z_OK) { return std::string(input); }

    zstr.next_in  = reinterpret_cast<Bytef*>(const_cast<char*>(input.data()));
    zstr.avail_in = static_cast<uInt>(input.size());

    std::string output;
    Bytef       out_buffer[ZLIB_STREAM_CHUNK];
    int         ret;

    do {
        zstr.next_out  = out_buffer;
        zstr.avail_out = ZLIB_STREAM_CHUNK;

        ret = inflate(&zstr, Z_NO_FLUSH);

        if (ret == Z_DATA_ERROR || ret == Z_MEM_ERROR) { break; }

        size_t have = ZLIB_STREAM_CHUNK - zstr.avail_out;
        output.append(reinterpret_cast<char*>(out_buffer), have);
    } while (zstr.avail_out == 0);

    inflateEnd(&zstr);
    return (ret == Z_STREAM_END) ? output : std::string(input);
}
} // namespace nbt::detail
