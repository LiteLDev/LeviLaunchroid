// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <nbt/types/TypedNbt.hpp>

namespace nbt {

class NbtContent : public TypedNbt {
public:
    std::string& mBuffer;

public:
    NBT_API ~NbtContent();

    NBT_API NbtContent(
        std::string&                       buffer,
        CompoundTag&&                      fileData,
        bool                               isSnbt,
        std::optional<NbtFileFormat>       fileFormat,
        std::optional<NbtCompressionType>  compressionType,
        std::optional<NbtCompressionLevel> compressionLevel,
        std::optional<SnbtFormat>          snbtFormat,
        std::optional<uint8_t>             snbtIndent,
        std::optional<SnbtNumberFormat>    snbtNumberFormat
    );

    NBT_API void save() const;

public:
    [[nodiscard]] NBT_API static std::optional<NbtContent>
    open(std::string& content, std::optional<NbtFileFormat> fileFormat = std::nullopt, bool strictMatchSize = true);

    [[nodiscard]] NBT_API static std::optional<NbtContent> openSnbt(std::string& content);
};

} // namespace nbt
