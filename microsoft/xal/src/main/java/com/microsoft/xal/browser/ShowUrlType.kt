/*
 * Copyright (C) 2018-2022 Тимашков Иван
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
package com.microsoft.xal.browser

import org.jetbrains.annotations.Contract

/**
 * 13.08.2022
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
enum class ShowUrlType {
    Normal,
    CookieRemoval,
    CookieRemovalSkipIfSharedCredentials,
    NonAuthFlow;

    companion object {
        @JvmStatic
        @Contract(pure = true)
        fun fromInt(value: Int): ShowUrlType? {
            return when (value) {
                0 -> Normal
                1 -> CookieRemoval
                2 -> CookieRemovalSkipIfSharedCredentials
                3 -> NonAuthFlow
                else -> null
            }
        }
    }

    override fun toString(): String {
        val i = entries[ordinal].ordinal
        return when (i) {
            0 -> "Normal"
            1 -> "CookieRemoval"
            2 -> "CookieRemovalSkipIfSharedCredentials"
            3 -> "NonAuthFlow"
            else -> "Unknown"
        }
    }
}
