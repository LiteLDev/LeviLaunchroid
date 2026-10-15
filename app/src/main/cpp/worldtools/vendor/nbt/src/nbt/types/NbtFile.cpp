// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/types/NbtFile.hpp"
#include "nbt/detail/FileUtils.hpp"
#include "nbt/io/NBTIO.hpp"
#include <fstream>

namespace nbt {

NbtFile::~NbtFile() {
    if (mAutoSave) { save(); }
}

NbtFile::NbtFile(
    std::filesystem::path const&       filePath,
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
  mFilePath(filePath) {}

void NbtFile::setFilePath(std::filesystem::path const& filePath) { mFilePath = filePath; }

void NbtFile::save() const {
    if (mIsSnbtFile) {
        io::saveSnbtToFile(*this, mFilePath, mSnbtFormat.value_or(SnbtFormat::Minimize), mSnbtIndent.value_or(4));
    } else {
        io::saveToFile(
            *this,
            mFilePath,
            mFileFormat.value_or(NbtFileFormat::LittleEndian),
            mCompressionType.value_or(NbtCompressionType::Gzip),
            mCompressionLevel.value_or(NbtCompressionLevel::Default)
        );
    }
}

std::optional<NbtFile> NbtFile::open(
    std::filesystem::path const& filePath,
    std::optional<NbtFileFormat> fileFormat,
    bool                         fileMemoryMap,
    bool                         strictMatchSize
) {
    std::string content;
    auto        absPath = std::filesystem::absolute(filePath);
    detail::readFile(absPath, content, fileMemoryMap);
    if (!fileFormat.has_value()) { fileFormat = io::detectContentFormat(content, strictMatchSize); }
    if (auto data = io::parseFromContent(content, fileFormat, strictMatchSize)) {
        auto compressionType = io::detectContentCompressionType(content);
        return NbtFile(
            absPath,
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

std::optional<NbtFile> NbtFile::openSnbt(std::filesystem::path const& filePath) {
    auto          absPath = std::filesystem::absolute(filePath);
    std::ifstream fRead(absPath, std::ios::ate);
    if (fRead.is_open()) {
        std::string content;
        auto        size = fRead.tellg();
        fRead.seekg(0);
        content.resize(static_cast<size_t>(size));
        fRead.read(content.data(), size);
        if (auto data = CompoundTag::fromSnbt(content)) {
            return NbtFile(
                absPath,
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
    }
    return std::nullopt;
}

} // namespace nbt
