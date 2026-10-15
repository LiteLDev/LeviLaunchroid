// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/TypedNbt.hpp"

namespace nbt {

TypedNbt::TypedNbt(
    CompoundTag&&                      fileData,
    bool                               isSnbt,
    std::optional<NbtFileFormat>       fileFormat,
    std::optional<NbtCompressionType>  compressionType,
    std::optional<NbtCompressionLevel> compressionLevel,
    std::optional<SnbtFormat>          snbtFormat,
    std::optional<uint8_t>             snbtIndent,
    std::optional<SnbtNumberFormat>    snbtNumberFormat
)
: CompoundTag(std::move(fileData)),
  mIsSnbtFile(isSnbt),
  mFileFormat(fileFormat),
  mCompressionType(compressionType),
  mCompressionLevel(compressionLevel),
  mSnbtFormat(snbtFormat),
  mSnbtIndent(snbtIndent),
  mSnbtNumberFormat(snbtNumberFormat),
  mAutoSave(true) {}

void TypedNbt::setFileData(CompoundTag const& fileData) { static_cast<CompoundTag&>(*this) = fileData; }

void TypedNbt::setIsSnbtFile(bool isSnbt) {
    mIsSnbtFile = isSnbt;
    if (mIsSnbtFile) { mFileFormat.reset(); }
}

void TypedNbt::setFileFormat(NbtFileFormat format) {
    mIsSnbtFile = false;
    mFileFormat = format;
}

void TypedNbt::setCompressionType(NbtCompressionType type) { mCompressionType = type; }

void TypedNbt::setCompressionLevel(NbtCompressionLevel level) { mCompressionLevel = level; }

void TypedNbt::setSnbtFormat(SnbtFormat format) {
    setIsSnbtFile(true);
    mSnbtFormat = format;
}

void TypedNbt::setSnbtIndent(uint8_t indent) { mSnbtIndent = indent; }

void TypedNbt::setAutoSave(bool value) { mAutoSave = value; }


} // namespace nbt