// Copyright © 2025 GlacieTeam.All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/NBT.hpp"

namespace nbt {

std::optional<NbtFile> open(std::filesystem::path const& path) {
    if (auto result = NbtFile::open(path)) { return result; }
    if (auto result = NbtFile::openSnbt(path)) { return result; }
    return std::nullopt;
}

std::optional<NbtContent> load(std::string& content) {
    if (auto result = NbtContent::open(content)) { return result; }
    if (auto result = NbtContent::openSnbt(content)) { return result; }
    return std::nullopt;
}

std::optional<CompoundTag> parse(std::string_view content) {
    if (auto result = io::parseFromContent(content)) { return result; }
    if (auto result = io::parseSnbtFromContent(content)) { return result; }
    return std::nullopt;
}

} // namespace nbt