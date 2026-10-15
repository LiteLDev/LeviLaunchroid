// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/NbtContent.hpp"
#include "nbt/io/NBTIO.hpp"
#include <fstream>

namespace nbt {

NbtContent::~NbtContent() {
    if (mAutoSave) { save(); }
}

NbtContent::NbtContent(
    std::string&                       buffer,
    CompoundTag&&                      fileData,
    bool                               isSnbt,
    std::optional<NbtFileFormat>       fileFormat,
    std::optional<NbtCompressionType>  compressionType,
    std::optional<NbtCompressionLevel> compressionLevel,
    std::optional<SnbtFormat>          snbtFormat,
    std::optional<uint8_t>             snbtIndent,
    std::optional<SnbtNumberFormat>    snbtNumberFormat
)
: TypedNbt(
      std::move(fileData),
      isSnbt,
      fileFormat,
      compressionType,
      compressionLevel,
      snbtFormat,
      snbtIndent,
      snbtNumberFormat
  ),
  mBuffer(buffer) {}

void NbtContent::save() const {
    if (mIsSnbtFile) {
        mBuffer.assign(toSnbt(mSnbtFormat.value_or(SnbtFormat::Minimize), mSnbtIndent.value_or(4)));
    } else {
        mBuffer.assign(
            io::saveAsBinary(
                *this,
                mFileFormat.value_or(NbtFileFormat::LittleEndian),
                mCompressionType.value_or(NbtCompressionType::Gzip),
                mCompressionLevel.value_or(NbtCompressionLevel::Default)
            )
        );
    }
}

std::optional<NbtContent>
NbtContent::open(std::string& content, std::optional<NbtFileFormat> fileFormat, bool strictMatchSize) {
    if (!fileFormat.has_value()) { fileFormat = io::detectContentFormat(content, strictMatchSize); }
    if (auto data = io::parseFromContent(content, fileFormat, strictMatchSize)) {
        auto compressionType = io::detectContentCompressionType(content);
        return NbtContent(
            content,
            std::move(data).value(),
            false,
            fileFormat,
            compressionType,
            NbtCompressionLevel::Default,
            std::nullopt,
            std::nullopt,
            std::nullopt
        );
    }
    return std::nullopt;
}

std::optional<NbtContent> NbtContent::openSnbt(std::string& content) {
    if (auto data = CompoundTag::fromSnbt(content)) {
        return NbtContent(
            content,
            std::move(data).value(),
            true,
            std::nullopt,
            std::nullopt,
            std::nullopt,
            SnbtFormat::Minimize,
            0,
            SnbtNumberFormat::Default
        );
    }
    return std::nullopt;
}

} // namespace nbt
