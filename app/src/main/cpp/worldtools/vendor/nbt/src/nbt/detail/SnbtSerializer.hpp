// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include "nbt/types/SnbtFormat.hpp"
#include <string>

namespace nbt {

class CompoundTag;
class EndTag;
class ByteTag;
class ShortTag;
class IntTag;
class LongTag;
class FloatTag;
class DoubleTag;
class ByteArrayTag;
class StringTag;
class ListTag;
class IntArrayTag;
class LongArrayTag;

namespace detail {

std::string TypedToSnbt(ByteTag const&, uint8_t, SnbtFormat, bool, SnbtNumberFormat);
std::string TypedToSnbt(ShortTag const&, uint8_t, SnbtFormat, bool, SnbtNumberFormat);
std::string TypedToSnbt(IntTag const&, uint8_t, SnbtFormat, bool, SnbtNumberFormat);
std::string TypedToSnbt(LongTag const&, uint8_t, SnbtFormat, bool, SnbtNumberFormat);
std::string TypedToSnbt(FloatTag const&, uint8_t, SnbtFormat, bool, SnbtNumberFormat);
std::string TypedToSnbt(DoubleTag const&, uint8_t, SnbtFormat, bool, SnbtNumberFormat);
std::string TypedToSnbt(ByteArrayTag const&, uint8_t, SnbtFormat, bool, SnbtNumberFormat);
std::string TypedToSnbt(StringTag const&, uint8_t, SnbtFormat, bool, SnbtNumberFormat);
std::string TypedToSnbt(ListTag const&, uint8_t, SnbtFormat, bool, SnbtNumberFormat);
std::string TypedToSnbt(CompoundTag const&, uint8_t, SnbtFormat, bool, SnbtNumberFormat);
std::string TypedToSnbt(IntArrayTag const&, uint8_t, SnbtFormat, bool, SnbtNumberFormat);
std::string TypedToSnbt(LongArrayTag const&, uint8_t, SnbtFormat, bool, SnbtNumberFormat);
std::string TypedToSnbt(EndTag const&, uint8_t, SnbtFormat, bool, SnbtNumberFormat);

} // namespace detail

} // namespace nbt
