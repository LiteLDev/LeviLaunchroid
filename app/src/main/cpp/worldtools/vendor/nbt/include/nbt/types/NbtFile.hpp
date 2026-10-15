// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <filesystem>
#include <nbt/types/TypedNbt.hpp>

namespace nbt {

class NbtFile : public TypedNbt {
public:
    std::filesystem::path mFilePath{};

public:
    NBT_API ~NbtFile();

    NBT_API NbtFile(
        std::filesystem::path const&       filePath,
        CompoundTag&&                      fileData,
        bool                               isSnbt,
        std::optional<NbtFileFormat>       fileFormat,
        std::optional<NbtCompressionType>  compressionType,
        std::optional<NbtCompressionLevel> compressionLevel,
        std::optional<SnbtFormat>          snbtFormat,
        std::optional<uint8_t>             snbtIndent,
        std::optional<SnbtNumberFormat>    snbtNumberFormat
    );

    NBT_API void setFilePath(std::filesystem::path const& filePath);

    NBT_API void save() const;

public:
    [[nodiscard]] NBT_API static std::optional<NbtFile> open(
        std::filesystem::path const& filePath,
        std::optional<NbtFileFormat> fileFormat      = std::nullopt,
        bool                         fileMemoryMap   = false,
        bool                         strictMatchSize = true
    );

    [[nodiscard]] NBT_API static std::optional<NbtFile> openSnbt(std::filesystem::path const& filePath);
};

} // namespace nbt
