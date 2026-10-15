// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <nbt/types/CompoundTagVariant.hpp>
#include <nbt/types/NbtCompressionLevel.hpp>
#include <nbt/types/NbtCompressionType.hpp>
#include <nbt/types/NbtFileFormat.hpp>

namespace nbt {

class TypedNbt : public CompoundTag {
public:
    bool                               mIsSnbtFile{false};
    std::optional<NbtFileFormat>       mFileFormat{};
    std::optional<NbtCompressionType>  mCompressionType{};
    std::optional<NbtCompressionLevel> mCompressionLevel{};
    std::optional<SnbtFormat>          mSnbtFormat{};
    std::optional<uint8_t>             mSnbtIndent{};
    std::optional<SnbtNumberFormat>    mSnbtNumberFormat{};
    bool                               mAutoSave{true};

public:
    NBT_API TypedNbt(
        CompoundTag&&                      fileData,
        bool                               isSnbt,
        std::optional<NbtFileFormat>       fileFormat,
        std::optional<NbtCompressionType>  compressionType,
        std::optional<NbtCompressionLevel> compressionLevel,
        std::optional<SnbtFormat>          snbtFormat,
        std::optional<uint8_t>             snbtIndent,
        std::optional<SnbtNumberFormat>    snbtNumberFormat
    );

    NBT_API void setFileData(CompoundTag const& fileData);

    NBT_API void setIsSnbtFile(bool isSnbt);

    NBT_API void setFileFormat(NbtFileFormat format);

    NBT_API void setCompressionType(NbtCompressionType type);

    NBT_API void setCompressionLevel(NbtCompressionLevel level);

    NBT_API void setSnbtFormat(SnbtFormat format);

    NBT_API void setSnbtIndent(uint8_t indent);

    NBT_API void setAutoSave(bool value);

    NBT_API void setSnbtNumberFormat(SnbtNumberFormat format);
};

} // namespace nbt