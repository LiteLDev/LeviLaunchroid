// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <cstdint>

namespace nbt::literals {

namespace integral {

constexpr uint8_t operator""_b(unsigned long long value) noexcept { return static_cast<uint8_t>(value); }

constexpr short operator""_s(unsigned long long value) noexcept { return static_cast<short>(value); }

constexpr int operator""_i(unsigned long long value) noexcept { return static_cast<int>(value); }

constexpr int64_t operator""_l(unsigned long long value) noexcept { return static_cast<int64_t>(value); }

constexpr int8_t operator""_sb(unsigned long long value) noexcept { return static_cast<int8_t>(value); }

constexpr short operator""_ss(unsigned long long value) noexcept { return static_cast<short>(value); }

constexpr int operator""_si(unsigned long long value) noexcept { return static_cast<int>(value); }

constexpr int64_t operator""_sl(unsigned long long value) noexcept { return static_cast<int64_t>(value); }

constexpr uint8_t operator""_ub(unsigned long long value) noexcept { return static_cast<uint8_t>(value); }

constexpr uint16_t operator""_us(unsigned long long value) noexcept { return static_cast<uint16_t>(value); }

constexpr uint32_t operator""_ui(unsigned long long value) noexcept { return static_cast<uint32_t>(value); }

constexpr uint64_t operator""_ul(unsigned long long value) noexcept { return static_cast<uint64_t>(value); }

} // namespace integral

namespace floating_point {

constexpr float operator""_f(long double value) noexcept { return static_cast<float>(value); }

constexpr double operator""_d(long double value) noexcept { return static_cast<double>(value); }

} // namespace floating_point

} // namespace nbt::literals
