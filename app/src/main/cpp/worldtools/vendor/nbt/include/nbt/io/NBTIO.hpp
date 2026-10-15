// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <filesystem>
#include <nbt/types/CompoundTagVariant.hpp>
#include <nbt/types/NbtCompressionLevel.hpp>
#include <nbt/types/NbtCompressionType.hpp>
#include <nbt/types/NbtFileFormat.hpp>

namespace nbt::io {

[[nodiscard]] NBT_API std::optional<NbtFileFormat>
                      detectContentFormat(std::string_view content, bool strictMatchSize = true);

[[nodiscard]] NBT_API std::optional<NbtFileFormat>
detectFileFormat(std::filesystem::path const& path, bool fileMemoryMap = false, bool strictMatchSize = true);

[[nodiscard]] NBT_API NbtCompressionType
detectFileCompressionType(std::filesystem::path const& path, bool fileMemoryMap = false);

[[nodiscard]] NBT_API NbtCompressionType detectContentCompressionType(std::string_view content);

[[nodiscard]] NBT_API std::optional<CompoundTag> parseFromContent(
    std::string_view             content,
    std::optional<NbtFileFormat> format          = std::nullopt,
    bool                         strictMatchSize = true
);

[[nodiscard]] NBT_API std::optional<CompoundTag> parseFromFile(
    std::filesystem::path const& path,
    std::optional<NbtFileFormat> format          = std::nullopt,
    bool                         fileMemoryMap   = false,
    bool                         strictMatchSize = true
);

[[nodiscard]] NBT_API std::string saveAsBinary(
    CompoundTag const&  nbt,
    NbtFileFormat       format           = NbtFileFormat::LittleEndian,
    NbtCompressionType  compressionType  = NbtCompressionType::Gzip,
    NbtCompressionLevel compressionLevel = NbtCompressionLevel::Default,
    std::optional<int>  headerVersion    = std::nullopt
);

NBT_API bool saveToFile(
    CompoundTag const&           nbt,
    std::filesystem::path const& path,
    NbtFileFormat                format           = NbtFileFormat::LittleEndian,
    NbtCompressionType           compressionType  = NbtCompressionType::Gzip,
    NbtCompressionLevel          compressionLevel = NbtCompressionLevel::Default,
    std::optional<int>           headerVersion    = std::nullopt
);

[[nodiscard]] NBT_API std::optional<CompoundTag> parseSnbtFromFile(std::filesystem::path const& path);

NBT_API bool saveSnbtToFile(
    CompoundTag const&           nbt,
    std::filesystem::path const& path,
    SnbtFormat                   format          = SnbtFormat::Default,
    uint8_t                      indent          = 4,
    SnbtNumberFormat             nbtNumberFormat = SnbtNumberFormat::Default
);

[[nodiscard]] NBT_API std::optional<CompoundTag>
                      parseSnbtFromContent(std::string_view content, std::optional<size_t> parsedLength = std::nullopt);

[[nodiscard]] NBT_API std::string dumpSnbt(
    CompoundTag const& nbt,
    SnbtFormat         format          = SnbtFormat::Default,
    uint8_t            indent          = 4,
    SnbtNumberFormat   nbtNumberFormat = SnbtNumberFormat::Default
);

[[nodiscard]] NBT_API bool validateContent(
    std::string_view binary,
    NbtFileFormat    format          = NbtFileFormat::LittleEndian,
    bool             strictMatchSize = true
);

[[nodiscard]] NBT_API bool validateFile(
    std::filesystem::path const& path,
    NbtFileFormat                format          = NbtFileFormat::LittleEndian,
    bool                         fileMemoryMap   = false,
    bool                         strictMatchSize = true
);

[[nodiscard]] NBT_API std::string encodeBsae64(std::string_view content);

[[nodiscard]] NBT_API std::string decodeBsae64(std::string_view content);

[[nodiscard]] NBT_API std::optional<CompoundTag> parseFromBsae64(
    std::string_view             content,
    std::optional<NbtFileFormat> format          = std::nullopt,
    bool                         strictMatchSize = true
);

[[nodiscard]] NBT_API std::string saveAsBase64(
    CompoundTag const&  nbt,
    NbtFileFormat       format           = NbtFileFormat::LittleEndian,
    NbtCompressionType  compressionType  = NbtCompressionType::Gzip,
    NbtCompressionLevel compressionLevel = NbtCompressionLevel::Default,
    std::optional<int>  headerVersion    = std::nullopt
);

[[nodiscard]] NBT_API int parseHeaderVersionFromContent(std::string_view content);

[[nodiscard]] NBT_API int parseHeaderVersionFromFile(std::filesystem::path const& path);

} // namespace nbt::io
