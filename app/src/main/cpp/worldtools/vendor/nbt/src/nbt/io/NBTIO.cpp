// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/io/NBTIO.hpp"
#include "nbt/detail/Base64.hpp"
#include "nbt/detail/CompressionUtils.hpp"
#include "nbt/detail/FileUtils.hpp"
#include "nbt/detail/Validate.hpp"
#include <fstream>

namespace nbt::io {

constexpr std::byte operator""_byte(unsigned long long value) noexcept { return static_cast<std::byte>(value); }

std::optional<NbtFileFormat> detectContentFormat(std::string_view content, bool strictMatchSize) {
    if (validateContent(content, NbtFileFormat::LittleEndianWithHeader, strictMatchSize)) {
        return NbtFileFormat::LittleEndianWithHeader;
    } else if (validateContent(content, NbtFileFormat::LittleEndian, strictMatchSize)) {
        return NbtFileFormat::LittleEndian;
    } else if (validateContent(content, NbtFileFormat::BigEndianWithHeader, strictMatchSize)) {
        return NbtFileFormat::BigEndianWithHeader;
    } else if (validateContent(content, NbtFileFormat::BigEndian, strictMatchSize)) {
        return NbtFileFormat::BigEndian;
    } else if (validateContent(content, NbtFileFormat::BedrockNetwork, strictMatchSize)) {
        return NbtFileFormat::BedrockNetwork;
    } else {
        return std::nullopt;
    }
}

std::optional<NbtFileFormat>
detectFileFormat(std::filesystem::path const& path, bool fileMemoryMap, bool strictMatchSize) {
    if (std::filesystem::exists(path)) {
        std::string content;
        detail::readFile(path, content, fileMemoryMap);
        content.assign(detail::decompress(content));
        return detectContentFormat(content, strictMatchSize);
    }
    return std::nullopt;
}

NbtCompressionType detectFileCompressionType(std::filesystem::path const& path, bool fileMemoryMap) {
    if (std::filesystem::exists(path)) {
        std::string content;
        detail::readFile(path, content, fileMemoryMap);
        if (content.size() > 2) {
            const auto& b0 = static_cast<std::byte>(static_cast<uint8_t const&>(content[0]));
            const auto& b1 = static_cast<std::byte>(static_cast<uint8_t const&>(content[1]));
            if (b0 == 0x1F_byte && b1 == 0x8B_byte) {
                return NbtCompressionType::Gzip;
            } else if (b0 == 0x78_byte && (b1 == 0x01_byte || b1 == 0x9C_byte || b1 == 0xDA_byte)) {
                return NbtCompressionType::Zlib;
            }
        }
    }
    return NbtCompressionType::None;
}

NbtCompressionType detectContentCompressionType(std::string_view content) {
    if (content.size() > 2) {
        const auto& b0 = static_cast<std::byte>(static_cast<uint8_t const&>(content[0]));
        const auto& b1 = static_cast<std::byte>(static_cast<uint8_t const&>(content[1]));
        if (b0 == 0x1F_byte && b1 == 0x8B_byte) {
            return NbtCompressionType::Gzip;
        } else if (b0 == 0x78_byte && (b1 == 0x01_byte || b1 == 0x9C_byte || b1 == 0xDA_byte)) {
            return NbtCompressionType::Zlib;
        }
    }
    return NbtCompressionType::None;
}

std::optional<CompoundTag>
_parseFromBinary(std::string& content, std::optional<NbtFileFormat> format, bool strictMatchSize) {
    content.assign(detail::decompress(content));
    if (!format.has_value()) { format = detectContentFormat(content, strictMatchSize); }
    if (!format.has_value()) { return std::nullopt; }
    switch (*format) {
    case NbtFileFormat::LittleEndian: {
        return CompoundTag::fromBinaryNbt(content, true);
    }
    case NbtFileFormat::LittleEndianWithHeader: {
        return CompoundTag::fromBinaryNbtWithHeader(content, true);
    }
    case NbtFileFormat::BigEndian: {
        return CompoundTag::fromBinaryNbt(content, false);
    }
    case NbtFileFormat::BigEndianWithHeader: {
        return CompoundTag::fromBinaryNbtWithHeader(content, false);
    }
    case NbtFileFormat::BedrockNetwork: {
        return CompoundTag::fromNetworkNbt(content);
    }
    default:
        return std::nullopt;
    }
}

std::optional<CompoundTag>
parseFromContent(std::string_view content, std::optional<NbtFileFormat> format, bool strictMatchSize) {
    std::string input(content);
    return _parseFromBinary(input, format, strictMatchSize);
}

std::optional<CompoundTag> parseFromFile(
    std::filesystem::path const& path,
    std::optional<NbtFileFormat> format,
    bool                         fileMemoryMap,
    bool                         strictMatchSize
) {
    if (std::filesystem::exists(path)) {
        std::string content;
        detail::readFile(path, content, fileMemoryMap);
        return _parseFromBinary(content, format, strictMatchSize);
    }
    return std::nullopt;
}

std::string saveAsBinary(
    CompoundTag const&  nbt,
    NbtFileFormat       format,
    NbtCompressionType  compressionType,
    NbtCompressionLevel compressionLevel,
    std::optional<int>  headerVersion
) {
    std::string content;
    switch (format) {
    case NbtFileFormat::LittleEndian: {
        content = nbt.toBinaryNbt(true);
        break;
    }
    case NbtFileFormat::LittleEndianWithHeader: {
        content = nbt.toBinaryNbtWithHeader(true, headerVersion);
        break;
    }
    case NbtFileFormat::BigEndian: {
        content = nbt.toBinaryNbt(false);
        break;
    }
    case NbtFileFormat::BigEndianWithHeader: {
        content = nbt.toBinaryNbtWithHeader(false, headerVersion);
        break;
    }
    case NbtFileFormat::BedrockNetwork: {
        content = nbt.toNetworkNbt();
        break;
    }
    default: {
        break;
    }
    }
    switch (compressionType) {
    case NbtCompressionType::Zlib: {
        return detail::compress(content, static_cast<int>(compressionLevel), 15);
    }
    case NbtCompressionType::Gzip: {
        return detail::compress(content, static_cast<int>(compressionLevel), 31);
    }
    default:
        return content;
    }
}

bool saveToFile(
    CompoundTag const&           nbt,
    std::filesystem::path const& path,
    NbtFileFormat                format,
    NbtCompressionType           compressionType,
    NbtCompressionLevel          compressionLevel,
    std::optional<int>           headerVersion
) {
    std::string content;
    switch (format) {
    case NbtFileFormat::LittleEndian: {
        content = nbt.toBinaryNbt(true);
        break;
    }
    case NbtFileFormat::LittleEndianWithHeader: {
        content = nbt.toBinaryNbtWithHeader(true, headerVersion);
        break;
    }
    case NbtFileFormat::BigEndian: {
        content = nbt.toBinaryNbt(false);
        break;
    }
    case NbtFileFormat::BigEndianWithHeader: {
        content = nbt.toBinaryNbtWithHeader(false, headerVersion);
        break;
    }
    case NbtFileFormat::BedrockNetwork: {
        content = nbt.toNetworkNbt();
        break;
    }
    default: {
        return false;
    }
    }
    if (!std::filesystem::exists(path.parent_path())) { std::filesystem::create_directories(path.parent_path()); }
    switch (compressionType) {
    case NbtCompressionType::Zlib: {
        content.assign(detail::compress(content, static_cast<int>(compressionLevel), 15));
        break;
    }
    case NbtCompressionType::Gzip: {
        content.assign(detail::compress(content, static_cast<int>(compressionLevel), 31));
        break;
    }
    default: {
        break;
    }
    }
    std::ofstream fWrite(path, std::ios::out | std::ios::binary);
    if (fWrite.is_open()) {
        fWrite.write(content.data(), static_cast<std::streamsize>(content.size()));
        fWrite.close();
        return true;
    }
    return false;
}

std::optional<CompoundTag> parseSnbtFromFile(std::filesystem::path const& path) {
    std::ifstream fRead(path, std::ios::ate);
    if (fRead.is_open()) {
        std::string content;
        auto        size = fRead.tellg();
        fRead.seekg(0);
        content.resize(static_cast<size_t>(size));
        fRead.read(content.data(), size);
        return CompoundTag::fromSnbt(content);
    }
    return std::nullopt;
}

bool saveSnbtToFile(
    CompoundTag const&           nbt,
    std::filesystem::path const& path,
    SnbtFormat                   format,
    uint8_t                      indent,
    SnbtNumberFormat             nfmt
) {
    std::ofstream fWrite;
    if (!std::filesystem::exists(path.parent_path())) { std::filesystem::create_directories(path.parent_path()); }
    fWrite.open(path, std::ios_base::out);
    if (!fWrite.is_open()) { return false; }
    fWrite << nbt.toSnbt(format, indent, nfmt);
    fWrite.close();
    return true;
}

std::optional<CompoundTag> parseSnbtFromContent(std::string_view content, std::optional<size_t> parsedLength) {
    return CompoundTag::fromSnbt(content, parsedLength);
}

std::string dumpSnbt(CompoundTag const& nbt, SnbtFormat format, uint8_t indent, SnbtNumberFormat snbtNumberFormat) {
    return nbt.toSnbt(format, indent, snbtNumberFormat);
}

bool validateContent(std::string_view binary, NbtFileFormat format, bool strictMatchSize) {
    switch (format) {
    case NbtFileFormat::LittleEndian: {
        BytesDataInput stream(binary, false, true);
        auto           streamSize = stream.size();
        if (static_cast<Tag::Type>(stream.getByte()) != Tag::Type::Compound) { return false; }
        if (stream.getPosition() + sizeof(short) > streamSize) { return false; }
        auto strSize = static_cast<size_t>(stream.getShort());
        if (stream.getPosition() + strSize > streamSize) { return false; }
        stream.ignoreBytes(strSize);
        if (!detail::validateCompoundTag(stream, streamSize)) { return false; }
        if (strictMatchSize) { return !stream.hasDataLeft(); }
        return true;
    }
    case NbtFileFormat::LittleEndianWithHeader: {
        BytesDataInput stream(binary, false, true);
        auto           streamSize = stream.size();
        if (stream.getPosition() + (2 * sizeof(int)) > streamSize) { return false; }
        stream.ignoreBytes(sizeof(int));
        auto nbtSize = static_cast<size_t>(stream.getInt());
        if (stream.getPosition() + nbtSize > streamSize) { return false; }
        if (static_cast<Tag::Type>(stream.getByte()) != Tag::Type::Compound) { return false; }
        if (stream.getPosition() + sizeof(short) > streamSize) { return false; }
        auto strSize = static_cast<size_t>(stream.getShort());
        if (stream.getPosition() + strSize > streamSize) { return false; }
        stream.ignoreBytes(strSize);
        if (!detail::validateCompoundTag(stream, streamSize)) { return false; }
        if (strictMatchSize) { return !stream.hasDataLeft(); }
        return true;
    }
    case NbtFileFormat::BigEndian: {
        BytesDataInput stream(binary, false, false);
        auto           streamSize = stream.size();
        if (static_cast<Tag::Type>(stream.getByte()) != Tag::Type::Compound) { return false; }
        if (stream.getPosition() + sizeof(short) > streamSize) { return false; }
        auto strSize = static_cast<size_t>(stream.getShort());
        if (stream.getPosition() + strSize > streamSize) { return false; }
        stream.ignoreBytes(strSize);
        if (!detail::validateCompoundTag(stream, streamSize)) { return false; }
        if (strictMatchSize) { return !stream.hasDataLeft(); }
        return true;
    }
    case NbtFileFormat::BigEndianWithHeader: {
        BytesDataInput stream(binary, false, false);
        auto           streamSize = stream.size();
        if (stream.getPosition() + (2 * sizeof(int)) > streamSize) { return false; }
        stream.ignoreBytes(sizeof(int));
        auto nbtSize = static_cast<size_t>(stream.getInt());
        if (stream.getPosition() + nbtSize > streamSize) { return false; }
        if (static_cast<Tag::Type>(stream.getByte()) != Tag::Type::Compound) { return false; }
        if (stream.getPosition() + sizeof(short) > streamSize) { return false; }
        auto strSize = static_cast<size_t>(stream.getShort());
        if (stream.getPosition() + strSize > streamSize) { return false; }
        stream.ignoreBytes(strSize);
        if (!detail::validateCompoundTag(stream, streamSize)) { return false; }
        if (strictMatchSize) { return !stream.hasDataLeft(); }
        return true;
    }
    case NbtFileFormat::BedrockNetwork: {
        bstream::ReadOnlyBinaryStream stream(binary, false, false);
        auto                          streamSize = stream.size();
        if (static_cast<Tag::Type>(stream.getByte()) != Tag::Type::Compound) { return false; }
        auto strSize = static_cast<size_t>(stream.getUnsignedVarInt());
        if (stream.isOverflowed() || stream.getPosition() + strSize > streamSize) { return false; }
        stream.ignoreBytes(strSize);
        if (!detail::validateCompoundTag(stream, streamSize)) { return false; }
        if (strictMatchSize) { return !stream.hasDataLeft(); }
        return true;
    }
    default:
        break;
    }
    return false;
}

bool validateFile(std::filesystem::path const& path, NbtFileFormat format, bool fileMemoryMap, bool strictMatchSize) {
    if (std::filesystem::exists(path)) {
        std::string content;
        detail::readFile(path, content, fileMemoryMap);
        content.assign(detail::decompress(content));
        return validateContent(content, format, strictMatchSize);
    }
    return false;
}

std::string encodeBsae64(std::string_view content) { return base64_utils::encode(content); }

std::string decodeBsae64(std::string_view content) { return base64_utils::decode(content); }

std::optional<CompoundTag>
parseFromBsae64(std::string_view content, std::optional<NbtFileFormat> format, bool strictMatchSize) {
    auto input = base64_utils::decode(content);
    return _parseFromBinary(input, format, strictMatchSize);
}

std::string saveAsBase64(
    CompoundTag const&  nbt,
    NbtFileFormat       format,
    NbtCompressionType  compressionType,
    NbtCompressionLevel compressionLevel,
    std::optional<int>  headerVersion
) {
    auto content = saveAsBinary(nbt, format, compressionType, compressionLevel, headerVersion);
    return base64_utils::encode(content);
}

int parseHeaderVersionFromContent(std::string_view content) { return CompoundTag::readHeaderVersion(content); }

int parseHeaderVersionFromFile(std::filesystem::path const& path) {
    std::ifstream file(path, std::ios::binary);
    if (file) {
        std::string buffer(4, '\0');
        file.read(buffer.data(), 4);
        file.close();
        return CompoundTag::readHeaderVersion(buffer);
    }
    return 0;
}

} // namespace nbt::io