// Copyright © 2025 GlacieTeam.All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <nbt/io/NBTIO.hpp>
#include <nbt/types/Literals.hpp>
#include <nbt/types/NbtContent.hpp>
#include <nbt/types/NbtFile.hpp>

namespace nbt {

[[nodiscard]] NBT_API std::optional<NbtFile> open(std::filesystem::path const& path);

[[nodiscard]] NBT_API std::optional<NbtContent> load(std::string& content);

[[nodiscard]] NBT_API std::optional<CompoundTag> parse(std::string_view content);

} // namespace nbt