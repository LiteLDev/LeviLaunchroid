// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once

#ifdef _NBT_EXPORT
#ifdef _WIN32
#ifdef _NBT_DLL
#define NBT_API __declspec(dllimport)
#else
#define NBT_API __declspec(dllexport)
#endif
#else
#define NBT_API __attribute__((visibility("default"), used))
#endif
#else
#define NBT_API
#endif