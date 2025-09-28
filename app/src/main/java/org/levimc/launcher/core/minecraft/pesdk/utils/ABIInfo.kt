/*
 * Copyright (C) 2018-2021 Тимашков Иван
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.levimc.launcher.core.minecraft.pesdk.utils

import android.os.Build

/**
 * @author Тимашков Иван
 * @author https://github.com/TimScriptov
 */
object ABIInfo {
    @JvmStatic
    fun getABI(): String {
        for (androidArch in Build.SUPPORTED_64_BIT_ABIS) {
            if (androidArch.contains("arm64-v8a")) {
                return "arm64-v8a"
            } else if (androidArch.contains("x86_64")) {
                return "x86"//"x86_64"
            }
        }
        for (androidArch in Build.SUPPORTED_32_BIT_ABIS) {
            if (androidArch.contains("armeabi-v7a")) {
                return "armeabi-v7a"
            } else if (androidArch.contains("x86")) {
                return "x86"
            }
        }
        @Suppress("DEPRECATION")
        return Build.CPU_ABI
    }
}